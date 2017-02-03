import React from 'react'
import renderer from 'react-test-renderer'
import ProductListItem from 'product/ProductListItem'

it('should have a product list', () => {
    const props = {
        editable: false,
        selected: 'a',
        registry: {
            'org.visallo.workproduct': []
        },
        product: {
            id: 'a',
            title: 'a',
            workspaceId: 'a',
            kind: 'g'
        }
    }
    const component = renderer.create(<ProductListItem {...props} />, {
        createNodeMock(element) {
            if (element.type === 'input') {
                return { select: () => {}, focus: () => {} };
            } else if (element.type === 'div') {
                return { addEventListener: () => {} }
            }
            return null;
        }
    })
    expect(component.toJSON()).toMatchSnapshot()
    //const list = shallow(<ProductList {...props} />);
    //expect(list)
})
