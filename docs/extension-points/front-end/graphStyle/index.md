
Graph Style Plugin
=====================

Plugin to configure the cytoscape stylesheet. http://js.cytoscape.org/#function-format

To register a styler:

        require(['configuration/plugins/registry'], function(registry) {
            registry.registerExtension('org.visallo.graph.style', function(cytoscapeStylesheet) {
                // Changes selected nodes color to red
                cytoscapeStylesheet.selector('node:selected')
                    .css({
                        color: '#FF0000'
                    })
            });
        })

