require(['configuration/plugins/registry'], function(registry) {
    registry.registerExtension('org.visallo.workproduct', {
        identifier: 'org.visallo.web.product.map.MapWorkProduct',
        componentPath: 'org/visallo/web/product/map/dist/Map',
        handleDrop: function(elements, product) {
            visalloData.storePromise.then(function(store) {
                store.dispatch({
                    type: 'ROUTE_TO_WORKER_ACTION',
                    payload: { productId: product.id, elements },
                    meta: {
                        workerImpl: 'org/visallo/web/product/map/dist/actions-impl',
                        name: 'dropElements'
                    }
                })
            })
        }
    })
    $(document).on('applicationReady currentUserVisalloDataUpdated', function() {
        $(document).trigger('registerKeyboardShortcuts', {
            scope: ['map.help.scope'].map(i18n),
            shortcuts: {
                'meta-a': { fire: 'selectAll', desc: i18n('visallo.help.select_all') },
                'delete': { fire: 'deleteSelected', desc: i18n('visallo.help.delete') },
                'alt-t': { fire: 'searchTitle', desc: i18n('visallo.help.search_title') },
                'alt-s': { fire: 'searchRelated', desc: i18n('visallo.help.search_related') }
            }
        });
    });
});
