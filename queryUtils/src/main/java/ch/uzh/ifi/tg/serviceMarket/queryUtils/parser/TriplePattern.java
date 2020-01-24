package ch.uzh.ifi.tg.serviceMarket.queryUtils.parser;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

/**
 * Created by Tobias Grubenmann on 26/04/15.
 */
public class TriplePattern {

    private Triple triple;

    public TriplePattern(Triple triple) {
        this.triple = triple;
    }

    public Triple getTriple() {
        return triple;
    }

    public String toString() {
        return triple.getSubject() + " " + triple.getPredicate() + " " + triple.getObject();
    }

    public String toSparqlTriple() {

        return nodeToString(triple.getSubject()) + " " + nodeToString(triple.getPredicate()) + " " + nodeToString(triple.getObject());
    }

    public static String nodeToString(Node node) {
        if (node.isBlank()) {
            return "[]";
        }
        if (node.isLiteral()) {
            return node.toString(true);
        }
        if (node.isURI()) {
            return "<" + node.toString() + ">";
        }
        if (node.isVariable()) {
            if (node.toString().startsWith("??")) {
                // blank node as variable
                return "[]";
            } else {
                return node.toString();
            }
        }
        return "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TriplePattern that = (TriplePattern) o;

        return !(getTriple() != null ? !getTriple().equals(that.getTriple()) : that.getTriple() != null);

    }

    @Override
    public int hashCode() {
        return getTriple() != null ? getTriple().hashCode() : 0;
    }
}
