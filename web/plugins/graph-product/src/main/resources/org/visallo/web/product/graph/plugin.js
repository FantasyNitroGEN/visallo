require(['configuration/plugins/registry', 'data/web-worker/store/actions'], function(registry, actions) {
    registry.registerExtension('org.visallo.workproduct', {
        identifier: 'org.visallo.web.product.graph.GraphWorkProduct',
        componentPath: 'org/visallo/web/product/graph/dist/Graph',
        storeActions: actions.createActions({
            workerImpl: 'org/visallo/web/product/graph/dist/actions-impl',
            actions: {
                removeElements: function(productId, elements, undoable) { return { productId, elements, undoable }},
                dropElements: function(productId, elements) { return { productId, elements }}
            }
        })
    })

    $(document).on('applicationReady currentUserVisalloDataUpdated', function() {
        $(document).trigger('registerKeyboardShortcuts', {
            scope: ['graph.help.scope'].map(i18n),
            shortcuts: {
                'meta-a': { fire: 'selectAll', desc: i18n('visallo.help.select_all') },
                'meta-e': { fire: 'selectConnected', desc: i18n('visallo.help.select_connected') },
                'delete': { fire: 'deleteSelected', desc: i18n('visallo.help.delete') },
                'alt-r': { fire: 'addRelatedItems', desc: i18n('visallo.help.add_related') },
                'alt-t': { fire: 'searchTitle', desc: i18n('visallo.help.search_title') },
                'alt-s': { fire: 'searchRelated', desc: i18n('visallo.help.search_related') },
                'undo': { fire: 'undo', desc: i18n('visallo.help.undo') },
                'redo': { fire: 'redo', desc: i18n('visallo.help.redo') }
            }
        });
    });
});
