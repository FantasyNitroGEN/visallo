package org.visallo.geoip;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.type.GeoPoint;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.file.FileSystemRepository;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GeoIpRepositoryTest {
    private GeoIpRepository geoIpRepository;

    @Mock
    private FileSystemRepository fileSystemRepository;

    @Before
    public void setUp() {
        when(fileSystemRepository.getInputStream(any(String.class))).thenReturn(new ByteArrayInputStream("".getBytes()));

        geoIpRepository = new GeoIpRepository(fileSystemRepository);
    }

    @Test
    public void testFindBadIpAddress() {
        try {
            geoIpRepository.find("300.1.1.1");
            throw new RuntimeException("Invalid IP Address expected");
        } catch (VisalloException ex) {
            // expected
        }
    }

    @Test
    public void testFind() {
        GeoPoint expected = new GeoPoint(39.0, -77.0, "Ashburn, VA");
        geoIpRepository.addGeoIp("199.27.0.0", 16, 4180316L, expected);
        geoIpRepository.addGeoIp("199.0.0.0", 8, null, new GeoPoint(0, 1));
        geoIpRepository.addGeoIp("199.27.75.0", 24, null, new GeoPoint(0, 2));
        geoIpRepository.addGeoIp("199.27.77.0", 24, null, new GeoPoint(0, 3));
        geoIpRepository.addLocation(4180316, "Ashburn, VA");

        GeoPoint geoPoint = geoIpRepository.find("199.27.76.133");
        assertEquals(expected, geoPoint);
        assertEquals(expected.getDescription(), geoPoint.getDescription());
    }

    @Test
    public void testAddGeoIpLine() {
        geoIpRepository.addGeoIpLine(Lists.newArrayList("1.0.0.0/24", "2077456", "2077456", "", "0", "0", "", "", ""));
    }

    @Test
    public void testLoad() {
        ByteArrayInputStream bain = new ByteArrayInputStream(
                ("network,geoname_id,registered_country_geoname_id,represented_country_geoname_id,is_anonymous_proxy,is_satellite_provider,postal_code,latitude,longitude\n" +
                        "1.0.0.0/24,2077456,2077456,,0,0,,-27.0000,133.0000\n" +
                        "1.0.1.0/24,1814991,1814991,,0,0,,35.0000,105.0000\n" +
                        "1.0.2.0/23,1814991,1814991,,0,0,,35.0000,105.0000\n" +
                        "1.0.4.0/22,2077456,2077456,,0,0,,-27.0000,133.0000\n" +
                        "1.0.8.0/21,1809858,1814991,,0,0,,23.1167,113.2500\n").getBytes());
        geoIpRepository.loadGeoIp(bain);

        bain = new ByteArrayInputStream(("2077347,en,OC,Oceania,AU,Australia,SA,\"South Australia\",,,Balaklava,,Australia/Adelaide\n" +
                "2077454,en,OC,Oceania,AU,Australia,WA,\"Western Australia\",,,Australind,,Australia/Perth\n" +
                "2077455,en,OC,Oceania,AU,Australia,SA,\"South Australia\",,,\"Australia Plains\",,Australia/Adelaide\n" +
                "2077456,en,OC,Oceania,AU,Australia,WA,\"Western Australia\",,,Augusta,,Australia/Perth\n" +
                "2077476,en,OC,Oceania,AU,Australia,WA,\"Western Australia\",,,Augusta,,Australia/Perth\n" +
                "2077579,en,OC,Oceania,AU,Australia,WA,\"Western Australia\",,,Armadale,,Australia/Perth\n" +
                "2077641,en,OC,Oceania,AU,Australia,WA,\"Western Australia\",,,Applecross,,Australia/Perth\n").getBytes());
        geoIpRepository.loadGeoLocations(bain);

        GeoPoint geoPoint = geoIpRepository.find("1.0.4.52");
        assertEquals(new GeoPoint(-27, 133), geoPoint);
        assertEquals("Oceania, Australia, Western Australia, Augusta", geoPoint.getDescription());
    }
}