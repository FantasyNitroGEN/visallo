
/*eslint no-undef:0,strict:0,quote-props:0*/
var require = {
    baseUrl: 'jsc',
    waitSeconds: 0,
    paths: {
        'arbor': '../libs/cytoscape/lib/arbor',
        'async': '../libs/requirejs-plugins/src/async',
        'atmosphere': '../libs/atmosphere/atmosphere',
        'bootstrap': '../libs/bootstrap/docs/assets/js/bootstrap',
        'bootstrap-datepicker': '../libs/bootstrap-datepicker/js/bootstrap-datepicker',
        'bootstrap-timepicker': '../libs/bootstrap-timepicker/js/bootstrap-timepicker',
        'bluebird': '../libs/bluebird/js/browser/bluebird.min',
        'chrono': '../libs/chrono/chrono.min',
        'colorjs': '../libs/color-js/color',
        'cytoscape': '../libs/cytoscape/dist/cytoscape',
        'd3': '../libs/d3/d3',
        'duration-js': '../libs/duration.js/duration',
        'easing': '../libs/jquery.easing/js/jquery.easing',
        'ejs': '../libs/ejs/ejs',
        'es5shim': '../libs/es5-shim/es5-shim',
        'es5sham': '../libs/es5-shim/es5-sham',
        'flight': '../libs/flight',
        'goog': '../libs/requirejs-plugins/src/goog',
        'gremlins': '../libs/gremlins.js/gremlins.min',
        'hbs': '../libs/require-handlebars-plugin/hbs',
        'handlebars': '../libs/require-handlebars-plugin/hbs/handlebars',
        'intercom': '../libs/intercom/intercom',
        'jstz': '../libs/jstz-detect/jstz.min',
        'jquery': '../libs/jquery/jquery',
        'jqueryui': '../libs/jquery-ui/ui/minified/jquery-ui.min',
        'jquery-scrollstop': '../libs/jquery-scrollstop/jquery.scrollstop',
        'jscache': '../libs/jscache/cache',
        'less': 'util/requirejs/less',
        'lessc': '../libs/require-less/lessc',
        'moment': '../libs/moment/min/moment-with-locales.min',
        'moment-timezone': '../libs/moment-timezone/builds/moment-timezone-with-data.min',
        'normalize': '../libs/require-less/normalize',
        'openlayers': '../libs/openlayers/OpenLayers.debug',
        'pathfinding': '../libs/PathFinding.js/lib/pathfinding-browser',
        'propertyParser': '../libs/requirejs-plugins/src/propertyParser',
        'rangy-core': '../libs/rangy-official/rangy-core',
        'rangy-text': '../libs/rangy-official/rangy-textrange',
        'rangy-highlighter': '../libs/rangy-official/rangy-highlighter',
        'rangy-cssclassapplier': '../libs/rangy-official/rangy-classapplier',
        'rangy-serializer': '../libs/rangy-official/rangy-serializer',
        'sf': '../libs/sf/sf',
        'text': '../libs/requirejs-text/text',
        'three': '../libs/threejs/build/three',
        'tpl': '../libs/requirejs-ejs/rejs',
        'underscore': '../libs/underscore/underscore',
        'underscore.inflection': '../libs/underscore.inflection/lib/underscore.inflection',
        'videojs': '../libs/video.js/dist/video-js/video'
    },
    shim: {
        'arbor': { deps: ['jquery'] },
        'atmosphere': { init: function() { return $.atmosphere; }, deps: ['jquery'] },
        'bootstrap': { exports: 'window', deps: ['jquery', 'jqueryui'] },
        'bootstrap-datepicker': { exports: 'window', deps: ['bootstrap'] },
        'bootstrap-timepicker': { exports: 'window', deps: ['bootstrap'] },
        'bluebird': { exports: 'Promise' },
        'chrono': { exports: 'chrono' },
        'colorjs': { init: function() { return this.net.brehaut.Color; } },
        'cytoscape': { exports: 'cytoscape', deps: ['arbor', 'easing'] },
        'd3': { exports: 'd3' },
        'duration-js': { exports: 'Duration' },
        'easing': { init: function() { return $.easing; }, deps: ['jquery', 'jqueryui'] },
        'ejs': { exports: 'ejs' },
        'hbs': { deps: ['util/handlebars/helpers'] },
        'intercom': { exports: 'Intercom' },
        'jquery': { exports: 'jQuery' },
        'jqueryui': { init: function() { return $.ui; }, deps: ['jquery'] },
        'jstz': { exports: 'jstz' },
        'openlayers': { exports: 'OpenLayers' },
        'pathfinding': { exports: 'PF' },
        'rangy-text': { deps: ['rangy-core']},
        'rangy-highlighter': { deps: ['rangy-core', 'rangy-cssclassapplier', 'rangy-serializer']},
        'rangy-cssclassapplier': { deps: ['rangy-core'] },
        'rangy-serializer': { deps: ['rangy-core'] },
        'jquery-scrollstop': { exports: 'jQuery', deps: ['jquery'] },
        'three': { exports: 'THREE' },
        'underscore': { exports: '_' },
        'videojs': { exports: 'videojs' }
    }
};


if (typeof window !== 'undefined') {
    if ('define' in window) {
        define([], function() {
            return require;
        });
    }
}
