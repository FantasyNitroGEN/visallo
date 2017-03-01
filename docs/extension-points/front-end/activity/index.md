# Activity

* [Activity JavaScript API `org.visallo.activity`](../../../javascript/org.visallo.activity.html)
* [Activity Example Code](https://github.com/visallo/doc-examples/tree/master/extension-activity)

Activity extension points allow plugins to add additional items to the activity panel (opened via the gears menubar icon.) These rows can show the progress of a long-running process or a front-end task using start/stop events.

<div style="text-align:center">
<img src="./activity.png" width="100%" style="max-width: 400px;">
</div>

## Tutorial

This tutorial will create an example long-running process, and an activity item that shows its progress, along with a custom finished component. For details on creating the back-end long-running process, see the tutorial code link above.

### Create a web plugin

The web plugin registers the resources needed, and creates a route to start the process.

{% github_embed "https://github.com/visallo/doc-examples/blob/c1bdaff/extension-activity/src/main/java/org/visallo/examples/activity/ActivityWebAppPlugin.java#L22-L29" %}{% endgithub_embed %}

The `POST` route to start the activity includes some filters before the `StartExample` handler. These are run in order and protect the route based on request and session conditions:

* `authenticator`: Will only allow authenticated users
* `csrfProtector`: Prevent cross-site request forgery attacks. Should be placed on all requests
* `ReadPrivilegeFilter`: Will only allow users that have this privilege. [Other filters](https://github.com/visallo/visallo/tree/master/web/web-base/src/main/java/org/visallo/web/privilegeFilters) available in Visallo

### Register Extension

Register the activity extension in the `plugin.js` file. The type provided should match the type of the custom `QueueItem`.

{% github_embed "https://github.com/visallo/doc-examples/blob/c1bdaff/extension-activity/src/main/resources/org/visallo/examples/activity/plugin.js#L1-L10" %}{% endgithub_embed %}

### Internationalization

Add a message bundle key for the type of activity

{% github_embed "https://github.com/visallo/doc-examples/blob/c1bdaff/extension-activity/src/main/resources/org/visallo/examples/activity/messages.properties#L1" %}{% endgithub_embed %}

### Finished Interface

Define the component to render when the process is complete, this just calls `alert` with the process json. The button will look at home with the dismiss button if it has `btn btn-mini` class names.

{% github_embed "https://github.com/visallo/doc-examples/blob/c1bdaff/extension-activity/src/main/resources/org/visallo/examples/activity/Finished.jsx#L1-L11" %}{% endgithub_embed %}

