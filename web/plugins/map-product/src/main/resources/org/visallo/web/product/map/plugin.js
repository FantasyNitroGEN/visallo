require(['configuration/plugins/registry', 'data/web-worker/store/actions'], function(registry, actions) {
    registry.registerExtension('org.visallo.workproduct', {
        identifier: 'org.visallo.web.product.map.MapWorkProduct',
        componentPath: 'org/visallo/web/product/map/dist/Map',
        storeActions: actions.createActions({
            workerImpl: 'org/visallo/web/product/map/dist/actions-impl',
            actions: {
                removeElements: function(productId, elements, undoable) { return { productId, elements, undoable }},
                dropElements: function(productId, elements, undoable) { return { productId, elements }}
            }
        })
    })

    $(document).on('applicationReady currentUserVisalloDataUpdated', function() {
        $(document).trigger('registerKeyboardShortcuts', {
            scope: ['map.help.scope'].map(i18n),
            shortcuts: {
                'meta-a': { fire: 'selectAll', desc: i18n('visallo.help.select_all') },
                'delete': { fire: 'deleteSelected', desc: i18n('visallo.help.delete') },
                'alt-t': { fire: 'searchTitle', desc: i18n('visallo.help.search_title') },
                'alt-s': { fire: 'searchRelated', desc: i18n('visallo.help.search_related') },
                'undo': { fire: 'undo', desc: i18n('visallo.help.undo') },
                'redo': { fire: 'redo', desc: i18n('visallo.help.redo') }
            }
        });
    });
});
