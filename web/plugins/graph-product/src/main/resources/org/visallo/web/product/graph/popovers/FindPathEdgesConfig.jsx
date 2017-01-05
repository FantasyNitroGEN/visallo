define([
    'react'
], function(React) {
    'use strict';

    const FindPathEdgesConfig = React.createClass({
        propTypes: {
            availableEdges: React.PropTypes.arrayOf(React.PropTypes.any).isRequired,
            selectedEdgeIris: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
            defaultSelectedEdgeIris: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
            onCancel: React.PropTypes.func.isRequired,
            onLimit: React.PropTypes.func.isRequired
        },

        getInitialState() {
            return {
                selectedEdgeIris: [],
                filter: ''
            };
        },

        handleCancel() {
            this.props.onCancel();
        },

        handleLimitClick() {
            this.props.onLimit(this.state.selectedEdgeIris);
        },

        componentWillMount() {
            this.setState({
                selectedEdgeIris: this.props.selectedEdgeIris
            });
        },

        handleEdgeChecked(edge, event) {
            const checked = event.target.checked;
            if (checked && this.state.selectedEdgeIris.indexOf(edge.title) < 0) {
                this.setState({
                    selectedEdgeIris: this.state.selectedEdgeIris.concat([edge.title])
                });
            } else if (!checked) {
                this.setState({
                    selectedEdgeIris: this.state.selectedEdgeIris.filter((iri) => {
                        return iri !== edge.title;
                    })
                });
            }
        },

        handleSelectAllClicked() {
            this.setState({
                selectedEdgeIris: this.props.availableEdges.map((e) => {
                    return e.title;
                })
            });
        },

        handleSelectNoneClicked() {
            this.setState({
                selectedEdgeIris: []
            });
        },

        handleSelectResetClicked() {
            this.setState({
                selectedEdgeIris: this.props.defaultSelectedEdgeIris
            });
        },

        handleSelectFilterChange(event) {
            const filter = event.target.value;
            this.setState({
                filter: filter
            });
        },

        render() {
            const edges = this.props.availableEdges
                .filter((e) => {
                    if (this.state.filter && this.state.filter.length > 0) {
                        return e.displayName.toLocaleLowerCase().indexOf(this.state.filter.toLocaleLowerCase()) >= 0;
                    }
                    return true;
                });

            return (<div className="find-path-edges-config">
                <div className="header">
                    <div className="title">{i18n('org.visallo.web.product.graph.findPath.edges.title')}</div>
                    <div className="actions">
                        <button
                            onClick={this.handleCancel}
                            className="btn btn-link">{i18n('org.visallo.web.product.graph.findPath.edges.cancel')}</button>
                    </div>
                </div>
                <div className="selection">
                    <div className="edge-list-header">
                        <div className="buttons">
                            <button
                                onClick={this.handleSelectAllClicked}
                                className="btn btn-mini">{i18n('org.visallo.web.product.graph.findPath.edges.select.all')}</button>
                            <button
                                onClick={this.handleSelectNoneClicked}
                                className="btn btn-mini">{i18n('org.visallo.web.product.graph.findPath.edges.select.none')}</button>
                            <button onClick={this.handleSelectResetClicked}
                                    className="btn btn-mini">{i18n('org.visallo.web.product.graph.findPath.edges.select.reset')}</button>
                        </div>
                        <input onChange={this.handleSelectFilterChange}/>
                    </div>
                    <div className="edge-list">
                        {edges.map((edge) => {
                            return (
                                <label key={edge.title}>
                                    <input type="checkbox"
                                           checked={this.state.selectedEdgeIris.indexOf(edge.title) >= 0}
                                           onChange={this.handleEdgeChecked.bind(this, edge)}/>
                                    {edge.displayName}
                                </label>
                            );
                        })}
                    </div>
                </div>
                <div className="actions">
                    <button onClick={this.handleLimitClick}
                            disabled={this.state.selectedEdgeIris.length === 0}
                            className="btn">{i18n('org.visallo.web.product.graph.findPath.edges.ok', this.state.selectedEdgeIris.length)}</button>
                </div>
            </div>);
        }
    });

    return FindPathEdgesConfig;
});
