// Define Visallo Globals for closure-compiler used in JavascriptResourceHandler for plugin js

// Underscore.js, and jQuery are implicit dependencies
var _;
var $;
var jQuery;

// Most 3rd party libraries check for these
var exports;
var module;

var visalloData;

/**
 * @type {string}
 * @const
 */
var TRANSITION_END;

/**
 * @type {string}
 * @const
 */
var ANIMATION_END;

/**
 * @param {string} key
 * @return {string}
 */
function i18n(key) {}

/**
 * @param {...?} varargs
 * @return {?}
 */
function define(varargs) {}

/**
 * @param {Array<String>} dependencies
 * @param {Function} callback
 * @return {?}
 */
function require(dependencies, callback) {}


function dispatchMain() {}