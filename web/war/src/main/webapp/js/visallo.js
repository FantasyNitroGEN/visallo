require([
    'jquery',
    'es5shim',
    'es5sham',
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
         es5shim,
         es5sham,
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
        TOTAL_PROGRESS = 5,
        MAX_RESIZE_TRIGGER_INTERVAL = 250,
        App, FullScreenApp, F, withDataRequest;

    $(function() {
        updateVisalloLoadingProgress('Dependencies');

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

            require([
                'util/messages',
                'util/vertex/urlFormatters',
                'util/withDataRequest',
                'util/handlebars/before_auth_helpers'
            ], function(i18n, _F, _withDataRequest) {
                updateVisalloLoadingProgress('Utilities');
                window.i18n = i18n;
                F = _F;
                withDataRequest = _withDataRequest;
                loadApplicationTypeBasedOnUrlHash();
            });
        }
    });

    /**
     * Switch between visallo and visallo-fullscreen-details based on url hash
     */
    function loadApplicationTypeBasedOnUrlHash(e) {
        var toOpen = F.vertexUrl.parametersInUrl(location.href),

            ids = toOpen && toOpen.vertexIds,

            workspaceId = toOpen && toOpen.workspaceId,

            // Is this the popoout details app? ids passed to hash?
            popoutDetails = !!(toOpen && toOpen.type === 'FULLSCREEN' && ids.length),

            // If this is a hash change
            event = e && e.originalEvent,

            // Is this the default visallo application?
            mainApp = !popoutDetails;

        if (event && isAddUrl(event.oldURL) && isMainApp(event.newURL)) {
            return;
        }

        if (event && isPopoutUrl(event.oldURL) && isPopoutUrl(event.newURL)) {
            return $('#app').trigger('vertexUrlChanged', {
                graphVertexIds: ids,
                workspaceId: workspaceId
            });
        }

        withDataRequest.dataRequest('user', 'me')
            .then(function() {
                attachApplication(false);
            })
            .catch(function() {
                attachApplication(true, '', {});
            })

        function attachApplication(loginRequired, message, options) {
            updateVisalloLoadingProgress('Extensions');

            if (!event) {
                $('html')
                    .toggleClass('fullscreenApp', mainApp)
                    .toggleClass('fullscreenDetails', popoutDetails);

                window.isFullscreenDetails = popoutDetails;
            }

            visalloData.isFullscreen = false;

            if (loginRequired) {
                require(['../plugins-before-auth'], function() {
                    updateVisalloLoadingProgress('User Interface');

                    $(document).one('loginSuccess', function() {
                        document.addEventListener('pluginsLoaded', function loaded() {
                            document.removeEventListener('pluginsLoaded', loaded);
                            loginSuccess(true);
                        }, false);
                        document.dispatchEvent(new Event('readyForPlugins'));
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
                })
            } else {
                document.addEventListener('pluginsLoaded', function loaded() {
                    updateVisalloLoadingProgress('User Interface');

                    document.removeEventListener('pluginsLoaded', loaded);
                    loginSuccess(false);
                }, false);
                document.dispatchEvent(new Event('readyForPlugins'));
            }
        }

        function loginSuccess(animate) {
            if (animate && (/^#?[a-z]+=/i).test(location.hash)) {
                window.location.reload();
            } else {
                withDataRequest.dataRequest('user', 'me').then(function() {

                    require([
                        'moment',
                        'bootstrap',
                        'jqueryui',
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
                                            graphVertexIds: ids,
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
                                        if (toOpen && toOpen.type === 'ADD' && ids.length) {
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

    function updateVisalloLoadingProgress(string) {
        if (!progressBar) {
            progressBar = $('#visallo-loading-static .bar')[0];
            progressBarText = $('#visallo-loading-static span')[0];
        }

        progress++;
        progressBarText.textContent = string;
        progressBar.style.width = Math.round(progress / TOTAL_PROGRESS * 100) + '%';
    }

    function removeVisalloLoading() {
        updateVisalloLoadingProgress('Starting');
        return new Promise(function(fulfill) {
            _.delay(function() {
                $('#visallo-loading-static').remove();
                fulfill();
            }, 500)
        });
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
