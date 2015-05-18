package org.visallo.worldFactbook;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;
import com.google.inject.Inject;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.ingest.FileImport;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.security.VisibilityTranslator;
import org.vertexium.Metadata;
import org.vertexium.Vertex;
import org.vertexium.VertexBuilder;
import org.vertexium.Visibility;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;

public class ImportXml extends CommandLineTool {
    private static final String COUNTRY_ID_PREFIX = "WORLDFACTBOOK_COUNTRY_";
    private static final String FLAG_EDGE_ID_PREFIX = "WORLDFACTBOOK_HAS_FLAG_";
    private static final String MAP_EDGE_ID_PREFIX = "WORLDFACTBOOK_HAS_MAP_";
    private static final String MULTI_VALUE_KEY = ImportXml.class.getName();
    private XPathExpression countryXPath;
    private XPathExpression fieldsXPath;
    private Visibility visibility = new Visibility("");
    private VisibilityTranslator visibilityTranslator;
    private FileImport fileImport;
    private String visibilitySource = "";
    private String entityHasImageIri;

    @Parameter(names = {"--in", "-i"}, required = true, arity = 1, converter = FileConverter.class, description = "XML Input file")
    private File inFile;

    @Parameter(names = {"--indir"}, required = true, arity = 1, converter = FileConverter.class, description = "Worldfact Book Download expanded directory")
    private File inDir;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new ImportXml(), args);
    }

    @Override
    protected int run() throws Exception {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        countryXPath = xPathfactory.newXPath().compile("//country");
        fieldsXPath = xPathfactory.newXPath().compile("field");

        if (!inFile.exists()) {
            System.err.println("Input file " + inFile.getAbsolutePath() + " does not exist.");
            return 1;
        }

        if (!inDir.exists()) {
            System.err.println("Input directory " + inDir.getAbsolutePath() + " does not exist.");
            return 1;
        }

        entityHasImageIri = getOntologyRepository().getRequiredRelationshipIRIByIntent("entityHasImage");

        importXml(inFile, inDir);

        getGraph().flush();

        return 0;
    }

    private void importXml(File inFile, File inDirFile) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inFile);

        NodeList countryNodes = (NodeList) countryXPath.evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < countryNodes.getLength(); i++) {
            Node countryNode = countryNodes.item(i);
            String countryId = getAttributeValue(countryNode, "id");
            Vertex countryVertex = importCountryNode(countryId, countryNode);
            importCountryFlag(inDirFile, countryId, countryVertex);
            importCountyMap(inDirFile, countryId, countryVertex);
        }
    }

    private Vertex importCountryFlag(File inDirFile, String countryId, Vertex countryVertex) throws Exception {
        File flagFileName = new File(inDirFile, "graphics/flags/large/" + countryId + "-lgflag.gif");
        if (!flagFileName.exists()) {
            LOGGER.debug("Could not find flag file: %s", flagFileName);
            return null;
        }

        Vertex flagVertex = fileImport.importFile(flagFileName, false, visibilitySource, null, Priority.NORMAL, getUser(), getAuthorizations());

        String flagTitle = "Flag of " + VisalloProperties.TITLE.getOnlyPropertyValue(countryVertex);
        Metadata flagImageMetadata = new Metadata();
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(flagImageMetadata, 0.5, visibilityTranslator.getDefaultVisibility());
        VisalloProperties.TITLE.addPropertyValue(flagVertex, MULTI_VALUE_KEY, flagTitle, flagImageMetadata, visibility, getAuthorizations());

        getGraph().addEdge(FLAG_EDGE_ID_PREFIX + countryId, countryVertex, flagVertex, entityHasImageIri, visibility, getAuthorizations());
        VisalloProperties.ENTITY_IMAGE_VERTEX_ID.setProperty(countryVertex, flagVertex.getId(), visibility, getAuthorizations());

        return flagVertex;
    }

    private Vertex importCountyMap(File inDirFile, String countryId, Vertex countryVertex) throws Exception {
        File mapFileName = new File(inDirFile, "graphics/maps/newmaps/" + countryId + "-map.gif");
        if (!mapFileName.exists()) {
            LOGGER.debug("Could not find map file: %s", mapFileName);
            return null;
        }

        Vertex mapVertex = fileImport.importFile(mapFileName, false, visibilitySource, null, Priority.NORMAL, getUser(), getAuthorizations());

        String flagTitle = "Map of " + VisalloProperties.TITLE.getOnlyPropertyValue(countryVertex);
        Metadata mapImageMetadata = new Metadata();
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(mapImageMetadata, 0.5, visibilityTranslator.getDefaultVisibility());
        VisalloProperties.TITLE.addPropertyValue(mapVertex, MULTI_VALUE_KEY, flagTitle, mapImageMetadata, visibility, getAuthorizations());

        getGraph().addEdge(MAP_EDGE_ID_PREFIX + countryId, countryVertex, mapVertex, entityHasImageIri, visibility, getAuthorizations());

        return mapVertex;
    }

    private Vertex importCountryNode(String id, Node countryNode) throws XPathExpressionException {
        String name = getAttributeValue(countryNode, "name");

        LOGGER.debug("importing %s:%s", id, name);

        VertexBuilder vertex = getGraph().prepareVertex(COUNTRY_ID_PREFIX + id, visibility);
        VisalloProperties.CONCEPT_TYPE.setProperty(vertex, WorldFactbookOntology.CONCEPT_TYPE_COUNTRY, visibility);
        VisalloProperties.TITLE.addPropertyValue(vertex, MULTI_VALUE_KEY, name, visibility);

        NodeList fieldNodes = (NodeList) fieldsXPath.evaluate(countryNode, XPathConstants.NODESET);
        for (int i = 0; i < fieldNodes.getLength(); i++) {
            Node fieldNode = fieldNodes.item(i);
            String ref = getAttributeValue(fieldNode, "ref");
            String value = fieldNode.getTextContent();
            vertex.addPropertyValue(MULTI_VALUE_KEY, "http://visallo.org/worldfactbook#" + ref, value, visibility);
        }

        return vertex.save(getAuthorizations());
    }

    private String getAttributeValue(Node node, String attributeName) {
        Node attribute = node.getAttributes().getNamedItem(attributeName);
        if (attribute == null) {
            return null;
        }
        return attribute.getNodeValue();
    }

    @Inject
    public void setFileImport(FileImport fileImport) {
        this.fileImport = fileImport;
    }

    @Inject
    public void setVisibilityTranslator(VisibilityTranslator visibilityTranslator) {
        this.visibilityTranslator = visibilityTranslator;
    }
}
