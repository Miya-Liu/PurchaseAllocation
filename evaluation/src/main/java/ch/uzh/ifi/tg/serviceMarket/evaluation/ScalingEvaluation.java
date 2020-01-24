package ch.uzh.ifi.tg.serviceMarket.evaluation;

import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataProduct;
import ch.uzh.ifi.tg.serviceMarket.solver.*;
import ch.uzh.ifi.tg.serviceMarket.market.*;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataBundle;
import ilog.concert.IloException;
import org.apache.commons.cli.*;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.SimpleValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryResultHandlerException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.algebra.In;
import org.openrdf.query.impl.ListBindingSet;
import org.openrdf.query.impl.TupleQueryResultBuilder;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLParser;

import java.io.*;
import java.util.*;

/**
 * Created by Tobias Grubenmann on 07.06.17.
 */
public class ScalingEvaluation {
    public static final int MAX_TRIPLEPATTERNS_PER_QUERY = 10;
    public static final int TRIPLES_PER_RANDOM = 5;
    public static final float RATIO_SCALE = 100;

    private List<InputStream> inputStreams;
    private List<List<BindingSet>> results;
    private Map<Integer, float[]> budgetList = new HashMap<>();
    private Map<Integer, Integer> xPoint = new HashMap<>();

    private PriceManager priceManager;

    private float priceCoefficient;
    private float valueCoefficient;
    private float budgetCoefficient;
    private int index;

    private boolean useGreedy;
    private int limit;
    private Float ratio;

    private PrintWriter writer;

    public ScalingEvaluation(float priceCoefficient, float valueCoefficient, float budgetCoefficient, int index, PrintWriter writer, int steps, boolean useGreedy, int limit, Float ratio) {
        this.priceCoefficient = priceCoefficient;
        this.valueCoefficient = valueCoefficient;
        this.budgetCoefficient = budgetCoefficient;
        this.index = index;
        this.writer = writer;
        this.useGreedy = useGreedy;
        this.limit = limit;
        this.ratio = ratio;
    }

    public static void main(String[] args) throws ParseException, IOException, IloException, QueryEvaluationException, QueryResultHandlerException, QueryResultParseException {
        Options options = new Options();
        options.addOption("p", "price", true, "price per data product");
        options.addOption("v", "value", true, "value per tripe");
        options.addOption("b", "budgetCoefficient", true, "budgetCoefficient coefficient");
        options.addOption("i", "index", true, "query index");
        options.addOption("o", "output", true, "output path");
        options.addOption("g", "greedy", false, "if true, uses greedy rule");
        options.addOption("l", "limit", true, "start limit");
        options.addOption("r", "ratio", true, "ratio of unique triples");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        float priceCoefficient = Float.parseFloat(cmd.getOptionValue("p"));
        float valuePerSolution = Float.parseFloat(cmd.getOptionValue("v"));
        float budget = Float.parseFloat(cmd.getOptionValue("b"));
        String outputPath = cmd.getOptionValue("o");

        int index = 0;
        if (cmd.hasOption("i")) {
            index = Integer.parseInt(cmd.getOptionValue("i"));
        }
        int steps = 100;
        if (cmd.hasOption("s")) {
            steps = Integer.parseInt(cmd.getOptionValue("s"));
        }
        int limit = 0;
        if (cmd.hasOption("l")) {
            limit = Integer.parseInt(cmd.getOptionValue("l"));
        }

        Float ratio = null;
        if (cmd.hasOption("r")) {
            ratio = Float.parseFloat(cmd.getOptionValue("r")) / RATIO_SCALE;
        }

        boolean useGreedy = cmd.hasOption("g");

        PrintWriter writer = new PrintWriter(outputPath, "UTF-8");

        //for (int i = 2; i < 26; i++) {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>   Start   " );
        runRuntimeEvaluation(priceCoefficient, valuePerSolution, budget, index, writer, steps, useGreedy, limit, ratio);
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>   End   " );
        //}

        writer.close();
    }
    
    public static void runRuntimeEvaluation(float priceCoefficient, float valuePerSolution, float budget, int index, PrintWriter writer, int steps, boolean useGreedy, int startLimit, Float ratio) throws QueryResultParseException, QueryResultHandlerException, QueryEvaluationException, IOException, IloException {
        ScalingEvaluation allocationSimulator = new ScalingEvaluation(priceCoefficient, valuePerSolution, budget, index, writer, steps, useGreedy, startLimit, ratio);

        allocationSimulator.parseResults();
        //allocationSimulator.runAllocation();

        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>   flat   " );
        for (int i : allocationSimulator.budgetList.keySet()){
            allocationSimulator.index = i;
            System.out.println("************************   " + allocationSimulator.index + "   ****************************" );
            allocationSimulator.runAllocation();
        }
    }

    public void parseResults() throws IOException, QueryResultParseException, QueryResultHandlerException, QueryEvaluationException {

        inputStreams = new ArrayList<>();
        results = new ArrayList<>();

        inputStreams.add(new FileInputStream("QueryResults/test/test.xml"));
        inputStreams.add(new FileInputStream("QueryResults/CD1.xml"));
        inputStreams.add(new FileInputStream("QueryResults/CD2.xml"));
        inputStreams.add(new FileInputStream("QueryResults/CD3.xml"));
        inputStreams.add(new FileInputStream("QueryResults/CD4.xml"));
        inputStreams.add(new FileInputStream("QueryResults/CD5.xml"));
        inputStreams.add(new FileInputStream("QueryResults/CD6.xml"));//6
        inputStreams.add(new FileInputStream("QueryResults/CD7.xml"));
        inputStreams.add(new FileInputStream("QueryResults/LD1.xml"));//8
        inputStreams.add(new FileInputStream("QueryResults/LD2.xml"));//9
        inputStreams.add(new FileInputStream("QueryResults/LD3.xml"));//10
        inputStreams.add(new FileInputStream("QueryResults/LD4.xml"));//11
        inputStreams.add(new FileInputStream("QueryResults/LD5.xml"));//12
        inputStreams.add(new FileInputStream("QueryResults/LD6.xml"));
        inputStreams.add(new FileInputStream("QueryResults/LD7.xml"));
        inputStreams.add(new FileInputStream("QueryResults/LD8.xml"));
        inputStreams.add(new FileInputStream("QueryResults/LD9.xml"));
        inputStreams.add(new FileInputStream("QueryResults/LD10.xml"));
        inputStreams.add(new FileInputStream("QueryResults/LD11.xml"));
        inputStreams.add(new FileInputStream("QueryResults/LS1.xml"));
        inputStreams.add(new FileInputStream("QueryResults/LS2.xml"));
        inputStreams.add(new FileInputStream("QueryResults/LS3.xml"));
        inputStreams.add(new FileInputStream("QueryResults/LS4.xml"));
        inputStreams.add(new FileInputStream("QueryResults/LS5.xml"));
        inputStreams.add(new FileInputStream("QueryResults/LS6.xml"));
        inputStreams.add(new FileInputStream("QueryResults/LS7.xml"));
        inputStreams.add(new FileInputStream("QueryResults/artificial_0.01.xml"));
        inputStreams.add(new FileInputStream("QueryResults/artificial_0.05.xml"));
        inputStreams.add(new FileInputStream("QueryResults/artificial_0.1.xml"));
        inputStreams.add(new FileInputStream("QueryResults/artificial_0.5.xml"));
        inputStreams.add(new FileInputStream("QueryResults/artificial_1.0.xml"));

        SPARQLResultsXMLParser parser = new SPARQLResultsXMLParser();

        for (InputStream inputStream : inputStreams) {

            TupleQueryResultBuilder tupleQueryResultBuilder = new TupleQueryResultBuilder();
            parser.setTupleQueryResultHandler(tupleQueryResultBuilder);
            parser.parseQueryResult(inputStream);

            TupleQueryResult resultIterator = tupleQueryResultBuilder.getQueryResult();

            List<BindingSet> result = new ArrayList<BindingSet>();

            while (resultIterator.hasNext()) {
                result.add(resultIterator.next());
            }

            results.add(result);
        }
        //int[] indexList = {1,6,8,9,10,11,12,13,14,15,18,19,20,21,23,24,25};

        float[] settings_1 = {0, 3.5f, 7, 10.5f, 14, 17.5f, 21, 24.5f, 28f, 31.5f, 35, 38.5f, 42, 45.5f, 49, 52.5f, 56, 59.5f, 63, 66.5f, 70};
        budgetList.put(1, settings_1);
        xPoint.put(1, 30);

        float[] settings_6 = {0, 0.5f, 1, 1.5f, 2, 2.5f, 3, 3.5f, 4, 4.5f, 5, 5.5f, 6, 6.5f, 7, 7.5f, 8, 8.5f, 9, 9.5f, 10};
        budgetList.put(6, settings_6);
        xPoint.put(6, 4);

        float[] settings_10 = {0,5,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80,85,90,95,100};
        budgetList.put(10, settings_10);
        xPoint.put(10, 53);

        float[] settings_11 = {0,2.5f,5,7.5f,10,12.5f,15,17.5f,20,22.5f,25,27.5f,30,32.5f,35,37.5f,40,42.5f,45,47.5f,50};
        budgetList.put(11, settings_11);
        xPoint.put(11, 17);

        float[] settings_12 = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20};
        budgetList.put(12, settings_12);
        xPoint.put(12, 9);

        float[] settings_13 = {0,1.5f,3,4.5f,6,7.5f,9,10.5f,12,13.5f,15,16.5f,18,19.5f,21,22.5f,24,25.5f,27,28.5f,30};
        budgetList.put(13, settings_13);
        xPoint.put(13, 13);

        float[] settings_15 = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20};
        budgetList.put(15, settings_15);
        xPoint.put(15, 7);

        float[] settings_24 = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20};
        budgetList.put(24, settings_24);
        xPoint.put(24, 9);

        float[] settings_25 = {0, 3.5f, 7, 10.5f, 14, 17.5f, 21, 24.5f, 28f, 31.5f, 35, 38.5f, 42, 45.5f, 49, 52.5f, 56, 59.5f, 63, 66.5f, 70};
        budgetList.put(25, settings_25);
        xPoint.put(25, 48);

        float[] settings_9 = {0,6,12,18,24,30,36,42,48,54,60,66,72,78,84,90,96,102,108,114,120};
        budgetList.put(9, settings_9);
        xPoint.put(9, 60);
/*
        float[] settings_8 = {0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 190, 200};
        budgetList.put(8, settings_8);
        xPoint.put(8, 50);

        float[] settings_20 = {0, 12.5f, 25, 37.5f, 50, 62.5f, 75, 87.5f, 100, 112.5f, 125, 137.5f, 150, 162.5f, 175, 187.5f, 200, 212.5f, 225, 237.5f, 250};
        budgetList.put(20, settings_20);
        xPoint.put(20, 50);

        float[] settings_23 = {0, 12.5f, 25, 37.5f, 50, 62.5f, 75, 87.5f, 100, 112.5f, 125, 137.5f, 150, 162.5f, 175, 187.5f, 200, 212.5f, 225, 237.5f, 250};
        budgetList.put(23, settings_23);
        xPoint.put(23, 50);

        float[] settings_18 = {0, 12.5f, 25, 37.5f, 50, 62.5f, 75, 87.5f, 100, 112.5f, 125, 137.5f, 150, 162.5f, 175, 187.5f, 200, 212.5f, 225, 237.5f, 250};
        budgetList.put(18, settings_18);
        xPoint.put(18, 50);

        float[] settings_19 = {0,40,80,120,160,200,240,280,320,360,400,440,480,520,560,600,640,680,720,760,800};
        budgetList.put(19, settings_19);
        xPoint.put(19, 50);

        float[] settings_14 = {0,40,80,120,160,200,240,280,320,360,400,440,480,520,560,600,640,680,720,760,800};
        budgetList.put(14, settings_14);
        xPoint.put(14, 50);

        float[] settings_21 = {0,300,600,900,1200,1500,1800,2100,2400,2700,3000,3300,3600,3900,4200,4500,4800,5100,5400,5700,6000};
        budgetList.put(21, settings_21);
        xPoint.put(21,50); */
    }

    public void runAllocation() throws IloException {

        writer.print("Identifier,Rule,Limit,Timeout,Runtime,Utility,Price,Budget");

        writer.println("");

        List<BindingSet> result;

        if (ratio == null) {

            result = results.get(index);

            if (limit != 0) {
                List<BindingSet> copy = new LinkedList<BindingSet>(result);
                Collections.shuffle(copy);
                result = copy.subList(0, limit);
            }
        } else {
            result = new ArrayList<>(limit);

            int uniqueIds = 1 + (int)((limit * TRIPLES_PER_RANDOM - 1) * ratio);
            List<Integer> ids = new ArrayList<>();
            for (int i = 0; i < limit * TRIPLES_PER_RANDOM; ++i) {
                ids.add(i % uniqueIds);
            }
            java.util.Collections.shuffle(ids);

            List<String> bindingNames = new ArrayList<>();
            for (int i = 0; i < TRIPLES_PER_RANDOM; ++i) {
                bindingNames.add("product" + i);
            }
            bindingNames.add("UUID");

            for (int i = 0; i < limit; ++i) {
                List<Value> values = new ArrayList<>();
                for (int j = 0; j < TRIPLES_PER_RANDOM; ++j) {
                    values.add(SimpleValueFactory.getInstance().createLiteral("Product_" + ids.get(i * TRIPLES_PER_RANDOM + j)));
                }
                values.add(SimpleValueFactory.getInstance().createLiteral("Solution_" + i));
                BindingSet bindingSet = new ListBindingSet(bindingNames, values);
                result.add(bindingSet);
            }
        }

        int productsPerSolution = getNumberOfProductsPerSolution(result);

        float utility = 0;
        float timeInSeconds = 0;
        float price = 0;
        float budget = 0;

        boolean timeOut = false;

        long startTime;

        ValueFunction valueFunction = new RowHashValueFunction(productsPerSolution * valueCoefficient);
        //experiment settings
        List<PriceManager> PriceManagerList = new ArrayList<>();

        Solver solver;

        //define a file writer to write all experiment results
        FileWriter fwGreedyOutput;
        FileWriter fwDynamicOutput;
        FileWriter fwNaiveOutput;
        String greedyFile = "output/value/" + this.index + "_freemium_25_GreedyResult.txt";
        String dynamicFile = "output/value/" + this.index + "_freemium_25_DynamicResult.txt";
        //String naiveFile = "output/" + this.index + "_flat_NaiveResult.txt";

        try{
            fwGreedyOutput = new FileWriter(new File(greedyFile));
            fwDynamicOutput = new FileWriter(new File(dynamicFile));
            //fwNaiveOutput = new FileWriter(new File(naiveFile));

            int xPoint = this.xPoint.get(this.index);
            System.out.println("xPoint: "+xPoint);
            //for(float[] setting: testSettings){
            for(float setting: this.budgetList.get(this.index)){
                float setPrice = setting;
                //float setPrice = setting[1];
                PriceManager priceManager = new HashPriceManager(priceCoefficient, xPoint, setPrice);

                System.out.println(setPrice);
/*
                System.out.println("Naive Algorithm");
                solver = new NaiveSolver(priceManager);
                startTime = System.nanoTime();
                float[] results = allocate(result, solver, priceManager, valueFunction);
                utility = results[0];
                price = results[1];
                budget = results[2];
                timeInSeconds = (System.nanoTime() - startTime) / 1.0e9f;
                String record = ""+ setPrice + ":\t" + utility + "\t" + price + "\t" + budget + "\t" + timeInSeconds + "\n";
                fwNaiveOutput.write(record);
*/
                System.out.println("Dynamic Algorithm");
                solver = new DynamicProgrammingSolver(priceManager);
                startTime = System.nanoTime();
                float[] results = allocate(result, solver, priceManager, valueFunction);
                utility = results[0];
                //System.out.println(utility);
                price = results[1];
                budget = results[2];
                timeInSeconds = (System.nanoTime() - startTime) / 1.0e9f;
                String record = ""+ setPrice + ":\t" + utility + "\t" + price + "\t" + budget + "\t" + timeInSeconds + "\n";
                //System.out.println(record);
                fwDynamicOutput.write(record);

                System.out.println("Greedy Algorithm");
                solver = new GreedyNaiveSolver(priceManager);
                startTime = System.nanoTime();
                results = allocate(result, solver, priceManager, valueFunction);
                utility = results[0];
                price = results[1];
                budget = results[2];
                timeInSeconds = (System.nanoTime() - startTime) / 1.0e9f;
                record = ""+ setPrice + ":\t" + utility + "\t" + price + "\t" + budget + "\t" + timeInSeconds + "\n";
                fwGreedyOutput.write(record);
            }
            //fwNaiveOutput.close();
            fwDynamicOutput.close();
            fwGreedyOutput.close();

        }catch (IOException ex){
            ex.printStackTrace();
        }
    }

    private float[] allocate(List<BindingSet> result, Solver solver, PriceManager priceManager, ValueFunction valueFunction) throws IloException {
        List<DataBundle> dataBundles = DataBundleFactory.createDataBundles(result, MAX_TRIPLEPATTERNS_PER_QUERY, valueFunction, priceManager);
        solver.addBundles(dataBundles);
        //DataBundleFactory.printBundes(dataBundles, false);

        float totalPrice = getTotalPrice(dataBundles);
        //System.out.println(totalPrice);
        float budget = dataBundles.size() * budgetCoefficient;
        //System.out.println(budget);
        //float budget = 1.8f;

        List<DataBundle> results = solver.solve(budget);
        //System.out.println("Solution Mappings in the results>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        //DataBundleFactory.printBundes(results, true);

        float price = solver.getCurrentPrice();
        float value = solver.getCurrentValue();
        //System.out.println(value);
        //System.out.println(results.size());
        //System.out.println(price);

        return new float[]{value, price, budget};
    }

    private int getNumberOfProductsPerSolution(List<BindingSet> bindingSets) {
        int count = 0;
        for (String bindingName : bindingSets.get(0).getBindingNames()) {
            if (bindingName.startsWith("product")) {
                count++;
            }
        }
        return count;
    }

    private float getTotalPrice(List<DataBundle> dataBundles) {
        float totalPrice = 0;
        Set<DataProduct> products = new HashSet<>();
        for (DataBundle dataBundle : dataBundles) {
            for (Set<DataProduct> productSet : dataBundle.getProductsList()) {
                products.addAll(productSet);
            }
        }

        for (DataProduct product : products) {
            totalPrice += product.getCost();
        }
        return totalPrice;
    }
}
