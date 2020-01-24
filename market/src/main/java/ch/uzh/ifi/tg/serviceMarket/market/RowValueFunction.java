package ch.uzh.ifi.tg.serviceMarket.market;

import org.openrdf.query.BindingSet;

import java.util.List;

/**
 * Created by Tobias Grubenmann on 22/09/16.
 */
public class RowValueFunction implements ValueFunction {

    private float valuePerRow;

    public RowValueFunction(float valuePerRow) {
        this.valuePerRow = valuePerRow;
    }

    public float getValue(List<BindingSet> bindings, int totalNumberOfBindings) {
        return bindings.size() * valuePerRow;
    }
}
