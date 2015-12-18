define([
    'flight/lib/component',
    'd3',
    'hbs!./text-overview-config-tpl'
], function(
    defineComponent,
    d3,
    template) {
    'use strict';

    var LIMITS = [
            { value: '', display: i18n('dashboard.text-overview.limits.none') },
            { value: '1', display: 1 },
            { value: '2', display: 2 },
            { value: '5', display: 5 },
            { value: '~', display: i18n('dashboard.text-overview.limits.other') }
        ],
        noColorDefault = 'white';

    return defineComponent(TextOverviewConfig);

    function TextOverviewConfig() {

        this.attributes({
            item: null,
            extension: null,
            limitSelector: 'select',
            limitOtherSelector: 'input',
            colorPickerSelector: '.color-picker li'
        })

        this.after('initialize', function() {
            var self = this,
                config = this.getConfiguration();

            this.on('change', {
                limitSelector: this.onChangeLimit,
                limitOtherSelector: this.onChangeOtherLimit
            });

            this.on('click', {
                colorPickerSelector: this.onPickColor
            });

            this.on('keyup', {
                limitOtherSelector: this.onChangeOtherLimit
            });

            if (!config.color) {
                config.color = noColorDefault;
            }
            this.$node.html(template({
                showOther: this.isOther(config.limit),
                currentLimit: config.limit,
                colors: _.chain(d3.scale.category10().range())
                    .map(function(c) {
                        return { color: c, noColor: false };
                    })
                    .tap(function(colors) {
                        colors.push({ color: noColorDefault, noColor: true })
                    })
                    .map(function(c) {
                        var match = config.color === c.color;
                        return _.extend({ selected: match }, c);
                    })
                    .value(),
                limits: LIMITS.map(function(l) {
                    return _.extend({
                        selected: (self.isOther(config.limit) && l.value === '~') ||
                            String(config.limit) === l.value
                    }, l);
                })
            }));
        });

        this.getConfiguration = function() {
            var config = this.attr.item.configuration,
                key = 'org-visallo-text-overview';

            if (!config[key]) {
                config[key] = {};
            }

            return config[key];
        };

        this.onPickColor = function(event) {
            var color = $(event.target).data('color');

            event.stopPropagation();

            this.getConfiguration().color = color;
            this.triggerChange();
        };

        this.isOther = function(limit) {
            return Boolean(limit && (
                limit === '~' ||
                _.every(LIMITS, function(l) {
                    return l.value !== String(limit);
                })
            ));
        };

        this.onChangeOtherLimit = function(event) {
            var other = parseInt($(event.target).val(), 10);

            if (!isNaN(other)) {
                this.getConfiguration().limit = other;
                if (event.type === 'change' || (event.type === 'keyup' && event.which === 13)) {
                    this.triggerChange();
                }
            }
        };

        this.onChangeLimit = function(event) {
            var newLimit = $(event.target).val(),
                showOther = this.isOther(newLimit),
                config = this.getConfiguration();

            this.select('limitOtherSelector')
                .val(config.limit)
                .toggle(showOther);
            if (showOther) {
                return;
            }

            if (newLimit) {
                config.limit = parseInt(newLimit, 10);
            } else {
                delete config.limit;
            }

            this.triggerChange();
        };

        this.triggerChange = function() {
            this.trigger('configurationChanged', {
                extension: this.attr.extension,
                item: this.attr.item
            });
        };

    }
});
