define([
    'flight/lib/component',
    'util/withDataRequest',
    'util/vertex/formatters',
    'util/privileges',
    'd3',
    'require'
], function(
    defineComponent,
    withDataRequest,
    F,
    Privileges,
    d3,
    require) {
    'use strict';

    var PERCENT_CLOSE_FOR_ROUNDING = 5; // Used for sorting x/y coordinates of detected objects
                                        // This is the distance (%) at which
                                        // objects are considered positioned similarly


    return defineComponent(DetectedObjects, withDataRequest);

    function DetectedObjects() {

        this.attributes({
            detectedObjectTagSelector: '.detected-object-tag',
            detectedObjectSelector: '.detected-object',
            model: null
        })

        this.after('initialize', function() {
            this.model = this.attr.model;

            this.node.classList.add('org-visallo-detectedObjects');
            this.on('updateModel', this.onUpdateModel);
            this.on('closeDropdown', this.onCloseDropdown);
            this.on('click', {
                detectedObjectSelector: this.onDetectedObjectClicked
            });

            var root = this.$node.closest('.org-visallo-layout-root');
            this.on(root, 'detectedObjectEdit', this.onDetectedObjectEdit);
            this.on(root, 'detectedObjectDoneEditing', this.onDetectedObjectDoneEditing);
            this.updateDetectedObjects();
        });

        this.onCloseDropdown = function(event) {
            var self = this;
            _.defer(function() {
                self.trigger('detectedObjectDoneEditing');
            })
        };

        this.onDetectedObjectDoneEditing = function(event, data) {
            this.$node.find('.underneath').teardownAllComponents().remove();
        };

        this.onDetectedObjectEdit = function(event, data) {
            if (data) {
                this.showForm(data, this.node);
            } else {
                this.$node.find('.underneath').teardownAllComponents();
            }
        };

        this.onUpdateModel = function(event, data) {
            this.model = data.model;
            this.updateDetectedObjects();
        };

        this.onDetectedObjectClicked = function(event) {
            if (Privileges.missingEDIT) {
                return;
            }

            event.preventDefault();

            var self = this,
                $target = $(event.target),
                propertyKey = $target.closest('.label-info').attr('data-property-key'),
                property = _.first(F.vertex.props(this.model, 'http://visallo.org#detectedObject', propertyKey));

            if (!property) {
                throw new Error('Unable to find detected object matching key:' + propertyKey);
            }
            this.$node.find('.focused').removeClass('focused');
            $target.closest('.detected-object').parent().addClass('focused');

            require(['util/actionbar/actionbar'], function(ActionBar) {
                ActionBar.teardownAll();
                self.$node.off('.actionbar')

                if ($target.hasClass('resolved')) {

                    ActionBar.attachTo($target, {
                        alignTo: 'node',
                        actions: $.extend({
                            Open: 'open.actionbar'
                        }, Privileges.canEDIT ? {
                            Unresolve: 'unresolve.actionbar'
                        } : {})
                    });

                    self.on('open.actionbar', function() {
                        self.trigger('selectObjects', { vertexIds: property.value.resolvedVertexId });
                    });
                    self.on('unresolve.actionbar', function() {
                        self.dataRequest('vertex', 'store', { vertexIds: property.value.resolvedVertexId })
                            .done(function(vertex) {
                                self.showForm({
                                    property: property,
                                    value: property.value,
                                    title: F.vertex.title(vertex),
                                    unresolve: true
                                },
                                    //$.extend({}, property.value, {
                                        //title: F.vertex.title(vertex),
                                        //propertyKey: property.key
                                    //}),
                                    $target
                                );
                            });
                    });

                } else if (Privileges.canEDIT) {

                    ActionBar.attachTo($target, {
                        alignTo: 'node',
                        actions: {
                            Resolve: 'resolve.actionbar'
                        }
                    });

                    self.on('resolve.actionbar', function(event) {
                        self.trigger('detectedObjectEdit', {
                            property: property,
                            value: property.value
                        });
                    })
                }
            });
        };

        this.showForm = function(data, $target) {
            var self = this;

            require(['../dropdowns/termForm/termForm'], function(TermForm) {
                self.$node.show().find('.underneath').teardownComponent(TermForm);
                var root = $('<div class="underneath">');

                if (data.property) {
                    root.appendTo(self.node);
                } else {
                    root.prependTo(self.node);
                }

                TermForm.attachTo(root, {
                    artifactData: self.model,
                    dataInfo: _.extend({}, data.property, {
                        originalPropertyKey: data.property && data.property.key,
                        title: data.title
                    }, data.value),
                    restrictConcept: data.value.concept,
                    existing: Boolean(data.property && data.property.resolvedVertexId),
                    detectedObject: true,
                    unresolve: data.unresolve || false
                });
            })
        };

        this.updateDetectedObjects = function() {
            var self = this,
                vertex = this.model,
                wasResolved = {},
                needsLoading = [],
                detectedObjects = vertex && F.vertex.props(vertex, 'detectedObject') || [],
                container = this.$node.toggle(detectedObjects.length > 0);

            detectedObjects.forEach(function(detectedObject) {
                var key = detectedObject.value.originalPropertyKey,
                    resolvedVertexId = detectedObject.value.resolvedVertexId;

                if (key) {
                    wasResolved[key] = true;
                }

                if (resolvedVertexId) {
                    needsLoading.push(resolvedVertexId);
                }
            });

            Promise.all([
                this.dataRequest('vertex', 'store', { vertexIds: needsLoading }),
                this.dataRequest('ontology', 'concepts')
            ]).done(function(results) {
                var vertices = results[0],
                    concepts = results[1],
                    verticesById = _.indexBy(vertices, 'id'),
                    roundCoordinate = function(percentFloat) {
                        return PERCENT_CLOSE_FOR_ROUNDING *
                            (Math.round(percentFloat * 100 / PERCENT_CLOSE_FOR_ROUNDING));
                    },
                    detectedObjectKey = _.property('key');

                d3.select(container.get(0))
                    .selectAll('.detected-object-tag')
                    .data(detectedObjects, detectedObjectKey)
                    .call(function() {
                        this.enter()
                            .append('span')
                            .attr('class', 'detected-object-tag')
                            .append('a');

                        this
                            .sort(function(a, b) {
                                var sort =
                                    roundCoordinate((a.value.y2 - a.value.y1) / 2 + a.value.y1) -
                                    roundCoordinate((b.value.y2 - b.value.y1) / 2 + b.value.y1)

                                if (sort === 0) {
                                    sort =
                                        roundCoordinate((a.value.x2 - a.value.x1) / 2 + a.value.x1) -
                                        roundCoordinate((b.value.x2 - b.value.x1) / 2 + b.value.x1)
                                }

                                return sort;
                            })
                            .style('display', function(detectedObject) {
                                if (wasResolved[detectedObject.key]) {
                                    return 'none';
                                }
                            })
                            .select('a')
                                .attr('data-vertex-id', function(detectedObject) {
                                    return detectedObject.value.resolvedVertexId;
                                })
                                .attr('data-property-key', function(detectedObject) {
                                    return detectedObject.key;
                                })
                                .attr('class', function(detectedObject) {
                                    var classes = 'label label-info detected-object opens-dropdown';
                                    if (detectedObject.value.edgeId) {
                                        return classes + ' resolved vertex'
                                    }
                                    return classes;
                                })
                                .text(function(detectedObject) {
                                    var resolvedVertexId = detectedObject.value.resolvedVertexId,
                                        resolvedVertex = resolvedVertexId && verticesById[resolvedVertexId];
                                    if (resolvedVertex) {
                                        return F.vertex.title(resolvedVertex);
                                    } else if (resolvedVertexId) {
                                        return i18n('detail.detected_object.vertex_not_found');
                                    }
                                    return concepts.byId[detectedObject.value.concept].displayName;
                                })
                    })
                    .exit().remove();

                    self.$node
                        .off('.detectedObject')
                        .on('mouseenter.detectedObject mouseleave.detectedObject',
                            self.attr.detectedObjectTagSelector,
                            self.onDetectedObjectHover.bind(self)
                        );

                    if (vertices.length) {
                        self.trigger('updateDraggables');
                    }
                });
        };

        this.onDetectedObjectHover = function(event) {
            var $target = $(event.target),
                tag = $target.closest('.detected-object-tag'),
                badge = tag.find('.label-info'),
                propertyKey = badge.attr('data-property-key');

            this.trigger(event.type === 'mouseenter' ? 'detectedObjectEnter' : 'detectedObjectLeave',
                F.vertex.props(this.model, 'http://visallo.org#detectedObject', propertyKey)
            );
        };

    }
});
