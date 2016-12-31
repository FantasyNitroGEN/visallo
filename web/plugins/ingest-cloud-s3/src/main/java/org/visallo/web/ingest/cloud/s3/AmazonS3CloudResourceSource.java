package org.visallo.web.ingest.cloud.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;
import org.visallo.core.ingest.cloud.CloudResourceSource;
import org.visallo.core.ingest.cloud.CloudResourceSourceItem;
import org.visallo.core.util.JSONUtil;

import java.io.InputStream;
import java.util.Collection;
import java.util.stream.Collectors;

public class AmazonS3CloudResourceSource implements CloudResourceSource {
    private final AmazonS3ClientFactory amazonS3ClientFactory;

    @Inject
    public AmazonS3CloudResourceSource(AmazonS3ClientFactory amazonS3ClientFactory) {
        this.amazonS3ClientFactory = amazonS3ClientFactory;
    }

    @Override
    public Collection<CloudResourceSourceItem> getItems(JSONObject configuration) {
        AmazonS3 s3 = getAmazonClient(configuration);

        String bucket = configuration.getString("bucket");
        JSONArray paths = configuration.getJSONArray("paths");

        return JSONUtil.toList(paths)
                .stream()
                .map(key -> new AmazonS3CloudResourceSourceItem(s3, bucket, (String) key))
                .collect(Collectors.toList());
    }

    private AmazonS3 getAmazonClient(JSONObject configuration) {
        JSONObject auth = configuration.getJSONObject("auth");
        String providerClass = auth.getString("providerClass");
        JSONObject credentials = auth.getJSONObject("credentials");

        return amazonS3ClientFactory.getClient(providerClass, credentials);
    }

    static class AmazonS3CloudResourceSourceItem implements CloudResourceSourceItem {
        private S3Object object;
        private AmazonS3 s3;
        private String bucket;
        private String key;

        AmazonS3CloudResourceSourceItem(AmazonS3 s3, String bucket, String key) {
            this.s3 = s3;
            this.bucket = bucket;
            this.key = key.replaceAll("^\\/", "");
        }

        @Override
        public InputStream getInputStream() {
            return getObject().getObjectContent();
        }

        @Override
        public String getName() {
            int last = key.lastIndexOf("/");
            String name = key;

            if (last >= 0) {
                name = name.substring(last + 1);
            }

            return name;
        }

        @Override
        public Long getSize() {
            return getObject().getObjectMetadata().getContentLength();
        }

        private synchronized S3Object getObject() {
            if (object == null) {
                object = s3.getObject(new GetObjectRequest(bucket, key));
            }
            return object;
        }
    }
}
