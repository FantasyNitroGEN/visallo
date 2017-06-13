define([
    // We mock store.js so have to use absolute path here
    '/base/jsc/data/web-worker/store/product/reducer'
], function(reducer) {

    const workspaceId = 'w1';
    const genState = ({
        wId = workspaceId,
        loading = false,
        loaded = false,
        products = []
    }) => ({
        workspaces: {[wId]: {loaded, loading, products}}
    });

    describe('productReducer', () => {

        it('should update product list', () => {
            const products = [{id: 'p1'}];
            const nextState = reducer(genState({}), {
                type: 'PRODUCT_LIST',
                payload: {loading: false, loaded: true, products, workspaceId}
            });
            nextState.should.deep.equal({
                workspaces: {
                    [workspaceId]: {
                        loading: false,
                        loaded: true,
                        products: {p1: {id: 'p1'}}
                    }
                }
            });
        });

        it('should update data with a new key/value', () => {
            const nextState = reducer(genState({
                products: {
                    p1: {
                        id: 'p1',
                        data: {
                            existingKey: {k1: 'v1', k2: 'v2'}
                        }
                    }
                }
            }), {
                type: 'PRODUCT_UPDATE_DATA',
                payload: {
                    workspaceId,
                    productId: 'p1',
                    key: 'key1',
                    value: {k1: 'v1', k2: 'v2'}
                }
            });
            nextState.workspaces[workspaceId].products['p1'].should.deep.equal({
                id: 'p1',
                data: {
                    existingKey: {k1: 'v1', k2: 'v2'},
                    key1: {k1: 'v1', k2: 'v2'}
                }
            });
        });

        it('should update data with an existing key/value', () => {
            const nextState = reducer(genState({
                products: {
                    p1: {
                        id: 'p1',
                        data: {
                            existingKey1: {k1: 'v1', k2: 'v2'},
                            existingKey2: {k1: 'v1', k2: 'v2'}
                        }
                    }
                }
            }), {
                type: 'PRODUCT_UPDATE_DATA',
                payload: {
                    workspaceId,
                    productId: 'p1',
                    key: 'existingKey1',
                    value: {k1: 'v1b', k2: 'v2b'}
                }
            });
            nextState.workspaces[workspaceId].products['p1'].should.deep.equal({
                id: 'p1',
                data: {
                    existingKey1: {k1: 'v1b', k2: 'v2b'},
                    existingKey2: {k1: 'v1', k2: 'v2'}
                }
            });
        });

        it('should update extendedData with a new key/value', () => {
            const nextState = reducer(genState({
                products: {
                    p1: {
                        id: 'p1',
                        extendedData: {
                            existingKey: {k1: 'v1', k2: 'v2'}
                        }
                    }
                }
            }), {
                type: 'PRODUCT_UPDATE_EXTENDED_DATA',
                payload: {
                    workspaceId,
                    productId: 'p1',
                    key: 'key1',
                    value: {k1: 'v1', k2: 'v2'}
                }
            });
            nextState.workspaces[workspaceId].products['p1'].should.deep.equal({
                id: 'p1',
                extendedData: {
                    existingKey: {k1: 'v1', k2: 'v2'},
                    key1: {k1: 'v1', k2: 'v2'}
                }
            });
        });

        describe('ELEMENT_UPDATE', () => {
            it('should update extendedData with authorized edges', () => {
                const nextState = reducer(
                    genState({
                        products: {
                            p1: {
                                id: 'p1',
                                extendedData: {
                                    edges: {
                                        e1: {
                                            edgeId: 'e1',
                                            inVertexId: 'v1',
                                            label: 'l',
                                            outVertexId: 'v2',
                                            unauthorized: true
                                        }
                                    },
                                    vertices: {}
                                }
                            }
                        }
                    }),
                    {
                        type: 'ELEMENT_UPDATE',
                        payload: {
                            edges: [
                                {
                                    id: 'e1',
                                    inVertexId: 'v1',
                                    label: 'l',
                                    outVertexId: 'v2'
                                }
                            ],
                            vertices: [],
                            workspaceId
                        }
                    }
                );
                nextState.workspaces[workspaceId].products['p1'].should.deep.equal({
                    id: 'p1',
                    extendedData: {
                        edges: {
                            e1: {
                                edgeId: 'e1',
                                inVertexId: 'v1',
                                label: 'l',
                                outVertexId: 'v2'
                            }
                        },
                        vertices: {}
                    }
                });
            });

            it('should update extendedData with unauthorized edges', () => {
                const nextState = reducer(
                    genState({
                        products: {
                            p1: {
                                id: 'p1',
                                extendedData: {
                                    edges: {
                                        e1: {
                                            edgeId: 'e1',
                                            inVertexId: 'v1',
                                            label: 'l',
                                            outVertexId: 'v2'
                                        }
                                    },
                                    vertices: {}
                                }
                            }
                        }
                    }),
                    {
                        type: 'ELEMENT_UPDATE',
                        payload: {
                            edges: [
                                {
                                    id: 'e1',
                                    _DELETED: true
                                }
                            ],
                            vertices: [],
                            workspaceId
                        }
                    }
                );
                nextState.workspaces[workspaceId].products['p1'].should.deep.equal({
                    id: 'p1',
                    extendedData: {
                        edges: {
                            e1: {
                                edgeId: 'e1',
                                inVertexId: 'v1',
                                label: 'l',
                                outVertexId: 'v2',
                                unauthorized: true
                            }
                        },
                        vertices: {}
                    }
                });
            });
        });
    });
});
