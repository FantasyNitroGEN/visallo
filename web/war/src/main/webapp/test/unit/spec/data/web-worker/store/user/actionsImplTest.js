define([
    '/base/jsc/data/web-worker/store/user/actions-impl.js',
    'data/web-worker/util/ajax'
], function(userActions, ajax) {

    describe('userActions', () => {
        it('should create an action to put a user', () => {
            const user = {
                id: '1',
                userName: 'Test User'
            };
            const expectedAction = {
                type: 'USER_PUT',
                payload: {
                    user
                }
            };

            userActions.putUser({ user }).should.deep.equal(expectedAction);
        });

        it('should create an action to put user preferences', () => {
            const preferences = {
                TestPref: 'test value'
            };
            const expectedAction = {
                type: 'USER_PUT_PREFS',
                payload: {
                    preferences
                }
            };

            userActions.putUserPreferences({ preferences })
                    .should.deep.equal(expectedAction);
        });

        it('should create an action to put a single user preference', () => {
            const name = 'TestPref';
            const value = 'test value';
            const expectedAction = {
                type: 'USER_PUT_PREF',
                payload: {
                    name,
                    value
                }
            };

            userActions.putUserPreference({ name, value })
                    .should.deep.equal(expectedAction);
        });


        it('should create USER_PUT_PREF and POST to /user/ui-preferences', () => {
            const dispatch = sinon.spy();
            const createFakeStore = (fakeData) => ({
                dispatch,
                getState() {
                    return fakeData;
                }
            });
            const dispatchWithStoreOf = (storeData, action) => {
                let dispatched;
                const dispatch = createFakeStore(storeData)(actionAttempt => {
                    dispatched = actionAttempt;
                });
                dispatch(action);
                return dispatched;
            };

            const name = 'TestPref';
            const value = 'test value';
            const expectedAction = {
                type: 'USER_PUT_PREF',
                payload: {
                    name,
                    value
                }
            };

            userActions.setUserPreference(
                {
                    name,
                    value
                }
            )(dispatch);
            expect(dispatch).to.have.been.calledWith(expectedAction);
            expect(ajax).to.have.been.calledWith(
                'POST',
                '/user/ui-preferences',
                {
                    name,
                    value
                }
            );
        });
    });
});
