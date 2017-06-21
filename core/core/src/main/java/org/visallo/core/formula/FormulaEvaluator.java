package org.visallo.core.formula;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.mozilla.javascript.*;
import org.vertexium.Authorizations;
import org.vertexium.VertexiumObject;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiElement;
import org.visallo.web.clientapi.model.ClientApiOntology;
import org.visallo.web.clientapi.model.ClientApiVertexiumObject;
import org.visallo.web.clientapi.util.ObjectMapperFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Evaluates JavaScript formulas (title, subtitle, etc) using Java's Rhino JavaScript interpreter.
 */
public class FormulaEvaluator {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(FormulaEvaluator.class);
    public static final String CONFIGURATION_PARAMETER_MAX_THREADS = FormulaEvaluator.class.getName() + ".max.threads";
    public static final int CONFIGURATION_DEFAULT_MAX_THREADS = 1;
    private Configuration configuration;
    private OntologyRepository ontologyRepository;
    private ExecutorService executorService;

    private static final ThreadLocal<Map<String, Scriptable>> threadLocalScope = new ThreadLocal<Map<String, Scriptable>>() {
        @Override
        protected Map<String, Scriptable> initialValue() {
            return new HashMap<>();
        }
    };

    @Inject
    public FormulaEvaluator(Configuration configuration, OntologyRepository ontologyRepository) {
        this.configuration = configuration;
        this.ontologyRepository = ontologyRepository;

        executorService = Executors.newFixedThreadPool(configuration.getInt(
                CONFIGURATION_PARAMETER_MAX_THREADS,
                CONFIGURATION_DEFAULT_MAX_THREADS
        ));
    }

    public void close() {
        executorService.shutdown();
    }

    public String evaluateTitleFormula(VertexiumObject vertexiumObject, UserContext userContext, Authorizations authorizations) {
        return evaluateFormula("Title", vertexiumObject, null, null, userContext, authorizations);
    }

    public String evaluateTimeFormula(VertexiumObject vertexiumObject, UserContext userContext, Authorizations authorizations) {
        return evaluateFormula("Time", vertexiumObject, null, null, userContext, authorizations);
    }

    public String evaluateSubtitleFormula(VertexiumObject vertexiumObject, UserContext userContext, Authorizations authorizations) {
        return evaluateFormula("Subtitle", vertexiumObject, null, null, userContext, authorizations);
    }

    public String evaluatePropertyDisplayFormula(
            VertexiumObject vertexiumObject,
            String propertyKey,
            String propertyName,
            UserContext userContext,
            Authorizations authorizations
    ) {
        return evaluateFormula("Property", vertexiumObject, propertyKey, propertyName, userContext, authorizations);
    }

    private String evaluateFormula(
            String type,
            VertexiumObject vertexiumObject,
            String propertyKey,
            String propertyName,
            UserContext userContext,
            Authorizations authorizations
    ) {
        FormulaEvaluatorCallable evaluationCallable = new FormulaEvaluatorCallable(
                type,
                vertexiumObject,
                propertyKey,
                propertyName,
                userContext,
                authorizations
        );

        try {
            return executorService.submit(evaluationCallable).get();
        } catch (InterruptedException e) {
            LOGGER.error(type + " evaluation interrupted", e);
        } catch (ExecutionException e) {
            LOGGER.error("Error encountered during " + type + " evaluation", e);
        }

        return "Unable to Evaluate " + type;
    }

    public Scriptable getScriptable(UserContext userContext) {
        Map<String, Scriptable> scopes = threadLocalScope.get();

        String mapKey = userContext.locale.toString() + userContext.timeZone;
        Scriptable scope = scopes.get(mapKey);
        if (scope == null) {
            scope = setupContext(getOntologyJson(userContext.getWorkspaceId()), getConfigurationJson(userContext.locale, userContext.getWorkspaceId()), userContext.timeZone);
            scopes.put(mapKey, scope);
        } else {
            scope.put("ONTOLOGY_JSON", scope, Context.toObject(getOntologyJson(userContext.getWorkspaceId()), scope));
        }
        return scope;
    }

    private Scriptable setupContext(String ontologyJson, String configurationJson, String timeZone) {
        Context context = Context.enter();
        context.setLanguageVersion(Context.VERSION_1_8);
        context.setOptimizationLevel(-1);

        RequireJsSupport browserSupport = new RequireJsSupport();

        ScriptableObject scope = context.initStandardObjects(browserSupport, true);

        try {
            scope.put("ONTOLOGY_JSON", scope, Context.toObject(ontologyJson, scope));
            scope.put("CONFIG_JSON", scope, Context.toObject(configurationJson, scope));
            scope.put("USERS_TIMEZONE", scope, Context.toObject(timeZone, scope));
        } catch (Exception e) {
            throw new VisalloException("Json resource not available", e);
        }

        String[] names = new String[]{"print", "load", "consoleWarn", "consoleError", "readFully"};
        browserSupport.defineFunctionProperties(names, scope.getClass(), ScriptableObject.DONTENUM);

        Scriptable argsObj = context.newArray(scope, new Object[]{});
        scope.defineProperty("arguments", argsObj, ScriptableObject.DONTENUM);

        loadJavaScript(scope);

        return scope;
    }

    private void loadJavaScript(ScriptableObject scope) {
        evaluateFile(scope, "../libs/underscore.js");
        evaluateFile(scope, "../libs/r.js");
        evaluateFile(scope, "../libs/windowTimers.js");
        evaluateFile(scope, "loader.js");
    }

    protected String getOntologyJson(String workspaceId) {
        // FIXME: user?? workspace?
        ClientApiOntology result = ontologyRepository.getClientApiObject();
        try {
            return ObjectMapperFactory.getInstance().writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            throw new VisalloException("Could not evaluate JSON: " + result, ex);
        }
    }

    protected String getConfigurationJson(Locale locale, String workspaceId) {
        return configuration.toJSON(locale, workspaceId).toString();
    }

    private void evaluateFile(ScriptableObject scope, String filename) {
        LOGGER.debug("evaluating file: %s", filename);
        try (InputStream is = FormulaEvaluator.class.getResourceAsStream("jsc/" + filename)) {
            if (is == null) {
                throw new VisalloException("File not found " + filename);
            }

            Context.getCurrentContext().evaluateString(scope, IOUtils.toString(is), filename, 0, null);
        } catch (JavaScriptException ex) {
            throw new VisalloException("JavaScript error in " + filename, ex);
        } catch (IOException ex) {
            throw new VisalloException("Could not read file: " + filename, ex);
        }
    }

    protected String toJson(VertexiumObject vertexiumObject, String workspaceId, Authorizations authorizations) {
        ClientApiVertexiumObject v = ClientApiConverter.toClientApi(vertexiumObject, workspaceId, authorizations);
        return v.toString();
    }

    public static class UserContext {
        private final Locale locale;
        private final String timeZone;
        private final String workspaceId;
        private final ResourceBundle resourceBundle;

        public UserContext(Locale locale, ResourceBundle resourceBundle, String timeZone, String workspaceId) {
            this.locale = locale == null ? Locale.getDefault() : locale;
            this.resourceBundle = resourceBundle;
            this.timeZone = timeZone;
            this.workspaceId = workspaceId;
        }

        public Locale getLocale() {
            return locale;
        }

        public ResourceBundle getResourceBundle() {
            return resourceBundle;
        }

        public String getTimeZone() {
            return timeZone;
        }

        public String getWorkspaceId() {
            return workspaceId;
        }
    }

    private class FormulaEvaluatorCallable implements Callable<String> {
        private final String propertyKey;
        private final String propertyName;
        private UserContext userContext;
        private String fieldName;
        private VertexiumObject vertexiumObject;
        private Authorizations authorizations;

        public FormulaEvaluatorCallable(
                String fieldName,
                VertexiumObject vertexiumObject,
                String propertyKey,
                String propertyName,
                UserContext userContext,
                Authorizations authorizations
        ) {
            this.fieldName = fieldName;
            this.vertexiumObject = vertexiumObject;
            this.propertyKey = propertyKey;
            this.propertyName = propertyName;
            this.userContext = userContext;
            this.authorizations = authorizations;
        }

        @Override
        public String call() throws Exception {
            Scriptable scope = getScriptable(userContext);
            Context context = Context.getCurrentContext();
            String json = toJson(vertexiumObject, userContext.getWorkspaceId(), authorizations);
            Object func = scope.get("evaluate" + fieldName + "FormulaJson", scope);

            if (func.equals(scope.NOT_FOUND)) {
                throw new VisalloException("formula function not found");
            }

            if (func instanceof Function) {
                Function function = (Function) func;
                Object result = function.call(
                        context,
                        scope,
                        scope,
                        new Object[]{json, propertyKey, propertyName}
                );

                String strResult = (String) context.jsToJava(result, String.class);
                return strResult;
            }

            throw new VisalloException("Unknown result from formula");
        }
    }
}
