package ch.uzh.ifi.tg.serviceMarket.solver;

import ch.uzh.ifi.tg.serviceMarket.market.PriceManager;
import ch.uzh.ifi.tg.serviceMarket.market.ValueFunction;
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
    public List<PriceManager> priceManager;// solver needs a price manager to know how to calculate a new price
    int[] providerProductsAmount = {0,0,0,0,0};

    private float value;
    private float price;
    private ValueFunction valueFunction;

    public NaiveSolver(List<PriceManager> priceManager) {
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

    public void initValue(List<NaiveSolver.Entry> eList){
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

        initValue(openList);
        List<DataBundle> entireAnswers = new ArrayList<>();

        for(Entry e: openList) {
            e.bundle.updateValue(e.value);
            entireAnswers.add(e.bundle);
        }

        List<DataBundle> naiveAnswer = new ArrayList<>();
        float curr_cost = 0;
        while(curr_cost < budget){
            int amount = naiveAnswer.size();
            amount++;
            float answer_cost = Float.MAX_VALUE;
            float utility = Float.MIN_VALUE;
            List<DataBundle> curr_answer = new ArrayList<>();

            List<List<DataBundle>> posb_answers = getCombinations(entireAnswers, amount);
            for(List<DataBundle> an_answer: posb_answers){
                float pre_cost = answer_cost;
                float curUtility = 0;
                Set<DataProduct> curr_products = new HashSet<>();
                for(DataBundle s: an_answer){
                    for(Set<DataProduct> products: s.getProductsList()){
                        curr_products.addAll(products);
                    }
                    curUtility += s.getValue();
                }
                answer_cost = getCompressedCost(curr_products);

                if(answer_cost < budget && curUtility > utility){
                    curr_answer = an_answer;
                    utility = curUtility;
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

        for (DataBundle s: naiveAnswer){
            value += s.getValue();
        }

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


    public float getCompressedCost(Set<DataProduct> currentProductLis){
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
        public long flatAmount = 0;
        public Map<DataProduct, Integer> provider = new HashMap<>(); //store the current provider of product

        public Entry(DataBundle bundle) {
            this.bundle = bundle;
            //this.value = bundle.getValue();
            this.value = 1;
            updatePrices();
            updateProviders();
        }

        public void updatePrices(){
            //System.out.println("Initialize entry of allocation algorithm");
            float currentMinimalPrice = Float.MAX_VALUE;
            Set<DataProduct> solutionProduct = new HashSet<>();

            for (Set<DataProduct> products : bundle.getProductsList())
                solutionProduct.addAll(products);

            this.price = getCompressedCost(solutionProduct);
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

        public float getUtility() {
            return value;
        }

    }

}



