package ch.uzh.ifi.tg.serviceMarket.queryUtils.visitor;

import ch.uzh.ifi.tg.serviceMarket.queryUtils.parser.VarCollector;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitorBase;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Tobias Grubenmann on 03/10/16.
 */
public class TableVistor extends OpVisitorBase {

    public static QuerySolutionMap solutions(Op op) {
        TableVistor visitor = new TableVistor();
        OpWalker.walk(op, visitor);
        return visitor.solutions();
    }

    public TableVistor() {
        querySolutionMap = new QuerySolutionMap();
    }

    private QuerySolutionMap querySolutionMap;

    public QuerySolutionMap solutions() {
        return querySolutionMap;
    }

    @Override
    public void visit(OpBGP opBGP) {
    }

    @Override
    public void visit(OpTriple opTriple) {
    }

    @Override
    public void visit(OpQuadPattern quadPattern) {
    }

    @Override
    public void visit(OpQuad opQuad) {
    }

    @Override
    public void visit(OpGraph opGraph) {
    }

    @Override
    public void visit(OpDatasetNames dsNames) {
    }

    @Override
    public void visit(OpAssign opAssign) {
    }

    @Override
    public void visit(OpExtend opExtend) {
    }

    @Override
    public void visit(OpTable opTable) {
        ResultSet resultSet = opTable.getTable().toResultSet();
        while (resultSet.hasNext()) {
            querySolutionMap.addAll(resultSet.next());
        }
    }
}

