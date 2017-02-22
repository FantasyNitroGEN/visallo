define([], function() {
    'use strict';

    const validActionKeys = 'type payload error meta'.split(' ');

    return {

        // Similar to https://github.com/acdlite/flux-standard-action
        isValidAction(action) {
            return (
                _.isObject(action) &&
                _.isString(action.type) &&
                Object.keys(action).every(isValidKey)
            );

            function isValidKey(key) { return validActionKeys.indexOf(key) > -1; }
        },

        /*
         * Create FSA compatible actions which include the worker action
         * mapper.
         *
         * We can't send functions over the worker wire, so just send keys and
         * map those to new actions that could be processed by thunk, promise
         * middleware.
         *
         * Specify functions as the value of actions to add create action
         * payloads from calling code. If value is not a function it's ignored.
         */
        createActions({ workerImpl, actions }) {
            if (!_.isString(workerImpl)) throw new Error('workerImpl must be defined as a string')
            if (!_.isObject(actions)) throw new Error('actions must be an object')

            return _.mapObject(actions, function(value, key) {
                var action = { type: 'ROUTE_TO_WORKER_ACTION', payload: {}, meta: { workerImpl: workerImpl, name: key } }
                if (_.isFunction(value)) {
                    return function(...params) {
                        return { ...action, payload: value(...params) }
                    }
                } else {
                    return action;
                }
            })
        },

        protectFromMain() {
            if (isMainThread()) throw new Error('This file should only be required in a worker thread')
        },

        protectFromWorker() {
            if (!isMainThread()) throw new Error('This file should only be required in the main thread');
        }

    }

    function isMainThread() {
        return typeof importScripts === 'undefined'
    }
})
