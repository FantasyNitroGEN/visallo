package org.visallo.web.routes.admin;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import org.apache.commons.io.IOUtils;
import org.semanticweb.owlapi.model.IRI;
import org.vertexium.Authorizations;
import org.vertexium.util.FilterIterable;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.VisalloResponse;
import org.visallo.web.clientapi.model.ClientApiSuccess;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.vertexium.util.IterableUtils.toList;

public class AdminUploadOntology implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(AdminUploadOntology.class);
    private final OntologyRepository ontologyRepository;

    @Inject
    public AdminUploadOntology(final OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    @Handle
    public ClientApiSuccess handle(
            Authorizations authorizations,
            @Optional(name = "documentIRI") String documentIRIString,
            HttpServletRequest request
        ) throws Exception {

        List<Part> files = toList(getFiles(request));
        if (files.size() != 1) {
            throw new RuntimeException("Wrong number of uploaded files. Expected 1 got " + files.size());
        }
        Part file = files.get(0);

        File tempFile = File.createTempFile("ontologyUpload", ".bin");
        writeToTempFile(file, tempFile);

        if (documentIRIString == null || documentIRIString.length() == 0) {
            documentIRIString = ontologyRepository.guessDocumentIRIFromPackage(tempFile);
        }

        IRI documentIRI = IRI.create(documentIRIString);

        LOGGER.info("adding ontology: %s", documentIRI.toString());
        ontologyRepository.writePackage(tempFile, documentIRI, authorizations);
        ontologyRepository.clearCache();

        tempFile.delete();

        return VisalloResponse.SUCCESS;
    }

    private Iterable<Part> getFiles(HttpServletRequest request) throws IOException, ServletException {
        return new FilterIterable<Part>(request.getParts()) {
            @Override
            protected boolean isIncluded(Part part) {
                return part.getName().equals("file");
            }
        };
    }

    private void writeToTempFile(Part file, File tempFile) throws IOException {
        InputStream in = file.getInputStream();
        try {
            FileOutputStream out = new FileOutputStream(tempFile);
            try {
                IOUtils.copy(in, out);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }
}
