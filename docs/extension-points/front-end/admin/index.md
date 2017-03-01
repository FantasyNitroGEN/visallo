# Admin

* [Admin JavaScript API `org.visallo.admin`](../../../javascript/org.visallo.admin.html)
* [Admin Example Code](https://github.com/visallo/doc-examples/tree/master/extension-admin)

Admin extensions allow sections to be placed in the admin pane that when clicked, open a custom component.

<div style="text-align:center">
<img src="./admin.png" width="100%" style="max-width: 461px;">
</div>

## Tutorial

This tutorial registers three admin extensions that show React, Flight, and an admin extension that opens a link.

### Create a web plugin

First, create the web plugin that registers the resources.

{% github_embed "https://github.com/visallo/doc-examples/blob/e2e737b/extension-admin/src/main/java/org/visallo/examples/admin/AdminWebAppPlugin.java#L17-L23" %}{% endgithub_embed %}

### Register Extension

Register the admin extensions in the `plugin.js` file.

{% github_embed "https://github.com/visallo/doc-examples/blob/e2e737b/extension-admin/src/main/resources/org/visallo/examples/admin/plugin.js#L3-L8" %}{% endgithub_embed %}

The other two are very similar except we had `sortHint` to "Open URL" so it is first.

{% github_embed "https://github.com/visallo/doc-examples/blob/e2e737b/extension-admin/src/main/resources/org/visallo/examples/admin/plugin.js#L22-L24" %}{% endgithub_embed %}

Notice we use the globally available [`i18n`](../../../javascript/global.html#i18n) function to display strings. These are defined in `messages.properties`.

{% github_embed "https://github.com/visallo/doc-examples/blob/e2e737b/extension-admin/src/main/resources/org/visallo/examples/admin/messages.properties#L2-L4" %}{% endgithub_embed %}


