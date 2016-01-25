define([
    'flight/lib/component',
    'flight/lib/registry',
    'util/vertex/formatters',
    'util/promise',
    'util/requirejs/promise!util/service/ontologyPromise',
    'configuration/plugins/registry',
    './layoutComponents',
    './layoutTypes'
], function(
    defineComponent,
    flightRegistry,
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
            var self = this;

            this.on('updateModel', this.onUpdateModel);

            this.renderRoot(this.attr.model)
                .catch(function(error) {
                    self.trigger('errorLoadingTypeContent');
                    console.error(error);
                })
                .then(function() {
                    self.trigger('finishedLoadingTypeContent');
                    if (!_.isEmpty(self.attr.focus)) {
                        self.$node.find('.org-visallo-texts').trigger('focusOnSnippet', self.attr.focus);
                    }
                })
        });

        this.renderRoot = function(model) {
            var root = findLayoutComponentsMatching({
                    identifier: 'org.visallo.layout.root',
                    model: model,
                    rootModel: model
                }),
                preparedModel = prepareModel(model),
                deferredAttachments = [];

            return renderLayoutComponent(this.node, preparedModel, root, undefined, model, deferredAttachments)
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
        };

        this.onUpdateModel = function(event, data) {
            var self = this;
            event.stopPropagation();
            if (event.target === this.node) {
                this.renderRoot(data.model).done(function() {
                    self.trigger('modelUpdated');
                })
            }
        };

    }

    function prepareModel(model) {
        if (_.isArray(model) && model.length === 1) {
            return _.first(model);
        }
        return model
    }

    function zipChildrenWithModels(layoutComponent, model) {
        if ('collectionItem' in layoutComponent) {
            if (!_.isArray(model)) {
                console.error('LayoutComponent', layoutComponent, 'model', model);
                throw new Error('Collection layout item is not passed an array');
            }

            return model.map(function(modelItem, i) {
                return {
                    child: layoutComponent.collectionItem,
                    modelTransform: function(model) {
                        return model[i];
                    },
                    model: model
                }
            });
        }
        return layoutComponent.children.map(function(child) {
            var transform;
            if (_.isFunction(child.model)) {
                transform = child.model
            } else if ('model' in child) {
                transform = function() {
                    return child.model
                }
            }
            return {
                child: child,
                modelTransform: transform,
                model: model
            }
        });
    }

    function teardownOldComponents(el, options) {
        var optionallyExcludingComponent = options && options.excluding,
            type = options && options.type,
            instanceInfos = flightRegistry.findInstanceInfoByNode(el);

        if (type !== 'layout' && type !== 'component') {
            throw new Error('type must be layout or component')
        }

        if (instanceInfos) {
            instanceInfos.forEach(function(info) {
                var key = 'teardownWhenNot' + type.substring(0, 1).toUpperCase() + type.substring(1),
                    componentToTeardown = info.instance[key];

                if (componentToTeardown && (!optionallyExcludingComponent || componentToTeardown !== optionallyExcludingComponent)) {
                    info.instance.teardown()
                }
            })
        }
    }

    function renderLayoutComponent(rootEl, model, layoutComponent, config, rootModel, deferredAttachments) {
        if (_.isFunction(layoutComponent.render)) {
            updateClasses(rootEl, layoutComponent.identifier, layoutComponent.className, config.className)
            return Promise.resolve(layoutComponent.render(rootEl, model, config, layoutComponent));
        }

        var zipped = zipChildrenWithModels(layoutComponent, model);
        while (rootEl.childElementCount > zipped.length) {
            $(rootEl.children[rootEl.childElementCount - 1]).teardownAllComponents().remove();
        }

        return Promise.all(
            zipped.map(function(childAndModel, i) {
                var child = childAndModel.child,
                    modelTransform = childAndModel.modelTransform || _.identity,
                    $el = i < rootEl.childElementCount ? $(rootEl.children[i]) : $('<div>'),
                    el = $el.get(0);

                updateClasses(el, child.ref, child.className)

                return Promise.resolve(modelTransform(model))
                    .then(function(model) {
                        if (_.isUndefined(model)) {
                            throw new Error('No model specified')
                        }
                        if ('ref' in child) {
                            teardownOldComponents(el, { type: 'component' })
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
                        return el;
                    })
                    .catch(function(error) {
                        console.error(error);
                    })
            }))
            .then(function(elements) {
                updateClasses(rootEl, layoutComponent.identifier, layoutComponent.className, config && config.className)
                return Promise.resolve(
                    initializeLayout(rootEl, elements, layoutComponent)
                );
            })
            .then(function() {
                deferredAttachments.push(attachComponent(layoutComponent, rootEl, model))
            })
    }

    function initializeLayout(el, domElements, layoutComponent) {
        var layoutConfig = layoutComponent.layout;
        if (!layoutConfig) {
            teardownOldComponents(el, { type: 'layout' });

            while (el.childElementCount < domElements.length) {
                var child = domElements[el.childElementCount]
                el.appendChild(child)
            }
            while (el.childElementCount > domElements.length) {
                el.removeChild(el.children[el.childElementCount - 1])
            }
            return
        }
        if (!layoutConfig.type) throw new Error('No layout type parameter specified');
        var type = _.findWhere(registry.extensionsForPoint('org.visallo.layout.type'), { type: layoutConfig.type });
        if (!type) throw new Error('No registered layout type for: ' + layoutConfig.type);

        return Promise.require(type.componentPath)
            .then(function(LayoutType) {
                var children = domElements.map(function(el, i) {
                    return {
                        element: el,
                        configuration: layoutComponent.collectionItem || layoutComponent.children[i]
                    }
                });

                teardownOldComponents(el, { type: 'layout', excluding: LayoutType });

                if ($(el).lookupComponent(LayoutType)) {
                    $(el).trigger('updateLayout', {
                        children: children
                    });
                } else {
                    LayoutType.attachTo(el, {
                        layoutConfig: layoutConfig.options || {},
                        children: children
                    });

                    flightInstanceInfo(el, LayoutType).instance.teardownWhenNotLayout = LayoutType
                }
            });
    }

    function attachComponent(config, el, model) {
        if (!config.componentPath) {
            return;
        }
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

                    teardownOldComponents(el, { type: 'component', excluding: Component })

                    var instanceInfos = flightRegistry.findInstanceInfoByNode(el),
                        componentFilter = function(info) {
                            return info.instance.constructor === Component;
                        },
                        eventFilter = function(eventInfo) {
                            return eventInfo.type === 'updateModel';
                        }

                    if (instanceInfos && instanceInfos.length && _.filter(instanceInfos, componentFilter).length) {
                        triggerUpdateModelAndStopPropagation(el, model);
                    } else {
                        Component.attachTo(el, attrs);
                    }

                    var instanceInfo = flightInstanceInfo(el, Component),
                        suppressWarning = instanceInfo &&
                            instanceInfo.instance.attr.ignoreUpdateModelNotImplemented === true;

                    if (instanceInfo) {
                        instanceInfo.instance.teardownWhenNotComponent = Component
                        if (!_.some(instanceInfo.events, eventFilter) && !suppressWarning) {

                            console.warn(
                                'Component added to layout doesn\'t respond to "updateModel". ' +
                                'Suppress warning with ' +
                                '"ignoreUpdateModelNotImplemented" attribute',
                                Component,
                                el
                            );
                        }
                    }

                });
        }
    }

    function updateClasses(el) {
        var classes = _.chain(arguments)
            .rest()
            .compact()
            .map(function(cls) {
                if (cls.indexOf(' ') >= 0) {
                    throw new Error('Spaces not allowed in classes')
                }
                if (cls.indexOf('.') >= 0) {
                    return packageNameToCssClass(cls);
                }
                return cls;
            })
            .value()

        removePreviousClasses(el);
        modifyClassesOnElement(el, 'add', classes);

        function packageNameToCssClass(packageName) {
            if (packageName) {
                return packageName.replace(/[.]/g, '-').replace(/[^a-zA-Z\-]/g, '').toLowerCase();
            }
        }
        function removePreviousClasses(el) {
            var toRemove = [];
            for (var i = 0; i < el.classList.length; i++) {
                var existing = el.classList.item(i),
                    index = classes.indexOf(existing);
                if (index === -1) {
                    // Special case, should be updated
                    if (existing !== 'type-content' ||
                        !_.contains(classes, 'org-visallo-layout-root')) {
                        toRemove.push(existing)
                    }
                } else {
                    classes.splice(index, 1)
                }
            }
            modifyClassesOnElement(el, 'remove', toRemove);
        }
        function modifyClassesOnElement(el, action, classes) {
            if (el && action && classes.length) {
                classes.forEach(function(cls) {
                    if (action === 'add' && !el.classList.contains(cls)) {
                        el.classList.add(cls);
                    } else if (action === 'remove' && el.classList.contains(cls)) {
                        el.classList.remove(cls);
                    }
                })
            }
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

        return selected;
    }

    function layoutComponentAppliesToModel(component, model, rootModel) {
        if ('applyTo' in component) {
            if (_.isFunction(component.applyTo)) {
                return Boolean(component.applyTo(model));
            }
            if (component.applyTo) {
                var applyTo = component.applyTo,
                    useModel = _.isUndefined(model) ? rootModel : model,
                    validApplyTos = ['type', 'displayType', 'conceptIri', 'edgeLabel'],
                    applyToKeys = _.keys(applyTo),
                    applyToIsValid = applyToKeys.length === 1 &&
                        _.contains(validApplyTos, applyToKeys[0] || '');

                if (!applyToIsValid) {
                    console.error(applyTo)
                    throw new Error('Invalid applyTo: Must contain 1 key: ' + validApplyTos.join(', '))
                }

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

    function triggerUpdateModelAndStopPropagation(el, model) {
        // Prevent infinite loop
        if (el.classList.contains('org-visallo-layout-root')) {
            return;
        }
        $(el)
            .off('updateModel', elementUpdateModelHandler)
            .on('updateModel', elementUpdateModelHandler)
            .trigger('updateModel', { model: model })
    }

    function elementUpdateModelHandler(event) {
        event.stopPropagation();
    }

    function flightInstanceInfo(el, Component) {
        var instanceInfos = flightRegistry.findInstanceInfoByNode(el);
        if (instanceInfos) {
            return _.find(instanceInfos, function(info) {
                return info.instance.constructor === Component;
            })
        }
    }

});
