package org.visallo.opennlpDictionary;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;
import com.google.inject.Inject;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.user.User;
import org.visallo.opennlpDictionary.model.DictionaryEntryRepository;
import org.apache.commons.io.FilenameUtils;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;

import static com.google.common.base.Preconditions.checkNotNull;

@Parameters(commandDescription = "Import dictionary files used by OpenNLPDictionaryExtractorGraphPropertyWorker")
public class DictionaryImporter extends CommandLineTool {
    private DictionaryEntryRepository dictionaryEntryRepository;

    @Parameter(names = {"--directory"}, required = true, arity = 1, description = "The directory to search for dictionary files")
    private String directory;

    @Parameter(names = {"--extension"}, arity = 1, converter = FileConverter.class, description = "Extension of dictionary files (default: dict)")
    private String extension = "dict";

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new DictionaryImporter(), args);
    }

    @Override
    protected int run() throws Exception {
        User user = getUser();
        FileSystem fs = getFileSystem();

        Path dictionaryPath = new Path(directory);
        FileStatus[] files = fs.listStatus(dictionaryPath, new DictionaryPathFilter(this.extension));
        for (FileStatus fileStatus : files) {
            LOGGER.info("Importing dictionary file: " + fileStatus.getPath().toString());
            String conceptName = FilenameUtils.getBaseName(fileStatus.getPath().toString());
            conceptName = URLDecoder.decode(conceptName, "UTF-8");
            Concept concept = getOntologyRepository().getConceptByIRI(conceptName);
            checkNotNull(concept, "Could not find concept with name " + conceptName);
            writeFile(fs.open(fileStatus.getPath()), conceptName, user);
        }

        return 0;
    }

    protected void writeFile(InputStream in, String concept, User user) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = br.readLine()) != null) {
            dictionaryEntryRepository.saveNew(line, concept, user);
        }

        in.close();
    }

    @Inject
    public void setDictionaryEntryRepository(DictionaryEntryRepository dictionaryEntryRepository) {
        this.dictionaryEntryRepository = dictionaryEntryRepository;
    }

    public static class DictionaryPathFilter implements PathFilter {
        private String extension;

        public DictionaryPathFilter(String extension) {
            this.extension = extension;
        }

        @Override
        public boolean accept(Path path) {
            String ext = FilenameUtils.getExtension(path.toString());
            if (ext == null) {
                return false;
            }
            return ext.equals(this.extension);
        }
    }
}
