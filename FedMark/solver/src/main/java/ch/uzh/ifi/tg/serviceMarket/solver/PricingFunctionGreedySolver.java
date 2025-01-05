package ch.uzh.ifi.tg.serviceMarket.solver;

import ch.uzh.ifi.tg.serviceMarket.market.ValueFunction;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataBundle;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataProduct;
import ilog.concert.IloException;

import java.util.*;

public class PricingFunctionGreedySolver implements Solver {
    List<PricingFunctionGreedySolver.Entry> openList;
    private float value;
    private float price;

    public PricingFunctionGreedySolver(){
        openList = new ArrayList<>();
    }

    @Override
    public float getCurrentValue() {
        return 0;
    }

    @Override
    public float getCurrentPrice() {
        return 0;
    }

    @Override
    public List<DataBundle> solve(float budget) throws IloException {
        List<DataBundle> result = new ArrayList<>();

        float totalPrice = 0;
        float totalValue = 0;

        Set<DataProduct> excludeList = new HashSet<>();

        while (openList.size() > 0) {
            // get current best bundle
            openList.sort(new EntryComparator());

            boolean entryFound = false;
            while (!entryFound && openList.size() > 0) {

                Entry highestUtilityRatioEntry = openList.remove(openList.size() - 1);

                if (highestUtilityRatioEntry.getUtility() > 0 && highestUtilityRatioEntry.price + totalPrice <= budget) {

                    totalPrice += highestUtilityRatioEntry.price;
                    totalValue += highestUtilityRatioEntry.value;

                    result.add(highestUtilityRatioEntry.bundle);
                    excludeList.addAll(highestUtilityRatioEntry.products); //entry.products contain all chose products, i.e, triple

                    for (Entry entry : openList) {
                        entry.updatePrices(excludeList);
                    }

                    entryFound = true;
                }
            }
        }

        this.value = totalValue;
        this.price = totalPrice;

        return result;
    }

    @Override
    public void addBundle(DataBundle dataBundle) throws IloException {
        openList.add(new Entry(dataBundle));
    }

    @Override
    public void addBundles(List<DataBundle> dataBundles, ValueFunction valueFunction) throws IloException {
        for (DataBundle dataBundle: dataBundles){
            addBundle(dataBundle);
        }
    }

    private class Entry {
        public float value; //the value of solution mapping
        public float price; //sum of the price of products
        public DataBundle bundle; //solution mapping
        public Set<DataProduct> products;//corresponding products, i.e., triple

        public Entry(DataBundle bundle) {
            this.bundle = bundle;
            this.value = bundle.getValue();
            updatePrices(null);
        }

        public void updatePrices(Set<DataProduct> excludeList) {
            float currentMinimalPrice = Float.MAX_VALUE;
            for (Set<DataProduct> products : bundle.getProductsList()) {
                float price = 0;
                for (DataProduct product : products) {
                    if (excludeList == null || !excludeList.contains(product)) {
                        price += product.getCost();
                    }
                }
                if (price < currentMinimalPrice) {
                    currentMinimalPrice = price;
                    this.products = products;
                }
            }
            this.price = currentMinimalPrice;
        }

        public float getUtility() {
            return value - price;
        }

        public float getUtilityPriceRatio() {
            if (price == 0) {
                return Float.MAX_VALUE;
            }
            return (value - price) / price;
        }
    }

    private class EntryComparator implements Comparator<Entry> {

        @Override
        public int compare(Entry o1, Entry o2) {
            return (int)Math.signum(o1.getUtilityPriceRatio() - o2.getUtilityPriceRatio());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof EntryComparator) {
                return true;
            }
            return false;
        }
    }
}
