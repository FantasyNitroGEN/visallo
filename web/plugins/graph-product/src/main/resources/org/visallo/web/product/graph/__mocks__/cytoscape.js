import _ from 'underscore'

var inst = null;

cytoscape.primeMockInstance = (proto) => inst = proto || {};

export default cytoscape

function cytoscape(cfg) {
    const SETTINGS = 'autounselectify'.split(' ')
    const currentSettings = _.object(SETTINGS.map(s => [s, false]));
    const settingsFns = _.mapObject(currentSettings, (val, name) => {
        return val => val ? currentSettings[name] = val : currentSettings[name]
    })
    const defaultJson = {
        elements: {
            nodes: [],
            edges: []
        }
    }
    const noop = () => {}
    var jsonResponse = defaultJson;
    const api = {
        _private: {
            minZoom: 0, maxZoom: 8
        },
        _setMockJsonResponse(r) {
            jsonResponse = r;
        },
        _createCyCollection(a) {
            a.size = () => a.length
            return a
        },

        // Stubbed methods
        on, renderer, batch, json, nodes, edges, reset,
        
        // Blank
        notify() {},
        trigger() {},

        // Settings functions
        ...settingsFns
    }

    return Object.assign(inst, _.omit(api, Object.keys(inst)));

    function on() { }
    function reset() { jsonResponse = defaultJson }
    function nodes() { return api._createCyCollection(jsonResponse.elements.nodes) }
    function edges() { return api._createCyCollection(jsonResponse.elements.edges) }
    function batch(fn) { fn() }
    function json() { return jsonResponse }
    function renderer() {
        return { getCachedImage() { return {} } }
    }
}
