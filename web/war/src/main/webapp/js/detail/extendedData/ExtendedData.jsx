define([
    'react',
    'detail/extendedData/ExtendedDataSection'
], function (React, ExtendedDataSection) {
    'use strict';

    return React.createClass({
        propTypes: {
            elementId: React.PropTypes.string.isRequired,
            elementType: React.PropTypes.string.isRequired,
            extendedDataTableNames: React.PropTypes.array.isRequired,
            onSearchExtendedData: React.PropTypes.func.isRequired
        },

        getInitialState() {
            return {
                tables: null
            };
        },

        componentWillMount() {
            const dataRequest = this.props.visalloApi.v1.dataRequest;
            dataRequest('ontology', 'properties').then((properties) => {
                const tables = _.map(this.props.extendedDataTableNames, function (tableName) {
                    const table = {
                        tableName: tableName,
                        displayName: tableName
                    };

                    const ontologyProperty = properties.byTitle[tableName];
                    if (ontologyProperty) {
                        table.displayName = ontologyProperty.displayName;
                    }

                    table.columns = ontologyProperty.tablePropertyIris
                        .map((propertyIri) => {
                            const prop = properties.byTitle[propertyIri];
                            if (!prop) {
                                console.error(`Table has property ${propertyIri} but could not be found in the ontology`);
                                return null;
                            }
                            return {
                                propertyIri,
                                displayName: prop.displayName
                            };
                        })
                        .filter(column => column);

                    return table;
                });

                this.setState({
                    tables: tables
                });
            });
        },

        render() {
            if (!this.state.tables) {
                return null;
            }

            return (
                <div>
                    {this.state.tables.map(table => {
                        return (<ExtendedDataSection
                            key={table.tableName}
                            visalloApi={this.props.visalloApi}
                            elementId={this.props.elementId}
                            elementType={this.props.elementType}
                            table={table}
                            onSearchExtendedData={this.props.onSearchExtendedData}
                        />);
                    })}
                </div>
            );
        }
    });
});
