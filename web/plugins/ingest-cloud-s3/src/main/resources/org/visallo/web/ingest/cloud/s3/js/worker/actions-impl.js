define([
    'data/web-worker/store/actions',
    'data/web-worker/util/ajax'
], function(actions, ajax) {

    actions.protectFromMain();

    const key = 'ingest-cloud-s3';

    const clearDirectories = () => ({
        type: 'INGEST_CLOUD_S3_CLEAR_CONTENTS'
    });

    const api = {
        connect: ({ providerClass, credentials }) => (dispatch, getState) => {
            dispatch({
                type: 'INGEST_CLOUD_S3_SET_CREDENTIALS',
                payload: {
                    providerClass,
                    credentials
                }
            })
            dispatch(api.openDirectory())
        },

        openDirectory: (name) => (dispatch, getState) => {
            const { auth, cwd, contentsByDir } = getState()[key]

            if (!auth) throw new Error('No authentication found for s3 in store. Did you dispatch connect?');

            const newDir = getNewDir();
            const newDirStr = newDir.join('/')

            dispatch({
                type: 'INGEST_CLOUD_S3_CD',
                payload: newDir
            })

            if (!contentsByDir[newDirStr]) {
                const { providerClass, credentials } = auth;

                ajax('POST', '/org/visallo/web/ingest/cloud/s3', {
                    providerClass,
                    credentials: JSON.stringify(credentials),
                    path: newDirStr
                })
                    .then(response => {
                        const { items, errorMessage } = response;
                        dispatch({
                            type: 'INGEST_CLOUD_S3_LOAD_CONTENTS',
                            payload: { path: newDirStr, contents: items, errorMessage }
                        })
                    })
                    .catch(response => {
                        console.error(response)
                        dispatch({ type: 'INGEST_CLOUD_S3_LOAD_CONTENTS' })
                    })
            }

            function getNewDir() {
                var newDir = cwd;
                if (name === '..') {
                    if (cwd.length) {
                        newDir = cwd.slice(0, cwd.length - 1);
                    }
                } else {
                    newDir = _.compact(cwd.concat([name]));
                }
                return newDir;
            }
        },

        selectItem: (name) => ({
            type: 'INGEST_CLOUD_S3_SELECT',
            payload: name
        }),

        refreshDirectories: () => (dispatch, getState) => {
            const { cwd } = getState()[key]
            dispatch(clearDirectories());
            if (cwd.length) {
                const dir = cwd[cwd.length - 1];
                dispatch(api.openDirectory());
            }
        }

    };

    return api;

})

