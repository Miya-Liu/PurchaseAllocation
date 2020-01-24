package ch.uzh.ifi.tg.serviceMarket.evaluation;

import ch.uzh.ifi.tg.serviceMarket.queryUtils.request.DataAndServiceQuery;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.request.Request;
import org.apache.jena.base.Sys;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tobias Grubenmann on 14/09/16.
 */
public class XMLParser {

    public static final String REQUEST_TAG = "request";
    public static final String QUERY_TAG = "query";
    public static final String DATA_TAG = "data";
    public static final String SERVICE_TAG = "service";
    public static final String ID_ATTRIBUTE = "id";
    public static final String VALUE_TAG = "value-per-row";
    public static final String BUDGET_TAG = "budgetCoefficient";

    public static final String SERVER_TAG = "server";

    public static List<Request> parseRequest(String filename) throws ParserConfigurationException, IOException, SAXException {

        List<Request> requests = new ArrayList<Request>();

        Document queryDoc = load(filename);

        NodeList requestNodes = queryDoc.getElementsByTagName(REQUEST_TAG);

        for (int i = 0; i < requestNodes.getLength(); ++i) {
            Node requestNode = requestNodes.item(i);

            if (requestNode.getNodeType() == Node.ELEMENT_NODE) {

                List<DataAndServiceQuery> queries = new ArrayList<DataAndServiceQuery>();

                NodeList queryNodes = ((Element)requestNode).getElementsByTagName(QUERY_TAG);

                String requestId = ((Element)requestNode).getAttribute(ID_ATTRIBUTE);

                for (int j = 0; j < queryNodes.getLength(); ++j) {
                    Node queryNode = queryNodes.item(j);

                    if (queryNode.getNodeType() == Node.ELEMENT_NODE) {

                        NodeList data = ((Element) queryNode).getElementsByTagName(DATA_TAG);
                        NodeList services = ((Element) queryNode).getElementsByTagName(SERVICE_TAG);

                        String queryId = ((Element) queryNode).getAttribute(ID_ATTRIBUTE);

                        String dataQuery = data.item(0).getTextContent().trim();

                        String servicePattern = services.item(0).getTextContent().trim();

                        NodeList valueList = ((Element) queryNode).getElementsByTagName(VALUE_TAG);
                        float valuePerRow = Float.parseFloat(valueList.item(0).getTextContent().trim());

                        NodeList budgetList = ((Element) queryNode).getElementsByTagName(BUDGET_TAG);
                        float budget = Float.parseFloat(budgetList.item(0).getTextContent().trim());

                        queries.add(new DataAndServiceQuery(queryId, dataQuery, servicePattern, valuePerRow, budget));
                    }
                }

                requests.add(new Request(requestId, queries));
            }
        }

        return requests;

    }

    public static List<String> parseServers(String filename) throws ParserConfigurationException, IOException, SAXException {
        List<String> servers = new ArrayList<String>();

        Document queryDoc = load(filename);

        NodeList serverNodes = queryDoc.getElementsByTagName(SERVER_TAG);

        for (int i = 0; i < serverNodes.getLength(); ++i) {
            servers.add(serverNodes.item(i).getTextContent().trim());
        }

        return servers;
    }

    private static Document load(String filename) throws ParserConfigurationException, IOException, SAXException {
        File fXmlFile = new File(filename);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document queryDoc = dBuilder.parse(fXmlFile);
        queryDoc.getDocumentElement().normalize();
        return queryDoc;
    }
}
