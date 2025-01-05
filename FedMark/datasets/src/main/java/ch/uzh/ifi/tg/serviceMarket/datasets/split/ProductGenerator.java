package ch.uzh.ifi.tg.serviceMarket.datasets.split;

import org.apache.commons.cli.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Tobias Grubenmann on 31.05.17.
 */
public class ProductGenerator {
    public static void main(String args[]) throws ParseException, IOException {

        Options options = new Options();
        options.addOption("i", "input", true, "input file");
        options.addOption("o", "output", true, "Output path");
        options.addOption("g", "graph", true, "Name of the graph");
        options.addOption("s", "suffix", true, "Suffix for output file name");
        options.addOption("c", "count", true, "start index for count");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        String fileName = cmd.getOptionValue("i");
        String graphName = cmd.getOptionValue("g");
        String outputPath = cmd.getOptionValue("o");
        String suffix = "";
        if (cmd.hasOption("s")) {
            suffix = "_" + cmd.getOptionValue("s");
        }

        Model model = null;
        PrintWriter outputStream = new PrintWriter(new FileWriter(String.valueOf(Paths.get(outputPath, graphName + suffix + ".trig"))));

        int count = 0;
        if (cmd.hasOption("c")) {
            count = Integer.parseInt(cmd.getOptionValue("c"));
        }

        /*BufferedReader inputStream = new BufferedReader(new FileReader(fileName));

        String line;
        while ((line = inputStream.readLine()) != null) {
            String productName = graphName + "_" + count;
            outputStream.println("{ <http://www.example.com/product/" + productName + "> <http://www.example.com/contains> <http://www.example.com/graph/" + productName + "> . }");
            outputStream.println("<http://www.example.com/graph/" + productName + "> {");
            outputStream.println(line);
            outputStream.println("}");

            ++count;
        }

        System.out.println("last count: " + count);*/

        try {
            model = RDFDataMgr.loadModel(fileName);

            StmtIterator iter = model.listStatements();
            while (iter.hasNext()) {
                String productName = graphName + "_" + count;
                outputStream.println("{ <http://www.example.com/product/" + productName + "> <http://www.example.com/contains> <http://www.example.com/graph/" + productName + "> . }");
                outputStream.println("<http://www.example.com/graph/" + productName + "> {");
                Model singleStmtModel = ModelFactory.createDefaultModel();
                singleStmtModel.add(iter.next());
                singleStmtModel.write(outputStream, "TURTLE");
                singleStmtModel.close();
                outputStream.println("}");

                ++count;
            }

            System.out.println("last count: " + count);

        } finally {
            if (model != null) {
                model.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }
}
