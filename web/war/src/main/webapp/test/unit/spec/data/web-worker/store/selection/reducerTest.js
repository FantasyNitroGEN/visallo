define(['/base/jsc/data/web-worker/store/selection/reducer'], function(reducer) {
    const emptyState = {
        idsByType: { vertices: [], edges: [] }
    };
    const genState = ({ vertices = [], edges = [] }) => ({
        idsByType: { vertices, edges }
    })

    describe.only('selectionReducer', () => {

        it('should initialize state', () => {
            reducer(null, {}).should.deep.equal(emptyState)
        })

        it('should should add selection to empty', () => {
            var result = reducer(emptyState, {
                type: 'SELECTION_ADD',
                payload: { selection: { vertices: ['a'], edges: [] }} 
            });
            result.should.deep.equal({
                idsByType: { vertices: ['a'], edges: [] }
            })

            result = reducer(emptyState, {
                type: 'SELECTION_ADD',
                payload: { selection: { vertices: [], edges: ['b'] }} 
            });
            result.should.deep.equal({
                idsByType: { vertices: [], edges: ['b'] }
            })
        })
        
        it('should should add selection to existing', () => {
            const vertices = ['a'], edges = ['b']
            var result = reducer(genState({ vertices, edges }), {
                type: 'SELECTION_ADD',
                payload: { selection: { vertices: ['a2'], edges: ['b2', 'b3'] }} 
            });
            result.should.deep.equal({
                idsByType: { vertices: ['a', 'a2'], edges: ['b', 'b2', 'b3'] }
            })
        })

        it('should should remove selection from existing', () => {
            const vertices = ['a'], edges = ['b', 'b3']
            var result = reducer(genState({ vertices, edges }), {
                type: 'SELECTION_REMOVE',
                payload: { selection: { vertices: ['a'], edges: ['b'] }} 
            });
            result.should.deep.equal({
                idsByType: { vertices: [], edges: ['b3'] }
            })
        })

        it('should should clear selection', () => {
            const vertices = ['a'], edges = ['b', 'b3']
            var result = reducer(genState({ vertices, edges }), {
                type: 'SELECTION_CLEAR'
            });
            result.should.deep.equal({
                idsByType: { vertices: [], edges: [] }
            })
        })

        it('should should set selection', () => {
            const vertices = ['a'], edges = ['b', 'b3']
            var result = reducer(genState({ vertices, edges }), {
                type: 'SELECTION_SET',
                payload: { selection: { vertices: ['a1'], edges: ['b1']} }
            });
            result.should.deep.equal({
                idsByType: { vertices: ['a1'], edges: ['b1'] }
            })
        })

        it('should should not create new objects if not updating', () => {
            const edges = [];
            var result = reducer(genState({ edges }), {
                type: 'SELECTION_ADD',
                payload: { selection: { vertices: ['a'], edges: [] }} 
            });
            expect(result.idsByType.edges === edges).to.be.true

            result = reducer(genState({ vertices: edges }), {
                type: 'SELECTION_ADD',
                payload: { selection: { vertices: [], edges: ['a'] }} 
            });
            expect(result.idsByType.vertices === edges).to.be.true

            result = reducer(genState({ vertices: edges, edges }), {
                type: 'SELECTION_ADD',
                payload: { selection: { vertices: [], edges: [] }} 
            });
            expect(result.idsByType.vertices === edges).to.be.true
            expect(result.idsByType.edges === edges).to.be.true
        })
    })
});
