
define([
    'flight/lib/component',
    './dateTpl.hbs',
    './dateTimezone.hbs',
    'util/vertex/formatters',
    './withPropertyField',
    './withHistogram',
    'util/popovers/withElementScrollingPositionUpdates'
], function(
    defineComponent,
    template,
    timezoneTemplate,
    F,
    withPropertyField,
    withHistogram,
    withPositionUpdates) {
    'use strict';

    return defineComponent(DateField, withPropertyField, withHistogram, withPositionUpdates);

    function DateField() {

        this.defaultAttrs({
            timeFieldSelector: '.timepicker',
            timezoneSelector: '.timezone',
            preventChangeHandler: true
        });

        this.before('initialize', function(node, config) {
            config.focus = false;
        });

        this.after('initialize', function() {
            var self = this,
                dateString = '',
                timeString = '';

            this.displayTime = this.attr.property.displayType !== 'dateOnly';

            this.$node.html(template({
                dateString: dateString,
                timeString: timeString,
                today: F.date.dateString(new Date()),
                todayTime: F.date.timeString(new Date()),
                displayTime: this.displayTime
            }));

            this.updateTimezone();

            this.select('timeFieldSelector').timepicker({
                template: false,
                showInputs: false,
                showSeconds: false,
                minuteStep: 15,
                defaultTime: timeString || false,
                disableMousewheel: true
            })

            this.on('click', {
                timezoneSelector: this.onTimezoneOpen
            });
            this.on('selectTimezone', this.onSelectTimezone);
            this.updateTimezone();

            const input = this.select('inputSelector').eq(0)

            // Set this value once here so the datepicker can open
            // to selected month and highlight the day
            if (this.attr.value) {
                if (this.displayTime) {
                    input.val(F.date.dateString(this.attr.value))
                } else {
                    input.val(F.date.dateStringUtc(this.attr.value))
                }
            }

            input.datepicker().on('changeDate', () => {
                this.triggerFieldUpdated();
            })
        });

        this.getValue = function() {
            const input = this.select('inputSelector').eq(0);
            const dateStr = input.val();

            if (this.displayTime) {
                var timeField = input.next('input.timepicker'),
                    timeVal = timeField.val();

                if (dateStr && timeVal) {
                    return F.timezone.dateTimeStringServer(dateStr + ' ' + timeVal, this.currentTimezone.name);
                }
            } else if (dateStr) {
                return F.date.dateStringServer(dateStr);
            }
        };

        this.getMetadata = function() {
            return this.currentTimezoneMetadata;
        }

        this.setValue = function(value) {
            let dateString, timeString;

            if (value) {
                var millis = _.isNumber(value) ? value : undefined,
                    date;

                if (_.isUndefined(millis) && _.isString(value) && value.length) {
                    if ((/^-?[0-9]+$/).test(value)) {
                        millis = parseInt(value, 10);
                    } else {
                        var looksLikeCorrectFormat = (/^\d+-\d+-\d+ \d+:\d+$/).test(value);
                        if (looksLikeCorrectFormat) {
                            var parsed = F.timezone.dateInServerFormat(value, 'Etc/UTC');
                            if (parsed) {
                                date = parsed.toDate();
                            }
                        } else {
                            date = F.date.looslyParseDate(value);
                        }
                        if (date) {
                            millis = date.getTime();
                        }
                    }
                } else if (value instanceof Date) {
                    dateString = F.date.dateString(value.getTime());
                } else if (isNaN(new Date(millis).getTime())) {
                    millis = null;
                }

                if (millis) {
                    if (this.displayTime) {
                        var fromZoneName = F.timezone.currentTimezone().name,
                            toZoneName = this.currentTimezone ?
                                this.currentTimezone.name :
                                fromZoneName;

                        if (fromZoneName !== toZoneName) {
                            millis = F.timezone.dateTimeStringToTimezone(millis, fromZoneName, toZoneName);
                        }
                        dateString = F.date.dateString(millis);
                        timeString = F.date.timeString(millis);
                    } else {
                        dateString = F.date.dateStringUtc(millis);
                    }
                }

            }
            this.select('inputSelector').eq(0).val(dateString);
            if (this.displayTime) {
                if (timeString) {
                    // Unable to change how am/pm is shown in timepicker
                    // so uppercase to match
                    timeString = timeString.toUpperCase()
                }
                this.select('timeFieldSelector').val(timeString);
            }
        };

        this.isValid = function(value) {
            return _.isString(value) && value.length && F.date.local(value);
        };

        this.onSelectTimezone = function(event, data) {
            if (data.name) {
                this.updateTimezone(data);
                this.triggerFieldUpdated();
            }
        };

        this.updateTimezone = function(tz) {
            if (this.displayTime) {

                var dateStringValue = this.getValue(),
                    date = dateStringValue && new Date(dateStringValue);

                if (tz) {
                    if (!_.isString(tz)) {
                        tz = tz.name;
                    }
                    this.currentTimezone = F.timezone.lookupTimezone(tz, date);
                } else {
                    if (!this.currentTimezone) {
                        this.currentTimezone = F.timezone.currentTimezone();
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
    }
});
