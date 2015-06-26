require(['configuration/plugins/userAccount/plugin'], function(UserAccountPlugin) {
    UserAccountPlugin.registerUserAccountPage({
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
                buttonSelector: 'button'
            });

            this.after('initialize', function() {
                var self = this;

                require(['hbs!org/visallo/web/changeEmail/template'], function(template) {
                    self.$node.html(template({
                        email: visalloData.currentUser.email
                    }));
                });

                this.on('click', {
                    buttonSelector: this.onChange
                })
            });

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
