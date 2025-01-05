package ch.uzh.ifi.tg.serviceMarket.queryUtils.parser;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.optimize.TransformFilterPlacement;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tobias Grubenmann on 14/04/15.
 */
public class Parser {

    public static List<TriplePattern> getTriplePatterns(String queryString) {

        List<TriplePattern> triplePatterns = new ArrayList<TriplePattern>();

        Query query = QueryFactory.create(queryString);

        Op op = Algebra.compile(query);

        // Move filter down as much as possible.
        op = Transformer.transform(new TransformFilterPlacement(), op);

        TripleVisitor tripleVisitor = new TripleVisitor();
        OpWalker.walk(op, tripleVisitor);

        for (Triple triple : tripleVisitor.getTriples()) {
            triplePatterns.add(new TriplePattern(triple));
        }

        return triplePatterns;
    }

}
