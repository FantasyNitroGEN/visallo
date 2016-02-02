package org.visallo.web;

import com.asual.lesscss.LessEngine;
import com.asual.lesscss.LessOptions;
import com.v5analytics.webster.Handler;
import com.v5analytics.webster.HandlerChain;
import com.v5analytics.webster.RequestResponseHandler;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;


public class LessResourceHandler implements RequestResponseHandler {
    private static LessEngine lessCompiler;

    private String lessResourceName;

    public LessResourceHandler(String lessResourceName) {
          this.lessResourceName = lessResourceName;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        response.setContentType("text/css");

        try (InputStream input = this.getClass().getResourceAsStream(lessResourceName)) {
            try (StringWriter writer = new StringWriter()) {
                IOUtils.copy(input, writer, StandardCharsets.UTF_8);
                String inputLess = writer.toString();
                String output = lessCompiler().compile(inputLess);

                try (PrintWriter outWriter = response.getWriter()) {
                    outWriter.println(output);
                }
            }
        }
    }

    private synchronized LessEngine lessCompiler() {
        if (lessCompiler == null) {
            lessCompiler = new LessEngine();
            LessOptions options = new LessOptions();
            options.setCompress(true);
            options.setCharset("UTF-8");
            lessCompiler = new LessEngine(options);
        }
        return lessCompiler;
    }
}
