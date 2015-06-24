define([
    'util/withDataRequest'
], function(withDataRequest) {
    'use strict';

    return withPropertyInfo;

    function withPropertyInfo() {

        this.showPropertyInfo = function(button, data, property) {
            var $target = $(button),
                shouldOpen = $target.lookupAllComponents().length === 0;

            Promise.all([
                Promise.require('util/popovers/propertyInfo/propertyInfo'),
                withDataRequest.dataRequest('ontology', 'properties')
            ]).done(function(results) {
                var PropertyInfo = results.shift(),
                    ontologyProperties = results.shift(),
                    ontologyProperty = ontologyProperties && property && property.name && ontologyProperties.byTitle[property.name];

                if (shouldOpen) {
                    PropertyInfo.teardownAll();
                    PropertyInfo.attachTo($target, {
                        data: data,
                        property: property,
                        ontologyProperty: ontologyProperty
                    });
                } else {
                    $target.teardownComponent(PropertyInfo);
                }
            });
        };

        this.hidePropertyInfo = function(button) {
            var $target = $(button);

            require(['util/popovers/propertyInfo/propertyInfo'], function(PropertyInfo) {
                $target.teardownComponent(PropertyInfo);
            });
        }

    }
});
