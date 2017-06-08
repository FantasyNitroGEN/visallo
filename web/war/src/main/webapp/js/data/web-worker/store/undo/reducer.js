define([], function() {
    'use strict';

    const initialState = {
        global: {
            undos: [],
            redos: []
        }
    };

    return function undoActionHistory(state = initialState, { type, payload }) {
        switch (type) {
            case 'UNDO': {
                const scope = payload && payload.undoScope || 'global';
                if (!state[scope]) {
                    return state;
                }
                const { undos, redos } = state[scope];
                if (!undos.length) {
                    return state;
                }
                const currentUndo = undos[undos.length - 1];

                return {
                    ...state,
                    [scope]: {
                        undos: undos.slice(0, undos.length - 1),
                        redos: [ ...redos, currentUndo ]
                    }
                };
            }
            case 'REDO': {
                const scope = payload && payload.undoScope || 'global';
                if (!state[scope]) {
                    return state;
                }
                const { undos, redos } = state[scope];
                if (!redos.length) {
                    return state;
                }
                const currentRedo = redos[redos.length - 1];

                return {
                    ...state,
                    [scope]: {
                        undos: [ ...undos, currentRedo ],
                        redos: redos.slice(0, redos.length - 1)
                    }
                };
            }
            case 'PUSH_UNDO':
            default: {
                if (!payload) {
                    return state;
                }

                const { undoScope: scope = 'global', undo, redo, undoActionType } = payload;
                if (!undo || !redo) {
                    return state;
                }

                const newUndo = {
                    undo,
                    redo,
                    type: undoActionType || type
                };

                const scopedState = state[scope] || { undos: [], redos: [] };
                const { undos, redos } = scopedState;

                return {
                    ...state,
                    [scope]: {
                        undos: [ ...undos, newUndo ],
                        redos: []
                    }
                };
            }
        }
    }
});
