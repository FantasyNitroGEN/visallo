define(['underscore'], function(_) {
    const warnOnce = _.once((message) => { console.warn(message) });

    return function fixCytoscapeCorsHandling(cy) {
        const r = cy.renderer();
        if (_.isFunction(r.getCachedImage)) {
            r.getCachedImage = _.wrap(r.getCachedImage, function(originalFunction, url, crossOrigin, onLoad) {
                if (_.isObject(crossOrigin) && crossOrigin.value) {
                    return originalFunction.bind(this)(url, crossOrigin.value, onLoad);
                } else {
                    warnOnce('getCachedImage signature has changed, upgrade Cytoscape dependency');
                    return originalFunction.bind(this)(url, crossOrigin, onLoad);
                }
            });
        } else {
            throw new Error('Expected to wrap getCachedImage function');
        }
    }
});
