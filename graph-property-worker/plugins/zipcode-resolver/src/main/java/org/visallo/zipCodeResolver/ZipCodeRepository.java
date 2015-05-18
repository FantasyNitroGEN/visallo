package org.visallo.zipCodeResolver;

import org.visallo.core.exception.VisalloException;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZipCodeRepository {
    private final Map<String, ZipCodeEntry> zipCodesByZipCode = new HashMap<>();

    public ZipCodeRepository() {
        try {
            InputStreamReader reader = new InputStreamReader(this.getClass().getResourceAsStream("zipcode.csv"));
            CsvListReader csvReader = new CsvListReader(reader, CsvPreference.STANDARD_PREFERENCE);
            csvReader.read(); // skip title line

            List<String> line;
            while ((line = csvReader.read()) != null) {
                if (line.size() < 5) {
                    continue;
                }
                String zipCode = line.get(0);
                String city = line.get(1);
                String state = line.get(2);
                double latitude = Double.parseDouble(line.get(3));
                double longitude = Double.parseDouble(line.get(4));
                zipCodesByZipCode.put(zipCode, new ZipCodeEntry(zipCode, city, state, latitude, longitude));
            }
        } catch (IOException ex) {
            throw new VisalloException("Could not read zipcode.csv", ex);
        }
    }

    public ZipCodeEntry find(String text) {
        int dash = text.indexOf('-');
        if (dash > 0) {
            text = text.substring(0, dash);
        }
        return zipCodesByZipCode.get(text);
    }
}
