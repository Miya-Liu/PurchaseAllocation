package ch.uzh.ifi.tg.serviceMarket.solver;

import ch.uzh.ifi.tg.serviceMarket.market.PriceManager;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataBundle;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataProduct;
import ilog.concert.IloException;
import org.apache.jena.base.Sys;
import org.apache.jena.reasoner.rulesys.builtins.Product;


import java.util.*;

public class GreedyNaiveSolver implements Solver {
    List<GreedyNaiveSolver.Entry> openList;
    public PriceManager priceManager;// solver needs a price manager to know how to calculate a new price
    int[] providerProductsAmount = {0,0,0,0,0};

    private float value;
    private float price;

    public GreedyNaiveSolver(PriceManager priceManager) {
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
    public void addBundles(List<DataBundle> dataBundles) throws IloException {
        for (DataBundle dataBundle: dataBundles){
            addBundle(dataBundle);
        }
    }

    public void initValue(List<GreedyNaiveSolver.Entry> eList){
        float percentage = 0;
        int amount = eList.size();
        double quaterValue = amount*0.25;
        int counter;
        for(counter = 0; counter < quaterValue; counter++){
            eList.get(counter).value += 0.25f;
        }
        //for(int i = 0; i < quaterValue; i++){
        //    eList.get(counter+i).value += 0.5f;
        //}
        //counter += quaterValue;
        //for(int i = 0; i < quaterValue; i++){
        //    eList.get(counter+i).value += 0.75f;
        //}
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

                    Map<DataProduct, Integer> providers = highestUtilityRatioEntry.provider;
                    for (DataProduct product: highestUtilityRatioEntry.products){
                        int index = providers.get(product);
                        if(excludeList.contains(product))
                            continue;
                        product.updateProviderIndex(index);
                        providerProductsAmount[index]++;
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

    public void printDataProducts(Set<DataProduct> excludeSet){
        System.out.println("\tProducts:");
        int count = 0;
        for (DataProduct product: excludeSet){
            count++;
            System.out.println("\t\t"+count);
            System.out.println("\t\t\tIdentifier:"+product.getIdentifier());
            System.out.println("\t\t\tProvider:"+product.getProviderIndex());
            System.out.println("\t\t\tCost:"+product.getCost());
        }
    }


    class Entry {
        public float value;
        public float price;
        public DataBundle bundle;
        public Set<DataProduct> products;
        public Map<DataProduct, Integer> provider = new HashMap<>(); //store the current provider of product

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

            for (Set<DataProduct> products : bundle.getProductsList()) {
                solutionProduct.addAll(products);
            }
            solutionProduct.addAll(includeList);
            //this.price = getCostFlat(solutionProduct);
            this.price = getCostFreemium(solutionProduct);
        }

        public void updatePrices(){
            //System.out.println("Initialize entry of allocation algorithm");
            float currentMinimalPrice = Float.MAX_VALUE;
            Set<DataProduct> solutionProduct = new HashSet<>();

            for (Set<DataProduct> products : bundle.getProductsList())
                solutionProduct.addAll(products);

            for (DataProduct product: solutionProduct){
                this.provider.put(product, 0);
            }
            //this.price = getCostFlat(solutionProduct);
            this.price = getCostFreemium(solutionProduct);

            this.products = solutionProduct;
        }

        public float getCostFlat(Set<DataProduct> currentProductLis) {
            float cost = 0;
            long[] amount = {0, 0};
            float[] sumCost = {0, 0};
            float difference = 0;
            float same = 0;

            Map<Integer, List<String>> providerContainer = new HashMap<>();
            Map<Integer, List<String>> providerContainerCost = new HashMap<>();
            for (int provider = 0; provider < 2; provider++) {
                List<String> productsID = new ArrayList<>();
                providerContainer.put(provider, productsID);
                List<String> productsIDCost = new ArrayList<>();
                providerContainerCost.put(provider, productsIDCost);
            }

            //collect provider amount and price
            for (DataProduct product : currentProductLis) {
                List<Integer> providerIndex = product.getProvidersList();
                for (int provider : providerIndex) {
                    amount[provider]++;
                    float price = priceManager.getPrice(product.getIdentifier(), provider, amount[provider]);
                    providerContainer.get(provider).add(product.getIdentifier());
                    String productCost = product.getIdentifier() + "_" + price;
                    providerContainerCost.get(provider).add(productCost);
                    sumCost[provider] += price;
                }
            }
            if (amount[0] == currentProductLis.size() && sumCost[0] <= sumCost[1]) {
                cost = sumCost[0];
                for (DataProduct product: currentProductLis)
                    product.updateProviderIndex(0);//all come from 0

                return cost;//random source answer and cheap
            }
            if (amount[1] == currentProductLis.size() && sumCost[1] <= sumCost[0]) {
                cost = sumCost[1];
                for (DataProduct product: currentProductLis) {
                    product.updateProviderIndex(1);//all come from 1
                    this.provider.put(product, 1);
                }
                return cost;//flat source answer and cheap
            }

            //to check the union of both of them
            float flatRate = sumCost[1] / amount[1];
            List<String> products = providerContainer.get(1);//flat
            List<String> otherProdcuts = providerContainer.get(0);//random
            List<String> otherProdcutsCost = providerContainerCost.get(0);

            List<String> sameProducts = new ArrayList<>();

            int index = 0;
            long countSame = 0;
            long countDifferent = 0;
            for (String s : otherProdcuts) {
                String s_Cost = otherProdcutsCost.get(index);
                String[] temp = s_Cost.split("_");

                if (products.contains(s)) {
                    same += Float.parseFloat(temp[2]);
                    sameProducts.add(s);
                    countSame++;
                } else {
                    difference += Float.parseFloat(temp[2]);
                    countDifferent++;
                }
                index++;
            }
            if (countSame == 0) {
                cost = sumCost[0] + sumCost[1];
                for (String s: providerContainer.get(1)){
                    for (DataProduct product: currentProductLis){
                        if(product.getIdentifier() == s)
                            this.provider.put(product, 1);
                    }
                }
                return cost;//no intersection
            }
            if (countSame == amount[1]) {
                //flat is subset of random
                float randomRate = same / countSame;
                if (flatRate < randomRate) {
                    cost = sumCost[1] + difference;
                    for (String s: sameProducts){
                        for (DataProduct product: currentProductLis){
                            if(product.getIdentifier() == s)
                                this.provider.put(product, 1);
                        }
                    }
                    return cost;
                }
                cost = sumCost[0];
                return cost;
            }
            if (countSame == amount[0]) {
                //random is the subset of flat
                cost = sumCost[1];
                for (DataProduct product: currentProductLis){
                    this.provider.put(product, 1);
                }
                return cost; // random cost will never cheap than 0
            }
            cost = sumCost[1] + difference;
            for (String s: sameProducts){
                for (DataProduct product: currentProductLis){
                    if(product.getIdentifier() == s)
                        this.provider.put(product, 1);
                }
            }
            return cost;
        }

        public float getCostFreemium(Set<DataProduct> currentProductList){
            float cost = 0;
            long[] amount = {0,0};
            float[] sumCost = {0,0};
            float difference = 0;
            float same = 0;

            Map<String, Float> productsCost = new HashMap<>();
            Map<Integer, List<String>> providerContainer = new HashMap<>();
            Map<Integer, List<String>> providerContainerCost = new HashMap<>();
            for(int provider = 0; provider < 2; provider++){
                List<String> productsID = new ArrayList<>();
                providerContainer.put(provider, productsID);
                List<String> productsIDCost = new ArrayList<>();
                providerContainerCost.put(provider, productsIDCost);
            }
            //sort the product list by cost from 0
            List<DataProduct> sortedList = new ArrayList<>();
            sortedList.addAll(currentProductList);
            sortedList.sort(new ProductComparator());
            //collect provider amount and price
            for(DataProduct product: sortedList){
                List<Integer> providerIndex = product.getProvidersList();
                for(int provider: providerIndex){
                    amount[provider]++;
                    float price = priceManager.getPrice(product.getIdentifier(), provider, amount[provider]);
                    providerContainer.get(provider).add(product.getIdentifier());
                    String productCost = product.getIdentifier() + "_" + price;
                    providerContainerCost.get(provider).add(productCost);
                    sumCost[provider] += price;
                }
            }
            //sort triples from 0 by price

            if (amount[0] == currentProductList.size() && sumCost[0] <= sumCost[1]){
                cost = sumCost[0];
                return cost;//random source answer and cheap
            }
            if (amount[1] == currentProductList.size() && sumCost[1] <= sumCost[0]){
                cost = sumCost[1];
                return cost;//flat source answer and cheap
            }

            List<String> products_0 = providerContainer.get(0);//flat
            List<String> otherProdcuts_1 = providerContainer.get(1);//random
            long B_A = 0; // in B but not A

            for (String s : otherProdcuts_1) {
                if (!products_0.contains(s)) {
                    B_A++;
                }
            }

            //to check the union of both of them
            long n = priceManager.getXPoint();
            n = n - B_A;

            List<String> products = providerContainer.get(1);//flat
            List<String> otherProdcuts = providerContainer.get(0);//random
            List<String> otherProdcutsCost = providerContainerCost.get(0);
            int index = 0;
            long countConj = 0;
            long countSame = 0;
            long countDifferent = 0;

            for (String s : otherProdcuts) {
                String s_Cost = otherProdcutsCost.get(index);
                String[] temp = s_Cost.split("_");

                if (products.contains(s)) {
                    if(n<=0) {
                        same += Float.parseFloat(temp[2]); //
                        countSame++;//not free part
                    }
                    else
                        sumCost[0] -= Float.parseFloat(temp[2]);
                    n--;
                    countConj++;
                } else {
                    difference += Float.parseFloat(temp[2]); //
                    countDifferent++;
                }
                index++;
            }
            if(B_A > priceManager.getXPoint()) { //must buy not free triples from B
                cost = sumCost[1] + difference;
                return cost;
            }
            if(countConj == 0){
                cost = sumCost[0]+sumCost[1];
                return cost;//no intersection
            }
            if(countConj == amount[1] && amount[1] < amount[0]){
                if(countSame == 0){
                    cost = difference;
                    return cost;
                }
                if(same < sumCost[1]){
                    cost = same + difference;
                    return cost;
                }
                cost = difference + sumCost[1];
                return cost;

            }
            if(countConj == amount[0] && amount[0] < amount[1]){
                if(countSame == 0){
                    cost = sumCost[1];
                    return cost;
                }
                if(sumCost[0]>sumCost[1]){
                    cost = sumCost[1];
                    return cost;
                }
                if(same > sumCost[1]){
                    cost = sumCost[1];
                    return cost;
                }
                if(same <= sumCost[1]){
                    cost = same;
                    return cost;
                }
            }
            if(countConj < amount[0] && countConj < amount[1]){
                //difference cost
                //intersection
                //split the not free part of 1
                //if not free is empty
                //return
                if(countSame == 0){
                    cost = sumCost[1] + difference;
                    return cost;
                }
                if(same > sumCost[1]){
                    cost = sumCost[1] + difference;
                    return cost;
                }
                if(same <= sumCost[1]){
                    cost = same + difference;
                    return cost;
                }
            }
            return cost;
        }


        public float[] updateProviderPrice(String productID, List<Integer> providers){
            float[] priceAndProvider = {0,0};
            float providerPrice = Float.MAX_VALUE;
            float index = 0;
            float currentPrice = 0;
            for (int p: providers) {
                long a = providerProductsAmount[p];
                a++;
                currentPrice = priceManager.getPrice(productID, p, a);
                if(currentPrice < providerPrice) {
                    providerPrice = currentPrice; //choose cheapest providers
                    index = p;
                }
            }
            priceAndProvider[0] = providerPrice;
            priceAndProvider[1] = index;
            return priceAndProvider;
        }

        public float[] initProviderPrice(String productID, List<Integer> providers){
            long[] amount = {0,0};
            float[] priceAndProvider = {0,0};
            float providerPrice = Float.MAX_VALUE;
            float index = 0;
            float currentPrice = 0;
            for (int p: providers) {
                long a = providerProductsAmount[p];
                currentPrice = priceManager.getInitPrice(productID, p, a);
                if(currentPrice < providerPrice) {
                    providerPrice = currentPrice; //choose cheapest providers
                    index = p;
                }
            }
            priceAndProvider[0] = providerPrice;
            priceAndProvider[1] = index;
            return priceAndProvider;
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

        public void printEntry(){
            System.out.println("Entry: "+ bundle + "\tvalue: " + value + "\tprice: " + "\tproducts: " + products + "\tprovider: " + provider);
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

    class ProductComparator implements Comparator<DataProduct> {
        @Override
        public int compare(DataProduct o1, DataProduct o2) {
            return (int)Math.signum(priceManager.getPrice(o2.getIdentifier()) - priceManager.getPrice(o1.getIdentifier()));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ProductComparator) {
                return true;
            }
            return false;
        }
    }


}

