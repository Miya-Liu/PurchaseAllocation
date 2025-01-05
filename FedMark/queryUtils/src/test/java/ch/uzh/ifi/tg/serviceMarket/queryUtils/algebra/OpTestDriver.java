package ch.uzh.ifi.tg.serviceMarket.queryUtils.algebra;

import org.apache.jena.base.Sys;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.OpWalker;

/**
 * Created by Tobias Grubenmann on 24/03/16.
 */
public class OpTestDriver {

    public static final String QUERY_STRING = "SELECT * Where {?a ?b ?c}";

    public static void main(String[] args) {
        Query query = QueryFactory.create(QUERY_STRING);

        Op op = Algebra.compile(query);

        Query newQuery = OpAsQuery.asQuery(op);

        System.out.println(newQuery);
    }
}
