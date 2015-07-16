define([
    'flight/lib/component',
    'hbs!./relationships',
    'hbs!./relationship',
    'util/withDataRequest',
    './withSelect'
], function(
    defineComponent,
    template,
    relationshipTemplate,
    withDataRequest,
    withSelect) {
    'use strict';

    return defineComponent(RelationshipSelector, withDataRequest, withSelect);

    function RelationshipSelector() {

        this.defaultAttrs({
            defaultText: i18n('relationship.field.placeholder'),
            fieldSelector: 'input',
            limitParentConceptId: ''
        });

        this.after('initialize', function() {
            this.$node.html(template(this.attr));

            this.on('click', {
                fieldSelector: this.showTypeahead
            });
            this.on('limitParentConceptId', this.onLimitParentConceptId);

            this.setupTypeahead();
        });

        this.showTypeahead = function() {
            this.select('fieldSelector').typeahead('lookup').select();
        };

        this.onLimitParentConceptId = function(event, data) {
            this.attr.limitParentConceptId = data.conceptId;
            this.transformRelationships();
        };

        this.setupTypeahead = function() {
            var self = this;

            this.dataRequest('ontology', 'ontology')
                .then(function(ontology) {
                    self.ontology = ontology;
                    if (self.attr.sourceConcept && self.attr.targetConcept) {
                        return self.dataRequest('ontology', 'relationshipsBetween',
                            self.attr.sourceConcept,
                            self.attr.targetConcept
                        );
                    }
                })
                .done(function(limitedToSourceDest) {

                    if (limitedToSourceDest) {
                        self.limitedToSourceDest = limitedToSourceDest;
                    }

                    var ontology = self.ontology,
                        ontologyConcepts = ontology.concepts,
                        relationshipOntology = ontology.relationships,
                        transformed = self.transformRelationships(),
                        placeholderForRelationships = function() {
                            return transformed.length ?
                                self.attr.defaultText :
                                i18n('relationship.field.no_valid');
                        },
                        isPlaceholder = function(placeholder) {
                            return placeholder === self.attr.defaultText ||
                               placeholder === i18n('relationship.field.no_valid');
                        },
                        placeholder = placeholderForRelationships(transformed),
                        field = self.select('fieldSelector').attr('placeholder', placeholder);

                    field.typeahead({
                        minLength: 0,
                        items: Number.MAX_VALUE,
                        source: function(query) {
                            var relationships = self.transformRelationships(),
                                placeholder = placeholderForRelationships(relationships);

                            relationships.splice(0, 0, placeholder);

                            return relationships;
                        },
                        matcher: function(relationship) {
                            if ($.trim(this.query) === '') {
                                return true;
                            }
                            if (isPlaceholder(relationship)) {
                                return false;
                            }

                            return Object.getPrototypeOf(this).matcher.call(this, relationship.displayName);
                        },
                        sorter: _.identity,
                        updater: function(relationshipTitle) {
                            var $element = this.$element,
                                relationship = relationshipOntology.byTitle[relationshipTitle];

                            self.currentRelationshipTitle = relationship && relationship.title;
                            self.trigger('relationshipSelected', {
                                relationship: relationship
                            });
                            return relationship && relationship.displayName || '';
                        },
                        highlighter: function(relationship) {
                            return relationshipTemplate(isPlaceholder(relationship) ?
                            {
                                relationship: {
                                    displayName: relationship
                                }
                            } : {
                                relationship: relationship
                            });
                        }
                    })

                    if (self.attr.focus) {
                        _.defer(function() {
                            field.focus();
                        })
                    }

                    self.allowEmptyLookup(field);
                    self.trigger('rendered');
                });
        }

        this.transformRelationships = function() {
            var self = this,
                list = this.limitedToSourceDest || this.ontology.relationships.list;

            if (this.attr.limitParentConceptId) {
                list = _.chain(this.ontology.relationships.groupedBySourceDestConcepts)
                    .map(function(r, key) {
                        return ~key.indexOf(self.attr.limitParentConceptId) ? r : undefined;
                    })
                    .compact()
                    .flatten(true)
                    .unique(_.property('title'))
                    .value();
            }

            var previousSelectionFound = false,
                transformed = _.chain(list)
                    .sortBy('displayName')
                    .reject(function(r) {
                        return r.userVisible === false;
                    })
                    .map(function(r) {
                        if (r.title === self.currentRelationshipTitle) {
                            previousSelectionFound = true;
                        }
                        return _.extend({}, r, {
                            toString: function() {
                                return r.title;
                            }
                        })
                    })
                    .value();

            if (this.currentRelationshipTitle && !previousSelectionFound) {
                this.currentRelationshipTitle = null;
                this.select('fieldSelector').val('');
                this.trigger('relationshipSelected', {
                    relationship: null
                });
            }

            return transformed;
        }
    }
});
