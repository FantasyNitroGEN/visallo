define(['configuration/plugins/registry'], function(registry) {
    'use strict';

    registry.registerExtension('org.visallo.web.dashboard.item', {
        title: 'Saved Search',
        description: 'Run a saved search',
        identifier: 'org-visallo-web-saved-search',
        componentPath: 'search/dashboard/savedSearch',
        configurationPath: 'search/dashboard/configure',
        grid: {
            width: 4,
            height: 4
        }
    });

    registry.registerExtension('org.visallo.web.dashboard.item', {
        title: 'Notifications',
        description: 'List of system and user notifications',
        identifier: 'org-visallo-web-notifications',
        componentPath: 'notifications/dashboardItem',
        grid: {
            width: 3,
            height: 3
        }
    });

    registry.registerExtension('org.visallo.web.dashboard.item', {
        title: 'Entity Counts',
        description: 'Show total counts for entity types',
        identifier: 'org-visallo-web-dashboard-concept-counts',
        report: {
            defaultRenderer: 'org-visallo-pie',
            endpoint: '/vertex/search',
            endpointParameters: {
                q: '*',
                size: 0,
                filter: '[]',
                aggregations: [
                    {
                        type: 'term',
                        name: 'field',
                        field: 'http://visallo.org#conceptType'
                    }
                ].map(JSON.stringify)
            }
        },
        grid: {
            width: 4,
            height: 2
        }
    });

    registry.registerExtension('org.visallo.web.dashboard.item', {
        title: 'Relationship Counts',
        description: 'Show total counts for relationship types',
        identifier: 'org-visallo-web-dashboard-edge-counts',
        report: {
            defaultRenderer: 'org-visallo-pie',
            endpoint: '/edge/search',
            endpointParameters: {
                q: '*',
                size: 0,
                filter: '[]',
                aggregations: [
                    {
                        type: 'term',
                        name: 'field',
                        field: '__edgeLabel'
                    }
                ].map(JSON.stringify)
            }
        },
        grid: {
            width: 4,
            height: 2
        }
    });

    registry.registerExtension('org.visallo.web.dashboard.item', {
        title: 'Welcome to Visallo',
        description: 'Learn how to work in Visallo',
        identifier: 'org-visallo-web-dashboard-welcome',
        componentPath: 'dashboard/items/welcome/welcome',
        options: {
            preventDefaultConfig: true
        },
        grid: {
            width: 5,
            height: 4
        }
    });
})
