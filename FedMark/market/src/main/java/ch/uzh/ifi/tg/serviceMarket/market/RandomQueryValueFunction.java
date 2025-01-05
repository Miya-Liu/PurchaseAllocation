package ch.uzh.ifi.tg.serviceMarket.market;

import org.openrdf.query.BindingSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by Tobias Grubenmann on 10.07.17.
 */
public class RandomQueryValueFunction implements ValueFunction {
    private float valuePerQuery;

    private Map<List<BindingSet>, Float> valueMap;

    private Random random;

    public RandomQueryValueFunction(float valuePerRow) {
        this.valuePerQuery = valuePerRow;
        random = new Random();
        valueMap = new HashMap<>();
    }

    @Override
    public float getValue(List<BindingSet> bindings, int totalNumberOfBindings) {
        if (!valueMap.containsKey(bindings)) {
            valueMap.put(bindings, random.nextFloat() * valuePerQuery * (((float) bindings.size()) / totalNumberOfBindings));
        }
        return valueMap.get(bindings);
    }

    // by Miya
    @Override
    public String getDistribution(){
        return "e";
    }

    @Override
    public void setDistribution(String d){
        return;
    }
}