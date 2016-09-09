
define([
    '../store',
    '../util/ajax',
    '../store/element/actions-impl'
], function(store, ajax, elementActions) {
    'use strict';

    function propertyUrl(elementType, property) {
        return '/' + elementType + '/' +
            (property.name === 'http://visallo.org/comment#entry' ? 'comment' : 'property');
    }

    registerStoreListenerAndFireVerticesUpdated();

    return {
        updateElement(workspaceId, element) {
            if (arguments.length === 1) {
                element = workspaceId;
                workspaceId = store.getStore().getState().workspace.currentId;
            }
            store.getStore().dispatch(
                elementActions.updateElement(workspaceId, element)
            );
        },

        putSearchResults(elements) {
            store.getStore().dispatch(elementActions.putSearchResults(elements))
        },

        createStoreAccessorOrDownloader: (type) => (options) => {
            const key = type + 'Ids';
            const resultKey = type === 'vertex' ? 'vertices' : 'edges';
            const state = store.getStore().getState();
            const workspaceId = state.workspace.currentId;
            const elements = state.element[workspaceId];
            const returnSingular = !_.isArray(options[key]);
            const elementIds = returnSingular ? [options[key]] : options[key];

            var toRequest = elementIds;
            if (elements) {
                toRequest = _.reject(toRequest, id => id in elements[resultKey]);
            }

            return (
                toRequest.length ?
                ajax('POST', `/${type}/multiple`, { [key]: toRequest }) :
                Promise.resolve({[resultKey]:[]})
            ).then(function(result) {
                const results = result[resultKey];

                if (results.length) {
                    store.getStore().dispatch(elementActions.update({ [resultKey]: results, workspaceId }));
                }

                if (elements) {
                    const existing = _.pick(elements[resultKey], elementIds)
                    return Object.values(existing).concat(results)
                }
                return results;
            }).then(function(ret) {
                return returnSingular && ret.length ? ret[0] : ret;
            })
        },

        vertexPropertyUrl: function(property) {
            return propertyUrl('vertex', property);
        },

        edgePropertyUrl: function(property) {
            return propertyUrl('edge', property);
        }
    };

    function registerStoreListenerAndFireVerticesUpdated() {
        const redux = store.getStore();
        var previousElements, previousElementsWorkspaceId;
        redux.subscribe(function() {
            const state = redux.getState();
            const workspaceId = state.workspace.currentId;
            const newElements = state.element[workspaceId];
            const workspaceChanged = workspaceId !== previousElementsWorkspaceId;

            if (previousElements && !workspaceChanged && newElements !== previousElements) {
                ['vertices', 'edges'].forEach(function(type) {
                    const updated = [];
                    const deleted = [];
                    _.each(newElements[type], (el, id) => {
                        if (id in previousElements[type] && el !== previousElements[type][id]) {
                            if (el === null) {
                                deleted.push(id);
                            } else {
                                updated.push(el);
                            }
                        }
                    })

                    if (updated.length) {
                        dispatchMain('rebroadcastEvent', {
                            eventName: `${type}Updated`,
                            data: {
                                [type]: updated
                            }
                        })
                    }
                    if (deleted.length) {
                        let fire = (data) => {
                            dispatchMain('rebroadcastEvent', { eventName: `${type}Deleted`, data })
                        }
                        if (type === 'vertices') {
                            fire({ vertexIds: deleted });
                        } else {
                            deleted.forEach(id => fire({ edgeId: id }));
                        }
                    }
                })
            }
            previousElements = newElements;
            previousElementsWorkspaceId = workspaceId;
        });
    }
});
