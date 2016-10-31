define([
    './requirejs/promise!./service/ontologyPromise'
], function(ontology) {
    'use strict';

    return {
        getPropertyAcls: function(element) {
            return mergeElementPropertyAcls(
                ontologyPropertiesToAclProperties(ontology.properties)
            );

            function ontologyPropertiesToAclProperties(properties) {
                return properties.list.map(function(property) {
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
                _.each(element, function (e) {
                    _.each(e.acl.propertyAcls, function (elementPropertyAcl) {
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
                    })
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
