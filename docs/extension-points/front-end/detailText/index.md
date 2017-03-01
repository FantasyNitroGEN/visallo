# Element Inspector Text

* [Element Inspector Text JavaScript API `org.visallo.detail.text`](../../../javascript/org.visallo.detail.text.html)
* [Element Inspector Text Example Code](https://github.com/visallo/doc-examples/tree/master/extension-detail-text)

Replace the default text collapsible section content in the Element Inspector.

<div class="alert alert-warning">
The console will show a warning if multiple extensions are found for a given vertex, name, and key. The extension used is non-deterministic.
</div>

## Tutorial

<div style="text-align:center">
<img src="./section.png" width="100%" style="max-width: 385px;">
</div>

### Web Plugin

Register the resources needed.

{% github_embed "https://github.com/visallo/doc-examples/blob/ac5f5428/extension-detail-text/src/main/java/org/visallo/examples/detail_text/DetailTextWebAppPlugin.java#L17-L19" %}{% endgithub_embed %}

### Register Extension

Now, register the text extension for all text properties.

{% github_embed "https://github.com/visallo/doc-examples/blob/ac5f5428/extension-detail-text/src/main/resources/org/visallo/examples/detail_text/plugin.js#L3-L8" %}{% endgithub_embed %}

### Component

The component can be React or Flight, here is a React example that prints the name, key pair.

{% github_embed "https://github.com/visallo/doc-examples/blob/ac5f5428/extension-detail-text/src/main/resources/org/visallo/examples/detail_text/Example.jsx#L7-L17" %}{% endgithub_embed %}

