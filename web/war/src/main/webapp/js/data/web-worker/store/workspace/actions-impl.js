define(['../actions', '../../util/ajax'], function(actions, ajax) {
    actions.protectFromMain();

    const sort = workspaces => _.sortBy(
        _.filter(workspaces, function(w) {
            return !w.sharedToUser && ('createdBy' in w);
        }),
        w => w.title.toLowerCase()
    );

    const api = {

        setCurrent: ({ workspaceId }) => (dispatch, getState) => {
            if (!workspaceId) {
                var workspaces = getState().workspace;
                Promise.try(() => {
                    if (workspaces.allLoaded) {
                        return Promise.resolve(sort(Object.values(workspaces.byId)));
                    } else {
                        return ajax('GET', '/workspace/all')
                            .then(function(result) {
                                dispatch(api.setAll({ workspaces: result.workspaces }));
                                return sort(result.workspaces);
                            })
                    }
                }).then(list => {
                    if (list.length) {
                        return list[0].workspaceId
                    }
                    return ajax('POST', '/workspace/create').then(workspace => {
                        dispatch(api.update({ workspace }))
                        return workspace.workspaceId;
                    })
                }).then(workspaceId => {
                    dispatch({ type: 'WORKSPACE_SETCURRENT', payload: { workspaceId } })
                    pushSocketMessage({
                        type: 'setActiveWorkspace',
                        data: { workspaceId: workspaceId }
                    });
                })
            } else {
                dispatch({ type: 'WORKSPACE_SETCURRENT', payload: { workspaceId } })
                dispatch(api.get({ workspaceId }))
                pushSocketMessage({
                    type: 'setActiveWorkspace',
                    data: { workspaceId: workspaceId }
                });
            }
        },

        setAll: ({ workspaces }) => ({
            type: 'WORKSPACE_SET_ALL',
            payload: { workspaces }
        }),

        deleteWorkspace: ({ workspaceId }) => (dispatch, getState) => {
            const workspaces = getState().workspace;
            const workspace = workspaces.byId[workspaceId];

            if (workspace) {
                dispatch({
                    type: 'WORKSPACE_DELETE',
                    payload: { workspaceId }
                })

                const workspaces = getState().workspace;
                if (workspaces.currentId === workspaceId) {
                    dispatch(api.setCurrent({ workspaceId: undefined }))
                }
            }
        },

        get: ({ workspaceId, invalidate }) => (dispatch, getState) => {
            var workspace = getState().workspace.byId[workspaceId];
            if (!workspace || invalidate) {
                ajax('GET', '/workspace', { workspaceId })
                    .then(workspace => dispatch(api.update({ workspace })))
            }
        },

        update: ({ workspace }) => (dispatch, getState) => {
            const state = getState();
            const { currentId, byId } = state.workspace;
            dispatch({
                type: 'WORKSPACE_UPDATE',
                payload: { workspace }
            })
            if (!currentId || !byId[currentId]) {
                dispatch(api.setCurrent({ workspaceId: workspace.workspaceId }))
            } else {
                require([
                    'data/web-worker/store/product/actions-impl',
                    'data/web-worker/store/product/selectors'
                ], (productActions, productSelectors) => {
                    const selectedProduct = productSelectors.getProduct(state);
                    if (selectedProduct && selectedProduct.extendedData) {
                        console.log('workspaceChange triggered product invalidate'); //TODO: remove debugging
                            dispatch(productActions.get({ productId: selectedProduct.id, invalidate: true }));
                    }
                })
            }
        }
    }

    return api;
})

