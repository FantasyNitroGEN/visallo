define([
    'flight/lib/component',
    'configuration/admin/utils/withFormHelpers',
    'hbs!org/visallo/web/devTools/templates/element-editor',
    'util/messages',
    'd3',
    'util/vertex/formatters',
    'util/withDataRequest'
], function(
    defineComponent,
    withFormHelpers,
    template,
    i18n,
    d3,
    F,
    withDataRequest) {
    'use strict';

    return defineComponent(ElementEditor, withDataRequest, withFormHelpers);

    function ElementEditor() {
        this.defaultAttrs({
            loadSelector: '.refresh',
            deleteElementSelector: '.delete-element',
            workspaceInputSelector: '.workspaceId',
            elementInputSelector: '.elementId',
            collapsibleSelector: '.collapsible > h1',
            editSelector: '.prop-edit',
            deleteSelector: '.prop-delete',
            saveSelector: '.prop-save',
            cancelSelector: '.prop-cancel'
        });

        this.after('initialize', function() {
            var self = this;

            this.on('click', {
                loadSelector: this.onLoad,
                editSelector: this.onEdit,
                deleteElementSelector: this.onElementDelete,
                deleteSelector: this.onDelete,
                saveSelector: this.onSave,
                cancelSelector: this.onCancel,
                collapsibleSelector: this.onToggleExpand
            });

            this.on(document, 'objectsSelected', this.onObjectsSelected);
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);
            this.on(document, 'edgesUpdated', this.onEdgesUpdated);

            this.on('change keyup paste', {
                elementInputSelector: this.validate,
                workspaceInputSelector: this.validate
            })

            this.$node.html(template({
                workspaceId: visalloData.currentWorkspaceId,
                elementId: ''
            }));

            this.onObjectsSelected(null, visalloData.selectedObjects);
        });

        this.validate = function() {
            var valid = (
                $.trim(this.select('workspaceInputSelector').val()).length &&
                $.trim(this.select('elementInputSelector').val()).length
            );
            this.select('loadSelector').add(this.select('deleteElementSelector'))
                .prop('disabled', !valid);

            if (!valid) {
                this.$node.find('section').remove();
            }
        };

        this.onEdit = function(event) {
            var button = $(event.target),
                li = button.closest('li').addClass('editing');
        };

        this.onDelete = function(event) {
            var self = this,
                button = $(event.target),
                li = button.closest('li').addClass('show-hover-items');

            this.handleSubmitButton(
                button,
                this.dataRequest(self.elementType, 'deleteProperty',
                    this.$node.find('.elementId').val(),
                    li.data('property'),
                    this.$node.find('.workspaceId').val()
                )
                    .then(function() {
                        li.removeClass('show-hover-items');
                        self.onLoad();
                    })
                    .catch(function() {
                        self.showError();
                    })
            );
        };

        this.onSave = function(event) {
            var self = this,
                button = $(event.target),
                li = button.closest('li'),
                property = li.data('property');

            this.handleSubmitButton(
                button,
                this.dataRequest(self.elementType, 'setProperty',
                    this.$node.find('.elementId').val(),
                    {
                        key: li.find('input[name=key]').val(),
                        name: property.name || li.find('input[name=name]').val(),
                        value: li.find('input[name=value],textarea[name=value]').val(),
                        visibilitySource: li.find('textarea[name="http://visallo.org#visibilityJson"]').val(),
                        justificationText: 'Admin tools element editor',
                        metadata: JSON.parse(li.find('textarea[name=metadata]').val())
                    },
                    this.$node.find('.workspaceId').val()
                )
                    .then(function() {
                        if (li.closest('.collapsible').next('.collapsible').length) {
                            li.removeClass('editing');
                        }
                        this.onLoad();
                    })
                    .catch(function() {
                        self.showError();
                    })
            );
        };

        this.onCancel = function(event) {
            var li = $(event.target).closest('li'),
                section = li.closest('.collapsible');
            if (section.next('.collapsible').length === 0) {
                section.removeClass('expanded');
            } else {
                li.removeClass('editing');
            }
        };

        this.onToggleExpand = function(event) {
            var item = $(event.target)
                .closest('.collapsible')
                .toggleClass('expanded');

            if (item.next('.collapsible').length === 0) {
                item.find('.multivalue').addClass('editing');
            } else {
                item.find('.editing').removeClass('editing');
            }
        };

        this.onVerticesUpdated = function(event, data) {
            if (this.currentElementId) {
                var vertex = _.findWhere(data && data.vertices, { id: this.currentElementId })
                if (vertex) {
                    this.update(vertex);
                }
            }
        };

        this.onEdgesUpdated = function(event, data) {
            var edge = data && data.edges && _.findWhere(data.edges, { id: this.currentElementId });
            if (edge) {
                this.update(edge);
            }
        };

        this.onObjectsSelected = function(event, data) {
            var element = data && _.first(data.vertices) || _.first(data.edges);

            if (element) {
                this.select('elementInputSelector').val(element.id);
                this.update(element);
            }
        };

        this.onLoad = function() {
            var self = this,
                workspaceId = this.select('workspaceInputSelector').val(),
                elementId = this.select('elementInputSelector').val();

            // Try vertex, then edge store
            this.dataRequest('vertex', 'store', {
                workspaceId: workspaceId,
                vertexIds: elementId
            })
                .then(function(element) {
                    if (_.isUndefined(element)) {
                        return self.dataRequest('edge', 'store', {
                            workspaceId: workspaceId,
                            edgeIds: elementId
                        })
                    }
                    return element;
                })
                .then(function(element) {
                    self.update(element);
                })
        };

        this.onElementDelete = function(event) {
            var self = this,
                button = $(event.target),
                elementId = this.select('elementInputSelector').val(),
                workspaceId = this.select('workspaceInputSelector').val();

            this.handleSubmitButton(
                button,
                this.dataRequest('admin', self.elementType + 'Delete', elementId, workspaceId)
                    .then(function() {
                        self.$node.find('section').remove();
                        self.$node.find('.elementId').val('');
                        self.showSuccess('Successfully deleted ' + self.elementType);
                    })
                    .catch(function() {
                        self.showError();
                    })
            );
        };

        this.update = function(element) {
            this.validate();

            if (!element) {
                this.showError('Element does not exist');
                this.$node.find('section').remove();
                return;
            }

            this.hideError();
            this.elementType = F.vertex.isEdge(element) ? 'edge' : 'vertex';

            var newElement = element.id !== this.currentElementId,
                addNewText = i18n('admin.element.editor.addNewProperty.label');

            this.currentElementId = element.id;
            d3.select(this.$node.children('div')[0])
                .selectAll('section')
                .data(
                    _.chain(element.properties)
                      .groupBy('name')
                      .pairs()
                      .sortBy(function(pair) {
                          return pair[0];
                      })
                      .tap(function(pairs) {
                          pairs.push([
                              addNewText,
                              [{
                                  sandboxStatus: '',
                                  'http://visallo.org#visibilityJson': {
                                      source: ''
                                  },
                                  name: '',
                                  value: '',
                                  key: ''
                              }]
                          ]);
                      })
                      .value()
                )
                .call(function() {
                    this.enter()
                        .append('section').attr('class', 'collapsible')
                        .call(function() {
                            this.append('h1')
                                .call(function() {
                                    this.append('span').attr('class', 'badge');
                                    this.append('strong');
                                })
                            this.append('div').append('ol').attr('class', 'props inner-list');
                        });

                    this.order();
                    this.select('h1 strong')
                        .text(function(d) { return d[0]; })
                        .attr('title', function(d) { return d[0]; });
                    this.select('.badge').text(function(d) {
                        return F.number.pretty(d[1].length);
                    })
                    this.select('ol.props')
                        .selectAll('li.multivalue')
                        .data(function(d) {
                            return d[1];
                        })
                        .call(function() {
                            this.enter()
                                .append('li').attr('class', 'multivalue')
                                    .call(function() {
                                        this.append('div').attr('class', 'show-on-hover')
                                            .call(function() {
                                                this.append('button')
                                                    .attr('class', 'btn btn-mini prop-edit')
                                                    .text('Edit');
                                                this.append('button')
                                                    .attr('class', 'btn btn-danger btn-mini prop-delete')
                                                    .text('Delete');
                                            })
                                        this.append('ul').attr('class', 'inner-list');
                                        this.append('button')
                                            .attr('class', 'btn btn-primary prop-save')
                                            .text('Save');
                                        this.append('button')
                                            .attr('class', 'btn btn-default prop-cancel')
                                            .text('Cancel');
                                    })

                            this.order();
                            this.each(function(d) {
                                $(this).data('property', _.pick(d, 'name', 'key'));
                            })
                            this.select('ul').selectAll('li')
                                .data(function(d) {
                                    var notMetadata = ['name', 'key', 'value', 'sandboxStatus',
                                        'http://visallo.org#visibilityJson',
                                        '_sourceMetadata',
                                        'http://visallo.org#justification'
                                    ];

                                    return _.chain(d)
                                        .clone()
                                        .tap(function(property) {
                                            var vis = property.metadata &&
                                                property.metadata['http://visallo.org#visibilityJson'];

                                            property.metadata = _.omit(property.metadata, notMetadata);

                                            property['http://visallo.org#visibilityJson'] = vis || { source: '' };
                                        })
                                        .pairs()
                                        .reject(function(pair) {
                                            if (pair[0] === 'metadata') {
                                                return false;
                                            }

                                            return (pair[0] === 'name' && !(d.name === '' && d.key === '')) ||
                                                notMetadata.indexOf(pair[0]) === -1;
                                        })
                                        .sortBy(function(pair) {
                                            var order = (
                                                    'name key value sandboxStatus ' +
                                                    'http://visallo.org#visibilityJson'
                                                ).split(' '),
                                                index = order.indexOf(pair[0]);

                                            if (index >= 0) {
                                                return '' + index;
                                            }

                                            return ('' + order.length) + pair[0];
                                        })
                                        .value()
                                })
                                .call(function() {
                                    this.enter()
                                        .append('li')
                                        .append('label')
                                        .attr('class', 'nav-header')

                                    this.order().select('label').each(function(d) {
                                        var display = {
                                            'http://visallo.org#visibilityJson':
                                                i18n('admin.element.editor.visibility.label'),
                                            'http://visallo.org#justification':
                                                i18n('admin.element.editor.justification.label'),
                                            _sourceMetadata:
                                                i18n('admin.element.editor.justification.label'),
                                            sandboxStatus:
                                                i18n('admin.element.editor.sandboxStatus.label')
                                        };
                                        this.textContent = (display[d[0]] || d[0]) + ' ';
                                        d3.select(this)
                                            .call(function() {
                                                var isJustification = display[d[0]] === display['http://visallo.org#justification'],
                                                    isMetadata = d[0] === 'metadata' || isJustification,
                                                    displayAsJson = _.isObject(d[1]),
                                                    value = d[0] === 'http://visallo.org#visibilityJson' ?
                                                        d[1].source :
                                                        displayAsJson ? JSON.stringify(d[1], null, 4) : d[1];

                                                this.append('span')
                                                    .call(function() {
                                                        var cls = [];

                                                        if (isMetadata) {
                                                            cls.push('metadata');
                                                        }
                                                        if (isJustification) {
                                                            cls.push('justification');
                                                        }
                                                        if (displayAsJson) {
                                                            cls.push('display-as-json');
                                                        }
                                                        if (d[0] === 'key') {
                                                            cls.push('is-key');
                                                        }
                                                        if (cls.length) {
                                                            this.attr('class', cls.join(' '));
                                                        }

                                                        this.text(value);
                                                    });

                                                if (isMetadata || displayAsJson) {
                                                    this.append('textarea')
                                                        .attr('name', d[0])
                                                        .text(value)
                                                        .call(function() {
                                                            if (isJustification) {
                                                                this.attr('readonly', true);
                                                            }
                                                        })
                                                } else {
                                                    this.append('input')
                                                        .attr('name', d[0])
                                                        .call(function() {
                                                            if (d[0] === 'sandboxStatus') {
                                                                this.attr('readonly', true);
                                                            }
                                                        })
                                                        .attr('type', 'text')
                                                        .attr('value', value);
                                                }
                                            })
                                    });
                                })
                                .exit().remove();
                        })
                        .exit().remove();
                })
                .exit().remove();

        }

    }
});
