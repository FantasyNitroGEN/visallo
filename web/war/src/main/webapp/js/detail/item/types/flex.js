define([
    'flight/lib/component'
], function(defineComponent) {
    'use strict';

    return defineComponent(FlexLayout);

    function FlexLayout() {

        this.attributes({
            layoutConfig: null,
            children: null
        });

        this.after('initialize', function() {

            this.on('updateLayout', function(event, data) {
                if (event.target === this.node) {
                    this.renderChildren(data.children);
                }
            })

            this.renderChildren(this.attr.children);
        });

        this.renderChildren = function(children) {
            var styling = calculateStyles(this.attr.layoutConfig)
            this.$node.css(styling)

            while (this.node.childElementCount < children.length) {
                var child = children[this.node.childElementCount]
                if (child.configuration.style) {
                    $(child.element).css(child.configuration.style);
                }
                this.node.appendChild(child.element)
            }
            while (this.node.childElementCount > children.length) {
                this.node.removeChild(this.node.children[this.node.childElementCount - 1])
            }
        };
    }

    function calculateStyles(override) {
        return _.extend({
                'flex-direction': 'column',
                display: 'flex'
            }, _.chain(override)
            .map(function(value, key) {
                return ['flex-' + key, value];
            })
            .object()
            .value()
        );
    }
});
