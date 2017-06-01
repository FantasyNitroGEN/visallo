
define([
    'flight/lib/component'
], function(defineComponent) {
    'use strict';

    return defineComponent(ClipboardManager);

    /**
     * Manages clipboard events
     */
    function ClipboardManager() {

        this.after('initialize', function() {
            var self = this;

            this.on('applicationReady currentUserVisalloDataUpdated', _.once(function() {
                require(['rangy-core', 'rangy-text', 'rangy-highlighter'], function(rangy) {
                    if (!rangy.initialized) rangy.init();
                });
            }));

            // Fullscreen view isn't loaded yet but this isn't needed
            // if it will be
            if (/^#?v=/.test(window.location.hash)) {
                return;
            }

            this.textarea = $('<textarea class="clipboardManager" autocomplete="false" spellcheck="false" />')
                .css({
                    position: 'absolute',
                    zIndex: 10,
                    cursor: 'pointer',
                    top: '-300px',
                    height: '10px',
                    boxSizing: 'border-box'
                })
                .on({
                    cut: this.onCut.bind(this),
                    copy: this.onCopy.bind(this),
                    paste: this.onPaste.bind(this),
                    keydown: this.onKeyDown.bind(this),
                    keyup: this.onKeyDown.bind(this)
                })
                .appendTo(document.body);

            this.on('click', this._onClick);
            this.on('clipboardClear', this.clear);
            this.on('clipboardSet', this.set);
            this.on('clipboardFocus', this.focus);
        });

        this.onKeyDown = function(event) {
            this.textarea.val(this.lastSetData && this.lastSetData.text || '').focus().select();
        };

        this.focus = function() {
            this.textarea.focus();
        };

        this.set = function(event, data) {
            this.lastSetData = data;
            this.textarea.val(data && data.text || '').focus().select();
        };

        this.clear = function() {
            this.lastSetData = null;
            this.textarea.val('');
        };

        this.onPaste = function(event) {
            var self = this;
            _.defer(function() {
                require(['util/vertex/urlFormatters'], function(F) {
                    var textarea = self.textarea,
                        val = textarea.val(),
                        parameters = F.vertexUrl.parametersInUrl(val),
                        vertices = parameters && parameters.vertexIds && parameters.vertexIds.length || 0,
                        edges = parameters && parameters.edgeIds && parameters.edgeIds.length || 0,
                        total = vertices + edges;

                    console.debug('Clipboard: Paste', val);

                    self.trigger('clipboardPaste', {data: val});
                    self.lastSetData = null;
                    textarea.val('').focus();

                    if (total === 1) {
                        self.trigger('displayInformation', {message: i18n('element.clipboard.paste.one')});
                    } else if (total > 1) {
                        self.trigger('displayInformation', {
                            message: i18n('element.clipboard.paste.some', total)
                        });
                    }
                });
            });
        };

        this.onCopy = function() {
            console.debug('Clipboard: Copy', this.textarea.val());

            var self = this,
                val = this.textarea.val();

            if (val.length) {
                require(['util/vertex/urlFormatters'], function(F) {
                    var parameters = F.vertexUrl.parametersInUrl(val),
                        vertices = parameters && parameters.vertexIds && parameters.vertexIds.length || 0,
                        edges = parameters && parameters.edgeIds && parameters.edgeIds.length || 0,
                        total = vertices + edges;

                    if (total === 1) {
                        self.trigger('displayInformation', { message: i18n('element.clipboard.copy.one') });
                    } else if (total > 1) {
                        self.trigger('displayInformation', {
                            message: i18n('element.clipboard.copy.some', total)
                        });
                    } else if (!total) {
                        self.trigger('displayInformation', { message: i18n('clipboard.copy') });
                    }
                })
            }
        };

        this.onCut = function() {
            var val = this.textarea.val();
            console.debug('Clipboard: Cut', val);
            this.trigger('clipboardCut', { data: val });
        };

        this._onClick = function(event) {
            if ($(event.target).is('input,select,textarea,.Select,.Select *,.visallo-allow-focus,.visallo-allow-focus *')) return;

            var inFocus = $(':focus');
            // Check for previous focus, since we are going to steal it to
            // support browser cut/copy/paste events
            if (inFocus.length) {
                this.trigger(inFocus[0], 'focusLostByClipboard');
            } else {
                this.trigger(event.target, 'focusLostByClipboard');
            }

            if (window.getSelection().isCollapsed === false) return;

            this.focus();
        };

    }
});
