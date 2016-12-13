define([
    'react'
], function(React) {
    'use strict';

    const Config = React.createClass({
        propTypes: {
            item: React.PropTypes.object.isRequired,
            extension: React.PropTypes.object.isRequired,
            visalloApi: React.PropTypes.object.isRequired,
            configurationChanged: React.PropTypes.func.isRequired
        },

        getInitialState: () => {return { savedSearches: [] }},

        componentWillMount() {
            this.props.visalloApi.v1.dataRequest('search', 'all')
                .then((searches) => {
                    this.setState({
                        savedSearches: searches
                    });
                });
        },

        onChangeSearch(e) {
            const { savedSearches } = this.state;
            const search = _.find(savedSearches, (savedSearch) => {
                return e.target.value === savedSearch.id;
            });

            let { item, extension, configurationChanged } = this.props;
            const title = item.configuration.title || `${extension.title}: ${search.name}`;

            const configuration = {
                ...item.configuration,
                searchId: search.id,
                searchParameters: search.parameters,
                title: title
            };
            item = { ...item, configuration: configuration }

            configurationChanged({ item: item, extension: extension });
        },

        render() {
            const { savedSearches } = this.state;
            const { searchId } = this.props.item.configuration;

            return (
                <section>
                    <label>{i18n('com.visallo.table.config.search.label')}</label>
                    <select
                        className="search"
                        value={searchId ? searchId : ''}
                        onChange={this.onChangeSearch}
                        disabled={!savedSearches}
                    >
                        <option
                            key="default"
                            value={''}
                        >
                            {i18n('com.visallo.table.config.search.placeholder')}
                        </option>
                        {savedSearches.map(
                            ({ name, id }) => <option key={id} value={id}>{name}</option>
                        )}
                    </select>
                </section>
            );
        }
    });

    return Config;
});
