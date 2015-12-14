define([
    'configuration/plugins/registry',
    'util/requirejs/promise!util/service/ontologyPromise'
], function(registry, ontology) {
    'use strict';

    registry.documentExtensionPoint('org.visallo.web.dashboard.reportrenderer',
        'Define custom report renderers for dashboard',
        function(e) {
            return _.isFunction(e.supportsResponse) &&
                _.isString(e.identifier) &&
                e.identifier &&
                _.isString(e.label) &&
                e.componentPath;
        }
    );

    registry.registerExtension('org.visallo.web.dashboard.reportrenderer', {
        identifier: 'org-visallo-element-list',
        supportsResponse: function(data) {
            return data.type === 'TYPE_ELEMENTS';
        },
        label: 'Element List',
        componentPath: 'dashboard/reportRenderers/element-list'
    });

    registry.registerExtension('org.visallo.web.dashboard.reportrenderer', {
        identifier: 'org-visallo-bar-vertical',
        supportsResponse: function(data) {
            return data.type === 'TYPE_AGGREGATION' &&
                _.size(data.root) === 1 &&
                _.isFunction(data.root[0].displayName) &&
                _.every(data.root[0].buckets, function(item) {
                    return item.value && ('count' in item.value || 'nestedResults' in item.value) && !('cell' in item.value)
                });
        },
        label: 'Vertical Bar Chart',
        componentPath: 'dashboard/reportRenderers/bar'
    });

    registry.registerExtension('org.visallo.web.dashboard.reportrenderer', {
        identifier: 'org-visallo-bar-horizontal',
        supportsResponse: function(data) {
            return data.type === 'TYPE_AGGREGATION' &&
                _.size(data.root) === 1 &&
                data.root[0].type !== 'histogram' &&
                _.isFunction(data.root[0].displayName) &&
                _.every(data.root[0].buckets, function(item) {
                    return item.value && ('count' in item.value || 'nestedResults' in item.value) && !('cell' in item.value)
                });
        },
        label: 'Horizontal Bar Chart',
        componentPath: 'dashboard/reportRenderers/bar'
    });

    registry.registerExtension('org.visallo.web.dashboard.reportrenderer', {
        identifier: 'org-visallo-pie',
        supportsResponse: function(data) {
            return data.type === 'TYPE_AGGREGATION' &&
                _.size(data.root) === 1 &&
                _.isFunction(data.root[0].displayName) &&
                _.every(data.root[0].buckets, function(item) {
                    return item.value && 'count' in item.value && !('nested' in item.value) && !('cell' in item.value)
                });
        },
        label: 'Pie Chart',
        componentPath: 'dashboard/reportRenderers/pie'
    });

    registry.registerExtension('org.visallo.web.dashboard.reportrenderer', {
        identifier: 'org-visallo-choropleth',
        label: 'Choropleth',
        supportsResponse: function(data) {
            return data.type === 'TYPE_AGGREGATION' &&
                _.size(data.root) === 1 &&
                _.isFunction(data.root[0].displayName) &&
                ontology.properties.byTitle[data.root[0].field] &&
                _.contains(
                    ontology.properties.byTitle[data.root[0].field].intents || [],
                    'zipCode'
                ) &&
                _.every(data.root[0].buckets, function(item) {
                    return item.value && 'count' in item.value && !('nested' in item.value) && !('cell' in item.value)
                });
        },
        componentPath: 'dashboard/reportRenderers/choropleth'
    });

    registry.registerExtension('org.visallo.web.dashboard.reportrenderer', {
        identifier: 'org-visallo-subway',
        label: 'Subway',
        supportsResponse: function(data) {
            return data.type === 'TYPE_AGGREGATION' &&
                _.size(data.root) === 1 &&
                _.isFunction(data.root[0].displayName) &&
                _.every(data.root[0].buckets, function(item) {
                    return item.value && 'count' in item.value && !('nested' in item.value) && !('cell' in item.value)
                });
        },
        componentPath: 'dashboard/reportRenderers/subway'
    });

    registry.registerExtension('org.visallo.web.dashboard.reportrenderer', {
        identifier: 'org-visallo-text-overview',
        configurationPath: 'dashboard/configs/report/text-overview-config',
        supportsResponse: function(data) {
            return data.type === 'TYPE_AGGREGATION' &&
                _.size(data.root) === 1 &&
                _.isFunction(data.root[0].displayName) &&
                _.every(data.root[0].buckets, function(item) {
                    return item.value && 'count' in item.value && !('nested' in item.value) && !('cell' in item.value)
                });
        },
        label: 'Text Overview',
        componentPath: 'dashboard/reportRenderers/text-overview'
    });

    registry.registerExtension('org.visallo.web.dashboard.reportrenderer', {
        identifier: 'org-visallo-geohash',
        supportsResponse: function(data) {
            return data.type === 'TYPE_AGGREGATION' &&
                _.size(data.root) === 1 &&
                _.isFunction(data.root[0].displayName) &&
                _.every(data.root[0].buckets, function(item) {
                    return item.value && ('cell' in item.value && 'count' in item.value && 'point' in item.value);
                });
        },
        label: 'Heatmap',
        componentPath: 'dashboard/reportRenderers/geohash'
    });
});
