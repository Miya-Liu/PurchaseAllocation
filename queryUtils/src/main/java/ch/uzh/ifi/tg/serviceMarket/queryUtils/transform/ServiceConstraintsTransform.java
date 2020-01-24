package ch.uzh.ifi.tg.serviceMarket.queryUtils.transform;

import ch.uzh.ifi.tg.serviceMarket.queryUtils.provider.ServiceDescription;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.visitor.TableVistor;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.algebra.*;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.algebra.table.TableData;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.util.NodeIsomorphismMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Tobias Grubenmann on 05/10/16.
 */
public class ServiceConstraintsTransform extends TransformCopy {

    private int graphIndex;
    private String host;
    private ServiceDescription serviceDescription;

    public ServiceConstraintsTransform(int graphIndex, String host, ServiceDescription serviceDescription) {
        this.graphIndex = graphIndex;
        this.host = host;
        this.serviceDescription = serviceDescription;
    }

    @Override
    public Op transform(OpTable opTable) {
        String askQuery = "ASK { " + serviceDescription.getServiceDescription() + " }";
        Query bindingsQuery = OpAsQuery.asQuery(opTable);
        String bindingValues = bindingsQuery.getQueryPattern().toString();

        String suffix = "";
        Pattern p = Pattern.compile("\\?\\S+_(\\d+)");
        Matcher m = p.matcher(bindingsQuery.getQueryPattern().toString());
        if (m.find()) {
            suffix = "_" + m.group(1);
        }

        bindingValues = bindingValues.replaceAll("(\\?\\S+)_\\d+", "$1");
        Query newBindingQuery = QueryFactory.create("SELECT * WHERE { " + bindingValues + " }");
        QuerySolutionMap querySolutionMap = TableVistor.solutions(Algebra.compile(newBindingQuery));
        List<Binding> products = runAskQuery(host, serviceDescription, askQuery, graphIndex, suffix, querySolutionMap);

        Op newOpTable = new OpRemove();

        if (products.size() > 0) {
            Table table = new TableData(Arrays.<Var>asList(new Var[]{Var.alloc("product" + graphIndex + suffix)}), products);

            newOpTable = OpTable.create(table);
        }

        return newOpTable;
    }

    private List<Binding> runAskQuery(String host, ServiceDescription serviceDescription, String askQuery, int graphIndex, String suffix, QuerySolutionMap querySolutionMap) {

        List<Binding> products = new ArrayList<Binding>();

        Model model = serviceDescription.getServiceModel();

        String productsQuery = "SELECT ?product WHERE { <" + host + "> <http://www.example.com/hosts> ?product }";

        QueryExecution queryExec = QueryExecutionFactory.create(productsQuery, model);
        ResultSet results = queryExec.execSelect();

        while (results.hasNext()) {
            QuerySolution solution = results.next();

            QuerySolutionMap initialBinding = new QuerySolutionMap();
            initialBinding.addAll(querySolutionMap);
            initialBinding.add("product" + graphIndex, solution.get("product"));

            queryExec = QueryExecutionFactory.create(askQuery, model, initialBinding);
            boolean isCompatible = queryExec.execAsk();
            if (isCompatible) {
                products.add(BindingFactory.binding(Var.alloc("product" + graphIndex + suffix), solution.get("product").asNode()));
            }
        }
        return products;
    }

    @Override
    public Op transform(OpAssign opAssign, Op subOp) {
        if (subOp instanceof OpRemove) {
            return subOp;
        }
        return super.transform(opAssign, subOp);
    }

    @Override
    public Op transform(OpConditional opConditional, Op left, Op right) {
        if (left instanceof OpRemove) {
            return left;
        }
        if (right instanceof OpRemove) {
            return right;
        }
        return super.transform(opConditional, left, right);
    }

    @Override
    public Op transform(OpDiff opDiff, Op left, Op right) {
        if (left instanceof OpRemove) {
            return left;
        }
        if (right instanceof OpRemove) {
            return right;
        }
        return super.transform(opDiff, left, right);
    }

    @Override
    public Op transform(OpDisjunction opDisjunction, List<Op> elts) {
        for (Op op : elts) {
            if (op instanceof OpRemove) {
                return op;
            }
        }
        return super.transform(opDisjunction, elts);
    }

    @Override
    public Op transform(OpDistinct opDistinct, Op subOp) {
        if (subOp instanceof OpRemove) {
            return subOp;
        }
        return super.transform(opDistinct, subOp);
    }

    @Override
    public Op transform(OpExtend opExtend, Op subOp) {
        if (subOp instanceof OpRemove) {
            return subOp;
        }
        return super.transform(opExtend, subOp);
    }

    @Override
    public Op transform(OpFilter opFilter, Op subOp) {
        if (subOp instanceof OpRemove) {
            return subOp;
        }
        return super.transform(opFilter, subOp);
    }

    @Override
    public Op transform(OpGraph opGraph, Op subOp) {
        if (subOp instanceof OpRemove) {
            return subOp;
        }
        return super.transform(opGraph, subOp);
    }

    @Override
    public Op transform(OpGroup opGroup, Op subOp) {
        if (subOp instanceof OpRemove) {
            return subOp;
        }
        return super.transform(opGroup, subOp);
    }

    @Override
    public Op transform(OpJoin opJoin, Op left, Op right) {
        if (left instanceof OpRemove) {
            return left;
        }
        if (right instanceof OpRemove) {
            return right;
        }
        return super.transform(opJoin, left, right);
    }

    @Override
    public Op transform(OpLabel opLabel, Op subOp) {
        if (subOp instanceof OpRemove) {
            return subOp;
        }
        return super.transform(opLabel, subOp);
    }

    @Override
    public Op transform(OpLeftJoin opLeftJoin, Op left, Op right) {
        if (left instanceof OpRemove) {
            return left;
        }
        if (right instanceof OpRemove) {
            return right;
        }
        return super.transform(opLeftJoin, left, right);
    }

    @Override
    public Op transform(OpList opList, Op subOp) {
        if (subOp instanceof OpRemove) {
            return subOp;
        }
        return super.transform(opList, subOp);
    }

    @Override
    public Op transform(OpMinus opMinus, Op left, Op right) {
        if (left instanceof OpRemove) {
            return left;
        }
        if (right instanceof OpRemove) {
            return right;
        }
        return super.transform(opMinus, left, right);
    }

    @Override
    public Op transform(OpOrder opOrder, Op subOp) {
        if (subOp instanceof OpRemove) {
            return subOp;
        }
        return super.transform(opOrder, subOp);
    }

    @Override
    public Op transform(OpProcedure opProcedure, Op subOp) {
        if (subOp instanceof OpRemove) {
            return subOp;
        }
        return super.transform(opProcedure, subOp);
    }

    @Override
    public Op transform(OpProject opProject, Op subOp) {
        if (subOp instanceof OpRemove) {
            return subOp;
        }
        return super.transform(opProject, subOp);
    }

    @Override
    public Op transform(OpPropFunc opPropFunc, Op subOp) {
        if (subOp instanceof OpRemove) {
            return subOp;
        }
        return super.transform(opPropFunc, subOp);
    }

    @Override
    public Op transform(OpReduced opReduced, Op subOp) {
        if (subOp instanceof OpRemove) {
            return subOp;
        }
        return super.transform(opReduced, subOp);
    }

    @Override
    public Op transform(OpSequence opSequence, List<Op> elts) {
        for (Op op : elts) {
            if (op instanceof OpRemove) {
                return op;
            }
        }
        return super.transform(opSequence, elts);
    }

    @Override
    public Op transform(OpService opService, Op subOp) {
        if (subOp instanceof OpRemove) {
            return subOp;
        }
        return super.transform(opService, subOp);
    }

    @Override
    public Op transform(OpSlice opSlice, Op subOp) {
        if (subOp instanceof OpRemove) {
            return subOp;
        }
        return super.transform(opSlice, subOp);
    }

    @Override
    public Op transform(OpTopN opTopN, Op subOp) {
        if (subOp instanceof OpRemove) {
            return subOp;
        }
        return super.transform(opTopN, subOp);
    }

    @Override
    public Op transform(OpUnion opUnion, Op left, Op right) {
        if (left instanceof OpRemove) {
            return right; // sic!
        }
        if (right instanceof OpRemove) {
            return left; // sic!
        }
        return super.transform(opUnion, left, right);
    }

    public class OpRemove extends Op0 {

        public int hashCode() {
            return 0;
        }

        public boolean equalTo(Op op, NodeIsomorphismMap nodeIsomorphismMap) {
            if (this == op) return true;
            if (op == null || getClass() != op.getClass()) return false;
            return true;
        }

        public Op apply(Transform transform) {
            return this;
        }

        public Op0 copy() {
            return new OpRemove();
        }

        public void visit(OpVisitor opVisitor) {
            return;
        }

        public String getName() {
            return "Remove";
        }


    }
}
