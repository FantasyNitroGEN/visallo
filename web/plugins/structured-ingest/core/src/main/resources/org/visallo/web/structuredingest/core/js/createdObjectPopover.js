
define([
    'flight/lib/component',
    'util/popovers/withPopover',
    'util/visibility/edit',
    './util'
], function(
    defineComponent,
    withPopover,
    VisibilityEditor,
    util) {
    'use strict';

    return defineComponent(CreatedObjectPopover, withPopover);

    function CreatedObjectPopover() {

        this.defaultAttrs({
            deleteEntitySelector: 'button.deleteEntity',
            deletePropertySelector: 'button.deleteProperty'
        })

        this.before('initialize', function(node, config) {
            config.template = '/org/visallo/web/structuredingest/core/templates/createdObject'
            config.isEdge = 'inVertex' in config.object;
            config.type = config.isEdge ? 'Relationship' : 'Entity';
            config.properties = _.chain(config.object.properties)
                .map(function(p) {
                    return {
                        hide: p.name === util.CONCEPT_TYPE,
                        headerName: p.key,
                        isIdentifier: Boolean(p.hints && p.hints.isIdentifier),
                        propertyName: (
                            config.ontologyProperties.byTitle[p.name] &&
                            config.ontologyProperties.byTitle[p.name].displayName ||
                            p.name
                        )
                    }
                })
                .value();
        });

        this.after('initialize', function() {
            this.after('setupWithTemplate', function() {
                this.on(this.popover, 'click', {
                    deleteEntitySelector: this.onDeleteEntity,
                    deletePropertySelector: this.onDeleteProperty
                });
                this.on(this.popover, 'visibilitychange', this.onVisibilityChanged);
                VisibilityEditor.attachTo(this.popover.find('.visibility'), {
                    placeholder: this.attr.isEdge ?
                        'Relationship Visibility' :
                        'Entity Visibility',
                    value: this.attr.object.visibilitySource
                });
            })
        })

        this.onVisibilityChanged = function(event, data) {
            this.attr.object.visibilitySource = data.value;
        };

        this.onDeleteEntity = function(event) {
            event.stopPropagation();
            event.preventDefault();
            this.trigger('removeMappedObject');
        };

        this.onDeleteProperty = function(event) {
            event.stopPropagation();
            event.preventDefault();

            var $li = $(event.target).closest('li'),
                index = $li.index(),
                property = this.attr.object.properties[index];

            this.trigger('removeMappedObjectProperty', {
                propertyIndex: index
            });

            $li.remove();
            this.positionDialog();
        };
    }
})

