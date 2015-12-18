define([], [
    {
        extensionId: 'org-visallo-web-dashboard-welcome',
        configuration: { metrics: { x: 0, y: 0, width: 6, height: 5 } }
    },
    {
        extensionId: 'org-visallo-web-dashboard-concept-counts',
        configuration: { metrics: { x: 6, y: 0, width: 3, height: 2 }, reportRenderer: 'org-visallo-pie' }
    },
    {
        extensionId: 'org-visallo-web-notifications',
        configuration: { metrics: { x: 9, y: 0, width: 3, height: 2 } }
    },
    {
        extensionId: 'org-visallo-web-dashboard-edge-counts',
        configuration: { metrics: { x: 6, y: 2, width: 6, height: 3 }, reportRenderer: 'org-visallo-bar-vertical' }
    }
]);
