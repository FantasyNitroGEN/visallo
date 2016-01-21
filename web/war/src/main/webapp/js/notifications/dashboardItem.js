define([
    'flight/lib/component',
    './notifications'
], function(
    defineComponent,
    Notifications) {
    'use strict';

    return defineComponent(NotificationsDashboardItem);

    function NotificationsDashboardItem() {

        this.after('initialize', function() {
            Notifications.attachTo(this.node, {
                allowDismiss: false,
                animated: false,
                showUserDismissed: true
            });
        });

    }
});
