
define([
    'flight/lib/component',
    'util/formatters',
    './help.hbs',
    './sections.hbs'
], function(
    defineComponent,
    F,
    helpTemplate,
    sectionsTemplate) {
    'use strict';

    var SCOPE_SORTING_HINTS = [
        'visallo.help.scope',
        'search.help.scope',
        'graph.help.scope',
        'map.help.scope'
    ].map(i18n);

    return defineComponent(Help);

    function prettyCommand(shortcut) {
        return F.string.shortcut(shortcut.character, shortcut);
    }

    function sortFn(s1, s2) {
        var i1 = SCOPE_SORTING_HINTS.indexOf(s1),
            i2 = SCOPE_SORTING_HINTS.indexOf(s2);

        if (i1 < 0) i1 = SCOPE_SORTING_HINTS.length;
        if (i2 < 0) i2 = SCOPE_SORTING_HINTS.length;

        return i1 === i2 ? 0 : i1 > i2;
    }

    function Help() {
        this.after('initialize', function() {
            this.allShortcuts = {};

            this.onRegisterKeyboardShortcuts = _.debounce(this.onRegisterKeyboardShortcuts.bind(this), 1000);

            this.on('escape', this.onEscape);
            this.on(document, 'toggleHelp', this.onDisplay);
            this.on(document, 'keyboardShortcutsRegistered', this.onKeyboardShortcutsRegistered);
            this.on(document, 'registerKeyboardShortcuts', this.onRegisterKeyboardShortcuts);

            this.$node.html(helpTemplate({}));

            this.trigger(document, 'registerKeyboardShortcuts', {
                scope: 'Help',
                shortcuts: {
                    escape: { fire: 'escape', desc: i18n('help.shortcut.escape.desc') },
                    'shift-/': { fire: 'toggleHelp', desc: i18n('help.shortcut.toggle.desc') }
                }
            })
            this.trigger(document, 'requestKeyboardShortcuts');

        });

        this.onRegisterKeyboardShortcuts = function() {
            // Shortcuts updated, regenerate list from keyboard.js
            this.trigger(document, 'requestKeyboardShortcuts');
        };

        this.onKeyboardShortcutsRegistered = function(e, data) {
            this.$node.find('ul').html(
                sectionsTemplate({
                    scopes: Object.keys(data).sort(sortFn).map(scope => ({
                        className: scope.toLowerCase(),
                        title: scope,
                        shortcuts: Object.keys(data[scope]).sort().map(key => ({
                            display: prettyCommand(data[scope][key]),
                            desc: data[scope][key].desc
                        }))
                    }))
                })
            );
        };

        this.onDisplay = function(e) {
            if (this.$node.is(':visible')) {
                this.$node.modal('hide');
            } else {
                this.$node.modal();
                _.defer(function() {
                    this.trigger('focusComponent');
                }.bind(this));
            }
        };

        this.onEscape = function(e) {
            this.$node.modal('hide');
            e.stopPropagation();
        };
    }
});
