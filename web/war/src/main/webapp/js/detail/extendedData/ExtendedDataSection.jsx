define([
    'react',
    'configuration/plugins/registry',
    'components/DetailsSection',
    'detail/extendedData/ExtendedDataSectionContents'
], function (React, registry, DetailsSection, ExtendedDataSectionContents) {
    'use strict';

    registry.documentExtensionPoint('org.visallo.detail.extendedData', 'Replace extended data section with custom component', function(e) {
        return _.isFunction(e.shouldReplaceExtendedDataSection) && _.isString(e.componentPath);
    });

    return React.createClass({
        propTypes: {
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
            onSearchExtendedData: React.PropTypes.func.isRequired
        },

        getInitialState() {
            return {
                badge: null,
                badgeTitle: null,
                sectionExpanded: false,
                sectionContents: null
            };
        },

        componentWillMount() {
            this.componentWillReceiveProps(this.props);
        },

        componentWillReceiveProps(nextProps) {
            const extensions = registry.extensionsForPoint('org.visallo.detail.extendedData')
                .filter((e) => {
                    return e.shouldReplaceExtendedDataSection({
                        elementId: nextProps.elementId,
                        elementType: nextProps.elementType,
                        tableName: nextProps.table.tableName
                    });
                });

            if (extensions.length > 1) {
                console.warn('Multiple extensions wanting to override text', extensions);
            }

            if (extensions.length === 0) {
                this.setState({
                    sectionContents: ExtendedDataSectionContents
                });
            } else {
                Promise.require(extensions[0].componentPath)
                    .then((component) => {
                        this.setState({
                            sectionContents: component
                        });
                    });
            }
        },

        handleSectionToggle(expanded) {
            this.setState({
                sectionExpanded: expanded
            });
        },

        handleSectionSearchClick() {
            this.props.onSearchExtendedData({
                elementType: this.props.elementType,
                elementId: this.props.elementId,
                tableName: this.props.table.tableName
            });
        },

        handleContentsLoad(event) {
            this.setState({
                badge: event.badge,
                badgeTitle: event.badgeTitle
            });
        },

        render() {
            if (!this.state.sectionContents) {
                return null;
            }

            return (
                <DetailsSection
                    title={this.props.table.displayName}
                    visalloApi={this.props.visalloApi}
                    className="extended-data-table-section"
                    badge={this.state.badge}
                    badgeTitle={this.state.badgeTitle}
                    onToggle={this.handleSectionToggle}
                    onSearchClick={this.handleSectionSearchClick}
                    expanded={this.state.sectionExpanded}>
                    <this.state.sectionContents
                        visalloApi={this.props.visalloApi}
                        elementId={this.props.elementId}
                        elementType={this.props.elementType}
                        table={this.props.table}
                        onLoad={this.handleContentsLoad}
                    />
                </DetailsSection>
            );
        }
    });
});
