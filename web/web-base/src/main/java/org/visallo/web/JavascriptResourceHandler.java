package org.visallo.web;

import com.google.javascript.jscomp.*;
import com.google.javascript.jscomp.Compiler;
import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.RequestResponseHandler;
import org.apache.commons.io.IOUtils;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;


public class JavascriptResourceHandler implements RequestResponseHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(JavascriptResourceHandler.class);
    private static final ExecutorService compilationExecutor = Executors.newFixedThreadPool(5);

    private String jsResourceName;
    private String compiledJavascript;
    private Long compiledLastModified;
    private FutureTask<String> compilationTask;

    public JavascriptResourceHandler(final String jsResourceName) {
        this.jsResourceName = jsResourceName;

        compilationTask = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return compileIfNecessary();
            }
        });

        compilationExecutor.execute(compilationTask);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        response.setContentType("application/javascript");

        if (compilationTask.isDone()) {
            compileIfNecessary();
        } else {
            compilationTask.get();
        }

        write(response.getWriter(), compiledJavascript);
    }

    private void write(PrintWriter writer, String output) {
        if (output != null) {
            try (PrintWriter outWriter = writer) {
                outWriter.println(output);
            }
        } else {
            throw new VisalloException("Unable to compile " + jsResourceName);
        }
    }

    private long getLastModified() throws IOException {
        URL url = this.getClass().getResource(jsResourceName);
        return url.openConnection().getLastModified();
    }

    private String compileIfNecessary() throws IOException {
        long lastModified = getLastModified();
        if (compiledLastModified == null || compiledLastModified != lastModified || compiledJavascript == null) {
            compiledLastModified = lastModified;
            try (InputStream input = this.getClass().getResourceAsStream(jsResourceName)) {
                try (StringWriter writer = new StringWriter()) {
                    IOUtils.copy(input, writer, StandardCharsets.UTF_8);
                    String inputJavascript = writer.toString();
                    compiledJavascript = runClosureCompilation(inputJavascript);
                }
            }
        }

        return compiledJavascript;
    }

    private String runClosureCompilation(String inputJavascript) throws IOException {
        Compiler.setLoggingLevel(Level.INFO);
        Compiler compiler = new Compiler(new JavascriptResourceHandlerErrorManager());

        CompilerOptions compilerOptions = new CompilerOptions();
        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(compilerOptions);
        WarningLevel.VERBOSE.setOptionsForWarningLevel(compilerOptions);
        compilerOptions.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT6);
        compilerOptions.setLanguageOut(CompilerOptions.LanguageMode.ECMASCRIPT5);
        compilerOptions.setEnvironment(CompilerOptions.Environment.BROWSER);

        List<SourceFile> inputs = new ArrayList<SourceFile>();
        inputs.add(SourceFile.fromCode(jsResourceName, inputJavascript));

        List<SourceFile> externs = AbstractCommandLineRunner.getBuiltinExterns(compilerOptions);
        externs.add(SourceFile.fromInputStream("visallo-externs.js", this.getClass().getResourceAsStream("visallo-externs.js"), Charset.forName("UTF-8")));
        Result result = compiler.compile(externs, inputs, compilerOptions);
        if (result.success) {
            return compiler.toSource();
        }
        return null;
    }
}

class JavascriptResourceHandlerErrorManager extends BasicErrorManager {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(JavascriptResourceHandlerErrorManager.class);

    @Override
    public void println(CheckLevel checkLevel, JSError jsError) {
        if (checkLevel.equals(CheckLevel.ERROR)) {
            LOGGER.error("%s:%s %s", jsError.sourceName, jsError.getLineNumber(), jsError.description);
        }
    }

    @Override
    protected void printSummary() {
        if (this.getErrorCount() > 0) {
            LOGGER.error("%d error(s)", this.getErrorCount());
        }
    }
}