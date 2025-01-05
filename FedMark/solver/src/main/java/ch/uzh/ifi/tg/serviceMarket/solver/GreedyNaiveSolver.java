package ch.uzh.ifi.tg.serviceMarket.solver;

import ch.uzh.ifi.tg.serviceMarket.market.PriceManager;
import ch.uzh.ifi.tg.serviceMarket.market.ValueFunction;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataBundle;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataProduct;
import ilog.concert.IloException;
import org.apache.jena.base.Sys;
import org.apache.jena.reasoner.rulesys.builtins.Product;


import java.util.*;

public class GreedyNaiveSolver implements Solver {
    List<GreedyNaiveSolver.Entry> openList;
    public List<PriceManager> priceManager;// solver needs a price manager to know how to calculate a new price
    int[] providerProductsAmount = {0,0,0,0,0};

    private float value;
    private float price;
    public ValueFunction valueFunction;

    public GreedyNaiveSolver(List<PriceManager> priceManager) {
        openList = new ArrayList<>();
        this.priceManager = priceManager;
    }

    @Override
    public void addBundle(DataBundle dataBundle) throws IloException {
        Entry e = new Entry(dataBundle);
        //e.printEntry();
        openList.add(e);
    }

    @Override
    public void addBundles(List<DataBundle> dataBundles, ValueFunction valueFunction) throws IloException {
        this.valueFunction = valueFunction;
        for (DataBundle dataBundle: dataBundles){
            addBundle(dataBundle);
        }
    }

    public void initValue(List<GreedyNaiveSolver.Entry> eList){
        int amount = eList.size();
        double quaterValue = amount*0.25;
        int counter = 0;
        String vd = this.valueFunction.getDistribution();
        if (vd.equals("e"))
            return;
        switch (vd){
            case "s":
                counter = 0;
                for(int i = 0; i < quaterValue; i++){
                    eList.get(counter).value += 0.25f;
                    counter ++;
                }
                return;
            case "m":
                counter = 0;
                for(int i = 0; i < quaterValue; i++){
                    eList.get(counter).value += 0.25f;
                    counter ++;
                }
                for(int i = 0; i < quaterValue; i++){
                    eList.get(counter).value += 0.5f;
                    counter ++;
                }
                return;
            case "l":
                counter = 0;
                for(int i = 0; i < quaterValue; i++){
                    eList.get(counter).value += 0.25f;
                    counter ++;
                }
                for(int i = 0; i < quaterValue; i++){
                    eList.get(counter).value += 0.5f;
                    counter ++;
                }
                for(int i = 0; i < quaterValue; i++){
                    eList.get(counter).value += 0.75f;
                    counter ++;
                }
                return;
        }
    }

    @Override
    public List<DataBundle> solve(float budget) throws IloException {
        initValue(openList);

        //System.out.println("Budget: "+ budget);
        List<DataBundle> result = new ArrayList<>();

        float totalPrice = 0;
        float totalValue = 0;

        Set<DataProduct> excludeList = new HashSet<>();
        //int count = 0;

        //System.out.println("Iteration starts >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        while (openList.size() > 0) {
            //count ++;
            //System.out.println(count);
            // get current best bundle
            openList.sort(new EntryComparator());

            boolean entryFound = false;
            while (!entryFound && openList.size() > 0) {

                Entry highestUtilityRatioEntry = openList.remove(openList.size() - 1);


                if (highestUtilityRatioEntry.value >= 0 && highestUtilityRatioEntry.price <= budget) {

                    totalPrice = highestUtilityRatioEntry.price;
                    //System.out.println(highestUtilityRatioEntry.value);
                    totalValue += highestUtilityRatioEntry.value;

                    result.add(highestUtilityRatioEntry.bundle);

                    //Map<DataProduct, List<Integer>> providers = highestUtilityRatioEntry.provider;
                    for (DataProduct product: highestUtilityRatioEntry.products){
                        //int index = providers.get(product);
                        if(excludeList.contains(product))
                            continue;
                        //product.updateProviderIndex(index);
                        //providerProductsAmount[index]++;
                        excludeList.add(product);
                    }
                    //excludeList.addAll(highestUtilityRatioEntry.products);

                    for (Entry entry : openList) {
                        entry.updateDuplicatedPrice(excludeList, totalPrice);

                    }

                    entryFound = true;
                }
            }
        }

        //System.out.println("Selected products amount: "+excludeList.size());
        //printDataProducts(excludeList);

        //System.out.println("Selected amount from 1: " + providerProductsAmount[1]);
        value = totalValue;
        //System.out.println(value);
        price = totalPrice;
        return result;
    }

    @Override
    public float getCurrentPrice() {
        return this.price;
    }

    @Override
    public float getCurrentValue() {
        return this.value;
    }

    public float getCompressedCost(Entry e, Set<DataProduct> currentProductLis){
        float cost = 0;
        long[] amount = {0,0,0,0,0,0,0,0,0};
        float[] sumCost = {0,0,0,0,0,0,0,0,0};

        Map<DataProduct, Float> productsCost = new HashMap<>();
        Map<Integer, Set<DataProduct>> providerContainer = new HashMap<>();
        Map<Integer, List<String>> providerContainerCost = new HashMap<>();
        for(int provider = 0; provider < 9; provider++){
            Set<DataProduct> products = new HashSet<>();
            providerContainer.put(provider, products);
            List<String> productsIDCost = new ArrayList<>();
            providerContainerCost.put(provider, productsIDCost);
        }

        //collect provider amount and price
        Set<DataProduct> duplicatedProducts = new HashSet<>();
        for(DataProduct product: currentProductLis){
            List<Integer> providerIndex = product.getProvidersList();

            if (providerIndex.size() > 1){
                duplicatedProducts.add(product);
            }
            for(int provider: providerIndex){
                amount[provider]++;
                providerContainer.get(provider).add(product);
            }
        }

        for(int provider = 0 ; provider < 9 ; provider ++){
            sumCost[provider] = priceManager.get(provider).getPrice(providerContainer.get(provider), amount[provider]);
        }


        for(DataProduct product: duplicatedProducts){
            List<Integer> providerIndex = product.getProvidersList();
            float min_price = Float.MAX_VALUE;
            for (int provider: providerIndex){
                float price = priceManager.get(provider).getInitPrice(product);
                if (price < min_price){
                    min_price = price;
                }
            }
            productsCost.put(product, min_price);
        }

        List<Map.Entry<DataProduct, Float>> list = new LinkedList<Map.Entry<DataProduct, Float>>(productsCost.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<DataProduct, Float>>() {
            @Override
            public int compare(Map.Entry<DataProduct, Float> o1, Map.Entry<DataProduct, Float> o2) {
                return o1.getValue().compareTo(o2.getValue()); // ascending order
            }
        });
        productsCost.clear();
        for (Map.Entry<DataProduct, Float> ele: list){
            productsCost.put(ele.getKey(), ele.getValue());
        }
        for (Map.Entry<DataProduct, Float> dpProduct: productsCost.entrySet()){
            List<Integer> providerIndex = dpProduct.getKey().getProvidersList();

            float min_reduction = Float.MAX_VALUE;
            int keeper = 10;
            for (int provider : providerIndex){
                Set<DataProduct> estContainer = new HashSet<>(providerContainer.get(provider));
                estContainer.remove(dpProduct.getKey());
                long estAmount = amount[provider]- 1;
                float diff =  sumCost[provider] -
                        priceManager.get(provider).getPrice(estContainer, estAmount);
                if (diff < min_reduction){
                    keeper = provider;
                    min_reduction = diff;
                }
            }
            for (int provider : providerIndex){
                if (provider != keeper){
                    providerContainer.get(provider).remove(dpProduct.getKey());
                    amount[provider] -= 1;
                    sumCost[provider] = priceManager.get(provider).getPrice(providerContainer.get(provider), amount[provider]);
                }
            }
        }

        for (int i = 0 ; i < 9 ; i ++){
            cost += sumCost[i];
        }
        return cost;
    }


    class Entry {
        public float value;
        public float price;
        public DataBundle bundle;
        public Set<DataProduct> products;
        public Map<DataProduct, List<Integer>> provider = new HashMap<>(); //store the current provider of product

        public Entry(DataBundle bundle) {
            this.bundle = bundle;
            //this.value = bundle.getValue();
            this.value = 1;
            updatePrices();
        }

        /**
         * the solution update its price, based on chosen products in includeList
         * and the amount of each provider.
         * for greedy algorithm, it always chooses cheapest combination of products
         * for dynamic programming: not sure yet
         * @param includeList
         */
        public void updateDuplicatedPrice(Set<DataProduct> includeList, float currentCost){
            float currentMinimalPrice = Float.MAX_VALUE;
            float preCost = currentCost;
            Set<DataProduct> solutionProduct = new HashSet<>();

            for (Set<DataProduct> products : this.bundle.getProductsList()) {
                solutionProduct.addAll(products);
            }
            solutionProduct.addAll(includeList);
            this.price = getCompressedCost(this, solutionProduct);

        }

        public void updatePrices(){
            //System.out.println("Initialize entry of allocation algorithm");
            float currentMinimalPrice = Float.MAX_VALUE;
            Set<DataProduct> solutionProduct = new HashSet<>();

            for (Set<DataProduct> products : bundle.getProductsList())
                solutionProduct.addAll(products);

            this.price = getCompressedCost(this, solutionProduct);

            this.products = solutionProduct;
        }

        public float getUtility() {
            return value - price;
        }


    }

    private class EntryComparator implements Comparator<Entry> {

        @Override
        public int compare(Entry o1, Entry o2) {
            int x = (int)Math.signum(o2.price - o1.price);
            int y = (int)Math.signum(o2.value - o1.value);
            if(x == 0)
                return y;
            return x;
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

