define([
    'flight/lib/component',
    './restrictValues',
    'util/vertex/formatters',
    './withPropertyField',
    'hbs!./restrictValuesMultiTpl'
], function(defineComponent, RestrictValues, F, withPropertyField, template) {
    'use strict';

    return defineComponent(RestrictValuesMultiField, withPropertyField);

    function RestrictValuesMultiField() {

        function compare(v1, v2) {
            return v1.toLowerCase().localeCompare(v2.toLowerCase());
        }

        this.defaultAttrs({
            removeSelector: 'ul.values li .remove-value'
        });

        this.after('initialize', function() {
            var self = this;

            this.on('click', {
                removeSelector: onRemoveItemClick
            });

            this.$node.html(template({}));

            var $restrictValues = this.$node.find('.restrict-values');
            RestrictValues.attachTo($restrictValues, {
                property: this.attr.property,
                value: '',
                preventChangeHandler: true,
                placeholderKey: 'field.restrict_values_multi.form.placeholder'
            });
            $restrictValues.on('change', onRestrictValueChange);

            this.setValue(this.attr.value);

            function onRestrictValueChange(event) {
                var value = $(event.target).val();
                if (self.addValue(value)) {
                    $restrictValues.trigger('setValue', null);
                    self.trigger('propertychange', {
                        propertyId: self.attr.property.title,
                        value: self.getValue(),
                        metadata: _.isFunction(self.getMetadata) && self.getMetadata() || {},
                        options: {}
                    });
                }
            }

            function onRemoveItemClick(event) {
                var $item = $(event.target).parent();
                var value = $item.data('value');
                $item.remove();
                self.removeValue(value);
            }
        });

        this.addValue = function(value) {
            if (this.values.indexOf(value) === -1) {
                this.values.push(value);
                return true;
            } else {
                return false;
            }
        };

        this.removeValue = function(value) {
            this.values.splice(this.values.indexOf(value), 1);
            this.trigger('propertychange', {
                propertyId: this.attr.property.title,
                value: this.getValue(),
                metadata: _.isFunction(this.getMetadata) && this.getMetadata() || {},
                options: {}
            });
        };

        this.setValue = function(values) {
            var self = this;
            this.values = [];
            this.$node.find('ul.values').empty();

            values.sort(compare).forEach(function(value) {
                if (self.addValue(value)) {
                    self.$node.find('ul.values')
                        .append(
                            '<li><div>' + F.vertex.propDisplay(self.attr.property.title, value) +
                            '</div><button class="remove-value remove-icon">x</button></li>');
                    self.$node.find('ul.values li').last().data('value', value);
                }
            });
        };

        this.getValue = function() {
            return this.values.slice(0);
        };

        this.isValid = function(values) {
            return values.length;
        };
    }
});
