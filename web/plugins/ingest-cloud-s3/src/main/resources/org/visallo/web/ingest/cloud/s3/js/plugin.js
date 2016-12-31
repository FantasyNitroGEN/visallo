require(['configuration/plugins/registry'], function(registry) {
    registry.registerExtension('org.visallo.ingest.cloud', {
        identifier: 'org.visallo.web.ingest.cloud.s3.AmazonS3CloudResourceSource',
        componentPath: 'org/visallo/web/ingest/cloud/s3/dist/Config'
    })
});
