define([
    'flight/lib/component',
    'util/vertex/formatters',
    'util/promise',
    'util/requirejs/promise!util/service/ontologyPromise',
    'configuration/plugins/registry',
    './layoutComponents',
    './layoutTypes'
], function(
    defineComponent,
    F,
    Promise,
    ontology,
    registry,
    layoutComponents,
    layoutTypes) {
    'use strict';

    registry.documentExtensionPoint('org.visallo.layout.component', 'Layout Component', function(e) {
        if (!_.isFunction(e.applyTo) && _.isObject(e.applyTo)) {
            return e.applyTo.type || e.applyTo.conceptIri || e.applyTo.edgeLabel || e.applyTo.displayType;
        }
        return _.isArray(e.children) || _.isFunction(e.render) || _.isObject(e.collectionItem);
    })
    registry.documentExtensionPoint('org.visallo.layout.type', 'Layout Type', function(e) {
        return _.isString(e.type) && _.isString(e.componentPath);
    })

    return defineComponent(Item);

    function Item() {

        this.attributes({
            model: null,
            focus: {}
        })

        this.after('teardown', function() {
            this.$node.empty();
        })

        this.after('initialize', function() {
            this.on('updateModel', this.onUpdateModel);

            var self = this,
                root = findLayoutComponentsMatching({
                    identifier: 'org.visallo.layout.root',
                    model: this.attr.model,
                    rootModel: this.attr.model
                }),
                model = prepareModel(this.attr.model),
                deferredAttachments = [];

            renderLayoutComponent(this.node, model, root, undefined, model, deferredAttachments)
                .then(function() {
                    return Promise.all(
                        deferredAttachments.map(function(f) {
                            if (f) {
                                return f()
                            }
                            return Promise.resolve();
                        })
                    )
                })
                .then(function() {
                    self.trigger('finishedLoadingTypeContent');
                    if (!_.isEmpty(self.attr.focus)) {
                        self.$node.find('.org-visallo-texts').trigger('focusOnSnippet', self.attr.focus);
                    }
                })
                .done();
        });

        this.onUpdateModel = function(event, data) {
            this.$node.children().trigger('modelUpdated', data);
        };

    }

    function prepareModel(model) {
        if (_.isArray(model) && model.length === 1) {
            return _.first(model);
        }
        return model
    }

    function synchronizeLayoutChildren(el, model, layoutComponent, config, rootModel, deferredAttachments) {
        var childNodeIndex = 0;
        return layoutComponent.children.reduce(function(promise, child) {
            return promise.then(function() {
                var childNode = el.childNodes[childNodeIndex++],
                    $childNode = $(childNode);
                    if ('componentPath' in child) {
                        return Promise.require(child.componentPath)
                        .then(function(Component) {
                            var index = -1,
                                existingNode = _.find(el.childNodes, function(el, i) {
                                    var componentIsAttached = Boolean($(el).lookupComponent(Component));
                                    if (componentIsAttached) {
                                        index = i;
                                    }
                                    return componentIsAttached;
                                });

                                if (existingNode) {
                                    if (existingNode !== childNode) {
                                        $(existingNode).insertBefore(childNode);
                                    }
                                } else {
                                    deferredAttachments.push(attachComponent(child, $('<div>').insertBefore($childNode), model))
                                }
                        });
                    } else if ('ref' in child) {
                        return renderLayoutComponent(el, model, layoutComponent, child, rootModel, deferredAttachments)
                    }
            })
        }, Promise.resolve())
        .then(function() {
            while (el.childNodes.length > layoutComponent.children.length) {
                $(el.childNodes[el.childNodes.length - 1]).teardownAllComponents().remove();
            }
        });
    }

    function renderLayoutComponent(el, model, layoutComponent, config, rootModel, deferredAttachments) {
        if (_.isFunction(layoutComponent.render)) {
            return Promise.resolve(layoutComponent.render(el, model, config, layoutComponent));
        }

        $(el).empty();

        var childrenAndModel;

        if ('collectionItem' in layoutComponent) {
            if (!_.isArray(model)) {
                console.error('LayoutComponent', layoutComponent, 'model', model);
                throw new Error('Collection layout item is not passed an array');
            }

            childrenAndModel = model.map(function(modelItem) {
                return {
                    child: layoutComponent.collectionItem,
                    model: modelItem
                }
            });
        } else {
            childrenAndModel = layoutComponent.children.map(function(child) {
                return {
                    child: child,
                    model: model
                }
            })
        }

        return Promise.all(childrenAndModel.map(function(childAndModel) {
                var child = childAndModel.child,
                    model = childAndModel.model,
                    $el = $('<div>'),
                    el = $el.get(0),
                    cls = _.compact([
                            child.className,
                            packageNameToCssClass(child.ref)
                        ]).forEach(function(cls) {
                            addClasesToElement(cls, el);
                        });
                return (_.isFunction(child.model) ?
                    Promise.resolve(child.model(model)) :
                    Promise.resolve(child.model || model)
                ).then(function(model) {
                    if (_.isUndefined(model)) {
                        throw new Error('No model specified')
                    }
                    if ('ref' in child) {
                        var childComponent = findLayoutComponentsMatching({
                            identifier: child.ref,
                            model: model,
                            rootModel: rootModel
                        });
                        return renderLayoutComponent(el, model, childComponent, child, rootModel, deferredAttachments)
                    }
                    if ('componentPath' in child) {
                        deferredAttachments.push(attachComponent(child, el, model))
                        return;
                    }
                    $el.html(document.createComment('Unable to Render: ' + JSON.stringify(child)));
                })
                .then(function() {
                    $(el).off('modelUpdated').on('modelUpdated', function(event, data) {
                        if (event.target !== el &&
                            $(event.target).parentsUntil(el)) return;

                        $(this).children().trigger(event.type, data);

                        if ('ref' in child) {
                            var childComponent = findLayoutComponentsMatching({
                                identifier: child.ref,
                                model: data.model,
                                rootModel: rootModel
                            });
                            if (childComponent) {
                                (_.isFunction(child.model) ?
                                        Promise.resolve(child.model(data.model)) :
                                        Promise.resolve(child.model || data.model)
                                ).then(function(model) {
                                    var modelUpdatedDeferredAttachments = [],
                                        args = [el, model, childComponent, child, rootModel, modelUpdatedDeferredAttachments];
                                    if (_.isFunction(childComponent.render)) {
                                        return renderLayoutComponent.apply(undefined, args);
                                    } else if (childComponent.children) {
                                        return synchronizeLayoutChildren.apply(undefined, args)
                                            .then(function() {
                                                modelUpdatedDeferredAttachments.forEach(function(f) {
                                                    if (f) { f() }
                                                })
                                            })
                                    }
                                });
                            }
                        }
                    });
                    return el;
                })
                .catch(function(error) {
                    console.error(error);
                })
            }))
            .then(function(elements) {
                var cls = _.compact([
                        layoutComponent.className,
                        packageNameToCssClass(layoutComponent.identifier)
                    ]).forEach(function(cls) {
                        addClasesToElement(cls, el);
                    })

                return Promise.resolve(
                    initializeLayout(layoutComponent.layout, el, elements, layoutComponent.children)
                );
            })
            .then(function() {
                deferredAttachments.push(attachComponent(layoutComponent, el, model))
            })
    }

    function initializeLayout(layoutConfig, el, domElements, childrenConfig) {
        if (!layoutConfig) {
            return $(el).html(domElements)
        }
        if (!layoutConfig.type) throw new Error('No layout type parameter specified');
        var type = _.findWhere(registry.extensionsForPoint('org.visallo.layout.type'), { type: layoutConfig.type });
        if (!type) throw new Error('No registered layout type for: ' + layoutConfig.type);

        return Promise.require(type.componentPath)
            .then(function(LayoutType) {
                LayoutType.attachTo(el, {
                    layoutConfig: layoutConfig.options || {},
                    children: domElements.map(function(el, i) {
                        return {
                            element: el,
                            configuration: childrenConfig[i]
                        }
                    })
                });
            });
    }

    function attachComponent(config, el, model) {
        if (!config.componentPath) return;
        return function() {
            return Promise.require(config.componentPath)
                .then(function(Component) {
                    var attrs = {};
                    if (config.modelAttribute) {
                        attrs[config.modelAttribute] = model
                    } else if (_.isFunction(config.attributes)) {
                        attrs = config.attributes(model);
                    } else if (config.attributes) {
                        attrs = config.attributes;
                    } else {
                        attrs.model = model;
                    }

                    $(el).teardownComponent(Component);
                    Component.attachTo(el, attrs);
                });
        }
    }

    function packageNameToCssClass(packageName) {
        if (packageName) {
            return packageName.replace(/[.]/g, '-').replace(/[^a-zA-Z\-]/g, '').toLowerCase();
        }
    }

    function findLayoutComponentsMatching(match) {
        if (!match) {
            throw new Error('Match object is required');
        }
        if (!_.isString(match.identifier) || !match.identifier.length) {
            throw new Error('Match identifier required');
        }
        if (!match.rootModel) {
            throw new Error('Match rootModel is required');
        }
        if (!match.model) {
            throw new Error('Match model is required');
        }

        var components = registry.extensionsForPoint('org.visallo.layout.component'),
            possible = _.filter(components, function(comp) {
                if (comp.identifier === match.identifier) {
                    return layoutComponentAppliesToModel(comp, match.model, match.rootModel);
                }
            }),
            sorted = _.chain(possible)
                // Secondary sort concept hierarchy
                .sortBy(function(comp) {
                    var applyTo = comp.applyTo;
                    if (applyTo && applyTo.conceptIri) {
                        var concept = ontology.concepts.byId[applyTo.conceptIri];
                        if (concept) return concept.ancestors.length;
                    }
                    return 0;
                })
                // Primary sort by applyTo
                .sortBy(function(comp) {
                    var applyTo = comp.applyTo;

                    if (_.isFunction(applyTo)) return -1;
                    if (applyTo) {
                        if (applyTo.conceptIri) return 0;
                        if (applyTo.edgeLabel) return 0;
                        if (applyTo.displayType) return 1;
                        if (applyTo.type === 'vertex') return 2;
                        if (applyTo.type === 'edge') return 2;
                        if (applyTo.type === 'element') return 3;
                    }
                    return Number.MAX_VALUE;
                })
                .value(),
            selected = _.first(sorted);

        if (!selected) {
            console.error(match)
            throw new Error('No layout component available for');
        }

        //if (sorted.length > 1) {
            //console.debug('Found ' + sorted.length + ' layout components:', sorted);
        //}

        return selected;
    }

    function layoutComponentAppliesToModel(component, model, rootModel) {
        if ('applyTo' in component) {
            if (_.isFunction(component.applyTo)) {
                return Boolean(component.applyTo(model));
            }
            if (component.applyTo) {
                var applyTo = component.applyTo,
                    useModel = _.isUndefined(model) ? rootModel : model;

                if (useModel) {
                    var multiple = _.isArray(useModel) && useModel.length > 1,
                        single = !_.isArray(useModel) || useModel.length === 1;

                    if (multiple) {
                        if (applyTo.type === 'element[]' && _.every(multiple, function(e) {
                            return _.isObject(e) && 'id' in e;
                        })) {
                            return true;
                        }
                    } else {
                        useModel = _.isArray(useModel) ? _.first(useModel) : useModel;
                        if (_.isObject(useModel) && 'id' in useModel && useModel.properties) {
                            var isEdge = F.vertex.isEdge(useModel),
                                isVertex = !isEdge;

                            if (applyTo.type === 'element') return true;
                            if (isVertex) {
                                if (applyTo.conceptIri || applyTo.displayType) {
                                    var conceptIriProperty = _.findWhere(useModel.properties, { name: 'http://visallo.org#conceptType'}),
                                        conceptIri = conceptIriProperty && conceptIriProperty.value || 'http://www.w3.org/2002/07/owl#Thing',
                                        concept;

                                    while (conceptIri) {
                                        if (applyTo.conceptIri && conceptIri === applyTo.conceptIri) {
                                            return true;
                                        }
                                        concept = ontology.concepts.byId[conceptIri];
                                        if (applyTo.displayType && concept && concept.displayType === applyTo.displayType) {
                                            return true;
                                        }
                                        conceptIri = concept && concept.parentConcept;
                                    }
                                } else if (applyTo.type === 'vertex') return true;
                            } else {
                                if (component.applyTo.edgeLabel && component.applyTo.edgeLabel === useModel.label) return true;
                                if (component.applyTo.type === 'edge') return true;
                            }
                        }
                    }

                    return false;
                }
                console.error(component, useModel);
                throw new Error('Unexpected model or applyTo object');
            }
            console.warn('Unexpected applyTo value:', component.applyTo, component)
            return false;
        }
        return true;
    }

    function addClasesToElement(classes, element) {
        var split = _.isString(classes) ? _.compact(classes.split(/\s+/)) : classes;
        split.forEach(function(cls) {
            element.classList.add(cls);
        });
    }
});
