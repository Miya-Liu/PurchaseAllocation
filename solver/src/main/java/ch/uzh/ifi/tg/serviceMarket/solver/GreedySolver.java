package ch.uzh.ifi.tg.serviceMarket.solver;

import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataBundle;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataProduct;
import ch.uzh.ifi.tg.serviceMarket.market.PriceManager;
import ilog.concert.IloException;

import java.util.*;

/**
 * Created by Tobias Grubenmann on 21/09/16.
 */
public class GreedySolver implements Solver {

    List<Entry> openList;
    public PriceManager priceManager;
    Map<Integer, Long> providerProductsAmount = new HashMap<>(); //store current product amount from each provider

    private float value;
    private float price;

    public GreedySolver(PriceManager priceManager) {
        openList = new ArrayList<>();
        this.priceManager = priceManager;
    }

    public void addBundles(List<DataBundle> dataBundles) throws IloException {
        for (DataBundle dataBundle : dataBundles) {
            addBundle(dataBundle);
        }
    }

    public void addBundle(DataBundle dataBundle) throws IloException {

        openList.add(new Entry(dataBundle));

    }

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
                    excludeList.addAll(highestUtilityRatioEntry.products);

                    for (Entry entry : openList) {
                        entry.updatePrices(excludeList);
                    }

                    entryFound = true;
                }
            }
        }

        System.out.print(excludeList.size());
        System.out.print(excludeList);
        value = totalValue;
        price = totalPrice;

        return result;
    }

    public float getCurrentValue() {
        return value;
    }

    public float getCurrentPrice() {
        return price;
    }

    private class Entry { //treat as a solution mapping
        public float value;
        public float price;
        public DataBundle bundle; //treat as a solution mapping
        public Set<DataProduct> products;
        public Map<DataProduct, Integer> provider = new HashMap<>(); //store the current provider of product

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
