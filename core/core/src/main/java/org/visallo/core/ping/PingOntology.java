package org.visallo.core.ping;

import com.google.common.base.Strings;
import org.visallo.core.model.properties.types.DateSingleValueVisalloProperty;
import org.visallo.core.model.properties.types.LongSingleValueVisalloProperty;
import org.visallo.core.model.properties.types.StringSingleValueVisalloProperty;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PingOntology {
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final String HOST_NAME = getHostName();

    public static final String BASE_IRI = "http://visallo.org/ping";

    public static final String IRI_CONCEPT_PING = BASE_IRI + "#ping";

    public static final DateSingleValueVisalloProperty CREATE_DATE = new DateSingleValueVisalloProperty(BASE_IRI + "#createDate");
    public static final StringSingleValueVisalloProperty CREATE_REMOTE_ADDR = new StringSingleValueVisalloProperty(BASE_IRI + "#createRemoteAddr");
    public static final LongSingleValueVisalloProperty SEARCH_TIME_MS = new LongSingleValueVisalloProperty(BASE_IRI + "#searchTimeMs");
    public static final LongSingleValueVisalloProperty RETRIEVAL_TIME_MS = new LongSingleValueVisalloProperty(BASE_IRI + "#retrievalTimeMs");
    public static final DateSingleValueVisalloProperty GRAPH_PROPERTY_WORKER_DATE = new DateSingleValueVisalloProperty(BASE_IRI + "#gpwDate");
    public static final StringSingleValueVisalloProperty GRAPH_PROPERTY_WORKER_HOSTNAME = new StringSingleValueVisalloProperty(BASE_IRI + "#gpwHostname");
    public static final StringSingleValueVisalloProperty GRAPH_PROPERTY_WORKER_HOST_ADDRESS = new StringSingleValueVisalloProperty(BASE_IRI + "#gpwHostAddress");
    public static final LongSingleValueVisalloProperty GRAPH_PROPERTY_WORKER_WAIT_TIME_MS = new LongSingleValueVisalloProperty(BASE_IRI + "#gpwWaitTimeMs");
    public static final DateSingleValueVisalloProperty LONG_RUNNING_PROCESS_DATE = new DateSingleValueVisalloProperty(BASE_IRI + "#lrpDate");
    public static final StringSingleValueVisalloProperty LONG_RUNNING_PROCESS_HOSTNAME = new StringSingleValueVisalloProperty(BASE_IRI + "#lrpHostname");
    public static final StringSingleValueVisalloProperty LONG_RUNNING_PROCESS_HOST_ADDRESS = new StringSingleValueVisalloProperty(BASE_IRI + "#lrpHostAddress");
    public static final LongSingleValueVisalloProperty LONG_RUNNING_PROCESS_WAIT_TIME_MS = new LongSingleValueVisalloProperty(BASE_IRI + "#lrpWaitTimeMs");

    public static String getVertexId(Date date) {
        return "PING_" + new SimpleDateFormat(DATE_TIME_FORMAT).format(date) + "_" + HOST_NAME;
    }

    private static String getHostName() {
        // Windows
        String host = System.getenv("COMPUTERNAME");
        if (!Strings.isNullOrEmpty(host)) {
            return host;
        }

        // Unix'ish
        host = System.getenv("HOSTNAME");
        if (!Strings.isNullOrEmpty(host)) {
            return host;
        }

        // Java which requires DNS resolution
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
            return "Unknown";
        }
    }
}
