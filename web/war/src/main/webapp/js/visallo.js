require([
    'jquery',
    'flight/lib/compose',
    'flight/lib/registry',
    'flight/lib/advice',
    'flight/lib/logger',
    'flight/lib/debug',
    'underscore',
    'underscore.inflection',
    'util/visibility',
    'util/privileges',
    'util/jquery.flight',
    'util/jquery.removePrefixedClasses',
    'util/jquery/plugins',
    'util/promise'
],
function(jQuery,
         compose,
         registry,
         advice,
         withLogging,
         debug,
         _,
         _inflection,
         Visibility,
         Privileges) {
    'use strict';

    $.ui = { keyCode: { ENTER: 13 } };

    // Debug retina/non-retina by changing to 1/2
    // window.devicePixelRatio = 1;

    window.requestAnimationFrame =
        typeof window === 'undefined' ?
        function() { } :
        (
            window.requestAnimationFrame ||
            window.mozRequestAnimationFrame ||
            window.webkitRequestAnimationFrame ||
            window.msRequestAnimationFrame ||
            function(callback) {
                setTimeout(callback, 1000 / 60);
            }
        );
    window.TRANSITION_END = 'transitionend webkitTransitionEnd MSTransitionEnd oTransitionEnd otransitionend';
    window.ANIMATION_END = 'animationend webkitAnimationEnd MSAnimationEnd oAnimationEnd oanimationend';

    var progress = 0,
        progressBar = null,
        progressBarText = null,
        TOTAL_PROGRESS = 4,
        MAX_RESIZE_TRIGGER_INTERVAL = 250,
        App, FullScreenApp, F, withDataRequest,
        previousUrl = window.location.href;

    $(function() {
        require(['cli']);
        require(['data/data'], configureApplication);

        function configureApplication(Data) {
            // Flight Logging
            try {
                debug.enable(false);
                DEBUG.events.logNone();
            } catch(e) {
                console.warn('Error enabling DEBUG mode for flight' +
                ', probably because Safari Private Browsing enabled', e);
            }

            // Default templating
            _.templateSettings.escape = /\{([\s\S]+?)\}/g;
            _.templateSettings.evaluate = /<%([\s\S]+?)%>/g;
            _.templateSettings.interpolate = /\{-([\s\S]+?)\}/g;

            Data.attachTo(document);
            Visibility.attachTo(document);
            Privileges.attachTo(document);
            $(window)
                .on('hashchange', loadApplicationTypeBasedOnUrlHash)
                .on('resize', _.throttle(function(event) {
                    if (event.target !== window) {
                        return;
                    }
                    $(document).trigger('windowResize');
                }, MAX_RESIZE_TRIGGER_INTERVAL));

            Promise.require('util/messages')
                .then(function(_i18n) {
                    window.i18n = _i18n;
                    updateVisalloLoadingProgress('dependencies');

                    require([
                        'util/vertex/urlFormatters',
                        'util/withDataRequest',
                        'util/handlebars/before_auth_helpers'
                    ], function(_F, _withDataRequest) {
                        F = _F;
                        withDataRequest = _withDataRequest;
                        loadApplicationTypeBasedOnUrlHash();
                    })
                })
        }
    });

    function userHasValidPrivileges(me) {
        return me.privileges && me.privileges.length > 0;
    }

    /**
     * Switch between visallo and visallo-fullscreen-details based on url hash
     */
    function loadApplicationTypeBasedOnUrlHash(e) {
        var toOpen = F.vertexUrl.parametersInUrl(location.href),
            vertexIds = toOpen && toOpen.vertexIds,
            edgeIds = toOpen && toOpen.edgeIds,
            toOpenIds = (
                (vertexIds && vertexIds.length) ||
                (edgeIds && edgeIds.length)
            ),

            workspaceId = toOpen && toOpen.workspaceId,

            // Is this the popoout details app? ids passed to hash?
            popoutDetails = !!(toOpen && toOpen.type === 'FULLSCREEN' && toOpenIds),

            // If this is a hash change
            event = e && e.originalEvent,

            // Is this the default visallo application?
            mainApp = !popoutDetails,
            newUrl = window.location.href;

        if (event && isAddUrl(previousUrl) && isMainApp(newUrl)) {
            previousUrl = newUrl;
            return;
        }

        if (event && isPopoutUrl(previousUrl) && isPopoutUrl(newUrl)) {
            previousUrl = newUrl;
            return $('#app').trigger('vertexUrlChanged', {
                vertexIds: vertexIds,
                edgeIds: edgeIds,
                workspaceId: workspaceId
            });
        }

        previousUrl = newUrl;
        Promise.all(visalloPluginResources.beforeAuth.map(Promise.require))
            .then(function() {
                return withDataRequest.dataRequest('user', 'me')
            })
            .then(function(me) {
                if (!userHasValidPrivileges(me)) {
                    throw new Error('missing privileges')
                }
                attachApplication(false);
            })
            .catch(function() {
                attachApplication(true, '', {});
            });

        function attachApplication(loginRequired, message, options) {
            if (!event) {
                $('html')
                    .toggleClass('fullscreenApp', mainApp)
                    .toggleClass('fullscreenDetails', popoutDetails);

                window.isFullscreenDetails = popoutDetails;
            }

            visalloData.isFullscreen = false;

            if (loginRequired) {
                // Login required, once less progress item (no after-auth plugins)
                TOTAL_PROGRESS--;
                updateVisalloLoadingProgress('userinterface');

                $(document).one('loginSuccess', function() {
                    Promise.all(visalloPluginResources.afterAuth.map(Promise.require))
                        .catch(function(error) {
                            $('#login').trigger('showErrorMessage', {
                                message: i18n('visallo.loading.progress.pluginerror')
                            })
                            throw error;
                        })
                        .then(function() {
                            loginSuccess(true);
                        })
                });

                require(['login'], function(Login) {
                    removeVisalloLoading().then(function() {
                        Login.teardownAll();
                        Login.attachTo('#login', {
                            errorMessage: message,
                            errorMessageOptions: options,
                            toOpen: toOpen
                        });
                    })
                });
            } else {
                updateVisalloLoadingProgress('extensions', 0);
                var len = visalloPluginResources.afterAuth.length,
                    i = 0;
                Promise.all(visalloPluginResources.afterAuth.map(function(path) {
                    return Promise.require(path).then(function() {
                        updateVisalloLoadingProgress('extensions', ++i / len);
                    })
                }))
                    .catch(function(error) {
                        removeVisalloLoading('pluginerror');
                        throw error;
                    })
                    .then(function() {
                        updateVisalloLoadingProgress('userinterface');
                        loginSuccess(false);
                    })
            }
        }

        function loginSuccess(animate) {
            if (animate && (/^#?[a-z]+=/i).test(location.hash)) {
                window.location.reload();
            } else {
                withDataRequest.dataRequest('user', 'me').then(function(me) {
                    if (!userHasValidPrivileges(me)) {
                        $('#login .authentication').html(
                            '<span style="color: #D42B34;">' + i18n('visallo.login.missingPrivileges')
                            + '<p/><a class="logout" href="">' + i18n('visallo.login.logout') + '</a>'
                            + '</span>');
                        return;
                    }

                    require([
                        'moment',
                        'bootstrap',
                        'easing',
                        'jquery-scrollstop',
                        'bootstrap-datepicker',
                        'bootstrap-timepicker',
                        'util/formatters',
                        'util/visibility/util',
                        'util/handlebars/after_auth_helpers'
                    ], function(moment) {
                        var language = 'en';
                        try {
                            var languagePref = localStorage.getItem('language');
                            if (languagePref) {
                                language = languagePref;
                            }
                        } catch(langerror) { /*eslint no-empty:0 */ }
                        moment.locale(language);

                        // Default datepicker options
                        $.fn.datepicker.defaults.format = 'yyyy-mm-dd';
                        $.fn.datepicker.defaults.autoclose = true;

                        if (popoutDetails) {
                            visalloData.isFullscreen = true;
                            $('#login').remove();
                            require(['appFullscreenDetails'], function(comp) {
                                removeVisalloLoading().then(function() {
                                    if (event) {
                                        location.reload();
                                    } else {
                                        if (App) {
                                            App.teardownAll();
                                        }
                                        FullScreenApp = comp;
                                        FullScreenApp.teardownAll();
                                        FullScreenApp.attachTo('#app', {
                                            vertexIds: vertexIds,
                                            edgeIds: edgeIds,
                                            workspaceId: workspaceId
                                        });
                                    }
                                });
                            });
                        } else {
                            if (!animate) {
                                $('#login').remove();
                            }
                            require(['app'], function(comp) {
                                removeVisalloLoading().then(function() {
                                    App = comp;
                                    if (event) {
                                        location.reload();
                                    } else {
                                        if (FullScreenApp) {
                                            FullScreenApp.teardownAll();
                                        }
                                        App.teardownAll();
                                        var appOptions = {};
                                        if (toOpen && toOpen.type === 'ADD' && vertexIds.length) {
                                            appOptions.addVertexIds = toOpen;
                                        }
                                        if (toOpen && toOpen.type === 'ADMIN' && toOpen.section && toOpen.name) {
                                            appOptions.openAdminTool = _.pick(toOpen, 'section', 'name');
                                        }
                                        if (animate) {
                                            $('#login .authentication button.loading').removeClass('loading');
                                            appOptions.animateFromLogin = true;
                                        }

                                        App.attachTo('#app', appOptions);
                                        _.defer(function() {
                                            // Cache login in case server goes down
                                            require(['login']);
                                        });

                                        if (animate) {
                                            $('#login .logo').one(TRANSITION_END, function() {
                                                $('#login')
                                                    .find('.authentication').teardownAllComponents()
                                                    .end().teardownAllComponents();
                                            });
                                        }
                                    }
                                });
                            });
                        }
                    });
                });
            }
        }
    }

    function updateVisalloLoadingProgress(string, percentInProgress) {
        if (!progressBar) {
            progressBar = $('#visallo-loading-static .bar')[0];
            progressBarText = $('#visallo-loading-static span')[0];
        }
        if (progressBar.dataset.finished) return;

        var translatedString = i18n('visallo.loading.progress.' + string);

        if (arguments.length === 2 && percentInProgress < 1.0) {
            var segment = 1 / TOTAL_PROGRESS,
                inc = segment * percentInProgress;
            progressBarText.textContent = translatedString;
            progressBar.style.width = Math.round((progress / TOTAL_PROGRESS + inc) * 100) + '%';
        } else {
            progress++;
            progressBarText.textContent = translatedString;
            progressBar.style.width = Math.round(progress / TOTAL_PROGRESS * 100) + '%';
        }
    }

    function removeVisalloLoading(error) {
        if (error) {
            progressBar.dataset.finished = true;
            progressBarText.textContent = i18n('visallo.loading.progress.' + error);
            progressBarText.style.color = '#D42B34';
        } else {
            updateVisalloLoadingProgress('starting');
            return new Promise(function(fulfill) {
                _.delay(function() {
                    $('#visallo-loading-static').remove();
                    fulfill();
                }, 500)
            });
        }
    }

    function isPopoutUrl(url) {
        return F.vertexUrl.isFullscreenUrl(url);
    }

    function isAddUrl(url) {
        return (/#add=/).test(url);
    }

    function isMainApp(url) {
        return (/#\s*$/).test(url) || url.indexOf('#') === -1;
    }

});
