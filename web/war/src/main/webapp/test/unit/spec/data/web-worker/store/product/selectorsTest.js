define(['/base/jsc/data/web-worker/store/product/selectors'], function(selectors) {

    describe('productSelectors', () => {
        const productId = 'p1';
        const genState = () => {
            const product = {
                id: productId,
                extendedData: { vertices: [], edges: [] }
            };
            const elements = { vertices: {}, edges: {} };

            return { product, elements }
        };

        it('should get the elementIds in a product', () => {
            let { product, elements } = genState();

            expect(selectors.getElementIdsInProduct.resultFunc(product, elements)).to.deep.equal({ vertices: [], edges: [] });

            product.extendedData.vertices = [{ id: 'v1' }];
            elements.vertices['v1'] = { id: 'v1' };
            expect(selectors.getElementIdsInProduct.resultFunc(product, elements))
                .to.deep.equal({ vertices: [{ id: 'v1' }], edges: [] });

            product.extendedData.edges = [{ edgeId: 'e1' }];
            elements.edges['e1'] = { id: 'e1' };
            expect(selectors.getElementIdsInProduct.resultFunc(product, elements))
                .to.deep.equal({ vertices: [{ id: 'v1' }], edges: [{ edgeId: 'e1'}]});
        })

        it('should filter unauthorized vertexIds', () => {
            let { product, elements } = genState();

            product.extendedData.vertices.push({ id: 'v1', unauthorized: true });
            expect(selectors.getElementIdsInProduct.resultFunc(product, elements))
                .to.deep.equal({ vertices: [], edges: [] });
        })

        it('should get the elements in a product', () =>{
            let { elements } = genState();
            elements.vertices['v1'] = { id: 'v1' };
            elements.edges['e1'] = { id:'e1' };
            const elementIds = { vertices: [], edges: []};

            expect(selectors.getElementsInProduct.resultFunc(elementIds, elements))
                .to.deep.equal({ vertices: {}, edges: {}});

            elementIds.edges = [{ edgeId: 'e1' }];
            expect(selectors.getElementsInProduct.resultFunc(elementIds, elements))
                .to.deep.equal({ vertices: {}, edges: { 'e1': { id: 'e1' }}});

            elementIds.vertices = [{ id: 'v1' }];
            expect(selectors.getElementsInProduct.resultFunc(elementIds, elements))
                .to.deep.equal({ vertices: { 'v1': { id: 'v1' }}, edges: { 'e1': { id: 'e1' }}});
        })

        it('should get the selected elementIds in a product', () => {
            const elementIds = { vertices: [], edges: []};
            const selection = { vertices: [], edges: []}

            expect(selectors.getSelectedElementsInProduct.resultFunc(selection, elementIds))
                .to.deep.equal({ vertices: {}, edges: {}});

            elementIds.vertices = [{ id: 'v1' }];
            expect(selectors.getSelectedElementsInProduct.resultFunc(selection, elementIds))
                .to.deep.equal({ vertices: {}, edges: {}});

            selection.vertices = ['v1'];
            expect(selectors.getSelectedElementsInProduct.resultFunc(selection, elementIds))
                .to.deep.equal({ vertices: {'v1': 'v1'}, edges: {}});

            elementIds.edges = [{ edgeId: 'e1' }];
            selection.edges = ['e1'];
            expect(selectors.getSelectedElementsInProduct.resultFunc(selection, elementIds))
                .to.deep.equal({ vertices: {'v1': 'v1'}, edges: {'e1': 'e1'}});
        })

        it('should get the focused elementIds in a product', () => {
            const elementIds = { vertices: [], edges: []};
            const focusing = { vertexIds: {}, edgeIds: {}}

            expect(selectors.getFocusedElementsInProduct.resultFunc(focusing, elementIds))
                .to.deep.equal({ vertices: {}, edges: {}});

            elementIds.vertices = [{ id: 'v1' }];
            expect(selectors.getFocusedElementsInProduct.resultFunc(focusing, elementIds))
                .to.deep.equal({ vertices: {}, edges: {}});

            focusing.vertexIds = { 'v1': true };
            expect(selectors.getFocusedElementsInProduct.resultFunc(focusing, elementIds))
                .to.deep.equal({ vertices: {'v1': true}, edges: {}});

            focusing.vertexIds = { 'v2': true };
            expect(selectors.getFocusedElementsInProduct.resultFunc(focusing, elementIds))
                .to.deep.equal({ vertices: {}, edges: {}});

            elementIds.edges = [{ edgeId: 'e1' }];
            focusing.edgeIds = { 'e1': true };
            expect(selectors.getFocusedElementsInProduct.resultFunc(focusing, elementIds))
                .to.deep.equal({ vertices: {}, edges: {'e1': true}});

        })

        it('should get the status of the workspace products', () => {
            const productState = { loading: true, loaded: false };
            expect(selectors.getStatus.resultFunc(null)).to.deep.equal({ loading: false, loaded: false });
            expect(selectors.getStatus.resultFunc(productState)).to.deep.equal(productState);
        })

    })
});
