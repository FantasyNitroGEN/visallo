define([
    'flight/lib/component',
    './columnEditor',
    './relationEditor',
    './util',
    '../templates/form.hbs',
    '../templates/table.hbs',
    '../templates/preview.hbs',
    'util/withDataRequest',
    'util/vertex/formatters',
    'util/popovers/withElementScrollingPositionUpdates',
    'util/ontology/propertySelect',
    'require',
    'velocity',
    'velocity-ui'
], function(
    defineComponent,
    ColumnEditor,
    RelationEditor,
    util,
    template,
    tableTemplate,
    previewTemplate,
    withDataRequest,
    F,
    withElementScrollingUpdates,
    FieldSelection,
    require) {
    'use strict';

    var ANIMATION_DURATION = 200;

    return defineComponent(StructuredMappingForm, withDataRequest, withElementScrollingUpdates);

    function StructuredMappingForm() {

        this.defaultAttrs({
            cellSelector: '.tableview table td, .tableview table th',
            cancelSelector: '.cancel',
            importSelector: '.import',
            publishSelector: '.shouldPublish',
            segmentedControlSelector: '.segmented-control button',
            changeSheetSelector: '.modal-header h1 select',
            createdEntitiesSelector: '.created .vertex',
            createdObjectsSelector: '.created li',
            errorBadge: '.error .badge',
            noHeaderSelector: '.entities .help button'
        })

        this.after('initialize', function() {
            var self = this;

            this.$node.addClass('mode-header');

            this.mappedObjects = {
                vertices: [],
                edges: []
            };
            this.parseOptions = {
                hasHeaderRow: true,
                startRowIndex: 0,
                sheetIndex: 0
            }
            this.$modal = this.$node.html(template({
                title: F.vertex.title(this.attr.vertex)
            }));
            this.$modal.modal();

            this.on('click', {
                cancelSelector: this.onCancel,
                importSelector: this.onImport,
                publishSelector: this.onSetPublish,
                segmentedControlSelector: this.onSegmentedControl,
                cellSelector: this.onCellClick,
                errorBadge: this.onErrorBadgeClick,
                createdObjectsSelector: this.onCreatedObjectsClick,
                noHeaderSelector: this.onNoHeaderClick
            });
            this.on('mouseover', {
                cellSelector: this.onCellMouse
            })
            this.on('mouseout', {
                cellSelector: this.onCellMouse
            })
            this.on('mousedown', {
                createdEntitiesSelector: this.onEntityMouseDown
            })
            this.on('mouseup', {
                createdEntitiesSelector: this.onEntityUp
            })
            this.on('hidden', function() {
                this.$modal.remove();
            });
            this.on('change', {
                changeSheetSelector: this.onChangeSheet
            });
            this.on('updateMappedObject', this.onUpdateMappedObject);
            this.on('removeMappedObject', this.onRemoveMappedObject);
            this.on('removeMappedObjectProperty', this.onRemoveMappedObjectProperty);
            this.on('errorHandlingUpdated', this.onUpdateErrorHandling);

            this.flashOnce = _.once(this.flashPlaceholder.bind(this));

            _.defer(this.loadInfo.bind(this));

            this.dataRequest('config', 'properties').done(function(properties) {
                self.runningUserGuide = properties["userGuide.enabled"] !== "false";
            });
        });

        this.onChangeSheet = function(event) {
            var sheet = this.select('changeSheetSelector').val();
            this.loadInfo(parseInt(sheet, 10));
        };

        this.onSetPublish = function(event) {
            var checked = $(event.target).is(':checked');
            this.shouldPublish = checked;
            event.stopPropagation();
        };

        this.loadInfo = function(sheetIndex, headerIndex) {
            var self = this,
                options = {
                    maxRows: 5
                };

            if (!_.isUndefined(sheetIndex)) {
                options.sheetIndex = sheetIndex;
                this.parseOptions.sheetIndex = sheetIndex;
            }
            if (!_.isUndefined(headerIndex)) {
                options.maxRows = 3;
                options.headerIndex = headerIndex;
                options.hasHeaderRow = this.parseOptions.hasHeaderRow;
                this.parseOptions.startRowIndex = headerIndex;
            }

            return util.analyze(this.attr.vertex.id, options)
                .then(function(result) {
                    var headers = result.headers,
                        rows = result.rows,
                        chooseHeader = result.hints.allowHeaderSelection && _.isUndefined(headerIndex);

                    self.sheets = result.sheets;
                    self.sendColumnIndices = result.hints.sendColumnIndices;
                    self.headers = headers;
                    self.headerTypes = result.headerTypes;
                    self.$node
                        .toggleClass('mode-header', Boolean(chooseHeader))
                        .toggleClass('mode-map', !chooseHeader)
                        .find('.tableview')
                        .html(tableTemplate(result))
                    self.trigger('updateMappedObject');
                });
        };

        this.onCellMouse = function(event) {
            var self = this,
                oldItems = this.$node.find('.hover-highlight'),
                modeIsChoosingHeader = this.$node.hasClass('mode-header'),
                $target = $(event.target);

            if (this.lastHoverCall) {
                clearTimeout(this.lastHoverCall);
            }

            if (event.type === 'mouseover' && !$target.is('th .badge')) {
                var newItems;

                if (modeIsChoosingHeader) {
                    newItems = $target.siblings('td,th').andSelf();
                } else {
                    var index = $target.closest('td,th').index();
                    newItems = this.$node.find('tr > *:nth-child(' + (index + 1) + ')');
                }
                oldItems.not(newItems).removeClass('hover-highlight');
                newItems.addClass('hover-highlight');
            } else {
                this.lastHoverCall = setTimeout(function() {
                    oldItems.removeClass('hover-highlight');
                }, 50);
            }
        };

        this.onNoHeaderClick = function(event) {
            this.parseOptions.hasHeaderRow = false;
            $(event.target).closest('.help').text(
                i18n('csv.file_import.choose-data-start.placeholder')
            )
        };

        this.onCreatedObjectsClick = function(event) {
            var self = this;

            if (this.$node.find('.segmented-control .entities.active').length) {
                event.stopPropagation();
                event.preventDefault();

                Promise.all([
                    this.dataRequest('ontology', 'properties'),
                    Promise.require('org/visallo/web/structuredingest/core/js/createdObjectPopover')
                ]).done(function(results) {
                    var ontologyProperties = results.shift(),
                        CreatedObjectPopover = results.shift(),
                        $target = $(event.target),
                        $li = $target.closest('li'),
                        index = $li.index(),
                        object;

                    if ($li.is('.edge')) {
                        object = self.mappedObjects.edges[index - self.mappedObjects.vertices.length];
                    } else {
                        object = self.mappedObjects.vertices[index];
                    }

                    $target.find('.badge').teardownAllComponents();
                    if ($target.lookupComponent(CreatedObjectPopover)) {
                        CreatedObjectPopover.teardownAll();
                    } else {
                        CreatedObjectPopover.teardownAll();
                        CreatedObjectPopover.attachTo(event.target, {
                            headers: self.headers,
                            object: object,
                            ontologyProperties: ontologyProperties
                        });
                    }
                });
            }
        };

        this.onEntityMouseDown = function(event) {
            this.mousedownTarget = event.target;
            this.handleEventElement(event);
        };

        this.onEntityUp = function(event) {
            if (!$(event.target).is(this.mousedownTarget)) {
                this.handleEventElement(event);
            }
        };

        this.handleEventElement = function(event) {
            if (this.$node.find('.segmented-control .relationships.active').length) {
                var $gel = $(event.target).closest('li').toggleClass('active'),
                    selected = $gel.parent().find('.active'),
                    len = selected.length;

                if (len === 2) {
                    this.targetEntity = selected.not(this.sourceEntity)[0];
                    this.configureRelation();
                } else {
                    if (len < 2) {
                        this.targetEntity = null;
                    }
                    if (len === 1) {
                        this.sourceEntity = selected[0];
                    } else if (len < 1) {
                        this.sourceEntity = null;
                    }
                    this.updatePlaceholder();
                    this.performTransitionToPlaceholder();
                }
                this.updateCurve(len);
            }
        };

        this.updateCurve = function(selected) {
            var self = this,
                $bodyOffset = this.$node.find('.modal-body').offset();

            require(['d3'], function(d3) {
                d3.select(self.$node.find('.modal-body section.relationships')[0])
                    .selectAll('svg')
                    .data(function() {
                        if (selected === 2 && self.sourceEntity && self.targetEntity) {
                            self.off('mousemove');
                            return [1];
                        }
                        if (selected === 1 && self.sourceEntity && window.lastMousePositionX) {
                            return [1];
                        }
                        self.off('mousemove');
                        return []
                    })
                    .call(function() {
                        this.enter().append('svg')
                            .append('defs')
                            .append('marker')
                            .attr({
                                id: 'triangle',
                                viewBox: '0 0 10 10',
                                refX: '1',
                                refY: '5',
                                markerWidth: '4',
                                markerHeight: '4',
                                orient: 'auto'
                            })
                            .append('path')
                            .style({ stroke: 'none', fill: '#0088cc' })
                            .attr('d', 'M 0 0 L 10 5 L 0 10 z')
                        this.exit().remove();
                    })
                    .selectAll('path.line')
                    .data([1])
                    .call(function() {
                        this.enter().append('path')
                            .attr('class', 'line')
                            .attr('marker-end', 'url(#triangle)');
                        this.exit().remove();
                    })
                    .attr('d', function() {
                        return calculatePath();
                    })
                if (!self.targetEntity && self.sourceEntity) {
                    var $line = self.$node.find('path.line');
                    self.off('mousemove');
                    self.on('mousemove', function(event) {
                        var $target = $(event.target).closest('li'),
                            eligible;
                        if ($target.is('.vertex:not(.active)')) {
                            eligible = $target[0];
                        }
                        $line.attr('d', calculatePath(eligible));
                    });
                }
            });

            function calculatePath(eligible) {
                var padding = 8,
                    controlDistance = 25,
                    toXY = function(el) {
                        var pos = [];
                        if (el || eligible) {
                            var $el = $(el || eligible),
                                position = $el.position();
                            pos[0] = position.left + $el.outerWidth(true) / 2;
                            pos[1] = position.top + $el.outerHeight(true);
                        } else {
                            pos[0] = window.lastMousePositionX - $bodyOffset.left;
                            pos[1] = window.lastMousePositionY - $bodyOffset.top;
                        }
                        return pos;
                    },
                    sourcePosition = toXY(self.sourceEntity),
                    targetPosition = toXY(self.targetEntity),
                    controlPosition = [];

                targetPosition[1] += padding;
                controlPosition[0] = targetPosition[0] + padding * (sourcePosition[0] < targetPosition[0] ? -1 : 1);
                controlPosition[1] = targetPosition[1] + controlDistance;

                return [
                    'M' + sourcePosition.join(','),
                    'Q' + controlPosition.join(','),
                    targetPosition.join(',')
                ].join(' ')
            }
        };

        this.updatePlaceholder = function() {
            var self = this,
                $placeholder = this.getCurrentContainer().find('.help');

            if (this.$node.hasClass('mode-header')) {
                if (this.sheets.length > 1) {
                    if (!this.select('changeSheetSelector').length) {
                        this.$node.find('.modal-header h1 span').html(
                            $('<select>')
                                .html(
                                    $.map(this.sheets, function(sheetName, i) {
                                        return $('<option>').val(i).text(sheetName);
                                    })
                                )
                        );
                    }
                } else if (this.sheets.length && !_.isEmpty(this.sheets[0])) {
                    this.$node.find('.modal-header h1 span').text(this.sheets[0]);
                }
                $placeholder.text(i18n('csv.file_import.choose-header.placeholder'))
                    .append(
                        $('<button>')
                            .addClass('btn btn-link')
                            .text(i18n('csv.file_import.choose-header.no-header'))
                    )
            } else if (this.$node.find('.segmented-control .relationships.active').length) {
                var selected = this.$node.find('.created .vertex.active').length;
                $placeholder.text(
                    selected === 0 ?
                        i18n('csv.file_import.relationships.placeholder.no-selection') :
                        selected === 1 ?
                        i18n('csv.file_import.relationships.placeholder.one-selection') :
                        i18n('csv.file_import.relationships.placeholder.too-many-selected')
                );
            } else {
                $placeholder.text(i18n('csv.file_import.entities.placeholder'));
                _.delay(function() {
                    self.flashOnce();
                }, 250);
            }
        };

        this.findPropertiesForColumn = function(key) {
            return _.chain(this.mappedObjects.vertices.concat(this.mappedObjects.edges))
                .map(function(o) {
                    return util.findPropertiesForColumnInObject(o, key);
                })
                .flatten(true)
                .value();
        };

        this.onUpdateErrorHandling = function(event, data) {
            var key = data.key,
                properties = this.findPropertiesForColumn(key);

            if (properties.length) {
                properties.forEach(function(p) {
                    if (data.errorStrategy) {
                        p.errorStrategy = data.errorStrategy;
                    } else {
                        delete p.errorStrategy;
                    }
                })
            } else console.error('Expected at least one property for column:' + key);
        };

        this.removeObjectFromObjectInfo = function(objectInfo) {
            if (objectInfo) {
                if (objectInfo.isVertex) {
                    this.mappedObjects.edges = _.reject(this.mappedObjects.edges, function(e, i) {
                        // remove any edges with this vertex
                        var edgeUsesVertex = (
                            e.inVertex === objectInfo.index ||
                            e.outVertex === objectInfo.index
                        );

                        // fix offsets
                        if (objectInfo.index < e.inVertex) e.inVertex--;
                        if (objectInfo.index < e.outVertex) e.outVertex--;

                        return edgeUsesVertex;
                    });
                }
                objectInfo.list.splice(objectInfo.index, 1);
                Promise.require('org/visallo/web/structuredingest/core/js/createdObjectPopover')
                    .then(function(Popover) {
                        Popover.teardownAll();
                    })
            }
        };

        this.objectInfoForElement = function(el) {
            var $li = $(el),
                index = $li.index(),
                isVertex = true,
                object,
                list;

            if ($li.is('.edge')) {
                isVertex = false;
                list = this.mappedObjects.edges;
                index -= this.mappedObjects.vertices.length;
                object = list[index];
            } else {
                list = this.mappedObjects.vertices;
                object = this.mappedObjects.vertices[index];
            }

            return {
                index: index,
                object: object,
                list: list,
                isVertex: isVertex,
                isEdge: !isVertex
            };
        };

        this.onRemoveMappedObject = function(event, data) {
            var objectInfo = this.objectInfoForElement($(event.target).closest('li'));

            if (objectInfo) {
                this.removeObjectFromObjectInfo(objectInfo);
                this.renderMappedObjects();
                this.enableFooterButtons(true);
            }
        };

        this.onRemoveMappedObjectProperty = function(event, data) {
            var objectInfo = this.objectInfoForElement($(event.target).closest('li'));

            if (objectInfo && objectInfo.object) {
                objectInfo.object.properties.splice(data.propertyIndex, 1);
                if (objectInfo.object.properties.length === 1) {
                    this.removeObjectFromObjectInfo(objectInfo);
                }
                this.renderMappedObjects();
                this.enableFooterButtons(true);
            }
        };

        this.onUpdateMappedObject = function(event, data) {
            if (data) {
                var type = data.type === 'vertex' ? 'vertices' : 'edges',
                    list = this.mappedObjects[type],
                    finished = !!data.finished,
                    shouldCreateNew = true;

                this.mappedObjects[type] = list = list.map(function(o) {
                    if (o.id === data.object.id) {
                        shouldCreateNew = false;
                        return data.object;
                    }
                    return o;
                });
                if (shouldCreateNew) {
                    list.push(data.object);
                }
            }
            if (!data || data.finished) {
                if (data && data.type === 'vertex' && ('column' in data)) {
                    this.$node.find('thead th').eq(data.column)
                        .removeClass('error')
                        .find('.badge').remove();
                }
                this.performTransitionToPlaceholder();
                this.$node.find('.created .active').removeClass('active');
                this.updatePlaceholder();
                this.updateCurve(0);
            }

            this.renderMappedObjects();
        };

        this.renderMappedObjects = function() {
            var self = this,
                allObjects = this.mappedObjects.vertices.concat(this.mappedObjects.edges);

            this.setImportButtonToPreview(true);

            this.$node.find('.highlight').removeClass('highlight');
            this.$node.find('.segmented-control button.relationships').prop('disabled', this.mappedObjects.vertices.length < 2);

            require(['d3', 'util/vertex/formatters'], function(d3, F) {
                var isVertex = function(index) {
                    return index < self.mappedObjects.vertices.length;
                };

                d3.select(self.$node.find('.created')[0])
                    .selectAll('li')
                    .data(allObjects)
                    .call(function() {
                        this.enter().append('li')
                        this.exit().remove();
                    })
                    .order()
                    .each(function(f, i) {
                        var isV = isVertex(i), text;
                        $(this)
                            .toggleClass('vertex', isV)
                            .toggleClass('edge', !isV)

                        this.normalize()
                        if (isV) {
                            var propertySuffix = '',
                                len = f.properties.length - 1;
                            if (len) {
                                propertySuffix = ' (' + F.string.plural(len, 'property', 'properties') + ')';
                            }
                            text = f.displayName + propertySuffix;
                        } else {
                            text = f.displayName;
                        }

                        if (this.childNodes.length) {
                            this.childNodes[0].textContent = text;
                        } else {
                            this.appendChild(document.createTextNode(text));
                        }
                    });
            });

            this.$node.find('thead th').each(function(i) {
                var hasColumn = _.some(allObjects, function(o) {
                    return _.find(o.properties, function(p) {
                        var key = self.headers[i];
                        if (p.key === key) return true;
                        if (p.hints && (
                            key === p.hints.columnLatitude ||
                            key === p.hints.columnLongitude)) {
                            return true;
                        }
                    });
                });
                $(this).toggleClass('checked', hasColumn);
            })
        };

        this.onErrorBadgeClick = function(event) {
            var self = this;

            event.stopPropagation();
            event.preventDefault();
            require(['./errorsPopover'], function(ErrorsPopover) {
                var $target = $(event.target);
                $target.closest('li').teardownAllComponents();
                if ($target.lookupComponent(ErrorsPopover)) {
                    ErrorsPopover.teardownAll();
                } else {
                    var type = $target.closest('table').length ?
                            'column' : $target.closest('.vertex').length ?
                            'vertex' : 'edge',
                        attrs = { },
                        key;

                    if (type === 'column') {
                        key = self.headers[$target.closest('th').index()]

                        var properties = self.findPropertiesForColumn(key),
                            errorStrategy = properties.length ?
                                properties[0].errorStrategy :
                                '';

                        attrs.errorStrategy = errorStrategy;
                    } else if (type === 'vertex') {
                        key = $target.closest('li').index();
                    } else {
                        key = $target.closest('li').index() - self.mappedObjects.vertices.length;
                    }

                    attrs.key = key;
                    attrs.errors = self.errors[type][key];

                    ErrorsPopover.teardownAll();
                    ErrorsPopover.attachTo(event.target, attrs);
                }
            });
        };

        this.onCellClick = function(event) {
            var self = this,
                $target = $(event.target);

            if ($target.is('.badge')) return;
            if (this.$node.hasClass('mode-header')) {
                this.loadInfo(this.parseOptions.sheetIndex, $target.closest('tr').data('index'))
            } else {
                var index = $(event.target).index();

                this.$node.find('.highlight').removeClass('highlight')
                this.$node.find('tr').each(function() {
                    $(this).find('td,th').eq(index).addClass('highlight');
                })

                this.configureColumn(self.headers[index]);
            }
        };

        this.flashPlaceholder = function() {
            this.$node.find('.help:visible')
                .velocity({ scale: 1.1 }, 'fast')
                .velocity({ scale: 1 }, 'fast')
        }

        this.configureColumn = function(key) {
            var self = this,
                $editor = this.willTransitionToEditor('entities', ColumnEditor);

            this.off('fieldRendered');
            this.on('fieldRendered', function() {
                self.performTransitionToEditor($editor);
            })
            ColumnEditor.attachTo($editor, {
                header: self.headers[key],
                type: self.headerTypes[key],
                allHeaders: self.headers,
                key: key,
                vertices: this.mappedObjects.vertices
            });
        };

        this.configureRelation = function() {
            var self = this,
                $editor = this.willTransitionToEditor('relationships', RelationEditor),
                attr = [this.sourceEntity, this.targetEntity].map(function(el) {
                    var index = $(el).index();
                    return {
                        index: index,
                        displayName: self.mappedObjects.vertices[index].displayName,
                        concept: _.findWhere(self.mappedObjects.vertices[index].properties, {
                            name: util.CONCEPT_TYPE
                        }).value
                    };
                });

            this.off('fieldRendered');
            this.on('fieldRendered', function() {
                self.performTransitionToEditor($editor);
            })
            RelationEditor.attachTo($editor, {
                edges: this.mappedObjects.edges,
                sourceConcept: attr[0].concept,
                targetConcept: attr[1].concept,
                sourceIndex: attr[0].index,
                targetIndex: attr[1].index,
                sourceDisplayName: attr[0].displayName,
                targetDisplayName: attr[1].displayName
            });
        };

        this.willTransitionToEditor = function(type, Editor) {
            var $container = this.$node.find('section.' + type + ' .form-container');

            $container.css('height', parseInt($container.height(), 10) + 'px');

            var $editor = $container.find('div').teardownComponent(Editor),
                $placeholder = $container.find('h1.help');

            if ($placeholder.css('display') !== 'none') {
                $editor.css({
                    display: 'block',
                    visibility: 'hidden'
                })
            }

            return $editor;
        };

        this.performTransitionToEditor = function($editor) {
            var self = this,
                $container = $editor.closest('.form-container'),
                $placeholder = $container.find('h1.help'),
                $table = this.$node.find('.tableview'),
                $hideRows = this.$node.find('tr:nth-child(n+2)'),
                calculateNewTableHeight = function() {
                    return _.reduce(
                        self.$node.find('tr:nth-child(1)').map(function() {
                            return $(this).height();
                        }), function(n, i) {
                            return n + i;
                        }
                    );
                };

            if ($placeholder.css('display') === 'none') {
                $container.css('height', 'auto');
                return;
            }

            $container.css('overflow', 'hidden');
            this.$node.find('.modal-body').css('overflow', 'hidden');

            var animationSequence = _.compact([
                this.$node.find('.segmented-control .entities.active').length ?
                {
                    e: $table,
                    p: { height: calculateNewTableHeight() },
                    o: {
                        duration: ANIMATION_DURATION / 2,
                        begin: function() {
                            $table.css({
                                overflow: 'hidden',
                                height: $table.height()
                            });
                        },
                        complete: function() {
                            $table.css('height', 'auto');
                            $hideRows.hide();
                        }
                    }
                } : undefined,
                {
                    e: $placeholder,
                    p: { opacity: 0 },
                    o: { display: 'none', duration: ANIMATION_DURATION / 2 }
                },
                {
                    e: $container,
                    p: { backgroundColor: '#eee', height: parseInt($editor.outerHeight(true), 10) + 'px' },
                    o: {
                        sequenceQueue: false,
                        duration: ANIMATION_DURATION,
                        complete: function() { $container.css('height', 'auto') }
                    }
                },
                {
                    e: $editor,
                    p: { opacity: 1 },
                    o: {
                        sequenceQueue: false,
                        delay: ANIMATION_DURATION / 2,
                        duration: ANIMATION_DURATION / 2,
                        begin: function() { $editor.css({ opacity: 0, visibility: 'visible' }); },
                        complete: function() {
                            self.enableFooterButtons(false);
                            self.$node.find('.modal-body').css('overflow', 'visible');
                            $container.css('overflow', 'visible');
                        }
                    }
                }
            ]);

            /*eslint new-cap:0*/
            $.Velocity.RunSequence(animationSequence);
        };

        this.enableFooterButtons = function(enable) {
            if (enable) {
                this.select('importSelector').prop('disabled', this.mappedObjects.vertices.length === 0);
                this.$node.find('.modal-footer button.cancel').prop('disabled', false);
            } else {
                this.$node.find('.modal-footer button').prop('disabled', true);
                this.$node.find('.segmented-control button.preview').prop('disabled', true);
            }
        }

        this.getCurrentContainer = function() {
            var type = this.$node.find('.segmented-control .relationships.active').length ?
                'relationships' : 'entities',
                $container = this.$node.find('section.' + type + ' .form-container');

            return $container;
        }

        this.performTransitionToPlaceholder = function() {
            var self = this,
                $container = this.getCurrentContainer(),
                $editor = $container.find('div'),
                $table = this.$node.find('.tableview'),
                $placeholder = $container.find('h1.help');

            if ($placeholder.css('display') !== 'none') {
                return;
            }

            $.Velocity.RunSequence(_.compact([
                {
                    e: $editor,
                    p: { opacity: 0 },
                    o: {
                        duration: ANIMATION_DURATION / 2,
                        display: 'none',
                        complete: function() {
                            $editor.teardownAllComponents();
                        }
                    }
                },
                {
                    e: $container,
                    p: {
                        height: $placeholder.outerHeight(true),
                        backgroundColor: '#ffffff'
                    },
                    o: {
                        sequenceQueue: false,
                        duration: ANIMATION_DURATION,
                        begin: function() {
                            $container.css('height', $container.height());
                        },
                        complete: function() {
                            $container.css('height', 'auto');
                        }
                    }
                },
                {
                    e: $placeholder,
                    p: { opacity: 1 },
                    o: {
                        sequenceQueue: false,
                        delay: ANIMATION_DURATION / 2,
                        duration: ANIMATION_DURATION / 2,
                        begin: function() {
                            $placeholder.css({ opacity: 0, display: 'block' })
                        },
                        complete: function() {
                            self.enableFooterButtons(true);
                        }
                    }
                },
                this.$node.find('.segmented-control .entities.active').length ?
                {
                    e: $table
                        .css('height', $table.height())
                        .find('tr').show()
                        .end(),
                    p: { height: $table.children('table').height() },
                    o: {
                        duration: ANIMATION_DURATION / 2,
                        complete: function() {
                            $table.css({
                                height: 'auto',
                                overflow: 'auto'
                            });
                        }
                    }
                } : undefined
            ]));
        };

        this.handleErrors = function(result) {
            var self = this,
                errors = result.errors || [],
                mappingErrors = result.mappingErrors || [],
                totalErrors = errors.length + mappingErrors.length,
                transformErrors = function(mappingType) {
                    return _.chain(errors)
                        .filter(function(e) {
                            return mappingType in e;
                        })
                        .tap(function(errors) {
                            _.filter(mappingErrors, function(me) {
                                return mappingType in me;
                            }).forEach(function(e) {
                                e.mappingError = true;
                                errors.push(e);
                            })
                        })
                },
                mapObject = function(items) {
                    return {
                        count: items.length,
                        uniqueMessages: _.unique(items, function(i) {
                            return i.message
                        }),
                        list: items
                    }
                },
                columnErrors = transformErrors('propertyMapping')
                    .groupBy(function(e) {
                        return self.sendColumnIndices ?
                            self.headers[parseInt(e.propertyMapping.key, 10)] :
                            e.propertyMapping.key
                    })
                    .mapObject(mapObject)
                    .value(),
                entityErrors = transformErrors('vertexMapping')
                    .groupBy(function(e) {
                        var index = _.findIndex(self.mappedObjects.vertices, function(v) {
                            var visibilityMatches = v.visibilitySource === (
                                    e.vertexMapping.visibilityJson &&
                                    e.vertexMapping.visibilityJson.source
                                ) || '',
                                match = visibilityMatches && _.all(e.vertexMapping.propertyMappings, function(mapping) {
                                    var predicate = { name: mapping.name };
                                    if ('value' in mapping) {
                                        predicate.value = mapping.value;
                                    } else {
                                        predicate.key = mapping.key;
                                        if ('visibilityJson' in mapping) {
                                            predicate.visibilitySource = (
                                                mapping.visibilityJson &&
                                                mapping.visibilityJson.source
                                            ) || '';
                                        }
                                    }
                                    return _.findWhere(v.properties, predicate);
                                });

                            return match;
                        })

                        return index;
                    })
                    .mapObject(mapObject)
                    .value(),
                edgeErrors = transformErrors('edgeMapping')
                    .groupBy(function(e) {
                        var index = _.findIndex(self.mappedObjects.edges, function(edge) {
                            var mapping = e.edgeMapping,
                                visibilityMatches = edge.visibilitySource === (
                                    mapping.visibilityJson &&
                                    mapping.visibilityJson.source
                                ) || '',
                                match = visibilityMatches &&
                                    edge.inVertex === mapping.inVertexIndex &&
                                    edge.outVertex === mapping.outVertexIndex &&
                                    edge.label === mapping.label;

                            return match;
                        })

                        return index;
                    })
                    .mapObject(mapObject)
                    .value(),
                markError = function(typeErrors, options) {
                    return function(i) {
                        var $el = $(this);
                        var key = self.headers[i];
                        $el.removeClass('error').find('.badge').remove();
                        if (key in typeErrors) {
                            var errors = typeErrors[key];
                            $el.addClass('error')[options && options.before ? 'prepend' : 'append'](
                                $('<span>')
                                    .addClass('badge badge-important')
                                    .attr('title', i18n('csv.file_import.errors.show-errors-message'))
                                    .text(F.number.pretty(errors.count))
                            );
                        }
                    }
                }

            var $active = this.select('segmentedControlSelector').filter('.active');
            if ($active.is('.relationships')) {
                $active.removeClass('active').siblings('.entities').addClass('active')
                this.$node.find('section.relationships').hide();
                this.$node.find('section.entities').show()
            }
            this.$node.find('.tableview tr th').each(markError(columnErrors, { before: true }));
            this.$node.find('.created .vertex').each(markError(entityErrors));
            this.$node.find('.created .edge').each(markError(edgeErrors));
            this.$node.find('.importMessage')
                .removeClass('info')
                .attr('title', i18n('csv.file_import.errors.show-column-errors-message'))
                .text(F.string.plural(totalErrors, 'error'));

            this.errors = {
                column: columnErrors,
                vertex: entityErrors,
                edge: edgeErrors
            };
        };

        this.onCancel = function(event) {
            this.$modal.modal('hide');
        };

        this.onImport = function(event) {
            var $message = this.$node.find('.importMessage').empty(),
                $active = this.select('segmentedControlSelector').filter('.active');

            if ($active.is('.relationships')) {
                this.switchPanel('entities')
            }

            this.ingestPreview();
        };

        this.ingestPreview = function() {
            var self = this,
                $button = this.select('importSelector').addClass('loading').prop('disabled', true),
                $message = this.$node.find('.importMessage').empty(),
                mapping = {
                    vertices: this.mappedObjects.vertices.map(function(v) {
                        return {
                            visibilitySource: v.visibilitySource,
                            properties: v.properties.map(function(p) {
                                var moveHints = $.extend({}, p, p.hints || {});
                                if (self.sendColumnIndices) {
                                    if ('key' in moveHints) {
                                        moveHints.key = self.headers.indexOf(moveHints.key);
                                    }
                                }
                                delete moveHints.hints;
                                if (('columnLatitude' in moveHints) ||
                                    ('columnLongitude' in moveHints)) {
                                    delete moveHints.column;
                                }
                                return moveHints;
                            })
                        }
                    }),
                    edges: this.mappedObjects.edges.map(function(e) {
                        return _.pick(e, 'inVertex', 'outVertex', 'label', 'properties', 'visibilitySource')
                    })
                };

            this.on(document, 'structuredImportDryrunProgress', function(event, data) {
                $message
                    .addClass('info')
                    .attr('title', '')
                    .text(i18n('csv.file_import.checking.percent', F.number.percent(data.row / data.total)));
            });

            this.dataRequest('org-visallo-structuredingest', 'ingest', mapping, self.attr.vertex.id, this.parseOptions, this.currentImportActionIsPreview, this.shouldPublish)
                .then(function(result) {
                    $message.empty().removeClass('info');

                    if (result.success) {
                        self.$modal.modal('hide');
                        self.trigger('showActivityDisplay');
                    } else if (result.errors || result.mappingErrors) {
                        self.handleErrors(result);
                    } else if (result.vertices) {
                        self.$node.find('.segmented-control button.preview').prop('disabled', false);
                        self.switchPanel('preview');
                        self.setImportButtonToPreview(false);
                        self.dataRequest('ontology', 'ontology').then(function(ontology) {
                            var processType = function(elementType) {
                                return _.chain(result[elementType].numbers)
                                    .map(function(v, type) {
                                        var ontologyType = ontology[elementType === 'vertices' ? 'concepts' : 'relationships'];
                                        var ontologyThing = ontologyType.byId[type];
                                        return {
                                            elementType: i18n('csv.file_import.preview.table.' + elementType),
                                            showElementType: false,
                                            type: ontologyThing ? ontologyThing.displayName : type,
                                            count: F.number.pretty(v.created)
                                        }
                                    })
                                    .sortBy('type')
                                    .tap(function(list) {
                                        if (list.length) {
                                            list[0].showElementType = true;
                                        }
                                    })
                                    .value()
                            };
                            var elements = processType('vertices').concat(processType('edges'))
                            self.$node.find('.modal-body .preview .form-container div').html(
                                previewTemplate({
                                    elements: elements,
                                    didTruncate: result.didTruncate,
                                    processedRows: F.number.pretty(result.processedRows)
                                })
                            )
                        })
                    } else {
                        throw new Error();
                    }
                })
                .catch(function(error) {
                    var $errorElement;

                    if(self.runningUserGuide){
                        $errorElement = $('<a />')
                            .attr('href', 'user-guide/csv/index.html#problems')
                            .attr('target', '_blank')
                            .text(i18n('csv.file_import.errors.unable_to_import_user_guide'));
                    }
                    else {
                        $errorElement = $('<span />')
                            .text(i18n('csv.file_import.errors.unable_to_import'));
                    }

                    $errorElement.attr('title', error.message || i18n('csv.file_import.errors.unknown'));

                    $message.empty().removeClass('info').append($errorElement);
                })
                .finally(function() {
                    self.off(document, 'structuredImportDryrunProgress');
                    $button.removeClass('loading').prop('disabled', false);
                })
        };

        this.onSegmentedControl = function(event, data) {
            var $button = $(event.target),
                sectionName = $button.data('visible');

            this.switchPanel(sectionName);
        }

        this.switchPanel = function(panelName) {
            var $button = this.$node.find('.segmented-control').find('button.' + panelName).addClass('active'),
                $section = this.$node.find('.' + panelName);

            $button.siblings('button.active').removeClass('active');

            if (panelName === 'relationships' && this.previouslySelected) {
                this.previouslySelected.addClass('active');
                this.previouslySelected = null;
            } else {
                var activeElements = this.$node.find('.created .active').removeClass('active');
                if (panelName === 'entities') {
                    this.previouslySelected = activeElements;
                }
            }

            this.$node.find('.created').toggle(panelName !== 'preview');

            $section.show().siblings('section').hide();
        }

        this.setImportButtonToPreview = function(preview) {
            if (preview) {
                this.select('importSelector').text(i18n('csv.file_import.mapping.import.preview'));
            } else {
                this.select('importSelector').text(i18n('csv.file_import.mapping.import.ingest'));
            }
            this.currentImportActionIsPreview = preview;
        }
    }
});
