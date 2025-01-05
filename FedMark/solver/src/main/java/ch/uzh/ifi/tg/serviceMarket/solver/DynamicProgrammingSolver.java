package ch.uzh.ifi.tg.serviceMarket.solver;

import ch.uzh.ifi.tg.serviceMarket.market.PriceManager;
import ch.uzh.ifi.tg.serviceMarket.market.ValueFunction;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataBundle;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataProduct;
import ilog.concert.IloException;

import java.util.*;

public class DynamicProgrammingSolver implements Solver {
    List<DynamicProgrammingSolver.Entry> openList;
    public List<PriceManager> priceManager;// solver needs a price manager to know how to calculate a new price
    int[] providerProductsAmount = {0,0,0,0,0};

    private float value;
    private float price;
    public ValueFunction valueFunction;

    public DynamicProgrammingSolver(List<PriceManager> priceManager) {
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

    public void initValue(List<DynamicProgrammingSolver.Entry> eList){
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
        openList.sort(new EntryComparator_1());
        //System.out.println("Budget: "+ budget);
        //Solution Index: results.size_budget
        //Solution results: List<DataBundle>
        //Solution products: Set<DataProduct>
        //Solution Cost: Float
        //Solution value: Float
        Map<String, Set<DataBundle>> dyanmicResults = new HashMap<>();
        Map<String, Set<DataProduct>> dynamicProductResults = new HashMap<>();
        Map<String, Float> dynamicValue = new HashMap<>();
        Map<String, Float> dynamicCost = new HashMap<>();
        Map<String, List<Float>> dynamicSolutionBudget = new HashMap<>();
        List<DataBundle> result = new ArrayList<>();
        Set<Float> variedBudgets = new HashSet<>();

        float budgetStep = 0.5f;
        //System.out.println("Iteration starts >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        while (openList.size() > 0) {

            boolean entryFound = false;
            while (!entryFound && openList.size() > 0) {
                Set<DataProduct> productList = new HashSet<>();//store selected products product
                Set<DataProduct> inProductList = new HashSet<>();
                Set<DataProduct> tmpProductList = new HashSet<>();
                Set<DataProduct> caseProductList = new HashSet<>();
                Set<DataBundle> solutionList = new HashSet<>();
                float formerCost;
                float caseCost = 0;
                float caseBudget = 0;

                Entry highestUtilityRatioEntry = openList.remove(openList.size() - 1);
                float cost = 0; //get cost of the entry
                Set<DataProduct> currentProductList = new HashSet<>();
                for(Set<DataProduct> products:highestUtilityRatioEntry.bundle.getProductsList()){
                    currentProductList.addAll(products);
                }
                cost = getCompressedCost(highestUtilityRatioEntry, currentProductList);

                List<DataBundle> formerList = new ArrayList<>(result);
                result.add(highestUtilityRatioEntry.bundle);
                if(cost < budget) {
                    variedBudgets.add(cost);
                }
                variedBudgets = sortFloatSet(variedBudgets);

                //initialize
                String newCase = "" + result.size() + "_" + 0f;
                String newBudget = "" + result.size();
                caseBudget = 0;
                List<Float> budgetList = new ArrayList<>();
                budgetList.add(caseBudget);
                dynamicSolutionBudget.put(newBudget, budgetList);
                //init 0 case
                if(cost == 0){
                    Set<DataProduct> tp = highestUtilityRatioEntry.products;
                    dynamicProductResults.put(newCase, tp);
                    Set<DataBundle> tmp = new HashSet<>();
                    tmp.add(highestUtilityRatioEntry.bundle);
                    dyanmicResults.put(newCase, tmp);
                    dynamicCost.put(newCase, cost);
                    dynamicValue.put(newCase, highestUtilityRatioEntry.value);
                }
                else {
                    Set<DataProduct> tp = new HashSet<>();
                    dynamicProductResults.put(newCase, tp);
                    Set<DataBundle> tmp = new HashSet<>();
                    //tmp.add(highestUtilityRatioEntry.bundle);
                    dyanmicResults.put(newCase, tmp);
                    dynamicCost.put(newCase, 0f);
                    dynamicValue.put(newCase, 0f);
                }

                float varideB = 0;
                List<Float> midBudgets = new ArrayList<>(variedBudgets);
                Set<DataProduct> curPreProcuts = new HashSet<>();
                Set<DataBundle> curPreResults = new HashSet<>();
                float curPreValue = 0;
                float curPreCost = 0;
                for(int i = 0; i < midBudgets.size(); i++){
                    varideB = midBudgets.get(i);
                    if(cost <= varideB)
                        break;
                    else {
                        //init corrent record with former record
                        int len = formerList.size();
                        String preCase = "" + len + "_" + varideB;
                        String cur = "" + result.size();
                        while(len >= 0){
                            if(dyanmicResults.get(preCase) != null && dyanmicResults.get(preCase).size() >= 0)
                                break;
                            len--;
                            preCase = "" + len + "_" + varideB;
                        }
                        dynamicSolutionBudget.get(cur).add(varideB);
                        String curCase = "" + result.size() + "_" + varideB;
                        if(dynamicValue.get(preCase) > curPreValue) {
                            curPreProcuts = dynamicProductResults.get(preCase);
                            dynamicProductResults.put(curCase, curPreProcuts);
                            curPreResults = dyanmicResults.get(preCase);
                            dyanmicResults.put(curCase, curPreResults);
                            curPreCost = dynamicCost.get(preCase);
                            dynamicCost.put(curCase, curPreCost);
                            curPreValue = dynamicValue.get(preCase);
                            dynamicValue.put(curCase, curPreValue);
                        }else {
                            //curPreProcuts = dynamicProductResults.get(preCase);
                            dynamicProductResults.put(curCase, curPreProcuts);
                            //curPreResults = dyanmicResults.get(preCase);
                            dyanmicResults.put(curCase, curPreResults);
                            //curPreCost = dynamicCost.get(preCase);
                            dynamicCost.put(curCase, curPreCost);
                            //curPreValue = dynamicValue.get(preCase);
                            dynamicValue.put(curCase, curPreValue);
                        }

                        if(dynamicProductResults.get(preCase) == null)
                            continue;
/*
                        System.out.println("Init with previewers: " + curCase);
                        System.out.println("\t Products:");
                        for (DataProduct p: curPreProcuts){
                            System.out.println("\t \t: " + p.getIdentifier());
                        }
                        System.out.println("\t Solutions:");
                        for (DataBundle s: curPreResults){
                            System.out.println("\t \t: " + s.getBindings().toString());
                        }
                        System.out.println("\t Value: " + curPreValue);
                        System.out.println("\t Cost: " + curPreCost);
                        System.out.println();
*/
                    }
                }
                if(varideB > budget) {
                    String preCase = "" + formerList.size() + "_" + budget;
                    String cur = "" + result.size();
                    dynamicSolutionBudget.get(cur).add(budget);
                    String curCase = "" + result.size() + "_" + budget;
                    dynamicProductResults.put(curCase, dynamicProductResults.get(preCase));
                    dyanmicResults.put(curCase, dyanmicResults.get(preCase));
                    dynamicCost.put(curCase, dynamicCost.get(preCase));
                    dynamicValue.put(curCase, dynamicValue.get(preCase));
                    continue;
                }

                float currentUtility = 0;
                if (highestUtilityRatioEntry.getUtility() > 0 ) {
                    float b = cost;
                    while(b < budget + 0.2) {
                        //System.out.println("Current Budegt: " + b);
                        variedBudgets.add(b);
                        variedBudgets = sortFloatSet(variedBudgets);

                        float currentValue = highestUtilityRatioEntry.value;
                        float costIncrease = 0.1f;
                        float newCost = 0;
                        Set<DataBundle> formerResult = new HashSet<>();
                        Set<DataBundle> currentResult = new HashSet<>();
                        formerCost = 0;
                        caseProductList = new HashSet<>();
                        solutionList = new HashSet<>();
                        inProductList = new HashSet<>();

                        //f[s-1, b]
                        String former = "" + formerList.size();
                        String formerCase = "";
                        if (dynamicSolutionBudget.get(former) != null){
                            List<Float> budgetIncrease = dynamicSolutionBudget.get(former);
                            for(int i = 0; i < budgetIncrease.size(); i++){
                                if(budgetIncrease.get(i) > b){
                                    if(i != 0)
                                        formerCase = "" + formerList.size() + "_" + budgetIncrease.get(i-1);
                                    break;
                                }
                                else
                                    if (budgetIncrease.get(i) + budgetStep > b){
                                        formerCase = "" + formerList.size() + "_" + budgetIncrease.get(i);
                                        break;
                                    }
                            }
                        }
                        float excludeCurrentSolutionValue = 0;
                        if (dynamicProductResults.get(formerCase) != null) {
                            productList = dynamicProductResults.get(formerCase);
                            formerResult = dyanmicResults.get(formerCase);
                            excludeCurrentSolutionValue = dynamicValue.get(formerCase);
                        }
/*
                        System.out.println("Former Info:" + formerCase);
                        System.out.println("\t Products:");
                        for (DataProduct p: productList){
                            System.out.println("\t \t: " + p.getIdentifier());
                        }
                        System.out.println("\t Solutions:");
                        for (DataBundle s: formerResult){
                            System.out.println("\t \t: " + s.getBindings().toString());
                        }
                        System.out.println("\t Value: " + excludeCurrentSolutionValue);
                        System.out.println();
*/
                        //f[s-1, b-cost]
                        float bDecrease = b-cost;
                        String includeCase = "";
                        String include = "" + formerList.size();
                        float includeCurrentSolutionValue = highestUtilityRatioEntry.value;
                        float preValue = 0;
                        float tmpCost = 0;
                        currentResult.add(highestUtilityRatioEntry.bundle);
                        inProductList.addAll(currentProductList);
                        Set<DataBundle> tmpResults = new HashSet<>();
                        Set<DataProduct> iterationProducts = new HashSet<>();
                        while(costIncrease >= 0) {
                            if (dynamicSolutionBudget.get(include) != null) {
                                List<Float> budgetIncrease = dynamicSolutionBudget.get(include);
                                for (int i = 0; i < budgetIncrease.size(); i++) {
                                    if (budgetIncrease.get(i) + 0.01> bDecrease) {
                                        includeCase = "" + formerList.size() + "_" + budgetIncrease.get(i);
                                        break;
                                    }/* else if (budgetIncrease.get(i+1) > bDecrease) {
                                        includeCase = "" + formerList.size() + "_" + budgetIncrease.get(i);
                                        break;
                                    }*/
                                }
                                if (includeCase == ""){
                                    includeCase = "" + formerList.size() + "_" + budgetIncrease.get(budgetIncrease.size()-1);//use the last one
                                }
                            }

                            if (dynamicProductResults.get(includeCase) != null) {
                                tmpProductList = new HashSet<>();
                                tmpProductList.addAll(currentProductList);
                                tmpProductList.addAll(dynamicProductResults.get(includeCase));
                                tmpCost = getCompressedCost(highestUtilityRatioEntry, tmpProductList);

                                formerCost = dynamicCost.get(includeCase);
                                if (tmpCost > b) {
                                    bDecrease += 0.05;
                                    if( (b + budgetStep - bDecrease) < 0) {
                                        break;
                                    }
                                    continue;
                                }

                                tmpResults = dyanmicResults.get(includeCase);
                                //tmpResults.addAll(dyanmicResults.get(includeCase));
                                iterationProducts = tmpProductList;
                                //iterationProducts.addAll(tmpProductList);
                                newCost = tmpCost;
                                preValue = dynamicValue.get(includeCase);
                                includeCurrentSolutionValue = preValue + currentValue;

                                bDecrease += budgetStep;
                                if( (b + budgetStep - bDecrease) < 0)
                                    break;
                            } else {
                                includeCurrentSolutionValue = currentValue;
                                newCost = cost;
                                break;
                            }
                        }
                        //includeCurrentSolutionValue = currentResult.size()*highestUtilityRatioEntry.value - newCost;
                        inProductList.addAll(iterationProducts);
                        currentResult.addAll(tmpResults);
/*
                        System.out.println("Include Case Info:" +includeCase);
                        System.out.println("\t Products:");
                        for (DataProduct p: inProductList){
                            System.out.println("\t \t: " + p.getIdentifier());
                        }
                        System.out.println("\t Solutions:");
                        for (DataBundle s: currentResult){
                            System.out.println("\t \t: " + s.getBindings().toString());
                        }
                        System.out.println("\t Value: " + includeCurrentSolutionValue);
                        System.out.println("\t Cost: " + newCost);
                        System.out.println();
*/
                        newCase = "" + result.size() + "_" + b;
                        if (excludeCurrentSolutionValue >= includeCurrentSolutionValue) {
                            caseProductList.addAll(productList);
                            solutionList = formerResult;
                            caseCost = formerCost;
                            currentUtility = excludeCurrentSolutionValue;
                        } else {
                            caseProductList.addAll(inProductList);
                            solutionList = currentResult;
                            caseCost = newCost;
                            currentUtility = includeCurrentSolutionValue;
                        }
                        newBudget = "" + result.size();
                        caseBudget = b;
                        dynamicSolutionBudget.get(newBudget).add(caseBudget);
                        dynamicProductResults.put(newCase, caseProductList);
                        dyanmicResults.put(newCase, solutionList);
                        dynamicCost.put(newCase, caseCost);
                        dynamicValue.put(newCase, currentUtility);
/*
                        System.out.println("New Create Info: " + newCase);
                        System.out.println("\t Products:");
                        for (DataProduct p: caseProductList){
                            System.out.println("\t \t: " + p.getIdentifier());
                        }
                        System.out.println("\t Solutions:");
                        for (DataBundle s: solutionList){
                            System.out.println("\t \t: " + s.getBindings().toString());
                        }
                        System.out.println("\t Value: " + currentUtility);
                        System.out.println("\t Cost: " + caseCost);
                        System.out.println();
*/
                        b+=budgetStep; //increase step
                    }
                    String budgetCase = "" + result.size() + "_" + budget;
                    dynamicProductResults.put(budgetCase, caseProductList);
                    dynamicCost.put(budgetCase, caseCost);
                    dynamicValue.put(budgetCase, currentUtility);
                    dyanmicResults.put(budgetCase, solutionList);
                }
            }
        }

        //System.out.println("Selected amount from 1: " + providerProductsAmount[1]);
/*
        System.out.println(dynamicProductResults.size());
        for (Map.Entry<String, List<DataBundle>> e: dyanmicResults.entrySet()){
            System.out.println(e.getKey());
            List<DataBundle> solutions = e.getValue();
            for(DataBundle solution: solutions){
                List<BindingSet> bindings = solution.getBindings();
                for (BindingSet binding: bindings)
                    System.out.println("\tsolution: " + binding.toString());
            }
        }
*/
        String finalCase = "" + result.size() + "_" + budget;
        value = dynamicValue.get(finalCase);
        //System.out.println(value);
        price = dynamicCost.get(finalCase);
        //System.out.println(dyanmicResults.get(finalCase).size());
        result = new ArrayList<>();
        result.addAll(dyanmicResults.get(finalCase));

        return result;
    }

    public Set<Float> sortFloatSet(Set<Float> set){
        Set<Float> sortedSet = new TreeSet<>();
        Object[] tmp = set.toArray();

        int preIndex;
        float current;
        for(int i = 1; i < tmp.length; i++){
            preIndex = i-1;
            current = Float.parseFloat(tmp[i].toString());
            while (preIndex >=0 && Float.parseFloat(tmp[preIndex].toString()) > current){
                tmp[preIndex + 1] = tmp[preIndex];
                preIndex--;
            }
            tmp[preIndex + 1] = current;
        }
        for (Object ob: tmp){
            sortedSet.add(Float.parseFloat(ob.toString()));
        }

        return sortedSet;
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

            this.price = getCompressedCost(this, solutionProduct);
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
}



