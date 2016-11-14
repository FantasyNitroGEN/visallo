define(['../actions', '../../util/ajax'], function(actions, ajax) {
    actions.protectFromMain();

    const api = {
        setPadding: ({ top, left, right, bottom }) => ({
            type: 'PANEL_SET_PADDING',
            payload: { top, left, right, bottom }
        })
    };

    return api;
});
