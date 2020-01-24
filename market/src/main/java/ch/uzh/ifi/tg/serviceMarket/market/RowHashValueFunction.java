package ch.uzh.ifi.tg.serviceMarket.market;

import org.openrdf.query.BindingSet;

import java.util.List;
import java.util.Random;

/**
 * Created by Tobias Grubenmann on 22.06.17.
 */
public class RowHashValueFunction implements ValueFunction {
    private float valuePerRow;

    public RowHashValueFunction(float valuePerRow) {
        this.valuePerRow = valuePerRow;
    }

    /** comment by Miya
     * get the value of the input bindings
     * @param bindings
     * @param totalNumberOfBindings the number of bindings of the query
     * @return the value of the input bindings
     */
    @Override
    public float getValue(List<BindingSet> bindings, int totalNumberOfBindings) {

        float value = 0;

        for (BindingSet binding : bindings) {
            String compoundKey = "";
            for (String name : binding.getBindingNames()) {
                compoundKey += binding.getValue(name).stringValue() + "|";
            }
            Random random = new Random(compoundKey.hashCode());
            value += (1 + random.nextFloat()) * valuePerRow;

        }

        return value;

    }
}
