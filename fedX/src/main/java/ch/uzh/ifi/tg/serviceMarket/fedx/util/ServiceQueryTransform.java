package ch.uzh.ifi.tg.serviceMarket.fedx.util;

import ch.uzh.ifi.tg.serviceMarket.queryUtils.parser.TriplePattern;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tobias Grubenmann on 21/09/16.
 */
public class ServiceQueryTransform extends TransformCopy {

    private String suffix;
    private BindingSet bindings;
    private List<String> providerVars;

    public ServiceQueryTransform(BindingSet bindings, String suffix) {
        this.suffix = suffix;
        this.bindings = bindings;
        providerVars = new ArrayList<String>();
    }

    public List<String> getProviderVars() {
        return providerVars;
    }

    @Override
    public Op transform(OpBGP opBGP) {

        List<Triple> newTriples = new ArrayList<Triple>();

        for (Triple triple : opBGP.getPattern().getList()) {
            Node newSubject = createNode(triple.getSubject(), suffix, providerVars, bindings);
            Node newPredicate = createNode(triple.getPredicate(), suffix, providerVars, bindings);
            Node newObject = createNode(triple.getObject(), suffix, providerVars, bindings);

            newTriples.add(new Triple(newSubject, newPredicate, newObject));
        }

        return new OpBGP(BasicPattern.wrap(newTriples));
    }

    private Node createNode(Node node, String suffix, List<String> providerVars, BindingSet bindings) {

        Node newNode = node;

        String nodeString = TriplePattern.nodeToString(node);

        if (node.isVariable()) {
            String originalVarName = nodeString.substring(1);
            nodeString = nodeString + suffix;
            String varName = nodeString.substring(1);
            if (bindings.hasBinding(originalVarName)) {
                Value value = bindings.getValue(originalVarName);
                if (value instanceof Literal) {
                    newNode = NodeFactory.createLiteral(value.stringValue());
                } else if (value instanceof URI) {
                    newNode = NodeFactory.createURI(value.stringValue());
                }
            } else {
                newNode = NodeFactory.createVariable(varName);
                providerVars.add(varName);
            }
        }

        return newNode;
    }
}
