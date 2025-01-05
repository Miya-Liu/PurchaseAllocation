package ch.uzh.ifi.tg.serviceMarket.evaluation;

import ch.uzh.ifi.tg.serviceMarket.market.PriceManager;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataProduct;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Created by Tobias Grubenmann on 30/09/16.
 */
public class Random90PercentPriceManager implements PriceManager {

    Map<String, Float> priceMap;

    Random random;

    private float priceCoefficient;

    public Random90PercentPriceManager(float priceCoefficient, int seed) {
        random = new Random(seed);
        this.priceCoefficient = priceCoefficient;
        priceMap = new HashMap<>();
    }

    public Random90PercentPriceManager(float priceCoefficient) {
        random = new Random();
        this.priceCoefficient = priceCoefficient;
        priceMap = new HashMap<>();
    }

    public float getPrice(String productId) {
        if (!priceMap.containsKey(productId)) {
            priceMap.put(productId, priceCoefficient * (0.9f + random.nextFloat() / 10));
        }
        return priceMap.get(productId);
    }

    @Override
    public long getXPoint(){
        return 0;
    }

    @Override
    public float getSetPrice(){return 0;}

    @Override
    public float getPrice(String productId, int p, long amount) {
        return 0;
    }

    @Override
    public float getPrice(Set<DataProduct> purchase, long amount){ return 0; }

    @Override
    public float getInitPrice(DataProduct p){ return 0 ;}

    @Override
    public String getFunction(){return "";}
}
