package org.visallo.tikaMimeType;

import com.google.inject.Inject;
import org.visallo.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import org.visallo.core.ingest.graphProperty.MimeTypeGraphPropertyWorker;
import org.visallo.core.ingest.graphProperty.MimeTypeGraphPropertyWorkerConfiguration;
import org.visallo.core.model.Description;
import org.visallo.core.model.Name;

import java.io.InputStream;

@Name("Tika MIME Type")
@Description("Uses Apache Tika to determine MIME type")
public class TikaMimeTypeGraphPropertyWorker extends MimeTypeGraphPropertyWorker {
    private TikaMimeTypeMapper mimeTypeMapper;

    @Inject
    public TikaMimeTypeGraphPropertyWorker(MimeTypeGraphPropertyWorkerConfiguration configuration) {
        super(configuration);
    }

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        super.prepare(workerPrepareData);
        mimeTypeMapper = new TikaMimeTypeMapper();
    }

    public String getMimeType(InputStream in, String fileName) throws Exception {
        String mimeType = mimeTypeMapper.guessMimeType(in, fileName);
        if (mimeType == null) {
            return null;
        }
        return mimeType;
    }
}
