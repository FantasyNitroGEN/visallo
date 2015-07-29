define([
    'flight/lib/component',
    'configuration/admin/utils/withFormHelpers',
    'hbs!org/visallo/web/importExportWorkspaces/export',
    'util/formatters',
    'util/messages',
    'util/withDataRequest',
    'd3'
], function(
    defineComponent,
    withFormHelpers,
    template,
    F,
    i18n,
    withDataRequest,
    d3
    ) {
    'use strict';

    return defineComponent(WorkspaceExport, withDataRequest, withFormHelpers);

    function WorkspaceExport() {

        this.defaultAttrs({
            selectSelector: 'select'
        });

        this.after('initialize', function() {
            var self = this;

            this.$node.html(template({}));

            this.on('change', {
                selectSelector: this.onChange
            });

            this.dataRequest('workspace', 'all')
                .then(function(workspaces) {
                    self.select('selectSelector')
                        .append(
                            workspaces.map(function(workspace) {
                                return $('<option>')
                                    .val(workspace.workspaceId)
                                    .text(workspace.title);
                            })
                        ).change();

                    if (workspaces.length) {
                        self.$node.find('a').removeAttr('disabled');
                    }
                })
                .catch(this.showError.bind(this, i18n('admin.workspace.export.workspace.error')))
                .finally(function() {
                    self.$node.find('.badge').remove();
                })
        });

        this.onChange = function() {
            var select = this.$node.find('select'),
                workspaceId = select.val();

            this.$node.find('a').attr({
                download: $(select.get(0).selectedOptions).text() + '.visalloWorkspace',
                href: 'admin/workspace/export?' + $.param({ workspaceId: workspaceId })
            });
        };

    }
});
