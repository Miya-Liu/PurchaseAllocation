package ch.uzh.ifi.tg.serviceMarket.solver;

import ch.uzh.ifi.tg.serviceMarket.market.PriceManager;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataBundle;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataProduct;
import ilog.concert.IloException;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Tobias Grubenmann on 21/09/16.
 */
public class IntegerProgramUnitTest {
    @Test
    public void testNonOverlappingIntegerProgram() throws IloException {

        Solver solver = new IntegerSolver();

        DataProduct p1 = new DataProduct("p1", 1);
        DataProduct p2 = new DataProduct("p2", 2);
        DataProduct p3 = new DataProduct("p3", 4);

        DataBundle b1 = new DataBundle(5, null);
        Set<DataProduct> products1 = new HashSet<DataProduct>();
        products1.add(p1);
        products1.add(p2);
        b1.addProducts(products1);

        solver.addBundle(b1);

        DataBundle b2 = new DataBundle(10, null);
        Set<DataProduct> products2 = new HashSet<DataProduct>();
        products2.add(p2);
        products2.add(p3);
        b2.addProducts(products2);

        solver.addBundle(b2);

        DataBundle b3 = new DataBundle(50, null);
        Set<DataProduct> products3 = new HashSet<DataProduct>();
        products3.add(p1);
        products3.add(p2);
        products3.add(p3);
        b3.addProducts(products3);

        solver.addBundle(b3);

        DataBundle b4 = new DataBundle(100, null);
        Set<DataProduct> products4 = new HashSet<DataProduct>();
        products4.add(p1);
        b4.addProducts(products4);

        solver.addBundle(b4);

        List<DataBundle> result = solver.solve(6);

        assertTrue(result.contains(b1));
        result.remove(b1);
        assertTrue(result.contains(b4));
        result.remove(b4);
        assertEquals(0, result.size());
        assertEquals(105, solver.getCurrentValue(), 0.00001f);
    }

    @Test
    public void testOverlappingIntegerProgram() throws IloException {

        Solver solver = new IntegerSolver();

        DataProduct p1 = new DataProduct("p1", 1);
        DataProduct p2 = new DataProduct("p2", 2);
        DataProduct p3 = new DataProduct("p3", 4);
        DataProduct p4 = new DataProduct("p4", 3);

        DataBundle b1 = new DataBundle(5, null);
        Set<DataProduct> products1 = new HashSet<DataProduct>();
        products1.add(p1);
        products1.add(p2);
        b1.addProducts(products1);

        solver.addBundle(b1);

        DataBundle b2 = new DataBundle(10, null);
        Set<DataProduct> products2 = new HashSet<DataProduct>();
        products2.add(p2);
        products2.add(p3);
        b2.addProducts(products2);

        solver.addBundle(b2);

        DataBundle b3 = new DataBundle(50, null);
        Set<DataProduct> products3 = new HashSet<DataProduct>();
        products3.add(p1);
        products3.add(p2);
        products3.add(p3);
        b3.addProducts(products3);
        Set<DataProduct> products3b = new HashSet<DataProduct>();
        products3b.add(p4);
        b3.addProducts(products3b);

        solver.addBundle(b3);

        DataBundle b4 = new DataBundle(100, null);
        Set<DataProduct> products4 = new HashSet<DataProduct>();
        products4.add(p1);
        b4.addProducts(products4);

        solver.addBundle(b4);

        List<DataBundle> result = solver.solve(6);

        assertTrue(result.contains(b1));
        result.remove(b1);
        assertTrue(result.contains(b3));
        result.remove(b3);
        assertTrue(result.contains(b4));
        result.remove(b4);
        assertEquals(0, result.size());
        assertEquals(155, solver.getCurrentValue(), 0.00001f);
    }
}
