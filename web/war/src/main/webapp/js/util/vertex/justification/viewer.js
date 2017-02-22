/**
 * Renders the json justification object. Could be either justification text or
 * full source info with link to source.
 *
 * One of `justificationMetadata` or `sourceMetadata` must be provided.
 * If both justificationMetadata and sourceMetadata are given, only justificationMetadata is used.
 *
 * @module
 * @flight Displays justification information
 * @attr {boolean} [linkToSource=true] Show the source link if available
 * @attr {object} [justificationMetadata]
 * @attr {string} justificationMetadata.justificationText The text to display
 * @attr {object} [sourceMetadata]
 * @attr {string} sourceMetadata.snippet The snippet from source material to display
 * @attr {string} sourceMetadata.textPropertyKey The property key of the text property in source
 * @attr {string} sourceMetadata.textPropertyName The property name of the text property in source
 * @attr {string} sourceMetadata.startOffset The character start index of snippet in source
 * @attr {string} sourceMetadata.endOffset The character end index of snippet in source
 * @attr {string} sourceMetadata.vertexId The vertexId of the source
 * @example <caption>Text</caption>
 * JustificationViewer.attachTo(node, {
 *     justificationMetadata: {
 *         justificationText: 'Justification for property here'
 *     }
 * })
 * @example <caption>Source Reference</caption>
 * JustificationViewer.attachTo(node, {
 *     sourceMetadata: {
 *         snippet: '[html snippet]',
 *         vertexId: vertexId,
 *         textPropertyKey: textPropertyKey,
 *         textPropertyName: textPropertyName,
 *         startOffset: 0,
 *         endOffset: 42
 *     }
 * })
 */
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
