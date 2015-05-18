# Admin Plugins

Use this as a template to create admin components. Be sure to `require`, not `define` components as they aren't loaded from requirejs.

    require([
        'configuration/admin/plugin',
        'util/messages'
    ], function(defineVisalloAdminPlugin, i18n) {
        'use strict';

        return defineVisalloAdminPlugin(MyAdminPlugin, {
            section: i18n('admin.myadminplugin.section'),
            name: i18n('admin.myadminplugin.name'),
            subtitle: i18n('admin.myadminplugin.subtitle')
        });

        function MyAdminPlugin() {
            this.after('initialize', function() {
                this.$node.text('My Plugin');
            });
        }
    });
