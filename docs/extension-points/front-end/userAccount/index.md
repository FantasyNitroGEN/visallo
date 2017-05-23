
User Account Page Plugin
========================

Plugin to configure the new pages for user account dialog.

To register a page:

```js
require(['public/v1/api'], function(registry) {
    registry.registerExtension('org.visallo.user.account.page', {
        identifier: 'changePassword',
        pageComponentPath: 'org.visallo.useraccount.changePassword'
    });


    define('org.visallo.useraccount.changePassword', [
        'flight/lib/component'
    ], function(defineComponent) {
        return defineComponent(ChangePassword);

        function ChangePassword() {
            this.after('initialize', function() {
                this.$node.html('Change Password');
            })
        }
    })
});
```

Remember to add a i18n value in a MessageBundle.properties. This will be displayed in the left pane

    useraccount.page.[Page Identifier].displayName=[String to display]

For example:

    useraccount.page.changePassword.displayName=Change Password

User Account Setting Plugin
===========================

Plugin to add a general setting to the user account settings page

To register a boolean setting:

```js
require([
  'public/v1/api',
], function(visallo) {
    visallo.registry.registerExtension('org.visallo.user.account.page.setting', {
        identifier: 'my-bool-setting',
        group: 'useraccount.page.settings.setting.group.test',
        displayName: 'org.project.setting.myBoolean',
        type: 'boolean',
        uiPreferenceName: 'org.project.myBoolean'
    });
});
```

To register a custom control setting:

```js
require([
  'public/v1/api'
], function(visallo) {
    visallo.registry.registerExtension('org.visallo.user.account.page.setting', {
        identifier: 'my-custom-setting',
        group: 'useraccount.page.settings.setting.group.test',
        displayName: 'org.project.setting.myCustom',
        type: 'custom',
        componentPath: 'org/project/setting/MyCustom'
    });
});
```
