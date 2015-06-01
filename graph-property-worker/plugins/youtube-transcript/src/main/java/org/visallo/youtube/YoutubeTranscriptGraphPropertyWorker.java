package org.visallo.youtube;

import org.vertexium.Element;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.vertexium.mutation.ExistingElementMutation;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkData;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorker;
import org.visallo.core.ingest.video.VideoTranscript;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.VisalloProperties;

import java.io.InputStream;

@Name("Youtube Transcript")
@Description("Processes the .youtubecc file")
public class YoutubeTranscriptGraphPropertyWorker extends GraphPropertyWorker {
    private static final String PROPERTY_KEY = YoutubeTranscriptGraphPropertyWorker.class.getName();

    @Override
    public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
        StreamingPropertyValue youtubeccValue = YoutubeTranscriptFileImportSupportingFileHandler.YOUTUBE_CC.getOnlyPropertyValue(data.getElement());
        VideoTranscript videoTranscript = YoutubeccReader.read(youtubeccValue.getInputStream());

        ExistingElementMutation<Vertex> m = data.getElement().prepareMutation();
        addVideoTranscriptAsTextPropertiesToMutation(m, PROPERTY_KEY, videoTranscript, data.createPropertyMetadata(), data.getVisibility());
        m.save(getAuthorizations());

        getGraph().flush();
        pushVideoTranscriptTextPropertiesOnWorkQueue(data.getElement(), PROPERTY_KEY, videoTranscript, data.getPriority());
    }


    @Override
    public boolean isHandled(Element element, Property property) {
        if (property == null) {
            return false;
        }

        StreamingPropertyValue youtubeccValue = YoutubeTranscriptFileImportSupportingFileHandler.YOUTUBE_CC.getOnlyPropertyValue(element);
        if (youtubeccValue == null) {
            return false;
        }

        if (!property.getName().equals(VisalloProperties.RAW.getPropertyName())) {
            return false;
        }
        String mimeType = (String) property.getMetadata().getValue(VisalloProperties.MIME_TYPE.getPropertyName());
        if (mimeType == null || !mimeType.startsWith("video")) {
            return false;
        }

        return true;
    }
}
