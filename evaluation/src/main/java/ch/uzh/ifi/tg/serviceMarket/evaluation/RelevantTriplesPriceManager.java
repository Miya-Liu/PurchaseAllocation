package ch.uzh.ifi.tg.serviceMarket.evaluation;

import ch.uzh.ifi.tg.serviceMarket.market.PriceManager;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataProduct;
import org.openrdf.query.BindingSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Tobias Grubenmann on 06.07.17.
 */
public class RelevantTriplesPriceManager implements PriceManager {

    public static final String PRODUCT_VAR = "product";

    private Map<String, Integer> relevantTriples;

    private List<BindingSet> bindings;
    private float priceCoefficient;
    private int maxNumberOfTriples;

    public RelevantTriplesPriceManager(float priceCoefficient, List<BindingSet> bindings, int maxNumberOfTriples) {
        this.priceCoefficient = priceCoefficient;
        this.bindings = bindings;
        this.maxNumberOfTriples = maxNumberOfTriples;
        relevantTriples = new HashMap<>();
    }

    @Override
    public float getPrice(String productId) {
        if (!relevantTriples.containsKey(productId)) {
            int count = 0;
            for (BindingSet binding : bindings) {
                for (int i = 1; i <= maxNumberOfTriples; ++i) {
                    if (binding.hasBinding(PRODUCT_VAR + i)) {
                        if (binding.getValue(PRODUCT_VAR + i).stringValue().equals(productId)) {
                            count++;
                        }
                    }
                }
            }
            relevantTriples.put(productId, count);
        }

        return relevantTriples.get(productId) * priceCoefficient;

    }

    @Override
    public float getPrice(String productId, int p, long amount) {
        return 0;
    }

    @Override
    public float getInitPrice(String productId, int p, long amount) {
        return 0;
    }

    @Override
    public long getXPoint(){
        return 0;
    }

    @Override
    public float getSetPrice(){return 0;}
}
