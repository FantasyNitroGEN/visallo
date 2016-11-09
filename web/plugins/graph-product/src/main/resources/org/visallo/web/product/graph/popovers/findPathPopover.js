
define([
    'flight/lib/component',
    './withVertexPopover',
    'util/formatters',
    'util/withFormFieldErrors',
    'util/withDataRequest'
], function(
    defineComponent,
    withVertexPopover,
    F,
    withFormFieldErrors,
    withDataRequest) {
    'use strict';

    return defineComponent(FindPathPopover, withVertexPopover, withFormFieldErrors, withDataRequest);

    function FindPathPopover() {

        this.defaultAttrs({
            buttonSelector: 'button.find-path',
            removeSelector: 'button.remove-edge',
            edgeSelector: 'select.edgeLabel',
            hopSelector: 'select.hops'
        });

        this.before('teardown', function() {
            this.trigger('finishedVertexConnection');
        });

        this.before('initialize', function(node, config) {
            config.hops = config.connectionData && config.connectionData.hops || 2;
            config.template = 'findPathPopover';
        });

        this.getTemplate = function() {
            return new Promise(f => require(['./findPathPopoverTpl'], f));
        };

        this.popoverInitialize = function() {
            this.edgeLabels = [];
            this.hops = 3;
            this.trigger('defocusPaths');

            this.positionDialog();

            this.loadEdgeTypes();

            this.on(this.popover, 'change', {
                edgeSelector: this.onChangeEdge,
                hopSelector: this.onChangeHops
            })
            this.on(this.popover, 'click', {
                buttonSelector: this.onFindPath,
                removeSelector: this.onRemove
            })
        };

        this.onRemove = function() {
            var $li = $(event.target).closest('li'),
                id = $li.data('id');

            if (id) {
                this.edgeLabels = _.without(this.edgeLabels, id);
                $li.remove();
                this.positionDialog();
            }
        };

        this.onChangeHops = function(event) {
            this.hops = $(event.target).val();
        };

        this.onChangeEdge = function(event) {
            var $select = $(event.target),
                id = $select.val(),
                relationship = this.relationships.byTitle[id];

            if (id && relationship) {
                this.popover.find('.popover-content ul').append(
                    $('<li>').text(relationship.displayName)
                        .data('id', relationship.title)
                        .append('<button class="btn btn-danger btn-mini remove-edge">&times;</button>')
                );

                this.edgeLabels.push(relationship.title);
                this.positionDialog();

                $select.val('');
            }
        };

        this.loadEdgeTypes = function() {
            var self = this;
            this.dataRequest('ontology', 'relationships')
                .done(function(relationships) {
                    self.relationships = relationships;
                    self.popover.find('select.edgeLabel').html(
                        $.map(
                            _.chain(relationships.list)
                                .filter(function(r) {
                                    return r.userVisible !== false;
                                })
                                .sortBy('displayName')
                                .tap(function(r) {
                                    r.splice(0, 0, {
                                        displayName: 'Limit Edge Type...',
                                        title: ''
                                    })
                                })
                                .value(),
                            function(r) {

                                return $('<option>').val(r.title).text(r.displayName);
                            }
                        )
                    )
                })
        }


        this.onFindPath = function(event) {
            if ($(event.target).is('.remove-edge')) return;

            var self = this,
                $target = $(event.target).addClass('loading'),
                parameters = {
                    outVertexId: this.attr.outVertexId,
                    inVertexId: this.attr.inVertexId,
                    depth: 5,
                    hops: this.hops,
                    edgeLabels: this.edgeLabels
                },
                buttons = this.popover.find('button').attr('disabled', true);

            this.clearFieldErrors(this.popover);
            this.dataRequest('vertex', 'findPath', parameters)
                .then(function() {
                    self.teardown();
                    self.trigger('showActivityDisplay');
                })
                .catch(function(e) {
                    console.error(e);
                    buttons.removeAttr('disabled');
                    $target.removeClass('loading');
                    self.markFieldErrors(i18n('popovers.find_path.error'), self.popover);
                })
        };
    }
});
