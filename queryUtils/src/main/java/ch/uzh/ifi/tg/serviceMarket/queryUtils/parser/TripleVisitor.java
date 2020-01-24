package ch.uzh.ifi.tg.serviceMarket.queryUtils.parser;

import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.OpVisitor;
import org.apache.jena.sparql.algebra.op.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tobias Grubenmann on 14/04/15.
 */
public class TripleVisitor implements OpVisitor {

    private List<Triple> triples;

    public TripleVisitor() {
        triples = new ArrayList<Triple>();
    }

    public List<Triple> getTriples() {
        return triples;
    }

    public void visit(OpBGP opBGP) {
        for (Triple triple : opBGP.getPattern().getList()) {
            if (!triples.contains(triple)) {
                triples.add(triple);
            }
        }
    }

    public void visit(OpQuadPattern opQuadPattern) {
    }

    public void visit(OpQuadBlock opQuadBlock) {
    }

    public void visit(OpTriple opTriple) {
    }

    public void visit(OpQuad opQuad) {
    }

    
    public void visit(OpPath opPath) {
    }

    
    public void visit(OpTable opTable) {
    }

    
    public void visit(OpNull opNull) {
    }

    
    public void visit(OpProcedure opProcedure) {
    }

    
    public void visit(OpPropFunc opPropFunc) {
    }

    
    public void visit(OpFilter opFilter) {
    }

    
    public void visit(OpGraph opGraph) {
    }

    
    public void visit(OpService opService) {
    }

    
    public void visit(OpDatasetNames opDatasetNames) {
    }

    
    public void visit(OpLabel opLabel) {
    }

    
    public void visit(OpAssign opAssign) {
    }

    
    public void visit(OpExtend opExtend) {
    }

    
    public void visit(OpJoin opJoin) {
    }

    
    public void visit(OpLeftJoin opLeftJoin) {
    }

    
    public void visit(OpUnion opUnion) {

    }

    
    public void visit(OpDiff opDiff) {
    }

    
    public void visit(OpMinus opMinus) {
    }

    
    public void visit(OpConditional opConditional) {
    }

    
    public void visit(OpSequence opSequence) {
    }

    
    public void visit(OpDisjunction opDisjunction) {
    }

    
    public void visit(OpExt opExt) {
    }

    
    public void visit(OpList opList) {
    }

    
    public void visit(OpOrder opOrder) {
    }

    
    public void visit(OpProject opProject) {
    }

    
    public void visit(OpReduced opReduced) {
    }

    
    public void visit(OpDistinct opDistinct) {
    }

    
    public void visit(OpSlice opSlice) {
    }

    
    public void visit(OpGroup opGroup) {
    }

    
    public void visit(OpTopN opTopN) {
    }
}
