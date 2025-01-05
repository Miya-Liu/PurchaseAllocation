package ch.uzh.ifi.tg.serviceMarket.evaluation;

import ilog.concert.IloException;
import org.apache.commons.cli.ParseException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultParseException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Tobias Grubenmann on 03.07.17.
 */
public class GreedyIntegerEvaluation {

    public static void main(String[] args) throws ParseException, IOException, IloException, QueryEvaluationException, QueryResultHandlerException, QueryResultParseException {

        PrintWriter writer = new PrintWriter("output/FedMarkJournalEvaluation.csv", "UTF-8");

        List<Integer> modes = Arrays.asList(5);
        //List<Float> values = Arrays.asList(1.0f, 0.45f, 2.75f);
        List<Float> values = Arrays.asList(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
        List<Float> prices = new ArrayList<>();

        List<Integer> distribution = Arrays.asList(
                101, // CD1
                9054, // CD2
                4527, // CD3
                9054, // CD4
                4527, // CD5
                823, // CD6
                9054, // CD7
                29, // LD1
                49, // LD2
                56, // LD3
                181, // LD4
                905, // LD5
                823, // LD6
                9, // LD7
                412, // LD8
                9054, // LD9
                3018, // LD10
                38, // LD11
                8, // LS1
                27, // LS2
                1, // LS3
                3018, // LS4
                23, // LS5
                323, // LS6
                63); // LS7

        for (int i = 0; i <= 805; ++i) {
            prices.add((float)(Math.pow(10, i/400.0) - 1));
        }

        float budget = Float.MAX_VALUE;

        for (int mode : modes) {
            writer.write("mode: " + mode + "\n,");
            for (float price : prices) {
                writer.write(price + ",");
            }

            writer.write("\n");

            // use Integer Allocation Rule
            writer.write("revenue: integer,");
            for (float price : prices) {
                float[] result = AllocationSimulator.getFedMarkRevenueAndWelfare(price, 1, values.get(mode), budget, mode, distribution, false);
                writer.write(result[0] + ",");
                writer.flush();
            }
            writer.write("\n");
            writer.flush();

            // use Greedy Allocation Rule
            writer.write("revenue: greedy,");
            for (float price : prices) {
                float[] result = AllocationSimulator.getFedMarkRevenueAndWelfare(price, -1,  values.get(mode), budget, mode, distribution, true);
                writer.write(result[0] + ",");
                writer.flush();
            }
            writer.write("\n");
            writer.flush();

            }

        writer.close();

    }
}
