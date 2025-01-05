package ch.uzh.ifi.tg.serviceMarket.evaluation;

import ch.uzh.ifi.tg.serviceMarket.queryUtils.request.DataAndServiceQuery;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.request.Request;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.openrdf.model.Value;
import org.openrdf.model.impl.SimpleValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryResultHandlerException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.impl.ListBindingSet;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;

/**
 * Created by Tobias Grubenmann on 21.08.17.
 */
public class ArtificialResultGenerator {

    public static void main(String args[]) throws Exception {

        Options options = new Options();
        options.addOption("o", "output", true, "output directory");
        options.addOption("n", "number", true, "number of solutions");
        options.addOption("t", "triples", true, "triples per solution");
        options.addOption("r", "ratio", true, "ratio between available and required triples");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        String outputDirectory = cmd.getOptionValue("o");
        int numberOfSolutions = Integer.parseInt(cmd.getOptionValue("n"));
        int triplesPerSolution = Integer.parseInt(cmd.getOptionValue("t"));
        float ratio = Float.parseFloat(cmd.getOptionValue("r"));

        generateSolutions(outputDirectory, numberOfSolutions, triplesPerSolution, ratio);

        System.out.println("Done.");
    }


    public static void generateSolutions(String outputDirectory, int numberOfSolutions, int triplesPerSolution, float ratio) throws FileNotFoundException, QueryResultHandlerException {

        Random random = new Random();

        int triples = (int)(numberOfSolutions * triplesPerSolution * ratio);

        SPARQLResultsXMLWriter writer = new SPARQLResultsXMLWriter(new FileOutputStream(new File(outputDirectory + "/artificial_" +  ratio + ".xml")));

        writer.startDocument();

        List<String> bindingNames = new ArrayList<>();
        for (int i=1; i <= triplesPerSolution; ++i) {
            bindingNames.add("product" + i);
        }
        bindingNames.add("a");

        writer.startQueryResult(bindingNames);

        List<Integer> productIndices = new ArrayList<>(numberOfSolutions * triplesPerSolution);
        for (int i = 0; i < numberOfSolutions * triplesPerSolution; ++i) {
            productIndices.add(i % triples);
        }
        Collections.shuffle(productIndices);

        for (int i = 0; i < numberOfSolutions; ++i) {

            List<Value> values = new ArrayList<>();

            for (int j = 0; j < triplesPerSolution; ++j) {

                values.add(SimpleValueFactory.getInstance().createIRI("http://www.example.com/product/product_" + productIndices.get(i * triplesPerSolution + j)));
            }

            values.add(SimpleValueFactory.getInstance().createIRI("http://www.example.com/a" + random.nextDouble()));

            writer.handleSolution(new ListBindingSet(bindingNames, values));

        }

        writer.endQueryResult();
    }
}
