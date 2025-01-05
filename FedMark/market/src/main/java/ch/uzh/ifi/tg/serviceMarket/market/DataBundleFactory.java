package ch.uzh.ifi.tg.serviceMarket.market;

import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataBundle;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataProduct;

import org.apache.jena.sparql.lang.SyntaxVarScope;
import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.impl.ListBindingSet;

import java.util.*;

/**
 * Created by Tobias Grubenmann on 21/09/16.
 */
public class DataBundleFactory {

    public static final String PRODUCT_VAR = "product";
    public static Map<String, ArrayList<Integer>> duplicators = new HashMap<>();
    public static List<String> neighbors = new ArrayList<>();
    static {
        neighbors.add("chebi");
        neighbors.add("drugbank");
        neighbors.add("dbpedia");
        neighbors.add("geonames");
        neighbors.add("jamendo");
        neighbors.add("kegg");
        neighbors.add("linkedmdb");
        neighbors.add("nytimes");
        neighbors.add("semanticweb");
    }
    static {
        int count = neighbors.size();
        for(String s: neighbors){
            ArrayList<Integer> tmp = new ArrayList<>();
            int index = neighbors.indexOf(s);
            int nxt_neb = (index+1) % count;
            tmp.add(nxt_neb);
            int pre_neb = (index-1+count) % count;
            tmp.add(pre_neb);
            duplicators.put(s, tmp);
        }
    }

    public static List<DataBundle> createDataBundles(List<BindingSet> bindings, int numberOfTriples,
                                                     ValueFunction valueFunction, List<PriceManager> priceManager) {

        // get product dependencies for bindings

        Map<BindingSet, SortedSet<String>> bindingsKeyMap = new HashMap<BindingSet, SortedSet<String>>();
        Map<String, SortedSet<DataProduct>> keyProductsMap = new HashMap<String, SortedSet<DataProduct>>();

        for (BindingSet binding : bindings) {

            BindingSet queryBinding = getQueryBinding(binding, numberOfTriples);

            SortedSet<DataProduct> products = getProducts(binding, numberOfTriples, priceManager.get(0));
            String compoundKey = getCompoundKey(products);

            if (!bindingsKeyMap.containsKey(queryBinding)) {
                SortedSet<String> keys = new TreeSet<String>();
                bindingsKeyMap.put(queryBinding, keys);
            }

            bindingsKeyMap.get(queryBinding).add(compoundKey);

            if (!keyProductsMap.containsKey(compoundKey)) {
                keyProductsMap.put(compoundKey, products);
            }
        }

        // group bindings based on dependencies

        Map<String, List<BindingSet>> bindingMap = new HashMap<String, List<BindingSet>>();

        for (BindingSet binding : bindingsKeyMap.keySet()) {
            String compoundKey = getCompoundKeyForKeys(bindingsKeyMap.get(binding));

            if (!bindingMap.containsKey(compoundKey)) {
                bindingMap.put(compoundKey, new ArrayList<BindingSet>());
            }

            bindingMap.get(compoundKey).add(binding);
        }

        // create bundles

        List<DataBundle> bundles = new ArrayList<DataBundle>();

        for (List<BindingSet> bundleBindings : bindingMap.values()) {
            float value = valueFunction.getValue(bundleBindings, bindings.size());
            DataBundle bundle = new DataBundle(value, bundleBindings);

            BindingSet firstBinding = bundleBindings.get(0);

            for (String key : bindingsKeyMap.get(firstBinding)) {
                bundle.addProducts(keyProductsMap.get(key));
            }

            bundles.add(bundle);
        }

        //System.out.println("Original query results >>>>>>>>>>>>>>>>>>>>>>>>>>");
        //printBundes(bundles, false);

        /**
         * Updated by Miya
         * add providers for each product
         *
        List<DataBundle> duplicatedBundles = new ArrayList<DataBundle>();
        for (DataBundle dataBundle: bundles) {
            for (Set<DataProduct> product : dataBundle.getProductsList()) {
                for (DataProduct p : product) {
                    List<Integer> providers = getProviders(p.getIdentifier());
                    dataBundle.addProviders(p.getIdentifier(), providers);
                }
            }
            duplicatedBundles.add(dataBundle);
        }
        */
        List<DataBundle> duplicatedBundles = new ArrayList<DataBundle>();
        for (DataBundle dataBundle: bundles) {
            for (Set<DataProduct> product : dataBundle.getProductsList()) {
                for (DataProduct p : product) {
                    List<Integer> providers = getProviders(p.getIdentifier());
                    dataBundle.addProviders(p.getIdentifier(), providers);
                }
            }
            duplicatedBundles.add(dataBundle);
        }


        //System.out.println("Adding providers for query results >>>>>>>>>>>>>>>>>>>>>>>>>>");
        //printBundes(duplicatedBundles, false);

        return duplicatedBundles;
    }

    public static List<DataBundle> createDataBundles(List<BindingSet> bindings, int numberOfTriples,
                                                     ValueFunction valueFunction, PriceManager priceManager) {

        // get product dependencies for bindings

        Map<BindingSet, SortedSet<String>> bindingsKeyMap = new HashMap<BindingSet, SortedSet<String>>();
        Map<String, SortedSet<DataProduct>> keyProductsMap = new HashMap<String, SortedSet<DataProduct>>();

        for (BindingSet binding : bindings) {

            BindingSet queryBinding = getQueryBinding(binding, numberOfTriples);

            SortedSet<DataProduct> products = getProducts(binding, numberOfTriples, priceManager);
            String compoundKey = getCompoundKey(products);

            if (!bindingsKeyMap.containsKey(queryBinding)) {
                SortedSet<String> keys = new TreeSet<String>();
                bindingsKeyMap.put(queryBinding, keys);
            }

            bindingsKeyMap.get(queryBinding).add(compoundKey);

            if (!keyProductsMap.containsKey(compoundKey)) {
                keyProductsMap.put(compoundKey, products);
            }
        }

        // group bindings based on dependencies

        Map<String, List<BindingSet>> bindingMap = new HashMap<String, List<BindingSet>>();

        for (BindingSet binding : bindingsKeyMap.keySet()) {
            String compoundKey = getCompoundKeyForKeys(bindingsKeyMap.get(binding));

            if (!bindingMap.containsKey(compoundKey)) {
                bindingMap.put(compoundKey, new ArrayList<BindingSet>());
            }

            bindingMap.get(compoundKey).add(binding);
        }

        // create bundles

        List<DataBundle> bundles = new ArrayList<DataBundle>();

        for (List<BindingSet> bundleBindings : bindingMap.values()) {
            float value = valueFunction.getValue(bundleBindings, bindings.size());
            DataBundle bundle = new DataBundle(value, bundleBindings);

            BindingSet firstBinding = bundleBindings.get(0);

            for (String key : bindingsKeyMap.get(firstBinding)) {
                bundle.addProducts(keyProductsMap.get(key));
            }

            bundles.add(bundle);
        }

        //System.out.println("Original query results >>>>>>>>>>>>>>>>>>>>>>>>>>");
        //printBundes(bundles, false);

        /**
         * Updated by Miya
         * add providers for each product
         *
         List<DataBundle> duplicatedBundles = new ArrayList<DataBundle>();
         for (DataBundle dataBundle: bundles) {
         for (Set<DataProduct> product : dataBundle.getProductsList()) {
         for (DataProduct p : product) {
         List<Integer> providers = getProviders(p.getIdentifier());
         dataBundle.addProviders(p.getIdentifier(), providers);
         }
         }
         duplicatedBundles.add(dataBundle);
         }
         */
        List<DataBundle> duplicatedBundles = new ArrayList<DataBundle>();
        int pdcNum = bundles.get(0).getProductsList().size() * bundles.size();
        int count = 0;
        for (DataBundle dataBundle: bundles) {
            for (Set<DataProduct> product : dataBundle.getProductsList()) {
                if (count < pdcNum / 2)
                    for (DataProduct p : product) {
                        List<Integer> providers = getProviders(p.getIdentifier());
                        dataBundle.addProviders(p.getIdentifier(), providers);
                    }
                count ++;
            }
            duplicatedBundles.add(dataBundle);
        }


        //System.out.println("Adding providers for query results >>>>>>>>>>>>>>>>>>>>>>>>>>");
        //printBundes(duplicatedBundles, false);

        return duplicatedBundles;
    }

    public static SortedSet<DataProduct> getProducts(BindingSet binding, int numberOfTriples, PriceManager priceManager) {
        SortedSet<DataProduct> products = new TreeSet<DataProduct>();
        for (int i = 1; i <= numberOfTriples; ++i) {
            if (binding.hasBinding(PRODUCT_VAR + i)) {
                String productId = binding.getValue(PRODUCT_VAR + i).stringValue();
                float cost = priceManager.getPrice(productId);

                DataProduct product = new DataProduct(productId, cost);
                products.add(product);
            }
        }
        return products;
    }

    public static String getCompoundKey(SortedSet<DataProduct> products) {
        String compoundKey = "";

        for (DataProduct product : products) {
            compoundKey += "[" + product.getIdentifier() + "]";
        }
        compoundKey += "v";
        return compoundKey;
    }

    public static String getCompoundKeyForKeys(SortedSet<String> keys) {
        String compoundKey = "";
        for (String key : keys) {
            compoundKey += key + "v";
        }
        return compoundKey;
    }

    public static BindingSet getQueryBinding(BindingSet binding, int numberOfTriples) {
        List<String> newBindingNames = new ArrayList<String>();
        List<Value> newValues = new ArrayList<Value>();
        for (String bindingName : binding.getBindingNames()) {
            if (!isProductName(bindingName, numberOfTriples)) {
                newBindingNames.add(bindingName);
                newValues.add(binding.getValue(bindingName));
            }
        }
        return new ListBindingSet(newBindingNames, newValues);
    }

    public static boolean isProductName(String name, int numberOfTriples) {
        boolean isProductName = false;
        for (int i = 1; i <= numberOfTriples; ++i) {
            if (name.equals(PRODUCT_VAR + i)) {
                isProductName = true;
                break;
            }
        }
        return isProductName;
    }

    /**
     * created by Miya
     * @param productId
     * @return a list of integer which consist of providers that provider this product
     */
    public static List<Integer> getProviders(String productId){
        List<Integer> providersID = new ArrayList<Integer>();
        String[] idParts = productId.split("_(?=[^_]+$)");
        String sors = idParts[0].substring(idParts[0].lastIndexOf('/') + 1);

        ArrayList<Integer> nebor = duplicators.get(sors);
        providersID.addAll(nebor);
        providersID.add(neighbors.indexOf(sors));
        return providersID;
    }

    private static int getRandomNumberInRange(int min, int max){
        int randomInt = 0;
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        Random r = new Random();
        randomInt = r.nextInt((max - min) + 1) + min;
        return randomInt;
    }

    public static void printBundes(List<DataBundle> bundles, boolean result){
        int count = 0;
        Set<DataProduct> allProducts = new HashSet<>();
        Map<String, List<Integer>> allProviders = new HashMap<>();
        for (DataBundle bundle: bundles){
            count++;
            System.out.println("Bundle: "+count);
            System.out.println("Bundle Value: "+bundle.getValue());
            System.out.println("Bundle Bindings: ");
            for (BindingSet bindingSet: bundle.getBindings()){
                for (Binding binding: bindingSet){
                    System.out.println("\t"+binding);
                }
            }
            if(!result) {
                System.out.println("Bundle Products List: ");
                for (Set<DataProduct> products: bundle.getProductsList()){
                    for (DataProduct product: products){
                        System.out.println("\tIdentifier"+product.getIdentifier());
                        System.out.println("\tCost"+product.getCost());
                        allProducts.add(product);
                    }
                }
                System.out.println("Bundle Products Providers: ");
                Map<String, List<Integer>> providers = bundle.getProviders();
                for (Map.Entry<String, List<Integer>> e : providers.entrySet()) {
                    allProviders.put(e.getKey(), e.getValue());
                    System.out.println("\tProduct:" + e.getKey());
                    System.out.print("\tProviders:");
                    for (int p : e.getValue()) {
                        System.out.print("\t" + p);
                    }
                    System.out.println("");
                }
            }else {
                System.out.println("Bundle Products List: ");
                for (Set<DataProduct> products: bundle.getProductsList()){
                    for (DataProduct product: products){
                        System.out.println("\tIdentifier: "+product.getIdentifier());
                        System.out.println("\tCost: "+product.getCost());
                        System.out.println("\tChosen Provider:" + product.getProviderIndex());
                    }
                }
                System.out.println();

            }
            System.out.println();
        }
/*
        System.out.println("All products information");
        float overallCost = 0;
        for (DataProduct product: allProducts){
            System.out.println("\tIdentifier"+product.getIdentifier());
            System.out.println("\tCost"+product.getCost());
            overallCost += product.getCost();
            List<Integer> providers = allProviders.get(product.getIdentifier());
            System.out.print("\tProviders:");
            for (int p : providers) {
                System.out.print("\t" + p);
            }
            System.out.println("");
        }
        System.out.println("All products overall cost: " + overallCost);
        */
    }

}
