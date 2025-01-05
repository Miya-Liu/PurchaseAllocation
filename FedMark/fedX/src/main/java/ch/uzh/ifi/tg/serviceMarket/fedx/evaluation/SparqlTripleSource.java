/*
 * Copyright (C) 2008-2013, fluid Operations AG
 *
 * FedX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.uzh.ifi.tg.serviceMarket.fedx.evaluation;

import ch.uzh.ifi.tg.serviceMarket.fedx.FederationManager;
import ch.uzh.ifi.tg.serviceMarket.fedx.algebra.ExclusiveGroup;
import ch.uzh.ifi.tg.serviceMarket.fedx.algebra.FilterValueExpr;
import ch.uzh.ifi.tg.serviceMarket.fedx.evaluation.iterator.FilteringInsertBindingsIteration;
import ch.uzh.ifi.tg.serviceMarket.fedx.evaluation.iterator.FilteringIteration;
import ch.uzh.ifi.tg.serviceMarket.fedx.evaluation.iterator.InsertBindingsIteration;
import ch.uzh.ifi.tg.serviceMarket.fedx.exception.ExceptionUtil;
import ch.uzh.ifi.tg.serviceMarket.fedx.structures.Endpoint;
import ch.uzh.ifi.tg.serviceMarket.fedx.structures.SparqlEndpointConfiguration;
import ch.uzh.ifi.tg.serviceMarket.fedx.util.QueryStringUtil;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.provider.ServiceDescription;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.transform.RemoveBindingsTransform;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.transform.ServiceConstraintsTransform;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.visitor.TableVistor;
import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.EmptyIteration;
import info.aduna.iteration.ExceptionConvertingIteration;
import info.aduna.iteration.Iterations;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.OpTable;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.impl.EmptyBindingSet;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A triple source to be used for (remote) SPARQL endpoints.<p>
 * 
 * This triple source supports the {@link SparqlEndpointConfiguration} for
 * defining whether ASK queries are to be used for source selection.
 * 
 * @author Andreas Schwarte
 *
 */
public class SparqlTripleSource extends TripleSourceBase implements TripleSource {

	
	private boolean useASKQueries = true;
	
	SparqlTripleSource(Endpoint endpoint) {
		super(FederationManager.getMonitoringService(), endpoint);
		if (endpoint.getEndpointConfiguration() instanceof SparqlEndpointConfiguration) {
			SparqlEndpointConfiguration c = (SparqlEndpointConfiguration) endpoint.getEndpointConfiguration();
			this.useASKQueries = c.supportsASKQueries();
		}			
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> getStatements(
			String preparedQuery, RepositoryConnection conn, BindingSet bindings, FilterValueExpr filterExpr)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		// Get Bindings
		/*Query jenaQuery = QueryFactory.create(preparedQuery);
		Op op = Algebra.compile(jenaQuery);
		RemoveBindingsTransform removeBindingsTransform = new RemoveBindingsTransform();
		op = Transformer.transform(removeBindingsTransform, op);
		jenaQuery = OpAsQuery.asQuery(op);
		preparedQuery = jenaQuery.toString();*/

        // Get graph index
		int graphIndex = 0;
		Pattern p = Pattern.compile("\\?graph(\\d+)");
		Matcher m = p.matcher(preparedQuery);
		if (m.find()) {
			graphIndex = Integer.parseInt(m.group(1));
		}

        Query jenaQuery = QueryFactory.create(preparedQuery);
        Op op = Algebra.compile(jenaQuery);
        op = Transformer.transform(new RemoveBindingsTransform(), op);
        /*op = Transformer.transform(new ServiceConstraintsTransform(graphIndex, conn.toString(), FederationManager.getInstance().getServiceDescription()), op);

        if (op instanceof ServiceConstraintsTransform.OpRemove) {
            return new EmptyIteration<BindingSet, QueryEvaluationException>();
        }*/

        jenaQuery = OpAsQuery.asQuery(op);
        preparedQuery = jenaQuery.toString();

		//if (hasValidProducts(conn, removeBindingsTransform.getOpTables(), graphIndex)) {

			TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, preparedQuery, null);
			disableInference(query);

			CloseableIteration<BindingSet, QueryEvaluationException> res = null;
			try {

				// evaluate the query
				monitorRemoteRequest();
				res = query.evaluate();

				// apply filter and/or insert original bindings
				if (filterExpr != null) {
					if (bindings.size() > 0)
						res = new FilteringInsertBindingsIteration(filterExpr, bindings, res);
					else
						res = new FilteringIteration(filterExpr, res);
					if (!res.hasNext()) {
						Iterations.closeCloseable(res);
						return new EmptyIteration<BindingSet, QueryEvaluationException>();
					}
				} else if (bindings.size() > 0) {
					res = new InsertBindingsIteration(res, bindings);
				}

				return res;

			} catch (QueryEvaluationException ex) {
				Iterations.closeCloseable(res);
				throw ExceptionUtil.traceExceptionSourceAndRepair(conn, ex, "Subquery: " + preparedQuery);
			}
		/*}

		return new EmptyIteration<BindingSet, QueryEvaluationException>();*/
	}

	/*private boolean hasValidProducts(RepositoryConnection conn, List<OpTable> bindings, int graphIndex) {

		ServiceDescription serviceDescription = FederationManager.getInstance().getServiceDescription();

		boolean hasProducts = false;

        String askQuery = "ASK { " + serviceDescription.getServiceDescription() + " }";

		if (bindings.size() > 0) {
			for (OpTable binding : bindings) {
				Query bindingsQuery = OpAsQuery.asQuery(binding);
				String bindingValues = bindingsQuery.getQueryPattern().toString();
				bindingValues = bindingValues.replaceAll("(\\?\\S+)_\\d+", "$1");
                Query newBindingQuery = QueryFactory.create("SELECT * WHERE { " + bindingValues + " }");
                QuerySolutionMap querySolutionMap = TableVistor.solutions(Algebra.compile(newBindingQuery));
                hasProducts = hasProducts || runAskQuery(conn, serviceDescription, askQuery, graphIndex, querySolutionMap);
				if (hasProducts) {
					break;
				}
			}
		} else {
			hasProducts = runAskQuery(conn, serviceDescription, askQuery, graphIndex, new QuerySolutionMap());
		}

		return hasProducts;
	}

	private boolean runAskQuery(RepositoryConnection conn, ServiceDescription serviceDescription, String askQuery, int graphIndex, QuerySolutionMap querySolutionMap) {
		String host = conn.toString();
		Model model = serviceDescription.getServiceModel();

		String productsQuery = "SELECT ?product WHERE { <" + host + "> <http://www.example.com/hosts> ?product }";

		QueryExecution queryExec = QueryExecutionFactory.create(productsQuery, model);
		ResultSet results = queryExec.execSelect();

		boolean hasProducts = false;

		while (results.hasNext()) {
			QuerySolution solution = results.next();

            QuerySolutionMap initialBinding = new QuerySolutionMap();
            initialBinding.addAll(querySolutionMap);
            initialBinding.add("product" + graphIndex, solution.get("product"));

			queryExec = QueryExecutionFactory.create(askQuery, model, initialBinding);
            boolean askQueryResponse = queryExec.execAsk();
			hasProducts = hasProducts || askQueryResponse;
			if (hasProducts) {
				break;
			}
		}
		return hasProducts;
	}*/

	public CloseableIteration<BindingSet, QueryEvaluationException> getStatements(
			StatementPattern stmt, RepositoryConnection conn,
			BindingSet bindings, FilterValueExpr filterExpr)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException  {
		
		throw new RuntimeException("NOT YET IMPLEMENTED.");
	}

	@Override
	public boolean hasStatements(RepositoryConnection conn, Resource subj,
			URI pred, Value obj, Resource... contexts)
			throws RepositoryException {
		
		if (!useASKQueries) {
			StatementPattern st = new StatementPattern(new Var("s", subj), new Var("p", pred), new Var("o", obj));
			try {
				return hasStatements(st, conn, EmptyBindingSet.getInstance());
			} catch (Exception e) {
				throw new RepositoryException(e);
			}
		}		
		return super.hasStatements(conn, subj, pred, obj, contexts);
	}

	public boolean hasStatements(StatementPattern stmt, RepositoryConnection conn,
			BindingSet bindings) throws RepositoryException,
			MalformedQueryException, QueryEvaluationException {

		// decide whether to use ASK queries or a SELECT query
		if (useASKQueries) {
			/* remote boolean query */
			String queryString = QueryStringUtil.askQueryString(stmt, bindings);
			BooleanQuery query = conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryString, null);
			disableInference(query);
			
			try {
				monitorRemoteRequest();
				boolean hasStatements = query.evaluate();
				return hasStatements;
			} catch (QueryEvaluationException ex) {
				throw ExceptionUtil.traceExceptionSourceAndRepair(conn, ex, "Subquery: " + queryString);			
			}
			
		} else {
			/* remote select limit 1 query */
			String queryString = QueryStringUtil.selectQueryStringLimit1(stmt, bindings);
			TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			disableInference(query);
			
			TupleQueryResult qRes = null;
			try {
				monitorRemoteRequest();
				qRes = query.evaluate();
				boolean hasStatements = qRes.hasNext();
				return hasStatements;
			} catch (QueryEvaluationException ex) {
				throw ExceptionUtil.traceExceptionSourceAndRepair(conn, ex, "Subquery: " + queryString);			
			} finally {
				if (qRes!=null)
					qRes.close();
			}
		}
		
	}
	
	@Override
	public boolean hasStatements(ExclusiveGroup group,
			RepositoryConnection conn, BindingSet bindings)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {
		
		if (!useASKQueries) {
			
			/* remote select limit 1 query */
			String queryString = QueryStringUtil.selectQueryStringLimit1(group, bindings);
			TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			disableInference(query);
			
			TupleQueryResult qRes = null;
			try {
				monitorRemoteRequest();
				qRes = query.evaluate();
				boolean hasStatements = qRes.hasNext();
				return hasStatements;
			} catch (QueryEvaluationException ex) {
				throw ExceptionUtil.traceExceptionSourceAndRepair(conn, ex, "Subquery: " + queryString);			
			} finally {
				if (qRes!=null)
					qRes.close();
			}
		}		
		
		// default handling: use ASK query
		return super.hasStatements(group, conn, bindings);
	}

	public boolean usePreparedQuery() {
		return true;
	}

	public CloseableIteration<BindingSet, QueryEvaluationException> getStatements(
			TupleExpr preparedQuery, RepositoryConnection conn,
			BindingSet bindings, FilterValueExpr filterExpr)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {
		
		throw new RuntimeException("NOT YET IMPLEMENTED.");
	}

	public CloseableIteration<Statement, QueryEvaluationException> getStatements(
			RepositoryConnection conn, Resource subj, URI pred, Value obj,
			Resource... contexts) throws RepositoryException,
			MalformedQueryException, QueryEvaluationException
	{
		
		// TODO add handling for contexts
		monitorRemoteRequest();
		RepositoryResult<Statement> repoResult = conn.getStatements(subj, pred, obj, true);
		
		return new ExceptionConvertingIteration<Statement, QueryEvaluationException>(repoResult) {
			@Override
			protected QueryEvaluationException convert(Exception arg0) {
				return new QueryEvaluationException(arg0);
			}
		};		
	}

}
