package org.visallo.subrip;

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

@Name("Sub-rip")
@Description("Imports supporting sub-rip transcripts")
public class SubRipTranscriptFileImportSupportingFileHandler extends FileImportSupportingFileHandler {
    public static final String SUBRIP_CC_FILE_NAME_SUFFIX = ".srt";
    public static final StreamingVisalloProperty SUBRIP_CC = new StreamingVisalloProperty("http://visallo.org#subrip");
    private static final String MULTI_VALUE_KEY = SubRipTranscriptFileImportSupportingFileHandler.class.getName();

    @Override
    public boolean isSupportingFile(File f) {
        return f.getName().endsWith(SUBRIP_CC_FILE_NAME_SUFFIX);
    }

    @Override
    public AddSupportingFilesResult addSupportingFiles(VertexBuilder vertexBuilder, File f, Visibility visibility) throws Exception {
        File mappingJsonFile = new File(f.getParentFile(), f.getName() + SUBRIP_CC_FILE_NAME_SUFFIX);
        if (mappingJsonFile.exists()) {
            final FileInputStream subripIn = new FileInputStream(mappingJsonFile);
            StreamingPropertyValue subripValue = new StreamingPropertyValue(subripIn, byte[].class);
            subripValue.searchIndex(false);
            SUBRIP_CC.addPropertyValue(vertexBuilder, MULTI_VALUE_KEY, subripValue, visibility);
            return new AddSupportingFilesResult() {
                @Override
                public void close() throws IOException {
                    subripIn.close();
                }
            };
        }
        return null;
    }
}
