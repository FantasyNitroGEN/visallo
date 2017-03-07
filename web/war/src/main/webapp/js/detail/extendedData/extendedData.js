define([
    'flight/lib/component',
    'util/component/attacher',
    'util/vertex/formatters'
], function (defineComponent, attacher, F) {
    'use strict';

    return defineComponent(HasInteractionWithDetailsShim);

    function HasInteractionWithDetailsShim() {
        this.attacher = null;

        this.after('initialize', function () {
            this.on('updateModel', this.onUpdateModel);

            this.$node.text('');

            attacher()
                .node(this.node)
                .path('detail/extendedData/ExtendedData')
                .params({
                    elementId: this.attr.data.id,
                    elementType: F.vertex.isEdge(this.attr.data) ? 'EDGE' : 'VERTEX',
                    extendedDataTableNames: this.attr.data.extendedDataTableNames,
                    onSearchExtendedData: this.handleSearchExtendedData.bind(this)
                })
                .attach()
                .then((attacher) => {
                    this.attacher = attacher;
                });
        });

        this.before('teardown', function () {
            if (this.attacher) {
                this.attacher.teardown();
            }
            $(this).empty();
        });

        this.handleSearchExtendedData = function (options) {
            this.trigger(document, 'searchByElementExtendedData', options);
        };

        this.onUpdateModel = function () {

        };
    }
});
