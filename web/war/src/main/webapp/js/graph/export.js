define([
    'flight/lib/component',
    'hbs!./exportTpl'
], function(
    defineComponent,
    template) {
    'use strict';

    return defineComponent(GraphExport);

    function GraphExport() {

        this.attributes({
            viewSelector: 'input[name=crop]',
            transparentSelector: 'input[name=bg]',
            downloadSelector: 'a.download',
            cy: null
        })

        this.before('teardown', function() {
            this.unregisterCyListeners();
        })

        this.after('initialize', function() {
            var state = {
                full: true,
                transparent: false,
                workspaceName: visalloData.currentWorkspaceName
            };

            this.state = state;
            this.updatePreview = _.debounce(this.updatePreview.bind(this), 100);
            this.unregisterCyListeners = function() {};

            this.on('click', {
                downloadSelector: this.onDownload
            });
            this.on('change', {
                viewSelector: this.onChangeView,
                transparentSelector: this.onChangeBackground
            });

            if (!this.state.full) {
                this.registerCyListeners();
            }

            this.$node.html(template(state));
            this.updatePreview();
        });

        this.onDownload = function(event) {
            var self = this;
            _.delay(function() {
                self.trigger('closePopover');
            }, 250);
        };

        this.registerCyListeners = function() {
            var cy = this.attr.cy;
            var updatePreview = this.updatePreview.bind(this);
            this.unregisterCyListeners = function() {
                cy.off('zoom pan', updatePreview);
            }
            cy.on('zoom pan', updatePreview);
        };

        this.onChangeView = function(event) {
            this.state.full = event.target.value === 'full'
            if (this.state.full) {
                this.unregisterCyListeners();
            } else {
                this.registerCyListeners();
            }
            this.updatePreview();
        };

        this.onChangeBackground = function(event) {
            this.state.transparent = event.target.checked
            this.updatePreview();
        };

        this.updatePreview = function() {
            var cy = this.attr.cy,
                dataUri = cy.png({
                    full: this.state.full,
                    scale: 2,
                    bg: this.state.transparent ? false : 'white'
                });

            this.$node.find('.preview')[0].style.backgroundImage = 'url(' + dataUri + ')';
            this.select('downloadSelector').attr('disabled', false)[0].href = dataUri;
            this.trigger('positionDialog');
        };

    }
});
