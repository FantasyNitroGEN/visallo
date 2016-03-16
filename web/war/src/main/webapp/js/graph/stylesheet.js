
define([
    'cytoscape',
    'util/retina',
    'util/requirejs/promise!util/service/ontologyPromise',
    'colorjs'
], function(cytoscape, retina, ontology, colorjs) {
    'use strict';

    var GENERIC_SIZE = 30,
        CUSTOM_IMAGE_SIZE = 50,
        IMAGE_ASPECT_RATIO = 4 / 3,
        VIDEO_ASPECT_RATIO = 19 / 9,
        style = cytoscape.stylesheet();

    return load;

    function defaultStyle(previousStyle) {
        return (previousStyle || style)

            .selector('core')
            .css({
                'outside-texture-bg-color': '#efefef'
            })

            .selector('node')
            .css({
                'background-color': '#fff',
                'background-image': 'data(imageSrc)',
                'background-fit': 'contain',
                'border-color': 'white',
                'font-family': 'helvetica',
                'font-size': 18 * retina.devicePixelRatio,
                'min-zoomed-font-size': 4,
                'text-events': 'yes',
                'text-outline-color': 'white',
                'text-outline-width': 2,
                'text-valign': 'bottom',
                opacity: 1,
                color: '#999',
                content: 'data(truncatedTitle)',
                height: GENERIC_SIZE * retina.devicePixelRatio,
                shape: 'roundrectangle',
                width: GENERIC_SIZE * retina.devicePixelRatio
            })

            .selector('node.video')
            .css({
                shape: 'rectangle',
                width: (CUSTOM_IMAGE_SIZE * retina.devicePixelRatio) * VIDEO_ASPECT_RATIO,
                height: (CUSTOM_IMAGE_SIZE * retina.devicePixelRatio) / VIDEO_ASPECT_RATIO
            })

            .selector('node.image')
            .css({
                shape: 'rectangle',
                width: (CUSTOM_IMAGE_SIZE * retina.devicePixelRatio) * IMAGE_ASPECT_RATIO,
                height: (CUSTOM_IMAGE_SIZE * retina.devicePixelRatio) / IMAGE_ASPECT_RATIO
            })

            .selector('node.hasCustomGlyph')
            .css({
                width: CUSTOM_IMAGE_SIZE * retina.devicePixelRatio,
                height: CUSTOM_IMAGE_SIZE * retina.devicePixelRatio
            })

            .selector('node.hover')
            .css({
                opacity: 0.6
            })

            .selector('node.focus')
            .css({
                color: '#00547e',
                'font-weight': 'bold',
                'overlay-color': '#a5e1ff',
                'overlay-padding': 10 * retina.devicePixelRatio,
                'overlay-opacity': 0.5
            })

            .selector('edge.focus')
            .css({
                'overlay-color': '#a5e1ff',
                'overlay-padding': 7 * retina.devicePixelRatio,
                'overlay-opacity': 0.5
            })

            .selector('node.temp')
            .css({
                'background-color': 'rgba(255,255,255,0.0)',
                'background-image': 'none',
                width: '1',
                height: '1'
            })

            .selector('node.controlDragSelection')
            .css({
                'border-width': 5 * retina.devicePixelRatio,
                'border-color': '#a5e1ff'
            })

            .selector('node:selected')
            .css({
                'background-color': '#0088cc',
                'background-image': 'data(selectedImageSrc)',
                'border-color': '#0088cc',
                'border-width': 2 * retina.devicePixelRatio,
                color: '#0088cc'
            })

            .selector('edge:selected')
            .css({
                'line-color': '#0088cc',
                color: '#0088cc',
                'target-arrow-color': '#0088cc',
                'source-arrow-color': '#0088cc',
                width: 4 * retina.devicePixelRatio
            })

            .selector('edge')
            .css({
                'font-size': 11 * retina.devicePixelRatio,
                'target-arrow-shape': 'triangle',
                color: '#aaa',
                content: visalloData.currentUser.uiPreferences.edgeLabels !== 'false' ?
                    'data(label)' : '',
                //'curve-style': 'haystack',
                'min-zoomed-font-size': 3,
                'text-outline-color': 'white',
                'text-outline-width': 2,
                width: 2.5 * retina.devicePixelRatio
            })

            .selector('edge.label')
            .css({
                content: 'data(label)',
                'font-size': 12 * retina.devicePixelRatio,
                color: '#0088cc',
                'text-outline-color': 'white',
                'text-outline-width': 4
            })

            .selector('edge.path-hidden-verts')
            .css({
                'line-style': 'dashed',
                content: 'data(label)',
                'font-size': 16 * retina.devicePixelRatio,
                color: 'data(pathColor)',
                'text-outline-color': 'white',
                'text-outline-width': 4
            })

            .selector('edge.path-edge')
            .css({
                'line-color': 'data(pathColor)',
                'target-arrow-color': 'data(pathColor)',
                'source-arrow-color': 'data(pathColor)',
                width: 4 * retina.devicePixelRatio
            })

            .selector('edge.temp')
            .css({
                width: 4,
                'line-color': '#0088cc',
                'line-style': 'dotted',
                'target-arrow-color': '#0088cc'
            });
    }

    function load(previousStyle, styleReady) {
        var style = defaultStyle(previousStyle);

        require(['configuration/plugins/registry'], function(registry) {
            registry.documentExtensionPoint('org.visallo.graph.style',
                'Apply additional cytoscape styles',
                function(e) {
                    return _.isFunction(e);
                },
                'http://docs.visallo.org/extension-points/front-end/graphStyle'
            );
            registry.extensionsForPoint('org.visallo.graph.style').forEach(function(styler) {
                styler(style);
            })
            styleReady(style);
        });
    }

});
