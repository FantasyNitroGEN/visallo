
define([
    'flight/lib/component',
    'tpl!./geoLocation',
    './withPropertyField',
    'util/withDataRequest'
], function(defineComponent, template, withPropertyField, withDataRequest) {
    'use strict';

    return defineComponent(GeoLocationField, withPropertyField, withDataRequest);

    function makeNumber(v) {
        return parseFloat(v, 10);
    }

    function splitLatLon(latLonStr) {
        var parts = latLonStr.split(',');
        if (parts.length === 2) {
            return [ $.trim(parts[0]), $.trim(parts[1]) ];
        }
        return null;
    }

    function GeoLocationField() {

        this.defaultAttrs({
            descriptionSelector: '.description',
            latSelector: '.lat',
            lonSelector: '.lon',
            radiusSelector: '.radius'
        });

        this.after('initialize', function() {
            var self = this;

            this.hasGeocoder().done(function(enabled) {
                self.attr.hasGeocoder = enabled;
                self.$node.html(template(self.attr));
                self.setupDescriptionTypeahead();
                self.on(self.select('descriptionSelector'), 'focus blur', self.onFocusDescription);
                self.on('change keyup', {
                    inputSelector: function(event) {
                        var latLon = splitLatLon(self.getValues()[1]);
                        if (latLon) {
                            var latInput = self.$node.find('input.lat'),
                                lonInput = self.$node.find('input.lon');
                            latInput.val(latLon[0]);
                            lonInput.val(latLon[1]);
                            lonInput.focus();
                        }

                        self.triggerFieldUpdated();
                    }
                });
            });
        });

        this.onFocusDescription = function(event) {
            if (!this.attr.predicates || !this.attr.hasGeocoder) {
                return;
            }

            this.$node.toggleClass('desc-focus', event.type === 'focus');
        };

        this.triggerFieldUpdated = function() {
            var values = _.compact(this.getValues());
            this.filterUpdated(values.map(function(v, i) {
                if (values.length === 3 && i === 0) {
                    return v;
                }
                return makeNumber(v);
            }));
        };

        this.isValid = function() {
            var self = this,
                expected = 2,
                values = this.getValues();

            if (!self.attr.predicates && self.attr.hasGeocoder) {
                expected++;
            }
            if (self.attr.predicates) {
                expected++;
            }

            return (values.length === expected) &&
                _.every(values, function(v, i) {
                    var valIsValid = false;
                    if ((self.attr.hasGeocoder && !self.attr.predicates) && i === 0) {
                        valIsValid = true;
                    } else if (self.attr.predicates && i === (self.attr.hasGeocoder ? 3 : 2)) {
                        var radiusElement = self.select('radiusSelector');
                        valIsValid = makeNumber(v) > 0;
                        (valIsValid ? radiusElement.removeClass : radiusElement.addClass)('invalid');
                    } else {
                        var latLonElement;
                        valIsValid = v.length && _.isNumber(makeNumber(v)) && !isNaN(v);
                        if (i === (self.attr.hasGeocoder ? 1 : 0)) {
                            latLonElement = self.select('latSelector');
                            valIsValid = valIsValid && (makeNumber(v) >= -90 && makeNumber(v) <= 90);
                        } else {
                            latLonElement = self.select('lonSelector');
                        }
                        latLonElement.toggleClass('invalid', !valIsValid);
                    }
                    return valIsValid;
                });
        };

        this.hasGeocoder = function() {
            return this.dataRequest('config', 'properties').then(function(config) {
                return config['geocoder.enabled'] === 'true';
            });
        };

        this.setupDescriptionTypeahead = function() {
            var self = this;

            if (this.attr.hasGeocoder) {
                var savedResults, request;

                self.select('descriptionSelector')
                    .parent().css('position', 'relative').end()
                    .typeahead({
                        items: 15,
                        minLength: 3,
                        source: function(q, process) {
                            if (request && request.cancel) {
                                request.cancel();
                            }

                            request = self.dataRequest('map', 'geocode', q)
                                .then(function(data) {
                                    savedResults = _.indexBy(data.results, 'name');
                                    process(_.keys(savedResults));
                                })
                                .catch(function() {
                                    process([]);
                                })
                        },
                        matcher: function(item) {
                            return true;
                        },
                        updater: function(item) {
                            var result = savedResults[item];
                            if (result) {
                                var lat = self.$node.find('.lat').val(result.latitude)
                                        .parent().removePrefixedClasses('pop-'),
                                    lon = self.$node.find('.lon').val(result.longitude)
                                        .parent().removePrefixedClasses('pop-');

                                requestAnimationFrame(function() {
                                    lat.addClass('pop-fast');
                                    _.delay(function() {
                                        lon.addClass('pop-fast');
                                    }, 250)
                                });

                                if (self.isValid()) {
                                    self.triggerFieldUpdated();
                                } else {
                                    self.$node.find('.radius').focus();
                                }

                                return result.name;
                            }
                            return item;
                        }
                    });
                }
        }
    }
});
