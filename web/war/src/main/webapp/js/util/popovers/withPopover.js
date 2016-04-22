define([], function() {
    'use strict';

    var ERROR_NO_POSITION_RESPONSE =
            'Unable to attach popover, ' +
            'nothing responded to registerForPositionChanges';

    return withPopover;

    function withPopover() {

        this.defaultAttrs({
            withPopoverInputSelector: 'input,select',
            hideDialog: false
        })

        this.before('teardown', function() {
            clearTimeout(this.positionChangeErrorCheck);
            $(document).off('.popoverclose')
            this.trigger('unregisterForPositionChanges')
        });

        this.after('teardown', function() {
            this.dialog.remove();
            $('.popover-bg-overlay').remove();
        });

        this.after('initialize', function() {
            var t = this.attr.template || 'noTemplate',
                path;

            if (/^\//.test(t)) {
                path = 'hbs!' + t.substring(1);
            } else {
                path = 'hbs!util/popovers/' + t;
            }

            require([path], this.setupWithTemplate.bind(this));
        });

        this.setupWithTemplate = function(tpl) {
            var self = this;

            if (this.attr.overlay) {
                $(document.body).append('<div class="popover-bg-overlay">')
            }

            this.dialog = $('<div class="dialog-popover">')
                .css({
                    position: 'absolute',
                    display: this.attr.hideDialog ? 'none' : 'block'
                })
                .html(tpl(this.attr))
                .appendTo(document.body);

            if (this.$node.closest('.modal').length) {
                this.dialog.css('zIndex', 1050);
            }

            this.popover = this.dialog.find('.popover');

            this.on(this.popover, 'positionDialog', this.positionDialog);
            this.on(this.popover, 'closePopover', this.teardown);
            this.on(this.popover, 'keyup', {
                withPopoverInputSelector: this.withPopoverOnKeyup
            })

            if (this.attr.teardownOnTap !== false) {
                this.on(document, 'mousedown', function mousedown(e) {
                    var x = e.clientX, y = e.clientY;
                    this.on(document, 'mouseup', function mouseup(e) {
                        this.off(document, 'mouseup', mouseup);

                        if ($(e.target).closest(self.popover).length) {
                            return;
                        }
                        var x2 = e.clientX, y2 = e.clientY,
                            distance = Math.sqrt((x2 - x) * (x2 - x) + (y2 - y) * (y2 - y))

                        if (distance < 5) {
                            // Wait a little in case other event handlers are
                            // looking for popover
                            _.defer(function() {
                                self.teardown();
                            })
                        }
                    })
                })
            }

            this.registerAnchorTo();
        };

        this.withPopoverOnKeyup = function(event) {
            if (this.enterShouldSubmit && event.which === $.ui.keyCode.ENTER) {
                var selector = this.attr[this.enterShouldSubmit];
                if (selector) {
                    this.popover.find(this.attr[this.enterShouldSubmit]).not(':disabled').click();
                } else {
                    console.warn('Selector to trigger on enter not found in popover', selector);
                }
            }
        };

        this.registerAnchorTo = function() {
            var self = this;

            this.on('positionChanged', this.onPositionChange);
            this.trigger('registerForPositionChanges', this.attr);

            this.positionChangeErrorCheck = _.delay(function() {
                if (!self.dialogPosition) {
                    console.error(ERROR_NO_POSITION_RESPONSE);
                    var w = $(window),
                        width = w.width(),
                        height = w.height();
                    self.dialogPosition = { x: width / 2, y: height / 2 };
                    self.positionDialog();
                }
            }, 500)
        };

        this.onPositionChange = function(event, data) {
            clearTimeout(this.positionChangeErrorCheck);
            var allBlank = !data || _.every(data.position, function(val) {
                    return val === 0;
                });

            if (allBlank) {
                if (!this.dialog.data('preventTeardown')) {
                    this.teardown();
                }
            } else {
                this.dialogPosition = data.position;
                this.dialogPositionIf = data.positionIf;
                this.dialogPositionZoom = data.zoom || 1;
                this.positionDialog();
                if (!this.throttledPositionDialog) {
                    this.throttledPositionDialog = true;
                    _.delay(function() {
                        this.positionDialog = _.throttle(this.positionDialog.bind(this), 1000 / 60);
                    }.bind(this), 500);
                }
            }
        };

        this.positionDialog = function() {
            var pos = this.dialogPositionIf && this.dialogPositionIf.above || this.dialogPosition;
            if (pos) {
                var $arrow = this.dialog.find('.arrow'),
                    scaling = this.attr.zoomWithGraph ?
                        (Math.min(1, Math.max(0.1, this.dialogPositionZoom / 0.4))) : 1,
                    menubarWidth = $('.menubar-pane').width() || 0,
                    padding = $arrow.outerHeight(true) * scaling,
                    width = this.popover.outerWidth() * scaling,
                    height = this.popover.outerHeight() * scaling,
                    windowWidth = $(window).width(),
                    windowHeight = $(window).height(),
                    maxLeft = windowWidth - width,
                    maxTop = windowHeight - height,
                    proposedForPosition = function(pos, aboveOrBelow) {
                        var calcLeft = pos.x - (width / 2),
                            calcTop;
                        if (aboveOrBelow === 'above') {
                            calcTop = (pos.yMin || pos.y) - height - padding;
                        } else {
                            calcTop = (pos.yMax || pos.y) + padding;
                        }
                        return {
                            left: Math.max(menubarWidth + padding, Math.min(maxLeft - padding, calcLeft)),
                            top: Math.max(padding, Math.min(maxTop - padding, calcTop))
                        };
                    },
                    proposed = proposedForPosition(pos, 'above');

                if (proposed.top + height > pos.y) {
                    proposed = proposedForPosition(
                        this.dialogPositionIf && this.dialogPositionIf.below || this.dialogPosition,
                        'below'
                    );
                    if (!~this.popover[0].className.indexOf('bottom')) {
                        this.popover.removeClass('top').addClass('bottom');
                    }
                } else if (!~this.popover[0].className.indexOf('top')) {
                    this.popover.removeClass('bottom').addClass('top');
                }

                var arrowLeft = pos.x - proposed.left,
                    arrowPadding = padding * 1.5,
                    maxLeftAllowed = width - arrowPadding,
                    percent = (Math.max(arrowPadding, Math.min(maxLeftAllowed, arrowLeft)) / width * 100) + '%';

                $arrow.css('left', percent);

                proposed.transform = 'translate(' + Math.round(proposed.left) + 'px,' + Math.round(proposed.top) + 'px)';
                proposed.transformOrigin = '0 0';
                delete proposed.top;
                delete proposed.left;

                if (this.attr.zoomWithGraph) {
                    proposed.transform += ' scale(' + scaling.toFixed(3) + ')';
                }
                this.dialog.css(proposed);
                this.popover.show();
            }
        }
    }
});
