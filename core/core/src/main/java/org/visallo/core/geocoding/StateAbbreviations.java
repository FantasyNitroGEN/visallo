package org.visallo.core.geocoding;

import java.util.HashMap;
import java.util.Map;

public class StateAbbreviations {
    private static final Map<String, String> STATES_BY_ABBREVIATION = new HashMap<>();
    private static final Map<String, String> STATES_BY_NAME = new HashMap<>();

    static {
        STATES_BY_ABBREVIATION.put("AL", "Alabama");
        STATES_BY_ABBREVIATION.put("AK", "Alaska");
        STATES_BY_ABBREVIATION.put("AZ", "Arizona");
        STATES_BY_ABBREVIATION.put("AR", "Arkansas");
        STATES_BY_ABBREVIATION.put("CA", "California");
        STATES_BY_ABBREVIATION.put("CO", "Colorado");
        STATES_BY_ABBREVIATION.put("CT", "Connecticut");
        STATES_BY_ABBREVIATION.put("DE", "Delaware");
        STATES_BY_ABBREVIATION.put("FL", "Florida");
        STATES_BY_ABBREVIATION.put("GA", "Georgia");
        STATES_BY_ABBREVIATION.put("HI", "Hawaii");
        STATES_BY_ABBREVIATION.put("ID", "Idaho");
        STATES_BY_ABBREVIATION.put("IL", "Illinois");
        STATES_BY_ABBREVIATION.put("IN", "Indiana");
        STATES_BY_ABBREVIATION.put("IA", "Iowa");
        STATES_BY_ABBREVIATION.put("KS", "Kansas");
        STATES_BY_ABBREVIATION.put("KY", "Kentucky");
        STATES_BY_ABBREVIATION.put("LA", "Louisiana");
        STATES_BY_ABBREVIATION.put("ME", "Maine");
        STATES_BY_ABBREVIATION.put("MD", "Maryland");
        STATES_BY_ABBREVIATION.put("MA", "Massachusetts");
        STATES_BY_ABBREVIATION.put("MI", "Michigan");
        STATES_BY_ABBREVIATION.put("MN", "Minnesota");
        STATES_BY_ABBREVIATION.put("MS", "Mississippi");
        STATES_BY_ABBREVIATION.put("MO", "Missouri");
        STATES_BY_ABBREVIATION.put("MT", "Montana");
        STATES_BY_ABBREVIATION.put("NE", "Nebraska");
        STATES_BY_ABBREVIATION.put("NV", "Nevada");
        STATES_BY_ABBREVIATION.put("NH", "New Hampshire");
        STATES_BY_ABBREVIATION.put("NJ", "New Jersey");
        STATES_BY_ABBREVIATION.put("NM", "New Mexico");
        STATES_BY_ABBREVIATION.put("NY", "New York");
        STATES_BY_ABBREVIATION.put("NC", "North Carolina");
        STATES_BY_ABBREVIATION.put("ND", "North Dakota");
        STATES_BY_ABBREVIATION.put("OH", "Ohio");
        STATES_BY_ABBREVIATION.put("OK", "Oklahoma");
        STATES_BY_ABBREVIATION.put("OR", "Oregon");
        STATES_BY_ABBREVIATION.put("PA", "Pennsylvania");
        STATES_BY_ABBREVIATION.put("RI", "Rhode Island");
        STATES_BY_ABBREVIATION.put("SC", "South Carolina");
        STATES_BY_ABBREVIATION.put("SD", "South Dakota");
        STATES_BY_ABBREVIATION.put("TN", "Tennessee");
        STATES_BY_ABBREVIATION.put("TX", "Texas");
        STATES_BY_ABBREVIATION.put("UT", "Utah");
        STATES_BY_ABBREVIATION.put("VT", "Vermont");
        STATES_BY_ABBREVIATION.put("VA", "Virginia");
        STATES_BY_ABBREVIATION.put("WA", "Washington");
        STATES_BY_ABBREVIATION.put("WV", "West Virginia");
        STATES_BY_ABBREVIATION.put("WI", "Wisconsin");
        STATES_BY_ABBREVIATION.put("WY", "Wyoming");
        STATES_BY_ABBREVIATION.put("AS", "American Samoa");
        STATES_BY_ABBREVIATION.put("DC", "District of Columbia");
        STATES_BY_ABBREVIATION.put("FM", "Federated States of Micronesia");
        STATES_BY_ABBREVIATION.put("GU", "Guam");
        STATES_BY_ABBREVIATION.put("MH", "Marshall Islands");
        STATES_BY_ABBREVIATION.put("MP", "Northern Mariana Islands");
        STATES_BY_ABBREVIATION.put("PW", "Palau");
        STATES_BY_ABBREVIATION.put("PR", "Puerto Rico");
        STATES_BY_ABBREVIATION.put("VI", "Virgin Islands");
        STATES_BY_ABBREVIATION.put("AE", "Armed Forces Africa");
        STATES_BY_ABBREVIATION.put("AA", "Armed Forces Americas");
        STATES_BY_ABBREVIATION.put("AE", "Armed Forces Canada");
        STATES_BY_ABBREVIATION.put("AE", "Armed Forces Europe");
        STATES_BY_ABBREVIATION.put("AE", "Armed Forces Middle East");
        STATES_BY_ABBREVIATION.put("AP", "Armed Forces Pacific");

        for (Map.Entry<String, String> stateByAbbreviation : STATES_BY_ABBREVIATION.entrySet()) {
            STATES_BY_NAME.put(stateByAbbreviation.getValue().toUpperCase(), stateByAbbreviation.getKey());
        }
    }

    public static String getAbbreviation(String state) {
        return STATES_BY_NAME.get(state.trim().toUpperCase());
    }

    /**
     * Gets the state's abbreviation if found otherwise returns the passed in string.
     */
    public static String getAbbreviationOrDefault(String state) {
        String abbreviation = STATES_BY_NAME.get(state.trim().toUpperCase());
        if (abbreviation != null) {
            return abbreviation;
        }
        return state;
    }

    public static String getName(String abbreviation) {
        return STATES_BY_ABBREVIATION.get(abbreviation.trim().toUpperCase());
    }
}
