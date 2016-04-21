package org.visallo.tikaTextExtractor;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.NumWordsRulesExtractor;
import org.apache.commons.io.IOUtils;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.parser.pdf.VisalloParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.SecureContentHandler;
import org.json.JSONException;
import org.json.JSONObject;
import org.vertexium.Element;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.vertexium.mutation.ExistingElementMutation;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.properties.types.LongVisalloProperty;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * By default raw properties will be text extracted into "http://visallo.org#text" with a text description of "Extracted Text".
 *
 * Configuration:
 *
 * <pre><code>
 * org.visallo.tikaTextExtractor.TikaTextExtractorGraphPropertyWorker.textExtractMapping.prop1.rawPropertyName=http://my.org#prop1
 * org.visallo.tikaTextExtractor.TikaTextExtractorGraphPropertyWorker.textExtractMapping.prop1.extractedTextPropertyName=http://my.org#prop1
 * org.visallo.tikaTextExtractor.TikaTextExtractorGraphPropertyWorker.textExtractMapping.prop1.textDescription=My Property 1
 *
 * org.visallo.tikaTextExtractor.TikaTextExtractorGraphPropertyWorker.textExtractMapping.prop2.rawPropertyName=http://my.org#prop2
 * org.visallo.tikaTextExtractor.TikaTextExtractorGraphPropertyWorker.textExtractMapping.prop2.extractedTextPropertyName=http://my.org#prop2
 * org.visallo.tikaTextExtractor.TikaTextExtractorGraphPropertyWorker.textExtractMapping.prop2.textDescription=My Property 2
 * </code></pre>
 */
@Name("Tika Text Extractor")
@Description("Uses Apache Tika to extract text")
public class TikaTextExtractorGraphPropertyWorker extends GraphPropertyWorker {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(TikaTextExtractorGraphPropertyWorker.class);

    @Deprecated
    public static final String MULTI_VALUE_KEY = TikaTextExtractorGraphPropertyWorker.class.getName();

    private static final String PROPS_FILE = "tika-extractor.properties";
    private static final String DATE_KEYS_PROPERTY = "tika.extraction.datekeys";
    private static final String SUBJECT_KEYS_PROPERTY = "tika.extraction.titlekeys";
    private static final String AUTHOR_PROPERTY = "tika.extractions.author";
    private static final String URL_KEYS_PROPERTY = "tika.extraction.urlkeys";
    private static final String TYPE_KEYS_PROPERTY = "tika.extraction.typekeys";
    private static final String EXT_URL_KEYS_PROPERTY = "tika.extraction.exturlkeys";
    private static final String SRC_TYPE_KEYS_PROPERTY = "tika.extraction.srctypekeys";
    private static final String RETRIEVAL_TIMESTAMP_KEYS_PROPERTY = "tika.extraction.retrievaltimestampkeys";
    private static final String CUSTOM_FLICKR_METADATA_KEYS_PROPERTY = "tika.extraction.customflickrmetadatakeys";
    private static final String NUMBER_OF_PAGES_PROPERTY = "tika.extraction.numberofpageskeys";

    private static final double SYSTEM_ASSIGNED_CONFIDENCE = 0.4;

    private final TikaTextExtractorGraphPropertyWorkerConfiguration configuration;

    private List<String> dateKeys;
    private List<String> subjectKeys;
    private List<String> urlKeys;
    private List<String> typeKeys;
    private List<String> extUrlKeys;
    private List<String> srcTypeKeys;
    private List<String> retrievalTimestampKeys;
    private List<String> customFlickrMetadataKeys;
    private List<String> authorKeys;
    private List<String> numberOfPagesKeys;
    private LongVisalloProperty pageCountProperty;
    private String authorPropertyIri;
    private String titlePropertyIri;

    @Inject
    public TikaTextExtractorGraphPropertyWorker(TikaTextExtractorGraphPropertyWorkerConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);

        // TODO: Create an actual properties class?
        Properties tikaProperties = new Properties();
        try {
            // don't require the properties file
            InputStream propsIn = Thread.currentThread().getContextClassLoader().getResourceAsStream(PROPS_FILE);
            if (propsIn != null) {
                tikaProperties.load(propsIn);
            }
        } catch (IOException e) {
            LOGGER.error("Could not load config: %s", PROPS_FILE);
        }

        String pageCountPropertyIri = getOntologyRepository().getPropertyIRIByIntent("pageCount");
        if (pageCountPropertyIri != null) {
            pageCountProperty = new LongVisalloProperty(pageCountPropertyIri);
        }

        dateKeys = Arrays.asList(tikaProperties.getProperty(DATE_KEYS_PROPERTY, "date,published,pubdate,publish_date,last-modified,atc:last-modified").split(","));
        subjectKeys = Arrays.asList(tikaProperties.getProperty(SUBJECT_KEYS_PROPERTY, "title,subject").split(","));
        urlKeys = Arrays.asList(tikaProperties.getProperty(URL_KEYS_PROPERTY, "url,og:url").split(","));
        typeKeys = Arrays.asList(tikaProperties.getProperty(TYPE_KEYS_PROPERTY, "Content-Type").split(","));
        extUrlKeys = Arrays.asList(tikaProperties.getProperty(EXT_URL_KEYS_PROPERTY, "atc:result-url").split(","));
        srcTypeKeys = Arrays.asList(tikaProperties.getProperty(SRC_TYPE_KEYS_PROPERTY, "og:type").split(","));
        retrievalTimestampKeys = Arrays.asList(tikaProperties.getProperty(RETRIEVAL_TIMESTAMP_KEYS_PROPERTY, "atc:retrieval-timestamp").split(","));
        customFlickrMetadataKeys = Arrays.asList(tikaProperties.getProperty(CUSTOM_FLICKR_METADATA_KEYS_PROPERTY, "Unknown tag (0x9286)").split(","));
        authorKeys = Arrays.asList(tikaProperties.getProperty(AUTHOR_PROPERTY, "author").split(","));
        numberOfPagesKeys = Arrays.asList(tikaProperties.getProperty(NUMBER_OF_PAGES_PROPERTY, "xmpTPg:NPages").split(","));

        authorPropertyIri = getOntologyRepository().getPropertyIRIByIntent("documentAuthor");
        titlePropertyIri = getOntologyRepository().getPropertyIRIByIntent("documentTitle");
        if (titlePropertyIri == null) {
            titlePropertyIri = getOntologyRepository().getPropertyIRIByIntent("artifactTitle");
        }
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        String mimeType = (String) data.getProperty().getMetadata().getValue(VisalloProperties.MIME_TYPE.getPropertyName());
        checkNotNull(mimeType, VisalloProperties.MIME_TYPE.getPropertyName() + " is a required metadata field");

        Charset charset = Charset.forName("UTF-8");
        Metadata metadata = new Metadata();
        metadata.set(Metadata.CONTENT_TYPE, mimeType);
        String text = extractText(in, mimeType, metadata);
        String propertyKey = getPropertyKey(data);
        TikaTextExtractorGraphPropertyWorkerConfiguration.TextExtractMapping textExtractMapping
                = configuration.getTextExtractMapping(data.getElement(), data.getProperty());

        ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();

        // TODO set("url", extractUrl(metadata));
        // TODO set("type", extractTextField(metadata, typeKeys));
        // TODO set("extUrl", extractTextField(metadata, extUrlKeys));
        // TODO set("srcType", extractTextField(metadata, srcTypeKeys));

        String author = extractTextField(metadata, authorKeys);
        if (authorPropertyIri != null && author != null && author.length() > 0) {
            m.addPropertyValue(propertyKey, authorPropertyIri, author, data.createPropertyMetadata(), data.getVisibility());
        }

        String customImageMetadata = extractTextField(metadata, customFlickrMetadataKeys);
        org.vertexium.Metadata textMetadata = data.createPropertyMetadata();
        VisalloProperties.MIME_TYPE_METADATA.setMetadata(textMetadata, "text/plain", getVisibilityTranslator().getDefaultVisibility());
        if (!Strings.isNullOrEmpty(textExtractMapping.getTextDescription())) {
            VisalloProperties.TEXT_DESCRIPTION_METADATA.setMetadata(
                    textMetadata,
                    textExtractMapping.getTextDescription(),
                    getVisibilityTranslator().getDefaultVisibility()
            );
        }

        if (customImageMetadata != null && !customImageMetadata.equals("")) {
            try {
                JSONObject customImageMetadataJson = new JSONObject(customImageMetadata);

                text = new JSONObject(customImageMetadataJson.get("description").toString()).get("_content") +
                        "\n" + customImageMetadataJson.get("tags").toString();
                StreamingPropertyValue textValue = new StreamingPropertyValue(new ByteArrayInputStream(text.getBytes(charset)), String.class);
                addTextProperty(textExtractMapping, m, propertyKey, textValue, textMetadata, data.getVisibility());

                Date lastUpdate = GenericDateExtractor
                        .extractSingleDate(customImageMetadataJson.get("lastupdate").toString());
                VisalloProperties.MODIFIED_DATE.setProperty(m, lastUpdate, data.createPropertyMetadata(), data.getVisibility());

                // TODO set("retrievalTime", Long.parseLong(customImageMetadataJson.get("atc:retrieval-timestamp").toString()));

                org.vertexium.Metadata titleMetadata = data.createPropertyMetadata();
                VisalloProperties.CONFIDENCE_METADATA.setMetadata(titleMetadata, SYSTEM_ASSIGNED_CONFIDENCE, getVisibilityTranslator().getDefaultVisibility());
                if (titlePropertyIri != null) {
                    m.addPropertyValue(propertyKey, titlePropertyIri, customImageMetadataJson.get("title").toString(), titleMetadata, data.getVisibility());
                }
            } catch (JSONException e) {
                LOGGER.warn("Image returned invalid custom metadata");
            }
        } else {
            StreamingPropertyValue textValue = new StreamingPropertyValue(new ByteArrayInputStream(text.getBytes(charset)), String.class);
            addTextProperty(textExtractMapping, m, propertyKey, textValue, textMetadata, data.getVisibility());

            VisalloProperties.MODIFIED_DATE.setProperty(m, extractDate(metadata), data.createPropertyMetadata(), data.getVisibility());
            String title = extractTextField(metadata, subjectKeys);
            if (title != null && title.length() > 0) {
                org.vertexium.Metadata titleMetadata = data.createPropertyMetadata();
                VisalloProperties.CONFIDENCE_METADATA.setMetadata(titleMetadata, SYSTEM_ASSIGNED_CONFIDENCE, getVisibilityTranslator().getDefaultVisibility());
                if (titlePropertyIri != null) {
                    m.addPropertyValue(propertyKey, titlePropertyIri, title, titleMetadata, data.getVisibility());
                }
            }

            // TODO set("retrievalTime", extractRetrievalTime(metadata));

            if (pageCountProperty != null) {
                String numberOfPages = extractTextField(metadata, numberOfPagesKeys);
                if (numberOfPages != null) {
                    org.vertexium.Metadata numberOfPagesMetadata = data.createPropertyMetadata();
                    VisalloProperties.CONFIDENCE_METADATA.setMetadata(numberOfPagesMetadata, SYSTEM_ASSIGNED_CONFIDENCE, getVisibilityTranslator().getDefaultVisibility());
                    pageCountProperty.addPropertyValue(m, propertyKey, Long.valueOf(numberOfPages), numberOfPagesMetadata, data.getVisibility());
                }
            }
        }

        m.save(getAuthorizations());

        getGraph().flush();
        getWorkQueueRepository().pushGraphPropertyQueue(
                data.getElement(),
                propertyKey,
                textExtractMapping.getExtractedTextPropertyName(),
                data.getWorkspaceId(),
                data.getVisibilitySource(),
                data.getPriority()
        );
    }

    private void addTextProperty(
            TikaTextExtractorGraphPropertyWorkerConfiguration.TextExtractMapping textExtractMapping,
            ExistingElementMutation<Vertex> m,
            String propertyKey,
            StreamingPropertyValue textValue,
            org.vertexium.Metadata textMetadata,
            Visibility visibility
    ) {
        m.addPropertyValue(propertyKey, textExtractMapping.getExtractedTextPropertyName(), textValue, textMetadata, visibility);
    }

    private String getPropertyKey(GraphPropertyWorkData data) {
        // To support legacy code that stored the extracted text into a predefined multi-valued key we need
        //   to look for the old text property with MULTI_VALUE_KEY and use that key if we find it
        if (VisalloProperties.TEXT.getProperty(data.getElement(), MULTI_VALUE_KEY) != null) {
            return MULTI_VALUE_KEY;
        }
        return data.getProperty().getKey();
    }

    private String extractText(InputStream in, String mimeType, Metadata metadata) throws IOException, SAXException, TikaException, BoilerpipeProcessingException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(in, out);
        byte[] textBytes = out.toByteArray();
        String text;

        metadata.set(Metadata.CONTENT_TYPE, mimeType);
        String bodyContent = extractTextWithTika(textBytes, metadata);

        if (isHtml(mimeType)) {
            text = extractTextFromHtml(IOUtils.toString(textBytes, "UTF-8"));
            if (text == null || text.length() == 0) {
                text = cleanExtractedText(bodyContent);
            }
        } else {
            text = cleanExtractedText(bodyContent);
        }

        return Normalizer.normalize(text, Normalizer.Form.NFC);
    }

    private static String extractTextWithTika(byte[] textBytes, Metadata metadata) throws TikaException, SAXException, IOException {
        TikaConfig tikaConfig = TikaConfig.getDefaultConfig();
        CompositeParser compositeParser = new CompositeParser(tikaConfig.getMediaTypeRegistry(), tikaConfig.getParser());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(output, "UTF-8");
        ContentHandler handler = new BodyContentHandler(writer);
        ParseContext context = new ParseContext();
        context.set(PDFParserConfig.class, new VisalloParserConfig());
        ByteArrayInputStream stream = new ByteArrayInputStream(textBytes);

        TemporaryResources tmp = new TemporaryResources();
        try {
            TikaInputStream tis = TikaInputStream.get(stream, tmp);

            // TIKA-216: Zip bomb prevention
            SecureContentHandler sch = new SecureContentHandler(handler, tis);
            try {
                compositeParser.parse(tis, sch, metadata, context);
            } catch (SAXException e) {
                // Convert zip bomb exceptions to TikaExceptions
                sch.throwIfCauseOf(e);
                throw e;
            }
        } finally {
            tmp.dispose();
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("extracted %d bytes", output.size());
            LOGGER.debug("metadata");
            for (String metadataName : metadata.names()) {
                LOGGER.debug("  %s: %s", metadataName, metadata.get(metadataName));
            }
        }
        return IOUtils.toString(output.toByteArray(), "UTF-8");
    }

    private String extractTextFromHtml(String text) throws BoilerpipeProcessingException {
        String extractedText;

        text = cleanHtml(text);

        extractedText = NumWordsRulesExtractor.getInstance().getText(text);
        if (extractedText != null && extractedText.length() > 0) {
            return extractedText;
        }

        extractedText = ArticleExtractor.getInstance().getText(text);
        if (extractedText != null && extractedText.length() > 0) {
            return extractedText;
        }

        return null;
    }

    private String cleanHtml(String text) {
        text = text.replaceAll("&mdash;", "--");
        text = text.replaceAll("&ldquo;", "\"");
        text = text.replaceAll("&rdquo;", "\"");
        text = text.replaceAll("&lsquo;", "'");
        text = text.replaceAll("&rsquo;", "'");
        return text;
    }

    private Date extractDate(Metadata metadata) {
        // find the date metadata property, if there is one
        String dateKey = TikaMetadataUtils.findKey(dateKeys, metadata);
        Date date = null;
        if (dateKey != null) {
            date = GenericDateExtractor
                    .extractSingleDate(metadata.get(dateKey));
        }

        if (date == null) {
            date = new Date();
        }

        return date;
    }

    private Long extractRetrievalTime(Metadata metadata) {
        Long retrievalTime = 0l;
        String retrievalTimeKey = TikaMetadataUtils.findKey(retrievalTimestampKeys, metadata);

        if (retrievalTimeKey != null) {
            retrievalTime = Long.parseLong(metadata.get(retrievalTimeKey));
        }

        return retrievalTime;
    }

    private String extractTextField(Metadata metadata, List<String> keys) {
        // find the title metadata property, if there is one
        String field = "";
        String fieldKey = TikaMetadataUtils.findKey(keys, metadata);

        if (fieldKey != null) {
            field = metadata.get(fieldKey);
        }

        if (field != null) {
            field = field.trim();
        }
        return field;
    }

    private String extractUrl(Metadata metadata) {
        // find the url metadata property, if there is one; strip down to domain name
        String urlKey = TikaMetadataUtils.findKey(urlKeys, metadata);
        String host = "";
        if (urlKey != null) {
            String url = metadata.get(urlKey);
            try {
                URL netUrl = new URL(url);
                host = netUrl.getHost();
                if (host.startsWith("www")) {
                    host = host.substring("www".length() + 1);
                }
            } catch (MalformedURLException e) {
                throw new RuntimeException("Bad url: " + url);
            }
        }
        return host;
    }

    private boolean isHtml(String mimeType) {
        return mimeType.contains("html");
    }

    private String cleanExtractedText(String extractedText) {
        return extractedText
                // Normalize line breaks
                .replaceAll("\r", "\n")
                // Remove tabs
                .replaceAll("\t", " ")
                // Remove non-breaking spaces
                .replaceAll("\u00A0", " ")
                // Remove newlines that are just paragraph wrapping
                .replaceAll("(?<![\\n])[\\n](?![\\n])", " ")
                // Remove remaining newlines with exactly 2
                .replaceAll("([ ]*\\n[ ]*)+", "\n\n")
                // Remove duplicate spaces
                .replaceAll("[ ]+", " ");
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        String mimeType = VisalloProperties.MIME_TYPE_METADATA.getMetadataValue(property.getMetadata());
        if (mimeType == null) {
            return false;
        }

        if (mimeType.startsWith("image") || mimeType.startsWith("video") || mimeType.startsWith("audio")) {
            return false;
        }

        return configuration.isHandled(element, property);
    }
}

