define(['/base/jsc/data/web-worker/store/undo/reducer'], function(reducer) {

    describe('undoReducer', () => {

        it('should initialize state', () => {
            reducer(undefined, {}).should.deep.equal({
                global: {
                    undos: [],
                    redos: []
                }
            });
        });

        it('should handle a global UNDO action when there is a saved global action to undo', () => {
            reducer(
                {
                    global: {
                        undos: [{ type: 'TEST' }],
                        redos: []
                    }
                },
                { type: 'UNDO' }
            ).should.deep.equal({
                global: {
                    undos: [],
                    redos: [{ type: 'TEST' }]
                }
            });
        });

        it('should handle a scoped UNDO action when there is a saved scoped action to undo', () => {
            reducer(
                {
                    global: {
                        undos: [],
                        redos: []
                    },
                    1: {
                        undos: [{ type: 'TEST' }],
                        redos: []
                    }
                },
                {
                    type: 'UNDO',
                    payload: {
                        undoScope: 1
                    }
                }
            ).should.deep.equal({
                global: {
                    undos: [],
                    redos: []
                },
                1: {
                    undos: [],
                    redos: [{ type: 'TEST' }]
                }
            });
        });

        it('should not handle a global UNDO action when there is no saved global action to undo', () => {
            reducer(
                {
                    global: {
                        undos: [],
                        redos: []
                    }
                },
                { type: 'UNDO' }
            ).should.deep.equal({
                global: {
                    undos: [],
                    redos: []
                }
            });
        });

        it('should not handle a scoped UNDO action before the scope has been created', () => {
            reducer(
                {
                    global: {
                        undos: [{ type: 'TEST' }],
                        redos: []
                    }
                },
                {
                    type: 'UNDO',
                    payload: {
                        undoScope: 1
                    }
                }
            ).should.deep.equal({
                global: {
                    undos: [{ type: 'TEST' }],
                    redos: []
                }
            });
        });

        it('should not handle a scoped UNDO action when there is no saved action in scope to undo', () => {
            reducer(
                {
                    global: {
                        undos: [{ type: 'TEST' }],
                        redos: []
                    },
                    1: {
                        undos: [],
                        redos: []
                    }
                },
                {
                    type: 'UNDO',
                    payload: {
                        undoScope: 1
                    }
                }
            ).should.deep.equal({
                global: {
                    undos: [{ type: 'TEST' }],
                    redos: []
                },
                1: {
                    undos: [],
                    redos: []
                }
            });
        });

        it('should handle a global REDO action when there is a saved global action to redo', () => {
            reducer(
                {
                    global: {
                        undos: [],
                        redos: [
                            { type: 'TEST_1' },
                            { type: 'TEST_2' }
                        ],
                    }
                },
                { type: 'REDO' }
            ).should.deep.equal({
                global: {
                    undos: [{ type: 'TEST_2' }],
                    redos: [{ type: 'TEST_1' }]
                }
            });
        });

        it('should handle a scoped REDO action when there is a saved scoped action to redo', () => {
            reducer(
                {
                    global: {
                        undos: [],
                        redos: []
                    },
                    1: {
                        undos: [],
                        redos: [{ type: 'TEST' }]
                    }
                },
                {
                    type: 'REDO',
                    payload: {
                        undoScope: 1
                    }
                }
            ).should.deep.equal({
                global: {
                    undos: [],
                    redos: []
                },
                1: {
                    undos: [{ type: 'TEST' }],
                    redos: []
                }
            });
        });

        it('should not handle a global REDO action when there is no saved global action to redo', () => {
            reducer(
                {
                    global: {
                        undos: [],
                        redos: []
                    }
                },
                { type: 'REDO' }
            ).should.deep.equal({
                global: {
                    undos: [],
                    redos: []
                }
            });
        });

        it('should not handle a scoped REDO action before the scope has been created', () => {
            reducer(
                {
                    global: {
                        undos: [],
                        redos: [{ type: 'TEST' }]
                    }
                },
                {
                    type: 'REDO',
                    payload: {
                        undoScope: 1
                    }
                }
            ).should.deep.equal({
                global: {
                    undos: [],
                    redos: [{ type: 'TEST' }]
                }
            });
        });

        it('should not handle a scoped REDO action when there is no saved action in scope to redo', () => {
            reducer(
                {
                    global: {
                        undos: [],
                        redos: [{ type: 'TEST' }]
                    },
                    1: {
                        undos: [],
                        redos: []
                    }
                },
                {
                    type: 'REDO',
                    payload: {
                        undoScope: 1
                    }
                }
            ).should.deep.equal({
                global: {
                    undos: [],
                    redos: [{ type: 'TEST' }]
                },
                1: {
                    undos: [],
                    redos: []
                }
            });
        });

        it('should handle global undoable actions when the undo stack is empty', () => {
            reducer(
                {
                    global: {
                        undos: [],
                        redos: []
                    }
                },
                {
                    type: 'TEST',
                    payload: {
                        undo: 'UNDO_PAYLOAD',
                        redo: 'REDO_PAYLOAD'
                    }
                }
            ).should.deep.equal({
                global: {
                    undos: [{ type: 'TEST', undo: 'UNDO_PAYLOAD', redo: 'REDO_PAYLOAD' }],
                    redos: []
                }
            });
        });

        it('should handle scoped undoable actions when the undo stack is empty', () => {
            reducer(
                {
                    global: {
                        undos: [],
                        redos: []
                    }
                },
                {
                    type: 'TEST',
                    payload: {
                        undoScope: 1,
                        undo: 'UNDO_PAYLOAD',
                        redo: 'REDO_PAYLOAD'
                    }
                }
            ).should.deep.equal({
                global: {
                    undos: [],
                    redos: []
                },
                1: {
                    undos: [{ type: 'TEST', undo: 'UNDO_PAYLOAD', redo: 'REDO_PAYLOAD' }],
                    redos: []
                }
            });
        });

        it('should handle global undoable actions when the undo stack is not empty', () => {
            reducer(
                {
                    global: {
                        undos: [{ type: 'TEST', undo: 'UNDO_PAYLOAD_1', redo: 'REDO_PAYLOAD_1' }],
                        redos: []
                    }
                },
                {
                    type: 'TEST',
                    payload: {
                        undo: 'UNDO_PAYLOAD_2',
                        redo: 'REDO_PAYLOAD_2'
                    }
                }
            ).should.deep.equal({
                global: {
                    undos: [
                        { type: 'TEST', undo: 'UNDO_PAYLOAD_1', redo: 'REDO_PAYLOAD_1' },
                        { type: 'TEST', undo: 'UNDO_PAYLOAD_2', redo: 'REDO_PAYLOAD_2' }
                    ],
                    redos: []
                }
            });
        });

        it('should handle scoped undoable actions when the undo stack is not empty', () => {
            reducer(
                {
                    global: {
                        undos: [{ type: 'TEST', undo: 'GLOBAL_UNDO_PAYLOAD', redo: 'GLOBAL_REDO_PAYLOAD' }],
                        redos: []
                    },
                    1: {
                        undos: [{ type: 'TEST', undo: 'UNDO_PAYLOAD_1', redo: 'REDO_PAYLOAD_1' }],
                        redos: []
                    }
                },
                {
                    type: 'TEST',
                    payload: {
                        undoScope: 1,
                        undo: 'UNDO_PAYLOAD_2',
                        redo: 'REDO_PAYLOAD_2'
                    }
                }
            ).should.deep.equal({
                global: {
                    undos: [{ type: 'TEST', undo: 'GLOBAL_UNDO_PAYLOAD', redo: 'GLOBAL_REDO_PAYLOAD' }],
                    redos: []
                },
                1: {
                    undos: [
                        { type: 'TEST', undo: 'UNDO_PAYLOAD_1', redo: 'REDO_PAYLOAD_1' },
                        { type: 'TEST', undo: 'UNDO_PAYLOAD_2', redo: 'REDO_PAYLOAD_2' }
                    ],
                    redos: []
                }
            });
        });

        it('should handle undos as a stack', () => {
            reducer(
                {
                    global: {
                        undos: [],
                        redos: []
                    },
                    1: {
                        undos: [{ type: 'TEST', undo: 'UNDO_PAYLOAD_1', redo: 'REDO_PAYLOAD_1' }],
                        redos: [{ type: 'TEST', undo: 'UNDO_PAYLOAD_2', redo: 'REDO_PAYLOAD_2' }]
                    }
                },
                {
                    type: 'UNDO',
                    payload: {
                        undoScope: 1
                    }
                }
            ).should.deep.equal({
                global: {
                    undos: [],
                    redos: []
                },
                1: {
                    undos: [],
                    redos: [
                        { type: 'TEST', undo: 'UNDO_PAYLOAD_2', redo: 'REDO_PAYLOAD_2' },
                        { type: 'TEST', undo: 'UNDO_PAYLOAD_1', redo: 'REDO_PAYLOAD_1' }
                    ]
                }
            });
        });

        it('should handle redos as a stack', () => {
            reducer(
                {
                    global: {
                        undos: [],
                        redos: []
                    },
                    1: {
                        undos: [{ type: 'TEST', undo: 'UNDO_PAYLOAD_1', redo: 'REDO_PAYLOAD_1' }],
                        redos: [{ type: 'TEST', undo: 'UNDO_PAYLOAD_2', redo: 'REDO_PAYLOAD_2' }]
                    }
                },
                {
                    type: 'REDO',
                    payload: {
                        undoScope: 1
                    }
                }
            ).should.deep.equal({
                global: {
                    undos: [],
                    redos: []
                },
                1: {
                    undos: [
                        { type: 'TEST', undo: 'UNDO_PAYLOAD_1', redo: 'REDO_PAYLOAD_1' },
                        { type: 'TEST', undo: 'UNDO_PAYLOAD_2', redo: 'REDO_PAYLOAD_2' }
                    ],
                    redos: []
                }
            });
        });

        it('should reset the global redo stack when a new global UNDO occurs', () => {
            reducer(
                {
                    global: {
                        undos: [],
                        redos: [{ type: 'TEST', undo: 'UNDO_PAYLOAD_1', redo: 'REDO_PAYLOAD_1' }],
                    }
                },
                {
                    type: 'TEST',
                    payload: {
                        undo: 'UNDO_PAYLOAD_2',
                        redo: 'REDO_PAYLOAD_2'
                    }
                }
            ).should.deep.equal({
                global: {
                    undos: [
                        { type: 'TEST', undo: 'UNDO_PAYLOAD_2', redo: 'REDO_PAYLOAD_2' }
                    ],
                    redos: []
                }
            });
        });

        it('should reset the scoped redo stack when a new UNDO occurs in the same scope', () => {
            reducer(
                {
                    global: {
                        undos: [],
                        redos: [{ type: 'TEST', undo: 'GLOBAL_UNDO_PAYLOAD', redo: 'GLOBAL_REDO_PAYLOAD' }],
                    },
                    1: {
                        undos: [],
                        redos: [{ type: 'TEST', undo: 'UNDO_PAYLOAD_1', redo: 'REDO_PAYLOAD_1' }],
                    }
                },
                {
                    type: 'TEST',
                    payload: {
                        undoScope: 1,
                        undo: 'UNDO_PAYLOAD_2',
                        redo: 'REDO_PAYLOAD_2'
                    }
                }
            ).should.deep.equal({
                global: {
                    undos: [],
                    redos: [{ type: 'TEST', undo: 'GLOBAL_UNDO_PAYLOAD', redo: 'GLOBAL_REDO_PAYLOAD' }],
                },
                1: {
                    undos: [
                        { type: 'TEST', undo: 'UNDO_PAYLOAD_2', redo: 'REDO_PAYLOAD_2' }
                    ],
                    redos: []
                }
            });
        });
    });
});
