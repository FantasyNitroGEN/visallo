define([
    'flight/lib/component',
    'd3',
    'util/withDataRequest',
    'util/requirejs/promise!util/service/ontologyPromise',
    'hbs!./aggregationTpl'
], function(
    defineComponent,
    d3,
    withDataRequest,
    ontology,
    template) {
    'use strict';

    var AGGREGATIONS = [
            { value: 'term', name: 'Counts' },
            { value: 'histogram', name: 'Histogram', filter: function(properties) {
                return _.filter(properties, function(p) {
                    return p.dataType.toLowerCase() === 'date';
                });
            }},
            { value: 'geohash', name: 'Geo-coordinate Cluster', filter: function(properties) {
                return _.filter(properties, function(p) {
                    return p.dataType.toLowerCase() === 'geolocation';
                });
            }},
            { value: 'statistics', name: 'Statistics' }
        ],
        INTERVAL_UNITS = [
            { value: 1000 * 60, label: 'minutes' },
            { value: 1000 * 60 * 60, label: 'hours' },
            { value: 1000 * 60 * 60 * 24, label: 'days' },
            { value: 1000 * 60 * 60 * 24 * 365, label: 'years' }
        ],
        HISTOGRAM_CALCULATED_BUCKETS = 20,
        PRECISIONS = [
            { value: 1, label: '5000 x 5000 km (large)' },
            { value: 2, label: '1000 x 500 km' },
            { value: 3, label: ' 150 x 150 km' },
            { value: 4, label: '  50 x 25 km' },
            { value: 5, label: '   5 x 5 km', selected: true },
            { value: 6, label: '   1 x 0.5 km' },
            { value: 7, label: ' 150 x 150 meters' },
            { value: 8, label: '  25 x 25 meters (small)' }
        ],
        defaultInterval = 20,
        idIncrement = 1,
        _mapzenPromise;

    return defineComponent(Aggregation, withDataRequest);

    function datumToDisplayName(d) {
        return ontology.properties.byTitle[d.field].displayName;
    }

    function Aggregation() {

        this.attributes({
            aggregationSelector: 'select.aggregation',
            addSelector: '.add-aggregation',
            formSelector: '.form',
            inputsSelector: '.agg .interval, .agg .precision',
            histogramIntervalSelector: '.histogram .interval',
            histogramDateIntervalSelector: '.histogram .date_interval',
            histogramIntervalUnitsSelector: '.histogram .interval_units',
            histogramIntervalValueSelector: '.histogram .interval_value',
            aggregations: []
        })

        this.after('initialize', function() {
            var self = this;

            this.on('change', {
                aggregationSelector: this.onChangeAggregation,
                inputsSelector: this.onChangeInputs,
                histogramDateIntervalSelector: this.onChangeInputs
            })

            this.on('keyup', {
                inputsSelector: this.onKeyup,
                histogramDateIntervalSelector: this.onKeyup
            });

            this.on('click', {
                addSelector: this.onAdd
            })

            this.on('propertyselected', this.onPropertySelected);
            this.on('filterProperties', this.onFilterProperties);

            this.mapzenSupported()
                .then(function(mapzen) {
                    if (!mapzen) {
                        var index = AGGREGATIONS.findIndex(function(a) {
                            return a.value === 'geohash';
                        });
                        AGGREGATIONS.splice(index, 1);
                    }

                    self.aggregations = (self.attr.aggregations || []).map(function addId(a) {
                        if (!a.id) a.id = idIncrement++;
                        if (_.isArray(a.nested)) {
                            a.nested = a.nested.map(addId);
                        }
                        return a;
                    });
                    self.currentAggregation = null;
                    self.updateAggregations(null, true);

                    self.$node.html(template({
                        aggregations: AGGREGATIONS,
                        precisions: PRECISIONS,
                        intervalUnits: INTERVAL_UNITS
                    }));
            });
        });

        this.onChangeInputs = function(event, data) {
            var $target = $(event.target), val, save = true;
            if ($target.is('.precision')) {
                this.currentAggregation.precision = $target.val();
            } else if ($target.is('.interval')) {
                this.currentAggregation.interval = $target.val();
            } else if ($target.is('.interval_value')) {
                val = parseInt($target.val(), 10) || defaultInterval;
                this.currentAggregation.interval = String(val * parseInt(this.select('histogramIntervalUnitsSelector').val(), 10));
            } else if ($target.is('.interval_units')) {
                val = parseInt(this.select('histogramIntervalValueSelector').val(), 10) || defaultInterval;
                this.currentAggregation.interval = String(parseInt($target.val(), 10) * val);
            }
            if (save) {
                this.save();
            }
        };

        this.onKeyup = function(event) {
            if (event.which === 13 && this.currentAggregation && this.currentAggregation.field) {
                this.save();
            }
        };

        this.onAdd = function(event) {
            if (!$(event.target).is(':visible')) return;

            if (_.isEmpty(this.aggregations)) {
                this.add({ type: '' });
            } else {
                this.add({ parentAggregation: this.aggregations[0] });
            }
        };

        this.add = function(aggregationToEdit) {
            if (this.currentAggregation) {
                this.select('addSelector').text('Add');
                this.currentAggregation = null;
                this.select('formSelector').hide();
                this.updateAggregations()
            } else {
                this.currentAggregation = aggregationToEdit;
                this.select('addSelector').text('Cancel').show();
                this.select('formSelector').show();
                var aggregationField = this.select('aggregationSelector');
                if (aggregationToEdit.parentAggregation) {
                    aggregationField.prop('disabled', true).val('term');
                } else {
                    aggregationField.prop('disabled', false);
                }
                this.updateAggregationDependents(this.currentAggregation.type || aggregationField.val());
            }

        };

        this.replaceObjectInListWithObject = function(list, newObject) {
            var existing = newObject.id && _.findWhere(list, { id: newObject.id }),
                replaced = false,
                index = existing && _.indexOf(list, existing);

            if (index >= 0) {
                list.splice(index, 1, newObject);
                replaced = true;
            }

            if (!replaced) {
                list.push(newObject);
            }
        };

        this.save = function() {
            var parentAggregation = this.currentAggregation.parentAggregation;
            if (parentAggregation) {
                parentAggregation.nested = parentAggregation.nested || [];
                this.currentAggregation.name = this.currentAggregation.field;
                this.replaceObjectInListWithObject(parentAggregation.nested, this.currentAggregation);
            } else {
                this.replaceObjectInListWithObject(this.aggregations, this.currentAggregation);
            }

            if (!this.currentAggregation.id) {
                this.currentAggregation.id = idIncrement++;
            }
            if (this.currentAggregation.type === 'histogram') {
                this.currentAggregation.minDocumentCount = 0;
            }
            this.currentAggregation = null;
            this.select('formSelector').hide();
            this.select('addSelector').text('Add');
            this.updateAggregations();
        }

        this.onPropertySelected = function(event, data) {
            var self = this;

            this.currentAggregation.field = data.property.title;
            this.currentAggregation.name = 'field';
            if (this.currentAggregation.type === 'histogram') {
                var $interval = this.select('histogramIntervalSelector'),
                    $intervalUnits = this.select('histogramIntervalUnitsSelector'),
                    $intervalValue = this.select('histogramIntervalValueSelector'),
                    $dateInterval = this.select('histogramDateIntervalSelector');

                this.loadStatsForAggregation(this.currentAggregation)
                    .done(function(stats) {
                        var range = stats.max - stats.min,
                            buckets = range / HISTOGRAM_CALCULATED_BUCKETS,
                            ontologyProperty = ontology.properties.byTitle[stats.field],
                            isDate = ontologyProperty && ontologyProperty.dataType === 'date',
                            interval = Math.round(buckets);

                        if (isDate) {
                            var minuteInterval = INTERVAL_UNITS[0].value;
                            self.currentAggregation.isDate = true;
                            interval = interval < minuteInterval ? minuteInterval : interval;
                            $intervalUnits.val(minuteInterval);
                        }
                        $interval.val(interval);
                        $interval.toggle(!isDate);
                        $dateInterval.toggle(isDate);

                        self.currentAggregation.interval = String(interval);
                        self.save();
                    });
            } else {
                this.save();
            }
        };

        this.loadStatsForAggregation = function(aggregation) {
            var self = this;

            return new Promise(function(f, r) {
                self.off('aggregationStatistics');
                self.on('aggregationStatistics', function(event, data) {
                    self.off('aggregationStatistics');
                    if (data.success && data.statistics.field === aggregation.field) {
                        f(data.statistics);
                    } else {
                        r()
                    }
                })
                self.trigger('statisticsForAggregation', {
                    aggregation: aggregation
                });
            });
        };

        this.onChangeAggregation = function(event) {
            var aggregation = $(event.target).val();
            this.updateAggregationDependents(aggregation);
        };

        this.editAggregation = function(aggregation) {
            this.currentAggregation = null;
            this.add(aggregation);
        };

        this.updateAggregations = function(d3) {
            var self = this;

            if (!d3) {
                var args = _.toArray(arguments);
                return require(['d3'], function(d3) {
                    args.splice(0, 1, d3);
                    self.updateAggregations.apply(self, args);
                });
            }

            d3.select(this.$node.find('ul.aggregations')[0])
                .selectAll('li')
                .data(this.aggregations)
                .call(function() {
                    this.enter().append('li').style({ 'flex-wrap': 'wrap', display: 'flex'})
                        .call(function() {
                            this.append('span')
                                .style({flex: 1, cursor: 'pointer'})
                                .on('click', self.editAggregation.bind(self));
                            this.append('button').attr('class', 'remove-icon')
                                .on('click', function() {
                                    self.aggregations.splice($(d3.event.target).closest('li').index(), 1);
                                    _.defer(function() {
                                        self.updateAggregations(d3);
                                    })
                                })
                            this.append('ul').style({
                                flex: '1 0 100%',
                                margin: 0,
                                padding: 0
                            })
                        })
                    this.exit().remove();
                    this.select('span').text(datumToDisplayName)
                })
                .select('ul')
                .selectAll('li')
                .data(function(d) {
                    return d.nested || [];
                })
                .call(function() {
                    this.enter().append('li').style('display', 'flex')
                        .call(function() {
                            this.append('span')
                                .style({ flex: 1, 'padding-left': '2em', cursor: 'pointer'})
                                .on('click', function(aggregation) {
                                    var parentAggregation = self.aggregations[$(d3.event.target).closest('li').parent().closest('li').index()];
                                    aggregation.parentAggregation = parentAggregation;
                                    self.editAggregation(aggregation);
                                });
                            this.append('button').attr('class', 'remove-icon')
                                .on('click', function() {
                                    var nestedLi = $(d3.event.target).closest('li'),
                                        li = nestedLi.parent().closest('li'),
                                        nestedIndex = nestedLi.index(),
                                        index = li.index();

                                    self.aggregations[index].nested.splice(nestedIndex, 1);
                                    _.defer(function() {
                                        self.updateAggregations(d3);
                                    })
                                })
                        });
                    this.exit().remove();
                    this.select('span').text(datumToDisplayName)
                })

            this.select('addSelector').toggle(this.aggregations.length === 0 || (
                _.isEmpty(this.aggregations[0].nested) && _.contains(['term'/*, 'histogram'*/], this.aggregations[0].type)
            ));

            this.trigger('aggregationsUpdated', {
                aggregations: this.aggregations.map(function(a) {
                    if (_.isArray(a.nested)) {
                        var clone = _.extend({}, a);
                        a.nested = a.nested.map(function(c) {
                            return _.omit(c, 'parentAggregation');
                        });
                    }
                    return a;
                })
            })
        };

        this.updateAggregationDependents = function(type) {
            var section = this.$node.find('.' + type).show(),
                others = section.siblings('div').hide(),
                aggregation = this.currentAggregation,
                placeholder;

            this.currentAggregation.type = type;

            switch (type) {
                case 'geohash':
                    if (!aggregation.precision) {
                        aggregation.precision = '5';
                    }

                    section.find('.precision').val(aggregation.precision);
                    placeholder = i18n('dashboard.search.aggregation.geohash.property.placeholder');
                    break;

                case 'histogram':
                    if (!aggregation.interval) {
                        aggregation.interval = String(defaultInterval);
                    }

                    var ontologyProperty = ontology.properties.byTitle[this.currentAggregation.field],
                        isDate = !!ontologyProperty && ontologyProperty.dataType === 'date',
                        $interval = section.find('.interval').toggle(!isDate);

                    section.find('.date_interval').toggle(isDate);
                    $interval.val(aggregation.interval);
                    if (isDate) {
                        var interval = parseInt(aggregation.interval, 10),
                            intervalUnitIndex = -1;

                        for (var i = 0; i < INTERVAL_UNITS.length; i++) {
                            if (interval < INTERVAL_UNITS[i].value) {
                                intervalUnitIndex = i;
                                break;
                            }
                        }
                        var intervalUnit = intervalUnitIndex === -1 ?
                            _.last(INTERVAL_UNITS) :
                            INTERVAL_UNITS[Math.max(0, intervalUnitIndex - 1)];

                        section.find('.interval_value').val(Math.round(interval / intervalUnit.value));
                        section.find('.interval_units').val(intervalUnit.value);
                    }
                    placeholder = i18n('dashboard.search.aggregation.histogram.property.placeholder');
                    break;

                case 'term':
                    break;

                case 'statistics':
                    break;

                default:
                    console.warn('No aggregation of type', aggregation);
            }

            this.select('aggregationSelector').val(type);
            this.attachPropertySelection(section.find('.property-select'), {
                selected: aggregation && aggregation.field,
                placeholder: placeholder || i18n('dashboard.savedsearches.aggregation.property.placeholder')
            });
        };

        this.onFilterProperties = function(event, data) {
            if ($(event.target).is('.property-select')) return;

            if (!data || _.isEmpty(data.properties)) {
                this.filteredProperties = null;
            } else {
                this.filteredProperties = data.properties;
            }

            this.$node.find('.property-select').trigger(event.type, {
                properties: this.filteredProperties
            });
        };

        this.attachPropertySelection = function(node, options) {
            var self = this;
            if (!options) {
                options = {};
            }
            return Promise.all([
                this.dataRequest('ontology', 'properties'),
                Promise.require('util/ontology/propertySelect')
            ]).spread(function(properties, FieldSelection) {
                var propertiesToFilter = self.filteredProperties || properties.list;

                node.teardownComponent(FieldSelection);

                FieldSelection.attachTo(node, {
                    selectedProperty: options.selected && properties.byTitle[options.selected] || null,
                    properties: self.filterProperties(propertiesToFilter),
                    showAdminProperties: true,
                    placeholder: options.placeholder || ''
                });
            });
        };

        this.filterProperties = function(properties) {
            var self = this;
            var aggregation = _.find(AGGREGATIONS, function(a) {
                return a.value === self.currentAggregation.type;
            });
            var filteredProperties = _.reject(properties, function(p) {
                var isUserVisible = p.title === 'http://visallo.org#conceptType' || p.userVisible,
                    isPropString = p.dataType === 'string';
                if (isPropString) {
                    var isSearchable = p.textIndexHints !== undefined ? p.textIndexHints.length > 0 : false;
                    return !isSearchable || !isUserVisible;
                }
                return !isUserVisible;
            });

            return _.isFunction(aggregation.filter) ? aggregation.filter(filteredProperties) : filteredProperties;
        };

        this.mapzenSupported = function() {
            if (!_mapzenPromise) {
                _mapzenPromise = new Promise(function(f) {
                    d3.json('mapzen/osm/all/0/0/0.json', function(error, json) {
                        f(!error);
                    });
                });
            }
            return _mapzenPromise;
        };

    }
});
