
define([
    'flight/lib/component',
    '../withPopover',
    'util/vertex/formatters',
    'util/withDataRequest'
], function(
    defineComponent,
    withPopover,
    F,
    withDataRequest) {
    'use strict';

    return defineComponent(DetailPopover, withPopover, withDataRequest);

    function DetailPopover() {

        this.defaultAttrs({
            addButtonSelector: '.btn-primary',
            cancelButtonSelector: '.btn-default',
            zoomWithGraph: true
        });

        this.before('teardown', function() {
            this.trigger('closePreviewVertex', { vertexId: this.attr.vertexId })
        });

        this.after('teardown', function() {
            this.$node.remove();
        })

        this.before('initialize', function(node, config) {
            config.template = 'detail/template';
            config.teardownOnTap = false;
            config.hideDialog = true;
            config.keepInView = false;

            this.after('setupWithTemplate', function() {
                var self = this;

                // Make even with graph so below panes
                this.dialog.css({
                    'z-index': 50,
                    'pointer-events': 'none'
                });
                this.popover.css({
                    padding: 0,
                    'border-radius': '3px',
                    border: 'none'
                })
                this.popover.find('.popover-content').css({
                    'border-radius': '3px'
                });

                this.popover.find('.close-popover').css('pointer-events', 'all').on('click', function() {
                    self.teardown();
                })

                this.on('show', function() {
                    this.dialog.show();
                })
                this.on('hide', function() {
                    this.dialog.hide();
                })

                this.load()
                    .then(this.done.bind(this))
                    .catch(this.fail.bind(this));
            });
        });

        this.load = function() {
            if (this.attr.vertexId) {
                return this.dataRequest('vertex', 'store', { vertexIds: this.attr.vertexId });
            } else {
                return this.dataRequest('edge', 'store', { edgeIds: this.attr.edgeIds })
                    .then(function(elements) {
                        if (elements.length === 1) {
                            return elements[0];
                        }
                        return elements;
                    });
            }
        };

        this.fail = function() {
            var self = this;
            Promise.require('tpl!util/alert').then(function(alertTemplate) {
                self.popover.find('.popover-content')
                    .html(alertTemplate({ error: i18n('popovers.preview_vertex.error') }));
                self.dialog.css('display', 'block');
                self.positionDialog();
            })
        };

        this.done = function(element) {
            var self = this,
                $node = this.popover.find('.popover-content .type-content');

            $node.on('finishedLoadingTypeContent errorLoadingTypeContent', function() {
                self.dialog.css('display', 'block');
                self.positionDialog();
            });
            require(['detail/item/item'], function(Item) {
                Item.attachTo($node, {
                    model: element,
                    constraints: ['width', 'height'],
                    context: 'popup'
                });
            })
        };

    }
});
