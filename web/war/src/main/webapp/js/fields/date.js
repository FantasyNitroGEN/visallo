
define([
    'flight/lib/component',
    'hbs!./dateTpl',
    'hbs!./dateTimezone',
    './withPropertyField',
    './withHistogram',
    'util/formatters',
    'util/popovers/withElementScrollingPositionUpdates'
], function(
    defineComponent,
    template,
    timezoneTemplate,
    withPropertyField,
    withHistogram,
    F,
    withPositionUpdates) {
    'use strict';

    return defineComponent(DateField, withPropertyField, withHistogram, withPositionUpdates);

    function DateField() {

        this.defaultAttrs({
            timeFieldSelector: '.timepicker',
            timezoneSelector: '.timezone'
        });

        this.before('initialize', function(node, config) {
            config.focus = false;
        });

        this.after('initialize', function() {
            var self = this,
                dateString = '',
                timeString = '';

            this.displayTime = this.attr.property.displayType !== 'dateOnly';

            if (this.attr.value) {
                var millis = _.isNumber(this.attr.value) ? this.attr.value : undefined;

                if (_.isUndefined(millis)) {
                    var date = F.date.looslyParseDate(this.attr.value);
                    if (date) {
                        millis = date.getTime();
                    }
                } else if (isNaN(new Date(millis).getTime())) {
                    millis = null;
                }

                if (millis) {
                    dateString = F.date.dateStringUtc(millis);
                    timeString = F.date.timeString(millis);
                } else {
                    this.attr.value = '';
                }

            }

            this.$node.html(template({
                dateString: dateString,
                timeString: timeString,
                today: F.date.dateString(new Date()),
                todayTime: F.date.timeString(new Date()),
                displayTime: this.displayTime,
                predicates: this.attr.predicates
            }));

            this.updateRangeVisibility();
            this.updateTimezone();

            this.getValues = function() {
                var inputs = this.$node.hasClass('alternate') ?
                        this.$node.find('.input-row input') :
                        this.select('visibleInputsSelector'),
                    values = inputs.map(function() {
                        return $(this).val();
                    }).toArray();

                if (this.displayTime && values.length > 1) {
                    var newValues = [], i;
                    for (i = 0; i < values.length; i += 2) {
                        newValues.push(values[i] + ' ' + values[i + 1]);
                    }
                    values = newValues;
                }

                return values.map(function(v) {
                    if (self.displayTime) {
                        return F.timezone.dateTimeStringToUtc(v, self.currentTimezone.name);
                    }
                    return v;
                });
            };

            this.select('timeFieldSelector').timepicker({
                template: false,
                showInputs: false,
                minuteStep: 15,
                defaultTime: timeString || false,
                showMeridian: false
            });

            this.on('change keyup', {
                    inputSelector: function() {
                        this.updateRangeVisibility();
                        this.updateTimezone();
                    }
                });

            this.$node.find('input').on('paste', function(event) {
                var $this = $(this);

                $this.datepicker('hide');
                $this[0].select();

                _.delay(function() {
                    var pasted = $this.val();

                    if (pasted) {
                        var date = F.date.looslyParseDate(pasted);
                        if (date) {
                            $this.val(F.date.dateString(date));
                            $this.datepicker('setDate', date);
                            $this.next('input.timepicker').timepicker('setTime', date);
                            $this.datepicker('update');
                            $this.blur();
                        }
                    }
                }, 500)
            });

            this.on('click', {
                timezoneSelector: this.onTimezoneOpen
            });

            this.on('selectTimezone', this.onSelectTimezone);
            this.updateTimezone();
        });

        this.triggerFieldUpdated = function() {
            if (this.isValid()) {
                var values = this.getValues(),
                    predicate = this.select('predicateSelector').val();
                if (this.displayTime) {
                    // apply seconds to the time
                    if (predicate === '=') {
                        // turn into a range across all seconds in this minute
                        predicate = 'range';
                        values[1] = values[0] + ':59';
                        values[0] += ':00';
                    } else if (predicate === 'range') {
                        values[0] += ':00';
                        values[1] += ':59';
                    } else if (predicate === '<') {
                        values[0] += ':00';
                    } else if (predicate === '>') {
                        values[0] += ':59';
                    }
                }
                this.filterUpdated(
                    values,
                    predicate,
                    {
                        metadata: this.currentTimezoneMetadata
                    }
                );
            }
        };

        this.onSelectTimezone = function(event, data) {
            if (data.name) {
                this.updateTimezone(data);
            }
        };

        this.updateTimezone = function(tz) {
            if (this.displayTime) {
                var values = this.getValues(),
                    date = (values && values[0]) ? new Date(values[0]) : null,
                    shiftTime = tz && tz.shiftTime;

                if (tz) {
                    if (!_.isString(tz)) {
                        tz = tz.name;
                    }
                    if (shiftTime) {
                        var inputs = this.$node.find('input');

                        if (values && values[0] && inputs.length > 1) {
                            date = F.timezone.date(values[0], 'Etc/UTC');
                            inputs.eq(0).val(date.toString('yyyy-MM-dd', tz)).datepicker('update');
                            inputs.eq(1).data('timepicker').setTime(date.toString('HH:mm', tz));
                        } else if (values && values[1] && inputs.length > 3) {
                            date = F.timezone.date(values[1], 'Etc/UTC');
                            inputs.eq(2).val(date.toString('yyyy-MM-dd', tz)).datepicker('update');
                            inputs.eq(3).data('timepicker').setTime(date.toString('HH:mm', tz));
                        }
                    }
                    this.currentTimezone = F.timezone.lookupTimezone(tz, date.getTime());
                } else {
                    if (!this.currentTimezone) {
                        this.currentTimezone = F.timezone.currentTimezone(date);
                    } else {
                        this.currentTimezone = F.timezone.lookupTimezone(this.currentTimezone.name, date);
                    }
                }

                this.currentTimezoneMetadata = {
                    'http://visallo.org#sourceTimezone': this.currentTimezone.name,
                    'http://visallo.org#sourceTimezoneOffset': this.currentTimezone.offset,
                    'http://visallo.org#sourceTimezoneOffsetDst': this.currentTimezone.tzOffset
                };

                this.select('timezoneSelector').replaceWith(
                    timezoneTemplate(this.currentTimezone)
                );

            }

            this.triggerFieldUpdated();
        };

        this.onTimezoneOpen = function(event) {
            var self = this,
                $target = $(event.target).closest('.timezone');

            event.preventDefault();

            if (!this.Timezone) {
                require(['util/popovers/timezone/timezone'], function(Timezone) {
                    self.Timezone = Timezone;
                    self.onTimezoneOpen(event);
                });
                return;
            }

            if ($target.lookupComponent(this.Timezone)) {
                return;
            }

            this.Timezone.attachTo($target, {
                scrollSelector: '.content',
                timezone: this.currentTimezone.name,
                sourceTimezone: this.attr.vertexProperty &&
                    this.attr.vertexProperty['http://visallo.org#sourceTimezone']
            });
        };

        this.isValid = function() {
            var self = this,
                dateRegex = /^\s*\d{4}-\d{1,2}-\d{1,2}\s*$/,
                dateTimeRegex = /^\s*\d{4}-\d{1,2}-\d{1,2}\s*\d{1,2}:\d{1,2}\s*$/;

            return _.every(this.getValues(), function(v, i) {
                if (self.displayTime) {
                    return dateTimeRegex.test(v);
                } else {
                    return dateRegex.test(v);
                }
            });
        };
    }
});
