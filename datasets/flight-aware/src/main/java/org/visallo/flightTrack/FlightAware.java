package org.visallo.flightTrack;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;
import com.google.inject.Inject;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.cmdline.converters.WorkQueuePriorityConverter;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.json.JSONObject;
import org.vertexium.Visibility;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

@Parameters(commandDescription = "Import FlightAware data")
public class FlightAware extends CommandLineTool {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(FlightAware.class);
    private static final String CONFIG_API_KEY = "flightaware.apikey";
    private static final String CONFIG_USERNAME = "flightaware.username";
    private FlightRepository flightRepository;

    @Parameter(names = {"--query"}, required = true, arity = 1, description = "Flight Aware query (eg \"-idents VRD*\")")
    private String query;

    @Parameter(names = {"--out"}, arity = 1, converter = FileConverter.class, description = "Output directory")
    private File outDir;

    @Parameter(names = {"--priority", "-p"}, arity = 1, converter = WorkQueuePriorityConverter.class, description = "Priority at which to enqueue")
    private Priority priority = Priority.NORMAL;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new FlightAware(), args);
    }

    @Override
    protected int run() throws Exception {
        if (outDir != null) {
            if (!outDir.exists() || !outDir.isDirectory()) {
                System.err.println("Could not find output directory " + outDir);
                return 1;
            }
        }
        String apiKey = getConfiguration().get(CONFIG_API_KEY, null);
        if (apiKey == null) {
            System.err.println("Could not find configuration " + CONFIG_API_KEY);
            return 1;
        }
        String userName = getConfiguration().get(CONFIG_USERNAME, null);
        if (userName == null) {
            System.err.println("Could not find configuration " + CONFIG_USERNAME);
            return 1;
        }
        FlightAwareClient client = new FlightAwareClient(apiKey, userName);

        Visibility visibility = new Visibility("");
        while (true) {
            LOGGER.info("Performing search");
            try {
                JSONObject json = client.search(query);

                if (outDir != null) {
                    String fileName = FlightRepository.ISO8601DATEFORMAT.format(new Date()) + ".json";
                    try (FileOutputStream out = new FileOutputStream(new File(outDir, fileName))) {
                        out.write(json.toString(2).getBytes());
                    }
                }

                flightRepository.save(json, visibility, priority, getAuthorizations());
            } catch (Exception ex) {
                LOGGER.error("Problem doing search", ex);
            }

            Thread.sleep(15 * 60 * 1000);
        }
    }

    @Inject
    public void setFlightRepository(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }
}
