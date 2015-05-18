package org.visallo.core.ingest;

import org.visallo.core.model.Description;
import org.visallo.core.model.Name;
import org.visallo.core.model.properties.VisalloProperties;
import org.vertexium.VertexBuilder;
import org.vertexium.Visibility;
import org.vertexium.property.StreamingPropertyValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@Name("Metadata Import")
@Description("Imports a .metadata.json file and assigns it to a metadata JSON property on that vertex")
public class MetadataFileImportSupportingFileHandler extends FileImportSupportingFileHandler {
    private static final String METADATA_JSON_FILE_NAME_SUFFIX = ".metadata.json";

    @Override
    public boolean isSupportingFile(File f) {
        return f.getName().endsWith(METADATA_JSON_FILE_NAME_SUFFIX);
    }

    @Override
    public AddSupportingFilesResult addSupportingFiles(VertexBuilder vertexBuilder, File f, Visibility visibility) throws FileNotFoundException {
        File mappingJsonFile = getMetadataFile(f);
        if (mappingJsonFile.exists()) {
            final FileInputStream mappingJsonInputStream = new FileInputStream(mappingJsonFile);
            StreamingPropertyValue mappingJsonValue = new StreamingPropertyValue(mappingJsonInputStream, byte[].class);
            mappingJsonValue.searchIndex(false);
            VisalloProperties.METADATA_JSON.setProperty(vertexBuilder, mappingJsonValue, visibility);
            return new AddSupportingFilesResult() {
                @Override
                public void close() throws IOException {
                    mappingJsonInputStream.close();
                }
            };
        }
        return null;
    }

    public static File getMetadataFile(File f) {
        return new File(f.getParentFile(), f.getName() + METADATA_JSON_FILE_NAME_SUFFIX);
    }
}
