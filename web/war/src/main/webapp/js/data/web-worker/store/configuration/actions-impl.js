define(['../actions', '../../util/ajax'], function(actions, ajax) {
    actions.protectFromMain();

    const api = {
        get: ({ locale }) => {
            var data = {};
            if (locale) {
                if (locale.language) data.localeLanguage = locale.language;
                if (locale.country) data.localeCountry = locale.country;
                if (locale.variant) data.localeVariant = locale.variant;
            }
            return ajax('GET', '/configuration', data)
                .then(result => api.update(result))
        },

        update: (configuration) => ({
            type: 'CONFIGURATION_UPDATE',
            payload: configuration
        })
    }

    return api;
})

