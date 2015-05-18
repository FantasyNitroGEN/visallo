package org.visallo.worldFactbook;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class XmlToOwl extends CommandLineTool {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(XmlToOwl.class);
    private static final String OWL_FILE_TEMPLATE_CONTENTS = "<!-- CONTENTS -->";
    private XPathExpression fieldXPath;
    private XPathExpression descriptionXPath;

    @Parameter(names = {"--in", "-i"}, required = true, arity = 1, converter = FileConverter.class, description = "XML Input file")
    private File inFile;

    @Parameter(names = {"--out", "-o"}, required = true, arity = 1, converter = FileConverter.class, description = "Output .owl file")
    private File outFile;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new XmlToOwl(), args);
    }

    @Override
    protected int run() throws Exception {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        fieldXPath = xPathfactory.newXPath().compile("//field");
        descriptionXPath = xPathfactory.newXPath().compile("description/text()");

        if (!inFile.exists()) {
            System.err.println("Input file " + inFile.getAbsolutePath() + " does not exist.");
            return 1;
        }

        String owlTemplate = readOwlTemplate();
        String contents = createOwlContents(inFile);

        String owl = owlTemplate.replace(OWL_FILE_TEMPLATE_CONTENTS, contents);

        writeOwl(owl, outFile);

        return 0;
    }

    private String createOwlContents(File inFile) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inFile);
        return createOwlContents(doc);
    }

    private String createOwlContents(Document doc) throws XPathExpressionException {

        NodeList fieldNodes = (NodeList) fieldXPath.evaluate(doc, XPathConstants.NODESET);

        StringBuilder results = new StringBuilder();
        for (int i = 0; i < fieldNodes.getLength(); i++) {
            Node fieldNode = fieldNodes.item(i);
            String xml = createDataPropertyOwlForField(fieldNode);
            if (xml == null) {
                continue;
            }
            results.append(xml);
            results.append("\n");
        }
        return results.toString();
    }

    private String createDataPropertyOwlForField(Node fieldNode) throws XPathExpressionException {
        String id = getAttributeValue(fieldNode, "id");
        if (id == null) {
            return null;
        }
        String name = getAttributeValue(fieldNode, "name");
        String description = (String) descriptionXPath.evaluate(fieldNode, XPathConstants.STRING);
        LOGGER.debug("creating field %s: %s", id, name);

        StringBuilder xml = new StringBuilder();
        xml.append("\t<owl:DatatypeProperty rdf:about=\"http://visallo.org/worldfactbook#").append(id).append("\">\n");
        xml.append("\t\t<rdfs:label xml:lang=\"en\">").append(name).append("</rdfs:label>\n");
        xml.append("\t\t<rdfs:comment xml:lang=\"en\">").append(description).append("</rdfs:comment>\n");
        xml.append("\t\t<rdfs:domain rdf:resource=\"http://visallo.org/worldfactbook#country\"/>\n");
        xml.append("\t\t<rdfs:range rdf:resource=\"&xsd;string\"/>\n");
        xml.append("\t</owl:DatatypeProperty>\n");
        return xml.toString();
    }

    private String getAttributeValue(Node node, String attributeName) {
        Node attribute = node.getAttributes().getNamedItem(attributeName);
        if (attribute == null) {
            return null;
        }
        return attribute.getNodeValue();
    }

    private void writeOwl(String owl, File outFile) throws IOException {
        try (FileOutputStream out = new FileOutputStream(outFile)) {
            out.write(owl.getBytes());
        }
    }

    private String readOwlTemplate() throws IOException {
        try (InputStream in = this.getClass().getResourceAsStream("owlFileTemplate.owl")) {
            return IOUtils.toString(in);
        }
    }
}
