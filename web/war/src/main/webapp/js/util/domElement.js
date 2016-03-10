define([], function() {
    'use strict';

    return {
        removeStyles: removeStyles,
        removeAllStyles: removeAllStyles
    };

    function removeAllStyles(el) {
        removeStyles.apply(null, [el].concat(_.keys(el.style)));
    }

    function removeStyles(el /*, classes...*/) {
        _.chain(arguments)
            .rest()
            .map(function(name) {
                if (_.isString(name)) {
                    return [
                        name,
                        name.replace(/([A-Z])/g, function(str, letter) {
                            return '-' + letter.toLowerCase();
                        })
                    ];
                }
            })
            .flatten()
            .compact()
            .unique()
            .each(function(name) {
                try {
                    el.style.removeProperty(name);
                    delete el.style[name];
                } catch(e) { /* eslint no-empty:0 */ }
            })
    }
});

