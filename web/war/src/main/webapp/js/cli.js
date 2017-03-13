define([], function() {
    'use strict';

    var globals = [
        {
            name: 'enableLiveReload',
            description: 'Enable liveReload when `grunt` is running, (persisted)',
            preferenceKey: 'liveReloadEnabled',
            value: function(enable) {
                if ('localStorage' in window) {
                    if (enable === true || typeof enable === 'undefined') {
                        console.debug('Enabling LiveReload...')
                        require([`//${location.host.replace(/:.*$/, '')}:35729/livereload.js`], function() {
                            console.debug('LiveReload successfully enabled');
                        });
                        localStorage.setItem('liveReloadEnabled', true);
                    } else {
                        console.debug('Disabling LiveReload')
                        localStorage.removeItem('liveReloadEnabled');
                    }
                }
            }
        },

        {
            name: 'enableHighContrast',
            description: 'Darken pane backgrounds for poor displays/projectors. (persisted)',
            preferenceKey: 'highcontrast',
            value: function(enable) {
                var stylesheet = document.querySelector('link[href*=visallo-contrast]');
                if (!enable && stylesheet) {
                    stylesheet.parentNode.removeChild(stylesheet);
                    localStorage.removeItem('highcontrast');
                }

                if (enable && !stylesheet) {
                    console.warn('Running high contrast stylesheet. "enableHighContrast(false)" to disable');
                    stylesheet = document.createElement('link');
                    stylesheet.rel = 'stylesheet';
                    stylesheet.href = '../css/visallo-contrast.css';
                    document.head.appendChild(stylesheet);
                    localStorage.setItem('highcontrast', true);
                }
            }
        },

        {
            name: 'enableGraphTracing',
            description: 'Enable Vertexium graph performance tracing',
            value: function(enable) {
                $(document).trigger('setPublicApi', { key: 'graphTraceEnable', obj: enable });
            }
        },

        {
            name: 'switchLanguage',
            description: 'Switch UI message bundle language: [en,es,de,fr,it,zh_TW] (persisted)',
            value: function(code) {
                if (!code) {
                    code = 'en';
                }
                var availableLocales = 'en es de fr it zh_TW'.split(' ');

                if (~availableLocales.indexOf(code)) {
                    var parts = code.split('_');
                    if (parts[0]) {
                        localStorage.setItem('language', parts[0]);
                    }
                    if (parts[1]) {
                        localStorage.setItem('country', parts[1]);
                    }
                    if (parts[2]) {
                        localStorage.setItem('variant', parts[2]);
                    }
                    location.reload();
                } else console.error('Available Locales: ' + availableLocales.join(', '));
            }
        },

        {
            name: 'enableComponentHighlighting',
            description: 'Enable component highlighting by mouse movement',
            value: function(enable) {
                require(['util/flight/componentHighlighter'], function(c) {
                    c.highlightComponents(enable);
                });
            }
        },

        {
            name: 'gremlins',
            description: 'Activate gremlins to stress test ui',
            value: function() {
                require(['gremlins'], function(gremlins) {
                    gremlins.createHorde()
                    .gremlin(gremlins.species.formFiller())
                    .gremlin(gremlins.species.clicker())
                    .gremlin(
                        gremlins.species.clicker()
                        .clickTypes(['click'])
                        .canClick(function(element) {
                            return $(element).is('button,a,li') &&
                                $(element).closest('.logout').length === 0;
                        }))
                    .gremlin(gremlins.species.typer())
                    .mogwai(gremlins.mogwais.fps())
                    .unleash({nb: 1000});
                })
            }
        }
    ];

    globals.forEach(function(global) {
        window[global.name] = global.value;

        if ('localStorage' in window) {
            if (global.preferenceKey &&
                localStorage.getItem(global.preferenceKey) &&
               _.isFunction(global.value)) {
                global.value(true);
            }
        }
    });

    var helpName = 'help';
    if (typeof window.help !== 'undefined') {
        helpName = 'visalloHelp';
        console.warn('Unable to create console help, already esists. Using visalloHelp');
    }

    window[helpName] = function() {
        console.group('Visallo Help')
            console.info('Descriptions of some of the debugging and global state objects');
            console.group('Global Helper Functions')
                _.sortBy(globals, 'name').forEach(function(global) {
                    console.log(global.name + ': ' + global.description);
                })
            console.groupEnd();
            console.group('Global Objects')
            console.log('visalloData: ' + 'Only Shared Global State');
            console.log('\t.currentUser: ' + 'Current user object');
            console.log('\t.currentWorkspaceId: ' + 'Current workspaceId');
            console.log('\t.selectedObjects: ' + 'Current object selection');
            console.log('\t\t.vertices: ' + 'Selected vertices');
            console.log('\t\t.vertexIds: ' + 'Selected vertexIds');
            console.log('\t\t.edges: ' + 'Selected edges');
            console.log('\t\t.edgeIds: ' + 'Selected edgeIds');
            console.groupEnd();
        console.groupEnd();
    }
})
