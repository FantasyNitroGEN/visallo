/*
 * Replaces prop-types library with no-check shims in production,
 * Normally webpack would handle this for us, but not using that yet...
 */
define(visalloEnvironment.dev ? ['react-proptypes-dev'] : [], function(PropTypes) {
    const PropTypeShims = getPropTypeShims();

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

    function getPropTypeShims() {
        const shim = function() {};
        shim.isRequired = shim;
        const getShim = function() { return shim; }
        const PropTypeShims = {
            any: shim,
            array: shim,
            arrayOf: getShim,
            bool: shim,
            checkPropTypes: shim,
            element: shim,
            func: shim,
            instanceOf: getShim,
            node: shim,
            number: shim,
            object: shim,
            objectOf: getShim,
            oneOf: getShim,
            oneOfType: getShim,
            shape: getShim,
            string: shim,
            symbol: shim
        };
        PropTypeShims.PropTypes = PropTypeShims;
        return PropTypeShims;
    }
})
