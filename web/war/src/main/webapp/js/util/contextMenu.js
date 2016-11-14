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
                    var target = event.target;
                    state++;
                    if (!state) return;

                    switch (state) {
                        case 1:
                            var originalTabindex = target.getAttribute('tabindex'),
                                targetIsField = $(target).is('input,select,textarea'),
                                disableCheck = targetIsField,
                                handler;

                            blurPromise = new Promise(function(v) {
                                    if (disableCheck) {
                                        v(false);
                                    } else {
                                        target.setAttribute('tabindex', -1);
                                        _.delay(function() {
                                            v(false);
                                        }, 100);
                                        self.on(target, 'blur', handler = function blurHandler(blurEvent) {
                                            self.trigger(target, 'hideMenu');
                                            v(true);
                                        });
                                    }
                                })
                                .tap(function() {
                                    if (!disableCheck) {
                                        self.off(target, 'blur', handler);
                                        if (originalTabindex) {
                                            target.setAttribute('tabindex', originalTabindex);
                                        } else {
                                            target.removeAttribute('tabindex');
                                        }
                                    }
                                });
                            break;

                        case 2:
                            if (blurPromise) {
                                blurPromise.done(function(menuBlocked) {
                                    if (menuBlocked) {
                                        contextMenuBlocked = true;
                                        self.trigger(target, 'warnAboutContextMenuDisabled');
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
                if ($(event.target).closest('.dropdown-menu').length) return;
                reset();
                downPosition = [event.pageX, event.pageY];
                altKey = event.altKey;
                ctrlKey = event.ctrlKey;
                state = 0;
            }, true);

            document.addEventListener('mouseup', function(event) {
                if ($(event.target).closest('.dropdown-menu').length) return;
                progressContextMenu(event);
            }, true);

            document.addEventListener('click', function(event) {
                if (event.which === 3) return;
                if ($(event.target).closest('.dropdown-menu').length) return;

                if (altKey || (ctrlKey && !contextMenuBlocked)) {
                    event.stopPropagation();
                    self.triggerContextMenu(event);
                }
                reset();
            }, true);

            var delay;

            document.addEventListener('contextmenu', function(event) {
                if ($(event.target).closest('.dropdown-menu').length) return;
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
