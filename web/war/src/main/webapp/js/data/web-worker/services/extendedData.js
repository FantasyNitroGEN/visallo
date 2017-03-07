define(['../util/ajax', 'configuration/plugins/registry'], function(ajax, registry) {
    'use strict';

    return {
        rows: function(elementType, elementId, tableName, options) {
            var parameters = {
                elementType: elementType,
                elementId: elementId,
                tableName: tableName
            };

            return ajax('GET', '/extended-data', parameters);
        }
    }
});
