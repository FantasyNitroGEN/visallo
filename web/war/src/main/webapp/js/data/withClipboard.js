define([
    'util/clipboardManager',
    'util/promise'
], function(ClipboardManager, Promise) {
    'use strict';

    return withClipboard;

    function getObjectsFromClipboardData(data) {
        return new Promise(function(fulfill) {
            var objects = { vertexIds: [], edgeIds: [] }

            if (data) {
                require(['util/vertex/urlFormatters'], function(F) {
                    var p = F.vertexUrl.parametersInUrl(data);
                    if (p && p.vertexIds) {
                        fulfill({...p});
                    } else {
                        fulfill(objects);
                    }
                })
            } else {
                fulfill(objects);
            }
        });
    }

    function formatVertexAction(action, vertices) {
        var len = vertices.length;
        return i18n('element.clipboard.action.' + (
            len === 1 ? 'one' : 'some'
        ), i18n('element.clipboard.action.' + action.toLowerCase()), len);
    }

    function withClipboard() {

        this.after('initialize', function() {
            ClipboardManager.attachTo(this.$node);

            this.on('clipboardPaste', this.onClipboardPaste);
            this.on('clipboardCut', this.onClipboardCut);
        });

        this.onClipboardCut = function(evt, data) {
            var self = this;

            getObjectsFromClipboardData(data.data).then(elements => {
                this.trigger('elementsCut', elements);
            });
        };

        this.onClipboardPaste = function(evt, data) {
            var self = this;

            getObjectsFromClipboardData(data.data).then(elements => {
                const { vertexIds, edgeIds } = elements;
                if (elements && !(_.isEmpty(vertexIds) && _.isEmpty(edgeIds))) {
                    this.trigger('elementsPasted', elements);
                } else {
                    this.trigger('genericPaste', data);
                }
            });
        };
    }
});
