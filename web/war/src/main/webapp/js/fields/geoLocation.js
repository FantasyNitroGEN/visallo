
define([
    'flight/lib/component',
    'hbs!./geoLocationTpl',
    'util/parsers',
    'util/vertex/formatters',
    './withPropertyField',
    'util/withDataRequest'
], function(defineComponent, template, P, F, withPropertyField, withDataRequest) {
    'use strict';

    return defineComponent(GeoLocationField, withPropertyField, withDataRequest);

    function makeNumber(v) {
        return P.number.parseFloat(v);
    }

    function splitLatLon(latLonStr) {
        var parts = latLonStr.split(',');
        if (parts.length === 2) {
            var lat = makeNumber(parts[0].trim()),
                lon = makeNumber(parts[1].trim());

            if (_.isNumber(lat) && _.isNumber(lon)) {
                return {
                    latitude: lat,
                    longitude: lon
                };
            }
        }
    }

    function invalidRange(coord, val) {
        return isNaN(coord) || coord < (val * -1) || coord > val;
    }

    function GeoLocationField() {

        this.defaultAttrs({
            descriptionSelector: '.description',
            latSelector: '.lat',
            lonSelector: '.lon',
            radiusSelector: '.radius'
        });

        this.before('initialize', function(node, config) {
            config.asyncRender = true;
        });

        this.after('initialize', function() {
            var self = this;

            this.rendered = new Promise(function(fulfill) {
                self.hasGeocoder().done(function(enabled) {
                    self.attr.hasGeocoder = enabled;
                    self.$node.html(template(self.attr));
                    self.setValue(self.attr.value);
                    self.setupDescriptionTypeahead();
                    self.on(self.select('descriptionSelector'), 'focus blur', self.onFocusDescription);
                    fulfill();
                    self.trigger('fieldRendered')

                    self.on('paste', {
                        latSelector: function(event) {
                            _.defer(function() {
                                var pastedValue = $(event.target).val();
                                if (pastedValue.length) {
                                    self.setValue(pastedValue);
                                    self.triggerFieldUpdated();

                                    self.select('latSelector')
                                        .add(self.select('lonSelector'))
                                        .animatePop();
                                }
                            })
                        }
                    })
                });
            })
        });

        this.setValue = function(value) {
            if (_.isString(value)) {
                var parsedValue = splitLatLon(value);
                if (parsedValue) {
                    value = parsedValue;
                }
            }
            this.select('descriptionSelector').val(value.description || '');
            this.select('latSelector').val(value.latitude || '');
            this.select('lonSelector').val(value.longitude || '');
            this.select('radiusSelector').val(value.radius || '');
        };

        this.getValue = function() {
            var value = {
                description: $.trim(this.select('descriptionSelector').val()),
                latitude: makeNumber($.trim(this.select('latSelector').val())),
                longitude: makeNumber($.trim(this.select('lonSelector').val()))
            };

            if (this.attr.onlySearchable) {
                value.radius = makeNumber($.trim(this.select('radiusSelector').val()));
            }
            return value;
        };

        this.onFocusDescription = function(event) {
            if (!this.attr.hasGeocoder) {
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

        this.isValid = function(value) {
            var self = this;

            return this.rendered.then(function() {
                var descField = self.select('descriptionSelector').length,
                    radiusField = self.select('radiusSelector'),
                    latitudeField = self.select('latSelector'),
                    longitudeField = self.select('lonSelector'),
                    name = self.attr.property.title,
                    radiusInvalid = radiusField.length && (isNaN(value.radius) || value.radius <= 0),
                    latitudeInvalid = invalidRange(value.latitude, 90),
                    longitudeInvalid = invalidRange(value.longitude, 180),
                    valid = !radiusInvalid && !latitudeInvalid && !longitudeInvalid;

                radiusField.toggleClass('invalid', radiusInvalid);
                latitudeField.toggleClass('invalid', latitudeInvalid);
                longitudeField.toggleClass('invalid', longitudeInvalid);

                return valid && F.vertex.singlePropValid(value, name);
            })
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
                                self.select('latSelector').val(result.latitude)
                                    .add(self.select('lonSelector').val(result.longitude))
                                    .animatePop().done(function() {
                                        self.$node.find('.radius').focus();
                                        self.triggerFieldUpdated();
                                    })

                                return result.name;
                            }
                            return item;
                        }
                    });
                }
        }
    }
});
