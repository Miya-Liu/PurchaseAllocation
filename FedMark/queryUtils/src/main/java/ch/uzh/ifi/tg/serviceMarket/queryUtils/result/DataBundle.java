package ch.uzh.ifi.tg.serviceMarket.queryUtils.result;

import org.openrdf.query.BindingSet;
import java.util.*;

/**
 * Created by Tobias Grubenmann on 21/09/16.
 */
public class DataBundle {

    private List<Set<DataProduct>> productsList;
    private float value;
    private List<BindingSet> bindings;
    /**
     * Updated by Miya
     * string is the identifier of a product, the integer list is a provider list of the product
     */
    private Map<String, List<Integer>> providers = new HashMap<>();
    public Map<String, Integer> provider = new HashMap<>(); //for save the final results

    public DataBundle(float value, List<BindingSet> bindings) {
        productsList = new ArrayList<Set<DataProduct>>();
        this.value = value;
        this.bindings = bindings;
    }

    public void addProducts(Set<DataProduct> products) {
        productsList.add(new HashSet<DataProduct>(products));
    }

    public List<Set<DataProduct>> getProductsList() {
        return productsList;
    }

    public float getValue() {
        return value;
    }

    public List<BindingSet> getBindings() {
        return bindings;
    }

    /**
     * Updated by Miya
     */
    public Map<String, List<Integer>> getProviders(){
        return providers;
    }
    public void addProviders(String k, List<Integer> p){
        for(Set<DataProduct> products: this.productsList){
            for (DataProduct product: products){
                if(product.getIdentifier() == k){
                    product.setProvidersList(p);
                }
            }
        }
        providers.put(k,p);
    }

    public void updateValue(float v){
        this.value = v;
    }
}
