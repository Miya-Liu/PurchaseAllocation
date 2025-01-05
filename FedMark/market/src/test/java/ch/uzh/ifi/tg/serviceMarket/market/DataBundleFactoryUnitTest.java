package ch.uzh.ifi.tg.serviceMarket.market;

import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataBundle;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.result.DataProduct;
import org.junit.Test;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.impl.ListBindingSet;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Tobias Grubenmann on 21/09/16.
 */
public class DataBundleFactoryUnitTest {
    @Test
    public void testTwoIndependentBindings() {

        ValueFactory valueFactory = new ValueFactoryImpl();

        List<String> bindingNames = new ArrayList<String>();

        bindingNames.add(DataBundleFactory.PRODUCT_VAR + "1");
        bindingNames.add(DataBundleFactory.PRODUCT_VAR + "2");
        bindingNames.add("variable");

        List<Value> values1 = new ArrayList<Value>();
        values1.add(valueFactory.createLiteral("product1"));
        values1.add(valueFactory.createLiteral("product2"));
        values1.add(valueFactory.createLiteral("a"));

        BindingSet bindings1 = new ListBindingSet(bindingNames, values1);

        List<Value> values2 = new ArrayList<Value>();
        values2.add(valueFactory.createLiteral("product1"));
        values2.add(valueFactory.createLiteral("product3"));
        values2.add(valueFactory.createLiteral("b"));

        BindingSet bindings2 = new ListBindingSet(bindingNames, values2);

        List<BindingSet> bindings = new ArrayList<BindingSet>();
        bindings.add(bindings1);
        bindings.add(bindings2);

        List<DataBundle> dataBundles =  DataBundleFactory.createDataBundles(bindings, 2, new RowValueFunction(1), new DummyPriceManager());

        assertEquals(2, dataBundles.size());

        DataBundle dataBundle1 = dataBundles.get(0);

        assertEquals(1, dataBundle1.getProductsList().size());

        List<DataProduct> products1 = new ArrayList<DataProduct>(dataBundle1.getProductsList().get(0));

        assertTrue(products1.contains(new DataProduct("product1", 0)));
        products1.remove(new DataProduct("product1", 0));
        assertTrue(products1.contains(new DataProduct("product2", 0)));
        products1.remove(new DataProduct("product2", 0));

        assertEquals(0, products1.size());

        assertEquals("\"a\"", dataBundle1.getBindings().get(0).getBinding("variable").getValue().toString());

        DataBundle dataBundle2 = dataBundles.get(1);

        assertEquals(1, dataBundle2.getProductsList().size());

        List<DataProduct> products2 = new ArrayList<DataProduct>(dataBundle2.getProductsList().get(0));

        assertTrue(products2.contains(new DataProduct("product1", 0)));
        products2.remove(new DataProduct("product1", 0));
        assertTrue(products2.contains(new DataProduct("product3", 0)));
        products2.remove(new DataProduct("product3", 0));

        assertEquals(0, products2.size());

        assertEquals("\"b\"", dataBundle2.getBindings().get(0).getBinding("variable").getValue().toString());
    }

    @Test
    public void testTwoBindingsWithSameProducts() {

        ValueFactory valueFactory = new ValueFactoryImpl();

        List<String> bindingNames = new ArrayList<String>();

        bindingNames.add(DataBundleFactory.PRODUCT_VAR + "1");
        bindingNames.add(DataBundleFactory.PRODUCT_VAR + "2");
        bindingNames.add("variable");

        List<Value> values1 = new ArrayList<Value>();
        values1.add(valueFactory.createLiteral("product1"));
        values1.add(valueFactory.createLiteral("product2"));
        values1.add(valueFactory.createLiteral("a"));

        BindingSet bindings1 = new ListBindingSet(bindingNames, values1);

        List<Value> values2 = new ArrayList<Value>();
        values2.add(valueFactory.createLiteral("product1"));
        values2.add(valueFactory.createLiteral("product2"));
        values2.add(valueFactory.createLiteral("b"));

        BindingSet bindings2 = new ListBindingSet(bindingNames, values2);

        List<BindingSet> bindings = new ArrayList<BindingSet>();
        bindings.add(bindings1);
        bindings.add(bindings2);

        List<DataBundle> dataBundles =  DataBundleFactory.createDataBundles(bindings, 2, new RowValueFunction(1), new DummyPriceManager());

        assertEquals(1, dataBundles.size());

        DataBundle dataBundle1 = dataBundles.get(0);

        assertEquals(1, dataBundle1.getProductsList().size());

        List<DataProduct> products1 = new ArrayList<DataProduct>(dataBundle1.getProductsList().get(0));

        assertTrue(products1.contains(new DataProduct("product1", 0)));
        products1.remove(new DataProduct("product1", 0));
        assertTrue(products1.contains(new DataProduct("product2", 0)));
        products1.remove(new DataProduct("product2", 0));

        assertEquals(0, products1.size());

        assertEquals("\"a\"", dataBundle1.getBindings().get(0).getBinding("variable").getValue().toString());
        assertEquals("\"b\"", dataBundle1.getBindings().get(1).getBinding("variable").getValue().toString());
    }

    @Test
    public void testTwoIdenticalBindings() {

        ValueFactory valueFactory = new ValueFactoryImpl();

        List<String> bindingNames = new ArrayList<String>();

        bindingNames.add(DataBundleFactory.PRODUCT_VAR + "1");
        bindingNames.add(DataBundleFactory.PRODUCT_VAR + "2");
        bindingNames.add("variable");

        List<Value> values1 = new ArrayList<Value>();
        values1.add(valueFactory.createLiteral("product1"));
        values1.add(valueFactory.createLiteral("product2"));
        values1.add(valueFactory.createLiteral("a"));

        BindingSet bindings1 = new ListBindingSet(bindingNames, values1);

        List<Value> values2 = new ArrayList<Value>();
        values2.add(valueFactory.createLiteral("product1"));
        values2.add(valueFactory.createLiteral("product3"));
        values2.add(valueFactory.createLiteral("a"));

        BindingSet bindings2 = new ListBindingSet(bindingNames, values2);

        List<Value> values3 = new ArrayList<Value>();
        values3.add(valueFactory.createLiteral("product1"));
        values3.add(valueFactory.createLiteral("product2"));
        values3.add(valueFactory.createLiteral("b"));

        BindingSet bindings3 = new ListBindingSet(bindingNames, values3);

        List<BindingSet> bindings = new ArrayList<BindingSet>();
        bindings.add(bindings1);
        bindings.add(bindings2);
        bindings.add(bindings3);

        List<DataBundle> dataBundles =  DataBundleFactory.createDataBundles(bindings, 2, new RowValueFunction(1), new DummyPriceManager());

        assertEquals(2, dataBundles.size());

        DataBundle dataBundle1 = dataBundles.get(0);

        assertEquals(2, dataBundle1.getProductsList().size());

        List<DataProduct> products1a = new ArrayList<DataProduct>(dataBundle1.getProductsList().get(0));

        assertTrue(products1a.contains(new DataProduct("product1", 0)));
        products1a.remove(new DataProduct("product1", 0));
        assertTrue(products1a.contains(new DataProduct("product2", 0)));
        products1a.remove(new DataProduct("product2", 0));

        assertEquals(0, products1a.size());

        List<DataProduct> products1b = new ArrayList<DataProduct>(dataBundle1.getProductsList().get(1));

        assertTrue(products1b.contains(new DataProduct("product1", 0)));
        products1b.remove(new DataProduct("product1", 0));
        assertTrue(products1b.contains(new DataProduct("product3", 0)));
        products1b.remove(new DataProduct("product3", 0));

        assertEquals(0, products1b.size());

        assertEquals("\"a\"", dataBundle1.getBindings().get(0).getBinding("variable").getValue().toString());

        DataBundle dataBundle2 = dataBundles.get(1);

        assertEquals(1, dataBundle2.getProductsList().size());

        List<DataProduct> products2 = new ArrayList<DataProduct>(dataBundle1.getProductsList().get(0));

        assertTrue(products2.contains(new DataProduct("product1", 0)));
        products2.remove(new DataProduct("product1", 0));
        assertTrue(products2.contains(new DataProduct("product2", 0)));
        products2.remove(new DataProduct("product2", 0));

        assertEquals(0, products2.size());

        assertEquals("\"b\"", dataBundle2.getBindings().get(0).getBinding("variable").getValue().toString());
    }
}
