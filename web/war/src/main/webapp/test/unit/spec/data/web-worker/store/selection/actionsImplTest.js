define(['/base/jsc/data/web-worker/store/actions'], function(actions) {
    actions.protectFromWorker = actions.protectFromMain = () => {}
    require(['/base/jsc/data/web-worker/store/selection/actions-impl'], function(actions) {
        describe('selectionActions', () => {

            it('should be able to add selections', () => {
                const selection = {};
                actions.add({ selection }).should.deep.equal({
                    type: 'SELECTION_ADD',
                    payload: { selection }
                })
            })

            it('should be able to remove selections', () => {
                const selection = {};
                actions.remove({ selection }).should.deep.equal({
                    type: 'SELECTION_REMOVE',
                    payload: { selection }
                })
            })

            it('should be able to clear selections', () => {
                actions.clear().should.deep.equal({ type: 'SELECTION_CLEAR' })
            })

            it('should be able to set selections', () => {
                const selection = {}
                actions.set({ selection }).should.deep.equal({
                    type: 'SELECTION_SET',
                    payload: { selection }
                })
            })
        })
    })
})
