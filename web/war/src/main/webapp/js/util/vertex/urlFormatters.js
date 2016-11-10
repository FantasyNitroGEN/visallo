
define([], function() {
    'use strict';

    var URL_TYPES = {
            FULLSCREEN: 'v',
            ADMIN: 'admin',
            REDIRECT: 'redirect',
            TOOLS: 'tools'
        },
        V = {
            url: function(vertices, workspaceId) {
                return window.location.href.replace(/#.*$/, '') +
                    '#v=' + _.map(vertices, function(v) {
                        if (_.isObject(v) && 'type' in v) {
                            return encodeURIComponent(v.type.substring(0, 1) + v.id);
                        }
                        return encodeURIComponent(_.isString(v) ? v : v.id);
                    }).join(',') +
                    '&w=' + encodeURIComponent(workspaceId);
            },

            fragmentUrl: function(vertices, workspaceId) {
                return V.url(vertices, workspaceId).replace(/^.*#/, '#');
            },

            isFullscreenUrl: function(url) {
                var toOpen = V.parametersInUrl(url);

                return toOpen &&
                    toOpen.type === 'FULLSCREEN' &&
                    ((toOpen.vertexIds && toOpen.vertexIds.length) ||
                    (toOpen.edgeIds && toOpen.edgeIds.length));
            },

            parametersInUrl: function(url) {
                var type = _.invert(URL_TYPES),
                    match = url.match(/#(v|admin|redirect|tools)=(.+?)(?:&w=(.*))?$/);

                if (match && match.length === 4) {
                    if (match[1] === URL_TYPES.ADMIN) {
                        var tool = match[2].split(':');
                        if (tool.length !== 2) {
                            return null;
                        }

                        return _.extend(_.mapObject({
                            section: tool[0],
                            name: tool[1]
                        }, function(v) {
                            return decodeURIComponent(v).replace(/\+/g, ' ');
                        }), { type: type[match[1]] });
                    }

                    if (match[1] === URL_TYPES.REDIRECT) {
                        return {
                            type: type[match[1]],
                            redirectUrl: match[2]
                        }
                    }

                    if (match[1] === URL_TYPES.TOOLS) {
                        const tools = _.unique(match[2].trim().split(','));
                        const toolsWithOptions = _.object(tools.map(tool => {
                            const optionsIndex = tool.indexOf('&');
                            var name = tool;
                            var options = {};
                            if (optionsIndex > 0) {
                                name = tool.substring(0, optionsIndex);
                                options = unserialize(tool.substring(optionsIndex + 1))
                            }
                            return [name, options || {}]
                        }))

                        return {
                            type: type[match[1]],
                            tools: toolsWithOptions
                        };
                    }

                    var objects = _.map(match[2].split(','), function(v) {
                            return decodeURIComponent(v);
                        }),
                        data = _.chain(objects)
                            .groupBy(function(o) {
                                var match = o.match(/^(v|e).*/);
                                if (match) {
                                    if (match[1] === 'v') return 'vertexIds';
                                    if (match[1] === 'e') return 'edgeIds';
                                }
                                return 'vertexIds';
                            })
                            .mapObject(function(ids) {
                                return ids.map(function(val) {
                                    return val.substring(1);
                                });
                            })
                            .value();

                    return _.extend({ vertexIds: [], edgeIds: [] }, data, {
                        workspaceId: decodeURIComponent(match[3] || ''),
                        type: type[match[1]]
                    });
                }
                return null;
            }
    };

    return $.extend({}, { vertexUrl: V });


    function unserialize(str) {
      str = decodeURIComponent(str);
      var chunks = str.split('&'),
          obj = {};
      chunks.forEach(function(c) {
          var split = c.split('=', 2);
          obj[split[0]] = split[1];
      })
      return obj;
    }
});
