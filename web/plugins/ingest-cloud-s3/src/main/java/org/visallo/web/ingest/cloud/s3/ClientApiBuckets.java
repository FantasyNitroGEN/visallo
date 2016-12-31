package org.visallo.web.ingest.cloud.s3;

import org.visallo.web.clientapi.model.ClientApiObject;

import java.util.Date;
import java.util.List;

public class ClientApiBuckets implements ClientApiObject {
    public String errorMessage;
    public List<ClientApiBucket> items;

    public static class ClientApiBucket {
        public Date date;
        public String name;
        public String type;
        public long size;
    }
}

