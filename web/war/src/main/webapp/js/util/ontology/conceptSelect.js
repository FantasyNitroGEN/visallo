/**
 * Allows a user to select an ontology concept from a searchable dropdown component.
 *
 * @module
 * @flight Dropdown selection component for selecting concepts from the ontology
 * @attr {string} [defaultText=Choose a Concept...] the placeholder text to display
 * @attr {boolean} [showAdminConcepts=false] Whether concepts that aren't user visible should be displayed
 * @attr {boolean} [onlySearchable=false] Only show concepts that have searchable attribute equal to true in ontology
 * @attr {string} [restrictConcept=''] Only allow selection of this concept or its descendants
 * @attr {string} [limitRelatedToConceptId=''] Only allow selection of concepts where there is a valid edge containing the passed in concept IRI
 * @attr {string} [selectedConceptId=''] Default the selection to this concept IRI
 * @attr {string} [selectedConceptIntent=''] Default the selection to this the first concept with this intent defined in ontology
 * @attr {boolean} [focus=false] Activate the field for focus when finished rendering
 * @attr {number} [maxItems=-1] Limit the maximum items that are shown in search list (-1 signifies no limit)
 * @fires module:util/ontology/conceptSelect#conceptSelected
 * @listens module:util/ontology/conceptSelect#clearSelectedConcept
 * @listens module:util/ontology/conceptSelect#selectConceptId
 * @listens module:util/ontology/conceptSelect#enableConcept
 * @example <caption>Use default component</caption>
 * ConceptSelect.attachTo(node)
 * @example <caption>Select a concept</caption>
 * ConceptSelect.attachTo(node, {
 *     selectedConceptId: 'http://www.visallo.org/minimal#person'
 * })
 */
define([
    'flight/lib/component',
    './concepts.hbs',
    './concept.hbs',
    'util/withDataRequest',
    'util/requirejs/promise!util/service/ontologyPromise',
    './withSelect'
], function(
    defineComponent,
    template,
    conceptTemplate,
    withDataRequest,
    ontology,
    withSelect) {
    'use strict';

    return defineComponent(ConceptSelector, withDataRequest, withSelect);

    function ConceptSelector() {

        this.defaultAttrs({
            defaultText: i18n('concept.field.placeholder'),
            fieldSelector: 'input',
            showAdminConcepts: false,
            onlySearchable: false,
            restrictConcept: '',
            limitRelatedToConceptId: '',
            maxItems: withSelect.maxItemsFromConfiguration('typeahead.concepts.maxItems')
        });

        this.after('initialize', function() {
            this.$node.html(template(this.attr));

            this.on('click', {
                fieldSelector: this.showTypeahead
            });

            /**
             * Clears the selected concept from the component. Will not fire
             * conceptSelected.
             *
             * @event module:util/ontology/conceptSelect#clearSelectedConcept
             * @example
             * ConceptSelect.attachTo($node)
             * //...
             * $node.trigger('clearSelectedConcept')
             */
            this.on('clearSelectedConcept', this.onClearConcept);

            /**
             * Set the selected concept. Will not fire conceptSelected.
             *
             * If no conceptId is passed or it's empty it'll clear the
             * selection.
             *
             * @event module:util/ontology/conceptSelect#selectConceptId
             * @property {object} data
             * @property {string} [data.conceptId=''] The concept IRI to select
             * @example
             * ConceptSelect.attachTo($node)
             * //...
             * $node.trigger('selectConceptId', {
             *     conceptId: 'http://www.visallo.org/minimal#person'
             * })
             */
            this.on('selectConceptId', this.onSelectConceptId);

            /**
             * Enable / Disable the component. Only pass one property (enable
             * or disable)
             *
             * @event module:util/ontology/conceptSelect#enableConcept
             * @property {object} data
             * @property {boolean} data.enable Enable this component and allow user entry
             * @property {boolean} data.disable Disable this component from user entry
             * @example <caption>Disable</caption>
             * ConceptSelect.attachTo($node)
             * //...
             * $node.trigger('enableConcept', { disable: true })
             * @example <caption>Enable</caption>
             * ConceptSelect.attachTo($node)
             * //...
             * $node.trigger('enableConcept', { enable: true })
             */
            this.on('enableConcept', this.onEnableConcept);
            this.on('change', {
                fieldSelector: this.onChange
            });

            this.setupTypeahead();
        });

        this.onSelectConceptId = function(event, data) {
            var self = this;
            Promise.resolve(
                this.conceptsById ||
                Promise.resolve(ontology.concepts).then(this.transformConcepts.bind(this))
            )
                .then(function() {
                    var concept = data && data.conceptId && self.conceptsById[data.conceptId];
                    self.select('fieldSelector').val(concept && concept.displayName || '');
                })
                .done();
        };

        this.showTypeahead = function() {
            this.select('fieldSelector').typeahead('lookup').select();
        }

        this.onConceptSelected = function(event) {
            var index = event.target.selectedIndex;

            /**
             * Triggered when the user selects a concept from the list.
             *
             * @event module:util/ontology/conceptSelect#conceptSelected
             * @property {object} data
             * @property {object} data.concept The concept object that was selected
             * @example
             * $node.on('conceptSelected', function(event, data) {
             *     console.log(data.concept)
             * })
             * ConceptSelect.attachTo($node)
             */
            this.trigger('conceptSelected', {
                concept: index > 0 ? this.allConcepts[index - 1].rawConcept : null
            });
        };

        this.onClearConcept = function(event) {
            this.select('fieldSelector').val('');
        };

        this.onEnableConcept = function(event, data) {
            if (data.disable || !data.enable) {
                this.select('conceptSelector').attr('disabled', true);
            } else {
                this.select('conceptSelector').removeAttr('disabled');
            }
        };

        this.onChange = function(event, data) {
            const value = $.trim(this.select('fieldSelector').val());
            if (!value) {
                this.trigger('conceptSelected', {
                    concept: null
                });
            }
        };

        this.setupTypeahead = function() {
            var self = this;

            Promise.resolve(ontology.concepts)
                .then(this.transformConcepts.bind(this))
                .done(function(concepts) {
                    concepts.splice(0, 0, self.attr.defaultText);

                    var field = self.select('fieldSelector')
                        .attr('placeholder', self.attr.defaultText),
                        selectedConcept;

                    if (self.attr.selectedConceptId) {
                        selectedConcept = self.conceptsById[self.attr.selectedConceptId];
                    }

                    if (self.attr.selectedConceptIntent) {
                        selectedConcept = _.find(self.allConcepts, function(c) {
                            if (c.rawConcept && c.rawConcept.intents) {
                                return _.contains(
                                    c.rawConcept.intents,
                                    self.attr.selectedConceptIntent
                                );
                            }
                        });
                    }

                    if (selectedConcept) {
                        field.val(selectedConcept.displayName);
                        _.defer(function() {
                            self.trigger('conceptSelected', { concept: selectedConcept });
                        })
                    }

                    field.typeahead({
                        minLength: 0,
                        items: self.attr.maxItems,
                        source: concepts,
                        matcher: function(concept) {
                            if ($.trim(this.query) === '') {
                                return true;
                            }
                            if (concept === self.attr.defaultText) {
                                return false;
                            }

                            return Object.getPrototypeOf(this).matcher.call(this, concept.flattenedDisplayName);
                        },
                        sorter: _.identity,
                        updater: function(conceptId) {
                            var $element = this.$element,
                                concept = self.conceptsById[conceptId];

                            self.trigger('conceptSelected', { concept: concept && concept.rawConcept });
                            return concept && concept.displayName || '';
                        },
                        highlighter: function(concept) {
                            return conceptTemplate(concept === self.attr.defaultText ?
                            {
                                concept: {
                                    displayName: concept,
                                    rawConcept: { }
                                },
                                path: null,
                                marginLeft: 0
                            } : {
                                concept: concept,
                                path: concept.flattenedDisplayName.replace(/\/?[^\/]+$/, ''),
                                marginLeft: concept.depth
                            });
                        }
                    })

                    if (self.attr.focus) {
                        window.focus();
                        _.defer(function() {
                            field.focus().select();
                        })
                    }

                    self.allowEmptyLookup(field);
                });
        }

        this.transformConcepts = function(concepts) {
            var self = this,
                limitRelatedSearch;

            if (this.attr.limitRelatedToConceptId) {
                limitRelatedSearch = this.dataRequest('ontology', 'relationships');
            } else {
                limitRelatedSearch = Promise.resolve();
            }

            return new Promise(function(fulfill, reject) {
                limitRelatedSearch.done(function(r) {
                    self.allConcepts = _.chain(
                            concepts[self.attr.showAdminConcepts ? 'forAdmin' : 'byTitle']
                        )
                        .filter(function(c) {
                            if (c.userVisible === false && self.attr.showAdminConcepts !== true) {
                                return false;
                            }

                            if (self.attr.restrictConcept) {

                                // Walk up tree to see if any match
                                var parentConceptId = c.id,
                                    shouldRestrictConcept = true;
                                do {
                                    if (self.attr.restrictConcept === parentConceptId) {
                                        shouldRestrictConcept = false;
                                        break;
                                    }
                                } while (
                                    parentConceptId &&
                                    (parentConceptId = concepts.byId[parentConceptId].parentConcept)
                                );

                                if (shouldRestrictConcept) {
                                    return false;
                                }
                            }

                            if (self.attr.onlySearchable && c.searchable === false) {
                                return false;
                            }

                            if (self.attr.limitRelatedToConceptId &&
                               r && r.groupedByRelatedConcept &&
                               r.groupedByRelatedConcept[self.attr.limitRelatedToConceptId]) {
                                if (r.groupedByRelatedConcept[self.attr.limitRelatedToConceptId].indexOf(c.id) === -1) {
                                    return false;
                                }
                            }

                            if (self.attr.limitRelatedToConceptId) {
                                var relatedToConcept = concepts.byId[self.attr.limitRelatedToConceptId];
                                if (relatedToConcept &&
                                    relatedToConcept.addRelatedConceptWhiteList &&
                                    relatedToConcept.addRelatedConceptWhiteList.indexOf(c.id) === -1) {
                                    return false;
                                }
                            }

                            return true;
                        })
                        .map(function(c) {
                            return {
                                id: c.id,
                                toString: function() {
                                    return this.id;
                                },
                                displayName: c.displayName,
                                flattenedDisplayName: c.flattenedDisplayName,
                                depth: c.flattenedDisplayName
                                         .replace(/[^\/]/g, '').length,
                                selected: self.attr.selected === c.id,
                                rawConcept: c
                            }
                        })
                        .value();

                    self.conceptsById = _.indexBy(self.allConcepts, 'id');

                    fulfill(self.allConcepts);
                });
            });
        }
    }
});
