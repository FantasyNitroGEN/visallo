define(['flight/lib/component'], function(defineComponent) {
    'use strict';

    return defineComponent(DefaultConfiguration);

    function DefaultConfiguration() {

        this.defaultAttrs({
            inputSelector: 'input',
            buttonSelector: 'button'
        });

        this.after('initialize', function() {
            var self = this,
                configuration = this.attr.item.configuration || {},
                extension = this.attr.extension;

            $('<section><label><header>Title</header>' +
              '<button class="btn btn-link btn-small">reset</button></label>' +
              '<input type="text"></section>')
                .find('input')
                .val(this.getTitle())
                .end()
                .find('button')
                .css({
                    width: 'auto',
                    margin: '0 0 0 0.5em',
                    padding: '0',
                    'line-height': '1.3em'
                })
                .toggle(this.hasCustomTitle())
                .end()
                .appendTo(this.$node.empty())

            this.on('click', {
                buttonSelector: this.onReset
            });

            this.triggerChange = _.debounce(this.triggerChange.bind(this), 500);

            this.on('keyup change', {
                inputSelector: this.onChange
            });

            this.$node.closest('.dashboardConfigurePopover').on('cardTitleChanged', function(event, data) {
                self.select('inputSelector').val(data.title);
            });
        });

        this.hasCustomTitle = function() {
            var item = this.attr.item,
                config = item.configuration;
            return Boolean(item.title && (
                (config.initialTitle || this.attr.extension.title) !== item.title
            ));
        };

        this.getTitle = function() {
            var item = this.attr.item,
                config = item && item.configuration;
            return item.title || config.initialTitle || this.attr.extension.title;
        };

        this.onReset = function(event) {
            var item = this.attr.item;

            if (item.configuration) {
                if (item.configuration.initialTitle) {
                    item.title = item.configuration.initialTitle;
                } else {
                    item.title = '';
                }
            }
            this.select('inputSelector').val(this.getTitle());
            this.triggerChange();
        };

        this.onChange = function(event) {
            var title = event.target.value.trim();

            if (!title.length) return;

            this.attr.item.title = title;
            this.triggerChange();
        };

        this.triggerChange = function() {
            this.select('buttonSelector').toggle(this.hasCustomTitle());

            this.trigger('configurationChanged', {
                extension: this.attr.extension,
                item: this.attr.item,
                options: {
                    changed: 'item.title'
                }
            });
        };
    }
});
