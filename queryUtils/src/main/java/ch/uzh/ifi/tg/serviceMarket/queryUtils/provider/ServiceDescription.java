package ch.uzh.ifi.tg.serviceMarket.queryUtils.provider;

import ch.uzh.ifi.tg.serviceMarket.queryUtils.parser.TriplePattern;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;

import java.util.List;

/**
 * Created by Tobias Grubenmann on 08/04/16.
 */
public class ServiceDescription {

    public static final String GRAPH_VAR = "graph";
    private final Model serviceModel;

    private List<TriplePattern> triplePatterns;
    private String serviceDescription;

    public ServiceDescription(List<TriplePattern> triplePatterns, String serviceDescription, Model serviceModel) {
        this.triplePatterns = triplePatterns;
        this.serviceDescription = serviceDescription;
        this.serviceModel = serviceModel;
    }

    public String getGraphVariable(Triple triple) {

        int index = 0;

        for (int i = 0; i < triplePatterns.size(); ++i) {
            if (triplePatterns.get(i).getTriple().equals(triple)) {
                index = i;
            }
        }
        return GRAPH_VAR + (index + 1);
    }

    public int getTripleIndex(Triple triple) {

        int index = -1;

        for (int i = 0; i < triplePatterns.size(); ++i) {
            if (triplePatterns.get(i).getTriple().equals(triple)) {
                index = i;
            }
        }

        return index + 1;
    }

    public int getNumberOfTriplePatterns() {
        return triplePatterns.size();
    }

    public Model getServiceModel() {
        return serviceModel;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }
}
