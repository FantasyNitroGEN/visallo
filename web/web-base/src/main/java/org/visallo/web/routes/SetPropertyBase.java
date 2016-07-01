package org.visallo.web.routes;

import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.BadRequestException;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.TimeZone;

public abstract class SetPropertyBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(SetPropertyBase.class);

    protected final Graph graph;
    protected final VisibilityTranslator visibilityTranslator;

    protected SetPropertyBase(Graph graph, VisibilityTranslator visibilityTranslator) {
        this.graph = graph;
        this.visibilityTranslator = visibilityTranslator;
    }

    protected boolean isCommentProperty(String propertyName) {
        return VisalloProperties.COMMENT.isSameName(propertyName);
    }

    protected String createPropertyKey(String propertyName, Graph graph) {
        return isCommentProperty(propertyName) ? createCommentPropertyKey() : graph.getIdGenerator().nextId();
    }

    protected void checkVisibilityParameter(
            String visibilitySource, Authorizations authorizations, User user, ResourceBundle resourceBundle) {
        if (!graph.isVisibilityValid(visibilityTranslator.toVisibility(visibilitySource).getVisibility(), authorizations)) {
            LOGGER.warn("%s is not a valid visibility for %s user", visibilitySource, user.getDisplayName());
            throw new BadRequestException("visibilitySource", resourceBundle.getString("visibility.invalid"));
        }
    }

    protected void checkRoutePath(String entityType, String propertyName, HttpServletRequest request) {
        boolean isComment = isCommentProperty(propertyName);
        if (isComment && request.getPathInfo().equals(String.format("/%s/property", entityType))) {
            throw new VisalloException(String.format("Use /%s/comment to save comment properties", entityType));
        } else if (!isComment && request.getPathInfo().equals(String.format("/%s/comment", entityType))) {
            throw new VisalloException(String.format("Use /%s/property to save non-comment properties", entityType));
        }
    }

    private static String createCommentPropertyKey() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(new Date());
    }
}
