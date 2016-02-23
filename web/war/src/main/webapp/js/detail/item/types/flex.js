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

        this.before('teardown', function() {
            syncStyles(this.node, null, { container: true });
            _.toArray(this.node.children).forEach(function(el) {
                syncStyles(el, null, { item: true });
            })
        })

        this.after('initialize', function() {

            this.on('updateLayout', function(event, data) {
                if (event.target === this.node) {
                    this.renderChildren(data.layoutConfig, data.children);
                }
            })

            this.renderChildren(this.attr.layoutConfig, this.attr.children);
        });

        this.renderChildren = function(layoutConfig, children) {
            var styling = calculateStyles(layoutConfig)
            syncStyles(this.node, styling, { container: true });

            // Adding new children not in DOM
            while (this.node.childElementCount < children.length) {
                var child = children[this.node.childElementCount]
                this.node.appendChild(child.element)
            }

            // Removing children that shouldn't be in DOM
            while (this.node.childElementCount > children.length) {
                this.node.removeChild(this.node.children[this.node.childElementCount - 1])
            }

            _.toArray(this.node.children).forEach(function(el, i) {
                var child = children[i],
                    style = child.configuration.style;

                syncStyles(el, transformStyle(style), { item: true });
            })
        };
    }

    function syncStyles(el, newStyles, options) {
        var styles = newStyles || {},
            toRemove = (options.container ?
                'flexDirection flexWrap display flexFlow justifyContent alignItems alignContent' :
                'order alignSelf flex flexGrow flexShrink flexBasis'
            ).split(' ');
        _.keys(el.style).forEach(function(name) {
            if (!(name in styles)) {
                if (_.contains(toRemove, name)) {
                    removeStyles(el, name);
                }
            }
        })

        if (newStyles) {
            $(el).css(newStyles);
        }
    }

    function removeStyles(el) {
        _.chain(arguments)
            .rest()
            .map(function(name) {
                return [
                    name,
                    name.replace(/([A-Z])/g, function(str, letter) {
                        return '-' + letter.toLowerCase();
                    })
                ];
            })
            .flatten()
            .compact()
            .unique()
            .each(function(name) {
                el.style.removeProperty(name);
                delete el.style[name];
            })
    }

    function transformStyle(css) {
        if (!css || !_.isObject(css)) {
            return;
        }
        // If it's an int, it can cause browsers issues
        if (css.flex) {
            css.flex = String(css.flex);
        }
        return css;
    }

    function calculateStyles(override) {
        return _.chain({
                flexDirection: 'column',
                display: 'flex'
            })
            .extend(_.chain(override)
                .map(function(value, key) {
                    return ['flex' + key.substring(0, 1).toUpperCase() + key.substring(1), value];
                })
                .object()
                .value()
            )
            .value()
    }
});
