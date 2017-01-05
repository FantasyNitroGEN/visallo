
define([
    'flight/lib/component',
    './withVertexPopover'
], function(
    defineComponent,
    withVertexPopover) {
    'use strict';

    return defineComponent(ControlDragPopover, withVertexPopover);

    function ControlDragPopover() {

        this.defaultAttrs({
            buttonSelector: 'button'
        });

        this.before('initialize', function(node, config) {
            config.template = 'controlDragPopover';
        });

        this.after('initialize', function() {
            this.on('click', {
                buttonSelector: this.onButton
            });
        });

        this.onButton = function(event) {
            var component = $(event.target).data('component'),
                attach = ComponentPopover => {
                    ComponentPopover.teardownAll();
                    ComponentPopover.attachTo(this.node, this.attr);
                    this.teardown();
                }

            if (component === 'createConnectionPopover') {
                require(['./createConnectionPopover'], attach);
            } else if (component === 'findPathPopover') {
                require(['./findPathPopoverShim'], attach);
            }
        }

        this.getTemplate = function() {
            return new Promise(f => require(['./controlDragPopoverTpl'], f));
        };
    }

});
