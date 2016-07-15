package org.visallo.web.util.js;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

public class BabelExecutorTest {

    private static BabelExecutor executor;

    @BeforeClass
    public static void before() {
        executor = new BabelExecutor();
    }

    @Test
    public void testShouldCompileJsx() throws Exception {
        testFixture("basic");
        testFixture("objectspread");
    }

    @Test
    public void testShouldCreateSourceMaps() throws Exception {
        assertEquals(null, testFixture("sourcemapinline", SourceMapType.INLINE).getSourceMap());
        assertEquals(
            getResource("sourcemapext-expected.js.map"),
            testFixture("sourcemapext", SourceMapType.EXTERNAL).getSourceMap()
        );
    }

    private void testFixture(String prefix) throws Exception {
        testFixture(prefix, SourceMapType.NONE);
    }

    private CachedCompilation testFixture(String prefix, SourceMapType mapType) throws Exception {
        Future<CachedCompilation> compilationTask = executor.submit(() -> compileFixtureForPrefix(prefix, mapType));
        CachedCompilation out = compilationTask.get();
        assertEquals(expectedForPrefix(prefix), out.getOutput());
        return out;
    }

    private String expectedForPrefix(String prefix) {
        return getResource(prefix + "-expected.js");
    }

    private CachedCompilation compileFixtureForPrefix(String prefix, SourceMapType sourceMap) {
        String resourcePath = prefix + "-source.jsx";
        CachedCompilation c = new CachedCompilation();
        c.setPath("/path");
        c.setInput(getResource(resourcePath));
        c.setResourcePath(resourcePath);
        try {
            executor.compileWithSharedEngine(c, sourceMap);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
        return c;
    }

    private String getResource(String resourcePath) {
        try (InputStream input = getClass().getResourceAsStream(resourcePath)) {
            return IOUtils.toString(input, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
