define(['/base/jsc/data/web-worker/store/actions'], function(actions) {
    actions.protectFromWorker = actions.protectFromMain = () => {};
    require(['/base/jsc/data/web-worker/store/element/actions-impl.js', 'data/web-worker/util/ajax'], function(actions, ajax) {

        describe('elementActions', () => {
            const workspaceId = 'ws1';
            const workspaceIds = ['ws0', 'ws1', 'ws2'];
            const deletedElement = (id) => ({ id, _DELETED: true });
            const genState = () => ({
                element: {focusing: {}},
                workspace: { currentId: workspaceId }
            });
            const multipleWorkspaceState = (() => {
                const state = { workspace: { currentId: workspaceId }};
                const elementState = {};

                for (let i = 0; i < 3; i++) {
                    elementState[`ws${i}`] = {
                        vertices: {
                            v1: { id: 'v1'}
                        },
                        edges: {
                            e1: { id: 'e1'}
                        }
                    }
                }

                state.element = elementState;
                return state;
            })();
            const genMultipleWorkspaceState = () => multipleWorkspaceState;
            const dispatch = sinon.spy();

            it('should be able to update a single element', () => {
                let element = { type: 'vertex'};
                actions.updateElement(workspaceId, element).should.deep.equal({
                    type: 'ELEMENT_UPDATE',
                    payload: {
                        vertices: [element],
                        workspaceId
                    }
                });

                element = { type: 'edge'};
                actions.updateElement(workspaceId, element).should.deep.equal({
                    type: 'ELEMENT_UPDATE',
                    payload: {
                        edges: [element],
                        workspaceId
                    }
                });
            });

             it('should be able to update multiple elements', () => {
                 const elements = { vertices: [{}], edges: [{}], workspaceId};
                 actions.update(elements).should.deep.equal({
                     type: 'ELEMENT_UPDATE',
                     payload: {
                        vertices: [{}],
                        edges: [{}],
                        workspaceId
                     }
                 });
             });

            it('should be able to delete elements', (done) => {
                const storeElement = {[workspaceId]: {
                    vertices: {
                        v1: {}
                    },
                    edges: {
                        e1: {}
                    }
                }};
                const genElementState = () => Object.assign(genState(), {element: storeElement});

                let elements = { vertexIds: ['v1'] };
                ajax.returns(Promise.resolve({ exists: { ['v1']: false }}));
                actions.deleteElements(elements)(dispatch, genElementState)
                    .then((result) => {
                         dispatch.should.have.been.calledWith({
                            type: 'ELEMENT_UPDATE',
                            payload: {
                                vertices: [deletedElement('v1')],
                                edges: [],
                                workspaceId
                            }
                         });

                        done();
                    });
            });

            it('should delete elements from each workspace', (done) => {
                let elements = { vertexIds: ['v1'] };
                ajax.returns(Promise.resolve({ exists: { ['v1']: false }}));
                actions.deleteElements(elements)(dispatch, genMultipleWorkspaceState)
                    .then((result) => {
                        workspaceIds.forEach((workspaceId) => {
                             dispatch.should.have.been.calledWith({
                                type: 'ELEMENT_UPDATE',
                                payload: {
                                    vertices: [deletedElement('v1')],
                                    edges: [],
                                    workspaceId
                                }
                             });
                        });

                        done();
                    });
            });

            it('propertyChange should update elements on each workspace', () => {
                sinon.spy(actions, 'get');
                const shouldUpdateEveryWorkspace = () => {
                    workspaceIds.forEach((workspaceId) => {
                        actions.get.should.have.been.calledWithMatch(sinon.match({workspaceId}));
                    });
                };

                let change = { workspaceId, graphVertexId: 'v1' };
                actions.propertyChange(change)(dispatch, genMultipleWorkspaceState);
                shouldUpdateEveryWorkspace();

                change = { workspaceId, graphEdgeId: 'e1' };
                actions.propertyChange(change)(dispatch, genMultipleWorkspaceState);
                shouldUpdateEveryWorkspace();

                actions.get.restore();
            });
        });
    });
});
