package org.visallo.core.formula;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.vertexium.Authorizations;
import org.vertexium.Vertex;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiOntology;
import org.visallo.web.clientapi.model.ClientApiVertex;
import org.visallo.web.clientapi.util.ObjectMapperFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.*;

public class FormulaEvaluator {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(FormulaEvaluator.class);
    public static final String CONFIGURATION_PARAMETER_MAX_THREADS = FormulaEvaluator.class.getName() + ".max.threads";
    public static final int CONFIGURATION_DEFAULT_MAX_THREADS = 1;

    private Configuration configuration;
    private OntologyRepository ontologyRepository;

    private ExecutorService executorService;

    private static final ThreadLocal<Map<String, Scriptable>> threadLocalScope = new ThreadLocal<Map<String, Scriptable>>() {
        @Override protected Map<String, Scriptable> initialValue() {
            return new HashMap<>();
        }
    };

    @Inject
    public FormulaEvaluator(Configuration configuration, OntologyRepository ontologyRepository) {
        this.configuration = configuration;
        this.ontologyRepository = ontologyRepository;

        executorService = Executors.newFixedThreadPool(configuration.getInt(CONFIGURATION_PARAMETER_MAX_THREADS, CONFIGURATION_DEFAULT_MAX_THREADS));
    }

    public void close() {
        executorService.shutdown();
    }

    public String evaluateTitleFormula(Vertex vertex, UserContext userContext, Authorizations authorizations) {
        return evaluateFormula("Title", vertex, userContext, authorizations);
    }

    public String evaluateTimeFormula(Vertex vertex, UserContext userContext, Authorizations authorizations) {
        return evaluateFormula("Time", vertex, userContext, authorizations);
    }

    public String evaluateSubtitleFormula(Vertex vertex, UserContext userContext, Authorizations authorizations) {
        return evaluateFormula("Subtitle", vertex, userContext, authorizations);
    }

    private String evaluateFormula(String type, Vertex vertex, UserContext userContext, Authorizations authorizations) {
        FormulaEvaluatorCallable evaluationCallable = new FormulaEvaluatorCallable(type, vertex, userContext, authorizations);

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
            scope = setupContext(getOntologyJson(), getConfigurationJson(userContext.locale), userContext.timeZone);
            scopes.put(mapKey, scope);
        }
        return scope;
    }

    private Scriptable setupContext(String ontologyJson, String configurationJson, String timeZone) {
        Context context = Context.enter();
        context.setLanguageVersion(Context.VERSION_1_6);

        RequireJsSupport browserSupport = new RequireJsSupport();

        ScriptableObject scope = context.initStandardObjects(browserSupport, true);

        try {
            scope.put("ONTOLOGY_JSON", scope, Context.toObject(ontologyJson, scope));
            scope.put("CONFIG_JSON", scope, Context.toObject(configurationJson, scope));
            scope.put("USERS_TIMEZONE", scope, Context.toObject(timeZone, scope));
        } catch (Exception e) {
            throw new VisalloException("Json resource not available", e);
        }

        String[] names = new String[]{"print", "load", "consoleWarn", "consoleError", "readFile"};
        browserSupport.defineFunctionProperties(names, scope.getClass(), ScriptableObject.DONTENUM);

        Scriptable argsObj = context.newArray(scope, new Object[]{});
        scope.defineProperty("arguments", argsObj, ScriptableObject.DONTENUM);

        loadJavaScript(scope);

        scope.sealObject();
        return scope;
    }

    private void loadJavaScript(ScriptableObject scope) {
        evaluateFile(scope, "libs/underscore.js");
        evaluateFile(scope, "libs/r.js");
        evaluateFile(scope, "libs/windowTimers.js");
        evaluateFile(scope, "loader.js");
    }

    protected String getOntologyJson() {
        ClientApiOntology result = ontologyRepository.getClientApiObject();
        try {
            return ObjectMapperFactory.getInstance().writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            throw new VisalloException("Could not evaluate JSON: " + result, ex);
        }
    }

    protected String getConfigurationJson(Locale locale) {
        return configuration.toJSON(locale).toString();
    }

    private Object evaluateFile(ScriptableObject scope, String filename) {
        InputStream is = FormulaEvaluator.class.getResourceAsStream(filename);
        if (is != null) {
            try {
                return Context.getCurrentContext().evaluateString(scope, IOUtils.toString(is), filename, 0, null);
            } catch (IOException e) {
                LOGGER.error("File not readable %s", filename);
            }
        } else LOGGER.error("File not found %s", filename);
        return null;
    }

    protected String toJson(Vertex vertex, String workspaceId, Authorizations authorizations) {
        ClientApiVertex v = ClientApiConverter.toClientApiVertex(vertex, workspaceId, authorizations);
        return v.toString();
    }

    public static class UserContext {
        private final Locale locale;
        private final String timeZone;
        private final String workspaceId;

        public UserContext(Locale locale, String timeZone, String workspaceId) {
            this.locale = locale == null ? Locale.getDefault() : locale;
            this.timeZone = timeZone;
            this.workspaceId = workspaceId;
        }

        public Locale getLocale() {
            return locale;
        }

        public String getTimeZone() {
            return timeZone;
        }

        public String getWorkspaceId() {
            return workspaceId;
        }
    }

    private class FormulaEvaluatorCallable implements Callable<String> {
        private UserContext userContext;
        private String fieldName;
        private Vertex vertex;
        private Authorizations authorizations;

        public FormulaEvaluatorCallable(String fieldName, Vertex vertex, UserContext userContext, Authorizations authorizations) {
            this.fieldName = fieldName;
            this.vertex = vertex;
            this.userContext = userContext;
            this.authorizations = authorizations;
        }

        @Override
        public String call() throws Exception {
            Scriptable scope = getScriptable(userContext);
            String json = toJson(vertex, userContext.getWorkspaceId(), authorizations);
            Function function = (Function) scope.get("evaluate" + fieldName + "FormulaJson", scope);
            Object result = function.call(Context.getCurrentContext(), scope, scope, new Object[]{json});

            return (String) Context.jsToJava(result, String.class);
        }
    }
}
