define([
    'util/withDataRequest'
], function(withDataRequest) {
    'use strict';

    return {
        getPropertyAcls: function(element) {
            const elements = Array.isArray(element) ? element : [element];

            return Promise.map(elements, (element) => {
                    let propertiesPromise = [];

                    if (element.type === 'vertex') {
                        propertiesPromise = withDataRequest.dataRequest('ontology', 'propertiesByConceptId', element.conceptType);
                    } else {
                        propertiesPromise = withDataRequest.dataRequest('ontology', 'propertiesByRelationship', element.label);
                    }

                    return propertiesPromise;
                })
                .then((elementsProperties) => {
                    const ontologyProperties = _.chain(elementsProperties)
                        .map((properties) => properties.list)
                        .flatten()
                        .uniq((p) => p.title)
                        .value();

                    return mergeElementPropertyAcls(
                        ontologyPropertiesToAclProperties(ontologyProperties)
                    );
                })

            function ontologyPropertiesToAclProperties(properties) {
                return properties.map(function(property) {
                    return {
                        key: null,
                        name: property.title,
                        addable: property.addable,
                        updateable: property.updateable,
                        deleteable: property.deleteable
                    };
                });
            }

            function mergeElementPropertyAcls(propertyAcls) {
                elements.forEach(function (e) {
                    e.acl.propertyAcls.forEach(function (elementPropertyAcl) {
                        var matches = _.where(propertyAcls, {
                            name: elementPropertyAcl.name,
                            key: elementPropertyAcl.keys || null
                        });
                        if (matches.length === 0) {
                            propertyAcls.push(elementPropertyAcl);
                        } else {
                            _.each(matches, function(r) {
                                _.extend(r, elementPropertyAcl);
                            });
                        }
                    });
                });

                return propertyAcls;
            }
        },

        findPropertyAcl: function(propertiesAcl, propName, propKey) {
            var props = _.where(propertiesAcl, {name: propName, key: propKey});
            if (props.length === 0) {
                var propsByName = _.where(propertiesAcl, {name: propName});
                if (propsByName.length === 0) {
                    throw new Error('no ACL property defined "' + propName + ':' + propKey + '"');
                }
                props = propsByName;
            }
            if (props.length !== 1) {
                throw new Error('more than one ACL property with the same name defined "' + propName + ':' + propKey + '" lenght: ' + props.length);
            }
            return props[0];
        }
    };
});
