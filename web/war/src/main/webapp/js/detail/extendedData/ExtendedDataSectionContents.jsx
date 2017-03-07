define([
    'react',
    'components/Pager',
    'detail/extendedData/ExtendedDataTable'
], function (React, Pager, ExtendedDataTable) {
    'use strict';

    return React.createClass({
        propTypes: {
            visalloApi: React.PropTypes.object.isRequired,
            elementId: React.PropTypes.string.isRequired,
            elementType: React.PropTypes.string.isRequired,
            table: React.PropTypes.shape({
                tableName: React.PropTypes.string.isRequired,
                displayName: React.PropTypes.string.isRequired,
                columns: React.PropTypes.arrayOf(React.PropTypes.shape({
                    propertyIri: React.PropTypes.string,
                    displayName: React.PropTypes.string
                }))
            }),
            onLoad: React.PropTypes.func.isRequired
        },

        getInitialState() {
            return {
                startIndex: 0,
                pageSize: 10,
                rows: null,
                totalHits: null,
            };
        },

        componentWillMount() {
            this.dataRequest = this.props.visalloApi.v1.dataRequest;
            this.dataRequest('config', 'properties').then((config) => {
                this.setState({
                    startIndex: 0,
                    pageSize: parseInt(config['element.extendedData.maxPerSection'], 10)
                });
                this.loadPage(0);
            });
        },

        handlePagePreviousClick() {
            this.loadPage(this.state.startIndex - this.state.pageSize);
        },

        handlePageNextClick() {
            this.loadPage(this.state.startIndex + this.state.pageSize);
        },

        loadPage(startIndex) {
            this.dataRequest('vertex', 'search', {
                query: '*',
                matchType: 'extended-data',
                otherFilters: {
                    elementExtendedData: {
                        elementType: this.props.elementType,
                        elementId: this.props.elementId,
                        tableName: this.props.table.tableName
                    }
                },
                paging: {
                    offset: startIndex,
                    size: this.state.pageSize
                }
            }).then((result) => {
                this.setState({
                    rows: result.elements,
                    totalHits: result.totalHits,
                    startIndex: startIndex
                });
                this.props.onLoad({
                    badge: result.totalHits,
                    badgeTitle: result.totalHits + (result.totalHits === 1 ? ' row' : ' rows')
                });
            });
        },

        render() {
            return (
                <div>
                    <ExtendedDataTable rows={this.state.rows} columns={this.props.table.columns}/>
                    {this.renderPaging()}
                </div>
            );
        },

        renderPaging() {
            const pageCount = Math.ceil(this.state.totalHits / this.state.pageSize);
            const page = Math.floor(this.state.startIndex / this.state.pageSize) + 1;
            if (this.state.startIndex > pageCount * this.state.pageSize) {
                return null;
            }
            return (<Pager page={page}
                           pageCount={pageCount}
                           onPreviousClick={this.handlePagePreviousClick}
                           onNextClick={this.handlePageNextClick}/>);
        }
    });
});
