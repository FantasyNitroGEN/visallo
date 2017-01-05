
define([
    'flight/lib/component',
    './booleanTpl.hbs',
    './withPropertyField',
    'util/formatters'
], function(defineComponent, template, withPropertyField, F) {
    'use strict';

    return defineComponent(BooleanField, withPropertyField);

    function makeNumber(v) {
        return parseFloat(v, 10);
    }

    function BooleanField() {

        this.before('initialize', function(node, config) {
            config.disableTooltip = true;
        })

        this.after('initialize', function() {
            this.$node.html(template({
                value: this.attr.value,
                display: i18n(true, 'field.boolean.' + this.attr.value + '.' + this.attr.property.title) || F.boolean.pretty(this.attr.value)
            }));
        });

        this.getValue = function() {
            return this.select('inputSelector').prop('checked') ? 'true' : 'false';
        };

        this.setValue = function(value) {
            this.select('inputSelector').prop('checked', value === true || value === 'true');
            this.update(value);
        };

        this.update = function(value) {
            this.$node.find('span').text(
                i18n(true, 'field.boolean.' + value + '.' + this.attr.property.title) ||
                (!this.attr.onlySearchable && this.attr.property.displayName) ||
                F.boolean.pretty(value)
            );
        };

        this.isValid = function() {
            return true;
        };
    }
});
