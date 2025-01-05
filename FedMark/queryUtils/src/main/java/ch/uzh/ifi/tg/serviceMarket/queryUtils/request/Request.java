package ch.uzh.ifi.tg.serviceMarket.queryUtils.request;

import java.util.List;

/**
 * Created by Tobias Grubenmann on 14/09/16.
 */
public class Request {

    private String id;
    private List<DataAndServiceQuery> queries;

    public Request(String id, List<DataAndServiceQuery> queries) {
        this.id = id;
        this.queries = queries;
    }

    public String getId() {
        return id;
    }

    public List<DataAndServiceQuery> getQueries() {
        return queries;
    }
}
