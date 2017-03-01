/**
 * Plugins should `require` this module for access to Visallo components and
 * helpers.
 *
 * @module public/v1/api
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
                     * Reference to {@link module:components/JustificationViewer|JustificationViewer}
                     *
                     * @see module:components/JustificationViewer
                     */
                    JustificationViewer: JustificationViewer,

                    /**
                     * Render lists of elements
                     *
                     * Reference to {@link module:components/List|List}
                     *
                     * @see module:components/List
                     */
                    List: List,

                    /**
                     * Concept select dropdown
                     *
                     * Reference to {@link
                     * module:components/ConceptSelect|ConceptSelect}
                     *
                     * @see module:components/ConceptSelect
                     */
                    OntologyConceptSelector: ConceptSelector,

                    /**
                     * Property select dropdown
                     *
                     * Reference to {@link module:components/PropertySelect|PropertySelect}
                     *
                     * @see module:components/PropertySelect
                     */
                    OntologyPropertySelector: PropertySelector,

                    /**
                     * Relationship select dropdown
                     *
                     * Reference to {@link module:components/RelationshipSelect|RelationshipSelect}
                     *
                     * @see module:components/RelationshipSelect
                     */
                    OntologyRelationshipSelector: RelationshipSelector
                },

                /**
                 * Helpful utities for formatting datatypes to user
                 * displayable values
                 *
                 * Reference to {@link module:formatters|Formatters}
                 *
                 * @see module:formatters
                 */
                formatters: F,

                /**
                 * Make service requests on web worker thread
                 *
                 * Reference to {@link module:dataRequest}
                 *
                 * @see module:dataRequest
                 */
                dataRequest: withDataRequest.dataRequest
            };

            return connected
        });
    }
});
