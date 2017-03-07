## Detail Text

Create extensions for `org.visallo.detail.extendedData` to replace the default extended data collapsible section content in the element inspector. 

<div class="alert alert-warning">
The console will show a warning if multiple extensions are found for a given tableName. The extension used is non-deterministic.
</div>

### Configuration Options

* `shouldReplaceExtendedDataSection` _(required)_ `[Function]`

    Set a function to determine if this extension should replace a particular extended data section. Return `true` to replace the extended data section content with the component referenced by `componentPath`.
    
    Called with 1 argument:

    * `info`
      * `elementId`   `[String]`
      * `elementType` `[String]`
      * `tableName`   `[String]`


* `componentPath` _(required)_ `[String]`
    
    Specifies the path to a React component that will be attached to the content of the extended data section when it's expanded.


### Example

Override all text sections and just display the name and key of the property.

```js
registry.registerExtension('org.visallo.detail.extendedData', {
    shouldReplaceExtendedDataSection: function(element, tableName) {
        return true;
    },
    componentPath: 'com/example/ExtendedDataPlugin'
});

define([
    'react'
], function (React) {
    'use strict';

    return React.createClass({
        propTypes: {
            visalloApi: React.PropTypes.object.isRequired,
            elementId: React.PropTypes.string.isRequired,
            elementType: React.PropTypes.string.isRequired,
            table: React.PropTypes.shape({
                tableName: React.PropTypes.string.isRequired,
                displayName: React.PropTypes.string.isRequired,
                columns: React.PropTypes.arrayOf(React.PropTypes.shape({
                    propertyIri: React.PropTypes.string,
                    displayName: React.PropTypes.string
                }))
            }),
            onLoad: React.PropTypes.func.isRequired
        },

        render() {
            return (<div>Example Extended Data Plugin</div>);
        }
    });
});
```

