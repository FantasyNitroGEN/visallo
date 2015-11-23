package org.visallo.zipCodeBoundaries;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.vertexium.type.GeoPoint;
import org.vertexium.type.GeoRect;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.file.FileSystemRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ZipCodeBoundariesRepositoryTest {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ZipCodeBoundariesRepositoryTest.class);
    private ZipCodeBoundariesRepository zipCodeBoundariesRepository;

    @Mock
    private Configuration configuration;

    @Mock
    private FileSystemRepository fileSystemRepository;

    @Before
    public void setUp() throws IOException {
        when(fileSystemRepository.getLocalFileFor(any(String.class))).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                File tempDir = new File(System.getProperty("java.io.tmpdir"));
                File dir = new File(tempDir, "cb_2014_us_zcta510_500k");
                File file = new File(dir, "cb_2014_us_zcta510_500k.shp");
                if (!file.exists()) {
                    File zipFile = new File(tempDir, "cb_2014_us_zcta510_500k.zip");
                    downloadFile(new URL("http://www2.census.gov/geo/tiger/GENZ2014/shp/cb_2014_us_zcta510_500k.zip"), zipFile);
                    unzip(zipFile, dir);
                }
                return file;
            }
        });

        zipCodeBoundariesRepository = new ZipCodeBoundariesRepository(configuration, fileSystemRepository);
    }

    private void unzip(File zipFile, File dir) throws IOException {
        try {
            LOGGER.info("Unzipping: %s", zipFile.getAbsolutePath());
            ZipFile zf = new ZipFile(zipFile.getAbsoluteFile());
            dir.mkdirs();
            zf.extractAll(dir.getAbsolutePath());
        } catch (ZipException e) {
            throw new IOException("Could not unzip file", e);
        }
    }

    private void downloadFile(URL url, File outputFile) throws IOException {
        LOGGER.info("Downloading: %s to %s", url.toString(), outputFile.getAbsolutePath());
        URLConnection urlConn = url.openConnection();
        try (InputStream in = urlConn.getInputStream()) {
            FileUtils.copyInputStreamToFile(in, outputFile);
        }
    }

    @Test
    public void testFind() throws Exception {
        GeoPoint northWest = new GeoPoint(39, -78);
        GeoPoint southEast = new GeoPoint(38.5, -77);
        List<Features.Feature> features = zipCodeBoundariesRepository.find(new GeoRect(northWest, southEast));
        assertEquals(163, features.size());
    }

    @Test
    public void testFindZipCode() {
        List<Features.Feature> features = zipCodeBoundariesRepository.findZipCodes(new String[]{"20147"});
        assertEquals(1, features.size());
        assertEquals(2, features.get(0).getCoordinates().size());
    }
}