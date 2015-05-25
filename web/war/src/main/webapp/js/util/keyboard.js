
define([
    'flight/lib/component'
], function(defineComponent) {
    'use strict';

    return defineComponent(Keyboard);

    function shouldFilter(e) {
        return $(e.target).is('input,select,textarea:not(.clipboardManager)');
    }

    function eventParts(e) {
        return _.pick(e, 'which', 'shiftKey', 'metaKey', 'ctrlKey', 'altKey');
    }

    function Keyboard() {
        this.after('teardown', function() {
            document.removeEventListener('mousedown', this.onDocumentMouseDown);
        });

        this.after('initialize', function() {
            this.shortcutsByScope = {};
            this.shortcuts = {};
            this.focusElementStack = [];

            window.visalloKeyboard = {};
            window.lastMousePositionX = this.mousePageX = $(window).width() / 2;
            window.lastMousePositionY = this.mousePageY = $(window).height() / 2;

            this.fireEventMetas = this.fireEvent;

            this.fireEventUp = _.debounce(this.fireEvent.bind(this), 100);
            this.fireEvent = _.debounce(this.fireEvent.bind(this), 100, true);

            this.on('keydown', this.onKeyDown);
            this.on('keyup', this.onKeyUp);
            this.on('click', this.onClick);
            this.on('mousemove', this.onMouseMove);
            this.on('didToggleDisplay', this.onToggleDisplay);
            this.on('focusLostByClipboard', this.onFocusLostByClipboard);
            this.on('focusComponent', this.onFocus);

            this.on(document, 'requestKeyboardShortcuts', this.onRequestKeyboardShortcuts);
            this.on(document, 'registerKeyboardShortcuts', this.onRegisterKeyboardShortcuts);

            this.onDocumentMouseDown = this.onDocumentMouseDown.bind(this);
            document.addEventListener('mousedown', this.onDocumentMouseDown, true);
        });

        this.onDocumentMouseDown = function(event) {
            window.visalloKeyboard = _.pick(event, 'shiftKey', 'altKey', 'metaKey', 'ctrlKey');
        };

        this.onRequestKeyboardShortcuts = function() {
            this.trigger('keyboardShortcutsRegistered', this.shortcutsByScope);
        };

        this.onToggleDisplay = function(e, data) {
            if (!data.visible) return;
            if (data.name === 'map') {
                this.pushToStackIfNotLast($('.map-pane').get(0));
            } else if (data.name === 'graph') {
                this.pushToStackIfNotLast($('.graph-pane:visible').get(0));
            }
        };

        this.onRegisterKeyboardShortcuts = function(e, data) {
            var self = this,
                scopes = ['Global'],
                shortcuts = this.shortcuts,
                shortcutsByScope = this.shortcutsByScope;

            if (data.scope) {
                if (_.isArray(data.scope)) {
                    scopes = data.scope;
                } else scopes = [data.scope];
            }

            require(['util/formatters'], function(F) {
                scopes.forEach(function(scope) {
                    Object.keys(data.shortcuts).forEach(function(key) {
                        var shortcut = $.extend({}, data.shortcuts[key], F.object.shortcut(key));

                        if (!shortcutsByScope[scope]) shortcutsByScope[scope] = {};
                        shortcuts[shortcut.forEventLookup] = shortcutsByScope[scope][shortcut.normalized] = shortcut;
                    });
                });
            })
        };

        this.onFocus = function(e) {
            this.pushToStackIfNotLast(e.target);
        };

        this.onClick = function(e) {
            this.pushToStackIfNotLast(e.target);
        };

        this.onFocusLostByClipboard = function(e) {
            var $target = $(e.target);

            if ($target.is('.clipboardManager')) return;
            if ($target.closest('.menubar-pane').length) return;

            this.pushToStackIfNotLast(e.target);
        };

        this.shortcutForEvent = function(event) {
            var w = event.which,
                keys = {
                    16: 'shiftKey',
                    17: 'controlKey',
                    18: 'altKey',
                    38: 'up',
                    40: 'down',
                    91: 'metaKey',
                    93: 'metaKey'
                };

            if (keys[w]) {
                return { preventDefault: false, fire: keys[w] };
            }
            if (event.metaKey || event.ctrlKey) {
                return this.shortcuts['CTRL-' + w] || this.shortcuts['META-' + w];
            }
            if (event.altKey) {
                return this.shortcuts['ALT-' + w];
            }
            if (event.shiftKey) {
                return this.shortcuts['SHIFT-' + w];
            }

            return this.shortcuts[w];
        };

        this.onKeyUp = function(e) {
            if (shouldFilter(e)) return;

            var shortcut = this.shortcutForEvent(e);

            this.lastEventParts = null;

            if (shortcut) {
                var f = this.fireEventUp;
                if (shortcut.preventDefault !== false) {
                    e.preventDefault();
                    f = this.fireEventMetas;
                }

                if (!(/META-/.test(shortcut.normalized))) {
                    f.call(this, shortcut.fire, _.pick(e, 'metaKey', 'ctrlKey', 'shiftKey', 'altKey'));
                }
                f.call(this, shortcut.fire + 'Up', _.pick(e, 'metaKey', 'ctrlKey', 'shiftKey', 'altKey'));
            }
        };

        this.onKeyDown = function(e) {
            if (shouldFilter(e)) return;

            var parts = eventParts(e);
            if (this.lastEventParts &&
                _.isEqual(parts, this.lastEventParts) &&
                this.lastEventTimestamp > (e.timeStamp - 1000)) {
                return;
            }

            this.lastEventParts = parts;
            this.lastEventTimestamp = e.timeStamp;

            var shortcut = this.shortcutForEvent(e);

            if (shortcut) {
                var f = this.fireEvent;
                if (shortcut.preventDefault !== false) {
                    e.preventDefault();
                    f = this.fireEventMetas;
                }

                // Ctrl keys don't get keyup events so trigger here
                if (/META-/.test(shortcut.normalized)) {
                    f.call(this, shortcut.fire, _.pick(e, 'metaKey', 'ctrlKey', 'shiftKey', 'altKey'));
                }
            }
        }

        this.onMouseMove = function(e) {
            window.lastMousePositionX = this.mousePageX = e.pageX || 0;
            window.lastMousePositionY = this.mousePageY = e.pageY || 0;
        }

        this.pushToStackIfNotLast = function(el) {
            if (!this.focusElementStack.length || this.focusElementStack[this.focusElementStack.length - 1] !== el) {
                this.focusElementStack.push(el);
            }
        };

        this.getTriggerElement = function() {
            var triggerElement;

            while (this.focusElementStack.length && !triggerElement) {
                var lastElement = _.last(this.focusElementStack),
                isVisible = $(lastElement).is(':visible');

                if (isVisible) {
                    triggerElement = lastElement;
                } else {
                    this.focusElementStack.pop();
                }
            }

            return triggerElement || this.$node;
        };

        this.fireEvent = function(name, data) {
            var te = this.getTriggerElement();
            data.pageX = this.mousePageX;
            data.pageY = this.mousePageY;
            this.trigger(te, name, data);
        }
    }
});
