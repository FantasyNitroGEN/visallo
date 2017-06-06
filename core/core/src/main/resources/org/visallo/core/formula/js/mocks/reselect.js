define([], function() {
    return {
        createSelector: function(deps, callback) {
            if (!_.isArray(deps)) {
                deps = _.toArray(arguments);
                callback = deps.unshift();
            }
            return function(state) {
                return callback.apply(null, deps.map(function(dep) {
                    return dep(state);
                }))
            }
        }
    }
});