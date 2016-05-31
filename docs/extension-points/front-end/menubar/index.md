Menubar Plugin
=================

Plugin to add new menubar items.

## Required parameters:

* title: The text under icon
* identifier: Identifier of this menubar icon (must be valid css class)
* action: (object)
    * type: either `pane`, `fullscreen`, or `url`
    * componentPath: path to component to initialize on activation (valid for `pane` or `fullscreen` action types)
    * url: the url to open when clicked (valid for `url` action type)
* icon: path to icon

## Optional parameters:

* welcomeTemplatePath: (string) Path to template to render in **Welcome to Visallo** dashboard card. 
* options: (object)
    * placementHint: either `top` or `bottom`
    * placementHintBefore: class of menubar icon to position before
    * placementHintAfter: class of menubar icon to position after

## Example

To register an item:

```js
require(['configuration/plugins/registry', 'util/messages'], function(registry, i18n) {
    registry.registerExtension('org.visallo.menubar', {
        title: i18n('com.mypluginpackage.myplugin.menubar.title'),
        identifier: 'com-mypluginpackage-myplugin',
        action: {
            type: 'pane',
            componentPath: 'com/mypluginpackage/myplugin/component'
        },
        welcomeTemplatePath: 'hbs!com/mypluginpackage/myplugin/templates/welcome',
        icon: '../img/glyphicons/white/glyphicons_066_tags@2x.png',
        options: {
            placementHint: 'top',
            placementHintAfter: 'search',
        }
    })
});
```
