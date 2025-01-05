package ch.uzh.ifi.tg.serviceMarket.queryUtils.transform;

import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tobias Grubenmann on 03/10/16.
 */
public class RemoveBindingsTransform extends TransformCopy {

    private List<OpTable> opTables;

    public RemoveBindingsTransform() {
        opTables = new ArrayList<OpTable>();
    }

    public List<OpTable> getOpTables() {
        return opTables;
    }

    @Override
    public Op transform(OpTable opTable) {
        opTables.add(opTable);
        return new OpBGP();
    }
}
