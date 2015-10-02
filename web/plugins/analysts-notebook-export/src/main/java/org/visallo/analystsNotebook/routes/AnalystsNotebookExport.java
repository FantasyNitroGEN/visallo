package org.visallo.analystsNotebook.routes;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import org.apache.commons.io.IOUtils;
import org.vertexium.Authorizations;
import org.visallo.analystsNotebook.AnalystsNotebookExporter;
import org.visallo.analystsNotebook.AnalystsNotebookVersion;
import org.visallo.analystsNotebook.model.Chart;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.parameterProviders.BaseUrl;
import org.visallo.web.parameterProviders.TimeZone;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnalystsNotebookExport implements ParameterizedHandler {
    private static final String VERSION_PARAMETER_NAME = "version";
    private static final AnalystsNotebookVersion DEFAULT_VERSION = AnalystsNotebookVersion.VERSION_8_9;
    private static final String FILE_EXT = "anx";
    private static final String CONTENT_TYPE = "application/xml";
    private final WorkspaceRepository workspaceRepository;
    private final AnalystsNotebookExporter analystsNotebookExporter;

    @Inject
    public AnalystsNotebookExport(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
        analystsNotebookExporter = InjectHelper.getInstance(AnalystsNotebookExporter.class);
    }

    @Handle
    public void handle(
            User user,
            Authorizations authorizations,
            @ActiveWorkspaceId String workspaceId,
            @Optional(name = VERSION_PARAMETER_NAME) String versionParameter,
            Locale locale,
            @TimeZone String timeZone,
            @BaseUrl String baseUrl,
            VisalloResponse response
    ) throws Exception {
        Workspace workspace = workspaceRepository.findById(workspaceId, user);

        AnalystsNotebookVersion version = DEFAULT_VERSION;
        if (versionParameter != null) {
            version = AnalystsNotebookVersion.valueOf(versionParameter);
        }

        Chart chart = analystsNotebookExporter.toChart(version, workspace, user, authorizations, locale, timeZone, baseUrl);

        List<String> comments = new ArrayList<String>();
        comments.add(String.format("Visallo Workspace: %s", workspace.getDisplayTitle()));
        comments.add(String.format("%s/#w=%s", baseUrl, workspaceId));
        comments.add(String.format("Exported %1$tF %1$tT %1$tz for Analyst's Notebook version %2$s", new Date(), version.toString()));

        String xml = chart.toXml(comments);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        response.setContentType(CONTENT_TYPE);
        response.setMaxAge(VisalloResponse.EXPIRES_1_HOUR);
        String filename = workspace.getDisplayTitle().replaceAll("[^A-Za-z0-9]", "_") + "_" + simpleDateFormat.format(new Date()) + "." + FILE_EXT;
        response.addHeader("Content-Disposition", "attachment; filename=" + filename.replaceAll("_{2,}", "_"));

        try (InputStream in = new ByteArrayInputStream(xml.getBytes())) {
            IOUtils.copy(in, response.getOutputStream());
        }
    }
}
