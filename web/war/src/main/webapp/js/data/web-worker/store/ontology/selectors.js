define(['reselect'], function(reselect) {
    const { createSelector } = reselect;

    const _visible = concept => concept.userVisible !== false &&
        concept.id !== 'http://www.w3.org/2002/07/owl#Thing' &&
        concept.id !== 'http://visallo.org#root';
    const _collectParents = concepts => concept => {
        const parents = [];
        _collect(concept);
        const path = parents.reverse();
        return {
            ...concept,
            path: '/' + path.concat([concept.displayName]).join('/'),
            depth: path.length,
            glyphIconHref: findParentProperty(concept, 'glyphIconHref')
        };

        function findParentProperty(concept, name) {
            if (concept[name]) return concept[name];
            if (concept.parentConcept) {
                return findParentProperty(concepts[concept.parentConcept], name);
            }
            return null;
        }

        function _collect(concept) {
            if (_visible(concept) && concept.parentConcept) {
                const parentConcept = concepts[concept.parentConcept];
                if (_visible(parentConcept)) {
                    parents.push(parentConcept.displayName)
                    _collect(parentConcept);
                }
            }
        }
    }

    const getWorkspace = (state) => state.workspace.currentId;

    const getOntologyRoot = (state) => state.ontology;

    const getConcepts = createSelector([getWorkspace, getOntologyRoot], (workspaceId, ontology) => {
        return ontology[workspaceId].concepts;
    })

    const getVisibleConcepts = createSelector([getConcepts], concepts => {
        return _.chain(concepts)
            .map()
            .filter(_visible)
            .map(_collectParents(concepts))
            .sortBy('displayName')
            .value()
    })

    const getVisibleConceptsWithHeaders = createSelector([getVisibleConcepts], concepts => {
        return _.chain(concepts)
            .sortBy('path')
            //.tap(x => {
                //console.log(x.map(c => c.fullPath))
            //})
            //.sortBy(c => c.pathComponents.length)
            //.sortBy(c => {
                //c.displayName
            //})
            //.tap(c => {
                //debugger
            //})
            .value()
    })


    return {
        getConcepts,
        getVisibleConcepts,
        getVisibleConceptsWithHeaders
    }
});
