package org.visallo.phoneNumber;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.i18n.phonenumbers.PhoneNumberMatch;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.termMention.TermMentionBuilder;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.VisibilityJson;
import org.vertexium.Element;
import org.vertexium.Property;
import org.vertexium.Vertex;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.vertexium.util.IterableUtils.count;

@Name("Phone Number Extractor")
@Description("Extracts phone numbers from text")
public class PhoneNumberGraphPropertyWorker extends GraphPropertyWorker {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(PhoneNumberGraphPropertyWorker.class);
    public static final String DEFAULT_REGION_CODE = "phoneNumber.defaultRegionCode";
    public static final String DEFAULT_DEFAULT_REGION_CODE = "US";

    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    private String defaultRegionCode;
    private String entityType;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);

        defaultRegionCode = (String) workerPrepareData.getConfiguration().get(DEFAULT_REGION_CODE);
        if (defaultRegionCode == null) {
            defaultRegionCode = DEFAULT_DEFAULT_REGION_CODE;
        }

        entityType = getOntologyRepository().getRequiredConceptIRIByIntent("phoneNumber");
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        LOGGER.debug("Extracting phone numbers from provided text");

        final String text = CharStreams.toString(new InputStreamReader(in, Charsets.UTF_8));

        Vertex outVertex = (Vertex) data.getElement();
        VisibilityJson visibilityJson = VisalloProperties.VISIBILITY_JSON.getPropertyValue(outVertex);
        final Iterable<PhoneNumberMatch> phoneNumbers = phoneNumberUtil.findNumbers(text, defaultRegionCode);
        List<Vertex> termMentions = new ArrayList<>();
        for (final PhoneNumberMatch phoneNumber : phoneNumbers) {
            final String formattedNumber = phoneNumberUtil.format(phoneNumber.number(), PhoneNumberUtil.PhoneNumberFormat.E164);
            int start = phoneNumber.start();
            int end = phoneNumber.end();

            Vertex termMention = new TermMentionBuilder()
                    .outVertex(outVertex)
                    .propertyKey(data.getProperty().getKey())
                    .start(start)
                    .end(end)
                    .title(formattedNumber)
                    .conceptIri(entityType)
                    .visibilityJson(visibilityJson)
                    .process(getClass().getName())
                    .save(getGraph(), getVisibilityTranslator(), getAuthorizations());
            termMentions.add(termMention);
        }
        getGraph().flush();
        applyTermMentionFilters(outVertex, termMentions);
        pushTextUpdated(data);

        LOGGER.debug("Number of phone numbers extracted: %d", count(phoneNumbers));
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
}
