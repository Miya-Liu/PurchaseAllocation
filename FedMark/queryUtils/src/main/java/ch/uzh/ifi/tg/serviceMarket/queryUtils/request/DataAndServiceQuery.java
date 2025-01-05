package ch.uzh.ifi.tg.serviceMarket.queryUtils.request;

import java.util.List;

/**
 * Created by Tobias Grubenmann on 14/09/16.
 */
public class DataAndServiceQuery {

    private String id;
    private String dataQuery;
    private String servicePattern;
    private float valuePerRow;
    private float budget;

    public DataAndServiceQuery(String id, String dataQuery, String servicePattern, float valuePerRow, float budget) {
        this.id = id;
        this.dataQuery = dataQuery;
        this.servicePattern = servicePattern;
        this.valuePerRow = valuePerRow;
        this.budget = budget;
    }

    public String getId() {
        return id;
    }

    public String getDataQuery() {
        return dataQuery;
    }

    public String getServicePattern() {
        return servicePattern;
    }

    public float getValuePerRow() {
        return valuePerRow;
    }

    public float getBudget() {
        return budget;
    }
}
