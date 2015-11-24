package org.visallo.opennlpDictionary;

import com.google.inject.Inject;
import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.namefind.DictionaryNameFinder;
import opennlp.tools.namefind.TokenNameFinder;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.Span;
import opennlp.tools.util.StringList;
import org.vertexium.Element;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.file.FileSystemRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.termMention.TermMentionBuilder;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.opennlpDictionary.model.DictionaryEntry;
import org.visallo.opennlpDictionary.model.DictionaryEntryRepository;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Name("OpenNLP Dictionary Extractor")
@Description("Extracts terms from text using an OpenNLP dictionary file")
public class OpenNLPDictionaryExtractorGraphPropertyWorker extends GraphPropertyWorker {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(OpenNLPDictionaryExtractorGraphPropertyWorker.class);
    private static final int NEW_LINE_CHARACTER_LENGTH = 1;
    private final DictionaryEntryRepository dictionaryEntryRepository;
    private final FileSystemRepository fileSystemRepository;

    private List<TokenNameFinder> finders;
    private Tokenizer tokenizer;
    private String locationIri;
    private String organizationIri;
    private String personIri;

    @Inject
    public OpenNLPDictionaryExtractorGraphPropertyWorker(
            DictionaryEntryRepository dictionaryEntryRepository,
            FileSystemRepository fileSystemRepository
    ) {
        this.dictionaryEntryRepository = dictionaryEntryRepository;
        this.fileSystemRepository = fileSystemRepository;
    }

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        this.locationIri = getOntologyRepository().getRequiredConceptIRIByIntent("location");
        this.organizationIri = getOntologyRepository().getRequiredConceptIRIByIntent("organization");
        this.personIri = getOntologyRepository().getRequiredConceptIRIByIntent("person");
        this.tokenizer = loadTokenizer();
        this.finders = loadFinders();
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        ObjectStream<String> untokenizedLineStream = new PlainTextByLineStream(new InputStreamReader(in));
        String line;
        int charOffset = 0;

        LOGGER.debug("Processing artifact content stream");
        Vertex outVertex = (Vertex) data.getElement();
        List<Vertex> termMentions = new ArrayList<>();
        while ((line = untokenizedLineStream.read()) != null) {
            termMentions.addAll(processLine(outVertex, data.getProperty().getKey(), line, charOffset, VisalloProperties.VISIBILITY_JSON.getPropertyValue(outVertex)));
            getGraph().flush();
            charOffset += line.length() + NEW_LINE_CHARACTER_LENGTH;
        }
        applyTermMentionFilters(outVertex, termMentions);
        pushTextUpdated(data);

        untokenizedLineStream.close();
        LOGGER.debug("Stream processing completed");
    }

    private List<Vertex> processLine(Vertex outVertex, String propertyKey, String line, int charOffset, VisibilityJson visibilityJson) {
        List<Vertex> termMentions = new ArrayList<>();
        String tokenList[] = tokenizer.tokenize(line);
        Span[] tokenListPositions = tokenizer.tokenizePos(line);
        for (TokenNameFinder finder : finders) {
            Span[] foundSpans = finder.find(tokenList);
            for (Span span : foundSpans) {
                termMentions.add(createTermMention(outVertex, propertyKey, charOffset, span, tokenList, tokenListPositions, visibilityJson));
            }
            finder.clearAdaptiveData();
        }
        return termMentions;
    }

    private Vertex createTermMention(Vertex outVertex, String propertyKey, int charOffset, Span foundName, String[] tokens, Span[] tokenListPositions, VisibilityJson visibilityJson) {
        String name = Span.spansToStrings(new Span[]{foundName}, tokens)[0];
        int start = charOffset + tokenListPositions[foundName.getStart()].getStart();
        int end = charOffset + tokenListPositions[foundName.getEnd() - 1].getEnd();
        String type = foundName.getType();
        String ontologyClassUri = mapToOntologyIri(type);

        return new TermMentionBuilder()
                .outVertex(outVertex)
                .propertyKey(propertyKey)
                .start(start)
                .end(end)
                .title(name)
                .conceptIri(ontologyClassUri)
                .visibilityJson(visibilityJson)
                .process(getClass().getName())
                .save(getGraph(), getVisibilityTranslator(), getUser(), getAuthorizations());
    }

    protected String mapToOntologyIri(String type) {
        String ontologyClassUri;
        if ("location".equals(type)) {
            ontologyClassUri = this.locationIri;
        } else if ("organization".equals(type)) {
            ontologyClassUri = this.organizationIri;
        } else if ("person".equals(type)) {
            ontologyClassUri = this.personIri;
        } else {
            ontologyClassUri = VisalloProperties.CONCEPT_TYPE_THING;
        }
        return ontologyClassUri;
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        if (property.getName().equals(VisalloProperties.RAW.getPropertyName())) {
            return false;
        }

        String mimeType = VisalloProperties.MIME_TYPE_METADATA.getMetadataValue(property.getMetadata(), null);
        return !(mimeType == null || !mimeType.startsWith("text"));
    }

    protected List<TokenNameFinder> loadFinders() throws IOException {
        List<TokenNameFinder> finders = new ArrayList<>();
        for (Map.Entry<String, Dictionary> dictionaryEntry : getDictionaries().entrySet()) {
            finders.add(new DictionaryNameFinder(dictionaryEntry.getValue(), dictionaryEntry.getKey()));
        }
        return finders;
    }

    protected Tokenizer loadTokenizer() throws IOException {
        String tokenizerFileName = "/" + OpenNLPDictionaryExtractorGraphPropertyWorker.class.getName() + "/en-token.bin";
        TokenizerModel tokenizerModel;
        try (InputStream tokenizerModelInputStream = fileSystemRepository.getInputStream(tokenizerFileName)) {
            tokenizerModel = new TokenizerModel(tokenizerModelInputStream);
        }

        return new TokenizerME(tokenizerModel);
    }

    private Map<String, Dictionary> getDictionaries() {
        Map<String, Dictionary> dictionaries = new HashMap<>();
        Iterable<DictionaryEntry> entries = dictionaryEntryRepository.findAll(getUser().getSimpleOrmContext());
        for (DictionaryEntry entry : entries) {

            if (!dictionaries.containsKey(entry.getConcept())) {
                dictionaries.put(entry.getConcept(), new Dictionary());
            }

            dictionaries.get(entry.getConcept()).put(tokensToStringList(entry.getTokens()));
        }

        return dictionaries;
    }

    private StringList tokensToStringList(String tokens) {
        return new StringList(tokens.split(" "));
    }
}
