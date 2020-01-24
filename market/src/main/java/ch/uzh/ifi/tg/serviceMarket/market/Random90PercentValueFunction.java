package ch.uzh.ifi.tg.serviceMarket.market;

import org.openrdf.query.BindingSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by Tobias Grubenmann on 22.06.17.
 */
public class Random90PercentValueFunction implements ValueFunction {
    private float valuePerRow;
    private Random random;
    private Map<List<BindingSet>, Float> valueMap;

    public Random90PercentValueFunction(float valuePerRow) {
        this.valuePerRow = valuePerRow;
        random = new Random();
        valueMap = new HashMap<>();
    }

    public float getValue(List<BindingSet> bindings, int totalNumberOfBindings) {
        if (!valueMap.containsKey(bindings)) {
            valueMap.put(bindings, valuePerRow * (0.9f + random.nextFloat() / 10));
        }
        return valueMap.get(bindings);
    }
}
