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

            this.on('parentLayoutChanged', function(event, data) {
                console.log(event.type, data)
            })

            this.renderChildren();
        });

        this.renderChildren = function() {
            var styling = calculateStyles(this.attr.layoutConfig)
            this.$node
                .css(styling)
                .empty()
                .append($.map(this.attr.children, function(child) {
                    if (child.configuration.style) {
                        $(child.element).css(child.configuration.style);
                    }
                    return child.element;
                }))
        }

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
