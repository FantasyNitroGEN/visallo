define([], function() {

    return function fixCytoscapeCorsHandling(cy) {
        const r = cy.renderer();
        if (_.isFunction(r.getCachedImage)) {
            r.getCachedImage = fixedGetCachedImage;
        } else {
            throw new Error('Expected to replace getCachedImage function');
        }
    }

    function fixedGetCachedImage(url, onLoad) {
        if (arguments.length !== 2) {
            throw new Error('Expected 2 arguments, maybe cytoscape was upgraded?');
        }
        if (!_.isString(url)) {
            throw new Error('Expected string url argument');
        }
        if (!_.isFunction(onLoad)) {
            throw new Error('Expected function onLoad argument');
        }

        var r = this;
        var imageCache = r.imageCache = r.imageCache || {};
        var cache = imageCache[ url ];

        if( cache ){
            if( !cache.image.complete ){
                cache.image.addEventListener('load', onLoad);
            }

            return cache.image;
        } else {
            cache = imageCache[ url ] = imageCache[ url ] || {};
            var image = cache.image = new Image(); // eslint-disable-line no-undef
            image.addEventListener('load', onLoad);
            if ((/^(\/\/|http)/).test(url)) {
                image.crossOrigin = 'Anonymous'; // prevent tainted canvas
            }
            image.onerror = function(e){
                console.warn('Error loading graph image', e)
            }
            image.src = url;

            return image;
        }
    }
});
