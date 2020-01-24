package ch.uzh.ifi.tg.serviceMarket.evaluation;

import ch.uzh.ifi.tg.serviceMarket.fedx.Config;
import ch.uzh.ifi.tg.serviceMarket.fedx.FedXFactory;
import ch.uzh.ifi.tg.serviceMarket.fedx.FederationManager;
import ch.uzh.ifi.tg.serviceMarket.fedx.QueryManager;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.parser.TriplePattern;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.provider.ServiceDescription;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.request.DataAndServiceQuery;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.request.Request;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.transform.InsertVariablesTransform;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.Transformer;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Tobias Grubenmann on 31.05.17.
 */
public class GenerateSolutions {

    public static void main(String args[]) throws Exception {

        Options options = new Options();
        options.addOption("i", "input", true, "input file");
        options.addOption("s", "servers", true, "servers file");
        options.addOption("o", "output", true, "output directory");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        String inputFileName = cmd.getOptionValue("i");
        String serversFileName = cmd.getOptionValue("s");
        String outputDirectory = cmd.getOptionValue("o");

        List<Request> requests = XMLParser.parseRequest(inputFileName);

        List<String> hosts = XMLParser.parseServers(serversFileName);

        for (Request request : requests) {

            for (DataAndServiceQuery dataAndServiceQuery : request.getQueries()) {

                runMarket(outputDirectory, dataAndServiceQuery, hosts);

            }

        }

        System.out.println("Done.");

        System.exit(0);
    }

    public static void runMarket(String outputDirectory, DataAndServiceQuery dataAndServiceQuery, List<String> hosts) throws Exception {

        runGraphQuery(outputDirectory, dataAndServiceQuery, hosts);

    }

    public static void runGraphQuery(String outputDirectory, DataAndServiceQuery dataAndServiceQuery, List<String> hosts) throws Exception {
        String queryString = dataAndServiceQuery.getDataQuery();
        String servicePattern = dataAndServiceQuery.getServicePattern();

        List<TriplePattern> triplePatterns = ch.uzh.ifi.tg.serviceMarket.queryUtils.parser.Parser.getTriplePatterns(queryString);

        ServiceDescription serviceDescription = new ServiceDescription(triplePatterns, servicePattern, null);

        Query jenaQuery = QueryFactory.create(queryString);
        Op op = Algebra.compile(jenaQuery);
        op = Transformer.transform(new InsertVariablesTransform(triplePatterns.size()), op);
        jenaQuery = OpAsQuery.asQuery(op);
        queryString = jenaQuery.toString();

        Config.initialize();
        FedXFactory.initializeSparqlFederation(hosts, serviceDescription);

        TupleQuery query = QueryManager.prepareTupleQuery(queryString);
        TupleQueryResult resultIterator = query.evaluate();

        SPARQLResultsXMLWriter writer = new SPARQLResultsXMLWriter(new FileOutputStream(new File(outputDirectory + "/" + dataAndServiceQuery.getId() + ".xml")));

        writer.startDocument();

        int numberOfTriplePatterns = triplePatterns.size();

        BindingSet bindings = resultIterator.next();


        Set<String> bindingNames = new HashSet<String>(bindings.getBindingNames());

        for (int i=1; i <= numberOfTriplePatterns; ++i) {
            bindingNames.add("product" + i);
        }

        writer.startQueryResult(new ArrayList<String>(bindingNames));


        writer.handleSolution(bindings);

        while (resultIterator.hasNext()) {
            bindings = resultIterator.next();
            //writer.startQueryResult(new ArrayList<String>(bindings.getBindingNames()));
            writer.handleSolution(bindings);

        }

        writer.endQueryResult();

        FederationManager.getInstance().shutDown();


    }

}
