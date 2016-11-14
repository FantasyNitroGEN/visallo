define([], function() {
    const GRAPH_SNAP_TO_GRID = 175;
    const GRAPH_SNAP_TO_GRID_Y = 75;
    const GRAPH_NODE_HEIGHT = 100;

    return snapPosition;

    function snapPosition(p) {
        return {
            x: snapCoordinate(p.x, GRAPH_SNAP_TO_GRID),
            y: snapCoordinate(p.y, GRAPH_SNAP_TO_GRID_Y) + (GRAPH_SNAP_TO_GRID_Y - GRAPH_NODE_HEIGHT) / 2
        }
    }

    function snapCoordinate(value, snap) {
        var rounded = Math.round(value),
            diff = (rounded % snap),
            which = snap / 2;

        if (rounded < 0 && Math.abs(diff) < which) return rounded - diff;
        if (rounded < 0) return rounded - (snap + diff);
        if (diff < which) return rounded - diff;

        return rounded + (snap - diff)
    }

})
