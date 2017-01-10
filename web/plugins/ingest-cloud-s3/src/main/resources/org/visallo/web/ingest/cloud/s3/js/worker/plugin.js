define([
    'configuration/plugins/registry',
    'updeep'
], function(registry, u) {

    registry.registerExtension('org.visallo.store', {
        key: 'ingest-cloud-s3',
        reducer: function(state, { type, payload }) {
            if (!state) return {
                auth: {},
                cwd: [],
                selected: [],
                contentsByDir: {}
            };

            switch (type) {
                case 'INGEST_CLOUD_S3_SET_CREDENTIALS':
                    return u({
                        auth: u.constant(payload),
                        cwd: u.constant([]),
                        selected: u.constant([]),
                        contentsByDir: u.constant({}),
                        loading: true
                    }, state);

                case 'INGEST_CLOUD_S3_CD':
                    return u({
                        cwd: payload,
                        selected: []
                    }, state);

                case 'INGEST_CLOUD_S3_LOAD_CONTENTS':
                    if (payload) {
                        return u({
                            loading: false,
                            authenticated: !Boolean(payload.errorMessage),
                            errorMessage: payload.errorMessage,
                            contentsByDir: {
                                [payload.path]: payload.contents
                            }
                        }, state);
                    } else {
                        return u({
                            loading: false,
                            authenticated: false,
                            errorMessage: 'Server error'
                        }, state)
                    }

                case 'INGEST_CLOUD_S3_SELECT':
                    if (payload) {
                        return u({
                            selected: function toggleSelected(selected) {
                                if (selected.includes(payload)) {
                                    return _.without(selected, payload)
                                }
                                return [...selected, payload];
                            }
                        }, state)
                    } else {
                        return u({
                            selected: []
                        }, state)
                    }

                case 'INGEST_CLOUD_S3_CLEAR_CONTENTS':
                    return {
                        ...state,
                        contentsByDir: {}
                    };
            }

            return state;
        }
    })
});
