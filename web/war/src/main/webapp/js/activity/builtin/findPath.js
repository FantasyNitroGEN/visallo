/*eslint no-labels:0*/
define([
    'flight/lib/component',
    'util/formatters',
    'util/withDataRequest'
], function(
    defineComponent,
    F,
    withDataRequest) {
    'use strict';

    return defineComponent(FindPath, withDataRequest);

    function FindPath() {

        this.after('teardown', function() {
            this.$node.empty();
            this.trigger('defocusPaths');
        });

        this.defaultAttrs({
            pathsSelector: '.found-paths',
            addVerticesSelector: '.add-vertices'
        });

        this.after('initialize', function() {

            this.on('click', {
                pathsSelector: this.onPathClick,
                addVerticesSelector: this.onAddVertices
            });

            this.loadDefaultContent();

            this.on(document, 'focusPaths', this.onFocusPaths);
            this.on(document, 'defocusPaths', this.onDefocusPaths);
            this.on(document, 'workspaceLoaded', this.onWorkspaceLoaded);
        });

        this.updateButton = function($button, workspaceId) {
            var self = this,
                onDifferentWorkspace = workspaceId !== this.attr.process.workspaceId,
                noResults = (self.attr.process.resultsCount || 0) === 0,
                disabled = onDifferentWorkspace || noResults;

            if (disabled) {
                $button.attr('disabled', true);
            } else {
                $button.removeAttr('disabled');
            }

            $button.attr('title', onDifferentWorkspace ?
                i18n('popovers.find_path.wrong_workspace') :
                i18n('popovers.find_path.show_path'));
        };

        this.onWorkspaceLoaded = function(event, data) {
            this.updateButton(this.select('pathsSelector'), data.workspaceId);
        };

        this.loadDefaultContent = function() {
            var count = this.attr.process.resultsCount || 0,
                $button = $('<button>').addClass('found-paths btn btn-mini')
                    .text(
                        i18n('popovers.find_path.paths.' + (
                             count === 0 ? 'none' : count === 1 ? 'one' : 'some'
                        ), F.number.pretty(count))
                    );

            this.updateButton($button, visalloData.currentWorkspaceId);

            this.$node.empty().append($button);
        };

        this.onFocusPaths = function(event, data) {
            if (data.processId !== this.attr.process.id) {
                this.loadDefaultContent();
            }
        };

        this.onDefocusPaths = function(event, data) {
            this.loadDefaultContent();
        };

        this.onAddVertices = function(event) {
            this.trigger('focusPathsAddVertexIds', { vertexIds: this.toAdd })
            this.loadDefaultContent();
        };

        this.onPathClick = function(event) {
            var self = this,
                $target = $(event.target).addClass('loading').attr('disabled', true);

            this.dataRequest('longRunningProcess', 'get', this.attr.process.id)
                .done(function(process) {
                    var paths = process.results && process.results.paths || [],
                        allVertices = _.flatten(paths),
                        vertices = _.chain(allVertices)
                            .unique()
                            .value();

                    self.toAdd = vertices;
                    self.trigger('focusPaths', {
                        paths: paths,
                        sourceId: self.attr.process.outVertexId,
                        targetId: self.attr.process.inVertexId,
                        processId: self.attr.process.id
                    });

                    $target.hide();

                    var $addButton = $('<button>').addClass('btn btn-mini btn-primary add-vertices');

                    if (vertices.length === 0) {
                        $addButton.attr('disabled', true);
                    }

                    $addButton.text(i18n('popovers.find_path.add'));
                    self.$node.append($addButton);
                })
        };
    }
});
