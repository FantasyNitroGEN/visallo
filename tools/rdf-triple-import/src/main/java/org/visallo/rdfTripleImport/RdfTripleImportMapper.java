package org.visallo.rdfTripleImport;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.vertexium.Authorizations;
import org.vertexium.Metadata;
import org.vertexium.Visibility;
import org.visallo.core.exception.VisalloException;
import org.visallo.vertexium.mapreduce.VisalloElementMapperBase;

import java.io.IOException;

public class RdfTripleImportMapper extends VisalloElementMapperBase<LongWritable, Text> {
    private RdfTripleImport rdfTripleImport;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        Visibility visibility = new Visibility(context.getConfiguration().get(RdfTripleImportMR.CONFIG_VISIBILITY_STRING, ""));
        Metadata metadata = new Metadata();
        Authorizations authorizations = getGraph().createAuthorizations();
        rdfTripleImport = new RdfTripleImport(getGraph(), metadata, visibility, authorizations);
    }

    @Override
    protected void safeMap(LongWritable key, Text lineText, Context context) throws Exception {
        String line = lineText.toString().trim();
        context.setStatus(line);
        rdfTripleImport.importRdfLine(line);
    }
}
