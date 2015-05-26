package org.visallo.geoip;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Metadata;
import org.vertexium.Vertex;
import org.vertexium.Visibility;
import org.vertexium.type.GeoPoint;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.test.GraphPropertyWorkerTestBase;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.vertexium.util.IterableUtils.count;

@RunWith(MockitoJUnitRunner.class)
public class GeoIpGraphPropertyWorkerTest extends GraphPropertyWorkerTestBase {
    public static final String TEST_GEO_LOCATION_PROPERTY_IRI = "http://visallo.org/test#geoLocation";
    private Visibility visibility = new Visibility("");
    private GeoIpGraphPropertyWorker gpw;

    @Mock
    private GeoIpRepository geoIpRepository;

    @Mock
    private OntologyRepository ontologyRepository;

    @Mock
    private FileSystem fileSystem;

    @Before
    public void setUp() throws Exception {
        when(ontologyRepository.getRequiredPropertyIRIByIntent(eq(GeoIpGraphPropertyWorker.GEO_LOCATION_INTENT))).thenReturn(TEST_GEO_LOCATION_PROPERTY_IRI);
        when(fileSystem.exists(eq(new Path("file:///test/GeoLite2-City-Blocks-IPv4.csv")))).thenReturn(true);
        when(fileSystem.exists(eq(new Path("file:///test/GeoLite2-City-Locations-en.csv")))).thenReturn(true);

        gpw = new GeoIpGraphPropertyWorker();
        gpw.setGeoIpRepository(geoIpRepository);
        gpw.setOntologyRepository(ontologyRepository);
    }

    @Test
    public void testNonIpAddressProperty() throws Exception {
        Metadata metadata = new Metadata();
        Vertex v1 = getGraph().prepareVertex("v1", visibility)
                .addPropertyValue("k1", "p1", "a.b.c.d", metadata, visibility)
                .save(getGraphAuthorizations());
        getGraph().flush();

        run(gpw, getWorkerPrepareData(), v1, v1.getProperty("k1", "p1"), null);

        v1 = getGraph().getVertex("v1", getGraphAuthorizations());
        assertEquals(1, count(v1.getProperties()));
    }

    @Test
    public void testIpAddressProperty() throws Exception {
        Metadata metadata = new Metadata();
        Vertex v1 = getGraph().prepareVertex("v1", visibility)
                .addPropertyValue("k1", "p1", "199.27.76.133", metadata, visibility)
                .save(getGraphAuthorizations());
        getGraph().flush();

        GeoPoint expectedGeoPoint = new GeoPoint(39, -77);
        when(geoIpRepository.find(eq("199.27.76.133"))).thenReturn(expectedGeoPoint);

        run(gpw, getWorkerPrepareData(), v1, v1.getProperty("k1", "p1"), null);

        v1 = getGraph().getVertex("v1", getGraphAuthorizations());
        assertEquals(2, count(v1.getProperties()));
        GeoPoint foundGeoPoint = (GeoPoint) v1.getPropertyValue("k1", TEST_GEO_LOCATION_PROPERTY_IRI);
        assertEquals(expectedGeoPoint, foundGeoPoint);
    }

    @Override
    protected FileSystem getFileSystem() {
        return this.fileSystem;
    }

    @Override
    protected Map getConfigurationMap() {
        Map result = super.getConfigurationMap();
        result.put(GeoIpGraphPropertyWorker.class.getName() + ".pathPrefix", "file:///test/");
        return result;
    }
}