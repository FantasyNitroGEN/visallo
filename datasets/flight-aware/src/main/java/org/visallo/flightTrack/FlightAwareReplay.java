package org.visallo.flightTrack;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;
import com.google.inject.Inject;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.cmdline.converters.WorkQueuePriorityConverter;
import org.visallo.core.model.workQueue.Priority;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.vertexium.Visibility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

@Parameters(commandDescription = "Replay FlightAware data")
public class FlightAwareReplay extends CommandLineTool {
    public static final double DEFAULT_REPLAY_SPEED = 1.0;
    private FlightRepository flightRepository;

    @Parameter(names = {"--in", "-i"}, required = true, arity = 1, converter = FileConverter.class, description = "Input directory")
    private File inDir;

    @Parameter(names = {"--speed", "-s"}, arity = 1, converter = FileConverter.class, description = "The speed to replay")
    private double replaySpeed = DEFAULT_REPLAY_SPEED;

    @Parameter(names = {"--priority", "-p"}, arity = 1, converter = WorkQueuePriorityConverter.class, description = "Priority at which to enqueue")
    private Priority priority = Priority.NORMAL;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new FlightAwareReplay(), args);
    }

    @Override
    protected int run() throws Exception {
        if (!inDir.exists()) {
            System.err.println(inDir.getAbsolutePath() + " does not exist");
            return 1;
        }

        ArrayList<File> files = toOrdered(inDir.listFiles());
        if (files.size() == 0) {
            return 2;
        }

        Date lastFileTime = null;
        Date startTime = new Date();
        for (int i = 0; i < files.size(); i++) {
            File f = files.get(i);
            Date t = parseDateFromFileName(f);
            if (i > 0) {
                long sleepTime = calculateSleepTime(startTime, lastFileTime, replaySpeed, t);
                LOGGER.debug("Sleeping for " + (((double) sleepTime) / 1000.0) + "s");
                Thread.sleep(sleepTime);
            }
            try {
                replayFile(f);
            } catch (Exception ex) {
                LOGGER.error("Could not replay file %s", f.getAbsolutePath(), ex);
            }
            lastFileTime = parseDateFromFileName(f);
        }

        return 0;
    }

    private void replayFile(File file) throws Exception {
        LOGGER.debug("Replaying file: " + file.getName());
        JSONObject json = readFile(file);
        Visibility visibility = new Visibility("");
        this.flightRepository.save(json, visibility, priority, getAuthorizations());
    }

    private static JSONObject readFile(File file) throws IOException, JSONException {
        try (FileInputStream in = new FileInputStream(file)) {
            String s = IOUtils.toString(in);
            return new JSONObject(s);
        }
    }

    private static long calculateSleepTime(Date startTime, Date lastFileTime, double replaySpeed, Date fileTime) {
        double timeSinceStart = new Date().getTime() - startTime.getTime();
        double fileTimeDiff = fileTime.getTime() - lastFileTime.getTime();
        double t = (fileTimeDiff - timeSinceStart) / replaySpeed;
        return (long) Math.max(t, 0);
    }

    private static Date parseDateFromFileName(File file) throws java.text.ParseException {
        String dateStr = file.getName();
        int extIndex = dateStr.lastIndexOf('.');
        dateStr = dateStr.substring(0, extIndex);
        return FlightRepository.ISO8601DATEFORMAT.parse(dateStr);
    }

    private static ArrayList<File> toOrdered(File[] files) {
        ArrayList<File> results = new ArrayList<>();
        for (File f : files) {
            if (f.getName().endsWith(".json")) {
                results.add(f);
            }
        }
        Collections.sort(results, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return results;
    }

    @Inject
    public void setFlightRepository(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }
}
