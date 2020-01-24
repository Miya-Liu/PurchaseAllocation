package ch.uzh.ifi.tg.serviceMarket.evaluation;

import ch.uzh.ifi.tg.serviceMarket.market.PriceManager;
import java.util.*;

/**
 * Created by Tobias Grubenmann on 30/09/16.
 */
public class HashPriceManager implements PriceManager {

    private float priceCoefficient;
    public Map<String, Integer> splitedInfo = new HashMap<String, Integer>();
    public long xpoint;
    public float setPrice;


    public HashPriceManager(float priceCoefficient, long xpoint, float setPrice) {
        this.priceCoefficient = priceCoefficient;
        this.xpoint = xpoint;
        this.setPrice = setPrice;
    }

    @Override
    public long getXPoint(){return xpoint;}

    @Override
    public float getSetPrice(){return setPrice;}

    public float getPrice(String productId) {
        Random random = new Random(productId.hashCode());
        return random.nextFloat() * priceCoefficient;
    }

    /**
     * Updated by Miya
     * @param productId
     * @param p
     * @param amount
     * @return
     */
    public float getPrice(String productId, int p, long amount){
        PricingFunctions pirceFunctions = new PricingFunctions(p, amount);
        float price = 0;
        switch (p){
            case 0: // free pricing strategy
                //price = pirceFunctions.getFreePrice();
                price = getPrice(productId);
                return price;
            case 1: // usage based pricing strategy
                //price = pirceFunctions.getUsageBasedPrice();
                //price = getPrice(productId);
                //price = pirceFunctions.getFlatPrice(amount);
                price = pirceFunctions.getFreemiumPrice(amount);
                return price;
            case 2: // freemium pricing strategy
                price = pirceFunctions.getFreemiumPrice(amount);
                return price;
            case 3: // freemium pricing strategy
                //price = pirceFunctions.getFreemiumPrice(amount);
                price = getPrice(productId);
                return price;
            case 4: // extra pricing strategy
                price = getPrice(productId);
                return price;
        }
        return 0;
    }


    public float getInitPrice(String productId, int p, long amount){
        PricingFunctions pirceFunctions = new PricingFunctions(p, amount);
        float price = 0;
        switch (p){
            case 0: // free pricing strategy
                //price = pirceFunctions.getFreePrice();
                price = getPrice(productId);
                return price;
            case 1: // usage based pricing strategy
                //price = pirceFunctions.getUsageBasedPrice();
                //price = getPrice(productId);
                //price = pirceFunctions.flatPrice;
                price = pirceFunctions.getFreemiumPrice(amount);
                return price;
            case 2: // freemium pricing strategy
                price = 0;
                return price;
            case 3: // freemium pricing strategy
                //price = pirceFunctions.getFreemiumPrice(amount);
                price = getPrice(productId);
                return price;
            case 4: // extra pricing strategy
                price = getPrice(productId);
                return price;
        }
        return 0;
    }


    /**
     * Updated by Miya
     * a class describe different pricing functions
     */
    class PricingFunctions {
        int index = 0;
        long amount = 0;
        private float flatPrice = setPrice; //define the flat price of the provider
        private float usagePrice = new Random(1).nextFloat(); //define the usage based price of the provider
        private long limitation = xpoint; //define the free access limitation of freemium pricing strategy
        private float freemiumPrice = setPrice; //new Random(3).nextFloat();
        public PricingFunctions(int index, long amount){
            this.index = index;
            this.amount = amount;
        }

        /**
         * free pricing strategy
         * @return 0
         */
        public float getFreePrice(){
            return 0;
        }

        /**
         * usage based pricing strategy
         * @return the price of each triple
         */
        public float getUsageBasedPrice(){
            return usagePrice;
        }

        /**
         * flat pricing strategy, if the triple is the first triple, return flat price, else return free
         * @param amount
         * @return
         */
        public float getFlatPrice(long amount){
            if (amount == 1){
                return flatPrice;
            }
            return 0;
        }

        /**
         * freemium pricing strategy, if the amount is no more than the limitation, it's free.
         * otherwise, the price if freemiumPrice per triple
         * @param amount
         * @return
         */
        public float getFreemiumPrice(long amount){
            if (amount <= limitation){
                return 0;
            }else if(amount == limitation + 1) {
                return freemiumPrice;
            }
            return 0;
        }
    }//end of the PricingFunctions class

}
