package ch.uzh.ifi.tg.serviceMarket.evaluation;

import ch.uzh.ifi.tg.serviceMarket.solver.IntegerSolver;
import ch.uzh.ifi.tg.serviceMarket.solver.GreedySolver;
import ch.uzh.ifi.tg.serviceMarket.solver.Solver;
import ch.uzh.ifi.tg.serviceMarket.market.*;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataBundle;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataProduct;
import ilog.concert.IloException;
import org.apache.commons.cli.*;
import org.openrdf.model.Value;
import org.openrdf.model.impl.SimpleValueFactory;
import org.openrdf.query.*;
import org.openrdf.query.impl.ListBindingSet;
import org.openrdf.query.impl.TupleQueryResultBuilder;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLParser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by Tobias Grubenmann on 07.06.17.
 */
public class AllocationSimulator {
    public static final int MAX_TRIPLES_PER_QUERY = 10;

    private List<InputStream> inputStreams;
    private List<List<BindingSet>> results;

    private List<Set<DataProduct>> allocatedProducts;

    private PriceManager priceManager;
    
    private int productsPerDataset;
    private float valueCoefficient;
    private float budget;
    /*
     * mode 0 = value per row
     * mode 1 = value per triple
     * mode 2 = value per query
     * mode 3 = random per row
     * mode 4 = random per triple
     * mode 5 = random value per query
     */
    public static final int VALUE_PER_ROW = 0;
    public static final int VALUE_PER_TRIPLE = 1;
    public static final int VALUE_PER_QUERY = 2;
    public static final int RANDOM_PER_ROW = 3;
    public static final int RANDOM_PER_TRIPLE = 4;
    public static final int RANDOM_PER_QUERY = 5;
    private int valueMode;

    private boolean greedyAllocation;

    private float priceCoefficient;
    private List<Integer> distribution;
    
    public AllocationSimulator(float priceCoefficient, int productsPerDataset, float valueCoefficient, float budget, int valueMode, boolean greedyAllocation, List<Integer> distribution) {
        this.productsPerDataset = productsPerDataset;
        this.valueCoefficient = valueCoefficient;
        this.budget = budget;
        this.valueMode = valueMode;
        this.greedyAllocation = greedyAllocation;
        this.priceCoefficient = priceCoefficient;
        this.distribution = distribution;
    }

    public static void main(String[] args) throws ParseException, IOException, IloException, QueryEvaluationException, QueryResultHandlerException, QueryResultParseException {
        Options options = new Options();
        options.addOption("p", "price", true, "price per data product");
        options.addOption("t", "triples", true, "products per dataset");
        options.addOption("v", "value", true, "value per entity (specified by mode)");
        options.addOption("b", "budget", true, "budget per query");
        options.addOption("m", "mode", true, "value mode (0 = value per row, 1 = value per triple, 2 = value per query");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        float priceCoefficient = Float.parseFloat(cmd.getOptionValue("p"));
        int triplesPerProduct = Integer.parseInt(cmd.getOptionValue("t"));
        float valuePerSolution = Integer.parseInt(cmd.getOptionValue("v"));
        float budget = Integer.parseInt(cmd.getOptionValue("b"));
        int valueMode = Integer.parseInt(cmd.getOptionValue("m"));

        float revenue = getFedMarkRevenueAndWelfare(priceCoefficient, triplesPerProduct, valuePerSolution, budget, valueMode)[0];

        System.out.println("*****REVENUE*****");
        System.out.println(revenue);
    }
    
    public static float[] getFedMarkRevenueAndWelfare(float priceCoefficient, int productsPerDataset, float valuePerSolution, float budget, int valueMode) throws QueryResultParseException, QueryResultHandlerException, QueryEvaluationException, IOException, IloException {
        return getFedMarkRevenueAndWelfare(priceCoefficient, productsPerDataset, valuePerSolution, budget, valueMode, null);
    }

    public static float[] getFedMarkRevenueAndWelfare(float priceCoefficient, int productsPerDataset, float valuePerSolution, float budget, int valueMode, List<Integer> distribution) throws QueryResultParseException, QueryResultHandlerException, QueryEvaluationException, IOException, IloException {
        return getFedMarkRevenueAndWelfare(priceCoefficient, productsPerDataset, valuePerSolution, budget, valueMode, distribution, false);
    }

    public static float[] getFedMarkRevenueAndWelfare(float priceCoefficient, int productsPerDataset, float valuePerSolution, float budget, int valueMode, List<Integer> distribution, boolean greedyAllocation) throws QueryResultParseException, QueryResultHandlerException, QueryEvaluationException, IOException, IloException {
        AllocationSimulator allocationSimulator = new AllocationSimulator(priceCoefficient, productsPerDataset, valuePerSolution, budget, valueMode, greedyAllocation, distribution);

        allocationSimulator.parseResults();
        if (productsPerDataset >= 0) {
            allocationSimulator.mergeProducts();
        }

        return allocationSimulator.runAllocation();
    }

    public void parseResults() throws IOException, QueryResultParseException, QueryResultHandlerException, QueryEvaluationException {

        inputStreams = new ArrayList<>();
        results = new ArrayList<>();

        inputStreams.add(new FileInputStream("QueryResults/CD1.xml"));
        inputStreams.add(new FileInputStream("QueryResults/CD2.xml"));
        inputStreams.add(new FileInputStream("QueryResults/CD3.xml"));
        inputStreams.add(new FileInputStream("QueryResults/CD4.xml"));
        inputStreams.add(new FileInputStream("QueryResults/CD5.xml"));
        inputStreams.add(new FileInputStream("QueryResults/CD6.xml"));
        inputStreams.add(new FileInputStream("QueryResults/CD7.xml"));
        inputStreams.add(new FileInputStream("QueryResults/LD1.xml"));
        inputStreams.add(new FileInputStream("QueryResults/LD2.xml"));
        inputStreams.add(new FileInputStream("QueryResults/LD3.xml"));
        inputStreams.add(new FileInputStream("QueryResults/LD4.xml"));
        inputStreams.add(new FileInputStream("QueryResults/LD5.xml"));
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
    
    public void mergeProducts() {
        for (int queryIndex = 0; queryIndex < results.size(); ++queryIndex) {

            for (int resultIndex = 0; resultIndex < results.get(queryIndex).size(); ++resultIndex) {
                BindingSet bindings = results.get(queryIndex).get(resultIndex);

                List<Value> newValues = new ArrayList<>();

                for (String bindingName : bindings.getBindingNames()) {

                    Value value = bindings.getValue(bindingName);

                    for (int i = 0; i < MAX_TRIPLES_PER_QUERY; ++i) {
                        if (bindingName.equals("product" + i)) {
                            String[] parts = value.stringValue().split("_(?=[^_]+$)");

                            if (productsPerDataset == 0) {
                                value = SimpleValueFactory.getInstance().createIRI("http://www.example.com/product");
                            }
                            else if (productsPerDataset == 1) {
                                value = SimpleValueFactory.getInstance().createIRI(parts[0]);
                            }
                            else {

                                int newProductNumber = Integer.parseInt(parts[1]) % productsPerDataset;

                                value = SimpleValueFactory.getInstance().createIRI(parts[0] + "_" + newProductNumber);
                            }
                        }
                    }
                    newValues.add(value);
                }

                results.get(queryIndex).set(resultIndex, new ListBindingSet(new ArrayList<String>(bindings.getBindingNames()), newValues));

            }
        }
    }

    public float[] runAllocation() throws IloException {

        float revenue = 0;
        float value = 0;

        for (int i = 0; i < results.size(); ++i) {

            priceManager = new ConstantPriceManager(priceCoefficient);
            allocatedProducts = new ArrayList<>();

            List<DataBundle> dataBundles = DataBundleFactory.createDataBundles(results.get(i), MAX_TRIPLES_PER_QUERY, getValueFunction(), priceManager);
            Solver solver;
            if (greedyAllocation) {
                solver = new GreedySolver(priceManager);
            } else {
                solver = new IntegerSolver();
            }
            solver.addBundles(dataBundles);

            List<DataBundle> bundles = solver.solve(budget);

            // for each query, get all allocated products
            Set<DataProduct> products = new HashSet<>();
            for (DataBundle bundle : bundles) {
                for (Set<DataProduct> products1 : bundle.getProductsList()) {
                    products.addAll(products1);
                }
            }

            allocatedProducts.add(products);

            int factor = 1;
            if (distribution != null) {
                factor = distribution.get(i);
            }
            for (int j = 0; j < allocatedProducts.size(); ++j) {
                for (DataProduct product : allocatedProducts.get(j)) {
                    revenue += factor * priceManager.getPrice(product.getIdentifier());
                }
            }

            value += factor * solver.getCurrentValue();

        }

        return new float[]{revenue, value};
    }

    private ValueFunction getValueFunction() {
        if (valueMode == VALUE_PER_ROW) {
            return new RowValueFunction(valueCoefficient);
        }
        if (valueMode == VALUE_PER_QUERY) {
            return new QueryValueFunction(valueCoefficient);
        }
        if (valueMode == RANDOM_PER_ROW) {
            return new RandomRowValueFunction(valueCoefficient);
        }
        return new RandomQueryValueFunction(valueCoefficient);
    }
}
