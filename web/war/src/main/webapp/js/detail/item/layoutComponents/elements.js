define([
    'detail/toolbar/toolbar',
    'util/vertex/formatters'
], function(Toolbar, F) {
    'use strict';

    return [
        {
            applyTo: { type: 'element[]' },
            identifier: 'org.visallo.layout.root',
            layout: { type: 'flex', options: { direction: 'column' }},
            componentPath: 'detail/item/elements',
            children: [
                { ref: 'org.visallo.layout.elements.header' },
                { ref: 'org.visallo.layout.elements.body', style: { flex: 1, overflow: 'auto', minHeight: '5.5em' } },
                { ref: 'org.visallo.layout.elements.list', style: { flex: '0 0', height: '150px', visibility: 'hidden' } }
            ]
        },
        {
            identifier: 'org.visallo.layout.elements.header',
            children: [
                { componentPath: 'detail/toolbar/toolbar', className: 'org-visallo-toolbar' }
            ]
        },
        {
            identifier: 'org.visallo.layout.elements.body',
            children: [
                { componentPath: 'detail/properties/histograms' }
            ]
        },
        {
            identifier: 'org.visallo.layout.elements.list',
            layout: { type: 'flex', options: { direction: 'column' }},
            children: [
                {
                    componentPath: 'util/element/list',
                    style: { overflow: 'auto', flex: 1 },
                    attributes: function(model) {
                        return {
                            items: model,
                            showSelected: false,
                            singleSelection: true,
                            ignoreUpdateModelNotImplemented: true
                        };
                    }
                }
            ]
        }
    ];
});
