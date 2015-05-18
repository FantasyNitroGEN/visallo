define(['../registry'], function(registry) {
    'use strict';

    return {
        registerMimeTypeFileImportHandler: function(mimeType, handler) {
            console.warn('Deprecated: Use registry instead')
            registry.registerExtension('org.visallo.fileImport', {
                mimeType: mimeType,
                handler: handler
            });
        }
    };
});
