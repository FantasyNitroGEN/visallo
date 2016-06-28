package org.visallo.web.plugin.adminImportRdf;

import com.google.common.io.Files;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import net.lingala.zip4j.core.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.vertexium.Authorizations;
import org.vertexium.util.FilterIterable;
import org.visallo.common.rdf.RdfImportHelper;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.user.User;
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
import java.io.OutputStream;
import java.util.List;
import java.util.TimeZone;

import static org.vertexium.util.IterableUtils.toList;

public class ImportRdf implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ImportRdf.class);
    private final RdfImportHelper rdfImportHelper;

    @Inject
    public ImportRdf(RdfImportHelper rdfImportHelper) {
        this.rdfImportHelper = rdfImportHelper;
    }

    @Handle
    public ClientApiSuccess handle(
            HttpServletRequest request,
            @Optional(name = "priority", defaultValue = "NORMAL") String priorityString,
            @Optional(name = "timeZone", defaultValue = "GMT") String timeZoneId,
            @Optional(name = "visibility", defaultValue = "") String visibilitySource,
            User user,
            Authorizations authorizations
    ) throws IOException, ServletException {
        final Priority priority = Priority.safeParse(priorityString);

        List<Part> parts = toList(getFiles(request));
        if (parts.size() != 1) {
            throw new RuntimeException("Wrong number of uploaded files. Expected 1 got " + parts.size());
        }
        Part part = parts.get(0);

        File tempDirectory = savePartToTemp(part);
        try {
            TimeZone timeZone = TimeZone.getTimeZone(timeZoneId);
            rdfImportHelper.importRdf(tempDirectory, timeZone, priority, visibilitySource, user, authorizations);
            return VisalloResponse.SUCCESS;
        } finally {
            try {
                FileUtils.forceDelete(tempDirectory);
            } catch (Exception ex) {
                LOGGER.error("Could not delete temp directory (%s) after failed save", tempDirectory.getAbsolutePath(), ex);
            }
        }
    }

    private File savePartToTemp(Part part) {
        File tempDir = Files.createTempDir();
        try {
            File uploadFile = new File(tempDir, part.getSubmittedFileName());
            try (OutputStream uploadFileOut = new FileOutputStream(uploadFile)) {
                IOUtils.copy(part.getInputStream(), uploadFileOut);
            }

            try {
                ZipFile zipped = new ZipFile(uploadFile);
                if (zipped.isValidZipFile()) {
                    zipped.extractAll(tempDir.getAbsolutePath());
                }
            } catch (Exception ex) {
                throw new VisalloException("Could not expand zip file: " + uploadFile.getAbsolutePath(), ex);
            }

            return tempDir;
        } catch (Exception ex) {
            try {
                FileUtils.forceDelete(tempDir);
            } catch (IOException e) {
                LOGGER.error("Could not delete temp directory (%s) after failed save", tempDir.getAbsolutePath(), e);
            }
            throw new VisalloException("Could not save uploaded file", ex);
        }
    }

    private Iterable<Part> getFiles(HttpServletRequest request) throws IOException, ServletException {
        return new FilterIterable<Part>(request.getParts()) {
            @Override
            protected boolean isIncluded(Part part) {
                return part.getName().equals("file");
            }
        };
    }
}
