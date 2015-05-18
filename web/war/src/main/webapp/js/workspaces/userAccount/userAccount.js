define([
    'flight/lib/component',
    'hbs!./pageList'
], function(
    defineComponent,
    listTemplate) {
    'use strict';

    return defineComponent(UserAccount);

    function UserAccount() {

        this.defaultAttrs({
            listSelector: '.modal-body > .nav',
            listItemSelector: '.modal-body > .nav li a',
            pageSelector: '.modal-body > .page'
        });

        this.after('teardown', function() {
            this.$node.remove();
        });

        this.after('initialize', function() {
            var self = this;

            this.on('hidden', this.teardown);

            this.on('click', {
                listItemSelector: this.onChangePage
            });

            require(['configuration/plugins/registry'], function(registry) {
                registry.registerExtension('org.visallo.user.account.page', {
                    identifier: 'access',
                    pageComponentPath: 'workspaces/userAccount/bundled/access/access'
                });
                var pages = registry.extensionsForPoint('org.visallo.user.account.page')
                self.select('listSelector').html(
                    listTemplate({
                        pages: _.chain(pages)
                            .map(function(page) {
                                page.displayName = i18n('useraccount.page.' + page.identifier + '.displayName');
                                return page;
                            })
                            .sortBy('displayName')
                            .value()
                    })
                ).find('a').eq(0).trigger('click');
            });
        });

        this.onChangePage = function(event) {
            var componentPath = $(event.target).closest('li')
                    .siblings('.active').removeClass('active').end()
                    .addClass('active')
                    .data('componentPath'),
                container = this.select('pageSelector').teardownAllComponents();

            require([componentPath], function(Page) {
                Page.attachTo(container);
            });
        };

    }
});
