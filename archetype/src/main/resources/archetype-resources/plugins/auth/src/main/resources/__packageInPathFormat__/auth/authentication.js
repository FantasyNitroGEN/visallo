#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
define([
    'public/v1/api',
    './login.hbs'
], function(
    visallo,
    template) {
    'use strict';

    return visallo.defineComponent(ExampleAuthentication);

    function ExampleAuthentication() {

        this.defaultAttrs({
            errorSelector: '.text-error',
            usernameSelector: '.login input.username',
            passwordSelector: 'input.password',
            loginButtonSelector: '.login .btn-primary',
            signInButtonSelector: '.signin',
            loginFormSelector: '.login'
        });

        this.after('initialize', function() {
            var self = this;

            this.${symbol_dollar}node.html(template(this.attr));
            this.enableButton(false);

            this.on('click', {
                loginButtonSelector: this.onLoginButton,
                signInButtonSelector: this.onSignInButton
            });

            this.on('keyup change paste', {
                usernameSelector: this.onUsernameChange,
                passwordSelector: this.onPasswordChange
            });

            this.select('usernameSelector').focus();
        });

        this.onSignInButton = function(event) {
            event.preventDefault();

            var form = this.select('loginFormSelector').show();
            _.defer(function() {
                form.find('input').eq(0).focus();
            });
        };

        this.checkValid = function() {
            var self = this,
                user = this.select('usernameSelector'),
                pass = this.select('passwordSelector');

            _.defer(function() {
                self.enableButton(
                    ${symbol_dollar}.trim(user.val()).length > 0 &&
                    ${symbol_dollar}.trim(pass.val()).length > 0
                );
            });
        };

        this.onUsernameChange = function(event) {
            this.checkValid();
        };

        this.onPasswordChange = function(event) {
            this.checkValid();
        };

        this.onLoginButton = function(event) {
            var self = this,
                ${symbol_dollar}error = this.select('errorSelector'),
                ${symbol_dollar}username = this.select('usernameSelector'),
                ${symbol_dollar}password = this.select('passwordSelector');

            event.preventDefault();
            event.stopPropagation();
            event.target.blur();

            if (this.submitting) {
                return;
            }

            this.enableButton(false, true);
            this.submitting = true;
            ${symbol_dollar}error.empty();

            ${symbol_dollar}.post('login', {
                username: ${symbol_dollar}username.val(),
                password: ${symbol_dollar}password.val()
            }).fail(function(xhr, status, error) {
                self.submitting = false;
                if (xhr.status === 403) {
                    error = i18n('${package}.auth.invalid');
                }
                ${symbol_dollar}error.text(error);
                self.enableButton(true);
            })
            .done(function() {
                self.trigger('loginSuccess');
            })
        };

        this.enableButton = function(enable, loading) {
            if (this.submitting) return;
            var button = this.select('loginButtonSelector');

            if (enable) {
                button.removeClass('loading').removeAttr('disabled');
            } else {
                button.toggleClass('loading', !!loading)
                    .attr('disabled', true);
            }
        };
    }

});
