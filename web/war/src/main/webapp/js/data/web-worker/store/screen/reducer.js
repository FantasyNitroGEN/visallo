define([], function() {
    'use strict';

    return function screen(state, { type, payload }) {
        if (!state) return { pixelRatio: 1 };

        switch (type) {
            case 'SCREEN_PIXELRATIO': return { ...state, pixelRatio: payload.pixelRatio };
        }

        return state
    }
})



