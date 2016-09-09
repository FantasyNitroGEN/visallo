define(['updeep'], function(u) {
    'use strict';

    return function workspace(state, { type, payload }) {
        if (!state) return { currentId: null, byId: {}, allLoaded: false };

        switch (type) {
            case 'WORKSPACE_SETCURRENT': return u({ currentId: payload.workspaceId }, state);
            case 'WORKSPACE_UPDATE': return update(state, payload.workspace);
            case 'WORKSPACE_DELETE': return deleteWorkspace(state, payload.workspaceId);
            case 'WORKSPACE_SET_ALL': return setAll(state, payload.workspaces);
        }

        return state
    }

    function setAll(state, workspaces) {
        const updates = _.mapObject(_.indexBy(workspaces, 'workspaceId'), w => u.constant(w));
        return u({ byId: updates, allLoaded: true }, state)
    }

    function deleteWorkspace(state, workspaceId) {
        return u({ byId: u.omit([workspaceId]) }, state);
    }

    function update(state, workspace) {
        if (!workspace) throw new Error('Workspace must not be undefined')
        return u({ byId: { [workspace.workspaceId]: u.constant(workspace) }}, state);
    }
})


