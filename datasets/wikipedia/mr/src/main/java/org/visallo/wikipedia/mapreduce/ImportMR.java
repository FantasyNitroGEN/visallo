package org.visallo.wikipedia.mapreduce;

import com.beust.jcommander.Parameter;
import com.google.inject.Inject;
import org.apache.accumulo.core.data.Mutation;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.vertexium.accumulo.mapreduce.AccumuloElementOutputFormat;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.vertexium.mapreduce.VisalloMRBase;
import org.visallo.wikipedia.WikipediaConstants;

import java.io.File;
import java.util.List;

public class ImportMR extends VisalloMRBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ImportMR.class);
    public static final String WIKIPEDIA_MIME_TYPE = "text/plain";
    public static final String MULTI_VALUE_KEY = ImportMR.class.getName();

    @Parameter(description = "<infile>")
    private List<String> inFileName;

    private OntologyRepository ontologyRepository;

    @Override
    protected void setupJob(Job job) throws Exception {
        verifyWikipediaPageConcept(ontologyRepository);
        verifyWikipediaPageInternalLinkWikipediaPageRelationship(ontologyRepository);

        job.setJarByClass(ImportMR.class);
        job.setMapperClass(ImportMRMapper.class);
        job.setNumReduceTasks(0);
        job.setMapOutputValueClass(Mutation.class);
        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(AccumuloElementOutputFormat.class);
        FileInputFormat.addInputPath(job, new Path(getConf().get("in")));
    }

    @Override
    protected String getJobName() {
        return "wikipediaImport";
    }

    private void verifyWikipediaPageInternalLinkWikipediaPageRelationship(OntologyRepository ontologyRepository) {
        if (!ontologyRepository.hasRelationshipByIRI(WikipediaConstants.WIKIPEDIA_PAGE_INTERNAL_LINK_WIKIPEDIA_PAGE_CONCEPT_URI)) {
            throw new RuntimeException(WikipediaConstants.WIKIPEDIA_PAGE_INTERNAL_LINK_WIKIPEDIA_PAGE_CONCEPT_URI + " relationship not found");
        }
    }

    private void verifyWikipediaPageConcept(OntologyRepository ontologyRepository) {
        Concept wikipediaPageConcept = ontologyRepository.getConceptByIRI(WikipediaConstants.WIKIPEDIA_PAGE_CONCEPT_URI);
        if (wikipediaPageConcept == null) {
            throw new RuntimeException(WikipediaConstants.WIKIPEDIA_PAGE_CONCEPT_URI + " concept not found");
        }
    }

    @Override
    protected void processArgs(JobConf conf, String[] args) {
        String inFileName = this.inFileName.get(0);
        LOGGER.info("inFileName: %s", inFileName);
        conf.set("in", inFileName);
        conf.set(ImportMRMapper.CONFIG_SOURCE_FILE_NAME, new File(inFileName).getName());
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new ImportMR(), args);
        System.exit(res);
    }

    @Inject
    public void setOntologyRepository(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }
}
