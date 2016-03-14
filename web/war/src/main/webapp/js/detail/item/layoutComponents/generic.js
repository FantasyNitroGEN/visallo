define([], function() {
    'use strict';

    return [
        {
            identifier: 'org.visallo.layout.text',
            render: function(el, model, config) {
                if (config && config.style && _.isString(config.style)) {
                    var cls = config.style;
                    switch (config.style) {
                      case 'title':
                      case 'subtitle':
                      case 'heading1':
                      case 'heading2':
                      case 'heading3':
                      case 'body':
                      case 'footnote':
                        break;
                      default:
                        throw new Error('Unknown config style: ' + config.style)
                    }
                    el.classList.add(config.style)
                }
                el.textContent = String(model);
            }
        },
        {
            identifier: 'org.visallo.layout.body',
            applyTo: function(model, options) {
                var enoughWidth = !_.contains(options.constraints, 'width');
                if (enoughWidth) {
                    var comment = _.findWhere(model.properties, { name: 'http://visallo.org/comment#entry' }),
                        hasRelations = !_.isEmpty(model.edgeLabels);
                    return comment || hasRelations;
                }
                return false;
            },
            children: [
                { ref: 'org.visallo.layout.body.split' },
                { componentPath: 'detail/text/text', className: 'org-visallo-texts' }
            ]
        },
        {
            identifier: 'org.visallo.layout.body.split',
            layout: { type: 'flex', options: { direction: 'row' }},
            children: [
                { ref: 'org.visallo.layout.body.left', style: { flex: 1 }},
                { ref: 'org.visallo.layout.body.right', style: { flex: 1 }}
            ]
        },
        {
            identifier: 'org.visallo.layout.body.left',
            children: [
                { componentPath: 'detail/properties/properties', className: 'org-visallo-properties', modelAttribute: 'data' }
            ]
        },
        {
            identifier: 'org.visallo.layout.body.right',
            children: [
                { componentPath: 'comments/comments', className: 'org.visallo-comments', modelAttribute: 'data' },
                { componentPath: 'detail/relationships/relationships', className: 'org-visallo-relationships', modelAttribute: 'data' }
            ]
        },
        {
            identifier: 'org.visallo.layout.body',
            children: [
                { componentPath: 'detail/properties/properties', className: 'org-visallo-properties', modelAttribute: 'data' },
                { componentPath: 'comments/comments', className: 'org.visallo-comments', modelAttribute: 'data' },
                { componentPath: 'detail/relationships/relationships', className: 'org-visallo-relationships', modelAttribute: 'data' },
                { componentPath: 'detail/text/text', className: 'org-visallo-texts' }
            ]
        },
        {
            identifier: 'org.visallo.layout.header',
            children: [
                { ref: 'org.visallo.layout.header.text' },
                { componentPath: 'detail/toolbar/toolbar', className: 'org-visallo-toolbar' }
            ]
        }
    ];
});
