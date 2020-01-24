package ch.uzh.ifi.tg.serviceMarket.market;

/**
 * Created by Tobias Grubenmann on 30/09/16.
 */
public interface PriceManager {
    float getPrice(String productId);
    float getPrice(String productId, int p, long amount);
    float getInitPrice(String productId, int p, long amount);
    long getXPoint();
    float getSetPrice();
}
