# Logout

* [Logout JavaScript API `org.visallo.logout`](../../../javascript/org.visallo.logout.html)
* [Logout Example Code](https://github.com/visallo/doc-examples/tree/master/extension-logout)

Plugin to add custom logout handlers. When the user explicitly logs out, or session expiration.

If the handler returns `false` all other logout handlers are skipped and the default logout process is cancelled.

## Tutorial

## Web Plugin

Register the plugin resource in a web plugin.

{% github_embed "https://github.com/visallo/doc-examples/blob/1155d95c/extension-logout/src/main/java/org/visallo/examples/logout/LogoutWebAppPlugin.java#L17" %}{% endgithub_embed %}

## Register Extension

Register the logout extension that warns the user, prevents the default logout action, and does the logout itself.

{% github_embed "https://github.com/visallo/doc-examples/blob/1155d95c/extension-logout/src/main/resources/org/visallo/examples/logout/plugin.js#L3-L16" %}{% endgithub_embed %}
