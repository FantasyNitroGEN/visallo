define([
    '../actions'
], function(actions) {
    actions.protectFromMain();

    const api = {
        undoForProduct: () => (dispatch, getState) => {
            const state = getState();
            const currentWorkspace = state.workspace.currentId;
            const currentProduct = state.product.workspaces[currentWorkspace].selected;

            return dispatch({
                type: 'UNDO',
                payload: {
                    undoScope: currentProduct
                }
            });
        },

        redoForProduct: () => (dispatch, getState) => {
            const state = getState();
            const currentWorkspace = state.workspace.currentId;
            const currentProduct = state.product.workspaces[currentWorkspace].selected;

            return dispatch({
                type: 'REDO',
                payload: {
                    undoScope: currentProduct
                }
            });
        },

    };

    return api;
});
