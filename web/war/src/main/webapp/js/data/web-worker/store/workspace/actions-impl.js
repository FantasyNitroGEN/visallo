define(['../actions', '../../util/ajax'], function(actions, ajax) {
    actions.protectFromMain();

    const api = {

        setCurrent: ({ workspaceId }) => (dispatch, getState) => {

            dispatch({ type: 'WORKSPACE_SETCURRENT', payload: { workspaceId } })
            dispatch(api.get({ workspaceId }))
            pushSocketMessage({
                type: 'setActiveWorkspace',
                data: { workspaceId: workspaceId }
            });
        },

        setAll: ({ workspaces }) => ({
            type: 'WORKSPACE_SET_ALL',
            payload: { workspaces }
        }),

        deleteWorkspace: ({ workspaceId, createIfEmpty = false }) => (dispatch, getState) => {
            const workspaces = getState().workspace;
            const workspace = workspaces.byId[workspaceId];

            if (workspace) {
                dispatch({
                    type: 'WORKSPACE_DELETE',
                    payload: { workspaceId }
                })

                const workspaces = getState().workspace;
                if (workspaces.currentId === workspaceId) {
                    const nextValidWorkspace = _.sortBy(_.filter(Object.values(workspaces.byId), function(w) {
                        return !w.sharedToUser && ('createdBy' in w);
                    }), w => w.title.toLowerCase())[0];
                    if (nextValidWorkspace) {
                        dispatch(api.setCurrent({ workspaceId: nextValidWorkspace.workspaceId }))
                    } else if (createIfEmpty) {
                        ajax('POST', '/workspace/create').then(workspace => {
                            dispatch(api.update({ workspace }))
                            dispatch(api.setCurrent({ workspaceId: workspace.workspaceId }))
                        })
                    }
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
            }
        }
    }

    return api;
})

