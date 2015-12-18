define([
    'flight/lib/component',
    'configuration/plugins/registry',
    'hbs!./welcomeTpl'
], function(
    defineComponent,
    registry,
    template) {
    'use strict';

    return defineComponent(Welcome);

    function Welcome() {

        var builtIn = {
                activity: 'hbs!dashboard/items/welcome/activity',
                admin: 'hbs!dashboard/items/welcome/admin',
                dashboard: 'hbs!dashboard/items/welcome/dashboard',
                graph: 'hbs!dashboard/items/welcome/graph',
                logout: 'hbs!dashboard/items/welcome/logout',
                search: 'hbs!dashboard/items/welcome/search',
                workspaces: 'hbs!dashboard/items/welcome/workspaces',
                map: 'hbs!dashboard/items/welcome/map'
            },
            icons = {
                activity: '../img/glyphicons/white/glyphicons_023_cogwheels@2x.png',
                admin: '../img/glyphicons/white/glyphicons_439_wrench@2x.png',
                dashboard: '../img/visallo-icon@2x.png',
                graph: '../img/glyphicons/white/glyphicons_326_share@2x.png',
                logout: '../img/glyphicons/white/glyphicons_387_log_out@2x.png',
                search: '../img/glyphicons/white/glyphicons_027_search@2x.png',
                workspaces: '../img/glyphicons/white/glyphicons_153_more_windows@2x.png',
                map: '../img/glyphicons/white/glyphicons_242_google_maps@2x.png'
            },
            menubarExtensions = registry.extensionsForPoint('org.visallo.menubar');

        this.attributes({
            highlightSelector: '*[data-selector]'
        });

        this.before('teardown', function() {
            $('.welcome-spotlight').remove();
        })

        this.after('initialize', function() {
            this.$node
                .css('overflow', 'auto')
                .addClass('org-visallo-welcome')
                .html(template({}));

            this.on('mouseover', {
                highlightSelector: this.onSpotlight
            });
            this.on('mouseout', {
                highlightSelector: this.onSpotlight
            })

            this.on(document, 'click', this.onSpotlight);

            Promise.resolve(getTemplates())
                .map(function(v) {
                    return Promise.require(v.path).then(function(template) {
                        v.template = template;
                        return v;
                    })
                })
                .done(this.render.bind(this));
        });

        this.onSpotlight = function(event) {
            var srcElement = event.target,
                selector = srcElement.dataset && srcElement.dataset.selector,
                valid = _.contains(['click', 'mouseover'], event.type),
                targetElement = selector && $(selector);

            if (valid && targetElement && targetElement.length) {
                targetElement = targetElement.get(0);
                this.adjustSpotlight(srcElement, targetElement);
            } else {
                $('.welcome-spotlight').hide();
            }
        };

        this.adjustSpotlight = function(src, dest) {
            var srcRect = rectForEl(src),
                destRect = rectForEl(dest);

            if (srcRect && destRect) {
                require(['d3'], function(d3) {
                    d3.select(document.body)
                        .selectAll('svg.welcome-spotlight')
                        .data([1])
                        .call(function() {
                            this.enter()
                                .append('svg')
                                    .attr('class', 'welcome-spotlight')
                                    .style({
                                        'pointer-events': 'none',
                                        position: 'absolute',
                                        display: 'none',
                                        left: 0,
                                        top: 0,
                                        bottom: 0,
                                        right: 0,
                                        width: '100%',
                                        height: '100%',
                                        'z-index': 1000
                                    })
                                .append('path')

                            this.select('path')
                                .style({
                                    stroke: 'rgba(251, 232, 37, 0.93)',
                                    fill: 'rgba(255, 236, 45, 0.48)',
                                    'stroke-width': 1
                                })
                                .attr('d', pathForSrcDestRect(srcRect, destRect))

                            var svg = this.node();
                            svg.style.opacity = 0;
                            svg.style.display = '';
                            requestAnimationFrame(function() {
                                svg.style.opacity = 1;
                            })
                        });
                });
            }

            function pathForSrcDestRect(s, d) {
                var dx = d.x - s.x,
                    dy = d.y - s.y,
                    dd = Math.sqrt(dx * dx + dy * dy),
                    r = d.r + 10,
                    padding = 5,
                    rInner = d.r + padding;

                if (dd < r) {
                    // Special case when too close or target too large,
                    // just draw rect around
                    return moveAbs(d.x1, d.y1) +
                        line(0, d.h) +
                        line(d.w, 0) +
                        line(0, -d.h) + 'Z ' +
                        moveAbs(d.x1 + padding, d.y1 + padding) +
                        line(d.w - padding * 2, 0) +
                        line(0, d.h - padding * 2) +
                        line((d.w - padding * 2) * -1, 0) + ' Z';
                }

                var a = Math.asin(r / dd),
                    b = Math.atan2(dy, dx),
                    t = b - a,
                    ta = { x: r * Math.sin(t) + dx, y: r * -Math.cos(t) + dy },
                    taInner = { x: rInner * Math.sin(t) + dx, y: rInner * -Math.cos(t) + dy },
                    tb;

                t = b + a;
                tb = { x: r * -Math.sin(t) + dx, y: r * Math.cos(t) + dy };

                //M 657,294 l 148,-268 a 14,14,20 1 1 25,16 L 657,294
                return moveAbs(s.x, s.y) +
                    line(ta.x, ta.y) +
                    arc(tb.x - ta.x, tb.y - ta.y, r) + 'Z ' +
                    moveAbs(d.x - rInner, d.y) +
                    //line(rInner * 2, 0)
                    //moveAbs(taInner.x + s.x, taInner.y + s.y) +
                    //line(tb.x, tb.y)
                    arc(rInner * 2, 0, rInner, true) +
                    arc(rInner * -2, 0, rInner, true);
                    //M657,294 m155,-248 a 10,10,10 1 0 .1,.1 z

                function moveAbs(x, y) { return 'M ' + x + ',' + y + ' '; }
                function arc(x, y, r, counterClockwise) {
                    return 'a' + r + ',' + r + ',' + r +
                        ' 1 ' + (counterClockwise ? 0 : 1) + ' ' +
                        x + ',' + y + ' ';
                }
                function line(x, y) { return 'l ' + x + ',' + y + ' '; }
            }

            function rectForEl(el) {
                var rects = el.getClientRects();
                if (rects.length) {
                    var r = rects[0];
                    return {
                        x: r.left + r.width / 2,
                        y: r.top + r.height / 2,
                        r: Math.max(r.width / 2, r.height / 2),
                        x1: r.left,
                        y1: r.top,
                        w: r.width,
                        h: r.height
                    };
                }
            }
        };

        this.render = function(sections) {
            this.$node.append($.map(sections, function(section) {
                var selector = '.menubar-pane .' + section.identifier;
                return $('<section>')
                    .append($('<h2>')
                        .toggleClass('has-icon', !!section.icon)
                        .text(section.display)
                        .prepend($('<div>')
                            .attr('data-selector', selector)
                            .css('background-image', section.icon && ('url(' + section.icon + ')') || null)
                        )
                    )
                    .append($('<p>').html(section.template({})));
            }));
        }

        function getTemplates() {
            return _.compact(getMenuIdentifiers().map(getDisplayAndTemplatePathForIdentifier));
        }

        function getMenuIdentifiers() {
            return _.compact(
                $('.menubar-pane li')
                .map(function() {
                    return $(this).data('identifier');
                })
                .toArray()
            );
        }

        function getDisplayAndTemplatePathForIdentifier(identifier) {
            if (identifier in builtIn) {
                return {
                    identifier: identifier,
                    display: i18n('menubar.icons.' + identifier),
                    icon: icons[identifier],
                    path: builtIn[identifier]
                };
            }

            var extension = _.findWhere(menubarExtensions, { identifier: identifier });
            if (extension) {
                if (extension.welcomeTemplatePath) {
                    return {
                        identifier: identifier,
                        display: extension.options && extension.options.tooltip || extension.title,
                        icon: extension.icon,
                        path: extension.welcomeTemplatePath
                    };
                } else console.warn('Consider adding welcomeTemplatePath to menubar extension', extension);
                return;
            }

            console.warn('No welcome template for identifier', identifier)
        }
    }
});
