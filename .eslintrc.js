module.exports = {
    "root": true,
    "env": {
        "browser": true,
        "es6": true,
        "amd": true,
        "jquery": true
    },
    "globals": {
        "DEBUG": false,
        "visalloData": false,
        "visalloCacheBreaker": false,
        "visalloPluginResources": false,
        "TRANSITION_END": false,
        "ANIMATION_END": false,
        "i18n": false,
        "_": false
    },
    "parserOptions": {
        "ecmaFeatures": {
            "experimentalObjectRestSpread": true,
            "jsx": true,
            "blockBindings": true,
            "destructuring": true,
            "spread": true,
            "arrowFunctions": true
        }
    },
    "plugins": [
        "react"
    ],
    "rules": {
        "no-alert": "error",
        "no-array-constructor": "error",
        "no-caller": "error",
        "no-catch-shadow": "error",
        "no-cond-assign": "error",
        "no-constant-condition": "error",
        "no-control-regex": "error",
        "no-debugger": "error",
        "no-delete-var": "error",
        "no-dupe-keys": "error",
        "no-duplicate-case": "error",
        "no-empty": "error",
        "no-empty-character-class": "error",
        "no-eq-null": "error",
        "no-eval": "error",
        "no-ex-assign": "error",
        "no-extend-native": "error",
        "no-extra-bind": "error",
        "no-extra-boolean-cast": "error",
        "no-extra-semi": "error",
        "no-fallthrough": "error",
        "no-func-assign": "error",
        "no-implied-eval": "error",
        "no-inner-declarations": ["error", "functions"],
        "no-invalid-regexp": "error",
        "no-irregular-whitespace": "error",
        "no-iterator": "error",
        "no-label-var": "error",
        "no-labels": "error",
        "no-lone-blocks": "error",
        "no-loop-func": "error",
        "no-mixed-spaces-and-tabs": "error",
        "no-multi-spaces": "error",
        "no-multi-str": "error",
        "no-native-reassign": "error",
        "no-negated-in-lhs": "error",
        "no-new": "error",
        "no-new-func": "error",
        "no-new-object": "error",
        "no-new-wrappers": "error",
        "no-obj-calls": "error",
        "no-octal": "error",
        "no-octal-escape": "error",
        "no-proto": "error",
        "no-redeclare": "error",
        "no-regex-spaces": "error",
        "no-script-url": "error",
        "no-sequences": "error",
        "no-shadow-restricted-names": "error",
        "no-spaced-func": "error",
        "no-sparse-arrays": "error",
        "no-trailing-spaces": "error",
        "no-undef": "error",
        "no-undef-init": "error",
        "no-undef-init": "error",
        "no-unreachable": "error",
        "no-unused-expressions": "error",
        "no-with": "error",
        "no-extra-parens": ["error", "functions"],
        "camelcase": "error",
        "comma-spacing": "error",
        "eol-last": "error",
        "eqeqeq": "error",
        "new-parens": "error",
        "space-infix-ops": "error",
        "strict": ["error", "function"],
        "use-isnan": "error",
        "valid-typeof": "error",
        "no-console": [
            "off",
            //"error",
            { "allow": ["warn", "error"] }
        ],
        "linebreak-style": [
            "error",
            "unix"
        ],
        "quotes": [
            "error",
            "single",
            { "avoidEscape": true }
        ],
        "keyword-spacing": [
            "error",
            { "overrides": { "catch": { "after": false } } }
        ],
        "arrow-parens": [
            "off",
            //"error",
            "as-needed"
        ]
    }
};
