package org.visallo.web.clientapi;

import org.visallo.web.clientapi.codegen.AdminApi;
import org.visallo.web.clientapi.codegen.ApiException;
import org.visallo.web.clientapi.util.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AdminApiExt extends AdminApi {
    public void uploadOntology(InputStream file) throws ApiException, IOException {
        // TODO has to be a better way than writing to a local file.
        File f = File.createTempFile("uploadOntology", ".xml");
        try {
            FileOutputStream out = new FileOutputStream(f);
            try {
                IOUtils.copy(file, out);
            } finally {
                out.close();
            }
            uploadOntology(f);
        } finally {
            f.delete();
        }
    }
}
