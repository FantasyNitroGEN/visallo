define([
    'react',
    './FindPathHopsImage'
], function (React, FindPathHopsImage) {
    'use strict';

    const FindPathTopLevelConfig = React.createClass({
        propTypes: {
            availableEdges: React.PropTypes.arrayOf(React.PropTypes.any).isRequired,
            selectedEdgeIris: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
            defaultSelectedEdgeIris: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
            maximumHops: React.PropTypes.number.isRequired,
            executing: React.PropTypes.bool,
            onMaximumHopsChange: React.PropTypes.func.isRequired,
            onExecute: React.PropTypes.func.isRequired,
            onEdgesValueConfigure: React.PropTypes.func.isRequired
        },

        handleMaximumHopsChange(event) {
            const value = event.target.value;
            this.props.onMaximumHopsChange(parseInt(value));
        },

        handleExecuteClick() {
            this.props.onExecute();
        },

        handleEdgesValueClick() {
            this.props.onEdgesValueConfigure();
        },

        render(){
            const edgesLabel = this.props.availableEdges.length === this.props.selectedEdgeIris.length
                ? i18n('org.visallo.web.product.graph.findPath.edgesAll')
                : arraysContainsSameItems(this.props.selectedEdgeIris, this.props.defaultSelectedEdgeIris)
                    ? i18n('org.visallo.web.product.graph.findPath.edgesDefault')
                    : i18n('org.visallo.web.product.graph.findPath.edgesCount', this.props.selectedEdgeIris.length);

            return (<div className="find-path-config">
                <div className="field">
                    <div className="field-title-value">
                        <div className="field-title">{i18n('org.visallo.web.product.graph.findPath.edges')}</div>
                        <div className="value" onClick={this.handleEdgesValueClick}>{edgesLabel}</div>
                    </div>
                    <div className="subtitle">{i18n('org.visallo.web.product.graph.findPath.edgesSubtitle')}</div>
                </div>
                <div className="field">
                    <div className="field-title-value">
                        <div className="field-title">{i18n('org.visallo.web.product.graph.findPath.maximumHops')}</div>
                        <div className="value">
                            <div className="select-wrapper">
                                <select value={this.props.maximumHops} onChange={this.handleMaximumHopsChange}>
                                    <option value="2">2</option>
                                    <option value="3">3</option>
                                    <option value="4">4</option>
                                    <option value="5">5</option>
                                </select>
                            </div>
                        </div>
                    </div>
                    <div className="subtitle">{i18n('org.visallo.web.product.graph.findPath.maximumHopsSubtitle')}</div>

                    <FindPathHopsImage
                        hops={this.props.maximumHops}
                        hopsTitle={i18n('org.visallo.web.product.graph.findPath.maximumHopsImageAltText', this.props.maximumHops)}
                        onChange={this.props.onMaximumHopsChange}/>
                </div>
                <div>
                    <button
                        className="btn btn-primary find-path"
                        disabled={this.props.executing}
                        onClick={this.handleExecuteClick}>{i18n('org.visallo.web.product.graph.findPath.execute')}</button>
                </div>
            </div>);
        }
    });

    function arraysContainsSameItems(a1, a2) {
        if (a1.length !== a2.length) {
            return false;
        }
        a1 = a1.sort();
        a2 = a2.sort();
        for (let i = 0; i < a1.length; i++) {
            if (a1[i] !== a2[i]) {
                return false;
            }
        }
        return true;
    }

    return FindPathTopLevelConfig;
});
