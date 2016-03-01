package org.visallo.core.cmdline;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;
import org.visallo.core.cmdline.converters.IRIConverter;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.semanticweb.owlapi.model.IRI;

import java.io.File;

@Parameters(commandDescription = "Import OWL files into the system")
public class OwlImport extends CommandLineTool {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(OwlImport.class, "cli-owlImport");

    @Parameter(names = {"--in", "-i"}, required = true, arity = 1, converter = FileConverter.class, description = "The input OWL file")
    private File inFile;

    @Parameter(names = {"--iri"}, arity = 1, converter = IRIConverter.class, description = "The document IRI (URI used for prefixing concepts)")
    private IRI documentIRI;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new OwlImport(), args);
    }

    @Override
    protected int run() throws Exception {
        if (this.documentIRI == null) {
            String guessedIri = getOntologyRepository().guessDocumentIRIFromPackage(inFile);
            documentIRI = IRI.create(guessedIri);
        }
        getOntologyRepository().importFile(inFile, documentIRI, getAuthorizations());
        getGraph().flush();
        getOntologyRepository().clearCache();
        LOGGER.info("owl import complete");
        return 0;
    }
}
