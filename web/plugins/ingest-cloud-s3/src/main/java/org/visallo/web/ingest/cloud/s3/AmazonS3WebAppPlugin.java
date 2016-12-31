package org.visallo.web.ingest.cloud.s3;

import com.v5analytics.webster.Handler;
import org.visallo.web.VisalloCsrfHandler;
import org.visallo.web.WebApp;
import org.visallo.web.WebAppPlugin;
import org.visallo.web.ingest.cloud.s3.routes.S3DirectoryListing;
import org.visallo.web.privilegeFilters.ReadPrivilegeFilter;

import javax.servlet.ServletContext;

public class AmazonS3WebAppPlugin implements WebAppPlugin {

    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        Class<? extends Handler> authenticator = authenticationHandler.getClass();
        Class<? extends Handler> csrfProtector = VisalloCsrfHandler.class;

        app.registerJavaScript("/org/visallo/web/ingest/cloud/s3/js/plugin.js", true);
        app.registerCompiledJavaScript("/org/visallo/web/ingest/cloud/s3/dist/Config.js");
        app.registerCompiledJavaScript("/org/visallo/web/ingest/cloud/s3/dist/BasicAuth.js");
        app.registerCompiledJavaScript("/org/visallo/web/ingest/cloud/s3/dist/SessionAuth.js");
        app.registerCompiledJavaScript("/org/visallo/web/ingest/cloud/s3/dist/actions-impl.js");

        app.registerCompiledWebWorkerJavaScript("/org/visallo/web/ingest/cloud/s3/dist/plugin-worker.js");

        app.registerLess("/org/visallo/web/ingest/cloud/s3/style.less");
        app.registerResourceBundle("/org/visallo/web/ingest/cloud/s3/messages.properties");

        app.post("/org/visallo/web/ingest/cloud/s3", authenticator, csrfProtector, ReadPrivilegeFilter.class, S3DirectoryListing.class);
    }
}
