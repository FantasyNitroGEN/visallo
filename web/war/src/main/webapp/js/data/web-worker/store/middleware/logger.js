define([], function() {
    return ({ getState }) => (next) => (action) => {
        var { type, payload } = action;
        console.info(`STORE_ACTION: ${type}`, payload)
        return next(action);
    }
})
