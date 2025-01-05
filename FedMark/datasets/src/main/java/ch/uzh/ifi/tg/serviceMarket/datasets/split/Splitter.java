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
 * Created by Tobias Grubenmann on 20/09/16.
 */
public class Splitter {
    public static void main(String args[]) throws ParseException, IOException {

        Options options = new Options();
        options.addOption("i", "input", true, "input file" );
        options.addOption("n", true, "Number of splits" );
        options.addOption("o", "output", true, "Output path" );
        options.addOption("g", "graph", true, "Name of the graph" );
        options.addOption("s", "suffix", true, "Suffix for output file name" );
        options.addOption("m", "meta", false, "Add meta data.");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse( options, args);

        String fileName = cmd.getOptionValue("i");
        int numberOfSplits = new Integer(cmd.getOptionValue("n"));
        String graphName = cmd.getOptionValue("g");
        String outputPath = cmd.getOptionValue("o");
        String suffix = "";
        if (cmd.hasOption("s")) {
            suffix = "_" + cmd.getOptionValue("s");
        }

        Model model = null;
        List<PrintWriter> outputStreams = new ArrayList<PrintWriter>();

        Random random = new Random();

        try {
            model = RDFDataMgr.loadModel(fileName);
            for (int i = 0; i < numberOfSplits; ++i) {
                String graph = graphName + i;
                outputStreams.add(new PrintWriter(new FileWriter(String.valueOf(Paths.get(outputPath, graph + suffix + ".trig")))));
                if (cmd.hasOption("m")) {
                    outputStreams.get(i).println("{ <http://www.example.com/provider/" + graph + "> <http://www.example.com/contains> <http://www.example.com/graph/" + graph + "> . }");
                }
                outputStreams.get(i).println("<http://www.example.com/graph/" + graph + "> {");
            }

            // random split
            StmtIterator iter = model.listStatements();
            while (iter.hasNext()) {
                int index = random.nextInt(numberOfSplits);
                Model singleStmtModel = ModelFactory.createDefaultModel();
                singleStmtModel.add(iter.next());
                singleStmtModel.write(outputStreams.get(index), "TURTLE");
            }

            for (int i = 0; i < numberOfSplits; ++i) {
                outputStreams.get(i).println("}");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (model != null) {
                model.close();
            }
            for (PrintWriter outputStream : outputStreams) {
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        }

    }
}
