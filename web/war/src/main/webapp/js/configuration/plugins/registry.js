/**
 * View all available extensions and registrations in the _Admin_ panel inside
 * Visallo under _UI Extensions_.  Documentation with examples is available at {@link http://docs.visallo.org/extension-points/front-end docs.visallo.org}.
 *
 * @module registry
 * @classdesc Registry for adding and removing extensions given documentated extension points
 */
define(['underscore'], function(_) {
    'use strict';

    var extensions = {},
        extensionDocumentation = {},
        uuidToExtensionPoint = {},
        legacyMappingToNew = {},
        uuidGen = 0,
        alreadyWarnedAboutDocsByExtensionPoint = {},
        alreadyWarnedAboutLegacyByExtensionPoint = {},
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
        shouldWarn = function(map, extensionPoint) {
            if (extensionPoint in map) {
                return;
            }
            map[extensionPoint] = true;
            return true;
        },
        shouldWarnAboutMissingDocs = _.partial(shouldWarn, alreadyWarnedAboutDocsByExtensionPoint),
        shouldWarnAboutLegacyUse = _.partial(shouldWarn, alreadyWarnedAboutLegacyByExtensionPoint),
        moveExistingLegacyExtensions = function(old, extensionPoint) {
            if (extensions[old]) {
                if (!extensions[extensionPoint]) {
                    extensions[extensionPoint] = {};
                }
                _.each(extensions[old], function(config, uuid) {
                    extensions[extensionPoint][uuid] = config
                });
            }
        },

        /**
         * @alias module:registry
         */
        api = {
            debug: function() {
                console.log(extensions);
            },
            clear: function() {
                extensions = {};
                extensionDocumentation = {};
                legacyMappingToNew = {};
                uuidToExtensionPoint = {};
                uuidGen = 0;
            },

            /**
             * Get the canonical name for an extension point.
             * Some extension points have been renamed, so we translate to
             * the current.
             *
             * @param {String} point The extension point to transform
             * @returns {String} The canonical name
             */
            canonicalName: function(point) {
                if (!point) return;
                if (point in legacyMappingToNew) {
                    var canonical = legacyMappingToNew[point];
                    if (shouldWarnAboutLegacyUse(point)) {
                        console.warn(`This extension point has been renamed from ${point} to ${canonical},
The extension will continue to work, but the old name will be removed in future releases`)
                    }
                    return canonical;
                }

                return point;
            },

            /**
             * Register new functionality at the given `extensionPoint`. View the {@link http://docs.visallo.org/extension-points/front-end docs} for available extension points, or in Visallo, open the Admin panel, then UI Extensions.
             *
             * @param {String} extensionPoint string that designates the
             * extension to extend.
             * @param {Object} extension configuration object based on the
             * extension requirements.
             * @returns {String} extensionUuid used to {@link module:configuration/plugins/registry.unregisterExtension unregister an extension}
             * @example
             * registry.registerExtension('org.visallo.menubar', {
             *     title: 'New'
             *     identifier: 'org-visallo-example-new',
             *     action: {
             *         type: 'full',
             *         componentPath: 'example-new-page'
             *     },
             *     icon: '../img/new.png'
             * });
             */
            registerExtension: function(point, extension) {
                const extensionPoint = api.canonicalName(point);
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
            unregisterAllExtensions: function(point) {
                const extensionPoint = api.canonicalName(point);
                if (!extensionPoint) {
                    throw new Error('extension point required to unregister')
                }

                var uuids = _.keys(extensions[extensionPoint])
                uuidToExtensionPoint = _.omit(uuidToExtensionPoint, uuids)
                delete extensions[extensionPoint]
                delete extensionDocumentation[extensionPoint]
            },

            /**
             * Remove a given extension using the value returned from {@link module:configuration/plugins/registry.registerExtension registerExtension}
             * @param {String} extensionUuid The extension registration to remove
             */
            unregisterExtension: function(extensionUuid) {
                if (!extensionUuid) {
                    throw new Error('extension uuid required to unregister')
                }

                var extensionPoint = api.canonicalName(uuidToExtensionPoint[extensionUuid]);
                if (!extensionPoint) {
                    throw new Error('extension uuid not found in registry')
                }

                var byId = extensions[extensionPoint];
                if (byId) {
                    delete byId[extensionUuid];
                }

                triggerChange(extensionPoint);
            },
            markUndocumentedExtensionPoint: function(point) {
                const extensionPoint = api.canonicalName(point);
                extensionDocumentation[extensionPoint] = {
                    undocumented: true
                }
            },

            /**
             * Adds information in the Admin Panel -> UI Extensions list about this extension point.
             *
             * @param {String} extensionPoint The extension point to document
             * @param {String} description About this extension point / what it does
             * @param {function} validator Gets any registered extensions and returns if it's valid
             * @param {String|Object} [options] External URL to documentation if string, or options
             * @param {String} [url] External documentation url
             * @param {String} [legacyName] Previous extension point name to include with this. Will warn when used
             */
            documentExtensionPoint: function(point, description, validator, options) {
                const extensionPoint = api.canonicalName(point);
                if (!description) {
                    throw new Error('Description required for documentation')
                }

                if (!_.isFunction(validator)) {
                    throw new Error('Validator required for documentation')
                }

                var externalDocumentationUrl = _.isString(options) ? options : null;

                if (options) {
                    if (options.legacyName) {
                        legacyMappingToNew[options.legacyName] = extensionPoint
                        moveExistingLegacyExtensions(options.legacyName, extensionPoint)
                    }
                    if (options.url) {
                        externalDocumentationUrl = options.url;
                    }
                }

                extensionDocumentation[extensionPoint] = {
                    description: description,
                    validator: validator,
                    externalDocumentationUrl: externalDocumentationUrl
                };
            },

            extensionPointDocumentation: function() {
                return _.omit(_.mapObject(extensionDocumentation, function(doc, point) {
                    if (doc.undocumented) return;
                    return {
                        extensionPoint: point,
                        description: doc.description,
                        validator: doc.validator.toString(),
                        externalDocumentationUrl: doc.externalDocumentationUrl,
                        registered: api.extensionsForPoint(point).map(replaceFunctions)
                    };
                }), function(value) {
                    return !value;
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

            /**
             * Get all the currently registered extensions for a given `extensionPoint`.
             *
             * @param {String} extensionPoint The extension point to get extensions
             * @returns {Array.<Object>} List of all registered (and valid if validator exists) extension configuration values
             */
            extensionsForPoint: function(point) {
                const extensionPoint = api.canonicalName(point);
                var documentation = extensionDocumentation[extensionPoint],
                    byId = extensions[extensionPoint];

                if (!documentation && shouldWarnAboutMissingDocs(extensionPoint)) {
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
