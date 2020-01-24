package ch.uzh.ifi.tg.serviceMarket.solver;

import ch.uzh.ifi.tg.serviceMarket.market.PriceManager;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataBundle;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataProduct;
import ilog.concert.IloException;
import org.apache.jena.base.Sys;

import java.util.*;
import java.util.stream.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class NaiveSolver implements Solver {
    List<NaiveSolver.Entry> openList;
    public PriceManager priceManager;// solver needs a price manager to know how to calculate a new price
    int[] providerProductsAmount = {0,0,0,0,0};

    private float value;
    private float price;

    public NaiveSolver(PriceManager priceManager) {
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

    public List<List<DataBundle>> getCombinations(List<DataBundle> entireAnswers, int amount){
        List<List<DataBundle>> conbinations = combinations(entireAnswers, amount);
        //System.out.println(conbinations.toString());
        return conbinations;
    }

    public static List<List<DataBundle>> combinations(List<DataBundle> list, int k) {
        if (k == 0 || list.isEmpty()) {//去除K大于list.size的情况。即取出长度不足K时清除此list
            return Collections.emptyList();
        }
        if (k == 1) {//递归调用最后分成的都是1个1个的，从这里面取出元素
            return list.stream().map(e -> Stream.of(e).collect(toList())).collect(toList());
        }
        Map<Boolean, List<DataBundle>> headAndTail = split(list, 1);
        List<DataBundle> head = headAndTail.get(true);
        List<DataBundle> tail = headAndTail.get(false);
        List<List<DataBundle>> c1 = combinations(tail, (k - 1)).stream().map(e -> {
            List<DataBundle> l = new ArrayList<>();
            l.addAll(head);
            l.addAll(e);
            return l;
        }).collect(toList());
        List<List<DataBundle>> c2 = combinations(tail, k);
        c1.addAll(c2);
        return c1;
    }

    public static Map<Boolean, List<DataBundle>> split(List<DataBundle> list, int n) {
        return IntStream
                .range(0, list.size())
                .mapToObj(i -> new AbstractMap.SimpleEntry<>(i, list.get(i)))
                .collect(Collectors.partitioningBy(entry -> entry.getKey() < n, Collectors.mapping(AbstractMap.SimpleEntry::getValue, toList())));
    }



    @Override
    public List<DataBundle> solve(float budget) throws IloException {
        //System.out.println("Budget: "+ budget);
        //Solution Index: results.size_budget
        //Solution results: List<DataBundle>
        //Solution products: Set<DataProduct>
        //Solution Cost: Float
        //Solution value: Float

        //write iteration, increase the number of solution mappings,
        //list possible solution mappings combination
        //calculate the cost of them and find the cheapest one
        //if the cost is lower than the budget, increase the number of solution mappings and iterate

        List<DataBundle> entireAnswers = new ArrayList<>();
        for(Entry e: openList)
            entireAnswers.add(e.bundle);

        List<DataBundle> naiveAnswer = new ArrayList<>();
        float curr_cost = 0;
        while(curr_cost < budget){
            int amount = naiveAnswer.size();
            amount++;
            float answer_cost = Float.MAX_VALUE;
            List<DataBundle> curr_answer = new ArrayList<>();

            List<List<DataBundle>> posb_answers = getCombinations(entireAnswers, amount);
            for(List<DataBundle> an_answer: posb_answers){
                float pre_cost = answer_cost;
                Set<DataProduct> curr_products = new HashSet<>();
                for(DataBundle s: an_answer){
                    for(Set<DataProduct> products: s.getProductsList()){
                        curr_products.addAll(products);
                    }
                }
                answer_cost = getCostFlat(curr_products);
                if(answer_cost < pre_cost){
                    curr_answer = an_answer;
                }
                else {
                    answer_cost = pre_cost;
                }
            }
            if(answer_cost > budget)
                break;
            curr_cost = answer_cost;
            naiveAnswer = curr_answer;
        }

        value = 1;
        price = curr_cost;
        //System.out.println(dyanmicResults.get(finalCase).size());
        List<DataBundle> result = naiveAnswer;

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

    public float getCostFlat(Set<DataProduct> currentProductLis){
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

        //collect provider amount and price
        for(DataProduct product: currentProductLis){
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
        if (amount[0] == currentProductLis.size() && sumCost[0] <= sumCost[1]){
            cost = sumCost[0];
            return cost;//random source answer and cheap
        }
        if (amount[1] == currentProductLis.size() && sumCost[1] <= sumCost[0]){
            cost = sumCost[1];
            return cost;//flat source answer and cheap
        }

        //to check the union of both of them
        float flatRate = sumCost[1] / amount[1];
        List<String> products = providerContainer.get(1);//flat
        List<String> otherProdcuts = providerContainer.get(0);//random
        List<String> otherProdcutsCost = providerContainerCost.get(0);
        int index = 0;
        long countSame = 0;
        long countDifferent = 0;
        for (String s : otherProdcuts) {
            String s_Cost = otherProdcutsCost.get(index);
            String[] temp = s_Cost.split("_");

            if (products.contains(s)) {
                same += Float.parseFloat(temp[2]);
                countSame++;
            } else {
                //System.out.println(Float.parseFloat(temp[2]));
                difference += Float.parseFloat(temp[2]);
                countDifferent++;
            }
            index++;
        }
        if(countSame == 0){
            cost = sumCost[0]+sumCost[1];
            return cost;//no intersection
        }
        if(countSame == amount[1]){
            //flat is subset of random
            float randomRate = same / countSame;
            if (flatRate < randomRate){
                cost = sumCost[1] + difference;
                return cost;
            }
            cost = sumCost[0];
            return cost;
        }
        if (countSame == amount[0]){
            //random is the subset of flat
            cost = sumCost[1];
            return cost; // random cost will never cheap than 0
        }
        cost = sumCost[1] + difference;
        return cost;
    }

    public float getCostFreemium(Entry e, Set<DataProduct> currentProductList){
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
        if (amount[0] == currentProductList.size() && sumCost[0] <= sumCost[1]){
            cost = sumCost[0];
            return cost;//random source answer and cheap
        }
        if (amount[1] == currentProductList.size() && sumCost[1] <= sumCost[0]){
            cost = sumCost[1];
            return cost;//flat source answer and cheap
        }

        //to check the union of both of them
        long n = this.priceManager.getXPoint();
        float flatRate = 0;
        if(sumCost[1]>0) {
            flatRate = sumCost[1] / (amount[1] - n);
        }

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
                if(n<0) {
                    same += Float.parseFloat(temp[2]);
                    countSame++;//not free part
                }
                else
                    sumCost[0] -= Float.parseFloat(temp[2]);
                n--;
                countConj++;
            } else {
                difference += Float.parseFloat(temp[2]);
                countDifferent++;
            }
            index++;
        }
        if(countConj == 0){
            cost = sumCost[0]+sumCost[1];
            return cost;//no intersection
        }
        if(countSame < amount[1]){
            //flat is subset of random
            float randomRate = same / countSame;
            if (flatRate < randomRate){
                cost = sumCost[1] + difference;
                return cost;
            }
            cost = sumCost[0];
            return cost;
        }
        if (countDifferent == 0){
            //random is the subset of flat
            if (amount[0] < amount[1]-n)
                return sumCost[1];
            float randomRate = same / countSame;
            if (flatRate < randomRate)
                return sumCost[1];
            cost = sumCost[0];
            return cost; // random cost will never cheap than 0
        }
        cost = sumCost[1] + difference;
        return cost;
    }


    class Entry {
        public float value;
        public float price;
        public DataBundle bundle;
        public Set<DataProduct> products;
        public long flatAmount = 0;
        public Map<DataProduct, Integer> provider = new HashMap<>(); //store the current provider of product

        public Entry(DataBundle bundle) {
            this.bundle = bundle;
            //this.value = bundle.getValue();
            this.value = 12;
            updatePrices();
            updateProviders();
        }

        /**
         * the solution update its price, based on chosen products in includeList
         * and the amount of each provider.
         * for greedy algorithm, it always chooses cheapest combination of products
         * for dynamic programming: not sure yet
         * @param includeList
         */
        public void updateDuplicatedPrice(Set<DataProduct> includeList){
            float currentMinimalPrice = Float.MAX_VALUE;
            for (Set<DataProduct> products : bundle.getProductsList()) {
                Map<String, List<Integer>> providersMap = new HashMap<>();
                providersMap = bundle.getProviders();
                float price = 0;
                for (DataProduct product : products) { //all related products
                    if (includeList == null || !includeList.contains(product)) {
                        String productID = product.getIdentifier();
                        List<Integer> providers = providersMap.get(productID);
                        float[] priceAndProvider = updateProviderPrice(productID, providers);
                        product.updateCost(priceAndProvider[0]);
                        price += priceAndProvider[0];
                        product.updateProviderIndex(Math.round(priceAndProvider[1]));
                        provider.put(product, Math.round(priceAndProvider[1])); //save current provider of the product
                    }
                }
                if (price < currentMinimalPrice) {
                    currentMinimalPrice = price;
                    this.products = products;
                }
            }
            this.price = currentMinimalPrice;
        }

        public void updatePrices(){
            //System.out.println("Initialize entry of allocation algorithm");
            float currentMinimalPrice = Float.MAX_VALUE;
            Set<DataProduct> solutionProduct = new HashSet<>();

            for (Set<DataProduct> products : bundle.getProductsList())
                solutionProduct.addAll(products);

            this.price = getCostFlat(solutionProduct);
            this.products = solutionProduct;
        }

        public void updateProviders(){
            Set<DataProduct> solutionProduct = new HashSet<>();

            for (Set<DataProduct> products : bundle.getProductsList())
                solutionProduct.addAll(products);

            for (DataProduct product: solutionProduct){
                List<Integer> providerIndex = product.getProvidersList();
                for (int provider : providerIndex) {
                    if(provider == 1)
                        this.flatAmount++;
                }
            }
        }

        public float[] updateProviderPrice(String productID, List<Integer> providers){
            float[] priceAndProvider = {0,0};
            float providerPrice = Float.MAX_VALUE;
            float BudgetIndex = 0;
            float currentPrice = 0;
            for (int p: providers) {
                long a = providerProductsAmount[p];
                currentPrice = priceManager.getPrice(productID, p, a);
                if(currentPrice < providerPrice) {
                    providerPrice = currentPrice; //choose cheapest providers
                    BudgetIndex = p;
                }
            }
            priceAndProvider[0] = providerPrice;
            priceAndProvider[1] = BudgetIndex;
            return priceAndProvider;
        }

        public float getUtility() {
            return value;
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

    class EntryComparator_1 implements Comparator<Entry> {
            @Override
            public int compare(Entry o1, Entry o2) {
                int x = (int)Math.signum(o1.flatAmount - o2.flatAmount);
                int y = (int)Math.signum(o2.price - o1.price);
                if(x == 0)
                    return y;
                return x;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj instanceof EntryComparator_1) {
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



