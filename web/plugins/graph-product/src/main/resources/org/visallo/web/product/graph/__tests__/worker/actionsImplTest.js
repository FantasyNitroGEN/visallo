import configureMockStore from 'redux-mock-store';
import thunk from 'redux-thunk';
import actions from '../../worker/actions-impl';

jest.mock('data/web-worker/store/actions', () => ({
    protectFromMain: jest.fn()
}));
jest.mock('data/web-worker/util/ajax', () => () =>
    Promise.resolve({
        extendedData: {
            edges: []
        }
    }));
jest.mock('data/web-worker/store/product/actions-impl', () => ({
    update: jest.fn(() => ({ type: 'MOCK_PRODUCT_UPDATE' }))
}));
jest.mock('data/web-worker/store/element/actions-impl', () => ({
    get: jest.fn(() => ({ type: 'MOCK_ELEMENT_GET' }))
}));

const middlewares = [ thunk ];
const mockStore = configureMockStore(middlewares);

describe('graph plugin actions', () => {
    describe('PRODUCT_GRAPH_SET_POSITIONS', () => {
        it('should not dispatch if the workspace is read only', () => {
            const store = mockStore({
                workspace: {
                    currentId: 'WORKSPACE1',
                    byId: {
                        WORKSPACE1: {
                            editable: false
                        }
                    }
                },
                product: {
                    workspaces: {
                        WORKSPACE1: {
                            products: {
                                PRODUCT1: {}
                            }
                        }
                    }
                }
            });
            expect(
               store.dispatch(
                actions.setPositions({
                    productId: 'PRODUCT1',
                    updateVertices: {
                        VERTEX1: {
                            pos: {
                                x: 4.5,
                                y: 10.1
                            }
                        }
                    },
                    undoable: true
                })
               )
            ).toBeUndefined();
        });

        it('should dispatch PRODUCT_GRAPH_SET_POSITIONS on an existing vertex', () => {
            const workspaceId = 'WORKSPACE1';
            const productId = 'PRODUCT1';
            const expectedActions = [
                {
                    type: 'PRODUCT_GRAPH_SET_POSITIONS',
                    payload: {
                        productId,
                        workspaceId,
                        updateVertices: {
                            VERTEX1: { id: 'VERTEX1', pos: { x: 5, y: 10 }}
                        },
                        undoScope: productId,
                        undo: {
                            productId,
                            updateVertices: {
                                VERTEX1: { id: 'VERTEX1', pos: { x: 0, y: 0 }}
                            }
                        },
                        redo: {
                            productId,
                            updateVertices: {
                                VERTEX1: { id: 'VERTEX1', pos: { x: 5, y: 10 }}
                            }
                        }
                    }
                }
            ];
            const store = mockStore({
                workspace: {
                    currentId: 'WORKSPACE1',
                    byId: {
                        WORKSPACE1: {
                            editable: true
                        }
                    }
                },
                product: {
                    workspaces: {
                        WORKSPACE1: {
                            products: {
                                PRODUCT1: {
                                    extendedData: {
                                        vertices: {
                                            VERTEX1: {
                                                id: 'VERTEX1',
                                                pos: {
                                                    x: 0,
                                                    y: 0
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            });

            return store.dispatch(
                actions.setPositions({
                    productId: 'PRODUCT1',
                    updateVertices: {
                        VERTEX1: {
                            id: 'VERTEX1',
                            pos: {
                                x: 4.5,
                                y: 10.1
                            }
                        }
                    },
                    undoable: true
                })
            ).then(() => {
                expect(store.getActions()).toEqual(expectedActions);
            });
        });

        it('should dispatch PRODUCT_GRAPH_SET_POSITIONS on a new vertex',  () => {
            const workspaceId = 'WORKSPACE1';
            const productId = 'PRODUCT1';
            const expectedActions = [
                {
                    type: 'PRODUCT_GRAPH_SET_POSITIONS',
                    payload: {
                        productId,
                        workspaceId,
                        updateVertices: {
                            VERTEX1: { id: 'VERTEX1', pos: { x: 5, y: 10 }}
                        },
                        undoScope: productId,
                        undo: {
                            productId,
                            removeElements: {
                                vertexIds: [ 'VERTEX1' ],
                                collapsedNodeIds: []
                            }
                        },
                        redo: {
                            productId,
                            updateVertices: {
                                VERTEX1: { id: 'VERTEX1', pos: { x: 5, y: 10 }}
                            }
                        }
                    }
                },
                {
                    type: 'MOCK_PRODUCT_UPDATE'
                },
                {
                    type: 'MOCK_ELEMENT_GET'
                }
            ];
            const store = mockStore({
                workspace: {
                    currentId: 'WORKSPACE1',
                    byId: {
                        WORKSPACE1: {
                            editable: true
                        }
                    }
                },
                product: {
                    workspaces: {
                        WORKSPACE1: {
                            products: {
                                PRODUCT1: {
                                    extendedData: { }
                                }
                            }
                        }
                    }
                }
            });

            return store.dispatch(
                actions.setPositions({
                    productId: 'PRODUCT1',
                    updateVertices: {
                        VERTEX1: {
                            id: 'VERTEX1',
                            pos: {
                                x: 4.5,
                                y: 10.1
                            }
                        }
                    },
                    undoable: true
                })
            ).then(() => {
                expect(store.getActions()).toEqual(expectedActions);
            });
        });

        it('should dispatch PRODUCT_GRAPH_SET_POSITIONS twice when snap to grid is enabled', () => {
            const workspaceId = 'WORKSPACE1';
            const productId = 'PRODUCT1';
            const expectedActions = [
                {
                    type: 'PRODUCT_GRAPH_SET_POSITIONS',
                    payload: {
                        productId,
                        workspaceId,
                        updateVertices: {
                            VERTEX1: { id: 'VERTEX1', pos: { x: 5, y: 10 }}
                        },
                        undoScope: productId,
                        undo: {
                            productId,
                            updateVertices: {
                                VERTEX1: { id: 'VERTEX1', pos: { x: 0, y: 0 }}
                            }
                        },
                        redo: {
                            productId,
                            updateVertices: {
                                VERTEX1: { id: 'VERTEX1', pos: { x: 5, y: 10 }}
                            }
                        }
                    }
                },
                {
                    type: 'PRODUCT_GRAPH_SET_POSITIONS',
                    payload: {
                        productId: 'PRODUCT1',
                        workspaceId: 'WORKSPACE1',
                        updateVertices: {
                            VERTEX1: { id: 'VERTEX1', pos: { x: 0, y: -12.5 }}
                        }
                    }
                }
            ];
            const store = mockStore({
                workspace: {
                    currentId: 'WORKSPACE1',
                    byId: {
                        WORKSPACE1: {
                            editable: true
                        }
                    }
                },
                product: {
                    workspaces: {
                        WORKSPACE1: {
                            products: {
                                PRODUCT1: {
                                    extendedData: {
                                        vertices: {
                                            VERTEX1: {
                                                id: 'VERTEX1',
                                                pos: {
                                                    x: 0,
                                                    y: 0
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            });

            return store.dispatch(
                actions.setPositions({
                    productId: 'PRODUCT1',
                    updateVertices: {
                        VERTEX1: {
                            id: 'VERTEX1',
                            pos: {
                                x: 4.5,
                                y: 10.1
                            }
                        }
                    },
                    undoable: true,
                    snapToGrid: true
                })
            ).then(() => {
                expect(store.getActions()).toEqual(expectedActions);
            });
        });

        it('should dispatch PRODUCT_GRAPH_SET_POSITIONS twice on a new vertex when snap to grid is enabled',  () => {
            const productId = 'PRODUCT1';
            const expectedActions = [
                {
                    type: 'PRODUCT_GRAPH_SET_POSITIONS',
                    payload: {
                        productId,
                        workspaceId: 'WORKSPACE1',
                        updateVertices: {
                            VERTEX1: { id: 'VERTEX1', pos: { x: 5, y: 10 }}
                        },
                        undoScope: productId,
                        undo: {
                            productId,
                            removeElements: {
                                vertexIds: [ 'VERTEX1' ],
                                collapsedNodeIds: []
                            }
                        },
                        redo: {
                            productId,
                            updateVertices: {
                                VERTEX1: { id: 'VERTEX1', pos: { x: 5, y: 10 }}
                            }
                        }
                    }
                },
                {
                    type: 'PRODUCT_GRAPH_SET_POSITIONS',
                    payload: {
                        productId,
                        workspaceId: 'WORKSPACE1',
                        updateVertices: {
                            VERTEX1: { id: 'VERTEX1', pos: { x: 0, y: -12.5 }}
                        }
                    }
                },
                {
                    type: 'MOCK_PRODUCT_UPDATE'
                },
                {
                    type: 'MOCK_ELEMENT_GET'
                }
            ];
            const store = mockStore({
                workspace: {
                    currentId: 'WORKSPACE1',
                    byId: {
                        WORKSPACE1: {
                            editable: true
                        }
                    }
                },
                product: {
                    workspaces: {
                        WORKSPACE1: {
                            products: {
                                [productId]: {
                                    extendedData: { }
                                }
                            }
                        }
                    }
                }
            });

            return store.dispatch(
                actions.setPositions({
                    productId,
                    updateVertices: {
                        VERTEX1: {
                            id: 'VERTEX1',
                            pos: {
                                x: 4.5,
                                y: 10.1
                            }
                        }
                    },
                    undoable: true,
                    snapToGrid: true
                })
            ).then(() => {
                expect(store.getActions()).toEqual(expectedActions);
            });
        });
    });
});
