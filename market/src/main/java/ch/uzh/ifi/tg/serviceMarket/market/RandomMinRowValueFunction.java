package ch.uzh.ifi.tg.serviceMarket.market;

import org.openrdf.query.BindingSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by Tobias Grubenmann on 22.06.17.
 */
public class RandomMinRowValueFunction implements ValueFunction {
    private float valuePerRow;
    private Random random;
    private Map<List<BindingSet>, Float> valueMap;
    private float minFactor;

    public RandomMinRowValueFunction(float valuePerRow, float minFactor) {
        this.valuePerRow = valuePerRow;
        random = new Random();
        valueMap = new HashMap<>();
        this.minFactor = minFactor;
    }

    public float getValue(List<BindingSet> bindings, int totalNumberOfBindings) {
        if (!valueMap.containsKey(bindings)) {
            valueMap.put(bindings, (minFactor + random.nextFloat()) * bindings.size() * valuePerRow);
        }
        return valueMap.get(bindings);
    }
}
