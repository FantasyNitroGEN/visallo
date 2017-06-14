/**
 * Allows a user to select an ontology relationship from a searchable dropdown component.
 *
 * @module components/RelationshipSelect
 * @flight Dropdown selection component for selecting relationships from the ontology
 * @attr {string} [defaultText=Choose a Relationship...] the placeholder text to display
 * @attr {string} [limitParentConceptId=''] Limit edges that contain this concept IRI on one side of the edge
 * @attr {string} [sourceConcept=''] Limit relationships to those that have this concept IRI as the source
 * @attr {string} [targetConcept=''] Limit relationships to those that have this concept IRI as the target
 * @attr {boolean} [focus=false] Activate the field for focus when finished rendering
 * @attr {number} [maxItems=-1] Limit the maximum items that are shown in search list (-1 signifies no limit)
 * @fires module:components/RelationshipSelect#relationshipSelected
 * @listens module:components/RelationshipSelect#limitParentConceptId
 * @listens module:components/RelationshipSelect#selectRelationshipId
 * @example
 * RelationshipSelect.attachTo(node)
 */
define([
    'flight/lib/component',
    'util/component/attacher'
], function(defineComponent, attacher) {

    return defineComponent(RelationshipSelector);

    function RelationshipSelector() {
        this.after('teardown', function() {
            this.attacher.teardown();
        })

        this.after('initialize', function() {
            if (this.attr.maxItems) console.warn('maxItems is no longer supported');
            var self = this;
            this.on('limitParentConceptId', function(event, data) {
                const { conceptId: concept, sourceConceptId: sourceConcept, targetConceptId: targetConcept } = data;
                self.attacher.params({ concept, sourceConcept, targetConcept }).attach();
            })
            this.on('selectRelationshipId', function(event, data) {
                const relationshipId = data && data.relationshipId || '';
                self.attacher.params({ ...self.attacher._params, value: relationshipId }).attach()
            })

            this.attacher = attacher()
                .node(this.node)
                .params({
                    placeholder: this.attr.defaultText,
                    autofocus: this.attr.focus === true,
                    concept: this.attr.limitParentConceptId,
                    sourceConcept: this.attr.sourceConcept,
                    targetConcept: this.attr.targetConcept
                })
                .behavior({
                    onSelected: (attacher, relationship) => {
                        this.trigger('relationshipSelected', { relationship })
                    }
                })
                .path('components/ontology/RelationshipSelector')

            this.attacher.attach();
        })
    }
});
