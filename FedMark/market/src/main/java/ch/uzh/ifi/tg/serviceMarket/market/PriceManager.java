package ch.uzh.ifi.tg.serviceMarket.market;

import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataProduct;

import java.util.Set;

/**
 * Created by Tobias Grubenmann on 30/09/16.
 */
public interface PriceManager {
    float getPrice(String productId);
    float getPrice(String productId, int p, long amount);
    float getPrice(Set<DataProduct> purchase, long amount);
    float getInitPrice(DataProduct p);
    long getXPoint();
    float getSetPrice();
    String getFunction();
}
