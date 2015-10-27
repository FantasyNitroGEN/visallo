define('data/web-worker/services/io-visallo-web-devTools', [
    'data/web-worker/util/ajax'
], function(ajax) {
    return {
          ontologyEditConcept: function(options) {
              return ajax('POST->HTML', '/org/visallo/web/devTools/saveOntologyConcept', options);
          },

          ontologyEditProperty: function(options) {
              return ajax('POST->HTML', '/org/visallo/web/devTools/saveOntologyProperty', options);
          },

          ontologyEditRelationship: function(options) {
            return ajax('POST->HTML', '/org/visallo/web/devTools/saveOntologyRelationship', options);
          }

    }
});
