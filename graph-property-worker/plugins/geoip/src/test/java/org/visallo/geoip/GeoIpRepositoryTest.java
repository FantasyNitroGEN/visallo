package org.visallo.geoip;

import org.junit.Before;
import org.junit.Test;
import org.vertexium.type.GeoPoint;
import org.visallo.core.exception.VisalloException;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;

public class GeoIpRepositoryTest {
    private GeoIpRepository geoIpRepository;

    @Before
    public void setUp() {
        geoIpRepository = new GeoIpRepository();
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
        GeoPoint expected = new GeoPoint(39.0, -77.0);
        geoIpRepository.addGeoIp("199.27.0.0", 16, expected);
        geoIpRepository.addGeoIp("199.0.0.0", 8, new GeoPoint(0, 1));
        geoIpRepository.addGeoIp("199.27.75.0", 24, new GeoPoint(0, 2));
        geoIpRepository.addGeoIp("199.27.77.0", 24, new GeoPoint(0, 3));

        GeoPoint geoPoint = geoIpRepository.find("199.27.76.133");
        assertEquals(expected, geoPoint);
    }

    @Test
    public void testAddGeoIpLine() {
        geoIpRepository.addGeoIpLine("1.0.0.0/24,2077456,2077456,,0,0,,,");
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
        geoIpRepository.load(bain);

        GeoPoint geoPoint = geoIpRepository.find("1.0.4.52");
        assertEquals(new GeoPoint(-27, 133), geoPoint);
    }
}