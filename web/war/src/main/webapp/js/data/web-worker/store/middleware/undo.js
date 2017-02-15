define([
    'configuration/plugins/registry',
], function createUndoMiddleware(registry) {
    registry.markUndocumentedExtensionPoint('org.visallo.store');

    const undoExtensions = registry.extensionsForPoint('org.visallo.store')
        .filter(e => e.undoActions);
    const undoActionsByType = undoExtensions
        .map(e => e.undoActions)
        .reduce((actions, a) => (
            {
                ...actions,
                ...a
            }
        ), {});

    return ({ dispatch, getState }) => next => action => {
        if (action.type === 'UNDO') {
            const { payload } = action;
            const scope = payload && payload.undoScope || 'global';
            const history = getState().undoActionHistory[scope];
            if (history) {
                const { undos } = history;
                const { type, undo } = undos[undos.length - 1] || {};
                if (undo && undoActionsByType[type]) {
                    dispatch(undoActionsByType[type].undo(undo));
                }
            }
        }
        if (action.type === 'REDO') {
            const { payload } = action;
            const scope = payload && payload.undoScope || 'global';
            const history = getState().undoActionHistory[scope];
            if (history) {
                const { redos } = history;
                const { type, redo } = redos[redos.length - 1] || {};
                if (redo && undoActionsByType[type]) {
                    dispatch(undoActionsByType[type].redo(redo));
                }
            }
        }
        return next(action);
    };
});
