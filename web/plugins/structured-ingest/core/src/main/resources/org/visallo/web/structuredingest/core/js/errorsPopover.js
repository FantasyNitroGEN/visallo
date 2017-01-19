
define([
    'flight/lib/component',
    'util/popovers/withPopover'
], function(
    defineComponent,
    withPopover) {
    'use strict';

    var STRATEGIES = [
        { value: '', display: 'Errors Stop Import' },
        { value: 'SKIP_CELL', display: 'Errors Discard Cell' },
        { value: 'SKIP_VERTEX', display: 'Errors Discard Entity' },
        { value: 'SKIP_ROW', display: 'Errors Discard Row' }
    ];

    return defineComponent(ErrorsPopover, withPopover);

    function ErrorsPopover() {

        this.defaultAttrs({
            selectSelector: 'select'
        })

        this.before('initialize', function(node, config) {
            config.template = '/org/visallo/web/structuredingest/core/templates/error'
            config.hideStrategy = _.some(config.errors.list, function(e) {
                return e.mappingError === true;
            });
            config.hasRow = 'rowIndex' in config;
            config.errorStrategies = STRATEGIES.map(function(s) {
                s.selected = config.errorStrategy === s.value;
                return s;
            })
        });

        this.after('initialize', function() {
            this.after('setupWithTemplate', function() {
                this.on(this.popover, 'change', {
                    selectSelector: this.onChange
                });
            })
        })

        this.onChange = function(event) {
            var type = $(event.target).val();
            this.trigger('errorHandlingUpdated', {
                key: this.attr.key,
                errorStrategy: type
            });
        }
    }
})

