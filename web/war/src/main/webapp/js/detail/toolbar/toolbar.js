define([
    'flight/lib/component',
    'hbs!./template',
    'configuration/plugins/registry',
    'util/vertex/formatters',
    'util/withDataRequest'
], function(
    defineComponent,
    template,
    registry,
    F,
    withDataRequest) {
    'use strict';

    registry.documentExtensionPoint('org.visallo.detail.toolbar',
        'Add detail pane toolbar items',
        function(e) {
            return e === 'DIVIDER' || (
                ('event' in e) && ('title' in e)
                );
        }
    );

    var DIVIDER = {
            divider: true
        },
        ToolbarComponent = defineComponent(Toolbar, withDataRequest);

    ToolbarComponent.ITEMS = {
        DIVIDER: DIVIDER,
        BACK: { title: '◀' },
        FORWARD: { title: '▶' },
        FULLSCREEN: {
            title: i18n('detail.toolbar.open.fullscreen'),
            cls: 'hide-in-fullscreen-details',
            subtitle: i18n('detail.toolbar.open.fullscreen.subtitle'), //'Open in New Window / Tab',
            event: 'openFullscreen'
        },
        ADD_PROPERTY: {
            title: i18n('detail.toolbar.add.property'),
            subtitle: i18n('detail.toolbar.add.property.subtitle'), // 'Add New Property to Entity',
            cls: 'requires-EDIT',
            event: 'addNewProperty'
        },
        ADD_IMAGE: {
            title: i18n('detail.toolbar.add.image'),
            subtitle: i18n('detail.toolbar.add.image.subtitle'), // 'Upload an Image for Entity',
            cls: 'requires-EDIT',
            event: 'addImage',
            options: {
                fileSelector: true
            }
        },
        ADD_COMMENT: {
            title: i18n('detail.toolbar.add.comment'),
            subtitle: i18n('detail.toolbar.add.comment.subtitle'), // 'Add New Comment to Entity',
            cls: 'requires-COMMENT',
            event: 'addNewComment'
        },
        AUDIT: {
            title: i18n('detail.toolbar.audit'),
            subtitle: i18n('detail.toolbar.audit.subtitle'),
            cls: 'audits',
            event: 'toggleAudit'
        },
        DELETE_ITEM: {
            title: i18n('detail.toolbar.delete'),
            cls: 'requires-EDIT',
            event: 'deleteItem'
        }
    };

    return ToolbarComponent;

    function Toolbar() {
        this.defaultAttrs({
            toolbarItemSelector: 'li',
            ignoreUpdateModelNotImplemented: true
        });

        this.after('initialize', function() {
            var self = this,
                model = this.attr.model;
            if (_.isArray(model)) {
                self.initializeToolbar(model);
            } else {
                self.dataRequest(model.type, 'acl', model.id).done(function(acl) {
                    self.initializeToolbar(model, acl);
                });
            }
        });

        this.initializeToolbar = function(model, acl) {
            var config = this.calculateItemsForModel(model, acl),
                toolbarItems = config.items,
                objects = config.objects,
                toolbarExtensions = registry.extensionsForPoint('org.visallo.detail.toolbar');

            toolbarExtensions.forEach(function(item) {
                if (!_.isFunction(item.canHandle) || item.canHandle(objects)) {
                    item.eventData = objects;
                    if (item.options && _.isFunction(item.options.insertIntoMenuItems)) {
                        item.options.insertIntoMenuItems(item, toolbarItems);
                    } else {
                        toolbarItems.push(item);
                    }
                }
            });

            this.on('click', {
                toolbarItemSelector: this.onToolbarItem
            });
            if (toolbarItems.length) {
                this.$node.html(template(config));
                this.$node.find('li').each(function() {
                    var $this = $(this),
                        shouldHide = $this.hasClass('has-submenu') ?
                            _.all($this.find('li').map(function() {
                                return $(this).css('display') === 'none';
                            }).toArray()) :
                            $this.hasClass('no-event');

                    if (shouldHide) {
                        $this.hide();
                    }
                })
            } else {
                this.$node.hide();
            }
        };

        this.calculateItemsForModel = function(model, acl) {
            var isArray = _.isArray(model),
                vertices = isArray ? _.where(model, { type: 'vertex' }) : (model.type === 'vertex' ? [model] : []),
                edges = isArray ? _.where(model, { type: 'edge' }) : (model.type === 'edge' ? [model] : []);

            if (isArray) {
                return {
                    items: [
                        {
                            title: i18n('detail.toolbar.open'),
                            submenu: [
                                ToolbarComponent.ITEMS.FULLSCREEN
                            ].concat(this.selectionHistory())
                        },
                        {
                            title: i18n('detail.multiple.selected', F.number.pretty(model.length)),
                            cls: 'disabled',
                            right: true,
                            event: 'none'
                        }
                    ],
                    objects: { vertices: vertices, edges: edges }
                }
            }

            return {
                items: [
                    {
                        title: i18n('detail.toolbar.open'),
                        submenu: _.compact([
                            ToolbarComponent.ITEMS.FULLSCREEN,
                            this.sourceUrlToolbarItem(model),
                            this.openToolbarItem(model),
                            this.downloadToolbarItem(model)
                        ]).concat(this.selectionHistory())
                    },
                    {
                        title: i18n('detail.toolbar.add'),
                        submenu: _.compact([
                            this.addPropertyToolbarItem(model, acl),
                            this.addImageToolbarItem(model),
                            ToolbarComponent.ITEMS.ADD_COMMENT
                        ])
                    },
                    {
                        icon: 'img/glyphicons/white/glyphicons_157_show_lines@2x.png',
                        right: true,
                        submenu: _.compact([
                            this.deleteToolbarItem(model)
                        ])
                    }
                ],
                objects: { vertices: vertices, edges: edges }
            };
        };

        this.onToolbarItem = function(event) {
            var self = this,
                $target = $(event.target).closest('li'),
                eventName = $target.data('event'),
                eventData = $target.data('eventData');

            if ($target.length && $target.hasClass('disabled')) {
                event.preventDefault();
                event.stopPropagation();
                return;
            }

            if (eventName && $(event.target).is('input[type=file]')) {
                $(event.target).one('change', function(e) {
                    if (e.target.files && e.target.files.length) {
                        self.trigger(eventName, $.extend({
                            files: e.target.files
                        }, eventData));
                    }
                });
                this.hideMenu();
                return;
            }

            if (eventName) {
                event.preventDefault();
                event.stopPropagation();

                _.defer(function() {
                    self.trigger(eventName, eventData);
                });

                this.hideMenu();
            }
        };

        this.hideMenu = function() {
            var node = this.$node.addClass('hideSubmenus');
            _.delay(function() {
                node.removeClass('hideSubmenus');
            }, 500);
        };

        this.selectionHistory = function() {
            if ('selectedObjectsStack' in visalloData) {
                var menus = [],
                    stack = visalloData.selectedObjectsStack;

                for (var i = stack.length - 1; i >= 0; i--) {
                    var s = stack[i];
                    menus.push({
                        title: s.title,
                        cls: 'history-item',
                        event: 'selectObjects',
                        eventData: {
                            vertexIds: s.vertexIds,
                            edgeIds: s.edgeIds
                            //options: {
                                //ignoreMultipleSelectionOverride: true
                            //}
                        }
                    });
                }
                if (menus.length) {
                    menus.splice(0, 0, DIVIDER, {
                        title: 'Previously Viewed',
                        cls: 'disabled'
                    });
                }
                return menus;
            }
        };

        this.openToolbarItem = function(model) {
            var rawProps = F.vertex.props(model, 'http://visallo.org#raw');
            if (rawProps.length) {
                return {
                    title: i18n('detail.artifact.open.original'),
                    subtitle: i18n('detail.artifact.open.original.subtitle'),
                    event: 'openOriginal'
                };
            }
        };

        this.downloadToolbarItem = function(model) {
            var rawProps = F.vertex.props(model, 'http://visallo.org#raw');
            if (rawProps.length) {
                return {
                    title: i18n('detail.artifact.open.download.original'),
                    subtitle: i18n('detail.artifact.open.download.original.subtitle'),
                    event: 'downloadOriginal'
                };
            }
        };

        this.sourceUrlToolbarItem = function(model) {
            if (_.isObject(model) && _.isArray(model.properties)) {
                var sourceUrl = _.findWhere(model.properties, { name: 'http://visallo.org#sourceUrl' });

                if (sourceUrl) {
                    return {
                        title: i18n('detail.toolbar.open.source_url'),
                        subtitle: i18n('detail.toolbar.open.source_url.subtitle'),
                        event: 'openSourceUrl',
                        eventData: {
                            sourceUrl: sourceUrl.value
                        }
                    };
                }
            }
        };

        this.addPropertyToolbarItem = function(model, acl) {
            var hasAddableProperties = _.where(acl.propertyAcls, { addable: true }).length > 0,
                disableAdd = (model.hasOwnProperty('updateable') && !model.updateable) || !hasAddableProperties;

            if (!disableAdd) {
                return ToolbarComponent.ITEMS.ADD_PROPERTY;
            }
        };

        this.addImageToolbarItem = function(model) {
            var displayType = F.vertex.displayType(model);
            var disableAddImage = (model.hasOwnProperty('updateable') && !model.updateable || model.type === 'edge');

            if (!disableAddImage && (displayType !== 'image' && displayType !== 'video')) {
                return ToolbarComponent.ITEMS.ADD_IMAGE;
            }
        };

        this.deleteToolbarItem = function(model) {
            var disableDelete = model.hasOwnProperty('deleteable') && !model.deleteable;

            if (!disableDelete) {
                return _.extend(ToolbarComponent.ITEMS.DELETE_ITEM, {
                    title: i18n('detail.toolbar.delete.entity'),
                    subtitle: i18n('detail.toolbar.delete.entity.subtitle')
                })
            }
        };
    }
});
