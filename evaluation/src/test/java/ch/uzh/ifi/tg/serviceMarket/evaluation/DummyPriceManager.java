package ch.uzh.ifi.tg.serviceMarket.evaluation;

import ch.uzh.ifi.tg.serviceMarket.market.PriceManager;

/**
 * Created by Tobias Grubenmann on 30/09/16.
 */
public class DummyPriceManager implements PriceManager {
    public float getPrice(String productId) {
        return 0;
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
