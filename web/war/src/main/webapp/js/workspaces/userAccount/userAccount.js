define([
    'flight/lib/component',
    './pageList.hbs',
    'util/component/attacher'
], function(
    defineComponent,
    listTemplate,
    Attacher) {
    'use strict';

    const ACCESS_EXTENSION_PAGE = {
        identifier: 'access',
        pageComponentPath: 'workspaces/userAccount/bundled/access/access'
    };

    const SETTINGS_EXTENSION_PAGE = {
        identifier: 'settings',
        pageComponentPath: 'workspaces/userAccount/bundled/settings/Settings'
    };

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
                var pages = registry.extensionsForPoint('org.visallo.user.account.page');
                if (!_.findWhere(pages, { identifier: SETTINGS_EXTENSION_PAGE.identifier })) {
                    registry.registerExtension('org.visallo.user.account.page', SETTINGS_EXTENSION_PAGE);
                    pages.push(SETTINGS_EXTENSION_PAGE);
                }
                if (!_.findWhere(pages, { identifier: ACCESS_EXTENSION_PAGE.identifier })) {
                    registry.registerExtension('org.visallo.user.account.page', ACCESS_EXTENSION_PAGE);
                    pages.push(ACCESS_EXTENSION_PAGE);
                }

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

            if (this._attacher) {
                this._attacher.teardown();
            }
            this._attacher = Attacher()
                .node(container)
                .path(componentPath);
            this._attacher.attach();
        };

    }
});
