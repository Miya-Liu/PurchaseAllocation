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
    private String valueDistribution;
    private float budgetCoefficient;
    private int index;

    public String pf; // let this be the name of pricing function settings file

    private String algorithm;
    private int limit;
    private Float ratio;

    private PrintWriter writer;

    public ScalingEvaluation(float priceCoefficient, String p_f, float valueCoefficient, String valueDistribution,
                             float budgetCoefficient, int index, PrintWriter writer, int steps, String useAlgorithm,
                             int limit, Float ratio) {
        this.priceCoefficient = priceCoefficient;
        this.pf = p_f;
        this.valueCoefficient = valueCoefficient;
        this.valueDistribution = valueDistribution;
        this.budgetCoefficient = budgetCoefficient;
        this.index = index;
        this.writer = writer;
        this.algorithm = useAlgorithm;
        this.limit = limit;
        this.ratio = ratio;
    }

    public static void main(String[] args) throws ParseException, IOException, IloException, QueryEvaluationException, QueryResultHandlerException, QueryResultParseException {
        Options options = new Options();
        //options.addOption("p", "price", true, "price per data product");
        options.addOption("pf", "p_function", true, "pricing function");
        options.addOption("v", "value", true, "value distribution"); // e, s, m, l
        //options.addOption("vd", "value_distribution", true, "value distribution"); // S, M, L
        //options.addOption("b", "budgetCoefficient", true, "budgetCoefficient coefficient");
        options.addOption("i", "index", true, "query index");
        //options.addOption("o", "output", true, "output path");
        options.addOption("a", "algorithm", true, "uses the input algorithm"); // brute, greedy, 3ddp
        options.addOption("l", "limit", true, "start limit");
        options.addOption("r", "ratio", true, "ratio of unique triples");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        float priceCoefficient = 1.0f;
        //float priceCoefficient = Float.parseFloat(cmd.getOptionValue("p"));
        String p_f = cmd.getOptionValue("pf");
        float valuePerSolution = 1.0f;
        String valueDistribution = cmd.getOptionValue("v");

        float budget = 0.6f;
        //float budget = Float.parseFloat(cmd.getOptionValue("b"));
        String outputPath = "output/1.txt";

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

        String useAlgorithm = cmd.getOptionValue("a");

        PrintWriter writer = new PrintWriter(outputPath, "UTF-8");

        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>   Start   " );
        runRuntimeEvaluation(priceCoefficient, p_f, valuePerSolution, valueDistribution, budget, index, writer, steps, useAlgorithm, limit, ratio);
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>   End   " );

        writer.close();
    }
    
    public static void runRuntimeEvaluation(float priceCoefficient, String p_f, float valuePerSolution,
                                            String valueDistribution, float budget, int index, PrintWriter writer,
                                            int steps, String useAlgorithm, int startLimit, Float ratio)
            throws QueryResultParseException, QueryResultHandlerException, QueryEvaluationException, IOException, IloException {
        ScalingEvaluation allocationSimulator = new ScalingEvaluation(priceCoefficient, p_f, valuePerSolution, valueDistribution, budget, index, writer, steps, useAlgorithm, startLimit, ratio);

        allocationSimulator.parseResults();
        //allocationSimulator.runAllocation();

        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>   " + p_f );
        System.out.println("************************ Query No. " + allocationSimulator.index + "   ****************************" );
        allocationSimulator.runAllocation();

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
        valueFunction.setDistribution(this.valueDistribution);

        Solver solver;

        FileWriter fwOutput;
        String resultFile;

        // output file name:
        // algorithm is the folder
        // query index _ pricing function index number _ value distribution _ algorithm name
        if (this.algorithm.equals("brute")){
            resultFile = "output/brute/" + this.index + "_" +
                    pf.replace(".txt", "").split("-")[1] + "_" + this.valueDistribution + "_" + "BruteResult.txt";
        }
        else if (this.algorithm.equals("greedy")){
            resultFile = "output/greedy/" + this.index + "_" +
                    pf.replace(".txt", "").split("-")[1] + "_" + this.valueDistribution + "_" + "GreedyResult.txt";
        }
        else
            resultFile = "output/dynamic/" + this.index + "_" +
                    pf.replace(".txt", "").split("-")[1] + "_" + this.valueDistribution + "_" + "DynamicResult.txt";

        try{
            fwOutput = new FileWriter(new File(resultFile));

            List<PriceManager> priceManager = new ArrayList<>();
            try{
                BufferedReader pf_settings = new BufferedReader(new FileReader(pf));
                while(pf_settings.ready()){
                    String a_pf = pf_settings.readLine();
                    if (a_pf != "\n"){
                        String[] parameters = a_pf.replace("\n", "").split("\t");
                        priceManager.add(new HashPriceManager(priceCoefficient,
                                Long.parseLong(parameters[1]), Float.parseFloat(parameters[2]), parameters[0]));
                    }
                }
            }catch (IOException ioe){
                ioe.printStackTrace();
            }

            System.out.println(this.pf);
            System.out.println(this.algorithm);
            for(int i = 0 ; i < 1 ; i ++){
                System.out.println(i);
                // output file format:
                // number, utility, price, budget and runtime
                if (this.algorithm.equals("brute")) {
                    //System.out.println("Brute Algorithm");
                    solver = new NaiveSolver(priceManager);
                    startTime = System.nanoTime();
                    float[] brute_results = allocate(result, solver, priceManager, valueFunction);
                    utility = brute_results[0];
                    price = brute_results[1];
                    budget = brute_results[2];
                    timeInSeconds = (System.nanoTime() - startTime) / 1.0e9f;
                    String brute_record = "" + i + ":\t" + utility + "\t" + price + "\t" + budget + "\t" + timeInSeconds + "\n";
                    fwOutput.write(brute_record);
                }

                if (this.algorithm.equals("3ddp")) {
                    //System.out.println("Dynamic Algorithm");
                    solver = new DynamicProgrammingSolver(priceManager);
                    startTime = System.nanoTime();
                    float[] results = allocate(result, solver, priceManager, valueFunction);
                    utility = results[0];
                    //System.out.println(utility);
                    price = results[1];
                    budget = results[2];
                    timeInSeconds = (System.nanoTime() - startTime) / 1.0e9f;
                    String record = "" + i + ":\t" + utility + "\t" + price + "\t" + budget + "\t" + timeInSeconds + "\n";
                    fwOutput.write(record);
                }

                if (this.algorithm.equals("greedy")) {
                    //System.out.println("Greedy Algorithm");
                    solver = new GreedyNaiveSolver(priceManager);
                    startTime = System.nanoTime();
                    float[] results = allocate(result, solver, priceManager, valueFunction);
                    utility = results[0];
                    price = results[1];
                    budget = results[2];
                    timeInSeconds = (System.nanoTime() - startTime) / 1.0e9f;
                    String record = "" + i + ":\t" + utility + "\t" + price + "\t" + budget + "\t" + timeInSeconds + "\n";
                    fwOutput.write(record);
                }

            }
            fwOutput.close();

        }catch (IOException ex){
            ex.printStackTrace();
        }
    }

    private float[] allocate(List<BindingSet> result, Solver solver, List<PriceManager> priceManager, ValueFunction valueFunction) throws IloException {
        List<DataBundle> dataBundles = DataBundleFactory.createDataBundles(result, MAX_TRIPLEPATTERNS_PER_QUERY, valueFunction, priceManager);
        solver.addBundles(dataBundles, valueFunction);

        float totalPrice = getTotalPrice(dataBundles);
        float budget = totalPrice * budgetCoefficient;

        List<DataBundle> results = solver.solve(budget);

        float price = solver.getCurrentPrice();
        float value = solver.getCurrentValue();

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
