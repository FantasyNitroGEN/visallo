define(['updeep'], function(updeep) {
    'use strict';

    return function user(state, { type, payload }) {
        if (!state) return { current: null };

        switch (type) {
            case 'USER_PUT':
                return updeep({ current: payload.user }, state);
            case 'USER_PUT_PREFS':
                return updeep({ current: { uiPreferences: payload.preferences }}, state);
            case 'USER_PUT_PREF':
                return updeep({ current: { uiPreferences: { [payload.name]: `${payload.value}` }}}, state);
        }

        return state
    }
})

