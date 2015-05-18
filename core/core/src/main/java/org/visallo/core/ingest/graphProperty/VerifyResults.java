package org.visallo.core.ingest.graphProperty;

import org.visallo.core.model.ontology.OntologyRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class VerifyResults {
    private List<Failure> failures = new ArrayList<>();

    public boolean verifyRequiredPropertyIntent(OntologyRepository ontologyRepository, String intentName) {
        String propertyIri = ontologyRepository.getPropertyIRIByIntent(intentName);
        if (propertyIri == null || propertyIri.length() == 0) {
            addFailure(new RequiredPropertyIntentFailure(intentName));
            return false;
        }
        return true;
    }

    public boolean verifyRequiredExecutable(String executableName) {
        try {
            final ProcessBuilder procBuilder = new ProcessBuilder("which", executableName);
            Process proc = procBuilder.start();
            if (proc.waitFor() == 0) {
                return true;
            }
        } catch (Exception ex) {
            String path = System.getenv("PATH");
            String[] pathParts = path.split(File.pathSeparator);
            for (String pathPart : pathParts) {
                if (new File(pathPart, executableName).exists()) {
                    return true;
                }
            }
        }

        addFailure(new RequiredExecutableFailure(executableName));
        return false;
    }

    public void addFailure(Failure failure) {
        failures.add(failure);
    }

    public Collection<Failure> getFailures() {
        return failures;
    }

    public String toString() {
        return String.format("VerifyResults: %d failures", getFailures().size());
    }

    public static abstract class Failure {
        public abstract String getMessage();

        public String toString() {
            return getMessage();
        }
    }

    public static class RequiredPropertyIntentFailure extends Failure {
        private final String intentName;

        public RequiredPropertyIntentFailure(String intentName) {
            this.intentName = intentName;
        }

        public String getIntentName() {
            return intentName;
        }

        @Override
        public String getMessage() {
            return String.format("Missing required property intent: %s", getIntentName());
        }
    }

    private class RequiredExecutableFailure extends Failure {
        private final String executableName;

        public RequiredExecutableFailure(String executableName) {
            this.executableName = executableName;
        }

        public String getExecutableName() {
            return executableName;
        }

        @Override
        public String getMessage() {
            return String.format("Missing required executable: %s", getExecutableName());
        }
    }

    public static class GenericFailure extends Failure {
        private final String message;

        public GenericFailure(String message) {
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }
}
