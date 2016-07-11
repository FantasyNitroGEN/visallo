/**
 * Rebuild the custom ./babel.js if changing plugins/presets
 *
 * https://github.com/v5analytics/babel-standalone
 *
 * Expected Scope:
 * input [String] JSX Source
 * resourcePath [String] Classpath to JSX Source File
 * sourcePath [String] Path to original source JSX File (for external sourcemaps)
 * sourceMapType [String|Boolean] Type of source map to create (external=true, infile='inline', none=false)
 */
(function() {
    try {
        var result = Babel.transform(input, {
          "sourceMap": sourceMapType,
          "presets": ["es2015"],
          "plugins": ["transform-react-jsx", "transform-react-display-name", "transform-object-rest-spread"]
        });

        if (result) {
            if (result.map) {
                result.map.sources = [sourcePath];
            }
            return {
                sourceMap: JSON.stringify(result.map),
                code: result.code
            }
        } else {
            return { error: 'No result from transform' };
        }
    } catch (e) {
        print("Error transpiling: " + resourcePath);
        print(e.stack);
        return { error: e.message };
    }
})();