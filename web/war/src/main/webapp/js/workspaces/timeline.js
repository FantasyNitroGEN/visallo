define([
    'flight/lib/component',
    'hbs!./timeline-tpl',
    'util/withDataRequest',
    'util/popovers/withElementScrollingPositionUpdates',
    'require'
], function(
    defineComponent,
    template,
    withDataRequest,
    withElementScrollingPositionUpdates,
    require) {
    'use strict';

    return defineComponent(Timeline, withDataRequest, withElementScrollingPositionUpdates);

    function Timeline() {

        this.defaultAttrs({
            timelineConfigSelector: '.timeline-config',
            timelineFitSelector: '.timeline-fit'
        });

        this.before('teardown', function() {
            if (this.Histogram) {
                this.$node.children('.timeline-svg-container').teardownComponent(this.Histogram);
            }
        })

        this.after('initialize', function() {
            var self = this;

            this.on('updateHistogramExtent', this.onUpdateHistogramExtent);
            this.on('timelineConfigChanged', this.onTimelineConfigChanged);

            this.ontologyPropertiesPromise = new Promise(function(fulfill) {
                self.on('ontologyPropertiesRenderered', function(event, data) {
                    self.foundOntologyProperties = data.ontologyProperties;
                    self.select('timelineConfigSelector').trigger('ontologyPropertiesChanged', {
                        ontologyProperties: self.foundOntologyProperties
                    });
                    fulfill();
                });
            });

            this.on('click', {
                timelineConfigSelector: this.onTimelineConfigToggle,
                timelineFitSelector: this.onFitTimeline
            });

            this.$node.on(TRANSITION_END, _.once(this.render.bind(this)));
        });

        this.onFitTimeline = function() {
            this.$node.children('.timeline-svg-container').trigger('fitHistogram');
        };

        this.onTimelineConfigChanged = function(event, data) {
            this.config = data.config;
            this.$node.children('.timeline-svg-container').trigger('propertyConfigChanged', {
                properties: data.config.properties
            });
        };

        this.onTimelineConfigToggle = function(event) {
            var self = this,
                $target = $(event.target),
                shouldOpen = $target.lookupAllComponents().length === 0;

            require(['./timeline-config'], function(TimelineConfig) {

                self.ontologyPropertiesPromise.done(function() {
                    if (shouldOpen) {
                        TimelineConfig.teardownAll();
                        TimelineConfig.attachTo($target, {
                            config: self.config,
                            ontologyProperties: self.foundOntologyProperties
                        });
                    } else {
                        $target.teardownComponent(TimelineConfig);
                    }
                })
            });
        };

        this.onUpdateHistogramExtent = function(event, data) {
            this.trigger('selectObjects', {
                vertexIds: data.vertexIds,
                edgeIds: data.edgeIds,
                options: {
                    fromHistogram: true
                }
            })
        };

        this.render = function() {
            var self = this;

            this.$node.html(template({}));

            Promise.all([
                Promise.require('fields/histogram/histogram'),
                this.dataRequest('ontology', 'properties'),
            ]).spread(function(Histogram, ontologyProperties) {
                self.Histogram = Histogram;
                Histogram.attachTo(self.$node.children('.timeline-svg-container'), {
                    noDataMessageDetailsText: i18n('timeline.no_data_details'),
                    includeYAxis: true
                });
            })
        }

    }
});
