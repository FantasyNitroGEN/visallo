package org.visallo.opennlpDictionary;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;
import com.google.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.model.file.FileSystemRepository;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.user.User;
import org.visallo.opennlpDictionary.model.DictionaryEntryRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;

import static com.google.common.base.Preconditions.checkNotNull;

@Parameters(commandDescription = "Import dictionary files used by OpenNLPDictionaryExtractorGraphPropertyWorker")
public class DictionaryImporter extends CommandLineTool {
    private DictionaryEntryRepository dictionaryEntryRepository;
    private FileSystemRepository fileSystemRepository;

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

        for (String fileName : fileSystemRepository.list(directory)) {
            if (!fileName.endsWith(extension)) {
                continue;
            }
            LOGGER.info("Importing dictionary file: " + fileName);
            String conceptName = FilenameUtils.getBaseName(fileName);
            conceptName = URLDecoder.decode(conceptName, "UTF-8");
            Concept concept = getOntologyRepository().getConceptByIRI(conceptName);
            checkNotNull(concept, "Could not find concept with name " + conceptName);
            writeFile(fileSystemRepository.getInputStream(fileName), conceptName, user);
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

    @Inject
    public void setFileSystemRepository(FileSystemRepository fileSystemRepository) {
        this.fileSystemRepository = fileSystemRepository;
    }
}
