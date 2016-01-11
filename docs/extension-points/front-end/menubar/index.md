Menubar Plugin
=================

Plugin to add new menubar items.

## Required parameters:

* title: The text under icon
* identifier: Identifier of this menubar icon (must be valid css class)
* action: (object)
    * type: either `pane` or `fullscreen`
    * componentPath: path to component to initialize on activation
* icon: path to icon

## Optional parameters:

* options: (object)
    * placementHint: either `top` or `bottom`
    * placementHintBefore: class of menubar icon to position before
    * placementHintAfter: class of menubar icon to position after

## Example

To register an item:

    require(['configuration/plugins/menubar/plugin', 'util/messages'], function(MenubarExtension, i18n) {
        MenubarExtension.registerMenubarItem({
            title: i18n('com.mypluginpackage.myplugin.menubar.title'),
            identifier: 'com-mypluginpackage-myplugin',
            action: {
                type: 'pane',
                componentPath: 'com/mypluginpackage/myplugin/component'
            },
            icon: '../img/glyphicons/white/glyphicons_066_tags@2x.png',
            options: {
                placementHint: 'top',
                placementHintAfter: 'search',
            }
        })
    })
