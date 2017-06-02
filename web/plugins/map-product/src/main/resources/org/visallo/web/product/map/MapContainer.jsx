define([
    'react',
    'react-redux',
    'react-dom',
    'data/web-worker/store/selection/actions',
    'data/web-worker/store/product/actions',
    'data/web-worker/store/product/selectors',
    'util/dnd',
    './worker/actions',
    'components/DroppableHOC',
    './Map'
], function(React, redux, ReactDom, selectionActions, productActions, productSelectors, dnd, mapActions, DroppableHOC, Map) {
    'use strict';

    const mimeTypes = [VISALLO_MIMETYPES.ELEMENTS];

    return redux.connect(

        (state, props) => {
            var ontologyProperties = state.ontology.properties,
                configProperties = state.configuration.properties,
                pixelRatio = state.screen.pixelRatio;

            return {
                ...props,
                configProperties,
                ontologyProperties,
                panelPadding: state.panel.padding,
                selection: productSelectors.getSelectedElementsInProduct(state),
                viewport: productSelectors.getViewport(state),
                productElementIds: productSelectors.getElementIdsInProduct(state),
                elements: productSelectors.getElementsInProduct(state),
                pixelRatio,
                mimeTypes,
                style: { height: '100%' }
            }
        },

        (dispatch, props) => {
            return {
                onClearSelection: () => dispatch(selectionActions.clear()),
                onSelectElements: (selection) => dispatch(selectionActions.set(selection)),
                onSelectAll: (id) => dispatch(productActions.selectAll(id)),

                onUpdatePreview: (id, dataUrl) => dispatch(productActions.updatePreview(id, dataUrl)),

                // TODO: these should be mapActions
                onUpdateViewport: (id, { pan, zoom }) => dispatch(productActions.updateViewport(id, { pan, zoom })),

                // For DroppableHOC
                onDrop: (event) => {
                    const elements = dnd.getElementsFromDataTransfer(event.dataTransfer);
                    if (elements) {
                        event.preventDefault();
                        event.stopPropagation();

                        dispatch(mapActions.dropElements(props.product.id, elements, { undoable: true }))
                    }
                },

                onDropElementIds(elementIds) {
                    dispatch(mapActions.dropElements(props.product.id, elements, { undoable: true }));
                },

                onRemoveElementIds: (elementIds) => {
                    dispatch(mapActions.removeElements(props.product.id, elements, { undoable: true }))
                },

                onVertexMenu: (element, vertexId, position) => {
                    $(element).trigger('showVertexContextMenu', { vertexId, position });
                }
            }
        }

    )(DroppableHOC(Map));
});
