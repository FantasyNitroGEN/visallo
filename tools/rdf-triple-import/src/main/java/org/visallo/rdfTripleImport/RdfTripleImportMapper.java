package org.visallo.rdfTripleImport;

import com.google.inject.Inject;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.vertexium.Authorizations;
import org.vertexium.Metadata;
import org.vertexium.Visibility;
import org.visallo.common.rdf.RdfTripleImportHelper;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.vertexium.mapreduce.VisalloElementMapperBase;
import org.visallo.vertexium.mapreduce.VisalloMRBase;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.IOException;
import java.util.Date;
import java.util.TimeZone;

public class RdfTripleImportMapper extends VisalloElementMapperBase<LongWritable, Text> {
    private RdfTripleImportHelper rdfTripleImportHelper;
    private User user;
    private VisibilityTranslator visibilityTranslator;
    private UserRepository userRepository;
    private VisibilityJson visibilityJson;
    private String sourceFileName;
    private Visibility visibility;
    private Authorizations authorizations;
    private TimeZone timeZone;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        visibility = new Visibility(context.getConfiguration().get(RdfTripleImportMR.CONFIG_VISIBILITY_STRING, RdfTripleImportMR.CONFIG_VISIBILITY_STRING_DEFAULT));
        authorizations = getGraph().createAuthorizations();
        String timeZoneId = context.getConfiguration().get(RdfTripleImportMR.CONFIG_TIME_ZONE, RdfTripleImportMR.CONFIG_TIME_ZONE_DEFAULT);
        timeZone = TimeZone.getTimeZone(timeZoneId);
        rdfTripleImportHelper = new RdfTripleImportHelper(getGraph());
        InjectHelper.inject(this);
        visibilityJson = new VisibilityJson();
        user = userRepository.getSystemUser();
        sourceFileName = context.getConfiguration().get(VisalloMRBase.CONFIG_SOURCE_FILE_NAME);
    }

    @Override
    protected void safeMap(LongWritable key, Text lineText, Context context) throws Exception {
        String line = lineText.toString().trim();
        context.setStatus(line);
        Metadata metadata = new Metadata();
        Date now = new Date();
        Visibility metadataVisibility = visibilityTranslator.getDefaultVisibility();
        VisalloProperties.SOURCE_FILE_OFFSET_METADATA.setMetadata(metadata, key.get(), metadataVisibility);
        VisalloProperties.SOURCE_FILE_NAME_METADATA.setMetadata(metadata, sourceFileName, metadataVisibility);
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, metadataVisibility);
        VisalloProperties.MODIFIED_DATE_METADATA.setMetadata(metadata, now, metadataVisibility);
        VisalloProperties.MODIFIED_BY_METADATA.setMetadata(metadata, user.getUserId(), metadataVisibility);
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(metadata, GraphRepository.SET_PROPERTY_CONFIDENCE, metadataVisibility);
        rdfTripleImportHelper.importRdfLine(line, metadata, timeZone, visibility, authorizations);
    }

    @Inject
    public void setVisibilityTranslator(VisibilityTranslator visibilityTranslator) {
        this.visibilityTranslator = visibilityTranslator;
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
