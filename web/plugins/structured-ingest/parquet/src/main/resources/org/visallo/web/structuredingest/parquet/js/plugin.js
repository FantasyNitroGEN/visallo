define(['configuration/plugins/registry'], function(registry) {

    var PARQUET_MIME_TYPE = 'application/x-parquet';

    registry.registerExtension('org.visallo.structuredingest', {
        mimeType: PARQUET_MIME_TYPE
    });
})