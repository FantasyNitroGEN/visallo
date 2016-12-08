define([
    'react',
    'react-virtualized',
    'react-resizable',
    './SelectableRowRenderer'
], function(
    React,
    ReactVirtualized,
    ReactResizable,
    SelectableRowRenderer) {
    'use strict';

    const { AutoSizer, InfiniteLoader, FlexTable, FlexColumn } = ReactVirtualized;
    const { Resizable } = ReactResizable;

    const PAGE_SIZE = 25,
        HEADER_HEIGHT = 25,
        ROW_HEIGHT = 35,
        CONFIGURE_COLUMN_WIDTH = 35,
        HEADER_COLUMN_MARGIN = 10,
        HEADER_COLUMN_BORDER = 1,
        SCROLLBAR_WIDTH = 8;

    const Table = React.createClass({
        propTypes: {
            data: React.PropTypes.array.isRequired,
            columns: React.PropTypes.array.isRequired,
            selected: React.PropTypes.array,
            sort: React.PropTypes.object,
            showRowNumbers: React.PropTypes.bool,
            loadMoreRows: React.PropTypes.func,
            onHeaderClick: React.PropTypes.func,
            onRowClick: React.PropTypes.func,
            onColumnResize: React.PropTypes.func,
            onConfigureClick: React.PropTypes.func
        },

        componentWillUpdate(nextProps) {
            if (nextProps.showRowNumbers !== this.props.showRowNumbers) {
                this._FlexTable.forceUpdateGrid();
            } else {
                this._FlexTable.recomputeRowHeights();
            }
        },

        render() {
            const {
                data,
                columns,
                selected,
                sort,
                showRowNumbers,
                scrollToIndex,
                onRowsRendered,
                onHeaderClick,
                onRowClick,
                onColumnResize,
                onConfigureColumnsClick } = this.props;
            const rowCount = data.length;
            const tableWidth = columns.reduce((memo, { width, visible }) => {
                return visible ?
                    (memo + width + HEADER_COLUMN_MARGIN + HEADER_COLUMN_BORDER) : memo
                }, (HEADER_COLUMN_MARGIN + SCROLLBAR_WIDTH)) + CONFIGURE_COLUMN_WIDTH;

            return (
                <div className="table">
                    <InfiniteLoader
                        loadMoreRows={({ startIndex, stopIndex }) => onRowsRendered(startIndex, stopIndex)}
                        isRowLoaded={({ index }) => !!data[index]}
                        rowCount={rowCount}
                        minimumBatchSize={PAGE_SIZE}

                    >
                        {({ onRowsRendered, registerChild }) => (
                            <AutoSizer disableWidth={true}>
                                {({ height }) => (
                                    <FlexTable
                                         overscanRowCount={0}
                                         width={tableWidth}
                                         height={height - 10}
                                         headerHeight={HEADER_HEIGHT}
                                         headerStyle={{marginRight: HEADER_COLUMN_MARGIN, borderRight: `${HEADER_COLUMN_BORDER}px solid white`}}
                                         rowClassName={({ index }) => !data[index] || data[index] === 'loading' ? 'loading' : ''}
                                         rowHeight={({ index }) => data[index] && data[index].height || ROW_HEIGHT}
                                         rowCount={rowCount}
                                         rowGetter={({ index }) => data[index] || {}}
                                         rowRenderer={(args) => SelectableRowRenderer(args, selected)}
                                         scrollToIndex={scrollToIndex}
                                         onRowsRendered={onRowsRendered}
                                         ref={(ref) => {
                                            this._FlexTable = ref;
                                            registerChild(ref);
                                         }}
                                         sort={({ sortBy }) => {onHeaderClick(sortBy) }}
                                    >
                                        <FlexColumn
                                           label="Columns"
                                           dataKey="index"
                                           disableSort={true}
                                           width={CONFIGURE_COLUMN_WIDTH}
                                           flexGrow={0}
                                           flexShrink={0}
                                           key="configureColumns"
                                           headerRenderer={() => (
                                               <div
                                                   className="configure-column-header"
                                                   onClick={(event) => onConfigureColumnsClick(event)}
                                               ></div>
                                           )}
                                           style={{height: '100%'}}
                                           cellRenderer={({ cellData }) =>
                                               indexCellRenderer(cellData, showRowNumbers)
                                           }
                                        />
                                        {columns.map(({ displayName, title, visible, width: columnWidth }) => {
                                            if (visible) {
                                                return (
                                                    <FlexColumn
                                                       label={displayName}
                                                       dataKey={title}
                                                       width={columnWidth}
                                                       flexGrow={0}
                                                       flexShrink={0}
                                                       key={title}
                                                       headerRenderer={(opts) => resizableHeaderRenderer({
                                                              ...opts,
                                                              sort: sort,
                                                              headerWidth: columnWidth,
                                                              onHeaderResize:onColumnResize
                                                       })}
                                                       cellRenderer={(args) => cellRenderer(args, columnWidth)}
                                                       style={{height: '100%'}}
                                                    />
                                                );
                                            }
                                        })}

                                    </FlexTable>
                                )}
                            </AutoSizer>
                        )}
                    </InfiniteLoader>
                </div>
            );
        }
    });

    return Table;

    function resizableHeaderRenderer({ dataKey, label, sort, headerWidth, onHeaderResize }) {
        const sortBy = sort.property === dataKey ? ' sort-' + sort.direction : '';

        return (
            <Resizable
                className="resizable-column-header"
                width={headerWidth}
                height={HEADER_HEIGHT}
                onResize={(event, { size }) => columnResize(size, false)}
                onResizeStop={(event, { size }) => columnResize(size, true)}
                onClick={(event) => {
                    if (event.target.className === 'react-resizable-handle') {
                        event.stopPropagation()
                    }
                }}
            >
                <div>
                    <span className={'FlexTable__headerTruncatedText' + sortBy} title={label}>{label}</span>
                </div>
            </Resizable>
        );

        function columnResize(size, shouldSave) {
            const { width } = size;
            onHeaderResize(dataKey, width, shouldSave);
        }
    }

    function cellRenderer({ dataKey, cellData }, width) {
        let i = 0;
        return (cellData ?
            cellData.map((data) => (
                <div
                    key={`${dataKey}(${i++}):${data}`}
                    className="property-value"
                    style={{width: width}}
                    dangerouslySetInnerHTML={{__html: data.outerHTML}}
                ></div>
            )) : ' '
        );
    }

    function indexCellRenderer(cellData, showRowNumbers) {
        return (
            <p
                className="property-value config-row-column"
                style={{width: CONFIGURE_COLUMN_WIDTH}}
            >
                {showRowNumbers ? cellData : null}
            </p>
        );
    }
});
