define([
    'configuration/plugins/registry',
    'util/promise',
], function(registry) {

    const createFakeStore = (fakeData, dispatch) => ({
        dispatch,
        getState() {
            return fakeData;
        }
    });

    const dispatchWithStoreAndExtension = ({ store, extensionConfig, action }) => {
        const moduleId = '/base/jsc/data/web-worker/store/middleware/undo';
        requirejs.undef(moduleId);
        if (extensionConfig) {
            registry.registerExtension('org.visallo.store', extensionConfig);
        }
        return Promise.require(moduleId)
            .then(undo => {
                let dispatched;
                const dispatch = undo(store)(actionAttempt => {
                    dispatched = actionAttempt;
                });
                dispatch(action);
                return dispatched;
            });
    };

    describe('undo middleware', () => {
        it('should always call next to dispatch the original action', () => {
            const action = { type: 'NOT_UNDO' };
            const dispatch = sinon.spy();
            const store = createFakeStore(
                {
                    undoActionHistory: {
                        global: {
                            undos: [],
                            redos: []
                        }
                    }
                },
                dispatch
            );

            return dispatchWithStoreAndExtension({
                store,
                action
            }).then(result => {
                expect(result).to.equal(action);
                dispatch.should.not.have.been.called;
            });
        });

        it('should not dispatch undo when there is nothing to undo', () => {
            const action = { type: 'UNDO' };
            const dispatch = sinon.spy();
            const store = createFakeStore(
                {
                    undoActionHistory: {
                        global: {
                            undos: [],
                            redos: []
                        }
                    }
                },
                dispatch
            );

            return dispatchWithStoreAndExtension({
                store,
                action
            }).then(result => {
                expect(result).to.equal(action);
                dispatch.should.not.have.been.called;
            });
        });

        it('should not dispatch a scoped undo when the scope has no history', () => {
            const action = {
                type: 'UNDO',
                payload: { undoScope: 1 }
            };
            const dispatch = sinon.spy();
            const store = createFakeStore(
                {
                    undoActionHistory: {
                        global: {
                            undos: [ { type: 'TEST' }],
                            redos: []
                        }
                    }
                },
                dispatch
            );

            return dispatchWithStoreAndExtension({
                store,
                action
            }).then(result => {
                expect(result).to.equal(action);
                dispatch.should.not.have.been.called;
            });
        });

        it('should not dispatch a scoped undo when there is nothing to undo', () => {
            const action = { type: 'UNDO', scope: 1 };
            const dispatch = sinon.spy();
            const store = createFakeStore(
                {
                    undoActionHistory: {
                        global: {
                            undos: [{ type: 'TEST' }],
                            redos: []
                        },
                        1: {
                            undos: [],
                            redos: []
                        }
                    }
                },
                dispatch
            );

            return dispatchWithStoreAndExtension({
                store,
                action
            }).then(result => {
                expect(result).to.equal(action);
                dispatch.should.not.have.been.called;
            });
        });

        it('should not dispatch undo when there is not a plugin for the type', () => {
            const action = { type: 'UNDO' };
            const actionToUndo = {
                type: 'NO_PLUGIN',
                undo: {}
            };
            const dispatch = sinon.spy();
            const store = createFakeStore(
                {
                    undoActionHistory: {
                        global: {
                            undos: [ actionToUndo ],
                            redos: []
                        }
                    }
                },
                dispatch
            );
            const extensionConfig = {
                key: 'test',
                undoActions: {
                        TEST: {
                            undo: () => ({ type: 'UNDO_TEST' })
                        }
                }
            };

            return dispatchWithStoreAndExtension({
                store,
                extensionConfig,
                action
            }).then(result => {
                expect(result).to.equal(action);
                dispatch.should.not.have.been.called;
            });
        });

        it('should not dispatch undo when there is not a plugin scoped for the type', () => {
            const action = { type: 'UNDO', scope: 1 };
            const dispatch = sinon.spy();
            const store = createFakeStore(
                {
                    undoActionHistory: {
                        global: {
                            undos: [ { type: 'TEST' }],
                            redos: []
                        },
                        1: {
                            undos: [ { type: 'NO_PLUGIN' } ],
                            redos: []
                        }
                    }
                },
                dispatch
            );
            const extensionConfig = {
                key: 'test',
                undoActions: {
                    TEST: {
                        undo: () => ({ type: 'UNDO_TEST' })
                    }
                }
            };

            return dispatchWithStoreAndExtension({
                store,
                extensionConfig,
                action
            }).then(result => {
                expect(result).to.equal(action);
                dispatch.should.not.have.been.called;
            });
        });

        it('should dispatch the last undo action when there is at least one action', () => {
            const action = { type: 'UNDO' };
            const actionToUndo = {
                type: 'TEST',
                undo: {}
            };
            const dispatch = sinon.spy();
            const store = createFakeStore(
                {
                    undoActionHistory: {
                        global: {
                            undos: [ actionToUndo ],
                            redos: []
                        }
                    }
                },
                dispatch
            );
            const extensionConfig = {
                key: 'test',
                undoActions: {
                    TEST: {
                        undo: () => ({ type: 'UNDO_TEST' })
                    }
                }
            };

            return dispatchWithStoreAndExtension({
                store,
                extensionConfig,
                action
            }).then(result => {
                expect(result).to.equal(action);
                dispatch.should.have.been.calledWith({ type: 'UNDO_TEST' });
            });
        });

        it('should dispatch the last scoped undo action when there is at least one scoped action', () => {
            const action = {
                type: 'UNDO',
                payload: { undoScope: 1 }
            };
            const actionToUndo = {
                type: 'TEST',
                undo: {}
            };
            const dispatch = sinon.spy();
            const store = createFakeStore(
                {
                    undoActionHistory: {
                        global: {
                            undos: [],
                            redos: []
                        },
                        1: {
                            undos: [ actionToUndo ],
                            redos: []
                        }
                    }
                },
                dispatch
            );
            const extensionConfig = {
                key: 'test',
                undoActions: {
                    TEST: {
                        undo: () => ({ type: 'UNDO_TEST' })
                    }
                }
            };

            return dispatchWithStoreAndExtension({
                store,
                extensionConfig,
                action
            }).then(result => {
                expect(result).to.equal(action);
                dispatch.should.have.been.calledWith({ type: 'UNDO_TEST' });
            });
        });


        it("should dispatch an undo action with the saved action's undo payload", () => {
            const action = { type: 'UNDO' };
            const actionToUndo = {
                type: 'TEST',
                undo: {
                    data: 'TEST_PAYLOAD'
                }
            };
            const dispatch = sinon.spy();
            const store = createFakeStore(
                {
                    undoActionHistory: {
                        global: {
                            undos: [ actionToUndo ],
                            redos: []
                        }
                    }
                },
                dispatch
            );
            const extensionConfig = {
                key: 'test',
                undoActions: {
                    TEST: {
                        undo: undo => ({
                            type: 'UNDO_TEST',
                            ...undo
                        })
                    }
                }
            };

            return dispatchWithStoreAndExtension({
                store,
                extensionConfig,
                action
            }).then(result => {
                expect(result).to.equal(action);
                dispatch.should.have.been.calledWith({
                    type: 'UNDO_TEST',
                    data: 'TEST_PAYLOAD'
                });
            });
        });

        it('should not dispatch redo when there is nothing to redo', () => {
            const action = { type: 'REDO' };
            const dispatch = sinon.spy();
            const store = createFakeStore(
                {
                    undoActionHistory: {
                        global: {
                            undos: [],
                            redos: []
                        }
                    }
                },
                dispatch
            );

            return dispatchWithStoreAndExtension({
                store,
                action
            }).then(result => {
                expect(result).to.equal(action);
                dispatch.should.not.have.been.called;
            });
        });

        it('should not dispatch a scoped redo when the scope has no history', () => {
            const action = {
                type: 'REDO',
                payload: { undoScope: 1 }
            };
            const dispatch = sinon.spy();
            const store = createFakeStore(
                {
                    undoActionHistory: {
                        global: {
                            undos: [],
                            redos: [{ type: 'TEST'  }]
                        }
                    }
                },
                dispatch
            );

            return dispatchWithStoreAndExtension({
                store,
                action
            }).then(result => {
                expect(result).to.equal(action);
                dispatch.should.not.have.been.called;
            });
        });

        it('should not dispatch a scoped redo when there is nothing to redo', () => {
            const action = {
                type: 'REDO',
                payload: { undoScope: 1 }
            };
            const dispatch = sinon.spy();
            const store = createFakeStore(
                {
                    undoActionHistory: {
                        global: {
                            undos: [],
                            redos: [{ type: 'TEST'  }]
                        },
                        1: {
                            undos: [],
                            redos: []
                        }
                    }
                },
                dispatch
            );

            return dispatchWithStoreAndExtension({
                store,
                action
            }).then(result => {
                expect(result).to.equal(action);
                dispatch.should.not.have.been.called;
            });
        });

        it('should not dispatch redo when there is not a plugin for the type', () => {
            const action = { type: 'REDO' };
            const actionToRedo = {
                type: 'NO_PLUGIN',
                redo: {}
            };
            const dispatch = sinon.spy();
            const store = createFakeStore(
                {
                    undoActionHistory: {
                        global: {
                            undos: [],
                            redos: [ actionToRedo ]
                        }
                    }
                },
                dispatch
            );
            const extensionConfig = {
                key: 'test',
                undoActions: {
                    TEST: {
                        redo: () => ({ type: 'REDO_TEST' })
                    }
                }
            };

            return dispatchWithStoreAndExtension({
                store,
                extensionConfig,
                action
            }).then(result => {
                expect(result).to.equal(action);
                dispatch.should.not.have.been.called;
            });
        });

        it('should not dispatch redo when there is not a plugin scoped for the type', () => {
            const action = {
                type: 'REDO',
                scope: 1
            };
            const actionToRedo = {
                type: 'NO_PLUGIN',
                redo: {}
            };
            const dispatch = sinon.spy();
            const store = createFakeStore(
                {
                    undoActionHistory: {
                        global: {
                            undos: [],
                            redos: [ { type: 'TEST' }]
                        },
                        1: {
                            undos: [],
                            redos: [ { type: 'NO_PLUGIN' } ]
                        }
                    }
                },
                dispatch
            );
            const extensionConfig = {
                key: 'test',
                undoActions: {
                    TEST: {
                        redo: () => ({ type: 'REDO_TEST' })
                    }
                }
            };

            return dispatchWithStoreAndExtension({
                store,
                extensionConfig,
                action
            }).then(result => {
                expect(result).to.equal(action);
                dispatch.should.not.have.been.called;
            });
        });

        it('should dispatch the last redo action when there is at least one action', () => {
            const action = { type: 'REDO' };
            const actionToRedo = {
                type: 'TEST',
                redo: {}
            };
            const dispatch = sinon.spy();
            const store = createFakeStore(
                {
                    undoActionHistory: {
                        global: {
                            undos: [],
                            redos: [ actionToRedo ]
                        }
                    }
                },
                dispatch
            );
            const extensionConfig = {
                key: 'test',
                undoActions: {
                    TEST: {
                        redo: () => ({ type: 'REDO_TEST' })
                    }
                }
            };

            return dispatchWithStoreAndExtension({
                store,
                extensionConfig,
                action
            }).then(result => {
                expect(result).to.equal(action);
                dispatch.should.have.been.calledWith({ type: 'REDO_TEST' });
            });
        });

        it('should dispatch the last scoped redo action when there is at least one action', () => {
            const action = {
                type: 'REDO',
                payload: { undoScope: 1 }
            };
            const actionToRedo = {
                type: 'TEST',
                redo: {}
            };
            const dispatch = sinon.spy();
            const store = createFakeStore(
                {
                    undoActionHistory: {
                        global: {
                            undos: [],
                            redos: []
                        },
                        1: {
                            undos: [],
                            redos: [ actionToRedo ]
                        }
                    }
                },
                dispatch
            );
            const extensionConfig = {
                key: 'test',
                undoActions: {
                    TEST: {
                        redo: () => ({ type: 'REDO_TEST' })
                    }
                }
            };

            return dispatchWithStoreAndExtension({
                store,
                extensionConfig,
                action
            }).then(result => {
                expect(result).to.equal(action);
                dispatch.should.have.been.calledWith({ type: 'REDO_TEST' });
            });
        });

        it("should dispatch a redo action with the saved action's redo payload", () => {
            const action = { type: 'REDO' };
            const actionToRedo = {
                type: 'TEST',
                redo: {
                    data: 'TEST_PAYLOAD'
                }
            };
            const dispatch = sinon.spy();
            const store = createFakeStore(
                {
                    undoActionHistory: {
                        global: {
                            undos: [],
                            redos: [ actionToRedo ]
                        }
                    }
                },
                dispatch
            );
            const extensionConfig = {
                key: 'test',
                undoActions: {
                    TEST: {
                        redo: redo => ({
                            type: 'REDO_TEST',
                            ...redo
                        })
                    }
                }
            };

            return dispatchWithStoreAndExtension({
                store,
                extensionConfig,
                action
            }).then(result => {
                expect(result).to.equal(action);
                dispatch.should.have.been.calledWith({
                    type: 'REDO_TEST',
                    data: 'TEST_PAYLOAD'
                });
            });
        });
    });
});
