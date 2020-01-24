package ch.uzh.ifi.tg.serviceMarket.queryUtils.transform;

import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.core.Var;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Tobias Grubenmann on 09/04/16.
 */
public class InsertVariablesTransform extends TransformCopy {

    private List<Var> variables;

    public static final String PRODUCT_VAR = "product";

    public InsertVariablesTransform(int numberOfTriplePatterns) {

        variables = new ArrayList<Var>();
        for (int i = 0; i < numberOfTriplePatterns; ++i) {
            variables.add(Var.alloc(PRODUCT_VAR + (i + 1)));
        }
    }

    @Override
    public Op transform(OpProject opProject, Op subOp) {
        Set<Var> vars = new HashSet<Var>(opProject.getVars());
        for (Var var : variables) {
            vars.add(var);
        }

        return new OpProject(subOp, new ArrayList<Var>(vars));
    }
}
