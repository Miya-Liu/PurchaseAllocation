package ch.uzh.ifi.tg.serviceMarket.solver;

import ch.uzh.ifi.tg.serviceMarket.market.PriceManager;
import ch.uzh.ifi.tg.serviceMarket.market.ValueFunction;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataBundle;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataProduct;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.*;

/**
 * Created by Tobias Grubenmann on 21/09/16.
 */
public class IntegerSolver implements Solver {

    private float value;
    private float price;
    public PriceManager priceManager;

    public static final float EPSILON = 0.00001f;

    private IloCplex cplex;

    private Map<DataProduct, IloNumVar> productVarMap;
    private Map<IloNumVar, DataBundle> varBundleMap;

    public IntegerSolver() throws IloException {
        cplex = new IloCplex();
        productVarMap = new HashMap<DataProduct, IloNumVar>();
        varBundleMap = new HashMap<IloNumVar, DataBundle>();
        value = 0;
    }

    @Override
    public void addBundles(List<DataBundle> dataBundles, ValueFunction valueFunction) throws IloException {
        for (DataBundle dataBundle : dataBundles) {
            addBundle(dataBundle);
        }
    }

    @Override
    public void addBundle(DataBundle dataBundle) throws IloException {

        List<IloNumVar> atomicAllocationVars = new ArrayList<IloNumVar>();

        for (Set<DataProduct> products : dataBundle.getProductsList()) {
            IloLinearNumExpr constraint = cplex.linearNumExpr();
            int count = 0;

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
            IloNumVar atomicAllocationVar = cplex.intVar(0, 1);
            constraint.addTerm(-count, atomicAllocationVar);
            cplex.addGe(constraint, 0);

            atomicAllocationVars.add(atomicAllocationVar);
        }

        IloNumVar bundleVar;

        if (atomicAllocationVars.size() == 1) {
            bundleVar = atomicAllocationVars.get(0);
        } else {
            bundleVar = cplex.intVar(0, 1);
            IloLinearNumExpr constraint = cplex.linearNumExpr();
            constraint.addTerm(-1, bundleVar);
            for (IloNumVar atomicAllocationVar : atomicAllocationVars) {
                constraint.addTerm(1, atomicAllocationVar);
            }
            cplex.addGe(constraint, 0);
        }

        varBundleMap.put(bundleVar, dataBundle);

    }

    @Override
    public List<DataBundle> solve(float budget) throws IloException {

        value = 0;

        List<DataBundle> result = new ArrayList<DataBundle>();

        IloLinearNumExpr objective = cplex.linearNumExpr();

        IloLinearNumExpr budgetConstraint = cplex.linearNumExpr();

        // utility = values - costs

        for (IloNumVar bundleVar : varBundleMap.keySet()) {
            objective.addTerm(varBundleMap.get(bundleVar).getValue(), bundleVar);
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
                value += varBundleMap.get(bundleVar).getValue();
                result.add(varBundleMap.get(bundleVar));
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
}
