define([
    'flight/lib/component',
    'flight/lib/registry',
    'util/domElement',
    'util/vertex/formatters',
    'util/promise',
    'util/requirejs/promise!util/service/ontologyPromise',
    'configuration/plugins/registry',
    './layoutComponents',
    './layoutTypes'
], function(
    defineComponent,
    flightRegistry,
    domElementUtil,
    F,
    Promise,
    ontology,
    registry,
    layoutComponents,
    layoutTypes) {
    'use strict';

    /**
     * The detail pane is rendered using a custom layout engine consisting of a tree of layout components.
     *
     * *Layout Components* are nodes in the layout tree that define the type of layout
     * and what children are included in the layout.
     *
     * The `children` defined in a layout component can be references (`ref`) to other
     * components, or FlightJS components specified with a `componentPath`. Layout
     * components can also specify a flight component to be attached to the node for implementing behavior.
     *
     * _Exactly one of the following is required: `render`, `collectionItem` or `children` (i.e., you cannot supply both render and collectionItem)._
     *
     * @param {org.visallo.layout.component~applyTo|org.visallo.layout.component~applyToFn} [applyTo] When does this component get used
     * @param {string} identifier Identifier of this component for use in other components in package syntax. Also transforms into css class – replacing package periods with dashes
     * @param {object} [layout]
     * @param {string} [layout.type] Which {@link org.visallo.layout.type|layout type} to render `children`.
     * @param {object} [layout.options] Layout-specific options
     * @param {string} [componentPath] Additional FlightJS component to attach to this node for behavior
     * @param {string} [className] Additional css classname to add to DOM
     * @param {function} [render] Function that renders the content, is passed the `model`, and `match` configuration.
     * @param {org.visallo.layout.component~child} [collectionItem] Reference to layout component to render for each item in model (requires model to be array).
     * @param {array.<org.visallo.layout.component~child>} [children] Children items to render
     */
    registry.documentExtensionPoint('org.visallo.layout.component',
        'Define the layout of the Element Inspector based on content',
        function(e) {
            if (!_.isFunction(e.applyTo) && _.isObject(e.applyTo)) {
                var optionalApplyToIsValid = null;
                if (e.applyTo.constraints) {
                    optionalApplyToIsValid = _.isArray(e.applyTo.constraints) && e.applyTo.constraints.length;
                }
                if (e.applyTo.contexts) {
                    optionalApplyToIsValid = optionalApplyToIsValid && _.isArray(e.applyTo.contexts) && e.applyTo.contexts.length;
                }
                if (optionalApplyToIsValid !== null) {
                    return optionalApplyToIsValid;
                }
                return (e.applyTo.type || e.applyTo.conceptIri || e.applyTo.edgeLabel || e.applyTo.displayType);
        }
        return _.isArray(e.children) || _.isFunction(e.render) || _.isObject(e.collectionItem);
    }, 'http://docs.visallo.org/extension-points/front-end/layout/component.html')

    /**
     * Visallo includes the [Flex](https://github.com/visallo/visallo/blob/master/web/war/src/main/webapp/js/detail/item/types/flex.js) layout type.
     *
     * Layout components are passed properties: `layoutConfig` and `children`
     *
     * @param {string} type The identifier for this layout type (used by layout components)
     * @param {string} componentPath
     */
    registry.documentExtensionPoint('org.visallo.layout.type',
        'Handles the layout of children within a component',
        function(e) {
            return _.isString(e.type) && _.isString(e.componentPath);
        }
    );

    return defineComponent(Item);

    function Item() {

        this.attributes({
            model: null,
            constraints: [],
            context: '',
            focus: {}
        })

        this.after('teardown', function() {
            this.$node.empty();
        })

        this.after('initialize', function() {
            var self = this;

            this.on('updateModel', this.onUpdateModel);
            this.on('updateConstraints', this.onUpdateConstraints);
            this.on('updateContext', this.onUpdateContext);

            this.model = this.attr.model;
            this.constraints = this.attr.constraints;
            this.context = this.attr.context;

            this.renderRoot()
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

        this.renderRoot = function() {
            var rootLayoutComponent = findLayoutComponentsMatching({
                    identifier: 'org.visallo.layout.root',
                    model: this.model,
                    rootModel: this.model,
                    constraints: this.constraints,
                    context: this.context
                }),
                deferredAttachments = [];

            return renderLayoutComponent(rootLayoutComponent, this.node, prepareModel(this.model), this.model, {
                deferredAttachments: deferredAttachments,
                constraints: this.constraints,
                context: this.context
            }).then(attachDeferredComponents(deferredAttachments))
        };

        this.onUpdateModel = function(event, data) {
            var self = this;
            event.stopPropagation();
            if (event.target === this.node) {
                this.model = data.model;
                this.renderRoot(this.model, this.constraints).then(function() {
                    self.trigger('modelUpdated');
                })
            }
        };

        this.onUpdateConstraints = function(event, data) {
            var self = this;
            event.stopPropagation();
            if (event.target === this.node) {
                this.constraints = data.constraints;
                this.renderRoot().then(function() {
                    self.trigger('constraintsUpdated');
                })
            }
        };

        this.onUpdateContext = function(event, data) {
            var self = this;
            event.stopPropagation();
            if (event.target === this.node) {
                this.context = data.context;
                this.renderRoot().then(function() {
                    self.trigger('contextUpdated');
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
        /**
         * _Exactly one of the following is required: `ref`, or `componentPath`._
         *
         * @typedef org.visallo.layout.component~child
         * @property {object} [style] CSS attributes to set on DOM
         * @property {string} [modelAttribute] Use this attribute name instead of `model` when attaching FlightJS component.
         * @property {function} [attributes] Transform attributes using function when attaching FlightJS component.
         * @property {function|object} [model] Change the model passed to this child, either a function or object.
         * * `function`: Transforms the state of the model at this level in the tree to the child. Return either the transformed model or a `Promise`.
         * * `object`: Static object to pass as the model.
         * @property {string} [ref] Reference to identifier of layout component to render.
         * @property {string} [componentPath] RequireJS path to FlightJS component to render.
         */
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
                var key = '__layoutTeardownWhenNot' + type.substring(0, 1).toUpperCase() + type.substring(1),
                    componentToTeardown = info.instance[key],
                    matchesType = info.instance.__layoutType === type;

                if (componentToTeardown && matchesType && (!optionallyExcludingComponent || componentToTeardown !== optionallyExcludingComponent)) {
                    info.instance.teardown()
                }
            })
        }
    }

    function renderLayoutComponent(layoutComponent, node, model, rootModel, opts) {
        var options = opts || {},
            config = options.config,
            constraints = aggregateConstraints(options.constraints, config),
            context = options.context,
            deferredAttachments = options.deferredAttachments,
            layoutComponentRender = layoutComponent && layoutComponent.render,
            configRender = config && config.render;

        if (_.isFunction(layoutComponentRender || configRender)) {
            updateClasses(node,
                layoutComponent && layoutComponent.identifier,
                layoutComponent && layoutComponent.className,
                config && config.className)
            return Promise.resolve((layoutComponentRender || configRender)(node, model, config, layoutComponent));
        }

        var zipped = zipChildrenWithModels(layoutComponent, model);

        return Promise.all(
            zipped.map(function(childAndModel, i) {
                var child = childAndModel.child,
                    modelTransform = childAndModel.modelTransform || _.identity,
                    el, $el;

                if (i < node.childElementCount) {
                    if ((/^DIV$/i).test(node.children[i].tagName) && !(
                        node.children[i].__isLayoutComponent && child.componentPath
                    )) {
                        $el = $(node.children[i]);
                        $el.contents().filter(function() {
                            return this.nodeType === Node.TEXT_NODE;
                        }).remove();
                    } else {
                        el = document.createElement('div');
                        node.insertBefore(el, node.children[i]);
                        node.removeChild(node.children[i + 1])
                        $el = $(el);
                    }
                } else {
                    $el = $('<div>');
                }
                el = $el.get(0);
                el.__isLayoutComponent = true;
                updateClasses(el, child.ref, child.className)

                return Promise.resolve(modelTransform(model))
                    .then(function(model) {
                        if (_.isUndefined(model)) {
                            throw new Error('No model specified')
                        }
                        if ('ref' in child) {
                            teardownOldComponents(el, { type: 'component' })
                            var aggregatedConstraints = aggregateConstraints(constraints, child),
                                childComponent = findLayoutComponentsMatching({
                                    identifier: child.ref,
                                    model: model,
                                    rootModel: rootModel,
                                    constraints: aggregatedConstraints,
                                    context: context
                                });
                            return renderLayoutComponent(childComponent, el, model, rootModel, {
                                config: child,
                                constraints: aggregatedConstraints,
                                context: context,
                                deferredAttachments: deferredAttachments
                            });
                        }
                        if ('componentPath' in child) {
                            deferredAttachments.push(attachComponent(child, el, model))
                            return;
                        }
                        if (_.isFunction(child.render)) {
                            return renderLayoutComponent(null, el, model, rootModel, {
                                config: child,
                                constraints: aggregatedConstraints,
                                context: context,
                                deferredAttachments: deferredAttachments
                            })
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
                updateClasses(node, layoutComponent.identifier, layoutComponent.className, config && config.className)
                return Promise.resolve(
                    initializeLayout(node, elements, layoutComponent)
                );
            })
            .then(function() {
                deferredAttachments.push(attachComponent(layoutComponent, node, model))
            })
    }

    function aggregateConstraints(parentConstraints, configuration) {
        if (configuration && configuration.constraints) {
            return _.unique((parentConstraints || []).concat(configuration.constraints));
        }
        return parentConstraints;
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
                        children: children,
                        layoutConfig: layoutConfig.options || {}
                    });
                } else {
                    LayoutType.attachTo(el, {
                        layoutConfig: layoutConfig.options || {},
                        children: children
                    });
                    var instance = flightInstanceInfo(el, LayoutType).instance
                    instance.__layoutType = 'layout';
                    instance.__layoutTeardownWhenNotLayout = LayoutType
                }
            });
    }

    function attachDeferredComponents(list) {
        return function() {
            return Promise.all(
                list.map(function(f) {
                    if (_.isFunction(f)) {
                        return Promise.resolve(f())
                    } else if (f) {
                        console.warn('Deferred attachment not a function?', f);
                    }
                    return Promise.resolve();
                })
            )
        }
    }

    function attachComponent(config, el, model) {
        if (!config.componentPath) {
            return;
        }
        if (el.__isLayoutComponent) {
            domElementUtil.removeAllStyles(el);
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
                        if (el.__isLayoutComponent && !config.identifier) {
                            el.textContent = '';
                        }
                        Component.attachTo(el, attrs);
                    }
                    if (el.__isLayoutComponent) {
                        delete el.__isLayoutComponent;
                    }

                    var instanceInfo = flightInstanceInfo(el, Component),
                        suppressWarning = instanceInfo &&
                            instanceInfo.instance.attr.ignoreUpdateModelNotImplemented === true;

                    if (instanceInfo) {
                        instanceInfo.instance.__layoutTeardownWhenNotComponent = Component
                        instanceInfo.instance.__layoutType = 'component';
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
        if (_.isUndefined(match.model)) {
            throw new Error('Match model is required');
        }
        if (!match.constraints) {
            match.constraints = [];
        }
        if (match.context && !_.isString(match.context)) {
            throw new Error('Match context must be string: ' + match.context);
        }
        if (!_.isArray(match.constraints)) {
            throw new Error('Contraints must be array: ' + match.constraints);
        }

        /**
         * _Only one of these allowed: conceptIri, edgeLabel, displayType, type*_
         *
         * @typedef org.visallo.layout.component~applyTo
         * @property {string} [conceptIri] Implies vertices, only those whose concept (or ancestor) matches this Iri.
         * @property {string} [edgeLabel] Implies edges, only those whose edgeLabel matches this Iri.
         * @property {string} [displayType] Match ontological `displayType` option.
         * @property {string} [type] Possible values: `vertex`, `edge`, `element`, `element[]`.
         * @property {array.<string>} [constraints] Possible values: `width`, `height`.
         * Match based on the view container, instead of matching model data.
         * Constraints control the layout selection for views that are width, and/or height constrained.
         * The detail pane is set to be width constrained, whereas the full screen view has no constraints.
         * @property {array.<string>} [contexts] For named templates. `popup` is defined by default for graph previews
         */

        /**
         * @callback org.visallo.layout.component~applyToFn
         * @param {object} model
         * @param {object} match
         * @param {array.<string>} match.contexts
         * @param {array.<string>} match.constraints
         * @returns {boolean} If this component should apply given the `model` and `match`
         */

        var components = registry.extensionsForPoint('org.visallo.layout.component'),
            possible = _.filter(components, function(comp) {
                if (comp.identifier === match.identifier) {
                    return layoutComponentAppliesToModel(comp, match.model, match.rootModel, match) &&
                        layoutComponentMatchesConstraints(comp, match.constraints) &&
                        layoutComponentMatchesContext(comp, match.context);
                }
            }),
            sorted = _.chain(possible)
                // sort concept hierarchy
                .sortBy(function(comp) {
                    var applyTo = comp.applyTo;
                    if (applyTo && applyTo.conceptIri) {
                        var concept = ontology.concepts.byId[applyTo.conceptIri];
                        if (concept) return concept.ancestors.length;
                    }
                    return 0;
                })
                // sort by applyTo
                .sortBy(function(comp) {
                    var applyTo = comp.applyTo,
                        order = -1;

                    if (_.isFunction(applyTo)) return order;
                    if (applyTo) {

                        if (applyTo.conceptIri) return order + 1;
                        if (applyTo.edgeLabel) return order + 1;

                        if (applyTo.displayType) return order + 2;

                        if (applyTo.type === 'vertex') return order + 3;
                        if (applyTo.type === 'edge') return order + 3;

                        if (applyTo.type === 'element') return order + 4;
                    }
                    return Number.MAX_VALUE;
                })
                // sort by applyTo constraints/contexts
                .sortBy(function(comp) {
                    var applyTo = comp.applyTo,
                        order = -1;

                    if (_.isFunction(applyTo)) return order;
                    if (applyTo) {

                        if (applyTo.contexts && applyTo.constraints) return order + 1;
                        if (applyTo.contexts) return order + 2;
                        if (applyTo.constraints) return order + 3;
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

    function layoutComponentMatchesContext(component, requiredContext) {
        var compContexts = component.applyTo && component.applyTo.contexts;
        if (compContexts) {
            return _.contains(compContexts, requiredContext);
        }

        return true;
    }

    function layoutComponentMatchesConstraints(component, requiredConstraints) {
        var compConstraints = component.applyTo && component.applyTo.constraints;
        if (compConstraints) {
            if (!requiredConstraints.length && compConstraints.length) {
                return false;
            }
            return requiredConstraints.length === compConstraints.length &&
                _.every(requiredConstraints, function(constraint) {
                    return _.contains(compConstraints, constraint);
                });
        }

        return true;
    }

    function layoutComponentAppliesToModel(component, model, rootModel, match) {
        if ('applyTo' in component) {
            if (_.isFunction(component.applyTo)) {
                return Boolean(component.applyTo(model, _.pick(match, 'constraints', 'context')));
            }
            if (component.applyTo) {
                var applyTo = component.applyTo,
                    useModel = _.isUndefined(model) ? rootModel : model,
                    validIfOnlyOneOff = ['type', 'displayType', 'conceptIri', 'edgeLabel'],
                    validApplyTos = ['type', 'displayType', 'conceptIri', 'edgeLabel', 'constraints', 'contexts'],
                    applyToKeys = _.keys(applyTo),
                    applyToIsValid = applyToKeys.length &&
                        _.intersection(validIfOnlyOneOff, applyToKeys).length <= 1 &&
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

                    var otherKeysToDeferChecking = ['constraints', 'contexts'],
                        onlyHasKeysThatAreDeferred = (
                            (applyToKeys.length === 1 && _.contains(otherKeysToDeferChecking, applyToKeys[0])) ||
                            (applyToKeys.length === 2 && _.difference(otherKeysToDeferChecking, applyToKeys).length === 0)
                        );

                    if (onlyHasKeysThatAreDeferred) {
                        return true;
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
