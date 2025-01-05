package ch.uzh.ifi.tg.serviceMarket.solver;

import ch.uzh.ifi.tg.serviceMarket.market.ValueFunction;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataBundle;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataProduct;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import javax.xml.crypto.Data;
import java.util.*;

/**
 * Created by Tobias Grubenmann on 21/09/16.
 */
public class IntegerBundleSolver implements Solver {

    private float value;
    private float price;

    private int triplesPerBundle;

    public static final float EPSILON = 0.00001f;

    private IloCplex cplex;

    private List<DataBundle> bundles;
    private Map<DataProduct, IloNumVar> productVarMap;
    private Map<IloNumVar, List<DataBundle>> varBundleMap;

    public IntegerBundleSolver(int triplesPerBundle) throws IloException {
        cplex = new IloCplex();
        bundles = new ArrayList<DataBundle>();
        productVarMap = new HashMap<DataProduct, IloNumVar>();
        varBundleMap = new HashMap<IloNumVar, List<DataBundle>>();
        value = 0;
        this.triplesPerBundle = triplesPerBundle;
    }

    public IntegerBundleSolver(int triplesPerBundle, float maxRuntime) throws IloException {
        this(triplesPerBundle);
        cplex.setParam(IloCplex.DoubleParam.TiLim, maxRuntime);
    }

    @Override
    public void addBundles(List<DataBundle> dataBundles, ValueFunction valueFunction) throws IloException {
        bundles.addAll(dataBundles);
    }

    @Override
    public void addBundle(DataBundle dataBundle) throws IloException {

        bundles.add(dataBundle);

    }

    private void createVars() throws IloException {

        int count = 0;
        IloLinearNumExpr constraint = cplex.linearNumExpr();
        List<DataBundle> dataBundles = new ArrayList<>();

        for (int i = 0; i < bundles.size(); ++i) {

            for (Set<DataProduct> products : bundles.get(i).getProductsList()) {

                for (DataProduct product : products) {
                    IloNumVar productVar;
                    if (productVarMap.containsKey(product)) {
                        productVar = productVarMap.get(product);
                    } else {
                        productVar = cplex.intVar(0, 1, product.getIdentifier());
                        productVarMap.put(product, productVar);
                    }
                    constraint.addTerm(1, productVar);
                    ++count;
                }

                dataBundles.add(bundles.get(i));

                if ((i + 1) % triplesPerBundle == 0 || i == bundles.size() - 1) {

                    IloNumVar solutionVar = cplex.intVar(0, 1);
                    constraint.addTerm(-count, solutionVar);
                    cplex.addGe(constraint, 0);

                    varBundleMap.put(solutionVar, dataBundles);

                    constraint = cplex.linearNumExpr();
                    count = 0;
                    dataBundles = new ArrayList<>();
                }
            }

        }

    }

    @Override
    public List<DataBundle> solve(float budget) throws IloException {

        createVars();

        value = 0;

        List<DataBundle> result = new ArrayList<DataBundle>();

        IloLinearNumExpr objective = cplex.linearNumExpr();

        IloLinearNumExpr budgetConstraint = cplex.linearNumExpr();

        // utility = values - costs

        for (IloNumVar bundleVar : varBundleMap.keySet()) {
            float value = 0;
            for (DataBundle bundle : varBundleMap.get(bundleVar)) {
                value += bundle.getValue();
            }
            objective.addTerm(value, bundleVar);
        }

        for (DataProduct product : productVarMap.keySet()) {
            budgetConstraint.addTerm(product.getCost(), productVarMap.get(product));
            objective.addTerm(-product.getCost(), productVarMap.get(product));
        }

        cplex.addLe(budgetConstraint, budget);

        cplex.addMaximize(objective);

        cplex.solve();

        for (IloNumVar bundleVar: varBundleMap.keySet()) {
            double coefficient = cplex.getValue(bundleVar);
            if (coefficient > 1 - EPSILON) {
                float bundleValue = 0;
                for (DataBundle bundle : varBundleMap.get(bundleVar)) {
                    bundleValue += bundle.getValue();
                }
                value += bundleValue;
                result.addAll(varBundleMap.get(bundleVar));
            }
        }

        for (DataProduct product : productVarMap.keySet()) {
            double coefficient = cplex.getValue(productVarMap.get(product));
            if (coefficient > 1 - EPSILON) {
                price += product.getCost();
            }
        }

        return result;
    }

    @Override
    public float getCurrentValue() {
        return value;
    }

    @Override
    public float getCurrentPrice() {
        return price;
    }

    public boolean hasReachedLimit() throws IloException {
        return cplex.getCplexStatus().equals(IloCplex.CplexStatus.AbortTimeLim);
    }

    public void end() throws IloException {
        cplex.end();
    }
}
