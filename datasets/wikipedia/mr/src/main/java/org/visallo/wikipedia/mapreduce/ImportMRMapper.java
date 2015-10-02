package org.visallo.wikipedia.mapreduce;

import org.apache.commons.codec.binary.Base64;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.sweble.wikitext.engine.EngineException;
import org.sweble.wikitext.engine.PageId;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.WtEngineImpl;
import org.sweble.wikitext.engine.config.WikiConfigImpl;
import org.sweble.wikitext.engine.nodes.EngProcessedPage;
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp;
import org.sweble.wikitext.parser.parser.LinkTargetException;
import org.vertexium.*;
import org.vertexium.accumulo.AccumuloAuthorizations;
import org.vertexium.property.StreamingPropertyValue;
import org.vertexium.util.ConvertingIterable;
import org.vertexium.util.JoinIterable;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.termMention.TermMentionBuilder;
import org.visallo.core.security.DirectVisibilityTranslator;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.vertexium.mapreduce.VisalloElementMapperBase;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.visallo.wikipedia.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

class ImportMRMapper extends VisalloElementMapperBase<LongWritable, Text> {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ImportMRMapper.class);
    public static final String TEXT_XPATH = "/page/revision/text/text()";
    public static final String TITLE_XPATH = "/page/title/text()";
    public static final String REVISION_TIMESTAMP_XPATH = "/page/revision/timestamp/text()";
    public static final SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    public static final String CONFIG_SOURCE_FILE_NAME = "sourceFileName";
    private static final String WIKIPEDIA_PROCESS = ImportMR.class.getName();
    private static final String MULTI_VALUE_KEY = ImportMRMapper.class.getName();

    private XPathExpression<org.jdom2.Text> textXPath;
    private XPathExpression<org.jdom2.Text> titleXPath;
    private XPathExpression<org.jdom2.Text> revisionTimestampXPath;
    private Visibility visibility;
    private Authorizations authorizations;
    private WikiConfigImpl config;
    private WtEngineImpl compiler;
    private String sourceFileName;
    private Counter pagesProcessedCounter;
    private Counter pagesSkippedCounter;
    private VisibilityJson visibilityJson;
    private VisibilityTranslator visibilityTranslator;
    private Visibility defaultVisibility;

    public ImportMRMapper() {
        this.textXPath = XPathFactory.instance().compile(TEXT_XPATH, Filters.text());
        this.titleXPath = XPathFactory.instance().compile(TITLE_XPATH, Filters.text());
        this.revisionTimestampXPath = XPathFactory.instance().compile(REVISION_TIMESTAMP_XPATH, Filters.text());
    }

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        this.visibilityTranslator = new DirectVisibilityTranslator();
        this.visibility = this.visibilityTranslator.getDefaultVisibility();
        this.defaultVisibility = this.visibilityTranslator.getDefaultVisibility();
        this.visibilityJson = new VisibilityJson();
        this.authorizations = new AccumuloAuthorizations();
        this.sourceFileName = context.getConfiguration().get(CONFIG_SOURCE_FILE_NAME);

        try {
            config = DefaultConfigEnWp.generate();
            compiler = new WtEngineImpl(config);
        } catch (Exception ex) {
            throw new IOException("Could not configure sweble", ex);
        }

        pagesProcessedCounter = context.getCounter(WikipediaImportCounters.PAGES_PROCESSED);
        pagesSkippedCounter = context.getCounter(WikipediaImportCounters.PAGES_SKIPPED);
    }

    @Override
    protected void safeMap(LongWritable filePosition, Text line, Context context) throws IOException, InterruptedException {
        ParsePage parsePage;

        TextConverter textConverter = new TextConverter(config);

        String pageString = line.toString().replaceAll("\\\\n", "\n");
        try {
            parsePage = new ParsePage(pageString).invoke();
        } catch (JDOMException e) {
            LOGGER.error("Could not parse XML: " + filePosition + ":\n" + pageString, e);
            context.getCounter(WikipediaImportCounters.XML_PARSE_ERRORS).increment(1);
            return;
        }
        context.progress();

        if (shouldSkip(parsePage)) {
            pagesSkippedCounter.increment(1);
            return;
        }

        String wikipediaPageVertexId = WikipediaConstants.getWikipediaPageVertexId(parsePage.getPageTitle());
        context.setStatus(wikipediaPageVertexId);

        try {
            String wikitext = getPageText(parsePage.getWikitext(), wikipediaPageVertexId, textConverter);
            parsePage.setWikitext(wikitext);
        } catch (Exception ex) {
            LOGGER.error("Could not process wikipedia text: " + filePosition + ":\n" + parsePage.getWikitext(), ex);
            context.getCounter(WikipediaImportCounters.WIKI_TEXT_PARSE_ERRORS).increment(1);
            return;
        }
        context.progress();

        String multiKey = ImportMR.MULTI_VALUE_KEY + '#' + parsePage.getPageTitle();

        Vertex pageVertex = savePage(wikipediaPageVertexId, parsePage, pageString, multiKey);
        context.progress();

        savePageLinks(context, pageVertex, textConverter, multiKey);

        pagesProcessedCounter.increment(1);
    }

    private boolean shouldSkip(ParsePage parsePage) {
        String lowerCaseTitle = parsePage.getPageTitle().toLowerCase();
        return lowerCaseTitle.startsWith("wikipedia:");
    }

    private Vertex savePage(String wikipediaPageVertexId, ParsePage parsePage, String pageString, String multiKey) throws IOException, InterruptedException {
        boolean isRedirect = parsePage.getWikitext().startsWith("REDIRECT:");

        StreamingPropertyValue rawPropertyValue = new StreamingPropertyValue(new ByteArrayInputStream(pageString.getBytes()), byte[].class);
        rawPropertyValue.store(true);
        rawPropertyValue.searchIndex(false);

        StreamingPropertyValue textPropertyValue = new StreamingPropertyValue(new ByteArrayInputStream(parsePage.getWikitext().getBytes()), String.class);

        VertexBuilder pageVertexBuilder = prepareVertex(wikipediaPageVertexId, visibility);
        VisalloProperties.CONCEPT_TYPE.setProperty(pageVertexBuilder, WikipediaConstants.WIKIPEDIA_PAGE_CONCEPT_URI, visibility);
        VisalloProperties.MIME_TYPE.addPropertyValue(pageVertexBuilder, MULTI_VALUE_KEY, ImportMR.WIKIPEDIA_MIME_TYPE, visibility);
        VisalloProperties.FILE_NAME.addPropertyValue(pageVertexBuilder, MULTI_VALUE_KEY, sourceFileName, visibility);
        VisalloProperties.SOURCE.addPropertyValue(pageVertexBuilder, MULTI_VALUE_KEY, WikipediaConstants.WIKIPEDIA_SOURCE, visibility);

        Metadata rawMetadata = new Metadata();
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(rawMetadata, isRedirect ? 0.3 : 0.4, defaultVisibility);
        VisalloProperties.RAW.setProperty(pageVertexBuilder, rawPropertyValue, rawMetadata, visibility);

        Metadata titleMetadata = new Metadata();
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(titleMetadata, isRedirect ? 0.3 : 0.4, defaultVisibility);
        WikipediaConstants.PAGE_TITLE.addPropertyValue(pageVertexBuilder, multiKey, parsePage.getPageTitle(), titleMetadata, visibility);

        Metadata sourceUrlMetadata = new Metadata();
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(sourceUrlMetadata, isRedirect ? 0.3 : 0.4, defaultVisibility);
        VisalloProperties.SOURCE_URL.addPropertyValue(pageVertexBuilder, multiKey, parsePage.getSourceUrl(), sourceUrlMetadata, visibility);

        if (parsePage.getRevisionTimestamp() != null) {
            Metadata publishedDateMetadata = new Metadata();
            VisalloProperties.CONFIDENCE_METADATA.setMetadata(publishedDateMetadata, isRedirect ? 0.3 : 0.4, defaultVisibility);
            WikipediaConstants.PUBLISHED_DATE.addPropertyValue(pageVertexBuilder, multiKey, parsePage.getRevisionTimestamp(), publishedDateMetadata, visibility);
        }

        if (!isRedirect) {
            Metadata textMetadata = new Metadata();
            VisalloProperties.TEXT_DESCRIPTION_METADATA.setMetadata(textMetadata, "Text", defaultVisibility);
            VisalloProperties.TEXT.addPropertyValue(pageVertexBuilder, multiKey, textPropertyValue, textMetadata, visibility);
        }

        Vertex pageVertex = pageVertexBuilder.save(authorizations);

        // because save above will cause the StreamingPropertyValue to be read we need to reset the position to 0 for search indexing
        rawPropertyValue.getInputStream().reset();
        textPropertyValue.getInputStream().reset();
        return pageVertex;
    }

    private String getPageText(String wikiText, String wikipediaPageVertexId, TextConverter textConverter) throws LinkTargetException, EngineException {
        String fileTitle = wikipediaPageVertexId;
        PageId pageId = new PageId(PageTitle.make(config, fileTitle), -1);
        EngProcessedPage compiledPage = compiler.postprocess(pageId, wikiText, null);
        String text = (String) textConverter.go(compiledPage.getPage());
        if (text.length() > 0) {
            wikiText = text;
        }
        return wikiText;
    }

    private void savePageLinks(Context context, Vertex pageVertex, TextConverter textConverter, String pageTextKey) throws IOException, InterruptedException {
        for (LinkWithOffsets link : getLinks(textConverter)) {
            savePageLink(context, pageVertex, link, pageTextKey);
            context.progress();
        }
    }

    private void savePageLink(Context context, Vertex pageVertex, LinkWithOffsets link, String pageTextKey) throws IOException, InterruptedException {
        String linkTarget = link.getLinkTargetWithoutHash();
        String linkVertexId = WikipediaConstants.getWikipediaPageVertexId(linkTarget);
        context.setStatus(pageVertex.getId() + " [" + linkVertexId + "]");
        VertexBuilder linkedPageVertexBuilder = prepareVertex(linkVertexId, visibility);
        VisalloProperties.CONCEPT_TYPE.setProperty(linkedPageVertexBuilder, WikipediaConstants.WIKIPEDIA_PAGE_CONCEPT_URI, visibility);
        VisalloProperties.MIME_TYPE.addPropertyValue(linkedPageVertexBuilder, MULTI_VALUE_KEY, ImportMR.WIKIPEDIA_MIME_TYPE, visibility);
        VisalloProperties.SOURCE.addPropertyValue(linkedPageVertexBuilder, MULTI_VALUE_KEY, WikipediaConstants.WIKIPEDIA_SOURCE, visibility);
        VisalloProperties.FILE_NAME.addPropertyValue(linkedPageVertexBuilder, MULTI_VALUE_KEY, sourceFileName, visibility);

        Metadata titleMetadata = new Metadata();
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(titleMetadata, 0.1, defaultVisibility);
        String linkTargetHash = Base64.encodeBase64String(linkTarget.trim().toLowerCase().getBytes());
        WikipediaConstants.PAGE_TITLE.addPropertyValue(linkedPageVertexBuilder, ImportMR.MULTI_VALUE_KEY + "#" + linkTargetHash, linkTarget, titleMetadata, visibility);

        Vertex linkedPageVertex = linkedPageVertexBuilder.save(authorizations);
        Edge edge = addEdge(WikipediaConstants.getWikipediaPageToPageEdgeId(pageVertex, linkedPageVertex),
                pageVertex,
                linkedPageVertex,
                WikipediaConstants.WIKIPEDIA_PAGE_INTERNAL_LINK_WIKIPEDIA_PAGE_CONCEPT_URI,
                visibility,
                authorizations);

        new TermMentionBuilder()
                .outVertex(pageVertex)
                .propertyKey(pageTextKey)
                .start(link.getStartOffset())
                .end(link.getEndOffset())
                .title(linkTarget)
                .conceptIri(WikipediaConstants.WIKIPEDIA_PAGE_CONCEPT_URI)
                .visibilityJson(visibilityJson)
                .process(WIKIPEDIA_PROCESS)
                .resolvedTo(linkedPageVertex, edge)
                .save(getGraph(), visibilityTranslator, authorizations);
    }

    private Iterable<LinkWithOffsets> getLinks(TextConverter textConverter) {
        return new JoinIterable<>(
                new ConvertingIterable<InternalLinkWithOffsets, LinkWithOffsets>(textConverter.getInternalLinks()) {
                    @Override
                    protected LinkWithOffsets convert(InternalLinkWithOffsets internalLinkWithOffsets) {
                        return internalLinkWithOffsets;
                    }
                },
                new ConvertingIterable<RedirectWithOffsets, LinkWithOffsets>(textConverter.getRedirects()) {
                    @Override
                    protected LinkWithOffsets convert(RedirectWithOffsets redirectWithOffsets) {
                        return redirectWithOffsets;
                    }
                }
        );
    }

    private class ParsePage {
        private String pageString;
        private String wikitext;
        private String pageTitle;
        private String sourceUrl;
        private Date revisionTimestamp;

        public ParsePage(String pageString) {
            this.pageString = pageString;
        }

        public String getWikitext() {
            return wikitext;
        }

        public String getPageTitle() {
            return pageTitle;
        }

        public String getSourceUrl() {
            return sourceUrl;
        }

        public Date getRevisionTimestamp() {
            return revisionTimestamp;
        }

        public ParsePage invoke() throws JDOMException, IOException {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new ByteArrayInputStream(pageString.getBytes()));
            pageTitle = textToString(titleXPath.evaluateFirst(doc));
            wikitext = textToString(textXPath.evaluate(doc));
            sourceUrl = "http://en.wikipedia.org/wiki/" + pageTitle;
            String revisionTimestampString = textToString(revisionTimestampXPath.evaluateFirst(doc));
            revisionTimestamp = null;
            try {
                revisionTimestamp = ISO8601DATEFORMAT.parse(revisionTimestampString);
            } catch (Exception ex) {
                LOGGER.error("Could not parse revision timestamp %s", revisionTimestampString, ex);
            }
            return this;
        }

        private String textToString(List<org.jdom2.Text> texts) {
            StringBuilder sb = new StringBuilder();
            for (org.jdom2.Text t : texts) {
                sb.append(textToString(t));
            }
            return sb.toString();
        }

        private String textToString(org.jdom2.Text text) {
            if (text == null) {
                return "";
            }
            return text.getText();
        }

        public void setWikitext(String wikitext) {
            this.wikitext = wikitext;
        }
    }
}