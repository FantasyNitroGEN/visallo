define([
    'flight/lib/component'
], function(defineComponent) {
    'use strict';

    return defineComponent(ContextMenu);

    function distance(p1, p2) {
        return Math.abs(
            Math.sqrt(
                Math.pow(p2[0] - p1[0], 2) +
                Math.pow(p2[1] - p1[1], 2)
            )
        );
    }

    function ContextMenu() {
        var hideMenu = false,
            testedForBlur = false;

        this.after('initialize', function() {
            var self = this,
                altKey = false,
                ctrlKey = false,
                downPosition,
                queueContextMenuEvent,
                state = 0,
                reset = function() {
                    altKey = false;
                    ctrlKey = false;
                    state = 0;
                    queueContextMenuEvent = false;
                },
                progressContextMenu = function(event) {
                    state++;
                    if (!state) return;

                    switch (state) {
                        case 1:
                            queueContextMenuEvent = true;
                            if (testedForBlur || hideMenu) {
                                return;
                            }

                            var originalTabindex = event.target.getAttribute('tabindex'),
                                handler;
                            event.target.setAttribute('tabindex', -1);
                            _.delay(function() {
                                self.off(event.target, 'blur', handler);
                            }, 500);
                            self.on(event.target, 'blur', handler = function blurHandler(blurEvent) {
                                queueContextMenuEvent = false;
                                hideMenu = true;
                                self.trigger(event.target, 'hideMenu');
                                self.off(event.target, 'blur', blurHandler);
                                if (originalTabindex) {
                                    event.target.setAttribute('tabindex', originalTabindex);
                                } else {
                                    event.target.removeAttribute('tabindex');
                                }
                            });
                            break;

                        case 2:
                            if (queueContextMenuEvent && !hideMenu && distance([event.pageX, event.pageY], downPosition) < 20) {
                                self.triggerContextMenu(event);
                            }
                            _.delay(reset, 250);
                            break;
                    }
                };

            document.addEventListener('mousedown', function(event) {
                reset();
                downPosition = [event.pageX, event.pageY];
                altKey = event.altKey;
                ctrlKey = event.ctrlKey;
                state = 0;
            }, true);

            document.addEventListener('mouseup', function(event) {
                progressContextMenu(event);
            }, true);

            document.addEventListener('click', function(event) {
                if (altKey || (ctrlKey && !hideMenu)) {
                    event.stopPropagation();
                    self.triggerContextMenu(event);
                }
                reset();
            }, true);

            var delay;

            document.addEventListener('contextmenu', function(event) {
                progressContextMenu(event);
            }, true);
        });

        this.triggerContextMenu = function(event) {
            testedForBlur = true;
            this.trigger(
                event.target,
                'showMenu',
                _.pick(event, 'type', 'originalEvent', 'pageX', 'pageY')
            );
        }

    }
});
