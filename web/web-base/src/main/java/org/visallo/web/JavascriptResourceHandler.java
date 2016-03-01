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
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;


public class JavascriptResourceHandler implements RequestResponseHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(JavascriptResourceHandler.class);
    private static final List<SourceFile> externs = new ArrayList<SourceFile>();

    private String jsResourceName;

    public JavascriptResourceHandler(String jsResourceName) {
          this.jsResourceName = jsResourceName;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        response.setContentType("application/javascript");

        try (InputStream input = this.getClass().getResourceAsStream(jsResourceName)) {
            try (StringWriter writer = new StringWriter()) {
                IOUtils.copy(input, writer, StandardCharsets.UTF_8);
                String inputJavascript = writer.toString();

                String output = compile(inputJavascript);
                if (output != null) {
                    try (PrintWriter outWriter = response.getWriter()) {
                        outWriter.println(output);
                    }
                } else {
                    throw new VisalloException("Unable to compile " + jsResourceName);
                }
            }
        }
    }

    private String compile(String input) {
        Compiler.setLoggingLevel(Level.INFO);
        Compiler compiler = new Compiler(new JavascriptResourceHandlerErrorManager());

        CompilerOptions compilerOptions = new CompilerOptions();
        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(compilerOptions);
        WarningLevel.DEFAULT.setOptionsForWarningLevel(compilerOptions);

        List<SourceFile> inputs = new ArrayList<SourceFile>();
        inputs.add(SourceFile.fromCode(jsResourceName, input));

        Result result = compiler.compile(externs, inputs, compilerOptions);
        if (result.success) {
            return compiler.toSource();
        }
        return null;
    }
}

class JavascriptResourceHandlerErrorManager implements ErrorManager {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(JavascriptResourceHandlerErrorManager.class);

    @Override
    public void report(CheckLevel checkLevel, JSError jsError) {
        if (checkLevel.equals(CheckLevel.ERROR)) {
            LOGGER.error("%s:%s %s", jsError.sourceName, jsError.getLineNumber(), jsError.description);
        }
    }

    @Override
    public void generateReport() {

    }

    @Override
    public int getErrorCount() {
        return 0;
    }

    @Override
    public int getWarningCount() {
        return 0;
    }

    @Override
    public JSError[] getErrors() {
        return new JSError[0];
    }

    @Override
    public JSError[] getWarnings() {
        return new JSError[0];
    }

    @Override
    public void setTypedPercent(double v) {

    }

    @Override
    public double getTypedPercent() {
        return 0;
    }
}