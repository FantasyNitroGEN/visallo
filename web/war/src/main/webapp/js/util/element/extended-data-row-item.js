define([
    'flight/lib/component',
    'tpl!./extended-data-row-item',
    'util/vertex/formatters'
], function(
    defineComponent,
    template,
    F) {
    'use strict';

    return defineComponent(ExtendedDataRowItem);

    function ExtendedDataRowItem() {

        this.after('initialize', function() {
            this.extendedDataRow = this.attr.item;

            const timeSubtitle = F.vertex.time(this.extendedDataRow),
                subtitle = F.vertex.subtitle(this.extendedDataRow);

            this.$node
                .addClass('extended-data-item')
                .addClass(timeSubtitle ? 'has-timeSubtitle' : '')
                .addClass(subtitle ? 'has-subtitle' : '')
                .html(template({
                    title: F.vertex.title(this.extendedDataRow),
                    timeSubtitle: timeSubtitle,
                    subtitle: subtitle
                }));

            this.$node.data('extendedData', true);
            if (this.extendedDataRow.id.elementType === 'VERTEX') {
                this.$node.data('vertexId', this.extendedDataRow.id.elementId);
            } else if (this.extendedDataRow.id.elementType === 'EDGE') {
                this.$node.data('edgeId', this.extendedDataRow.id.elementId);
            } else {
                console.error('unhandled element type: ' + this.extendedDataRow.id.elementType);
            }
        });

        this.before('teardown', function() {
            this.$node.removeData('vertexId');
            this.$node.removeData('edgeId');
            this.$node.removeClass('extended-data-item has-timeSubtitle has-subtitle');
            this.$node.empty();
        });
    }
});
