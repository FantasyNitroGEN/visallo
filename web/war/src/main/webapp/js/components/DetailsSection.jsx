define([
    'react'
], function (React) {
    'use strict';

    return React.createClass({
        propTypes: {
            /**
             * Title to display in the section header
             */
            title: React.PropTypes.string.isRequired,

            /**
             * Addition class names to apply to the section
             */
            className: React.PropTypes.string,

            /**
             * Value to display on the right of the section bar
             */
            badge: React.PropTypes.oneOfType([
                React.PropTypes.string,
                React.PropTypes.number
            ]),

            /**
             * The title attribute to place in badge HTML element
             */
            badgeTitle: React.PropTypes.string,

            /**
             * event called when the section is toggled. The first parameter is a boolean indicating what the new state
             * should be.
             */
            onToggle: React.PropTypes.func.isRequired,

            /**
             * event called when the search icon is clicked.
             */
            onSearchClick: React.PropTypes.func,

            /**
             * true, if expanded
             */
            expanded: React.PropTypes.bool.isRequired
        },

        toggleExpanded() {
            if (this.props.onToggle) {
                this.props.onToggle(!this.props.expanded);
            }
        },

        handleTitleClick() {
            this.toggleExpanded();
        },

        handleSearchClick(event) {
            event.stopPropagation();
            this.props.onSearchClick(event);
        },

        renderSearch() {
            if (!this.props.onSearchClick) {
                return;
            }
            return (<s title="Open in Search..." className="search" onClick={this.handleSearchClick}/>);
        },

        renderCount() {
            if (typeof this.props.badge === 'undefined' || this.props.badge === null) {
                return null;
            }
            return (<span title={this.props.badgeTitle} className="badge">{this.props.badge}</span>);
        },

        render() {
            return (<section
                className={`${this.props.className} collapsible ${this.props.expanded ? 'expanded' : ''}`}>
                <h1 onClick={this.handleTitleClick}>
                    <strong>{this.props.title}</strong>
                    {this.renderSearch()}
                    {this.renderCount()}
                </h1>
                {this.props.expanded ? this.props.children : null}
            </section>);
        }
    });
});
