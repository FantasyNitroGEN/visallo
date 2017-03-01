
define([
    'util/requirejs/promise!./service/messagesPromise'
], function(messages) {
    'use strict';

    /**
     * Use message bundles to convert keys to internationalized values.
     *
     * Use `registerMessageBundle` in web plugin to register custom bundles.
     *
     * Tokens can be placed in message values of the form: `{n}`, where `n` is
     * the argument number to replace.
     *
     * <div class="warning">
     * Will display console warning if key doesn't exist
     * </div>
     *
     * @global
     * @name i18n
     * @function
     * @param {string} key The key to lookup
     * @param {...string} args The arguments to replace in the value
     * @return {string} The string
     * @example <caption>Message Bundle Properties</caption>
     * visallo.help.logout=Logout
     * visallo.offline_overlay.last_check=Last checked {0}
     * @example <caption>Handlebars Template</caption>
     * {{ i18n 'visallo.help.logout' }}
     * {{ i18n 'visallo.offline_overlay.last_check', date }}
     * @example <caption>JavaScript Usage</caption>
     * i18n('visallo.help.logout')
     * // => Logout of Visallo
     *
     * i18n('visallo.offline_overlay.last_check', new Date().toString())
     * // => Last checked 2017-02-09T18:29:47.333Z
     */
    return function(ignoreWarning, key/**, args **/) {
        var args = Array.prototype.slice.call(arguments);
        if (ignoreWarning === true) {
            args.shift();
        } else {
            ignoreWarning = false;
        }

        key = args[0];
        if (key in messages) {
            if (args.length === 1) {
                return messages[key];
            }

            args.shift();
            return messages[key].replace(/\{(\d+)\}/g, function(m) {
                var index = parseInt(m[1], 10);
                return args[index];
            });
        }

        if (ignoreWarning) {
            return;
        } else {
            console.error('No message for key', key);
        }
        return key;
    };
});
