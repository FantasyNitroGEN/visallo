Search Filter Plugin
=================

Plugin to add new items to search filter component.

## Required parameters:

* componentPath: requirejs component path to flight component
* sectionHeader: Displays "Filter by Property" like text
* filterKeys: names of parameters filter will change
* *or*
* filterKey: name of param
* searchType: Visallo or Workspace. Only `Visallo` is implemented

## Optional parameters:

* initHidden: hide the filter initially

## Example

To register an item:

        require([
            'configuration/plugins/searchFilter/plugin',
            'util/messages'
        ], function(SearchFilterExtension, i18n) {
            SearchFilterExtension.registerSearchFilter({
                componentPath: 'com.myplugin',
                sectionHeader: 'My Search Filter',
                filterKeys: ['mypluginKey', 'mypluginOptions'],
                searchType: 'Visallo',
                initHidden: true
            })

            define('com.myplugin', ['flight/lib/component'], function(defineComponent) {
                return defineComponent(MySearchFilter);

                function MySearchFilter() {
                    this.after('initialize', function() {
                        // To trigger changes: 
                        this.trigger(this.attr.changedEventName, { mypluginKey: 'value' })
                    })
                }
            })
        })

