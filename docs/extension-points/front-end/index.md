
# Front-End Customization

Custom JavaScript and CSS can be added to the webapp using a [webapp plugin](../../getting-started/build.md#web-plugin).

        app.registerJavaScript("plugin.js");
        app.registerCss("plugin.css");

JavaScript can also be added to the web application, but not loaded immediately by passing `false` as an optional second parameter.

        app.registerJavaScript("myCustomComponent.js", false);

# Extension Points

Extension points are places built into Visallo that can define additional behavior. Custom plugins can also define extension points that can be implemented by other plugins.

An extension point is simply a mapping from a string – the namespaced extension point name – to a JavaScript object. What kind of object is defined by the consumer of the extension point.

All registered extension points are viewable in the admin panel, under `UI Extensions`

## Register an extension point

To register some custom behavior, require the `registry` module.

    require(['configuration/plugins/registry'], function(registry) {
        registry.registerExtension([extension point name], [extension point object])
    })

For example, to add an item to the menubar:

    registry.registerExtension('org.visallo.menubar', {
        title: 'New'
        identifier: 'org-visallo-example-new',
        action: {
            type: 'full',
            componentPath: 'example-new-page'
        },
        icon: '../img/new.png'
    });

Visallo plugins can also define extension points. They do not need to be defined ahead of time, simply ask the registry for all registered extensions using a unique point name.

    registry.extensionsForPoint([extension point name]);
    // Returns array of registered objects

It is good practice to define some documentation for your new extension point. Documenting provides a validation function, and a description shown in the admin panel under UI Extensions. Document the extension point prior to requesting `extensionsForPoint`.

    registry.documentExtensionPoint('org.visallo.menubar',
        'Add items to menubar',
        function validator(e) {
            return ('title' in e) && ('identifier' in e) && ('action' in e) && ('icon' in e);
        }
    );

This documentation appears in the admin pane under UI Extensions. Add an external documentation URL using an optional 3rd parameter.

    registry.documentExtensionPoint('com.example.point',
        'Description...',
        function () { return true; },
        'http://example.com/docs'
    );



