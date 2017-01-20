define([], function() {

    function PopoverHelper(node, cy) {
        if (!_.isElement(node) || !cy) {
            throw new Error('Invalid arguments');
        }
        this.node = node;
        this.cy = cy;
        this.$node = $(node);

        this.onViewportChangesForPositionChanges = this.onViewportChangesForPositionChanges.bind(this);
        this.onRegisterForPositionChanges = this.onRegisterForPositionChanges.bind(this);
        this.$node
            .on('registerForPositionChanges', this.onRegisterForPositionChanges)
            .on('unregisterForPositionChanges', this.onUnregisterForPositionChanges);
    }

    PopoverHelper.prototype.destroy = function() {
        this.cy.off('pan zoom position', this.onViewportChangesForPositionChanges);
        this.viewportPositionChanges = null;
        this.$node
            .off('registerForPositionChanges')
            .off('unregisterForPositionChanges');
    }

    PopoverHelper.prototype.onRegisterForPositionChanges = function(event, data) {
        var self = this,
            cy = this.cy,
            anchorTo = data && data.anchorTo;

        if (!anchorTo || (!anchorTo.vertexId && !anchorTo.edgeId && !anchorTo.page && !anchorTo.decorationId)) {
            return console.error('Registering for position events requires a vertexId, edgeId, page, or decorationId');
        }

        var cyNode = anchorTo.vertexId
            ? cy.getElementById(anchorTo.vertexId)
            : anchorTo.edgeId
            ? cy.edges().filter(function(edgeIndex, edge) {
                return _.any(edge.data('edgeInfos'), function(edgeInfo) {
                    return edgeInfo.edgeId === anchorTo.edgeId;
                });
            })
            : anchorTo.decorationId
            ? cy.getElementById(anchorTo.decorationId)
            : undefined;
        if (!cyNode || cyNode.length == 0) {
            return console.error('Could not find cyNode');
        }

        event.stopPropagation();

        var offset = self.$node.offset(),
            cyPosition = anchorTo.page && cy.renderer().projectIntoViewport(
                anchorTo.page.x + offset.left,
                anchorTo.page.y + offset.top
            ),
            eventPositionDataForCyElement = function(cyElement) {
                if (cyElement.is('edge')) {
                    var connected = cyNode.connectedNodes(),
                        p1 = connected.eq(0).renderedPosition(),
                        p2 = connected.eq(1).renderedPosition(),
                        center = { x: (p1.x + p2.x) / 2 + offset.left, y: (p1.y + p2.y) / 2 + offset.top };

                    return {
                        position: center
                    };
                }

                var positionInNode = cyNode.renderedPosition(),
                    nodeOffsetNoLabel = cyNode.renderedOuterHeight() / 2,
                    nodeOffsetWithLabel = cyNode.renderedBoundingBox({ includeLabels: true }).h,
                    eventData = {
                        position: {
                            x: positionInNode.x + offset.left,
                            y: positionInNode.y + offset.top
                        }
                    };

                eventData.positionIf = {
                    above: {
                        x: eventData.position.x,
                        y: eventData.position.y - nodeOffsetNoLabel
                    },
                    below: {
                        x: eventData.position.x,
                        y: eventData.position.y - nodeOffsetNoLabel + nodeOffsetWithLabel
                    }
                };
                return eventData;
            };

        if (!self.viewportPositionChanges) {
            self.viewportPositionChanges = [];
            cy.on('pan zoom position', self.onViewportChangesForPositionChanges);
        }

        self.viewportPositionChanges.push({
            el: event.target,
            fn: function(el) {
                var eventData = {},
                    zoom = cy.zoom();
                if (anchorTo.vertexId || anchorTo.edgeId || anchorTo.decorationId) {
                    if (!cyNode.removed()) {
                        eventData = eventPositionDataForCyElement(cyNode);
                    }
                } else if (anchorTo.page) {
                    eventData.position = {
                        x: cyPosition[0] * zoom + cy.pan().x,
                        y: cyPosition[1] * zoom + cy.pan().y
                    };
                }
                eventData.anchor = anchorTo;
                eventData.zoom = zoom;
                $(el).trigger('positionChanged', eventData);
            }
        });

        self.onViewportChangesForPositionChanges();
    };

    PopoverHelper.prototype.onViewportChangesForPositionChanges = function() {
        var self = this;

        if (this.viewportPositionChanges) {
            this.viewportPositionChanges.forEach(function(vpc) {
                vpc.fn.call(self, vpc.el);
            })
        }
    };

    PopoverHelper.prototype.onUnregisterForPositionChanges = function(event, data) {
        var self = this,
            cy = this.cy;

        if (self.viewportPositionChanges) {
            var index = _.findIndex(self.viewportPositionChanges, function(vpc) {
                return vpc.el === event.target;
            })
            if (index >= 0) {
                self.viewportPositionChanges.splice(index, 1);
            }
            if (self.viewportPositionChanges.length === 0) {
                cy.off('pan zoom position', self.onViewportChangesForPositionChanges);
                self.viewportPositionChanges = null;
            }
        }
    };

    return PopoverHelper;
});
