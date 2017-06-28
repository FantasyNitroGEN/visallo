/*
 * Replaces prop-types library with no-check shims in production
 */
define(visalloEnvironment.prod ? ['react-proptypes-dev'] : [], function(PropTypes) {
    const shim = function() {};
    shim.isRequired = shim;
    const getShim = function() { return shim; }
    const PropTypeShims = {
        array: shim, bool: shim, func: shim, number: shim, object: shim, string: shim, symbol: shim,
        any: shim, arrayOf: getShim, element: shim, instanceOf: getShim, node: shim, objectOf: getShim,
        oneOf: getShim, oneOfType: getShim, shape: getShim
    };

    if (PropTypes) {
        if (!shimPropTypesSameAsReal(PropTypeShims, PropTypes)) {
            throw new Error('PropTypes that are defined for production differ from those in react');
        }
        return PropTypes;
    }

    return PropTypeShims;

    function shimPropTypesSameAsReal(shims, real) {
        const shimKeys = Object.keys(shims);
        const realKeys = Object.keys(real);
        return shimKeys.length === realKeys.length &&
            _.intersection(shimKeys, realKeys).length === shimKeys.length;
    }
})
