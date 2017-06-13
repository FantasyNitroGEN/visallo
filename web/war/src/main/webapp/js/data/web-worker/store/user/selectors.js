define(['reselect'], function(reselect) {
    const { createSelector } = reselect;

    const getAuthorizations = (state) => state.user.current.authorizations;
    const getCurrent = (state) => state.user.current;
    const getDisplayName = (state) => state.user.current.displayName;
    const getLongRunningProcesses = (state) => state.user.current.longRunningProcesses;
    const getPreferences = (state) => state.user.current.uiPreferences;
    const getPrivileges = (state) => state.user.current.privileges;
    const getProperties = (state) => state.user.current.properties;
    const getUserName = (state) => state.user.current.userName;
    const getWorkspaceId = (state) => state.user.current.currentWorkspaceId;

    return {
        getAuthorizations: createSelector([getAuthorizations], authorizations => {
            return _.object(authorizations.map(a => [a, true]));
        }),
        getAuthorizationsList: createSelector([getAuthorizations], authorizations => {
            return authorizations.sort((a, b) => a.toLowerCase().localeCompare(b.toLowerCase()));
        }),
        getCurrent,
        getDisplayName,
        getLongRunningProcesses,
        getPreferences,
        getPrivileges: createSelector([getPrivileges], privileges => {
            return _.object(privileges.map(p => [p, true]))
        }),
        getPrivilegesList: createSelector([getPrivileges], privileges => {
            return privileges.sort((a, b) => a.toLowerCase().localeCompare(b.toLowerCase()));
        }),
        getProperties,
        getUserName,
        getWorkspaceId
    }
});
