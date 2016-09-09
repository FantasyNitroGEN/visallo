define([
    'util/keyboard'
], function(Keyboard) {
    'use strict';

    return withKeyboardRegistration;

    function withKeyboardRegistration() {

        this.after('setupMessages', function() {
            Keyboard.attachTo(this.node);
        });

        this.after('initialize', function() {
            var self = this;
            this.on('applicationReady currentUserVisalloDataUpdated', function() {
                this.trigger('registerKeyboardShortcuts', {
                    scope: ['search.help.scope'].map(i18n),
                    shortcuts: {
                        'alt-t': { fire: 'searchTitle', desc: i18n('visallo.help.search_title') },
                        'alt-s': { fire: 'searchRelated', desc: i18n('visallo.help.search_related') }
                    }
                });
            });
        });

    }
});
