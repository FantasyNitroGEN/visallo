package org.visallo.translate;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.google.common.io.Files;
import com.google.inject.Inject;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.vertexium.Element;
import org.vertexium.Metadata;
import org.vertexium.Property;
import org.vertexium.mutation.ExistingElementMutation;
import org.vertexium.property.StreamingPropertyValue;

import java.io.*;

@Name("Language Translate")
@Description("Uses a Translator plugin to translate text")
public class TranslateGraphPropertyWorker extends GraphPropertyWorker {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(TranslateGraphPropertyWorker.class);
    private Translator translator;
    private static final Object detectorFactoryLoadLock = new Object();
    private static boolean detectorFactoryLoaded = false;

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);

        synchronized (detectorFactoryLoadLock) {
            if (!detectorFactoryLoaded) {
                File profileDirectory = createTempProfileDirectory();
                DetectorFactory.loadProfile(profileDirectory);
                detectorFactoryLoaded = true;
            }
        }
    }

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        String text = IOUtils.toString(in, "UTF-8");
        if (text.length() < 50) {
            LOGGER.debug("Skipping language detection because the text is too short. (length: %d)", text.length());
            return;
        }

        String language;
        try {
            language = detectLanguage(text);
            if (language == null) {
                return;
            }
        } catch (Throwable ex) {
            LOGGER.warn("Could not detect language", ex);
            return;
        }

        ExistingElementMutation m = data.getElement().prepareMutation();
        VisalloProperties.LANGUAGE_METADATA.setMetadata(m, data.getProperty(), language, getVisibilityTranslator().getDefaultVisibility());

        boolean translated = false;
        String translatedTextPropertyKey = data.getProperty().getKey() + "#en";
        if (!language.equals("en") && !hasTranslatedProperty(data, translatedTextPropertyKey)) {
            LOGGER.debug("translating text of property: %s", data.getProperty().toString());
            String translatedText = translator.translate(text, language, data);
            if (translatedText != null && translatedText.length() > 0) {
                Object translatedTextValue;
                if (data.getProperty().getValue() instanceof StreamingPropertyValue) {
                    translatedTextValue = new StreamingPropertyValue(new ByteArrayInputStream(translatedText.getBytes()), String.class);
                } else {
                    translatedTextValue = translatedText;
                }
                Metadata metadata = data.createPropertyMetadata();
                VisalloProperties.LANGUAGE_METADATA.setMetadata(metadata, "en", getVisibilityTranslator().getDefaultVisibility());
                String description = VisalloProperties.TEXT_DESCRIPTION_METADATA.getMetadataValueOrDefault(data.getProperty().getMetadata(), null);
                if (description == null || description.length() == 0) {
                    description = "Text";
                }
                VisalloProperties.TEXT_DESCRIPTION_METADATA.setMetadata(metadata, description + " (en)", getVisibilityTranslator().getDefaultVisibility());
                VisalloProperties.MIME_TYPE_METADATA.setMetadata(metadata, "text/plain", getVisibilityTranslator().getDefaultVisibility());
                m.addPropertyValue(translatedTextPropertyKey, data.getProperty().getName(), translatedTextValue, metadata, data.getProperty().getVisibility());
                translated = true;
            }
        }

        m.save(getAuthorizations());

        if (translated) {
            getGraph().flush();
            getWorkQueueRepository().pushGraphPropertyQueue(
                    data.getElement(),
                    translatedTextPropertyKey,
                    data.getProperty().getName(),
                    data.getWorkspaceId(),
                    data.getVisibilitySource(),
                    data.getPriority()
            );
        }
    }

    public boolean hasTranslatedProperty(GraphPropertyWorkData data, String translatedTextPropertyKey) {
        return data.getElement().getProperty(translatedTextPropertyKey, data.getProperty().getName()) != null;
    }

    private String detectLanguage(String text) throws LangDetectException, IOException {
        Detector detector = DetectorFactory.create();
        detector.append(text);
        String lang = detector.detect();
        if (lang.length() == 0) {
            return null;
        }
        return lang;
    }

    @Override
    public boolean isHandled(Element element, Property property) {
        return isTextProperty(property);
    }

    public File createTempProfileDirectory() throws IOException {
        File tempDirectory = Files.createTempDir();
        tempDirectory.deleteOnExit();
        String[] filesList = getProfileFilesList();
        for (String profileFileName : filesList) {
            LOGGER.info("Copying langdetect profile file: %s", profileFileName);
            try {
                copyProfileFile(profileFileName, tempDirectory);
            } catch (Exception ex) {
                throw new VisalloException("Could not load profile file '" + profileFileName + "' to '" + tempDirectory + "'");
            }
        }
        LOGGER.info("created profile directory: %s", tempDirectory);
        return tempDirectory;
    }

    public void copyProfileFile(String profileFileName, File tempDirectory) throws IOException {
        File profileFile = new File(tempDirectory, profileFileName);
        String profileFileString = getFileAsString(profileFileName);
        new JSONObject(profileFileString).length(); // validate the json
        try (OutputStream profileFileOut = new FileOutputStream(profileFile)) {
            profileFileOut.write(profileFileString.getBytes("UTF-8"));
        }
        profileFile.deleteOnExit();
    }

    public String getFileAsString(String profileFileName) throws IOException {
        String profileFileString;
        try (InputStream profileFileIn = TranslateGraphPropertyWorker.class.getResourceAsStream(profileFileName)) {
            profileFileString = IOUtils.toString(profileFileIn, "UTF-8");
        }
        return profileFileString;
    }

    public String[] getProfileFilesList() throws IOException {
        String filesListContents = IOUtils.toString(TranslateGraphPropertyWorker.class.getResourceAsStream("files.list"), "UTF-8");
        return filesListContents.split(System.lineSeparator());
    }

    @Inject
    public void setTranslator(Translator translator) {
        this.translator = translator;
    }
}
