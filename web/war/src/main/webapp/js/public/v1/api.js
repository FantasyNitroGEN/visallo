/**
 * Plugins should `require` this module for access to Visallo components and
 * helpers.
 *
 * @module
 * @classdesc Visallo Top-Level API
 * @example
 * require(['public/v1/api'], function(visalloApi) {
 *     // ...
 * })
 */
define([
    'flight/lib/component',
    'configuration/plugins/registry',
    'public/connectReact'
], function(
    defineComponent,
    registry,
    connectReact) {
    'use strict';

    /**
     * @alias module:public/v1/api
     */
    return {

        /**
         * Connect to secondary dependencies
         *
         * @function
         * @returns {Promise.<(module:public/v1/api.connected)>} The connected objects
         */
        connect: connect,

        /**
         * {@link module:public/connectReact|Higher-order component}
         * for React that automcatically loads `connect`.
         *
         * @see module:public/connectReact
         */
        connectReact: connectReact,

        /**
         * Reference to Flight `defineComponent`
         *
         * @function
         * @deprecated React is now the preferred component model.
         * @example
         * // Creating react component
         * define(['react'], function(React) {
         *  return React.createClass({
         *      render() {
         *          return <h1>Hello</h1>
         *      }
         *  })
         * })
         */
        defineComponent: defineComponent,

        /**
         * {@link module:registry|Extension Registry}
         * component allows plugins to configure extension points.
         * @see module:registry
         */
        registry: registry
    };

    function connect() {
        return Promise.all([
            'util/element/list',
            'util/ontology/conceptSelect',
            'util/ontology/propertySelect',
            'util/ontology/relationshipSelect',
            'util/vertex/formatters',
            'util/vertex/justification/viewer',
            'util/visibility/edit',
            'util/visibility/view',
            'util/withDataRequest'
        ].map(function(module) {
            return Promise.require(module);
        })).spread(function(
            List,
            ConceptSelector,
            PropertySelector,
            RelationshipSelector,
            F,
            JustificationViewer,
            VisibilityEditor,
            VisibilityViewer,
            withDataRequest) {

            /**
             * Visallo Second-Level API
             *
             * @alias module:public/v1/api.connected
             * @namespace
             * @example
             * require(['public/v1/api'], function(api) {
             *     api.then(function(connected) {
             *         // ...
             *     })
             * })
             */
            var connected = {

                /**
                 * Shared user interface components
                 *
                 * @namespace
                 */
                components: {

                    /**
                     * Display justification values
                     *
                     * Reference to {@link module:util/vertex/justification/viewer|JustificationViewer}
                     *
                     * @see module:util/vertex/justification/viewer
                     */
                    JustificationViewer: JustificationViewer,

                    /**
                     * Render lists of elements
                     *
                     * Reference to {@link module:util/element/list|ElementList}
                     *
                     * @see module:util/element/list
                     */
                    List: List,

                    /**
                     * Concept select dropdown
                     *
                     * Reference to {@link module:util/ontology/conceptSelect|ConceptSelect}
                     *
                     * @see module:util/ontology/conceptSelect
                     */
                    OntologyConceptSelector: ConceptSelector,

                    /**
                     * Property select dropdown
                     *
                     * Reference to {@link module:util/ontology/propertySelect|PropertySelect}
                     *
                     * @see module:util/ontology/propertySelect
                     */
                    OntologyPropertySelector: PropertySelector,

                    /**
                     * Relationship select dropdown
                     *
                     * Reference to {@link module:util/ontology/relationshipSelect|RelationshipSelect}
                     *
                     * @see module:util/ontology/relationshipSelect
                     */
                    OntologyRelationshipSelector: RelationshipSelector
                },

                /**
                 * Helpful utities for formatting datatypes to user
                 * displayable values
                 *
                 * Reference to {@link module:util/vertex/formatters|Formatters}
                 *
                 * @see module:util/vertex/formatters
                 */
                formatters: F,

                /**
                 * Make service requests on web worker thread
                 *
                 * Reference to {@link module:util/withDataRequest|DataRequest}
                 *
                 * @see module:util/withDataRequest
                 */
                dataRequest: withDataRequest.dataRequest
            };

            return connected
        });
    }
});
