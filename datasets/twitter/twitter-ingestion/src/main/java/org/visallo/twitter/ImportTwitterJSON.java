package org.visallo.twitter;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;
import com.google.inject.Inject;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.cmdline.converters.WorkQueuePriorityConverter;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import twitter4j.Status;
import twitter4j.StatusFactory;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class ImportTwitterJSON extends CommandLineTool {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ImportTwitterJSON.class);
    private TweetTransformer tweetTransformer;

    @Parameter(names = {"--inFile", "-i"}, required = true, arity = 1, converter = FileConverter.class, description = "the input tweets.json[.gz] file")
    private File inFile;

    @Parameter(names = {"--limit", "-n"}, required = false, description = "the maximum number of tweets to import")
    private Integer limit;

    @Parameter(names = {"--priority", "-p"}, required = false, arity = 1, converter = WorkQueuePriorityConverter.class)
    Priority priority = Priority.LOW;

    @Inject
    public void setTweetTransformer(TweetTransformer tweetTransformer) {
        this.tweetTransformer = tweetTransformer;
    }

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new ImportTwitterJSON(), args);
    }

    @Override
    protected int run() throws Exception {
        InputStream inputStream = new FileInputStream(inFile);
        if (inFile.getName().endsWith(".gz")) {
            inputStream = new GZIPInputStream(inputStream);
        }
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        int lineNumber = 0;
        int importCount = 0;
        int errorCount = 0;
        while ((line = bufferedReader.readLine()) != null) {
            if (limit != null && importCount >= limit) {
                break;
            }
            LOGGER.debug("Tweet %d: %s", ++lineNumber, line);
            try {
                Status tweetStatus = StatusFactory.createStatus(line);
                tweetTransformer.transformTweetStatus(tweetStatus, priority);
                importCount++;
            } catch (Exception ex) {
                LOGGER.error("Could not transform tweet status", ex);
                errorCount++;
            }
        }
        LOGGER.info("Imported %d tweets (%d errors) from: %s", importCount, errorCount, inFile);
        return 0;
    }
}
