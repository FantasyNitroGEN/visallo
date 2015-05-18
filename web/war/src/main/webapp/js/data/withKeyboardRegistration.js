define([
    'util/keyboard'
], function(Keyboard) {
    'use strict';

    return withKeyboardRegistration;

    function withKeyboardRegistration() {

        this.after('setupMessages', function() {
            Keyboard.attachTo(this.node);

            this.trigger('registerKeyboardShortcuts', {
                scope: ['graph.help.scope', 'map.help.scope'].map(i18n),
                shortcuts: {
                    'meta-a': { fire: 'selectAll', desc: i18n('visallo.help.select_all') },
                    'meta-e': { fire: 'selectConnected', desc: i18n('visallo.help.select_connected') },
                    'delete': {
                        fire: 'deleteSelected',
                        desc: i18n('visallo.help.delete')
                    }
                }
            });

            this.trigger('registerKeyboardShortcuts', {
                scope: ['graph.help.scope', 'map.help.scope', 'search.help.scope'].map(i18n),
                shortcuts: {
                    'alt-r': { fire: 'addRelatedItems', desc: i18n('visallo.help.add_related') },
                    'alt-t': { fire: 'searchTitle', desc: i18n('visallo.help.search_title') },
                    'alt-s': { fire: 'searchRelated', desc: i18n('visallo.help.search_related') }
                }
            });
        });

    }
});
