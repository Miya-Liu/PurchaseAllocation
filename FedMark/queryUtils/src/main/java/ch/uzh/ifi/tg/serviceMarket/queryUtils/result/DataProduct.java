package ch.uzh.ifi.tg.serviceMarket.queryUtils.result;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tobias Grubenmann on 21/09/16.
 */
public class DataProduct implements Comparable<DataProduct> {

    private String identifier;
    private float cost;
    /**
     * updated by Miya
     */
    int providerIndex = 0;
    List<Integer> providersList = new ArrayList<>();

    public DataProduct(String identifier, float cost) {
        this.identifier = identifier;
        this.cost = cost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataProduct that = (DataProduct) o;

        return identifier != null ? identifier.equals(that.identifier) : that.identifier == null;

    }

    @Override
    public int hashCode() {
        return identifier != null ? identifier.hashCode() : 0;
    }

    public float getCost() {
        return cost;
    }
    public void updateCost(float c){
        cost = c;
    }

    public String getIdentifier() {
        return identifier;
    }

    /**
     * updated by Miya
     * @return
     */
    public int getProviderIndex(){
        return providerIndex;
    }

    public void updateProviderIndex(int p){
        providerIndex = p;
    }

    public void setProvidersList(List<Integer> providersList) {
        this.providersList = providersList;
    }

    public List<Integer> getProvidersList(){
        return providersList;
    }

    public int compareTo(DataProduct o) {
        return this.getIdentifier().compareTo(o.getIdentifier());
    }
}
