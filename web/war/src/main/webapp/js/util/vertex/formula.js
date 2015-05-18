define([], function() {
    'use strict';

    return formulaFunction;

    function formulaFunction(formula, vertex, V, optionalKey, optionalOpts) {
        if (!optionalOpts) {
            optionalOpts = {};
        }
        optionalOpts.ignoreDisplayFormula = true;

        var prop = _.partial(V.prop, vertex, _, optionalKey, optionalOpts),
            propRaw = _.partial(V.propRaw, vertex, _, optionalKey, optionalOpts),
            longestProp = _.partial(V.longestProp, vertex);

        try {

            // If the formula is an expression wrap and return it
            if (formula.indexOf('return') === -1) {
                formula = 'return (' + formula + ')';
            }

            /*eslint no-new-func:0*/
            return new Function(
                // Get property value and converted to string displayValue
                'prop', 'dependentProp',
                // Get actual raw property value
                'propRaw',
                // Get the longest property value and converted to string displayValue
                'longestProp',
                // Vertex Json
                'vertex',
                // Inner function string
                formula)(
                    prop,
                    prop,
                    propRaw,
                    longestProp,
                    vertex);

        } catch(e) {
            console.warn('Unable to execute formula: ' + formula + ' Reason: ', e);
        }
    }
});
