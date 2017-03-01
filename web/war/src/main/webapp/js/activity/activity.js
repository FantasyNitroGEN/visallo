
define([
    'flight/lib/component',
    './template.hbs',
    './handlers',
    'd3',
    'configuration/plugins/registry',
    'util/component/attacher',
    'util/formatters',
    'util/withCollapsibleSections',
    'util/withDataRequest'
], function(
    defineComponent,
    template,
    builtinHandlers,
    d3,
    registry,
    Attacher,
    F,
    withCollapsibleSections,
    withDataRequest) {
    'use strict';

    var AUTO_UPDATE_INTERVAL_SECONDS = 60, // For updating relative times
        ACTIVITY_EXTENSTION_POINT = 'org.visallo.activity',
        handlersByKind = {},
        handlersByType = {};

    return defineComponent(Activity, withCollapsibleSections, withDataRequest);

    function processIsIndeterminate(process) {
        var handler = handlersByType[process.type];
        return handler && (handler.kind === 'eventWatcher' || handler.indeterminateProgress);
    }

    function processIsFinished(process) {
        return process.canceled || process.endTime;
    }

    function processShouldAutoDismiss(process) {
        var handler = handlersByType[process.type];
        return handler && handler.autoDismiss === true;
    }

    function processAllowCancel(process) {
        var handler = handlersByType[process.type];
        return !handler || (handler.kind === 'longRunningProcess' || handler.allowCancel === true);
    }

    function Activity() {

        this.attributes({
            typesSelector: '.types',
            deleteButtonSelector: 'button.delete',
            cancelButtonSelector: 'button.cancel',
            noActivitySelector: '.no-activity'
        })

        this.before('teardown', function() {
            clearInterval(this.autoUpdateTimer);
        });

        this.updateByKindAndTypeExtensions = function() {
            var e = registry.extensionsForPoint(ACTIVITY_EXTENSTION_POINT);
            handlersByType = _.indexBy(e, 'type');
            handlersByKind = _.groupBy(e, 'kind');
        };

        this.after('initialize', function() {

            /**
             * Activity items display in the floating panel accessed from the
             * menubar gears icon.
             *
             * @param {string} type Type identifier for this kind of activity. There can be more than one activity of this type in progress, and the display will group by this value.
             *
             * Define `activity.tasks.type.[MY_ACTIVITY_TYPE]` message bundle string for localized display.
             * @param {string} kind Either `eventWatcher` or `longRunningProcess`
             * @param {org.visallo.activity~titleRenderer} titleRenderer Render the title for row
             * @param {Array.<string>} [eventNames] Required if `eventWatcher`. Start event name, end event name.
             * @param {string} [finishedComponentPath] Path to {@link org.visallo.activity~FinishedComponent} to render when task is complete.
             * @param {org.visallo.activity~onRemove} [onRemove] Invoked when row is removed
             * @param {boolean} [indeterminateProgress=false] If determinate progress is not available, will render indeterminate progress bar.
             * @param {boolean} [autoDismiss=false] Remove this activity row when complete
             * @param {boolean} [allowCancel=false] Whether the activity supports cancelling (will render cancel button if true).
             */
            registry.documentExtensionPoint(
                'org.visallo.activity',
                'Custom activity rows based on events or long running processes',
                function(e) {
                    return ('type' in e) && ('kind' in e) && _.isFunction(e.titleRenderer) &&
                        (e.kind !== 'eventWatcher' || (_.isArray(e.eventNames) && e.eventNames.length === 2));
                },
                'http://docs.visallo.org/extension-points/front-end/activity'
            );

            builtinHandlers.forEach(function(h) {
                registry.registerExtension(ACTIVITY_EXTENSTION_POINT, h);
            });

            this.updateByKindAndTypeExtensions();

            this.removedTasks = {};
            this.$node.html(template({}));

            this.tasks = visalloData.currentUser && visalloData.currentUser.longRunningProcesses || [];
            this.tasksById = _.indexBy(this.tasks, 'id');

            this.throttledUpdate = _.debounce(this.update.bind(this), 100);
            this.updateEventWatchers();

            this.on(document, 'menubarToggleDisplay', this.onToggleDisplay);
            this.on(document, 'longRunningProcessChanged', this.onLongRunningProcessChanged);
            this.on(document, 'longRunningProcessDeleted', this.onLongRunningProcessDeleted);
            this.on(document, 'showActivityDisplay', this.onShowActivityDisplay);
            this.on(document, 'extensionsChanged', this.onExtensionsChanged);
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);

            this.on('click', {
                deleteButtonSelector: this.onDelete,
                cancelButtonSelector: this.onCancel
            })
        });

        this.onShowActivityDisplay = function(event) {
            var visible = this.$node.closest('.visible').length > 0;

            if (!visible) {
                this.trigger('menubarToggleDisplay', { name: 'activity' });
            }
        };

        this.onCancel = function(event) {
            this.callServiceMethodRemove(event, 'cancel');
        };

        this.onDelete = function(event) {
            this.callServiceMethodRemove(event, 'delete');
        };

        this.callServiceMethodRemove = function(event, name) {
            var self = this,
                $button = $(event.target),
                processId = $button.closest('li').data('processId');

            if (processId) {
                $button.addClass('loading');
                this.dataRequest('longRunningProcess', name, processId)
                    .then(function() {
                        $(event.target).closest('li').remove();
                    })
                    .finally(function() {
                        $button.removeClass('loading');
                    })
            }
        };

        this.onLongRunningProcessChanged = function(event, data) {
            var task = data.process;

            this.addOrUpdateTask(task);
            this.update(true);
        };

        this.onLongRunningProcessDeleted = function(event, data) {
            var self = this,
                processId = data.processId;

            this.removeTask(processId);
            this.update(true);
        };

        this.onVerticesUpdated = function(event, data) {
            this.throttledUpdate();
        };

        this.onExtensionsChanged = function(event, data) {
            if (data.extensionPoint === ACTIVITY_EXTENSTION_POINT) {
                this.updateByKindAndTypeExtensions();
                this.updateEventWatchers();
                this.update();
            }
        };

        this.updateEventWatchers = function() {
            var self = this,
                namespace = '.ACTIVITY_HANDLER',
                eventWatchers = handlersByKind.eventWatcher;

            this.off(document, namespace);
            eventWatchers.forEach(function(activityHandler) {
                self.on(
                    document,
                    activityHandler.eventNames[0] + namespace,
                    self.onActivityHandlerStart.bind(self, activityHandler)
                );
                self.on(
                    document,
                    activityHandler.eventNames[1] + namespace,
                    self.onActivityHandlerEnd.bind(self, activityHandler)
                );
            });
        };

        this.removeTask = function(taskId) {
            this.removedTasks[taskId] = true;
            var task = this.tasksById[taskId];
            var handler = handlersByType[task.type];

            if (handler && handler.onRemove) {
                if (_.isFunction(handler.onRemove)) {
                    /**
                     * Invoked when the row is removed by user or `autoDismiss`
                     *
                     * @callback org.visallo.activity~onRemove
                     * @example
                     * onRemove() {
                     *     // "this" is the flight activity component
                     *     this.trigger(...);
                     * }
                     */
                    handler.onRemove.call(this);
                } else {
                    console.error('handler.onRemove expected a function, instead got: ', handler.onRemove);
                }
            }

            delete this.tasksById[taskId];
            this.tasks.splice(this.tasks.indexOf(task), 1);
        };

        this.addOrUpdateTask = function(task) {
            var existingTask = this.tasksById[task.id];

            if (this.removedTasks[task.id]) {
                return;
            }

            if (existingTask) {
                this.tasks.splice(this.tasks.indexOf(existingTask), 1, task);
            } else {
                this.tasks.splice(0, 0, task);
            }

            this.tasksById[task.id] = task;
        };

        this.onActivityHandlerStart = function(handler, event, data) {
            this.addOrUpdateTask({
                id: handler.type,
                type: handler.type,
                enqueueTime: Date.now(),
                startTime: Date.now(),
                eventData: data
            });
            this.update();
        };

        this.onActivityHandlerEnd = function(handler, event, data) {
            this.addOrUpdateTask({
                id: handler.type,
                type: handler.type,
                endTime: Date.now(),
                eventData: data
            });
            this.update();
        };

        this.onToggleDisplay = function(event, data) {
            var openingActivity = data.name === 'activity' && this.$node.closest('.visible').length;

            this.isOpen = openingActivity;

            if (openingActivity) {
                this.update();
                this.startAutoUpdate();
            } else {
                this.pauseAutoUpdate();
            }
        };

        this.pauseAutoUpdate = function() {
            clearInterval(this.autoUpdateTimer);
        };

        this.startAutoUpdate = function() {
            this.autoUpdateTimer = setInterval(this.update.bind(this), AUTO_UPDATE_INTERVAL_SECONDS * 1000)
        };

        this.notifyActivityMonitors = function(tasks) {
            var count = _.reduce(tasks, function(count, task) {
                if (processIsFinished(task)) {
                    return count;
                }

                return count + 1;
            }, 0);

            if (!this.previousCountForNotify || count !== this.previousCountForNotify) {
                this.trigger('activityUpdated', { count: count });
            }
        }

        this.update = function(closeIfEmpty) {
            var self = this,
                tasks = _.chain(this.tasks)
                    .filter(function(p) {
                        if (p.canceled) {
                            return false;
                        }

                        if (processIsFinished(p) && processShouldAutoDismiss(p)) {
                            return false;
                        }

                        return true;
                    })
                    .sortBy(function(p) {
                        return p.enqueueTime * -1;
                    })
                    .value(),
                data = _.chain(tasks)
                    .groupBy('type')
                    .pairs()
                    .sortBy(function(pair) {
                        return pair[0].toLowerCase();
                    })
                    .value();

            this.currentTaskCount = tasks.length;
            this.notifyActivityMonitors(tasks);

            if (!this.isOpen) {
                return;
            }

            var uniqueTypes = _.chain(data)
                .map(function(p) {
                    return p[0];
                })
                .unique()
                .map(function(type) {
                    var byType = handlersByType;
                    if (type in byType) {
                        if (byType[type].finishedComponentPath) {
                            return byType[type].finishedComponentPath;
                        }
                        return;
                    }

                    console.warn('No activity handler registered for type:', type);
                })
                .compact()
                .value();

            var promises = _.map(uniqueTypes, function(type) {
                return Promise.require(type);
            });

            return Promise.all(promises)
                .then(function(deps) {
                    self.updateWithDependencies.apply(self, [data, uniqueTypes].concat(deps))
                })
                .then(function() {
                    if (closeIfEmpty && self.currentTaskCount === 0) {
                        _.delay(function() {
                            var visible = self.$node.closest('.visible').length > 0;
                            if (visible) {
                                self.trigger('menubarToggleDisplay', { name: 'activity' });
                            }
                        }, 250)
                    }
                })
        };

        this.updateWithDependencies = function(data, requirePaths) {
            var finishedComponents = _.object(requirePaths, Array.prototype.slice.call(arguments, 2))

            this.select('noActivitySelector').toggle(data.length === 0);

            d3.select(this.select('typesSelector').get(0))
                .selectAll('section')
                .data(data)
                .order()
                .call(function() {
                    this.enter()
                        .append('section')
                        .call(function() {
                            this.attr('class', 'collapsible expanded')
                            this.append('h1')
                                .attr('class', 'collapsible-header')
                                .call(function() {
                                    this.append('strong');
                                    this.append('span').attr('class', 'badge');
                                })
                            this.append('ul').attr('class', 'collapsible-section')
                        });
                    this.exit()
                        .each(function() {
                            $('.actions-plugin', this).teardownAllComponents();
                        })
                        .remove();

                    this.select('.collapsible-header strong').text(function(pair) {
                        return i18n('activity.tasks.type.' + pair[0]);
                    })

                    this.select('.collapsible-header .badge').text(function(pair) {
                        return F.number.pretty(pair[1].length);
                    })

                    this.select('ul')
                        .selectAll('li')
                        .data(function(pair) {
                            return pair[1];
                        }, function(process) {
                            return process.id;
                        })
                        .order()
                        .call(function() {
                            this.enter()
                                .append('li')
                                .call(function() {
                                    this.append('div')
                                        .attr('class', 'actions-container')
                                        .call(function() {
                                            this.append('div')
                                                .attr('class', 'actions-plugin')
                                            this.append('button')
                                                .attr('class', 'btn btn-mini btn-danger delete')
                                                .text(i18n('activity.process.button.dismiss'))
                                        })
                                    this.append('div')
                                        .attr('class', 'type-container')
                                    this.append('div')
                                        .attr('class', 'progress-container')
                                        .call(function() {
                                            this.append('button').attr('class', 'cancel')
                                            this.append('div').attr('class', 'progress')
                                                .append('div')
                                                    .attr('class', 'bar')
                                        })
                                    this.append('div')
                                        .attr('class', 'progress-description')
                                });
                            this.exit()
                                .each(function() {
                                    $('.actions-plugin', this).teardownAllComponents();
                                })
                                .remove();

                            this.attr('data-process-id', _.property('id'));
                            // TODO: add transition to delay this?
                            this.attr('class', function(process) {
                                if (processIsFinished(process)) {
                                    return 'finished';
                                }
                            });

                            this.select('.actions-plugin').each(function() {
                                var datum = d3.select(this).datum(),
                                    handler = handlersByType[datum.type];

                                if (handler) {
                                    var componentPath = handler.finishedComponentPath,
                                        Component = componentPath && finishedComponents[componentPath];

                                    if (Component && datum.endTime) {
                                        /**
                                         * FlightJS or React Component to render when activity is completed
                                         *
                                         * @typedef org.visallo.activity~FinishedComponent
                                         * @property {object} activityItem The activity item
                                         */
                                        Attacher()
                                            .node(this)
                                            .component(Component)
                                            .params({
                                                process: datum
                                            })
                                            .attach()
                                        return;
                                    }
                                }

                                Attacher().node(this).teardown();
                            });

                            this.select('.type-container').each(function() {
                                var datum = d3.select(this).datum();

                                if (datum.type && datum.type in handlersByType) {
                                    /**
                                     * Function that is responsible for populating the text of the activity row.
                                     *
                                     * @callback org.visallo.activity~titleRenderer
                                     * @param {Element} el Html element to render title in
                                     * @param {object} activity The activity object. Either long running process json, or object with eventData.
                                     */
                                    handlersByType[datum.type].titleRenderer(this, datum);
                                } else if (datum.type) {
                                    console.warn('No activity formatter for ', datum.type);
                                } else {
                                    console.warn('No activity process doesn\'t contain a type property');
                                }
                            });

                            this.select('.bar').style('width', function(process) {
                                if (processIsIndeterminate(process)) {
                                    return '100%';
                                }
                                return ((process.progress || 0) * 100) + '%';
                            });

                            this.select('.progress-container').attr('class', function(process) {
                                var cls = 'progress-container ';
                                if (processAllowCancel(process)) {
                                    return cls;
                                }

                                return cls + 'no-cancel';
                            });

                            this.select('.progress').attr('class', function(process) {
                                var cls = 'progress ';

                                if (processIsIndeterminate(process)) {
                                    return cls + 'active progress-striped';
                                }
                                return cls + ' determinate';
                            });

                            this.select('.progress-description').text(function(process) {
                                var timePrefix = '';

                                if (process.endTime) {
                                    timePrefix = 'Finished ' + F.date.relativeToNow(F.date.utc(process.endTime));
                                } else if (process.startTime) {
                                    timePrefix = 'Started ' + F.date.relativeToNow(F.date.utc(process.startTime));
                                } else {
                                    timePrefix = 'Enqueued ' + F.date.relativeToNow(F.date.utc(process.enqueueTime));
                                }

                                if (process.progressMessage) {
                                    return timePrefix + ' – ' + process.progressMessage;
                                }

                                return timePrefix;
                            });
                        })
                });
        }

    }
});
