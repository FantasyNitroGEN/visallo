define([
    'flight/lib/component',
    'util/withDataRequest',
    './withItem'
], function(defineComponent, withDataRequest, withItem) {
    'use strict';

    return defineComponent(Elements, withDataRequest, withItem);

    function Elements() {

        this.attributes({
            model: null,
            ignoreUpdateModelNotImplemented: true,
            headerSelector: '.org-visallo-layout-elements-header',
            bodySelector: '.org-visallo-layout-elements-body',
            listSelector: '.org-visallo-layout-elements-list',
            singleSelector: '.org-visallo-layout-root'
        })

        this.after('initialize', function() {

            this.on(document, 'verticesUpdated', this.onCheckUpdate);
            this.on(document, 'edgesUpdated', this.onCheckUpdate);

            this.on(this.select('listSelector'), 'selectObjects', this.onSelectObjects);
            this.on(this.select('listSelector'), 'objectsSelected', this.onObjectsSelected);

            var self = this,
                $list = this.select('listSelector'),
                tries = null,
                renderedPromise = new Promise(function handler(resolve, reject) {
                    var $navList = $list.find('.element-list > *'),
                        height = $navList.outerHeight(true);
                    if (height > 0) {
                        resolve(height);
                    } else if (tries !== null && (++tries) < 5) {
                        _.delay(function() {
                            handler(resolve, reject);
                        }, 50)
                    } else {
                        var delay = 1000;
                        _.delay(function() {
                            reject(new Error('Element not rendered after ' + delay + 'ms'));
                        }, delay);
                        self.on('listRendered', function() {
                            tries = 0;
                            handler(resolve, reject);
                        });
                    }
                }),
                key = 'elements[].list',
                heightPreference = visalloData.currentUser.uiPreferences['pane-' + key],
                originalHeight = heightPreference ? parseInt(heightPreference, 10) : $list.height();

            $list.css({ height: 0, visibility: 'hidden' }).attr('data-height-preference', key);
            createResizable($list);

            // Hack to support nested resizables, destroy and recreate child
            // resizable on parent resize
            $list.parent().closest('.ui-resizable')
                .on('resizestart', function(event) {
                    if ($(event.target).closest($list).length) return;
                    if ($list.data('ui-resizable')) {
                        $list.resizable('destroy');
                    }
                })
                .on('resizestop', function() {
                    if ($(event.target).closest($list).length) return;
                    if (!$list.data('ui-resizable')) {
                        createResizable($list);
                    }
                })

            renderedPromise
                .then(function(height) {
                    var newHeight = originalHeight;
                    if (height < $list.height()) {
                        newHeight = height + 5;
                    }
                    $list.css({ visibility: 'visible', height: newHeight });
                })
                .catch(function() {
                    $list.css({ height: originalHeight, visibility: 'visible' })
                })
                .done();
        });

        this.onCheckUpdate = function(event, data) {
            var self = this,
                changed = false,
                changedObjects = data.vertices || data.edges,
                newModel = self.attr.model.map(function(m) {
                    var index = _.findIndex(changedObjects, function(v) {
                        return m.id === v.id;
                    });
                    if (index === -1) {
                        return m;
                    }
                    changed = true;
                    return changedObjects[index];
                })
            if (changed) {
                self.trigger('updateModel', { model: newModel })
            }
        };

        this.onObjectsSelected = function(event, data) {
            event.stopPropagation();
        };

        this.onSelectObjects = function(event, data) {
            event.stopPropagation();

            var self = this;

            this.dataRequest(data.vertexIds.length ? 'vertex' : 'edge', 'store', data)
                .done(function(results) {
                    var first = _.first(results),
                        $single = self.select('singleSelector'),
                        $list = self.select('listSelector'),
                        $histogram = self.select('bodySelector')
                            .add(self.select('headerSelector'));

                    if (self.currentSelectedItemId && first.id === self.currentSelectedItemId) {
                        $single.teardownAllComponents().remove();
                        $histogram.show();
                        $list.find('.element-list').trigger('objectsSelected', {
                            vertices: [], edges: [], vertexIds: [], edgeIds: []
                        });
                        self.currentSelectedItemId = null;
                    } else {
                        self.currentSelectedItemId = first.id;

                        if (!$single.length) {
                            $single = $('<div>')
                                .css('flex', 1)
                                .hide()
                                .insertBefore($list)
                        }

                        require(['detail/item/item'], function(Item) {
                            Item.attachTo($single.empty().teardownAllComponents(), {
                                constraints: ['width'],
                                model: first
                            })
                            self.select('bodySelector')
                                .add(self.select('headerSelector'))
                                .hide();
                            $single.show();
                        });

                        $list.find('.element-list').trigger('objectsSelected', {
                            vertices: data.vertexIds.length ? results : [],
                            edges: data.edgeIds.length ? results : [],
                            vertexIds: data.vertexIds,
                            edgeIds: data.edgeIds
                        })
                    }
                })
        };
    }

    function createResizable(el) {
        $(el).resizable({
            handles: 'n',
            start: function(event) {
                event.stopPropagation();
            },
            stop: function(event) {
                event.stopPropagation();
            },
            resize: function(event, ui) {
                event.stopPropagation();
                $(this).css('top', '');
            }
        });
    }

});
