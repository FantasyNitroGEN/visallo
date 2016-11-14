package org.visallo.web.routes.product;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.workspace.product.Product;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.model.workspace.product.WorkProduct;
import org.visallo.core.user.User;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.model.ClientApiProducts;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ProductAll implements ParameterizedHandler {
    private final WorkspaceRepository workspaceRepository;
    private final Configuration configuration;

    @Inject
    public ProductAll(
            WorkspaceRepository workspaceRepository,
            Configuration configuration
    ) {
        this.workspaceRepository = workspaceRepository;
        this.configuration = configuration;
    }

    @Handle
    public ClientApiProducts handle(
            @ActiveWorkspaceId String workspaceId,
            User user
    ) throws Exception {
        Collection<Product> products = workspaceRepository.findAllProductsForWorkspace(workspaceId, user);
        if (products == null) {
            throw new VisalloResourceNotFoundException("Could not find products for workspace " + workspaceId);
        }

        List<String> types = InjectHelper.getInjectedServices(WorkProduct.class, configuration).stream().map(p -> p.getClass().getName()).collect(Collectors.toList());

        return ClientApiConverter.toClientApiProducts(types, products);
    }
}
