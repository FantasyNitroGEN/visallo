define([
    'util/formatters',
    'tpl!util/alert'
], function(F, alertTemplate) {
    'use strict';

    return withFormHelpers;

    function withFormHelpers() {

        this.showSuccess = function(message) {
            this.$node.find('.alert').remove();
            this.$node.prepend(alertTemplate({ message: message || i18n('admin.plugin.success') }));
        };
        this.showError = function(message) {
            this.hideError();
            this.$node.prepend(alertTemplate({ error: message || i18n('admin.plugin.error') }));
        };
        this.hideError = function() {
            this.$node.find('.alert').remove();
        };
        this.handleSubmitButton = function(button, promise) {
            var $button = $(button),
                text = $button.text();

            $button.attr('disabled', true);

            if (promise.progress) {
                promise.progress(function(v) {
                    $button.text(F.number.percent(v) + ' ' + text);
                })
            }

            return promise.finally(function() {
                $button.removeAttr('disabled').text(text);
            });
        };
    }
});
