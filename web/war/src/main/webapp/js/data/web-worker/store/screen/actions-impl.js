define(['../actions'], function(actions) {
    actions.protectFromMain();

    return {
        setPixelRatio: ({ pixelRatio }) => ({
            type: 'SCREEN_PIXELRATIO',
            payload: { pixelRatio }
        })
    }
})
