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
        var contextMenuBlocked = false;

        this.after('initialize', function() {
            var self = this,
                altKey = false,
                ctrlKey = false,
                downPosition,
                state = 0,
                blurPromise,
                reset = function() {
                    altKey = false;
                    ctrlKey = false;
                    state = 0;
                    blurPromise = null;
                },
                progressContextMenu = function(event) {
                    state++;
                    if (!state) return;

                    switch (state) {
                        case 1:
                            var originalTabindex = event.target.getAttribute('tabindex'),
                                handler;
                            event.target.setAttribute('tabindex', -1);
                            blurPromise = new Promise(function(v) {
                                _.delay(function() {
                                    self.off(event.target, 'blur', handler);
                                    v(false);
                                }, 100);
                                self.on(event.target, 'blur', handler = function blurHandler(blurEvent) {
                                    self.trigger(event.target, 'hideMenu');
                                    self.off(event.target, 'blur', blurHandler);
                                    if (originalTabindex) {
                                        event.target.setAttribute('tabindex', originalTabindex);
                                    } else {
                                        event.target.removeAttribute('tabindex');
                                    }
                                    v(true);
                                });
                            })
                            break;

                        case 2:
                            if (blurPromise) {
                                blurPromise.done(function(menuBlocked) {
                                    if (menuBlocked) {
                                        contextMenuBlocked = true;
                                        self.trigger(event.target, 'warnAboutContextMenuDisabled');
                                    }
                                    if (!contextMenuBlocked && distance([event.pageX, event.pageY], downPosition) < 20) {
                                        self.triggerContextMenu(event);
                                    }
                                    _.delay(reset, 250);
                                })
                            }
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
                if (event.which === 3) return;

                if (altKey || (ctrlKey && !contextMenuBlocked)) {
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
            this.trigger(
                event.target,
                'showMenu',
                _.pick(event, 'type', 'originalEvent', 'pageX', 'pageY')
            );
        }

    }
});
