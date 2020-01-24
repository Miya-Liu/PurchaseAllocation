package ch.uzh.ifi.tg.serviceMarket.market;

import org.openrdf.query.BindingSet;

import java.util.List;

/**
 * Created by Tobias Grubenmann on 22.06.17.
 */
public class QueryValueFunction implements ValueFunction {
    private float valuePerQuery;

    public QueryValueFunction(float valuePerRow) {
        this.valuePerQuery = valuePerRow;
    }

    @Override
    public float getValue(List<BindingSet> bindings, int totalNumberOfBindings) {
        return valuePerQuery * (((float) bindings.size()) / totalNumberOfBindings);
    }
}
