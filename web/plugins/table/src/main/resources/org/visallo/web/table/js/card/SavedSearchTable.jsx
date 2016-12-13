define([
    'react',
    '../table/Tabs',
    '../table/Table'
], function(
    React,
    Tabs,
    Table) {
    'use strict';

    const SavedSearchTable = ({ tabs, activeTab, onTabClick, ...tableProps }) => {
        return (
            <div className="saved-search-table">
                <Tabs
                    tabs={tabs}
                    activeTab={activeTab}
                    onTabClick={onTabClick}
                />
                <Table
                    activeTab={activeTab}
                    {...tableProps}
                />
            </div>
        );
    };

    SavedSearchTable.propTypes = {
        data: React.PropTypes.array.isRequired,
        columns: React.PropTypes.array.isRequired,
        tabs: React.PropTypes.object.isRequired,
        activeTab: React.PropTypes.string,
        sort: React.PropTypes.object,
        selected: React.PropTypes.array,
        showRowNumbers: React.PropTypes.bool,
        onRowsRendered: React.PropTypes.func,
        onTabClick: React.PropTypes.func,
        onHeaderClick: React.PropTypes.func,
        onRowClick: React.PropTypes.func,
        onColumnResize: React.PropTypes.func,
        onConfigureColumnsClick: React.PropTypes.func
    };

    return SavedSearchTable;
});
