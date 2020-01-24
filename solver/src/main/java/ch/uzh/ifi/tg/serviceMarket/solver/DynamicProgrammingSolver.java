package ch.uzh.ifi.tg.serviceMarket.solver;

import ch.uzh.ifi.tg.serviceMarket.market.PriceManager;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataBundle;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataProduct;
import ilog.concert.IloException;

import java.util.*;

public class DynamicProgrammingSolver implements Solver {
    List<DynamicProgrammingSolver.Entry> openList;
    public PriceManager priceManager;// solver needs a price manager to know how to calculate a new price
    int[] providerProductsAmount = {0,0,0,0,0};

    private float value;
    private float price;

    public DynamicProgrammingSolver(PriceManager priceManager) {
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

    public void initValue(List<DynamicProgrammingSolver.Entry> eList){
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
                //cost = getCostFlat(highestUtilityRatioEntry, currentProductList);//basic cost
                cost = getCostFreemium(highestUtilityRatioEntry, currentProductList);//basic cost
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
                                //tmpCost = getCostFlat(highestUtilityRatioEntry, tmpProductList);
                                tmpCost = getCostFreemium(highestUtilityRatioEntry, tmpProductList);

                                //newCost = getCostFlat(highestUtilityRatioEntry, inProductList);
                                //newCost = getCostFreemium(highestUtilityRatioEntry, inProductList);
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

    public Set<DataProduct> sortProductSet(Set<DataProduct> originalSet){
        List<DataProduct> sortSet = new ArrayList<>();
        sortSet.addAll(originalSet);
        sortSet.sort(new ProductComparator());
        Set<DataProduct> newSet = new HashSet<>();
        for(DataProduct p: sortSet){
            newSet.addAll(sortSet);
        }
        //newSet.addAll(sortSet);
        return newSet;
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

    public float getCostFlat(Entry e, Set<DataProduct> currentProductLis){
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

    /*public float getCostFreemium(Entry e, Set<DataProduct> currentProductList){
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
                if(n<=0) {
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
    }*/

    public int[] sort(float[] sumCost){
        int[] costIndex = {0,0,0,0,0};
        int preIndex;
        float current;
        for(int i = 1; i < sumCost.length; i++){
            preIndex = i-1;
            current = sumCost[i];
            while (preIndex >=0 && sumCost[preIndex] > current){
                sumCost[preIndex + 1] = sumCost[preIndex];
                preIndex--;
            }
            sumCost[preIndex+1] = current;
            costIndex[preIndex+1] = i;
        }

        return costIndex;
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

            //this.price = getCostFlat(this, solutionProduct);
            this.price = getCostFreemium(this, solutionProduct);
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

    class SolutionIndex {
        public List<DataBundle> solutions = new ArrayList<>();
        public float caseBudget = 0;
        public SolutionIndex(List<DataBundle> solutions, float caseBudget){
            this.solutions = solutions;
            this.caseBudget = caseBudget;
        }
    }

    class SolutionEntry{
        public Set<DataProduct> productSet = new HashSet<>();
        float caseValue = 0;
        public SolutionEntry(Set<DataProduct> productSet, float caseValue){
            this.productSet = productSet;
            this.caseValue = caseValue;
        }
    }

    class CostEntry {
        public float allCost = 0;
        public float selfCost = 0;
        public CostEntry(float allCost, float selfCost){
            this.allCost = allCost;
            this.selfCost = selfCost;
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



