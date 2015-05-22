package org.visallo.rdfTripleImport;

import com.google.inject.Inject;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.vertexium.Authorizations;
import org.vertexium.Metadata;
import org.vertexium.Visibility;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.vertexium.mapreduce.VisalloElementMapperBase;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.io.IOException;
import java.util.Date;
import java.util.TimeZone;

public class RdfTripleImportMapper extends VisalloElementMapperBase<LongWritable, Text> {
    private RdfTripleImport rdfTripleImport;
    private User user;
    private VisibilityTranslator visibilityTranslator;
    private UserRepository userRepository;
    private VisibilityJson visibilityJson;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        Visibility visibility = new Visibility(context.getConfiguration().get(RdfTripleImportMR.CONFIG_VISIBILITY_STRING, RdfTripleImportMR.CONFIG_VISIBILITY_STRING_DEFAULT));
        Authorizations authorizations = getGraph().createAuthorizations();
        String timeZoneId = context.getConfiguration().get(RdfTripleImportMR.CONFIG_TIME_ZONE, RdfTripleImportMR.CONFIG_TIME_ZONE_DEFAULT);
        TimeZone timeZone = TimeZone.getTimeZone(timeZoneId);
        rdfTripleImport = new RdfTripleImport(getGraph(), timeZone, visibility, authorizations);
        InjectHelper.inject(this);
        visibilityJson = new VisibilityJson();
        user = userRepository.getSystemUser();
    }

    @Override
    protected void safeMap(LongWritable key, Text lineText, Context context) throws Exception {
        String line = lineText.toString().trim();
        context.setStatus(line);
        Metadata metadata = new Metadata();
        Date now = new Date();
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, visibilityTranslator.getDefaultVisibility());
        VisalloProperties.MODIFIED_DATE_METADATA.setMetadata(metadata, now, visibilityTranslator.getDefaultVisibility());
        VisalloProperties.MODIFIED_BY_METADATA.setMetadata(metadata, user.getUserId(), visibilityTranslator.getDefaultVisibility());
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(metadata, GraphRepository.SET_PROPERTY_CONFIDENCE, visibilityTranslator.getDefaultVisibility());
        rdfTripleImport.importRdfLine(line, metadata);
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
