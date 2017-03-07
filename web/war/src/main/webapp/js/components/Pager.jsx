define([
    'react'
], function(React) {
    'use strict';

    return React.createClass({
        propTypes: {
            page: React.PropTypes.number.isRequired,
            pageCount: React.PropTypes.number.isRequired,
            onPreviousClick: React.PropTypes.func.isRequired,
            onNextClick: React.PropTypes.func.isRequired
        },

        handlePreviousClick() {
            if (this.props.page > 0) {
                this.props.onPreviousClick();
            }
        },

        handleNextClick() {
            if (this.props.page < this.props.pageCount) {
                this.props.onNextClick();
            }
        },

        render() {
            return (<p className="paging">
                Page {this.props.page} of {this.props.pageCount}
                <button className="previous" disabled={this.props.page <= 1} onClick={this.handlePreviousClick}/>
                <button className="next" disabled={this.props.page >= this.props.pageCount}
                        onClick={this.handleNextClick}/>
            </p>);
        }
    });
});
