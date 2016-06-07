define(['underscore'], function(_) {
    'use strict';

    var extensions = {},
        extensionDocumentation = {},
        uuidToExtensionPoint = {},
        uuidGen = 0,
        alreadyWarnedAboutDocsByExtensionPoint = {},
        verifyArguments = function(extensionPoint, extension) {
            if (!_.isString(extensionPoint) && extensionPoint) {
                throw new Error('extensionPoint must be string');
            }
            if (!extension) {
                throw new Error('extension must be provided');
            }
        },
        triggerChange = function(extensionPoint) {
            if (typeof $ !== 'undefined' && typeof document !== 'undefined') {
                $(document).trigger('extensionsChanged', {
                    extensionPoint: extensionPoint
                })
            }
        },
        shouldWarn = function(extensionPoint) {
            if (extensionPoint in alreadyWarnedAboutDocsByExtensionPoint) {
                return;
            }
            alreadyWarnedAboutDocsByExtensionPoint[extensionPoint] = true;
            return true;
        },
        api = {
            debug: function() {
                console.log(extensions);
            },
            clear: function() {
                extensions = {};
                extensionDocumentation = {};
                uuidToExtensionPoint = {};
                uuidGen = 0;
            },
            registerExtension: function(extensionPoint, extension) {
                verifyArguments.apply(null, arguments);

                var byId = extensions[extensionPoint],
                    uuid = [extensionPoint, uuidGen++].join('-');

                if (!byId) {
                    byId = extensions[extensionPoint] = {};
                }

                byId[uuid] = extension;
                uuidToExtensionPoint[uuid] = extensionPoint;

                triggerChange(extensionPoint);

                return uuid;
            },
            unregisterAllExtensions: function(extensionPoint) {
                if (!extensionPoint) {
                    throw new Error('extension point required to unregister')
                }

                var uuids = _.keys(extensions[extensionPoint])
                uuidToExtensionPoint = _.omit(uuidToExtensionPoint, uuids)
                delete extensions[extensionPoint]
                delete extensionDocumentation[extensionPoint]
            },
            unregisterExtension: function(extensionUuid) {
                if (!extensionUuid) {
                    throw new Error('extension uuid required to unregister')
                }

                var extensionPoint = uuidToExtensionPoint[extensionUuid];
                if (!extensionPoint) {
                    throw new Error('extension uuid not found in registry')
                }

                var byId = extensions[extensionPoint];
                if (byId) {
                    delete byId[extensionUuid];
                }

                triggerChange(extensionPoint);
            },

            documentExtensionPoint: function(extensionPoint, description, validator, externalDocumentationUrl) {
                if (!description) {
                    throw new Error('Description required for documentation')
                }

                if (!_.isFunction(validator)) {
                    throw new Error('Validator required for documentation')
                }

                extensionDocumentation[extensionPoint] = {
                    description: description,
                    validator: validator,
                    externalDocumentationUrl: externalDocumentationUrl
                };
            },

            extensionPointDocumentation: function() {
                return _.mapObject(extensionDocumentation, function(doc, point) {
                    return {
                        extensionPoint: point,
                        description: doc.description,
                        validator: doc.validator.toString(),
                        externalDocumentationUrl: doc.externalDocumentationUrl,
                        registered: api.extensionsForPoint(point).map(replaceFunctions)
                    };
                });

                function replaceFunctions(object) {
                    if (_.isFunction(object)) {
                        return 'FUNCTION' + object.toString()
                    } else if (_.isArray(object)) {
                        return _.map(object, replaceFunctions);
                    } else if (_.isObject(object)) {
                        return _.mapObject(object, replaceFunctions);
                    }
                    return object;
                }
            },

            extensionsForPoint: function(extensionPoint) {
                var documentation = extensionDocumentation[extensionPoint],
                    byId = extensions[extensionPoint];

                if (!documentation && shouldWarn(extensionPoint)) {
                    console.warn('Consider adding documentation for ' +
                        extensionPoint +
                        '\n\tUsage: registry.documentExtensionPoint(\'' + extensionPoint + '\', desc, validator)'
                    );
                }

                if (byId) {
                    var registered = _.values(byId);
                    if (documentation) {
                        var validityChecked = _.partition(registered, documentation.validator);
                        if (validityChecked[1].length) {
                            console.warn(
                                'Extensions invalid',
                                validityChecked[1],
                                'according to validator',
                                documentation.validator.toString()
                            );
                        }
                        return validityChecked[0];
                    } else {
                        return registered;
                    }
                }
                return [];
            }
        };

    return api;
})
