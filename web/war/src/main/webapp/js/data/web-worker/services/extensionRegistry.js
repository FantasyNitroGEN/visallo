define(['configuration/plugins/registry'], function(registry) {
    'use strict';

    return {
        get: function() {
            return Promise.resolve(registry.extensionPointDocumentation());
        }
    }
});
