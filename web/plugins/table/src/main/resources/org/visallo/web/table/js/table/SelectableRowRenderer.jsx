/*forked from https://github.com/bvaughn/react-virtualized/blob/master/source/FlexTable/defaultRowRenderer.js */
define([
    'react'
], function(React) {
    'use strict';

    return SelectableRowRenderer;

    function SelectableRowRenderer({
      className,
      columns,
      index,
      isScrolling,
      onRowClick,
      onRowDoubleClick,
      onRowMouseOver,
      onRowMouseOut,
      rowData,
      style
    }, selected) {
        const a11yProps = {}

        if (
          onRowClick ||
          onRowDoubleClick ||
          onRowMouseOver ||
          onRowMouseOut
        ) {
          a11yProps['aria-label'] = 'row'
          a11yProps.role = 'row'
          a11yProps.tabIndex = 0

          if (onRowClick) {
            a11yProps.onClick = (event) => onRowClick(event, { index })
          }
          if (onRowDoubleClick) {
            a11yProps.onDoubleClick = () => onRowDoubleClick({ index })
          }
          if (onRowMouseOut) {
            a11yProps.onMouseOut = () => onRowMouseOut({ index })
          }
          if (onRowMouseOver) {
            a11yProps.onMouseOver = () => onRowMouseOver({ index })
          }
        }

        if (rowData && rowData.id) {
            const isSelected = selected.indexOf(rowData.id) > -1;

            if (isSelected) {
                className += ' selected';
            }
        }

        return (
            <div
                {...a11yProps}
                className={className}
                style={style}
                data-row-index={index}
            >
                {columns}
            </div>
        );
    }
});
