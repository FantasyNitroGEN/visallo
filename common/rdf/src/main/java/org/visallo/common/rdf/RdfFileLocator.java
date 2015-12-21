package org.visallo.common.rdf;

import org.visallo.core.exception.VisalloException;

import java.io.File;

class RdfFileLocator {
    public static File findBestFile(File input) {
        File inputFile = findRdfFile(input);
        if (inputFile == null && input.isDirectory()) {
            inputFile = findFirstFile(input);
        }
        return inputFile;
    }

    public static File findRdfFile(File input) {
        return findFile(input, new RdfFinder());
    }

    public static File findFirstFile(File input) {
        return findFile(input, new FileFinder());
    }

    /**
     * performs a breadth first search to prefer root files over children
     */
    private static File findFile(File input, Finder finder) {
        if (input.isFile()) {
            return input;
        }

        File[] files = input.listFiles();
        if (files == null) {
            throw new VisalloException("Could not get files from: " + input.getAbsolutePath());
        }

        for (File file : files) {
            if (finder.matches(file)) {
                return file;
            }
        }

        for (File file : files) {
            if (file.isDirectory()) {
                File rdfFile = findFile(file, finder);
                if (rdfFile != null) {
                    return rdfFile;
                }
            }
        }

        return null;
    }

    private abstract static class Finder {
        public abstract boolean matches(File file);
    }

    private static class FileFinder extends Finder {
        @Override
        public boolean matches(File file) {
            return file.isFile();
        }
    }

    private static class RdfFinder extends Finder {
        @Override
        public boolean matches(File file) {
            if (!file.isFile()) {
                return false;
            }

            String fileName = file.getAbsolutePath().toLowerCase();
            return fileName.endsWith(".nt") || fileName.endsWith(".xml");
        }
    }
}
