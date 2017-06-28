define([
    'create-react-class',
    'prop-types',
    'public/v1/api'
], function(createReactClass, PropTypes, visallo) {

    const UserTypeaheadInput = createReactClass({
        propTypes: {
            // callback when the typeahead input is changed
            onInputChange: PropTypes.func.isRequired,

            // callback when a user is selected
            onUserSelected: PropTypes.func.isRequired,

            // current selected user
            user: PropTypes.object
        },

        dataRequest: null,

        getInitialState() {
            return {
                input: ''
            };
        },

        componentWillMount() {
            visallo.connect()
                .then(({dataRequest})=> {
                    this.dataRequest = dataRequest;
                });
        },

        componentDidMount() {
            this.setupTypeahead(this.refs.user);
        },

        setupTypeahead(userInput) {
            var map = {};

            $(userInput).typeahead({
                source: (query, callback) => {
                    this.dataRequest('user', 'search', {query: query})
                        .done((users) => {
                            map = _.indexBy(users, (user) => {
                                return this.formatUser(user);
                            });
                            callback(_.keys(map));
                        });
                },
                updater: (displayValue) => {
                    var user = map[displayValue];
                    this.props.onUserSelected(user);
                    return displayValue;
                }
            });
        },

        handleInputChange(e) {
            const input = e.target.value;
            this.setState({
                input: input
            });
            this.props.onInputChange(input);
        },

        formatUser(user) {
            if (!user) {
                return '';
            }
            if (user.displayName === user.userName) {
                return user.displayName;
            }
            return user.displayName + ' (' + user.userName + ')';
        },

        formatUserInput() {
            if (!this.props.user) {
                return this.state.input;
            }
            return this.formatUser(this.props.user);
        },

        render() {
            return (
                <input type="text" ref="user"
                       value={this.formatUserInput()}
                       onChange={this.handleInputChange}
                       className="user"/>
            )
        }
    });

    return UserTypeaheadInput;
});
