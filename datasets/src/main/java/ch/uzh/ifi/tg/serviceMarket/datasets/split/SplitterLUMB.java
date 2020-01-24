package ch.uzh.ifi.tg.serviceMarket.datasets.split;

import org.apache.commons.cli.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Tobias Grubenmann on 20/09/16.
 */
public class SplitterLUMB {
    public static void main(String args[]) throws ParseException, IOException {

        for (int uniID = 0; uniID < 50; ++uniID) {

            Model model = ModelFactory.createDefaultModel();

            // Load departments
            for (int deptId = 0; deptId < 15; ++deptId) {

                String fileName = "/Users/tobiasgrubenmann/Documents/RDFData/LUBM/50/University0/University0_" + deptId + ".owl";

                model.add(RDFDataMgr.loadModel(fileName));
            }

            String graph = "lubm" + uniID;
            PrintWriter outputStream = new PrintWriter(new FileWriter("/Users/tobiasgrubenmann/Documents/RDFData/LUBM/50/" + graph + ".trig"));
            outputStream.println("<http://www.example.com/provider/lubm" + uniID + "> <http://www.example.com/contains> <http://www.example.com/graph/" + graph + "> .");
            outputStream.println("<http://www.example.com/graph/" + graph + "> {");

            model.write(outputStream, "NT");

            model.close();

            outputStream.println("}");

            outputStream.close();
        }

    }
}
