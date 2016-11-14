define(['reselect'], function({ createSelector }) {

    const getWorkspaceId = (state) => state.workspace.currentId;
    const getElements = (state) => state.element

    return {
        getElements: createSelector([getWorkspaceId, getElements], (workspaceId, elements) => {
            return (workspaceId && workspaceId in elements) ? elements[workspaceId] : {};
        })
    }
})
