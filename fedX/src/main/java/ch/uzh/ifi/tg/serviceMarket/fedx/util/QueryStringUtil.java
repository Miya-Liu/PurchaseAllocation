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

package ch.uzh.ifi.tg.serviceMarket.fedx.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import ch.uzh.ifi.tg.serviceMarket.fedx.FederationManager;
import ch.uzh.ifi.tg.serviceMarket.fedx.algebra.ExclusiveGroup;
import ch.uzh.ifi.tg.serviceMarket.fedx.algebra.FedXStatementPattern;
import ch.uzh.ifi.tg.serviceMarket.fedx.algebra.IndependentJoinGroup;
import ch.uzh.ifi.tg.serviceMarket.fedx.algebra.StatementTupleExpr;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.parser.Parser;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.parser.TriplePattern;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.provider.ServiceDescription;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.provider.StatementAndProviderVars;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.log4j.Logger;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.algebra.StatementPattern;
import org.openrdf.query.algebra.Var;
import org.openrdf.query.impl.EmptyBindingSet;

import ch.uzh.ifi.tg.serviceMarket.fedx.algebra.ExclusiveStatement;
import ch.uzh.ifi.tg.serviceMarket.fedx.algebra.FilterValueExpr;
import ch.uzh.ifi.tg.serviceMarket.fedx.evaluation.SparqlFederationEvalStrategyWithValues;
import ch.uzh.ifi.tg.serviceMarket.fedx.evaluation.iterator.BoundJoinVALUESConversionIteration;
import ch.uzh.ifi.tg.serviceMarket.fedx.exception.FilterConversionException;
import ch.uzh.ifi.tg.serviceMarket.fedx.exception.IllegalQueryException;

/**
 * Various static functions for query handling and parsing.
 * 
 * @author Andreas Schwarte
 */
public class QueryStringUtil {
	
	/* TODO make operations more performant, i.e. use same instance of StringBuilder more permanently */

	public static Logger log = Logger.getLogger(QueryStringUtil.class);
	
	/**
	 * A dummy URI which is used as a replacement for {@link BNode}s in {@link #appendBNode(StringBuilder, BNode)}
	 * since BNodes cannot be expressed in SPARQL queries
	 */
	public static final URI BNODE_URI = ValueFactoryImpl.getInstance().createURI("http://fluidops.com/fedx/bnode");
	
	/**
	 * returns true iff there is at least one free variable, i.e. there is no binding
	 * for any variable
	 * 
	 * @param stmt
	 * @param bindings
	 * @return
	 */
	public static boolean hasFreeVars(StatementPattern stmt, BindingSet bindings) {
		for (Var var : stmt.getVarList()) {
			if(!var.hasValue() && !bindings.hasBinding(var.getName()))
				return true;	// there is at least one free var				
		}
		return false;
	}
	
		
	/**
	 * Return a string representation of this statement using the following pattern, where
	 * variables are indicated using ?var and values are represented as strings.
	 * 
	 * Pattern: {s; p; o} 
	 * 
	 * @param stmt
	 * @return
	 */
	public static String toString(StatementPattern stmt) {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		appendVar(sb, stmt.getSubjectVar(), new HashSet<String>(), EmptyBindingSet.getInstance());
		sb.append("; ");
		appendVar(sb, stmt.getPredicateVar(), new HashSet<String>(), EmptyBindingSet.getInstance());
		sb.append("; ");
		appendVar(sb, stmt.getObjectVar(), new HashSet<String>(), EmptyBindingSet.getInstance());
		sb.append("}");
		return sb.toString();
	}
	
	public static String toString(Var var) {
		if (!var.hasValue())
			return "?" + var.getName();
		return getValueString(var.getValue());
	}
	
	
	/**
	 * Return a string representation of this statement using the following pattern, where
	 * variables are indicated using ?var and values are represented as strings.
	 * 
	 * Pattern: {s; p; o} 
	 *
	 * @return
	 */
	public static String toString(Resource subj, URI pred, Value obj) {
		return toString(QueryAlgebraUtil.toStatementPattern(subj, pred, obj));
	}
	
	
	/**
	 * Construct a SELECT query for the provided statement. 
	 * 
	 * @param stmt
	 * @param bindings
	 * @param filterExpr
	 * @param evaluated
	 * 			parameter can be used outside this method to check whether FILTER has been evaluated, false in beginning
	 * 
	 * @return
	 * @throws IllegalQueryException
	 * 				if the query does not have any free variables
	 */
	public static String selectQueryString(FedXStatementPattern stmt, BindingSet bindings, FilterValueExpr filterExpr, Boolean evaluated) throws IllegalQueryException {
		
		Set<String> varNames = new HashSet<String>();
		StatementAndProviderVars statementAndProviderVars = constructStatement(stmt, varNames, bindings);
		String s = statementAndProviderVars.getStatement().toString();
		varNames.addAll(statementAndProviderVars.getVars());
		
		// project only relevant variables, i.e. do not bind local variables
		varNames = project(stmt, varNames);
		
		StringBuilder res = new StringBuilder();
		
		res.append("SELECT ");
		
		if (varNames.size()==0)
			throw new IllegalQueryException("SELECT query needs at least one projection!");
		
		for (String var : varNames)
			res.append(" ?").append(var);
		
		res.append(" WHERE { ").append(s);
		
		if (filterExpr!=null) {
			try {
				String filter = FilterUtils.toSparqlString(filterExpr);
				res.append("FILTER ").append(filter);
				evaluated = true;
			} catch (FilterConversionException e) {
				log.warn("Filter could not be evaluated remotely. " + e.getMessage());
			}
		}
	
		res.append(" }");
		
		return res.toString();		
	}
	
	/**
	 * Construct a SELECT query for the provided {@link ExclusiveGroup}. Note that bindings
	 * and filterExpr are applied whenever possible.
	 *  
	 * @param group
	 * 				the expression for the query
	 * @param bindings
	 * 				the bindings to be applied
	 * @param filterExpr
	 * 				a filter expression or null
	 * @param evaluated
	 * 				parameter can be used outside this method to check whether FILTER has been evaluated, false in beginning
	 * 
	 * @return
	 * @throws IllegalQueryException 
	 * 
	 */
	public static String selectQueryString( ExclusiveGroup group, BindingSet bindings, FilterValueExpr filterExpr, Boolean evaluated) throws IllegalQueryException  {
		
		StringBuilder sb = new StringBuilder();
		Set<String> varNames = new HashSet<String>();
		
		for (ExclusiveStatement s : group.getStatements()) {
			StatementAndProviderVars statementAndProviderVars = constructStatement(s, varNames, bindings);
			sb.append(statementAndProviderVars.getStatement().toString());
			varNames.addAll(statementAndProviderVars.getVars());
		}
		
		// project only relevant variables, i.e. do not bind local variables
		varNames = project(group, varNames);
		
		if (varNames.size()==0)
			throw new IllegalQueryException("SELECT query needs at least one projection!");		

		StringBuilder res = new StringBuilder();	
		res.append("SELECT  ");
			
		for (String var : varNames)
			res.append(" ?").append(var);
			
		
		res.append(" WHERE { ").append(sb);
		
		if (filterExpr!=null) {
			try {
				String filter = FilterUtils.toSparqlString(filterExpr);
				res.append("FILTER ").append(filter);
				evaluated = true;
			} catch (FilterConversionException e) {
				log.warn("Filter could not be evaluated remotely. " + e.getMessage());
			}
		}
		
		res.append(" }");
		
		return res.toString();
	}

	/**
	 * Transform the exclusive group into a ASK query string
	 * 
	 * @param group
	 * @param bindings
	 * @return
	 * @throws IllegalQueryException
	 */
	public static String askQueryString( ExclusiveGroup group, BindingSet bindings) {
		
		StringBuilder sb = new StringBuilder();
		Set<String> varNames = new HashSet<String>();
		
		for (ExclusiveStatement s : group.getStatements()) {
			StatementAndProviderVars statementAndProviderVars = constructStatement(s, varNames, bindings);
			sb.append(statementAndProviderVars.getStatement().toString());
			varNames.addAll(statementAndProviderVars.getVars());
		}
		
		StringBuilder res = new StringBuilder();	
		res.append("ASK { ").append(sb.toString()).append(" }");
		return res.toString();
	}

	/**
	 * Construct a SELECT query string for a bound union.
	 * 
	 * Pattern:
	 * 
	 * SELECT ?v_1 ?v_2 ?v_N WHERE { { ?v_1 p o } UNION { ?v_2 p o } UNION ... } 
	 * 
	 * Note that the filterExpr is not evaluated at the moment.
	 * 
	 * @param stmt
	 * @param unionBindings
	 * @param filterExpr
	 * @param evaluated
	 * 			parameter can be used outside this method to check whether FILTER has been evaluated, false in beginning
	 * 
	 * @return
	 */
	public static String selectQueryStringBoundUnion( StatementPattern stmt, List<BindingSet> unionBindings, FilterValueExpr filterExpr, Boolean evaluated) {
					
		Set<String> varNames = new HashSet<String>();
		
		StringBuilder unions = new StringBuilder();
		for (int i=0; i<unionBindings.size(); i++) {
			StatementAndProviderVars statementAndProviderVars = constructStatementId(stmt, Integer.toString(i), varNames, unionBindings.get(i));
			String s = statementAndProviderVars.getStatement().toString();
			varNames.addAll(statementAndProviderVars.getVars());
			if (i>0)
				unions.append(" UNION");
			unions.append(" { ").append(s).append(" }");
		}
		
		StringBuilder res = new StringBuilder();
		
		res.append("SELECT ");
		
		for (String var : varNames)
			res.append(" ?").append(var);
				
		res.append(" WHERE {");
		
		res.append( unions );
		
		// TODO evaluate filter expression remote
//		if (filterExpr!=null) {
//		
//		}
	
		res.append(" }");
		
		return res.toString();
	}	
	
	/**
	 * Creates a bound join subquery using the SPARQL 1.1 VALUES operator.
	 * 
	 * Example subquery:
	 * 
	 * <source>
	 * SELECT ?v ?__index WHERE {
	 *   ?s name ?v.
	 * } VALUES (?s ?__index) { (:s1 1) (:s2 2) ... (:sN N) }
	 * </source>
	 * 
	 * @param stmt
	 * @param unionBindings
	 * @param filterExpr
	 * @param evaluated
	 * 			parameter can be used outside this method to check whether FILTER has been evaluated, false in beginning
	 * 
	 * @return
	 * @see SparqlFederationEvalStrategyWithValues
	 * @see BoundJoinVALUESConversionIteration
	 * @since 3.0
	 */
	public static String selectQueryStringBoundJoinVALUES(StatementPattern stmt, List<BindingSet> unionBindings, FilterValueExpr filterExpr, Boolean evaluated) {
		
		Set<String> varNames = new LinkedHashSet<String>();	
		StringBuilder res = new StringBuilder();
		
		StatementAndProviderVars statementAndProviderVars = constructStatement(stmt, varNames, new EmptyBindingSet());
		String stmtPattern = statementAndProviderVars.getStatement().toString();
		varNames.addAll(statementAndProviderVars.getVars());

		res.append("SELECT ");
		
		for (String var : varNames)
			res.append(" ?").append(var);
				
		res.append(" ?").append(BoundJoinVALUESConversionIteration.INDEX_BINDING_NAME).append(" WHERE {");
		
		res.append( stmtPattern );
		
		// TODO evaluate filter expression remote
//		if (filterExpr!=null) {
//		
//		}
	
		res.append(" }");
		
		// add VALUES clause
		res.append(" VALUES (");
		
		// find relevant bindings		
		for (String var : varNames)
			res.append("?").append(var).append(" ");
		res.append(" ?__index) { ");
		
		int index=0;
		for (BindingSet b : unionBindings) {
			res.append("(");
			for (String var : varNames) {
				if (b.hasBinding(var))
					appendValue(res, b.getValue(var)).append(" ");
				else
					res.append("UNDEF ");
			}
			res.append("\"").append(index).append("\") ");
			index++;
		}
		res.append(" }");
		
		return res.toString();
	}
	
	/**
	 * Modifies the set of varNames to remove those variables that are local
	 * to the expression, i.e. those that do not have to be projected. Does
	 * not create a copy!
	 * @param expr
	 * @param varNames
	 * @return
	 */
	private static Set<String> project(StatementTupleExpr expr, Set<String> varNames) {
		varNames.removeAll(expr.getLocalVars());
		return varNames;
	}
	
	/**
	 * Construct a SELECT query for a grouped bound check.
	 * 
	 * Pattern:
	 * 
	 * SELECT  ?o_1 .. ?o_N WHERE { { s1 p1 ?o_1 FILTER ?o_1=o1 } UNION ... UNION { sN pN ?o_N FILTER ?o_N=oN }}
	 * 
	 * @param stmt
	 * @param unionBindings
	 * @return
	 */
	public static String selectQueryStringBoundCheck(StatementPattern stmt, List<BindingSet> unionBindings) {
		
		Set<String> varNames = new HashSet<String>();
		
		StringBuilder unions = new StringBuilder();
		for (int i=0; i<unionBindings.size(); i++) {
			StatementAndProviderVars statementAndProviderVars = constructStatementCheckId(stmt, i, varNames, unionBindings.get(i));
			String s = statementAndProviderVars.getStatement().toString();
			varNames.addAll(statementAndProviderVars.getVars());
			if (i>0)
				unions.append(" UNION");
			unions.append(" { ").append(s).append(" }");
		}
		
		StringBuilder res = new StringBuilder();

		res.append("SELECT ");
		
		for (String var : varNames)
			res.append(" ?").append(var);
				
		res.append(" WHERE {").append( unions ).append(" }");
		
		return res.toString();
	}	
	
	
	/**
	 * 
	 * SELECT ?v_0 ?v_1 WHERE {
	 * 		{ ?v_0 p1 o1 . }
	 * 		UNION
	 * 		{ ?v_1 p2 o1 . }
	 * }
	 * @param joinGroup
	 * @param bindings
	 * @return
	 */
	public static String selectQueryStringIndependentJoinGroup(IndependentJoinGroup joinGroup, BindingSet bindings) {
		
		Set<String> varNames = new HashSet<String>();
		
		StringBuilder unions = new StringBuilder();
		for (int i=0; i<joinGroup.getMemberCount(); i++) {
			StatementPattern stmt = (StatementPattern)joinGroup.getMembers().get(i);
			StatementAndProviderVars statementAndProviderVars = constructStatementId(stmt, Integer.toString(i), varNames, bindings);
			String s = statementAndProviderVars.getStatement().toString();
			varNames.addAll(statementAndProviderVars.getVars());
			if (i>0)
				unions.append(" UNION");
			unions.append(" { ").append(s).append(" }");
		}
		
		StringBuilder res = new StringBuilder();
		
		res.append("SELECT ");
		
		for (String var : varNames)
			res.append(" ?").append(var);
				
		res.append(" WHERE {");
		
		res.append( unions );
				
		res.append(" }");
		
		return res.toString();
	}
	
	/**
	 * Construct a select query representing a bound independent join group.
	 * 
	 * ?v_%stmt%_%bindingId$
	 * 
	 * SELECT ?v_0_0 ?v_0_1 ?v_1_0 ... 
	 * 	WHERE { 
	 * 		{ ?v_0#0 p o UNION ?v_0_1 p o UNION ... } 
	 * 		UNION 
	 * 		{ ?v_1_0 p o UNION ?v_1_1 p o UNION ... } 
	 *      UNION
	 *      ...
	 *  }
	 * @param joinGroup
	 * @param bindings
	 * @return
	 */
	public static String selectQueryStringIndependentJoinGroup(IndependentJoinGroup joinGroup, List<BindingSet> bindings) {
		
		Set<String> varNames = new HashSet<String>();
		
		StringBuilder outerUnion = new StringBuilder();
		for (int i=0; i<joinGroup.getMemberCount(); i++) {
			StatementAndProviderVars statementAndProviderVars = constructInnerUnion((StatementPattern) joinGroup.getMembers().get(i), i, varNames, bindings);
			String innerUnion = statementAndProviderVars.toString();
			varNames.addAll(statementAndProviderVars.getVars());
			if (i>0)
				outerUnion.append(" UNION");
			outerUnion.append(" { ").append(innerUnion).append("}");
		}
		
		
		StringBuilder res = new StringBuilder();
		
		res.append("SELECT ");
		
		for (String var : varNames)
			res.append(" ?").append(var);
				
		res.append(" WHERE {");
		
		res.append( outerUnion );
				
		res.append(" }");
		
		return res.toString();
	}
	
	
	protected static StatementAndProviderVars constructInnerUnion(StatementPattern stmt, int outerID, Set<String> varNames, List<BindingSet> bindings) {

		List<String> providerVars = new ArrayList<String>();
		
		StringBuilder innerUnion = new StringBuilder();
			
		for (int idx=0; idx<bindings.size(); idx++) {
			if (idx>0)
				innerUnion.append("UNION ");
			StatementAndProviderVars statementAndProviderVars = constructStatementId(stmt, outerID + "_" + idx, varNames, bindings.get(idx));
			innerUnion.append("{").append(statementAndProviderVars.getStatement().toString()).append("} ");
			providerVars.addAll(statementAndProviderVars.getVars());
		}
				
		return new StatementAndProviderVars(innerUnion, providerVars);
	}
	
	
	/**
	 * Construct a boolean ASK query for the provided statement.
	 * 
	 * @param stmt
	 * @param bindings
	 * @return
	 */
	public static String askQueryString( StatementPattern stmt, BindingSet bindings ) {
		
		Set<String> varNames = new HashSet<String>();

		StatementAndProviderVars statementAndProviderVars = constructStatement(stmt, varNames, bindings);

		String s = statementAndProviderVars.getStatement().toString();
		
		StringBuilder res = new StringBuilder();
		
		res.append("ASK {");
		res.append(s).append(" }");
		
		return res.toString();		
	}
	
	/**
	 * Construct a SELECT query for the provided statement with LIMIT 1. Such query
	 * can be used for source selection instead of ASK queries.
	 * 
	 * @param stmt
	 * @param bindings
	 * @return
	 */
	public static String selectQueryStringLimit1( StatementPattern stmt, BindingSet bindings ) {
		
		Set<String> varNames = new HashSet<String>();

		StatementAndProviderVars statementAndProviderVars = constructStatement(stmt, varNames, bindings);

		String s = statementAndProviderVars.getStatement().toString();
		
		StringBuilder res = new StringBuilder();
		
		res.append("SELECT * WHERE {");
		res.append(s).append(" } LIMIT 1");
		
		return res.toString();		
	}
	
	/**
	 * Construct a SELECT query for the provided {@link ExclusiveGroup} with LIMIT 1. Such query
	 * can be used for source selection instead of ASK queries.
	 *
	 * @param bindings
	 * @return
	 */
	public static String selectQueryStringLimit1( ExclusiveGroup group, BindingSet bindings ) {
		
		Set<String> varNames = new HashSet<String>();		
		StringBuilder res = new StringBuilder();
		
		res.append("SELECT * WHERE { ");

		for (ExclusiveStatement s : group.getStatements())
			res.append( constructStatement(s, varNames, bindings) );
		
		res.append(" } LIMIT 1");
		
		return res.toString();		
	}
	
	/**
	 * Construct the statement string, i.e. "s p o . " with bindings inserted wherever possible. Note that
	 * the relevant free variables are added to the varNames set for further evaluation.
	 * 
	 * @param stmt
	 * @param varNames
	 * @param bindings
	 * 
	 * @return
	 */
	protected static StatementAndProviderVars constructStatement(StatementPattern stmt, Set<String> varNames, BindingSet bindings) {
		StringBuilder sb = new StringBuilder();

		TriplePattern originalTriplePattern = getOriginalTriplePattern(stmt.getSubjectVar(), stmt.getPredicateVar(), stmt.getObjectVar());

		StatementAndProviderVars statementAndProviderVarsGraph = appendGraph(sb, originalTriplePattern);
		sb = statementAndProviderVarsGraph.getStatement();
		
		sb = appendVar(sb, stmt.getSubjectVar(), varNames, bindings).append(" ");
		sb = appendVar(sb, stmt.getPredicateVar(), varNames, bindings).append(" ");
		sb = appendVar(sb, stmt.getObjectVar(), varNames, bindings).append(" . ");

		StatementAndProviderVars statementAndProviderVarsService = appendServiceDescription(sb, originalTriplePattern, bindings);

		List<String> providerVars = new ArrayList<String>();
		providerVars.addAll(statementAndProviderVarsGraph.getVars());
		providerVars.addAll(statementAndProviderVarsService.getVars());

		return new StatementAndProviderVars(statementAndProviderVarsService.getStatement(), providerVars);
	}

	/**
	 * Construct the statement string, i.e. "s p o . " with bindings inserted wherever possible. Variables
	 * are renamed to "var_"+varId to identify query results in bound queries. Note that
	 * the free variables are also added to the varNames set for further evaluation.
	 * 
	 * @param stmt
	 * @param varNames
	 * @param bindings
	 * 
	 * @return
	 */
	protected static StatementAndProviderVars constructStatementId(StatementPattern stmt, String varID, Set<String> varNames, BindingSet bindings) {
		StringBuilder sb = new StringBuilder();

		TriplePattern originalTriplePattern = getOriginalTriplePattern(stmt.getSubjectVar(), stmt.getPredicateVar(), stmt.getObjectVar());

		StatementAndProviderVars statementAndProviderVarsGraph = appendGraph(sb, originalTriplePattern, "_" + varID);
		sb = statementAndProviderVarsGraph.getStatement();
		
		sb = appendVarId(sb, stmt.getSubjectVar(), varID, varNames, bindings).append(" ");
		sb = appendVarId(sb, stmt.getPredicateVar(), varID, varNames, bindings).append(" ");
		sb = appendVarId(sb, stmt.getObjectVar(), varID, varNames, bindings).append(" . ");

		StatementAndProviderVars statementAndProviderVarsService = appendServiceDescription(sb, originalTriplePattern, bindings, "_" + varID);

		List<String> providerVars = new ArrayList<String>();
		providerVars.addAll(statementAndProviderVarsGraph.getVars());
		providerVars.addAll(statementAndProviderVarsService.getVars());

		return new StatementAndProviderVars(statementAndProviderVarsService.getStatement(), providerVars);
	}

	/**
	 * Construct the statement string, i.e. "s p ?o_varID FILTER ?o_N=o ". This kind of statement
	 * pattern is necessary to later on identify available results.
	 * 
	 * @param stmt
	 * @param varID
	 * @param varNames
	 * @param bindings
	 * @return
	 */
	protected static StatementAndProviderVars constructStatementCheckId(StatementPattern stmt, int varID, Set<String> varNames, BindingSet bindings) {
		StringBuilder sb = new StringBuilder();

		String _varID = Integer.toString(varID);

		TriplePattern originalTriplePattern = getOriginalTriplePattern(stmt.getSubjectVar(), stmt.getPredicateVar(), stmt.getObjectVar());

		StatementAndProviderVars statementAndProviderVarsGraph = appendGraph(sb, originalTriplePattern, "_" + _varID);
		sb = statementAndProviderVarsGraph.getStatement();

		sb = appendVarId(sb, stmt.getSubjectVar(), _varID, varNames, bindings).append(" ");
		sb = appendVarId(sb, stmt.getPredicateVar(), _varID, varNames, bindings).append(" ");
		
		sb.append("?o_").append(_varID);
		varNames.add("o_" + _varID);
		
		String objValue;
		if (stmt.getObjectVar().hasValue()) {
			objValue = getValueString(stmt.getObjectVar().getValue());
		} else if (bindings.hasBinding(stmt.getObjectVar().getName())){
			objValue = getValueString(bindings.getBinding(stmt.getObjectVar().getName()).getValue());
		} else {
			// just to make sure that we see an error, will be deleted soon
			throw new RuntimeException("Unexpected.");
		}
		
		sb.append(" FILTER (?o_").append(_varID).append(" = ").append(objValue).append(" )");

		StatementAndProviderVars statementAndProviderVarsService = appendServiceDescription(sb, originalTriplePattern, bindings, "_" + _varID);

		List<String> providerVars = new ArrayList<String>();
		providerVars.addAll(statementAndProviderVarsGraph.getVars());
		providerVars.addAll(statementAndProviderVarsService.getVars());

		return new StatementAndProviderVars(statementAndProviderVarsService.getStatement(), providerVars);
	}
	
	/**
	 * Append the variable to the provided StringBuilder.
	 * 
	 * Cases:
	 *  1) unbound: check provided bindingset for possible match 
	 *   a) match found: append matching value
	 *   b) no match: append ?varName and add to varNames
	 *  2) bound: append value
	 *  
	 * @param sb
	 * @param var
	 * @param varNames
	 * @param bindings
	 * 
	 * @return
	 * 		the stringbuilder
	 */
	protected static StringBuilder appendVar(StringBuilder sb, Var var, Set<String> varNames, BindingSet bindings) {
		if (!var.hasValue()) {
			if (bindings.hasBinding(var.getName()))
				return appendValue(sb, bindings.getValue(var.getName()));
			varNames.add(var.getName());
			return sb.append("?").append(var.getName());
		}
		else
			return appendValue(sb, var.getValue());
	}
	
	/**
	 * Append the variable to the provided StringBuilder, however change name of variable
	 * by appending "_varId" to it.
	 * 
	 * Cases:
	 *  1) unbound: check provided bindingset for possible match 
	 *   a) match found: append matching value
	 *   b) no match: append ?varName_varId and add to varNames
	 *  2) bound: append value
	 *  
	 * @param sb
	 * @param var
	 * @param varNames
	 * @param bindings
	 * 
	 * @return
	 * 		the complemented string builder
	 */
	protected static StringBuilder appendVarId(StringBuilder sb, Var var, String varID, Set<String> varNames, BindingSet bindings) {
		if (!var.hasValue()) {
			if (bindings.hasBinding(var.getName()))
				return appendValue(sb, bindings.getValue(var.getName()));
			String newName = var.getName() + "_" + varID;
			varNames.add(newName);
			return sb.append("?").append(newName);
		}
		else
			return appendValue(sb, var.getValue());
	}
	
	/**
	 * Return the string representation of this value, see {@link #appendValue(StringBuilder, Value)} for
	 * details.
	 * 
	 * @param value
	 * 
	 * @return
	 */
	protected static String getValueString(Value value) {
		StringBuilder sb = new StringBuilder();
		appendValue(sb, value);
		return sb.toString();
	}
	
	/**
	 * Append a string representation of the value to the string builder.
	 * 
	 * 1. URI: <http://myUri>
	 * 2. Literal: "myLiteral"^^<dataType>
	 * 
	 * @param sb
	 * @param value
	 * @return
	 */
	protected static StringBuilder appendValue(StringBuilder sb, Value value) {

		if (value instanceof URI)
			return appendURI(sb, (URI)value);
		if (value instanceof Literal)
			return appendLiteral(sb, (Literal)value);
		if (value instanceof BNode)
			return appendBNode(sb, (BNode)value);
		throw new RuntimeException("Type not supported: " + value.getClass().getCanonicalName());
	}
	
	/**
	 * Append the uri to the stringbuilder, i.e. <uri.stringValue>.
	 * 
	 * @param sb
	 * @param uri
	 * @return
	 */
	protected static StringBuilder appendURI(StringBuilder sb, URI uri) {
		sb.append("<").append(uri.stringValue()).append(">");
		return sb;
	}
	
	/**
	 * Append a dummy string (see {@link #BNODE_URI}) to represent the BNode.
	 * 
	 * Note: currently it is not possible to retrieve values for a BNode via
	 * SPARQL, hence we use a dummy BNode which does not produce any results.
	 * A warning is printed to debug.
	 * 
	 * @param sb
	 * @return
	 */
	protected static StringBuilder appendBNode(StringBuilder sb, BNode bNode) {
		log.debug("Cannot express BNodes in SPARQl: Bnode " + bNode.toString() + " is replaced with " + BNODE_URI.stringValue());
		// TODO think how this can be done in queries, for now we just append a
		// dummy URI which does not produce any results
		return appendURI(sb, BNODE_URI);
	}
	
	
	/**
	 * Append the literal to the stringbuilder.
	 * 
	 * @param sb
	 * @param lit
	 * @return
	 */
	protected static StringBuilder appendLiteral(StringBuilder sb, Literal lit) {
		sb.append('"');
		sb.append(lit.getLabel().replace("\"", "\\\""));
		sb.append('"');

		if (lit.getLanguage() != null) {
			sb.append('@');
			sb.append(lit.getLanguage());
		}

		if (lit.getDatatype() != null) {
			sb.append("^^<");
			sb.append(lit.getDatatype().stringValue());
			sb.append('>');
		}
		return sb;
	}	
	
	
	
	/**
	 * load the queries from a queries file located at the specified path.
	 * 
	 * Expected format:
	 *  - Queries are SPARQL queries in String format
	 *  - queries are allowed to span several lines
	 *  - a query is interpreted to be finished if an empty line occurs
	 *  
	 *  Ex:
	 *  
	 *  QUERY1 ...
	 *   Q1 cntd
	 *   
	 *  QUERY2
	 *
	 * @return
	 * 			a list of queries for the query type
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static List<String> loadQueries(String queryFile) throws FileNotFoundException, IOException {
		ArrayList<String> res = new ArrayList<String>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(queryFile));
			String tmp;
			String tmpQuery = "";
			while ((tmp = in.readLine()) != null){
				if (tmp.equals("")){
					if (!tmpQuery.equals(""))
						res.add(tmpQuery);
					tmpQuery = "";
				}
				else {
					tmpQuery = tmpQuery + tmp;
				}
			}
			if (!tmpQuery.equals(""))
				res.add(tmpQuery);
			return res;
		} finally {
			if (in!=null)
				in.close();
		}
		
	}

	private static StringBuilder appendVar(StringBuilder sb, Var var) {
		if (!var.hasValue()) {
			return sb.append("?").append(var.getName());
		}
		else
			return appendValue(sb, var.getValue());
	}

	private static TriplePattern getOriginalTriplePattern(Var subjectVar, Var predicateVar, Var objectVar) {
		StringBuilder sb = new StringBuilder();

		sb.append("SELECT * WHERE {");

		sb = appendVar(sb, subjectVar).append(" ");
		sb = appendVar(sb, predicateVar).append(" ");
		sb = appendVar(sb, objectVar).append(" . ");

		sb.append("}");

		List<TriplePattern> triplePatterns = Parser.getTriplePatterns(sb.toString());

		TriplePattern triplePattern = null;

		if (triplePatterns != null && triplePatterns.size() > 0) {
			triplePattern = triplePatterns.get(0);
		}

		return triplePattern;
	}

	private static StatementAndProviderVars appendGraph(StringBuilder sb, TriplePattern triplePattern) {

		return appendGraph(sb, triplePattern, "");
	}

	private static StatementAndProviderVars appendGraph(StringBuilder sb, TriplePattern triplePattern, String suffix) {
		ServiceDescription serviceDescription = FederationManager.getInstance().getServiceDescription();

		String graphVarName = serviceDescription.getGraphVariable(triplePattern.getTriple()) + suffix;

		sb.append("GRAPH ?").append(graphVarName).append(" {");

		return new StatementAndProviderVars(sb, new ArrayList<String>());
	}

	private static StatementAndProviderVars appendServiceDescription(StringBuilder sb, TriplePattern triplePattern, BindingSet bindings) {
		return appendServiceDescription(sb, triplePattern, bindings, "");
	}

	private static StatementAndProviderVars appendServiceDescription(StringBuilder sb, TriplePattern triplePattern, BindingSet bindings, String suffix) {
		ServiceDescription serviceDescription = FederationManager.getInstance().getServiceDescription();

		int tripleIndex = serviceDescription.getTripleIndex(triplePattern.getTriple());

		sb.append(" }");

		String productQuery = "SELECT * WHERE { ?product" + tripleIndex + " <http://www.example.com/contains> ?graph" + tripleIndex + " . }";

		ServiceQueryTransform serviceQueryTransform = new ServiceQueryTransform(bindings, suffix);

		Query query = QueryFactory.create(productQuery);
		Op op = Algebra.compile(query);
		op = Transformer.transform(serviceQueryTransform, op);
		query = OpAsQuery.asQuery(op);

		sb.append(query.getQueryPattern().toString());

		appendValues(sb, bindings, suffix, serviceDescription);

		List<String> productVars = new ArrayList<String>();
		for (int i = 1; i <= serviceDescription.getNumberOfTriplePatterns(); ++i) {
			productVars.add("product" + i + suffix);
		}

		return new StatementAndProviderVars(sb, productVars);
	}

	private static void appendValues(StringBuilder sb, BindingSet bindings,  String suffix, ServiceDescription serviceDescription) {
		boolean valueFound = false;
		for (int i = 1; i <= serviceDescription.getNumberOfTriplePatterns(); ++i) {
			if (bindings.hasBinding("product" + i)) {
				if (!valueFound) {
					valueFound = true;
					sb.append(" VALUES ( ");
				}
				sb.append("?product" + i + suffix + " ");
			}
		}
		if (valueFound) {
			sb.append(") { ( ");
			for (int i = 1; i <= serviceDescription.getNumberOfTriplePatterns(); ++i) {
				if (bindings.hasBinding("product" + i)) {
					String value = bindings.getValue("product" + i).stringValue();
					sb.append("<" + value + "> ");
				}
			}
			sb.append(") } ");
		}
	}
}
