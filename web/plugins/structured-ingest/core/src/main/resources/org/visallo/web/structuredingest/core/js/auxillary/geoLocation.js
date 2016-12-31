define([
    'flight/lib/component',
    'hbs!../../templates/auxillary/geoLocation'
], function(
    defineComponent,
    template) {
    'use strict';

    return defineComponent(GeoLocationAuxillary);

    function GeoLocationAuxillary() {

        this.defaultAttrs({
            containsSelector: 'select.contains',
            otherSectionSelector: '.otherSection',
            otherColumnSelector: 'select.otherColumn',
            formatSelector: 'select.format'
        });

        this.after('initialize', function() {
            var self = this,
                column = this.attr.mapping.column,
                hints = this.attr.mapping.hints;

            this.$node.html(template({
                otherColumn: self.attr.allHeaders.map(function(header, i) {
                    return {
                        value: i,
                        display: header,
                        selected:
                            column === hints.columnLatitude ?
                                hints.columnLongitude === i :
                            column === hints.columnLongitude ?
                                hints.columnLatitude === i : false,
                        disabled: i === column
                    };
                }),
                contains: [
                    { value: 'both', display: 'Latitude and Longitude' },
                    { value: 'latitude', display: 'Latitude' },
                    { value: 'longitude', display: 'Longitude' }
                ].map(function(c) {
                    if ('columnLatitude' in hints &&
                        c.value === 'latitude' &&
                        column === hints.columnLatitude) {
                        c.selected = true;
                    } else if ('columnLongitude' in hints &&
                        c.value === 'longitude' &&
                        column === hints.columnLongitude) {
                        c.selected = true;
                    } else if (c.value === 'both') {
                        c.selected = true;
                    }
                    return c;
                }),
                formats: [
                    { value: 'DEGREES_MINUTES_SECONDS', display: '0° 0\' 00.0" (degrees, minutes, seconds)' },
                    { value: 'DEGREES_DECIMAL_MINUTES', display: '0° 00.000\'  (degrees, decimal minutes)' },
                    { value: 'DECIMAL', display: '00.000°     (decimal degrees)' }
                ].map(function(f) {
                    if (f.value === hints.format) {
                        f.selected = true;
                    }
                    return f;
                })
            }));

            this.on('change', {
                containsSelector: this.onContainsChange,
                otherColumnSelector: this.onOtherColumnChange,
                formatSelector: this.onFormatChange
            })

            this.triggerChange();
        });

        this.onOtherColumnChange = function(event) {
            this.triggerChange();
        };

        this.onContainsChange = function(event) {
            this.triggerChange();
        };

        this.onFormatChange = function(event) {
            this.triggerChange();
        };

        this.triggerChange = function() {
            var self = this,
                contains = this.select('containsSelector').val(),
                otherColumn = this.select('otherColumnSelector').val(),
                otherColumnNumber = parseInt(otherColumn, 10);

            if (isNaN(otherColumnNumber)) {
                otherColumnNumber = undefined;
            }

            this.trigger('addAuxillaryData', _.tap({
                format: this.select('formatSelector').val()
            }, function(data) {
                if (contains === 'latitude') {
                    data.columnLatitude = self.attr.mapping.column
                    data.columnLongitude = otherColumnNumber;
                    self.$node.find('.otherType').text('Longitude')
                }
                if (contains === 'longitude') {
                    data.columnLatitude = otherColumnNumber;
                    data.columnLongitude = self.attr.mapping.column
                    self.$node.find('.otherType').text('Latitude')
                }
                self.select('otherSectionSelector').toggle(contains !== 'both');
            }));
        };

    }
});
