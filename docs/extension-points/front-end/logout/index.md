# Logout

Plugin to add custom logout handlers. When the user explicetly logs out, or session expiration.

        require(['configuration/plugins/registry'], function(registry) {
            registry.registerExtension('org.visallo.logout', function() {
                window.location.href = '/logout';
                return false;
            });
        });

If the handler returns `false` all other logout handlers are skipped and the default logout process is cancelled.
