import React from 'react'
import renderer from 'react-test-renderer'
import ProductList from 'product/ProductList'

it('should have a product list', () => {
    const props = {
        products: [],
        onCreate: () => {},
        onLoadProducts: () => {},
        types: [],
        registry: {},
        workspace: { editable: false },
        user: { privileges: [] },
        status: { loading: false, loaded: false }
    }
    const component = renderer.create(<ProductList {...props} />, {})
    expect(component.toJSON()).toMatchSnapshot()
})
