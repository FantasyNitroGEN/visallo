define([], function() {

    // For performace switch to non-bezier edges after this many
    const MaxEdgesBeforeHayStackOptimization = 250;

    const GENERIC_SIZE = 30;
    const CUSTOM_IMAGE_SIZE = 50;
    const IMAGE_ASPECT_RATIO = 4 / 3;
    const VIDEO_ASPECT_RATIO = 19 / 9;

    return function({ pixelRatio, edgesCount, edgeLabels, styleExtensions }) {

        return getDefaultStyles().concat(getExtensionStyles());

        function getExtensionStyles() {
            // Mock the cytoscape style fn api to the json style
            const collector = styleCollector();
            styleExtensions.forEach(fn => fn(collector.mock));
            return collector.styles;

            function styleCollector() {
                const styles = {};
                var currentSelector;
                const add = (obj) => {
                    if (!currentSelector) throw new Error('No selector found for style: ' + obj)
                    styles[currentSelector] = obj;
                    return api.mock;
                }
                const api = {
                    mock: {
                        selector: (str) => {
                            currentSelector = str;
                            return api.mock;
                        },
                        style: add,
                        css: add
                    },
                    get styles() {
                        return _.map(styles, (style, selector) => ({ selector, style }))
                    }
                }
                return api;
            }
        }

        function getDefaultStyles() {
            return [
                {
                    selector: 'core',
                    css: {
                        'outside-texture-bg-color': '#efefef'
                    }
                },
                {
                    selector: 'node',
                    css: {
                        'background-color': '#ccc',
                        'background-fit': 'contain',
                        'border-color': 'white',
                        'background-image-crossorigin': 'use-credentials',
                        'font-family': 'helvetica',
                        'font-size': 18 * pixelRatio,
                        'min-zoomed-font-size': 4,
                        'text-events': 'yes',
                        'text-outline-color': 'white',
                        'text-outline-width': 2,
                        'text-halign': 'center',
                        'text-valign': 'bottom',
                        content: 'Loading…',
                        opacity: 1,
                        color: '#999',
                        height: GENERIC_SIZE * pixelRatio,
                        shape: 'roundrectangle',
                        width: GENERIC_SIZE * pixelRatio
                    }
                },
                {
                    selector: 'node.drawEdgeToMouse',
                    css: {
                        'background-opacity': 0,
                        'text-events': 'no',
                        width: GENERIC_SIZE * pixelRatio,
                        height: GENERIC_SIZE * pixelRatio,
                        shape: 'ellipse',
                        content: '',
                        events: 'no'
                    }
                },
                {
                    selector: 'node.v',
                    css: {
                        'background-color': '#fff',
                        'background-image': 'data(imageSrc)',
                        content: 'data(truncatedTitle)',
                    }
                },
                {
                    selector: 'node:selected',
                    css: {
                        'background-color': '#0088cc',
                        'border-color': '#0088cc',
                        'border-width': 2 * pixelRatio,
                        color: '#0088cc'
                    }
                },
                {
                    selector: 'node[selectedImageSrc]:selected',
                    css: {
                        'background-image': 'data(selectedImageSrc)'
                    }
                },
                {
                    selector: 'node.decorationParent',
                    css: {
                        'background-image': 'none',
                        'background-color': 'transparent',
                        'background-opacity': 0,
                        'compound-sizing-wrt-labels': 'exclude',
                        content: ''
                    }
                },
                {
                    selector: 'node.decorationParent:active',
                    css: {
                        'background-color': 'transparent',
                        'background-opacity': 0,
                        'overlay-color': 'none',
                        'overlay-padding': 0,
                        'overlay-opacity': 0,
                        'border-width': 0
                    }
                },
                {
                    selector: 'node.decoration',
                    css: {
                        'background-color': '#F89406',
                        'background-image': 'none',
                        'border-width': 2,
                        'border-style': 'solid',
                        'border-color': '#EF8E06',
                        'text-halign': 'center',
                        'text-valign': 'center',
                        'font-size': 20,
                        color: 'white',
                        'text-outline-color': 'none',
                        'text-outline-width': 0,
                        content: 'data(label)',
                        events: 'no',
                        shape: 'roundrectangle',
                        'padding-left': 5,
                        'padding-right': 5,
                        'padding-top': 3,
                        'padding-bottom': 3,
                        width: 'label',
                        height: 'label'
                    }
                },
                {
                    selector: 'node.decoration.hidden',
                    css: {
                        display: 'none'
                    }
                },
                {
                    selector: 'node.video',
                    css: {
                        shape: 'rectangle',
                        width: (CUSTOM_IMAGE_SIZE * pixelRatio) * VIDEO_ASPECT_RATIO,
                        height: (CUSTOM_IMAGE_SIZE * pixelRatio) / VIDEO_ASPECT_RATIO
                    }
                },
                {
                    selector: 'node.image',
                    css: {
                        shape: 'rectangle',
                        width: (CUSTOM_IMAGE_SIZE * pixelRatio) * IMAGE_ASPECT_RATIO,
                        height: (CUSTOM_IMAGE_SIZE * pixelRatio) / IMAGE_ASPECT_RATIO
                    }
                },
                {
                    selector: 'node.hasCustomGlyph',
                    css: {
                        width: CUSTOM_IMAGE_SIZE * pixelRatio,
                        height: CUSTOM_IMAGE_SIZE * pixelRatio
                    }
                },
                {
                    selector: 'node.hover',
                    css: {
                        opacity: 0.6
                    }
                },
                {
                    selector: 'node.focus',
                    css: {
                        color: '#00547e',
                        'font-weight': 'bold',
                        'overlay-color': '#a5e1ff',
                        'overlay-padding': 10 * pixelRatio,
                        'overlay-opacity': 0.5
                    }
                },
                {
                    selector: 'edge.focus',
                    css: {
                        'overlay-color': '#a5e1ff',
                        'overlay-padding': 7 * pixelRatio,
                        'overlay-opacity': 0.5
                    }
                },
                {
                    selector: 'node.temp',
                    css: {
                        'background-color': 'rgba(255,255,255,0.0)',
                        'background-image': 'none',
                        width: '1',
                        height: '1'
                    }
                },
                {
                    selector: 'node.controlDragSelection',
                    css: {
                        'border-width': 5 * pixelRatio,
                        'border-color': '#a5e1ff'
                    }
                },
                {
                    selector: 'edge:selected',
                    css: {
                        'line-color': '#0088cc',
                        color: '#0088cc',
                        'target-arrow-color': '#0088cc',
                        'source-arrow-color': '#0088cc',
                        width: 4 * pixelRatio
                    }
                },
                {
                    selector: 'edge',
                    css: {
                        'font-size': 11 * pixelRatio,
                        'target-arrow-shape': 'triangle',
                        color: '#aaa',
                        content: edgeLabels !== 'false' ? 'data(label)' : '',
                        'curve-style': edgesCount > MaxEdgesBeforeHayStackOptimization ? 'haystack' : 'bezier',
                        'min-zoomed-font-size': 3,
                        'text-outline-color': 'white',
                        'text-outline-width': 2,
                        width: 2.5 * pixelRatio
                    }
                },
                {
                    selector: 'edge.label',
                    css: {
                        content: 'data(label)',
                        'font-size': 12 * pixelRatio,
                        color: '#0088cc',
                        'text-outline-color': 'white',
                        'text-outline-width': 4
                    }
                },
                {
                    selector: 'edge.drawEdgeToMouse',
                    css: {
                        events: 'no',
                        width: 4,
                        'line-color': '#0088cc',
                        'line-style': 'dotted',
                        'target-arrow-color': '#0088cc'
                    }
                },
                {
                    selector: 'edge.path-hidden-verts',
                    css: {
                        'line-style': 'dashed',
                        content: 'data(label)',
                        'font-size': 16 * pixelRatio,
                        color: 'data(pathColor)',
                        'text-outline-color': 'white',
                        'text-outline-width': 4
                    }
                },
                {
                    selector: 'edge.path-edge',
                    css: {
                        'line-color': 'data(pathColor)',
                        'target-arrow-color': 'data(pathColor)',
                        'source-arrow-color': 'data(pathColor)',
                        width: 4 * pixelRatio
                    }
                },
                {
                    selector: 'edge.temp',
                    css: {
                        width: 4,
                        'line-color': '#0088cc',
                        'line-style': 'dotted',
                        'target-arrow-color': '#0088cc'
                    }
                }
            ];
        }
    }
})
