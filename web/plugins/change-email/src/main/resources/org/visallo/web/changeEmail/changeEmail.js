define(['configuration/plugins/registry'], function(registry) {
    'use strict';

    registry.registerExtension('org.visallo.user.account.page', {
        identifier: 'changeEmail',
        pageComponentPath: 'org.visallo.useraccount.changeEmail'
    });

    define('org.visallo.useraccount.changeEmail', [
        'flight/lib/component',
        'util/withFormFieldErrors',
        'tpl!util/alert'
    ], function(defineComponent, withFormFieldErrors, alertTemplate) {
        return defineComponent(ChangeEmail, withFormFieldErrors);

        function ChangeEmail() {
            this.defaultAttrs({
                buttonSelector: 'button.btn-primary',
                inputSelector: 'input'
            });

            this.after('initialize', function() {
                var self = this;

                require(['org/visallo/web/changeEmail/template.hbs'], function(template) {
                    self.$node.html(template({
                        email: visalloData.currentUser.email
                    }));
                    self.validateEmail();
                });

                this.on('click', {
                    buttonSelector: this.onChange
                });
                this.on('change keyup', {
                    inputSelector: this.validateEmail
                });
            });

            this.validateEmail = function(event) {
                var inputs = this.select('inputSelector'),
                    anyInvalid = inputs.filter(function(i, input) {
                                    return input.validity && !input.validity.valid;
                                 }).length;

                if (anyInvalid) {
                    this.select('buttonSelector').attr('disabled', true);
                } else {
                    this.select('buttonSelector').removeAttr('disabled');
                }
            };

            this.onChange = function(event) {
                var self = this,
                    btn = $(event.target).addClass('loading').attr('disabled', true),
                    newEmail = this.$node.find('.current').val();

                this.clearFieldErrors(this.$node);
                this.$node.find('.alert-info').remove();

                $.post('changeEmail', {
                    email: newEmail,
                    csrfToken: visalloData.currentUser.csrfToken
                })
                    .always(function() {
                        btn.removeClass('loading').removeAttr('disabled');
                    })
                    .fail(function(e) {
                        self.markFieldErrors(e && e.responseText || e, self.$node);
                    })
                    .done(function() {
                        visalloData.currentUser.email = newEmail;
                        self.$node.prepend(alertTemplate({
                            message: i18n('useraccount.page.changeEmail.success')
                        }));
                    })
            };
        }
    })
})
