define([
    'react',
    'public/v1/api'
], function (React,
             visallo) {

    const UserTypeaheadInput = React.createClass({
        propTypes: {
            // callback when the typeahead input is changed
            onChange: React.PropTypes.func.isRequired,

            // callback when a user is selected
            onSelected: React.PropTypes.func.isRequired,

            // The current value of the typeahead input
            username: React.PropTypes.string
        },

        dataRequest: null,

        componentWillMount() {
            visallo.connect()
                .then(({dataRequest})=> {
                    this.dataRequest = dataRequest;
                });
        },

        componentDidMount() {
            this.setupTypeahead(this.refs.username);
        },

        setupTypeahead(usernameInput) {
            var groupedByDisplayName;

            $(usernameInput).typeahead({
                source: (query, callback) => {
                    this.dataRequest('user', 'search', query)
                        .done((users) => {
                            groupedByDisplayName = _.indexBy(users, 'displayName');
                            callback(_.keys(groupedByDisplayName));
                        });
                },
                updater: (displayName) => {
                    this.props.onChange(displayName);
                    this.props.onSelected(displayName);
                    return displayName;
                }
            });
        },

        handleInputChange(e) {
            this.props.onChange(e.target.value);
        },

        render() {
            return (
                <input type="text" ref="username" value={this.props.username} onChange={this.handleInputChange}
                       className="user"/>
            )
        }
    });

    return UserTypeaheadInput;
});
