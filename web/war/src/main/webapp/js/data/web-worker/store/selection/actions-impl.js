define(['../actions'], function(actions) {
    actions.protectFromMain();

    return {
        add: ({ selection }) => ({
            type: 'SELECTION_ADD',
            payload: { selection }
        }),

        remove: ({ selection }) => ({
            type: 'SELECTION_REMOVE',
            payload: { selection }
        }),

        clear: () => ({ type: 'SELECTION_CLEAR' }),

        set: ({ selection }) => ({
            type: 'SELECTION_SET',
            payload: { selection }
        })
    }
})
