import React from 'react'
import Graph from '../Graph'
import renderer from 'react-test-renderer'
import ReactTestUtils from 'react-dom/lib/ReactTestUtils'
import bluebird from 'bluebird'
import updeep from 'updeep'

it('Should only call extension functions on vertex changes', () => {
    let counters = {}
    const increment = (name, id) => {
        if (!counters[name]) {
            counters[name] = {}
        }
        if (!counters[name][id]) {
            counters[name][id] = 1
        } else {
            counters[name][id]++
        }
    }
    const count = (name, returnVal) => el => {
        if (_.isArray(el)) {
            increment(name, el[0].label);
        } else if (el.edges) {
            increment(name, el.type);
        } else {
            increment(name, el.id);
        }
        return returnVal;
    }
    const getCount = (name, elementId) => counters[name] && counters[name][elementId] || 0
    const assertCounts = (expected, numbers = [1, 2]) => {
        Object.keys(expected).forEach(id => {
            expect(expected[id] >= 0).toBe(true);

            numbers.forEach(num => {
                if (/^v/.test(id)) {
                    expect(getCount('applyTo-'+num, id)).toBe(expected[id])
                    expect(getCount('class-'+num, id)).toBe(expected[id])
                    expect(getCount('data-'+num, id)).toBe(expected[id])
                    expect(getCount('transform-'+num, id)).toBe(expected[id])
                } else {
                    expect(expected[id] >= 0).toBe(true)
                    expect(getCount('edgeClass-'+num, id)).toBe(expected[id])
                    expect(getCount('edgeTransform-'+num, id)).toBe(expected[id])
                }
            })
        })
    }
    const renderer = ReactTestUtils.createRenderer()
    let props = {
        workspace: { editable: true },
        uiPreferences: { },
        product: { extendedData: { vertices: {}, edges: {e1: {}, e2: {}}, compoundNodes: {} } },
        productElementIds: {
            vertices: {
                v1: { id:'v1', parent: 'root', type: 'vertex', pos: {x:0,y:0} },
                v2: { id:'v2', parent: 'root', type: 'vertex', pos: {x:0,y:0} }
            },
            edges: {
                e1: { edgeId:'e1', label: 'e1Label', inVertexId: 'v2', outVertexId: 'v1'  },
                e2: { edgeId:'e2', label: 'e2Label', inVertexId: 'v2', outVertexId: 'v1'  },
                e3: { edgeId:'e3', label: 'e2Label', inVertexId: 'v2', outVertexId: 'v1'  }
            }
        },
        ontology: { relationships: { } },
        elements: {
            vertices: {
                'v1': { id: 'v1', properties: [] },
                'v2': { id: 'v2', properties: [] }
            }, edges: {
                'e1': { id: 'e1', label: 'e1Label', properties: [] },
                'e2': { id: 'e2', label: 'e2Label', properties: [] },
                'e3': { id: 'e3', label: 'e2Label', properties: [] }
            }
        },
        selection: { vertices: {}, edges: {} },
        focusing: { vertices: {}, edges: {} },
        registry: {
            'org.visallo.graph.view': [],
            'org.visallo.graph.style': [],
            'org.visallo.graph.options': [],
            'org.visallo.graph.edge.class': [
                count('edgeClass-1', ''),
                count('edgeClass-2', '')
            ],
            'org.visallo.graph.edge.transformer': [
                count('edgeTransform-1', {}),
                count('edgeTransform-2', {})
            ],
            'org.visallo.graph.node.class': [
                count('class-1', ''), count('class-2', '')
            ],
            'org.visallo.graph.node.transformer': [
                count('transform-1', {}), count('transform-2', {})
            ],
            'org.visallo.graph.node.decoration': [
                { applyTo: count('applyTo-1', true), data: count('data-1', {}) },
                { applyTo: count('applyTo-2', true), data: count('data-2', {}) }
            ]
        },
        onUpdatePreview: () => {}
    }

    // Initial Render all called once
    renderer.render(<Graph {...props} />)
    assertCounts({ v1: 1, v2: 1, e1Label: 1, e2Label: 1 })

    // Change some other prop to re-render, but no functions should have been called
    renderer.render(<Graph {...props} panelPadding='new' />)
    assertCounts({ v1: 1, v2: 1, e1Label: 1, e2Label: 1 })

    // Change v2, e2 and re-render, only v2, e2 should update
    const changeElements = {
        vertices: {
            v1: props.elements.vertices.v1,
            v2: { id: 'v2', properties: [] }
        },
        edges: {
            e1: props.elements.edges.e1,
            e2: { id: 'e2', label: 'e2Label', properties: [] },
            e3: props.elements.edges.e3
        }
    };
    renderer.render(<Graph {...props} elements={changeElements} />)
    assertCounts({ v1: 1, v2: 2, e1Label: 1, e2Label: 2 })


    const add = item => {
        return (list) => {
            return list.concat([item])
        }
    };
    const changeRegistry = updeep({
        'org.visallo.graph.edge.class': add(count('edgeClass-3', '')),
        'org.visallo.graph.edge.transformer': add(count('edgeTransform-3', {})),
        'org.visallo.graph.node.class': add(count('class-3', '')),
        'org.visallo.graph.node.transformer': add(count('transform-3', {})),
        'org.visallo.graph.node.decoration': add({ applyTo: count('applyTo-3', true), data: count('data-3', {}) })
    }, props.registry)
    renderer.render(<Graph {...props} elements={changeElements} registry={changeRegistry} />)
    assertCounts({ v1: 2, v2: 3, e1Label: 2, e2Label: 3 })
    assertCounts({ v1: 1, v2: 1, e1Label: 1, e2Label: 1 }, [3])

    renderer.render(<Graph {...props} elements={changeElements} registry={changeRegistry} />)
    assertCounts({ v1: 2, v2: 3, e1Label: 2, e2Label: 3 })
    assertCounts({ v1: 1, v2: 1, e1Label: 1, e2Label: 1 }, [3])
})
