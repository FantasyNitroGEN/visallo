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
            children: [
                { componentPath: 'detail/properties/properties', modelAttribute: 'data' },
                { componentPath: 'comments/comments', modelAttribute: 'data' },
                { componentPath: 'detail/relationships/relationships', modelAttribute: 'data' },
                { componentPath: 'detail/text/text' }
            ]
        },
        {
            identifier: 'org.visallo.layout.header',
            children: [
                { ref: 'org.visallo.layout.header.text' },
                { componentPath: 'detail/toolbar/toolbar' }
            ]
        }
    ];
});
