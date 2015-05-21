package org.visallo.rdfTripleImport;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.vertexium.Authorizations;
import org.vertexium.Metadata;
import org.vertexium.Visibility;
import org.visallo.vertexium.mapreduce.VisalloElementMapperBase;

import java.io.IOException;
import java.util.TimeZone;

public class RdfTripleImportMapper extends VisalloElementMapperBase<LongWritable, Text> {
    private RdfTripleImport rdfTripleImport;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        Visibility visibility = new Visibility(context.getConfiguration().get(RdfTripleImportMR.CONFIG_VISIBILITY_STRING, RdfTripleImportMR.CONFIG_VISIBILITY_STRING_DEFAULT));
        Metadata metadata = new Metadata();
        Authorizations authorizations = getGraph().createAuthorizations();
        String timeZoneId = context.getConfiguration().get(RdfTripleImportMR.CONFIG_TIME_ZONE, RdfTripleImportMR.CONFIG_TIME_ZONE_DEFAULT);
        TimeZone timeZone = TimeZone.getTimeZone(timeZoneId);
        rdfTripleImport = new RdfTripleImport(getGraph(), metadata, timeZone, visibility, authorizations);
    }

    @Override
    protected void safeMap(LongWritable key, Text lineText, Context context) throws Exception {
        String line = lineText.toString().trim();
        context.setStatus(line);
        rdfTripleImport.importRdfLine(line);
    }
}
