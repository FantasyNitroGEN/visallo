
define([
    'flight/lib/component',
    'tpl!./string',
    './withPropertyField'
], function(defineComponent, template, withPropertyField) {
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
                    if (event.which === 13 || event.type === 'change') {
                        this.triggerFieldUpdated();
                    }
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
            return _.every(this.getValues(), function(v) {
                return $.trim(v).length > 0;
            });
        };
    }
});
