package org.visallo.quickStart;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;
import org.h2.Driver;
import org.h2.tools.RunScript;
import org.vertexium.elasticsearch.ElasticSearchSearchIndexConfiguration;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.FileConfigurationLoader;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.http.CachingHttpRepository;
import org.visallo.quickStart.gui.VisalloWindow;
import org.visallo.web.JettyWebServer;
import org.visallo.web.WebConfiguration;
import org.visallo.web.WebServer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

public class QuickStartJettyWebServer {

    private static class Arguments {
        @Parameter(names = {"--help", "-h"}, description = "Print help", help = true)
        private boolean help;

        @Parameter(names = {"--workingDir"}, converter = FileConverter.class, description = "Working directory")
        public File workingDirectory;
    }

    public static void main(String[] argsArray) throws Exception {
        new QuickStartJettyWebServer().run(argsArray);
    }

    protected void run(String[] argsArray) throws Exception {
        Arguments args = new Arguments();
        JCommander cmd = new JCommander(args, argsArray);
        if (args.help) {
            cmd.usage();
            System.exit(-1);
            return;
        }

        checkForConsole();
        System.out.println("Starting Visallo...");

        QuickStartWorkingDirectory workingDirectory = createQuickStartWorkingDirectory(args);
        File keyStoreFile = workingDirectory.getFile("quick-start.visallo.org.jks");
        File createSqlFile = workingDirectory.getFile("sql/create.sql");
        File h2Location = getH2Location(workingDirectory);

        String[] newArgs = createNewArguments(workingDirectory, keyStoreFile);
        updateSystemProperties(workingDirectory, h2Location.getAbsolutePath());

        createDatabaseIfNeeded(createSqlFile);

        CommandLineTool.main(new JettyWebServer(), newArgs, false);
    }

    protected QuickStartWorkingDirectory createQuickStartWorkingDirectory(Arguments args) throws IOException {
        return new QuickStartWorkingDirectory(args.workingDirectory);
    }

    private void checkForConsole() {
        if (System.console() == null && System.getProperty("debugger") == null) {
            VisalloWindow visalloWindow = new VisalloWindow(WebServer.DEFAULT_SERVER_PORT);
            PrintStream guiOut = new PrintStream(visalloWindow.getOutputStream());
            System.setOut(guiOut);
            System.setErr(guiOut);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private File getH2Location(QuickStartWorkingDirectory workingDirectory) {
        File h2Location = workingDirectory.getFile("h2/visallo");
        h2Location.getParentFile().mkdirs();
        return h2Location;
    }

    private void createDatabaseIfNeeded(File createSqlFile) {
        try {
            Class.forName(Driver.class.getName());
            String connectionString = System.getProperty("visallo.sql.connectionString");
            String userName = "root";
            String password = "";
            try (Connection conn = DriverManager.getConnection(connectionString, userName, password)) {
                RunScript.execute(conn, new FileReader(createSqlFile));
            }
        } catch (Exception ex) {
            throw new VisalloException("Could not execute create.sql script", ex);
        }
    }

    protected void updateSystemProperties(QuickStartWorkingDirectory workingDirectory, String h2Location) {
        System.setProperty(FileConfigurationLoader.ENV_VISALLO_DIR, workingDirectory.getDirectory().getAbsolutePath());
        System.setProperty(FileConfigurationLoader.ENV_SEARCH_LOCATIONS, FileConfigurationLoader.SearchLocation.SystemProperty.getValue());
        addOntologyToSystemProperties(workingDirectory);
        System.setProperty("visallo.sql.connectionString", "jdbc:h2:file:" + h2Location + ";AUTO_SERVER=TRUE");
        System.setProperty("visallo." + CachingHttpRepository.CONFIG_CACHE_DIR, workingDirectory.getFile("httpCache").getAbsolutePath());
        System.setProperty("visallo." + WebConfiguration.FIELD_JUSTIFICATION_VALIDATION, "OPTIONAL");
        File elasticsearchDirectory = workingDirectory.getFile("elasticsearch");
        System.setProperty("visallo.graph.search." + ElasticSearchSearchIndexConfiguration.IN_PROCESS_NODE_DATA_PATH, new File(elasticsearchDirectory, "data").getAbsolutePath());
        System.setProperty("visallo.graph.search." + ElasticSearchSearchIndexConfiguration.IN_PROCESS_NODE_LOGS_PATH, new File(elasticsearchDirectory, "logs").getAbsolutePath());
        System.setProperty("visallo.graph.search." + ElasticSearchSearchIndexConfiguration.IN_PROCESS_NODE_WORK_PATH, new File(elasticsearchDirectory, "work").getAbsolutePath());
    }

    protected void addOntologyToSystemProperties(QuickStartWorkingDirectory workingDirectory) {
        System.setProperty("visallo." + Configuration.ONTOLOGY_REPOSITORY_OWL + ".quick-start.iri", "http://visallo.org/quick-start");
        System.setProperty("visallo." + Configuration.ONTOLOGY_REPOSITORY_OWL + ".quick-start.file", workingDirectory.getFile("ontology/quick-start.owl").getAbsolutePath());
    }

    protected String[] createNewArguments(QuickStartWorkingDirectory workingDirectory, File keyStoreFile) {
        List<String> newArgs = new ArrayList<>();

        newArgs.add("--webAppDir");
        newArgs.add(workingDirectory.getFile("webapp").getAbsolutePath());

        newArgs.add("--keyStorePath");
        newArgs.add(keyStoreFile.getAbsolutePath());

        newArgs.add("--keyStorePassword");
        newArgs.add("password");

        return newArgs.toArray(new String[newArgs.size()]);
    }
}
