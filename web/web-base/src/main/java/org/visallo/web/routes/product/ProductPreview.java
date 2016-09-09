package org.visallo.web.routes.product;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.model.workspace.product.Product;
import org.visallo.core.user.User;
import org.visallo.web.VisalloResponse;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

public class ProductPreview implements ParameterizedHandler {
    private final WorkspaceRepository workspaceRepository;

    @Inject
    public ProductPreview(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }


    @Handle
    public void handle(
            @Required(name = "productId") String productId,
            @ActiveWorkspaceId String workspaceId,
            User user,
            VisalloResponse response
    ) throws Exception {
        Product product = workspaceRepository.findProductById(workspaceId, productId, null, false, user);
        InputStream preview = product.getPreviewDataUrl();
        if (preview == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            try {
                response.write(preview);
            } finally {
                preview.close();
            }
        }
    }
}
