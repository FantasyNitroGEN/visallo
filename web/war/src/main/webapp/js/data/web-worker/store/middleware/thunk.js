define([], function createThunkMiddleware() {
  return ({ dispatch, getState }) => next => action => {
    if (_.isFunction(action)) {
      return action(dispatch, getState);
    }
    return next(action);
  };
})
