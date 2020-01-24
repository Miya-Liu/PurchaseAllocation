package ch.uzh.ifi.tg.serviceMarket.solver;

import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataBundle;
import ilog.concert.IloException;

import java.util.List;

/**
 * Created by Tobias Grubenmann on 07.08.17.
 */
public interface Solver {
    void addBundles(List<DataBundle> dataBundles) throws IloException;

    void addBundle(DataBundle dataBundle) throws IloException;

    List<DataBundle> solve(float budget) throws IloException;

    float getCurrentValue();

    float getCurrentPrice();
}
