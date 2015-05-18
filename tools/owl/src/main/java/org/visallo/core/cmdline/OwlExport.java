package org.visallo.core.cmdline;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;
import org.visallo.core.cmdline.converters.IRIConverter;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.semanticweb.owlapi.model.IRI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

@Parameters(commandDescription = "Export OWL files from the system")
public class OwlExport extends CommandLineTool {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(OwlExport.class, "cli-owlExport");

    @Parameter(names = {"--out", "-o"}, arity = 1, converter = FileConverter.class, description = "The output OWL file")
    private File outFile;

    @Parameter(names = {"--iri"}, required = true, arity = 1, converter = IRIConverter.class, description = "The document IRI (URI used for prefixing concepts)")
    private IRI documentIRI;

    public static void main(String[] args) throws Exception {
        CommandLineTool.main(new OwlExport(), args);
    }

    @Override
    protected int run() throws Exception {
        OutputStream out;
        if (outFile != null) {
            out = new FileOutputStream(outFile);
        } else {
            out = System.out;
        }
        try {
            getOntologyRepository().exportOntology(out, this.documentIRI);
            LOGGER.info("owl export complete");
            return 0;
        } finally {
            out.close();
        }
    }
}
