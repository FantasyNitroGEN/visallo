define([], function() {
    'use strict';

    return formulaFunction;

    function formulaFunction(formula, vertex, V, optionalKey, optionalOpts) {
        if (!optionalOpts) {
            optionalOpts = {};
        }
        optionalOpts = _.extend({}, optionalOpts, { ignoreDisplayFormula: true });

        var prop = _.partial(V.prop, vertex, _, optionalKey, optionalOpts),
            propRaw = _.partial(V.propRaw, vertex, _, optionalKey, optionalOpts),
            longestProp = _.partial(V.longestProp, vertex);

        try {

            // If the formula is an expression wrap and return it
            if (formula.indexOf('return') === -1) {
                formula = 'return (' + formula + ')';
            }

            var scope = _.extend({}, optionalOpts.additionalScope || {}, {
                prop: prop,
                dependentProp: prop,
                propRaw: propRaw,
                longestProp: longestProp
            });

            if (V.isEdge(vertex)) {
                scope.edge = vertex;
            } else {
                scope.vertex = vertex;
            }

            var keys = [],
                values = [];

            _.each(scope, function(value, key) {
                values.push(value);
                keys.push(key);
            });

            /*eslint no-new-func:0*/
            return (new Function(keys.join(','), formula)).apply(null, values);
        } catch(e) {
            console.warn('Unable to execute formula: ' + formula + ' Reason: ', e);
        }
    }
});
