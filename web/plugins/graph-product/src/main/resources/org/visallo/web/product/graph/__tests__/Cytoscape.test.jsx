jest.mock('cytoscape')

import React from 'react'
import Cytoscape from '../Cytoscape'
import renderer from 'react-test-renderer'
import cytoscape from 'cytoscape'

const DEFAULT_NODE = {
    group: 'nodes',
    classes: '',
    data: { id: '' },
    position: { x: 0, y: 0 },
    grabbable: undefined,
    locked: undefined,
    renderedPosition: undefined,
    selectable: undefined,
    selected: false
};

var cy;
beforeEach(() => cy = cytoscape.primeMockInstance())

it('Should start with no cyNodes', () => {
    const component = render()
    expect(component.toJSON()).toMatchSnapshot()
    expect(cy.json().elements.nodes.length).toBe(0)
})

it('Should trigger preview after starting', () => {
    cy.png = jest.fn()
    const preview = jest.fn()
    const component = render({ hasPreview: false, initialProductDisplay: true, onUpdatePreview: preview })
    expect(preview).toHaveBeenCalled()
    expect(cy.png).toHaveBeenCalled()
})

it('Should not preview after starting if already has preview', () => {
    cy.png = jest.fn()
    const preview = jest.fn()
    const component = render({ hasPreview: true, initialProductDisplay: true, onUpdatePreview: preview })
    expect(preview).not.toHaveBeenCalled()
    expect(cy.png).not.toHaveBeenCalled()

    component.update(comp({ hasPreview: true, initialProductDisplay: false, onUpdatePreview: preview }))
    expect(preview).not.toHaveBeenCalled()
})

it('Should fit when nodes are added without position', () => {
    cy.trigger = jest.fn()
    cy.add = jest.fn()
    cy.png = jest.fn()
    cy.nodes = jest.fn()
    cy.style = jest.fn(() => ({ containerCss: () => 1000 }))
    const node = { ...DEFAULT_NODE, data: { id: 'id0' } }

    cy.add.mockReturnValue([node])
    cy.nodes.mockReturnValue({
        size: () => 1,
        boundingBox: () => ({ x1: 0, x2: 10, y1: 0, y2: 10, w: 10, h: 10 })
    })
    
    const component = render({ initialProductDisplay: false, elements: { nodes: [node] } })
    expect(cy.png).toHaveBeenCalledTimes(1)
    expect(cy.add).toHaveBeenCalledWith(node)
    expect(cy.trigger).toHaveBeenCalledWith('pan zoom viewport')
})

it('Should not fit when nodes are added with position', () => {
    cy.trigger = jest.fn()
    cy.add = jest.fn()
    cy.png = jest.fn()
    cy.nodes = jest.fn()
    cy.style = jest.fn(() => ({ containerCss: () => 1000 }))
    const node = { ...DEFAULT_NODE, data: { id: 'id0' }, position: { x: 0, y: 1 } }

    cy.add.mockReturnValue([node])
    cy.nodes.mockReturnValue({
        size: () => 1,
        boundingBox: () => ({ x1: 0, x2: 10, y1: 0, y2: 10, w: 10, h: 10 })
    })
    
    const component = render({ initialProductDisplay: false, elements: { nodes: [node] } })
    expect(cy.png).toHaveBeenCalledTimes(1)
    expect(cy.add).toHaveBeenCalledWith(node)
    expect(cy.trigger).not.toHaveBeenCalled()
})

it('should cleanup cytoscape instance on unmount', () => {
    cy.destroy = jest.fn()
    const component = render()
    component.unmount()
    expect(cy.destroy).toHaveBeenCalled()
})


function render(props = {}) {
    const options = {
        createNodeMock(element) {
            if (element.type === 'div') {
                return { getBoundingClientRect: () => ({ left: 0, right: 0, width: 1000, height: 1000 }) };
            }
            return null;
        }
    };
    const component = renderer.create(comp(props), options)
    return component
}

function comp(props = {}) {
    return <Cytoscape {...props} onCollapseSelectedNodes={() => {}}_disablePreviewDelay={true} />
}
