
define([
    'flight/lib/component',
    'tpl!./string',
    'util/vertex/formatters',
    './withPropertyField'
], function(defineComponent, template, F, withPropertyField) {
    'use strict';

    return defineComponent(StringField, withPropertyField);

    function StringField() {

        this.before('initialize', function(node, config) {
            config.defaultPredicate = '~';
        })

        this.after('initialize', function() {
            var self = this;

            this.$node.html(template(this.attr));

            this.on('change keyup', {
                inputSelector: function(event) {
                    this.triggerFieldUpdated();
                }
            });
        });

        this.triggerFieldUpdated = function() {
            this.filterUpdated(
                this.getValues(),
                this.select('predicateSelector').val()
            );
        };

        this.isValid = function() {
            var name = this.attr.property.title,
                values = this.getValues();

            return _.every(this.getValues(), function(v) {
                return $.trim(v).length > 0 && F.vertex.singlePropValid(v, name);
            });
        };
    }
});
