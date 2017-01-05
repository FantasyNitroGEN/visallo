define([
    'flight/lib/component',
    './viewerTpl.hbs',
    'util/withDataRequest',
    'util/vertex/formatters'
], function(defineComponent, template, withDataRequest, F) {
    'use strict';

    return defineComponent(JustificationViewer, withDataRequest);

    function JustificationViewer() {

        this.defaultAttrs({
            sourceInfoTitleSelector: '.sourceInfoTitle'
        })

        this.after('initialize', function() {
            var self = this;

            if (this.attr.linkToSource === undefined) {
                this.attr.linkToSource = this.$node.closest('a').length === 0;
            }

            this.$node.html(
                template(_.pick(this.attr, 'justificationMetadata', 'sourceMetadata', 'linkToSource'))
            );

            this.on('click', {
                sourceInfoTitleSelector: this.onSourceInfo
            });

            if (this.attr.sourceMetadata) {
                this.dataRequest('vertex', 'store', { vertexIds: this.attr.sourceMetadata.vertexId })
                    .then(function(vertex) {
                        var title = vertex && F.vertex.title(vertex);
                        self.select('sourceInfoTitleSelector').text(
                            title || i18n('popovers.property_info.title_unknown')
                        );
                        self.trigger('positionDialog');
                    });
            }
        });

        this.onSourceInfo = function(e) {
            e.preventDefault();

            var metadata = this.attr.sourceMetadata,
                vertexId = metadata.vertexId,
                textPropertyKey = metadata.textPropertyKey,
                textPropertyName = metadata.textPropertyName,
                offsets = [metadata.startOffset, metadata.endOffset];

            this.trigger(document, 'selectObjects', {
                vertexIds: [vertexId],
                focus: {
                    vertexId: vertexId,
                    textPropertyKey: textPropertyKey,
                    textPropertyName: textPropertyName,
                    offsets: offsets
                }
            })
        };
    }
});
