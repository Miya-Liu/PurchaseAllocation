package ch.uzh.ifi.tg.serviceMarket.evaluation;

import ch.uzh.ifi.tg.serviceMarket.market.ValueFunction;
import ch.uzh.ifi.tg.serviceMarket.solver.IntegerSolver;
import ch.uzh.ifi.tg.serviceMarket.solver.Solver;
import ch.uzh.ifi.tg.serviceMarket.market.DataBundleFactory;
import ch.uzh.ifi.tg.serviceMarket.market.RowValueFunction;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.parser.TriplePattern;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.provider.ServiceDescription;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.request.DataAndServiceQuery;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.request.Request;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataBundle;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataProduct;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.transform.InsertVariablesTransform;
import ch.uzh.ifi.tg.serviceMarket.fedx.Config;
import ch.uzh.ifi.tg.serviceMarket.fedx.FedXFactory;
import ch.uzh.ifi.tg.serviceMarket.fedx.FederationManager;
import ch.uzh.ifi.tg.serviceMarket.fedx.QueryManager;
import ilog.concert.IloException;
import org.apache.commons.cli.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.Transformer;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;

import java.io.PrintWriter;
import java.util.*;

/**
 * Created by Tobias Grubenmann on 23/09/16.
 */
public class Evaluation {

    public static void main(String args[]) throws Exception {

        Options options = new Options();
        options.addOption("i", "input", true, "input file");
        options.addOption("s", "servers", true, "servers file");
        options.addOption("n", true, "number of servers");
        options.addOption("o", "output", true, "output file");
        options.addOption("x", "noGraph", false, "run query without graph");
        options.addOption("t", "iter", true, "number of iterations");
        options.addOption("m", "model", true, "model for service data");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        String inputFileName = cmd.getOptionValue("i");
        String serversFileName = cmd.getOptionValue("s");
        String outputFileName = cmd.getOptionValue("o");
        boolean noGraphQuery = cmd.hasOption("x");
        ValueFunction valueFunction = null;
        //noGraphQuery = false;

        int iterations = Integer.parseInt(cmd.getOptionValue("t"));

        List<Request> requests = XMLParser.parseRequest(inputFileName);

        List<String> hosts = XMLParser.parseServers(serversFileName);

        if (cmd.hasOption("n")) {
            int numberOfServers = Integer.parseInt(cmd.getOptionValue("n"));
            hosts = hosts.subList(0, numberOfServers);
        }

        String modelFile = cmd.getOptionValue("m");
        Model serviceModel = RDFDataMgr.loadModel(modelFile);

        PrintWriter writer = new PrintWriter(outputFileName, "UTF-8");

        writer.println("Query,Query Time [ms],Allocation Time [ms]");

        for (Request request : requests) {

            for (DataAndServiceQuery dataAndServiceQuery : request.getQueries()) {

                runMarket(writer, dataAndServiceQuery, hosts, noGraphQuery, iterations, serviceModel, valueFunction);

            }

        }

        System.out.println("Done.");

        writer.close();

        System.exit(0);
    }

    public static void runMarket(PrintWriter writer, DataAndServiceQuery dataAndServiceQuery, List<String> hosts,
                                 boolean noGraphQuery, int iterations, Model serviceModel, ValueFunction valueFunction)
            throws Exception {

        long startTime = 0;
        double allocationTime = 0;
        double queryTime = 0;

        List<BindingSet> result = null;

        for (int i = 0; i < iterations; ++i) {

            startTime = System.currentTimeMillis();

            if (noGraphQuery) {
                result = runQuery(dataAndServiceQuery, hosts);
            } else {
                result = runGraphQuery(dataAndServiceQuery, hosts, serviceModel);
            }

            queryTime += System.currentTimeMillis() - startTime;
        }

        queryTime = queryTime * 1.0 / iterations;

        System.out.println(dataAndServiceQuery.getId());
        System.out.println("Query Time: " + queryTime + "ms");
        System.out.println("Count: " + result.size());

        List<DataBundle> allocation = new ArrayList<DataBundle>();

        List<TriplePattern> triplePatterns = ch.uzh.ifi.tg.serviceMarket.queryUtils.parser.Parser.getTriplePatterns(dataAndServiceQuery.getDataQuery());

        int numberOfTriplePatterns = triplePatterns.size();

        for (int i = 0; i < iterations; ++i) {
            startTime = System.currentTimeMillis();
            if (!noGraphQuery) {
                allocation = runAllocation(result, numberOfTriplePatterns, dataAndServiceQuery.getValuePerRow(), valueFunction, dataAndServiceQuery.getBudget());
            }
            allocationTime += System.currentTimeMillis() - startTime;
        }

        allocationTime = allocationTime * 1.0 / iterations;

        System.out.println("Allocation Time: " + allocationTime + "ms");

        Set<DataProduct> allocatedProducts = new HashSet<DataProduct>();

        for (DataBundle dataBundle : allocation) {
            System.out.println();
            System.out.println("\nResult:\n");
            for (BindingSet binding : dataBundle.getBindings()) {
                System.out.println(binding);
            }
            System.out.println();
        }

        float totalCost = 0;
        System.out.print("Allocated Products:");
        for (DataProduct dataProduct : allocatedProducts) {
            System.out.print(" " + dataProduct.getIdentifier());
            totalCost += dataProduct.getCost();
        }
        System.out.println();
        System.out.println("Total cost: " + totalCost);
        System.out.println("Budget: " + dataAndServiceQuery.getBudget());
        System.out.println();

        writer.print(dataAndServiceQuery.getId() + ",");
        writer.print(queryTime + ",");
        writer.print(allocationTime);
        writer.println();
    }

    public static List<BindingSet> runGraphQuery(DataAndServiceQuery dataAndServiceQuery, List<String> hosts, Model serviceModel) throws Exception {
        String queryString = dataAndServiceQuery.getDataQuery();
        String servicePattern = dataAndServiceQuery.getServicePattern();

        List<TriplePattern> triplePatterns = ch.uzh.ifi.tg.serviceMarket.queryUtils.parser.Parser.getTriplePatterns(queryString);

        ServiceDescription serviceDescription = new ServiceDescription(triplePatterns, servicePattern, serviceModel);

        Query jenaQuery = QueryFactory.create(queryString);
        Op op = Algebra.compile(jenaQuery);
        op = Transformer.transform(new InsertVariablesTransform(triplePatterns.size()), op);
        jenaQuery = OpAsQuery.asQuery(op);
        queryString = jenaQuery.toString();

        Config.initialize();
        FedXFactory.initializeSparqlFederation(hosts, serviceDescription);

        TupleQuery query = QueryManager.prepareTupleQuery(queryString);
        System.out.println(query);
        TupleQueryResult resultIterator = query.evaluate();

        List<BindingSet> result = new ArrayList<BindingSet>();

        while (resultIterator.hasNext()) {
            result.add(resultIterator.next());
        }

        FederationManager.getInstance().shutDown();

        return result;
    }

    public static List<DataBundle> runAllocation(List<BindingSet> bindings, int numberOfTriples, float valuePerRow, ValueFunction valueFunction, float budget) throws IloException {
        List<DataBundle> dataBundles =  DataBundleFactory.createDataBundles(bindings, numberOfTriples, new RowValueFunction(valuePerRow), new RandomPriceManager(0));
        Solver solver = new IntegerSolver();
        solver.addBundles(dataBundles, valueFunction);

        return solver.solve(budget);
    }

    public static List<BindingSet> runQuery(DataAndServiceQuery dataAndServiceQuery, List<String> hosts) throws Exception {
        String queryString = dataAndServiceQuery.getDataQuery();

        com.fluidops.fedx.Config.initialize();
        com.fluidops.fedx.FedXFactory.initializeSparqlFederation(hosts);

        TupleQuery query = com.fluidops.fedx.QueryManager.prepareTupleQuery(queryString);
        TupleQueryResult resultIterator = query.evaluate();

        List<BindingSet> result = new ArrayList<BindingSet>();

        while (resultIterator.hasNext()) {
            result.add(resultIterator.next());
        }

        com.fluidops.fedx.FederationManager.getInstance().shutDown();

        return result;
    }

}
