define([
    'flight/lib/component',
    'util/formatters',
    './withPropertyField',
    './dateRelativeTpl.hbs'
], function(
    defineComponent,
    F,
    withPropertyField,
    template) {
    'use strict';

    // Mapping to java.util.Calendar constants
    var UNITS_TIME = {
          second: 13,
          minute: 12,
          hour: 10,
          day: 5,
          week: 3,
          month: 2,
          year: 1
        },
        UNITS = _.omit(UNITS_TIME, 'second', 'minute', 'hour');

    return defineComponent(DateRelative, withPropertyField);

    function makeNumber(v) {
        return parseInt(v, 10);
    }

    function DateRelative() {

        this.defaultAttrs({
            amountSelector: '.amount',
            unitSelector: '.unit',
            pastPresentSelector: '.past-or-present'
        });

        this.after('initialize', function() {
            var self = this;

            this.displayTime = this.attr.property.displayType !== 'dateOnly';
            this.$node.html(template({
                units: this.getUnits()
            }));

            _.defer(function() {
                self.select('amountSelector').focus();
            })
        });

        this.calculateRelativeDate = function(value) {
            var now = new Date();

            switch (value.unit) {
                case UNITS.second:
                    now.setSeconds(now.getSeconds() + value.amount);
                    break;
                case UNITS.minute:
                    now.setMinutes(now.getMinutes() + value.amount);
                    break;
                case UNITS.hour:
                    now.setHours(now.getHours() + value.amount);
                    break;
                case UNITS.day:
                    now.setDate(now.getDate() + value.amount);
                    break;
                case UNITS.week:
                    now.setDate(now.getDate() + value.amount * 7);
                    break;
                case UNITS.month:
                    now.setMonth(now.getMonth() + value.amount);
                    break;
                case UNITS.year:
                    now.setFullYear(now.getFullYear() + value.amount);
                    break;
            }

            return now.getTime();
        };

        this.getValue = function() {
            var amount = makeNumber($.trim(this.select('amountSelector').val())),
                past = makeNumber($.trim(this.select('pastPresentSelector').val())),
                value = {
                    unit: makeNumber($.trim(this.select('unitSelector').val()))
                };

            if (!isNaN(amount) && !isNaN(past)) {
                value.amount = amount * past;

                var date = this.calculateRelativeDate(value);
                if (this.displayTime) {
                    value._date = F.date.dateStringUtc(date) + ' ' + F.date.timeStringUtc(date);
                } else {
                    value._date = F.date.dateString(date);
                }
            }

            return value;
        };

        this.setValue = function(value) {
            var inFuture = value && !isNaN(value.amount) && value.amount >= 0,
                defaultUnit = this.displayTime ? UNITS_TIME.hour : UNITS.day;

            this.select('amountSelector')
                .val((!value || isNaN(value.amount)) ? '' : Math.abs(value.amount));
            this.select('unitSelector')
                .val((!value || isNaN(value.unit)) ? defaultUnit : value.unit);
            this.select('pastPresentSelector')
                .val(inFuture ? '1' : '-1');
        };

        this.isValid = function(value) {
            return value &&
                _.isNumber(value.unit) &&
                !isNaN(value.unit) &&
                _.isNumber(value.amount) &&
                !isNaN(value.amount);
        };

        this.getUnits = function() {
            return _.chain(this.displayTime ? UNITS_TIME : UNITS)
                .map(function(value, key) {
                    return {
                        display: i18n('field.date.relative.unit.' + key),
                        value: value,
                        selected: false
                    }
                })
                .sortBy('value')
                .value();
        };

    }
});
