package org.visallo.web.ingest.cloud.s3.routes;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.user.User;
import org.visallo.web.ingest.cloud.s3.AmazonS3ClientFactory;
import org.visallo.web.ingest.cloud.s3.ClientApiBuckets;

import java.util.stream.Collectors;

public class S3DirectoryListing implements ParameterizedHandler {
    private static String Delimiter = "/";
    private final AmazonS3ClientFactory amazonS3ClientFactory;

    @Inject
    public S3DirectoryListing(AmazonS3ClientFactory amazonS3ClientFactory) {
        this.amazonS3ClientFactory = amazonS3ClientFactory;
    }

    @Handle
    public ClientApiBuckets handle(
            User user,
            @Required(name = "providerClass") String providerClass,
            @Optional(name = "credentials") String credentials,
            @Optional(name = "path") String path
    ) throws Exception {
        AmazonS3 s3 = amazonS3ClientFactory.getClient(providerClass, credentials);

        if (path == null || path.equals("/") || path.isEmpty()) {
            return getBuckets(s3);
        }

        return getItems(s3, path);
    }

    private ClientApiBuckets getItems(AmazonS3 s3, String path) {
        if (!path.endsWith(Delimiter)) path = path + Delimiter;

        ClientApiBuckets bucketsResponse = new ClientApiBuckets();
        try {
            int firstDelimiter = path.indexOf("/");
            String bucketName = "";
            if (firstDelimiter >= 0) {
                bucketName = path.substring(0, firstDelimiter);
            }
            String directoryKey = path.substring(firstDelimiter + 1);
            String prefix = directoryKey.length() > 0 ? directoryKey : null;
            ListObjectsRequest request = new ListObjectsRequest()
                    .withBucketName(bucketName)
                    .withDelimiter(Delimiter)
                    .withPrefix(prefix);
            ObjectListing objectListing = s3.listObjects(request);

            // Add all files in prefix
            bucketsResponse.items = objectListing.getObjectSummaries().stream()
                    .filter(s3ObjectSummary -> !directoryKey.equals(s3ObjectSummary.getKey()))
                    .map(item -> {
                        ClientApiBuckets.ClientApiBucket b = new ClientApiBuckets.ClientApiBucket();
                        b.date = item.getLastModified();
                        b.name = prefix == null ? item.getKey() :
                                item.getKey().substring(item.getKey().lastIndexOf("/") + 1);
                        b.type = "file";
                        b.size = item.getSize();
                        return b;
                    })
                    .sorted((o1, o2) -> o1.name.compareToIgnoreCase(o2.name))
                    .collect(Collectors.toList());

            // TODO: Check isTruncated and support pagination

            // Add all directories in prefix
            bucketsResponse.items.addAll(0,
                objectListing.getCommonPrefixes().stream().map(dir -> {
                    ClientApiBuckets.ClientApiBucket b = new ClientApiBuckets.ClientApiBucket();
                    dir = dir.replaceAll("\\/$", "");
                    int slashIndex = dir.lastIndexOf("/");
                    if (slashIndex >= 0) {
                        dir = dir.substring(slashIndex + 1);
                    }

                    b.name = dir;
                    b.type = "dir";
                    return b;
                })
                    .sorted((o1, o2) -> o1.name.compareToIgnoreCase(o2.name))
                    .collect(Collectors.toList())
            );
        } catch (AmazonServiceException e) {
            bucketsResponse.errorMessage = "An error occurred while listing items from Amazon S3: " +  e.getErrorMessage();
        }
        return bucketsResponse;
    }

    private ClientApiBuckets getBuckets(AmazonS3 s3) {
        ClientApiBuckets bucketsResponse = new ClientApiBuckets();
        try {
            bucketsResponse.items = s3.listBuckets()
                    .stream()
                    .map(bucket -> {
                        ClientApiBuckets.ClientApiBucket b = new ClientApiBuckets.ClientApiBucket();
                        b.date = bucket.getCreationDate();
                        b.name = bucket.getName();
                        b.type = "bucket";
                        return b;
                    })
                    .sorted((o1, o2) -> o1.name.compareToIgnoreCase(o2.name))
                    .collect(Collectors.toList());
        } catch (AmazonServiceException e) {
            bucketsResponse.errorMessage = "An error occurred while retrieving buckets from Amazon S3: " + e.getErrorMessage();
        }
        return bucketsResponse;
    }
}
