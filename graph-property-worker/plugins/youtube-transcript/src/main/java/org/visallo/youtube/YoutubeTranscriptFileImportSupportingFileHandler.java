package org.visallo.youtube;

import org.visallo.core.ingest.FileImportSupportingFileHandler;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.types.StreamingVisalloProperty;
import org.vertexium.VertexBuilder;
import org.vertexium.Visibility;
import org.vertexium.property.StreamingPropertyValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Name("Youtube Transcript")
@Description("Imports the supporting .youtubecc file")
public class YoutubeTranscriptFileImportSupportingFileHandler extends FileImportSupportingFileHandler {
    public static final String YOUTUBE_CC_FILE_NAME_SUFFIX = ".youtubecc";
    public static final StreamingVisalloProperty YOUTUBE_CC = new StreamingVisalloProperty("http://visallo.org#youtubecc");
    private static final String MULTI_VALUE_KEY = YoutubeTranscriptFileImportSupportingFileHandler.class.getName();

    @Override
    public boolean isSupportingFile(File f) {
        return f.getName().endsWith(YOUTUBE_CC_FILE_NAME_SUFFIX);
    }

    @Override
    public AddSupportingFilesResult addSupportingFiles(VertexBuilder vertexBuilder, File f, Visibility visibility) throws Exception {
        File mappingJsonFile = new File(f.getParentFile(), f.getName() + YOUTUBE_CC_FILE_NAME_SUFFIX);
        if (mappingJsonFile.exists()) {
            final FileInputStream youtubeccIn = new FileInputStream(mappingJsonFile);
            StreamingPropertyValue youtubeValue = new StreamingPropertyValue(youtubeccIn, byte[].class);
            youtubeValue.searchIndex(false);
            YOUTUBE_CC.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, youtubeValue, visibility);
            return new AddSupportingFilesResult() {
                @Override
                public void close() throws IOException {
                    youtubeccIn.close();
                }
            };
        }
        return null;
    }
}
