
define([
    './urlFormatters',
    './formula',
    'util/messages',
    'util/requirejs/promise!../service/ontologyPromise'
], function(
    F,
    formula,
    i18n,
    ontology) {
    'use strict';

    var propertiesByTitle = ontology.properties.byTitle,
        propertiesByDependentToCompound = ontology.properties.byDependentToCompound,
        V = {

            isPublished: function(vertex) {
                return V.sandboxStatus.apply(null, arguments) === undefined;
            },

            sandboxStatus: function(vertexOrProperty) {
                if (arguments.length === 3) {
                    var props = V.props.apply(null, arguments);
                    if (props.length) {
                        return _.any(props, function(p) {
                            return V.sandboxStatus(p) === undefined;
                        }) ? undefined : i18n('vertex.status.unpublished');
                    }
                    return;
                }

                return (/^(private|public_changed)$/i).test(vertexOrProperty.sandboxStatus) ?
                        i18n('vertex.status.unpublished') :
                        undefined;
            },

            getVertexIdsFromDataEventOrCurrentSelection: function(data) {
                // Normalize the vertexIds sent from a vertex menu event,
                // also checking the current object selection
                var vertexIds = [];

                if (data && data.vertexId) {
                    vertexIds = [data.vertexId];
                } else if (data && data.vertexIds) {
                    vertexIds = data.vertexIds;
                }

                if ((typeof window.visalloData !== 'undefined') &&
                   visalloData.selectedObjects &&
                   visalloData.selectedObjects.vertices.length > 0) {

                    var selectedVertexIds = _.pluck(visalloData.selectedObjects.vertices, 'id');
                    if (_.intersection(vertexIds, selectedVertexIds).length) {
                        vertexIds = vertexIds.concat(selectedVertexIds);
                    } else if (!vertexIds.length) {
                        vertexIds = selectedVertexIds;
                    }
                }

                return _.unique(vertexIds);
            },

            metadata: {
                // Define/override metadata dataType specific displayTransformers here
                //
                // All functions receive: function(el, value, property, vertexId)
                // set the value synchronously
                // - or -
                // append "Async" to function name and return a $.Deferred().promise()

                datetime: function(el, value) {
                    el.textContent = F.date.dateTimeString(value);
                },

                sandboxStatus: function(el, value) {
                    el.textContent = V.sandboxStatus({ sandboxStatus: value }) || '';
                },

                percent: function(el, value) {
                    el.textContent = F.number.percent(value);
                },

                userAsync: function(el, userId) {
                    return Promise.require('util/withDataRequest')
                        .then(function(withDataRequest) {
                            return withDataRequest.dataRequest('user', 'getUserNames', [userId])
                        })
                        .then(function(users) {
                            el.textContent = users && users[0] || i18n('user.unknown.displayName');
                        })
                }
            },

            properties: {
                // Define/override dataType specific displayTransformers here
                //
                // All functions receive: function(HtmlElement, property, vertex)
                // Must populate the dom element with value
                //
                // for example: geoLocation: function(...) { el.textContent = 'coords'; }

                visibility: function(el, property) {
                    $('<i>').text((
                        property.value &&
                        property.value.source
                    ) || i18n('visibility.blank')).appendTo(el);
                },

                geoLocation: function(el, property) {
                    if ($('#app.fullscreen-details').length) {
                        $(el).append(
                            F.geoLocation.pretty(property.value)
                        );
                        return;
                    }

                    var anchor = $('<a>')
                        .addClass('map-coordinates')
                        .data({
                            latitude: property.value.latitude,
                            longitude: property.value.longitude
                        }),
                        displayValue = F.geoLocation.pretty(property.value, true);

                    if (property.value.description) {
                        anchor.append(property.value.description + ' ');
                    }

                    $('<small>')
                        .css('white-space', 'nowrap')
                        .text(F.geoLocation.pretty(property.value, true))
                        .appendTo(anchor);

                    anchor.appendTo(el);
                },

                bytes: function(el, property) {
                    el.textContent = F.bytes.pretty(property.value);
                },

                link: function(el, property, vertex) {
                    var anchor = document.createElement('a'),
                        value = V.prop(vertex, property.name),
                        href = $.trim(value);

                    if (!(/^http/).test(href)) {
                        href = 'http://' + href;
                    }

                    anchor.setAttribute('href', href);
                    anchor.setAttribute('target', '_blank');
                    anchor.textContent = href;

                    el.appendChild(anchor);
                },

                textarea: function(el, property) {
                    $(el).html(_.escape(property.value || '').replace(/\r?\n+/g, '<br>'));
                },

                heading: function(el, property) {
                    var div = document.createElement('div'),
                        dim = 12,
                        half = dim / 2;

                    el.textContent = F.number.heading(property.value);
                    div.style.width = div.style.height = dim + 'px';
                    div.style.display = 'inline-block';
                    div.style.marginRight = '0.25em';
                    div = el.insertBefore(div, el.childNodes[0]);

                    require(['d3'], function(d3) {
                        d3.select(div)
                            .append('svg')
                                .style('vertical-align', 'middle')
                                .attr('width', dim)
                                .attr('height', dim)
                                .append('g')
                                    .attr('transform', 'rotate(' + property.value + ' ' + half + ' ' + half + ')')
                                    .call(function() {
                                        this.append('line')
                                            .attr('x1', half)
                                            .attr('y1', 0)
                                            .attr('x2', half)
                                            .attr('y2', dim)
                                            .call(styling)

                                        this.append('g')
                                            .attr('transform', 'rotate(30 ' + half + ' 0)')
                                            .call(createArrowLine)

                                        this.append('g')
                                            .attr('transform', 'rotate(-30 ' + half + ' 0)')
                                            .call(createArrowLine)
                                    });
                    });

                    function createArrowLine() {
                        this.append('line')
                            .attr('x1', half)
                            .attr('y1', 0)
                            .attr('x2', half)
                            .attr('y2', dim / 3)
                            .call(styling);
                    }
                    function styling() {
                        this.attr('stroke', '#555')
                            .attr('line-cap', 'round')
                            .attr('stroke-width', '1');
                    }
                }

            },

            hasMetadata: function(property) {
                var status = V.sandboxStatus(property),
                    modifiedBy = property['http://visallo.org#modifiedBy'],
                    modifiedDate = property['http://visallo.org#modifiedDate'],
                    sourceTimezone = property['http://visallo.org#sourceTimezone'],
                    confidence = property['http://visallo.org#confidence'],
                    justification = property['http://visallo.org#justification'],
                    source = property._sourceMetadata;

                return (
                    status ||
                    justification ||
                    source ||
                    modifiedBy ||
                    modifiedDate ||
                    sourceTimezone ||
                    confidence
                );
            },

            concept: function(vertex) {
                var conceptType = vertex && V.prop(vertex, 'conceptType'), concept;

                if (!conceptType || conceptType === 'Unknown') {
                    conceptType = 'http://www.w3.org/2002/07/owl#Thing';
                }

                concept = ontology.concepts.byId[conceptType];
                if (!concept && conceptType !== 'relationship') {
                    console.warn('Concept: ' + conceptType + ' is not in ontology');
                    concept = ontology.concepts.byId['http://www.w3.org/2002/07/owl#Thing'];
                }
                return concept;
            },

            conceptProperties: function(vertex) {
                var concept = V.concept(vertex),
                    properties = [];
                do {
                    properties = properties.concat(concept.properties);
                    concept = concept.parentConcept && ontology.concepts.byId[concept.parentConcept];
                } while (concept);
                return properties;
            },

            hasProperty: function(vertex, propertyName) {
                var concept = V.concept(vertex);
                do {
                    if (concept && concept.properties.indexOf(propertyName) >= 0) {
                        return true;
                    }
                    concept = concept.parentConcept && ontology.concepts.byId[concept.parentConcept];
                } while (concept);
                return false;
            },

            isKindOfConcept: function(vertex, conceptTypeFilter) {
                var conceptType = V.prop(vertex, 'conceptType');

                do {
                    if (conceptType === conceptTypeFilter) {
                        return true;
                    }

                    conceptType = ontology.concepts.byId[conceptType].parentConcept;
                } while (conceptType)

                return false;
            },

            externalImage: function(vertex, optionalWorkspaceId, url, maxWidth, maxHeight) {
                var params = {
                        vId: vertex.id,
                        url: url,
                        workspaceId: optionalWorkspaceId || visalloData.currentWorkspaceId,
                        maxWidth: maxWidth || 400,
                        maxHeight: maxHeight || 400
                    },
                    template = _.template('{origin}/resource/external?');

                return template({
                    origin: location.origin
                }) + $.param(params);
            },

            image: function(vertex, optionalWorkspaceId, width) {
                var entityImageUrl = V.prop(vertex, 'entityImageUrl');
                if (entityImageUrl) {
                    return V.externalImage(vertex, optionalWorkspaceId, entityImageUrl, width, width);
                }

                var entityImageVertexId = V.prop(vertex, 'entityImageVertexId'),
                    concept = V.concept(vertex),
                    displayType = V.displayType(vertex),
                    isImage = displayType === 'image',
                    isVideo = displayType === 'video';

                if (entityImageVertexId || isImage) {
                    return 'vertex/thumbnail?' + $.param({
                        workspaceId: optionalWorkspaceId || visalloData.currentWorkspaceId,
                        graphVertexId: entityImageVertexId || vertex.id,
                        width: width || 150
                    });
                }

                if (isVideo) {
                    var posterFrame = _.any(vertex.properties, function(p) {
                        return p.name === 'http://visallo.org#rawPosterFrame';
                    });
                    if (posterFrame) {
                        return 'vertex/poster-frame?' + $.param({
                            workspaceId: optionalWorkspaceId || visalloData.currentWorkspaceId,
                            graphVertexId: vertex.id
                        });
                    }
                }

                return concept.glyphIconHref;
            },

            imageIsFromConcept: function(vertex, optionalWorkspaceId) {
                return V.image(vertex, optionalWorkspaceId) === V.concept(vertex).glyphIconHref;
            },

            imageDetail: function(vertex, optionalWorkspaceId) {
                return V.image(vertex, optionalWorkspaceId, 800);
            },

            raw: function(vertex, optionalWorkspaceId) {
                return 'vertex/raw?' + $.param({
                    workspaceId: optionalWorkspaceId || visalloData.currentWorkspaceId,
                    graphVertexId: vertex.id
                });
            },

            imageFrames: function(vertex, optionalWorkspaceId) {
                var videoPreview = _.any(vertex.properties, function(p) {
                    return p.name === 'http://visallo.org#videoPreviewImage';
                });
                if (videoPreview) {
                    return 'vertex/video-preview?' + $.param({
                        workspaceId: optionalWorkspaceId || visalloData.currentWorkspaceId,
                        graphVertexId: vertex.id
                    });
                }
            },

            propName: function(name) {
                var autoExpandedName = (/^http:\/\/visallo.org/).test(name) ?
                        name : ('http://visallo.org#' + name),
                    ontologyProperty = propertiesByTitle[name] || propertiesByTitle[autoExpandedName],

                    resolvedName = ontologyProperty && (
                        ontologyProperty.title === name ? name : autoExpandedName
                    ) || name;

                return resolvedName;
            },

            longestProp: function(vertex, optionalName) {
                var properties = vertex.properties
                    .filter(function(a) {
                        var ontologyProperty = propertiesByTitle[a.name];
                        if (optionalName && optionalName !== a.name) {
                            return false;
                        }
                        return ontologyProperty && ontologyProperty.userVisible;
                    })
                    .map(function(a) {
                        var parentProperty = propertiesByDependentToCompound[a.name];
                        if (parentProperty && V.hasProperty(vertex, parentProperty)) {
                            return V.prop(vertex, parentProperty, a.key);
                        }
                        return V.prop(vertex, a.name, a.key);
                    })
                    .sort(function(a, b) {
                        return b.length - a.length;
                    });
                if (properties.length > 0) {
                    return properties[0];
                }
            },

            propFromAudit: function(vertex, propertyAudit) {
                //propertyName, newValue, previousValue
                return V.propDisplay(propertyAudit.propertyName, propertyAudit.newValue || propertyAudit.previousValue);
            },

            rollup: function(name, values) {
                name = V.propName(name);
                var ontologyProperty = propertiesByTitle[name],
                    min = Number.MAX_VALUE,
                    max = Number.MIN_VALUE,
                    sum = 0;

                if (ontologyProperty) {
                    switch (ontologyProperty.dataType) {
                        case 'date':
                            values.forEach(function(v) {
                                min = Math.min(v, min);
                                max = Math.max(v, max);
                                sum += v;
                            })

                            return {
                                span: F.date.relativeToDate(min, max),
                                average: F.date.dateString(sum / (values.length || 1))
                            }
                        case 'double':
                        case 'integer':
                        case 'currency':
                        case 'number':
                            sum = _.reduce(values, function(m, v) {
                                return m + v;
                            });
                            return {
                                sum: F.number.pretty(sum),
                                average: F.number.pretty(sum / (values.length || 1))
                            };
                    }
                }

                return {};
            },

            propDisplay: function(name, value, options) {
                name = V.propName(name);
                var ontologyProperty = propertiesByTitle[name];

                if (!ontologyProperty) {
                    return value;
                }

                if (ontologyProperty.possibleValues) {
                    var foundPossibleValue = ontologyProperty.possibleValues[value];
                    if (foundPossibleValue) {
                        return foundPossibleValue;
                    } else {
                        console.warn('Unknown ontology value for key', value, ontologyProperty);
                    }
                }

                if (ontologyProperty.displayType) {
                    switch (ontologyProperty.displayType) {
                        case 'phoneNumber': return F.string.phoneNumber(value);
                        case 'ssn': return F.string.ssn(value);
                        case 'bytes': return F.bytes.pretty(value);
                        case 'heading': return F.number.heading(value);
                        case 'duration' : return F.number.duration(value);
                    }
                }

                switch (ontologyProperty.dataType) {
                    case 'boolean': return F.boolean.pretty(value);

                    case 'date': {
                        if (ontologyProperty.displayType !== 'dateOnly') {
                            return F.date.dateTimeString(value);
                        }
                        return F.date.dateStringUtc(value);
                    }

                    case 'double':
                    case 'integer':
                    case 'currency':
                    case 'number': return F.number.pretty(value);
                    case 'geoLocation': return F.geoLocation.pretty(value);

                    default:

                        if (options && _.isObject(options)) {
                            return _.reduce(options, function(val, enabled, transformName) {
                                if (enabled === true &&
                                    transformName in F.string &&
                                    _.isFunction(F.string[transformName])) {
                                    return F.string[transformName](val);
                                }
                                return val;
                            }, value)
                        }
                        return value;
                }
            },

            prop: function(vertex, name, optionalKey, optionalOpts) {
                checkVertexAndPropertyNameArguments(vertex, name);

                if (_.isObject(optionalKey)) {
                    optionalOpts = optionalKey;
                    optionalKey = null;
                }

                name = V.propName(name);

                var value = V.propRaw(vertex, name, optionalKey, optionalOpts),
                    ignoreDisplayFormula = optionalOpts && optionalOpts.ignoreDisplayFormula,
                    ontologyProperty = propertiesByTitle[name];

                if (!ontologyProperty) {
                    return value;
                }

                if (_.isArray(value)) {
                    if (!optionalKey) {
                        var firstMatchingProperty = _.find(vertex.properties, function(p) {
                            return ~ontologyProperty.dependentPropertyIris.indexOf(p.name);
                        });
                        optionalKey = (firstMatchingProperty && firstMatchingProperty.key);
                    }
                    if (ontologyProperty.displayFormula) {
                        return formula(ontologyProperty.displayFormula, vertex, V, optionalKey);
                    } else {
                        var dependentIris = ontologyProperty && ontologyProperty.dependentPropertyIris || [];
                        if (dependentIris.length) {
                            return _.map(
                                dependentIris,
                                _.partial(V.prop, vertex, _, optionalKey, optionalOpts)
                            ).join(' ');
                        } else {
                            return value.join(' ');
                        }
                    }
                }

                if (!ignoreDisplayFormula && ontologyProperty.displayFormula) {
                    return formula(ontologyProperty.displayFormula, vertex, V, optionalKey, optionalOpts);
                }

                return V.propDisplay(name, value, optionalOpts);
            },

            props: function(vertex, name, optionalKey) {
                checkVertexAndPropertyNameArguments(vertex, name);

                var hasKey = !_.isUndefined(optionalKey);

                if (arguments.length === 3 && !hasKey) {
                    throw new Error('Undefined key is not allowed. Remove parameter to return all named properties');
                }

                name = V.propName(name);

                var ontologyProperty = ontology.properties.byTitle[name],
                    dependentIris = ontologyProperty && ontologyProperty.dependentPropertyIris,
                    foundProperties = _.filter(vertex.properties, function(p) {
                        if (dependentIris) {
                            return ~dependentIris.indexOf(p.name) && (
                                hasKey ? optionalKey === p.key : true
                            );
                        }

                        return name === p.name && (
                            hasKey ? optionalKey === p.key : true
                        );
                    });

                if (name === 'http://visallo.org#visibilityJson' && foundProperties.length === 0) {
                    // Protect against no visibility, just set to empty
                    return [{
                        key: '',
                        sandboxStatus: 'PUBLIC',
                        name: 'http://visallo.org#visibilityJson',
                        metadata: {},
                        value: {
                            source: ''
                        }
                    }];
                }

                return foundProperties;
            },

            propValid: function(vertex, values, propertyName, propertyKey) {
                checkVertexAndPropertyNameArguments(vertex, propertyName);
                if (!_.isArray(values)) {
                    throw new Error('Unable to validate without values array')
                }

                var ontologyProperty = ontology.properties.byTitle[propertyName],
                    dependentIris = ontologyProperty.dependentPropertyIris,
                    formulaString = ontologyProperty.validationFormula,
                    result = true;

                if (formulaString) {
                    if (values.length) {
                        var properties = [];
                        if (dependentIris) {
                            dependentIris.forEach(function(iri, i) {
                                var property = _.findWhere(vertex.properties, {
                                        name: iri,
                                        key: propertyKey
                                    }),
                                    value = _.isArray(values[i]) && values[i].length === 1 ? values[i][0] : values[i]

                                if (property) {
                                    property = _.extend({}, property, { value: value });
                                    if (_.isUndefined(values[i])) {
                                        property.value = undefined;
                                    }
                                } else {
                                    property = {
                                        name: iri,
                                        key: propertyKey,
                                        value: value
                                    };
                                }
                                properties.push(property);
                            })
                        }
                        vertex = _.extend({}, vertex, { properties: properties });
                    }
                    result = formula(formulaString, vertex, V, propertyKey);
                }

                return Boolean(result);
            },

            title: function(vertex) {
                var title = formulaResultForVertex(vertex, 'titleFormula')

                if (!title) {
                    title = V.prop(vertex, 'title', undefined, {
                        ignoreErrorIfTitle: true
                    });
                }

                return title;
            },

            subtitle: _.partial(formulaResultForVertex, _, 'subtitleFormula', ''),

            time: _.partial(formulaResultForVertex, _, 'timeFormula', ''),

            heading: function(vertex) {
                var headingProp = _.find(vertex.properties, function(p) {
                  return p.name.indexOf('heading') > 0;
                });
                if (headingProp) {
                    return headingProp.value;
                }
                return 0;
            },

            propRaw: function(vertex, name, optionalKey, optionalOpts) {
                checkVertexAndPropertyNameArguments(vertex, name);

                if (_.isObject(optionalKey)) {
                    optionalOpts = optionalKey;
                    optionalKey = null;
                }

                var hasKey = !_.isUndefined(optionalKey),
                    options = _.extend({
                        defaultValue: undefined,
                        ignoreErrorIfTitle: false
                    }, optionalOpts || {});

                if (options.ignoreErrorIfTitle !== true && name === 'title') {
                    throw new Error('Use title function, not generic prop');
                }

                name = V.propName(name);

                var ontologyProperty = propertiesByTitle[name],
                    dependentIris = ontologyProperty && ontologyProperty.dependentPropertyIris || [],
                    iris = dependentIris.length ? dependentIris : [name],
                    properties = transformMatchingVertexProperties(vertex, iris);

                if (dependentIris.length) {
                    if (options.throwErrorIfCompoundProperty) {
                        throw new Error('Compound properties that depend on compound properties are not allowed');
                    }

                    if (!hasKey && properties.length) {
                        optionalKey = properties[0].key;
                    }

                    options.throwErrorIfCompoundProperty = true;

                    return _.map(dependentIris, _.partial(V.propRaw, vertex, _, optionalKey, options));
                } else {
                    var foundProperties = hasKey ?
                            _.where(properties, { key: optionalKey }) :
                            properties,

                        hasValue = foundProperties &&
                            foundProperties.length &&
                            !_.isUndefined(foundProperties[0].value);

                    if (!hasValue &&
                        name !== 'http://visallo.org#title' &&
                        _.isUndefined(options.defaultValue)) {
                        return undefined;
                    }

                    return hasValue ? foundProperties[0].value :
                        (
                            options.defaultValue ||
                            i18n('vertex.property.not_available',
                                (ontologyProperty && ontologyProperty.displayName || '').toLowerCase() || name)
                        )
                }
            },

            isEdge: function(vertex) {
                var propsIsObjectNotArray = _.isObject(vertex && vertex.properties) &&
                    vertex.properties['http://visallo.org#conceptType'] === 'relationship';
                return propsIsObjectNotArray ||
                    V.prop(vertex, 'conceptType') === 'relationship' ||
                    (_.has(vertex, 'sourceVertexId') && _.has(vertex, 'destVertexId'));
            },

            isArtifact: function(vertex) {
                return _.contains(_.pluck(vertex.properties, 'name'), V.propName('raw'));
            },

            displayType: function(vertex) {
                if (!V.isArtifact(vertex)) {
                    return V.isEdge(vertex) ? 'edge' : 'entity';
                }

                var propNames = _.pluck(vertex.properties, 'name');
                if (_.some(propNames, function(propName) { return propName.indexOf('http://visallo.org#video-') === 0; })) {
                    return 'video';
                } else if (_.some(propNames, function(propName) { return propName.indexOf('http://visallo.org#audio-') === 0; })) {
                    return 'audio';
                } else {
                    var rawProp = V.props(vertex, V.propName('raw')),
                        rawPropMimeType = rawProp && rawProp.length && rawProp[0].metadata && rawProp[0].metadata[V.propName('mimeType')];
                    if (rawPropMimeType && rawPropMimeType.indexOf('image/') === 0) {
                        return 'image';
                    } else {
                        return 'document';
                    }
                }
            }
        };

    // Legacy
    V.properties.byte = V.properties.bytes;

    return $.extend({}, F, { vertex: V });

    function treeLookupForConceptProperty(conceptId, propertyName) {
        var ontologyConcept = conceptId && ontology.concepts.byId[conceptId],
            formulaString = ontologyConcept && ontologyConcept[propertyName];

        if (formulaString) {
            return formulaString;
        }

        if (ontologyConcept && ontologyConcept.parentConcept) {
            return treeLookupForConceptProperty(ontologyConcept.parentConcept, propertyName);
        }
    }

    function formulaResultForVertex(vertex, formulaKey, defaultValue) {
        var conceptId = V.prop(vertex, 'conceptType'),
            formulaString = treeLookupForConceptProperty(conceptId, formulaKey),
            result = defaultValue;

        if (formulaString) {
            result = formula(formulaString, vertex, V);
        }

        return result;
    }

    function transformMatchingVertexProperties(vertex, propertyNames) {
        var CONFIDENCE = 'http://visallo.org#confidence',
            sorted = _.chain(vertex.properties)
                .filter(function(p) {
                    return ~propertyNames.indexOf(p.name);
                })
                .sortBy(function(p) {
                    var value = V.propDisplay(p.name, p.value);
                    if (_.isString(value)) {
                        return value.toLowerCase();
                    }
                    return 0;
                })
                .sortBy(function(p) {
                    var confidence = p.metadata && p.metadata[CONFIDENCE]
                    if (confidence) {
                        return confidence * -1;
                    }
                    return 0;
                })
                .value()

        return sorted;
    }

    function checkVertexAndPropertyNameArguments(vertex, propertyName) {
        if (!vertex || !vertex.id || !_.isArray(vertex.properties)) {
            throw new Error('Vertex is invalid', vertex);
        }
        if (!propertyName || !_.isString(propertyName)) {
            throw new Error('Property name is invalid', propertyName);
        }
    }
});
