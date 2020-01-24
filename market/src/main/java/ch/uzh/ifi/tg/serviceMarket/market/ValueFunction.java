package ch.uzh.ifi.tg.serviceMarket.market;

import org.openrdf.query.BindingSet;

import java.util.List;

/**
 * Created by Tobias Grubenmann on 21/09/16.
 */
public interface ValueFunction {
    float getValue(List<BindingSet> bindings, int totalNumberOfBindings);
}
