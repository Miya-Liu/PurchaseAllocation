package ch.uzh.ifi.tg.serviceMarket.evaluation;

import ch.uzh.ifi.tg.serviceMarket.market.PriceManager;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataProduct;

import java.util.Random;
import java.util.Set;

/**
 * Created by Tobias Grubenmann on 30/09/16.
 */
public class ConstantPriceManager implements PriceManager {

    private float priceCoefficient;

    public ConstantPriceManager(float priceCoefficient) {
        this.priceCoefficient = priceCoefficient;
    }

    public float getPrice(String productId) {
        return priceCoefficient;
    }

    @Override
    public float getPrice(String productId, int p, long amount) {
        return 0;
    }

    @Override
    public float getPrice(Set<DataProduct> purchase, long amount){ return 0; }

    @Override
    public float getInitPrice(DataProduct p){ return 0 ;}

    @Override
    public long getXPoint(){
        return 0;
    }

    @Override
    public float getSetPrice(){return 0;}

    @Override
    public String getFunction(){return "";}
}
