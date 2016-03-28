define(['configuration/plugins/registry'], function(registry) {
    'use strict';

    registry.documentExtensionPoint('org.visallo.graph.node.decoration',
        'Description',
        function(e) {
            if (e.applyTo && !_.isFunction(e.applyTo)) return false;
            if (!_.isObject(e.alignment)) return false;
            if (!_.contains(['left', 'center', 'right'], e.alignment.h)) return false;
            if (!_.contains(['top', 'center', 'bottom'], e.alignment.v)) return false;
            return true;
        },
        'http://docs.visallo.org/extension-points/front-end/graphDecorations'
    );

    var registerListeners = _.once(_registerListeners),
        inc = 0,
        genId = function(prefix) {
            var id = (prefix || 'dec') + inc;
            inc++;
            return id;
        },
        api = {
            updateDecorations: function(vertices, options) {
                var delayed = { updates: [], additions: [] },
                    executeAndReturn = function(f) { return f(); };

                vertices.forEach(function(updatedVertex) {
                    var cyNode = options.vertexToCyNode(updatedVertex);
                    if (cyNode) {
                        var delayedResponse = api.updateCyNodeDecorations(cyNode, updatedVertex);
                        if (delayedResponse.updates.length) {
                            delayedResponse.updates.forEach(function(u) {
                                delayed.updates.push(u);
                            })
                        }
                        if (delayedResponse.additions.length) {
                            delayedResponse.additions.forEach(function(u) {
                                delayed.additions.push(u);
                            })
                        }
                    }
                });

                return Promise.all(delayed.updates.map(executeAndReturn))
                    .then(function() {
                        return Promise.all(delayed.additions.map(executeAndReturn));
                    })
            },
            updateCyNodeDecorations: function(cyNode, vertex) {
                var cy = cyNode.cy(),
                    decorations = _.filter(registry.extensionsForPoint('org.visallo.graph.node.decoration'), function(e) {
                        return !_.isFunction(e.applyTo) || e.applyTo(vertex);
                    }),
                    needsConvertToCompound = decorations.length && !cyNode.isChild(),
                    needsConvertFromCompound = !decorations.length && cyNode.isChild(),
                    parentCyNode,
                    delayed = { updates: [], additions: [] };

                registerListeners(cy);

                if (needsConvertToCompound) {
                    parentCyNode = cy.add({
                        group: 'nodes',
                        data: { id: genId('decP') },
                        classes: 'decorationParent',
                        selectable: false,
                        grabbable: false
                    });
                    cyNode.move({
                        parent: parentCyNode.id()
                    })
                } else if (needsConvertFromCompound) {
                    parentCyNode = cyNode.parent();
                } else {
                    parentCyNode = cyNode.parent();
                }
                var children = (parentCyNode.length ? parentCyNode[0] : parentCyNode)._private.children;

                if (decorations.length || (children && children.length)) {
                    var position = cyNode.position(),
                        specs = specsForNode(cyNode),
                        parentId = parentCyNode.id(),
                        toAdd = decorations.slice(0),
                        toRemove = cy.collection(),
                        addParent = function(d, previousInfo) {
                            var data = d ? _.clone(d) : {};
                            data.parent = previousInfo && previousInfo.parent || parentId;
                            data.id = previousInfo && previousInfo.id || genId();
                            return data;
                        };

                    children.forEach(function(decorationNode) {
                        if (!decorationNode.hasClass('decoration')) return
                        var e = decorationNode.scratch('extension'),
                            existingPromise = decorationNode.scratch('dataPromise'),
                            decorationIndex = _.findIndex(toAdd, function(d) {
                                return d === e;
                            }),
                            extension = decorationIndex >= 0 && toAdd[decorationIndex],
                            updateData = function(data) {
                                var previousData = decorationNode._private.data;

                                if (!_.isObject(data)) {
                                    throw new Error('data is not an object', data)
                                }
                                decorationNode._private.data = addParent(data, _.pick(previousData, 'id', 'parent'));
                                decorationNode.scratch('vertex', vertex);
                                decorationNode.trigger('data');
                                decorationNode.toggleClass('hidden', false);
                                decorationNode.updateStyle();
                                decorationNode.position(calculatePosition(e, decorationNode, specs));
                            };

                        if (existingPromise) {
                            existingPromise.cancel();
                        }

                        if (extension) {
                            toAdd.splice(decorationIndex, 1);
                            if (_.isFunction(extension.data)) {
                                delayed.updates.push(function delayedUpdate() {
                                    return Promise.resolve(extension.data(vertex)).then(updateData)
                                })
                            } else updateData(extension.data);
                        } else {
                            decorationNode.toggleClass('hidden', true);
                        }
                    })

                    var newNodes = toAdd.map(function(d) {
                            var classes = getClasses(d, vertex),
                                data = getData(d, vertex),
                                requiresDataPromise = data && _.isFunction(data.then),
                                dataPromise = Promise.resolve(data).then(addParent);

                            return {
                                requiresDataPromise: requiresDataPromise,
                                dataPromise: dataPromise,
                                nodeSpec: {
                                    group: 'nodes',
                                    classes: classes,
                                    data: requiresDataPromise ? undefined : addParent(data),
                                    scratch: {
                                        extension: d,
                                        vertex: vertex,
                                        dataPromise: dataPromise
                                    },
                                    grabbable: false,
                                    selectable: false,
                                    position: calculatePosition(d, null, specs)
                                }
                            };
                        });
                    if (newNodes.length) {
                        var promiseRequiredPartition = _.partition(newNodes, function(newNode) {
                                return newNode.requiresDataPromise;
                            }),
                            promiseRequired = promiseRequiredPartition[0],
                            noPromiseRequired = promiseRequiredPartition[1];

                        if (noPromiseRequired.length) {
                            var nodeSpecs = noPromiseRequired.map(_.property('nodeSpec')),
                                added = cy.add(nodeSpecs);
                            delayed.additions.push(function delayed() {
                                added.each(function() {
                                    var extension = this.scratch('extension');
                                    this.position(calculatePosition(extension, this, specs));
                                    triggerCreateHandler(extension, this);
                                })
                            })
                        }
                        if (promiseRequired.length) {
                            delayed.additions.push(function delayed() {
                                return Promise.all(
                                    promiseRequired.map(function(newNode) {
                                        newNode.nodeSpec.data = addParent(newNode.nodeSpec.data);
                                        newNode.nodeSpec.classes += ' hidden';
                                        var decorationNode = cy.add(newNode.nodeSpec)[0];
                                        return newNode.dataPromise.then(function(data) {
                                            decorationNode.data(data);
                                            delete newNode.nodeSpec.scratch.dataPromise;
                                            return new Promise(function(fulfill) {
                                                requestAnimationFrame(function() {
                                                    var extension = decorationNode.scratch('extension');
                                                    decorationNode.removeClass('hidden');
                                                    decorationNode.position(
                                                        calculatePosition(extension, decorationNode, specs)
                                                    );
                                                    triggerCreateHandler(extension, decorationNode);
                                                    fulfill();
                                                })
                                            });
                                        })
                                    }))
                            });
                        }
                    }
                }

                return delayed;
            }
        };

    return api;

    function triggerCreateHandler(extension, decorationNode) {
        if (_.isFunction(extension.onCreate)) {
            extension.onCreate.call(decorationNode);
        }
    }

    function eventTypeToHandlerName(event) {
        return {
            tap: 'onClick',
            mouseover: 'onMouseOver',
            mouseout: 'onMouseOut'
        }[event.type]
    }

    function _registerListeners(cy) {
        cy.on('tap mouseover mouseout', 'node.decoration', function(event) {
            var extension = this.scratch('extension'),
                handlerName = eventTypeToHandlerName(event);
            if (extension && _.isFunction(extension[handlerName])) {
                extension[handlerName].call(this, event, {
                    cy: cy,
                    vertex: this.scratch('vertex')
                });
            }
        });
        cy.on('remove', 'node.v', function(event) {
            var node = event.cyTarget;
            if (node.isChild()) {
                var decorations = node.siblings('.decoration');
                _.defer(function() {
                    cy.batch(function() {
                        decorations.toggleClass('hidden', true);
                    })
                });
            }
        })
        cy.on('position', function(event) {
            var node = event.cyTarget,
                decoration;

            if (node.hasClass('decoration')) return;
            if (node.isChild()) {
                decoration = node.siblings().filter('.decoration');
                if (decoration) {
                    var specs = specsForNode(node);
                    cy.batch(function() {
                        decoration.each(function() {
                            this.position(calculatePosition(this.scratch('extension'), this, specs));
                        })
                    })
                }
            }
        });
    }

    function getData(extension, vertex) {
        var data;
        if (_.isFunction(extension.data)) {
            data = extension.data(vertex);
        } else if (extension.data) {
            data = extension.data;
        }
        if (!_.isObject(data)) {
            throw new Error('data is not an object', data)
        }
        return data;
    }

    function getClasses(extension, vertex) {
        var cls = ['decoration'];

        if (_.isString(extension.classes)) {
            cls = cls.concat(extension.classes.trim().split(/\s+/));
        } else if (_.isFunction(extension.classes)) {
            var newClasses = extension.classes(vertex);
            if (!_.isArray(newClasses) && _.isString(newClasses)) {
                newClasses = newClasses.trim().split(/\s+/);
            }
            if (_.isArray(newClasses)) {
                cls = cls.concat(newClasses)
            }
        }

        return cls.join(' ');
    }

    function specsForNode(node) {
        return {
            position: node.position(),
            textHAlign: node.style('text-halign'),
            textVAlign: node.style('text-valign'),
            h: node.height(),
            w: node.width(),
            bbox: node.boundingBox({includeLabels: false}),
            bboxLabels: node.boundingBox({includeLabels: true})
        }
    }

    function calculatePosition(extension, decorationNode, specs) {
        if (extension.alignment) {
            var x, y,
                decBBox = decorationNode ?
                    decorationNode.boundingBox({ includeLabels: false }) :
                    { w: 0, h: 0 },
                decBBoxLabels = decorationNode ?
                    decorationNode.boundingBox({ includeLabels: true }) :
                    { w: 0, h: 0 },
                padding = 8;

            switch (extension.alignment.h) {
              case 'center':
                x = specs.position.x;
                break;

              case 'right':
                if (specs.textVAlign === extension.alignment.v && specs.textVAlign === 'center' && specs.textHAlign === extension.alignment.h) {
                    x = specs.position.x - specs.w / 2 + specs.bboxLabels.w + decBBoxLabels.w / 2
                } else if (specs.textVAlign === extension.alignment.v &&
                          (specs.textVAlign !== 'center' || specs.textHAlign === extension.alignment.h || specs.textHAlign === 'center')) {
                    x = specs.position.x + specs.bboxLabels.w / 2 + decBBoxLabels.w / 2
                } else {
                    x = specs.position.x + specs.w / 2 + decBBox.w / 2;
                }
                x += padding
                break;

              case 'left':
              default:
                if (specs.textVAlign === extension.alignment.v && specs.textVAlign === 'center' && specs.textHAlign === extension.alignment.h) {
                    x = specs.position.x + specs.w / 2 - specs.bboxLabels.w - decBBoxLabels.w / 2
                } else if (specs.textVAlign === extension.alignment.v &&
                    (specs.textVAlign !== 'center' || specs.textHAlign === extension.alignment.h || specs.textHAlign === 'center')) {
                    x = specs.position.x - specs.bboxLabels.w / 2 - decBBoxLabels.w / 2
                } else {
                    x = specs.position.x - specs.w / 2 - decBBox.w / 2;
                }
                x -= padding
            }
            switch (extension.alignment.v) {
              case 'center':
                y = specs.position.y;
                break;

              case 'bottom':
                if (specs.textVAlign === extension.alignment.v && extension.alignment.h === 'center') {
                    y = specs.position.y - specs.h / 2 + specs.bboxLabels.h + decBBoxLabels.h / 2 + padding;
                } else if (specs.textVAlign === extension.alignment.v) {
                    y = specs.position.y + specs.h / 2 + padding + (specs.bboxLabels.h - specs.h - padding) / 2;
                } else {
                    y = specs.position.y + specs.h / 2 + decBBoxLabels.h / 2 + padding;
                }
                break;

              case 'top':
              default:
                if (specs.textVAlign === extension.alignment.v && extension.alignment.h === 'center') {
                    y = specs.position.y + specs.h / 2 - specs.bboxLabels.h - decBBoxLabels.h / 2 - padding;
                } else if (specs.textVAlign === extension.alignment.v) {
                    y = specs.position.y - specs.h / 2 - padding - (specs.bboxLabels.h - specs.h - padding) / 2;
                } else {
                    y = specs.position.y - specs.h / 2 - decBBoxLabels.h / 2 - padding
                }
            }

            return { x: x, y: y };
        }
        throw new Error('Alignment required', extension);
    }

})
