package LinguaView.syntax;

import LinguaView.UIutils.Utils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.*;

/**
 * Created by draplater on 16-5-27.
 */
public class MetadataManager {

    public class Feature {
        String full;
        String abbreviate;

        Feature() {}

        public Feature(String full, String abbreviate) {
            this.full = full;
            this.abbreviate = abbreviate;
        }

        public String getAbbreviate() {
            return abbreviate;
        }

        public void setAbbreviate(String abbreviate) {
            this.abbreviate = abbreviate;
        }

        public String getFull() {
            return full;
        }

        public void setFull(String full) {
            this.full = full;
        }
    }

    public class GramFunc extends Feature {
        boolean isGovernable;

        public GramFunc(String full, String abbreviate, boolean governable) {
            super(full, abbreviate);
            isGovernable = governable;
        }

        public boolean isGovernable() {
            return isGovernable;
        }

        public void setGovernable(boolean governable) {
            isGovernable = governable;
        }

    }

    private Set<GramFunc> gramFuncList = new LinkedHashSet<>();
    private Set<Feature> featureList = new LinkedHashSet<>();

    public MetadataManager(String filename, String encoding)
            throws FileNotFoundException, UnsupportedEncodingException, XPathExpressionException {
        this(new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(filename),
                        encoding)));
    }

    public MetadataManager(String input)
            throws UnsupportedEncodingException, XPathExpressionException {
        this(new StringReader(input));
    }

    public MetadataManager(Node metaNode) {
        if(metaNode == null) {
            return;
        }
        NodeList nodes = metaNode.getChildNodes();
        for(int i = 0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if(! (node.getNodeType() == Node.ELEMENT_NODE))
                continue;
            String name = node.getNodeName();
            NamedNodeMap attrs = node.getAttributes();
            Map<String, String> attrMap = new HashMap<>();
            for(int j=0; j<attrs.getLength(); j++) {
                String attrName = attrs.item(j).getNodeName();
                String attrValue = attrs.item(j).getNodeValue();
                attrMap.put(attrName, attrValue);
            }

            String full = attrMap.get("full");
            String abbreviate = attrMap.get("short");

            if(name.equals("ft")) {
                Utils.logger.info(String.format("ft %s %s",
                        full, abbreviate));
                featureList.add(new Feature(full, abbreviate));
            } else if(name.equals("gf")) {
                boolean governable = false;
                if(attrMap.get("governable").equals("+")) {
                    governable = true;
                }
                Utils.logger.info(String.format("gf %s %s %s",
                        full, abbreviate, governable));
                gramFuncList.add(new GramFunc(full, abbreviate
                        , governable));
            }
        }
    }

    public MetadataManager(Reader input)
            throws UnsupportedEncodingException, XPathExpressionException {
        this(getMetaNode(input));
    }

    static private Node getMetaNode(Reader input) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource source = new InputSource(input);
        NodeList nodes = (NodeList) xpath.evaluate("/viewer/meta", source,
                XPathConstants.NODESET);
        if(nodes.getLength() == 0)
            return null;
        return nodes.item(0);
    }

    public Set<GramFunc> getGramFuncList() {
        return gramFuncList;
    }

    public Set<Feature> getFeatureList() {
        return featureList;
    }

    public boolean isEmpty() {
        return gramFuncList.isEmpty() && featureList.isEmpty();
    }
}
