define([], function() {
    'use strict';

    return {
        removeStyles: removeStyles,
        removeAllStyles: removeAllStyles
    };

    function removeAllStyles(el) {
        var styles = [];
        //loop through style object for IE11
        for (var i in el.style) {
            styles.push(i);
        }
        removeStyles.apply(null, [el].concat(styles));
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

