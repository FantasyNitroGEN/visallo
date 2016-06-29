define([
    'react',
    'gridlist',
    'jsx!./DashboardCard'
], function(
    React,
    GridList,
    DashboardCard) {
    'use strict';

    const CSSTransitionGroup = React.addons.CSSTransitionGroup;
    const PropTypes = React.PropTypes;
    const ZeroDeltaPosition = [0, 0];
    const NumberOfHeightLanes = 6;
    const ResizeTypes = ['w', 'h', 'both'];

    /**
     * Handles the absolute placement and grid layout of cards. Including the
     * interaction to reorder and resize.
     */
    const DashboardContent = React.createClass({
        propTypes: {
            items: PropTypes.array.isRequired,
            height: PropTypes.number.isRequired,
            onItemChanged: PropTypes.func
        },
        getDefaultProps() {
            return {
                lanes: NumberOfHeightLanes
            };
        },
        getInitialState() {
            var grid = transformItems(this.props.items, this.props.lanes)
            return {
                lanes: this.props.lanes,
                deltaPosition: ZeroDeltaPosition,
                dragging: null,
                resizing: null,
                cellSize: Math.floor(this.props.height / this.props.lanes),
                gridOptions: grid._options,
                items: grid.items
            }
        },
        componentWillUnmount() {
            this.handlerForItem.cache = {};
        },
        componentWillMount() {
            this.handlerForItem = _.memoize(this.handlerForItem, function(item, type) {
                return item.id + type;
            });
        },
        componentWillReceiveProps: function(props) {
            var grid = transformItems(props.items, props.lanes),
                cellSize = Math.floor(props.height / props.lanes);
            this.setState({
                cellSize: cellSize,
                gridOptions: grid._options,
                items: grid.items,
                initial: Boolean(!this.state.initial && grid.items.length)
            });
        },
        renderPlaceholder() {
            return (
                <li
                    key='placeholder'
                    style={this.calculateStyleForPlaceholder()}
                    className='placeholder'>
                    <div className='card-inner'></div>
                </li>
            );
        },
        renderItems() {
            var self = this,
                mutating = this.state.dragging || this.state.resizing,
                onDragDown = this.onDragDown,
                onResizeDown = this.onResizeDown,
                items = this.state.cellSize > 0 ? this.state.items : [];

            return items.map(function(item) {
                return (
                    <li key={item.id}
                        style={self.calculateStyleForItem(item, self.state.deltaPosition)}
                        className={mutating && mutating.id === item.id ? 'no-animate' : ''}
                        onMouseDown={self.handlerForItem(item, 'drag')}>

                        <DashboardCard item={item} />

                        {ResizeTypes.map(function(type) {
                            return <div key={type} onMouseDown={self.handlerForItem(item, type)} className={`h-${type}`}></div>
                        })}
                    </li>
                )
            });
        },
        render() {

            return (
                /*Need to test performance of this
                 <CSSTransitionGroup
                    component="ul"
                    className="dashboard-items"
                    transitionName="card"
                    transitionEnter={!this.state.initial}
                    transitionEnterTimeout={200}
                    transitionLeaveTimeout={200}>*/
                <ul className="dashboard-items">
                        {this.renderPlaceholder()}
                        {this.renderItems()}
                </ul>
                /*</CSSTransitionGroup>*/
            )
        },
        calculateStyleForPlaceholder(items) {
            var item = this.state.placeholderItem;
            if (item) {
                return this.calculateStyleForItem(item, ZeroDeltaPosition, { placeholder: true });
            }
            return { display: 'none' };
        },
        snapItemPositionToGrid(item) {
            var dim = this.calculateDimensionsForItem(item, this.state.deltaPosition),
                cellSize = this.state.cellSize,
                col = Math.round(dim.left / cellSize),
                row = Math.round(dim.top / cellSize);

            return [
                Math.max(0, col),
                Math.max(0, Math.min(this.props.lanes - item.h, row))
            ];
        },
        snapItemSizeToGrid(item) {
            var dim = this.calculateDimensionsForItem(item, this.state.deltaPosition),
                cellSize = this.state.cellSize,
                width = Math.round(dim.width / cellSize),
                height = Math.min(this.props.lanes, Math.round(dim.height / cellSize));
            return { w: width, h: height };
        },
        calculateDimensionsForItem(item, delta) {
            var isDragging = this.state.dragging && this.state.dragging.id === item.id,
                isResizing = this.state.resizing && this.state.resizing.id === item.id,
                cellSize = this.state.cellSize,
                deltaPosition = isDragging ? delta : ZeroDeltaPosition,
                deltaSize = isResizing ? applyResizeType(delta, this.state.resizingType) : ZeroDeltaPosition;
            return {
                left: item.x * cellSize + deltaPosition[0],
                top: item.y * cellSize + deltaPosition[1],
                width: cellSize * item.w + deltaSize[0],
                height: cellSize * item.h + deltaSize[1],
                isMutating: isDragging || isResizing
            };
        },
        calculateStyleForItem(item, delta, options) {
            var dimensions = this.calculateDimensionsForItem(item, delta),
                style = {
                    transform: `translate3d(${dimensions.left}px,${dimensions.top}px,0)`,
                    width: dimensions.width,
                    height: dimensions.height
                };

            if (dimensions.isMutating && (!options || !options.placeholder)) {
                style.zIndex = 2;
            }
            return style;
        },
        handlerForItem(item, type) {
            // Reuse bound handlers for performance
            var self = this, handler;
            if (type === 'drag') {
                handler = function(event) {
                    self.onDragDown(_.findWhere(self.state.items, { id: item.id }), event);
                };
            } else {
                handler = function(event) {
                    self.onResizeDown(type, _.findWhere(self.state.items, { id: item.id }), event);
                };
            }
            return handler;
        },
        onDragDown(item, event) {
            event.stopPropagation();
            event.preventDefault();
            this.setState({ dragging: item, startPosition: [event.pageX, event.pageY] })
            this.setupMutations();
        },
        onDragUp() {
            var update = {
                placeholderItem: null,
                dragging: null,
                resizing: null,
                resizingType: null,
                deltaPosition: ZeroDeltaPosition,
                startPosition: null
            };
            if (this.state.placeholderItem) {
                var items = this.state.items.concat([]),
                    index = _.findIndex(items, { id: this.state.placeholderItem.id });
                items.splice(index, 1, this.state.placeholderItem);
                update.items = stabilize(items);
                if (this.props.onItemChanged) {
                    this.props.onItemChanged(this.state.placeholderItem);
                }
            }
            this.setState(update);
            this.previousGridPosition = this.previousGridSize = null;
            window.removeEventListener('mousemove', this.onDragMove);
            window.removeEventListener('mouseup', this.onDragUp);
        },
        onDragMove(event) {
            var s = this.state.startPosition,
                item = this.state.dragging || this.state.resizing,
                x = event.pageX,
                y = event.pageY,
                gridPosition = null,
                gridSize = null;

            if (s && item) {
                if (this.state.dragging) {
                    gridPosition = this.snapItemPositionToGrid(item);
                } else {
                    gridSize = this.snapItemSizeToGrid(item);
                }

                var changed = (gridPosition && !_.isEqual(gridPosition, this.previousGridPosition)) ||
                    (gridSize && !_.isEqual(gridSize, this.previousGridSize)),
                    stateUpdate = { deltaPosition: [x - s[0], y - s[1]] };

                if (changed) {
                    var tmpGrid = new GridList(GridList.cloneItems(this.clonedItems), this.state.gridOptions),
                        clonedItem = _.find(tmpGrid.items, function(i) {
                            return i.id === item.id;
                        });

                    if (gridPosition) {
                        this.previousGridPosition = gridPosition;
                        tmpGrid.moveItemToPosition(clonedItem, gridPosition);
                    } else {
                        this.previousGridSize = gridSize;
                        tmpGrid.resizeItem(clonedItem, gridSize);
                    }

                    // Replace new position with currently dragging item so it
                    // doesn't snap into new position while still dragging
                    var index = _.findIndex(tmpGrid.items, { id: clonedItem.id });
                    tmpGrid.items.splice(index, 1, item);

                    stateUpdate.placeholderItem = clonedItem;
                    stateUpdate.items = stabilize(tmpGrid.items);
                }
                this.setState(stateUpdate);
            }
        },
        onResizeDown(type, item, event) {
            event.stopPropagation();
            event.preventDefault();
            this.setupMutations();
            this.setState({ resizing: item, resizingType: type, startPosition: [event.pageX, event.pageY] })
        },
        setupMutations() {
            this.clonedItems = GridList.cloneItems(this.state.items);
            window.addEventListener('mousemove', this.onDragMove);
            window.addEventListener('mouseup', this.onDragUp);
        }
    });

    return DashboardContent;

    function applyResizeType(size, type) {
        if (type === 'w') {
            return [size[0], 0];
        } else if (type === 'h') {
            return [0, size[1]];
        }
        return size;
    }

    function transformItems(items, lanes) {
        var _items = items.map(function(item) {
                var m = item.configuration.metrics,
                    w = Math.max(1, m.width),
                    h = Math.max(1, Math.min(lanes, m.height));
                return { id: item.id, x: m.x, y: m.y, w: w, h: h, item: item };
            }),
            grid = new GridList(_items, {
                direction: 'horizontal',
                lanes: lanes
            });
        grid.resizeGrid(lanes);
        grid.items = stabilize(_items)
        return grid;
    }

    function stabilize(items) {
        return _.sortBy(items, 'id');
    }
});
