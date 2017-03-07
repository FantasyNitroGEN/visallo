define([
    'react',
    'util/vertex/formatters'
], function (React, F) {
    'use strict';

    const ROW_PROP_TYPE = React.PropTypes.shape({
        id: React.PropTypes.shape({
            tableName: React.PropTypes.string,
            elementType: React.PropTypes.string,
            elementId: React.PropTypes.string,
            rowId: React.PropTypes.string
        }),
        properties: React.PropTypes.arrayOf(React.PropTypes.shape({
            name: React.PropTypes.string,
            value: React.PropTypes.any
        }))
    });

    const COLUMNS_PROP_TYPE = React.PropTypes.arrayOf(React.PropTypes.shape({
        propertyIri: React.PropTypes.string,
        displayName: React.PropTypes.string
    }));

    const Header = React.createClass({
        propTypes: {
            columns: COLUMNS_PROP_TYPE
        },

        render() {
            return (<thead>
            <tr>
                {this.props.columns.map((column) => {
                    return (<th key={column.propertyIri}>{column.displayName}</th>);
                })}
            </tr>
            </thead>)
        }
    });

    const Row = React.createClass({
        propTypes: {
            columns: COLUMNS_PROP_TYPE,
            row: ROW_PROP_TYPE
        },

        getPropertyValue(propertyIri) {
            return F.vertex.prop(this.props.row, propertyIri);
        },

        render() {
            return (<tr>
                {this.props.columns.map((column) => {
                    return (<td key={column.propertyIri}>{this.getPropertyValue(column.propertyIri)}</td>);
                })}
            </tr>);
        }
    });

    return React.createClass({
        propTypes: {
            columns: COLUMNS_PROP_TYPE,
            rows: React.PropTypes.arrayOf(ROW_PROP_TYPE)
        },

        getInitialState() {
            return {};
        },

        render() {
            if (!this.props.rows || !this.props.columns) {
                return (<div>Loading...</div>);
            }

            return (
                <table className="table">
                    <Header columns={this.props.columns}/>
                    <tbody>
                    {this.props.rows.map((row) => {
                        return (<Row key={row.id.rowId} columns={this.props.columns} row={row}/>);
                    })}
                    </tbody>
                </table>
            );
        }
    });
});
