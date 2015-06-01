require([
    'jquery',
    'jqueryui',
    'bootstrap',
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
    'timezone-js',
    'easing',
    'jquery-scrollstop',
    'bootstrap-datepicker',
    'bootstrap-timepicker',
    'util/jquery.flight',
    'util/jquery.removePrefixedClasses',
    'util/promise'
],
function(jQuery,
         jQueryui,
         bootstrap,
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
         Privileges,
         timezoneJS) {
    'use strict';

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

    timezoneJS.timezone.zoneFileBasePath = 'tz';
    timezoneJS.timezone.defaultZoneFile = ['northamerica', 'southamerica'];
    timezoneJS.timezone.init({ async: true });

    var progress = 0,
        progressBar = null,
        progressBarText = null,
        TOTAL_PROGRESS = 4,
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

            // Default datepicker options
            $.fn.datepicker.defaults.format = 'yyyy-mm-dd';
            $.fn.datepicker.defaults.autoclose = true;

            Data.attachTo(document);
            Visibility.attachTo(document);
            Privileges.attachTo(document);
            document.dispatchEvent(new Event('readyForPlugins'));
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
                'util/handlebars/helpers'
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
            updateVisalloLoadingProgress('User Interface');

            if (!event) {
                $('html')
                    .toggleClass('fullscreenApp', mainApp)
                    .toggleClass('fullscreenDetails', popoutDetails);

                window.isFullscreenDetails = popoutDetails;
            }

            visalloData.isFullscreen = false;

            if (loginRequired) {
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
            } else if (popoutDetails) {
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
                $('#login').remove();
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
                            App.attachTo('#app', appOptions);
                            _.defer(function() {
                                // Cache login in case server goes down
                                require(['login']);
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
