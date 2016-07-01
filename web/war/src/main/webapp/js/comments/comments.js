define([
    'flight/lib/component',
    'hbs!./template',
    './dropdowns/commentForm/commentForm',
    'util/withCollapsibleSections',
    'util/vertex/formatters',
    'util/withDataRequest',
    'util/popovers/propertyInfo/withPropertyInfo',
    'd3'
], function(
    defineComponent,
    template,
    CommentForm,
    withCollapsibleSections,
    F,
    withDataRequest,
    withPropertyInfo,
    d3) {
    'use strict';

    var VISIBILITY_NAME = 'http://visallo.org#visibilityJson';

    return defineComponent(Comments, withCollapsibleSections, withDataRequest, withPropertyInfo);

    function toCommentTree(properties) {
        var comments = _.chain(properties)
                .where({ name: 'http://visallo.org/comment#entry' })
                .sortBy(function(p) {
                    return p.key;
                })
                .value(),
            maxDepth = 1,
            total = comments.length,
            userIds = _.unique(_.map(comments, function(c) {
                return c.metadata['http://visallo.org#modifiedBy'];
            })),
            commentsByKey = _.indexBy(_.map(comments, function(c) {
                return [c, []];
            }), function(a) {
                return a[0].key;
            }),
            rootComments = _.filter(comments, function(p) {
                return !p.metadata['http://visallo.org/comment#path'];
            }),
            roots = [];

        comments.forEach(function(comment) {
            var path = comment.metadata['http://visallo.org/comment#path'];
            if (path) {
                var components = path.split('/');
                maxDepth = Math.max(maxDepth, components.length + 1);
                components.forEach(function(key, i, components) {
                    var value = commentsByKey[key];
                    if (!value) {
                        total++;
                        value = commentsByKey[key] = [{
                            key: key,
                            redacted: true,
                            metadata: {
                                'http://visallo.org#modifiedDate': ''
                            }
                        }, []];
                        if (i === 0) {
                            roots.push(value);
                        } else {
                            commentsByKey[components[i - 1]][1].push(value);
                        }
                    }
                    if (i === (components.length - 1)) {
                        value[1].push(commentsByKey[comment.key]);
                    }
                });
            } else {
                roots.push(commentsByKey[comment.key])
            }
        });

        return {
            roots: roots,
            userIds: userIds,
            maxDepth: maxDepth,
            total: total
        };
    }

    function getCreated(p) {
        // setProperty would set key in servers timezone instead of UTC
        var isLegacyKeyInServerTimezone = !(/Z$/).test(p.key),
            date = null;
        if (isLegacyKeyInServerTimezone) {
            var time = new Date(p.key + 'Z').getTime();
            if (isNaN(time)) {
                return undefined;
            }
            date = F.date.utc(time);
        } else {
            date = F.date.local(p.key);
        }
        var millis = date && date.getTime();
        if (millis && !isNaN(millis)) {
            return millis;
        }
        return undefined;
    }

    function isEdited(created, modified) {
        var equalTolerance = 1000;
        return (modified - created) > equalTolerance
    }

    function Comments() {

        this.attributes({
            data: null,
            ignoreUpdateModelNotImplemented: true
        });

        this.after('initialize', function() {
            this.on('editComment', this.onEditComment);

            this.type = F.vertex.isEdge(this.attr.data) ? 'edge' : 'vertex';
            if (this.type === 'vertex') {
                this.on(document, 'verticesUpdated', this.onVerticesUpdated);
            } else if (this.type === 'edge') {
                this.on(document, 'edgesUpdated', this.onEdgesUpdated);
            }

            this.on('commentOnSelection', this.onCommentOnSelection);
            this.on('editProperty', this.onEditProperty);
            this.on('deleteProperty', this.onDeleteProperty);

            this.$node.html(template({}));
            this.update();
        });

        this.onCommentOnSelection = function(event, data) {
            this.trigger('editComment', {
                sourceInfo: data
            });
        };

        this.onVerticesUpdated = function(event, data) {
            var vertex = data && data.vertices && _.findWhere(data.vertices, { id: this.attr.data.id });
            if (vertex) {
                this.attr.data = vertex;
                this.update();
            }
        };

        this.onEdgesUpdated = function(event, data) {
            var edge = data && data.edges && _.findWhere(data.edges, { id: this.attr.data.id });
            if (edge) {
                this.attr.data = edge;
                this.update();
            }
        };

        this.renderCommentLevel = function(maxDepth, level, selection) {
            var self = this;

            if (level > maxDepth) {
                return;
            }

            selection.enter()
                .append('li').attr('class', 'comment comment-' + level)
                .call(function() {
                    this.append('div').attr('class', 'wrap')
                        .call(function() {
                            this.append('div').attr('class', 'comment-text')
                            this.append('span').attr('class', 'visibility')
                            this.append('span').attr('class', 'user')
                            this.append('span').attr('class', 'date')
                            this.append('button').attr('class', 'info')
                            this.append('button').attr('class', 'replies btn-link btn')
                        })
                    this.append('ul').attr('class', 'collapsed');
                })

            selection.order()
            selection.select('.comment-text')
                .classed('redacted', function(p) {
                    return p[0].redacted || false;
                })
                .each(function() {
                    var p = d3.select(this).datum();
                    if (p[0].redacted) {
                        $(this).html(
                            i18n('detail.comments.missing') +
                            '<p>' +
                            i18n('detail.comments.missing.explanation') +
                            '</p>'
                        );
                    } else {
                        $(this).html(_.escape('\n' + p[0].value).replace(/\r?\n+/g, '<p>'));
                    }
                });
            selection.select('.visibility').each(function(p) {
                this.textContent = '';
                if (p[0].redacted) {
                    $(this).hide();
                    return;
                }
                F.vertex.properties.visibility(
                    this,
                    { value: p[0].metadata && p[0].metadata[VISIBILITY_NAME] },
                    self.attr.data.id
                );
            })
            selection.select('.user').each(function(p, i) {
                var $this = $(this);
                if (p[0].redacted) {
                    $this.hide();
                } else {
                    var currentUserId = $this.data('userId'),
                        newUserId = p[0].metadata['http://visallo.org#modifiedBy'],
                        currentText = $this.text(),
                        loading = i18n('detail.comments.user.loading');
                    if ((!currentUserId || currentUserId === newUserId) &&
                        (!currentText || currentText === loading)) {
                        $this.data('userId', newUserId).text(loading);
                    }
                }
            });
            selection.select('.date')
                .attr('style', function(p) {
                    return p[0].redacted ? 'display:none' : undefined;
                })
                .text(function(p) {
                    if (p[0].redacted) {
                        return '';
                    }
                    var modified = p[0].metadata['http://visallo.org#modifiedDate'],
                        created = getCreated(p[0]) || modified,
                        relativeString = modified && F.date.relativeToNow(F.date.utc(modified));

                    if (relativeString) {
                        if (isEdited(created, modified)) {
                            return i18n('detail.comments.date.edited', relativeString);
                        }
                        return relativeString;
                    }
                    return '';
                })
                .attr('title', function(p) {
                    if (p[0].redacted) {
                        return '';
                    }
                    var modified = p[0].metadata['http://visallo.org#modifiedDate'],
                        created = getCreated(p[0]) || modified,
                        modifiedStr = modified && F.date.dateTimeString(modified) || '',
                        createdStr = created && F.date.dateTimeString(created) || '';

                    if (isEdited(created, modified)) {
                        return i18n('detail.comments.date.hover.edited', createdStr, modifiedStr);
                    }
                    return modifiedStr;
                });
            selection.select('.replies')
                .attr('style', function(p) {
                    if (p[1].length === 0) {
                        return 'display:none';
                    }
                })
                .classed('open', function() {
                    return !$(this).closest('li').children('ul').hasClass('collapsed');
                })
                .text(function(p) {
                    return F.string.plural(p[1].length, 'reply', 'replies');
                })
                .on('click', function(property) {
                    $(this).toggleClass('open')
                        .closest('li').children('ul').toggleClass('collapsed')
                });
            selection.select('.info')
                .attr('style', function(p) {
                    return p[0].redacted ? 'display:none' : undefined;
                })
                .on('click', function(property) {
                    if (property[0].redacted) {
                        return;
                    }
                    self.showPropertyInfo(this, self.attr.data, property[0]);
                });
            selection.exit()
                .call(function() {
                    this.select('.info').each(function() {
                        $(this).teardownAllComponents();
                    })
                })
                .remove();

            var nextLevel = level + 1,
                subselection = selection
                    .select(function() {
                        return $(this).children('ul')[0];
                    })
                    .selectAll('.comment-' + nextLevel)
                    .data(function(p) {
                        return p[1] || [];
                    });

            this.renderCommentLevel(maxDepth, nextLevel, subselection);
        };

        this.update = function() {
            var self = this,
                commentsTreeResponse = toCommentTree(this.attr.data.properties),
                commentsTree = commentsTreeResponse.roots,
                selection = d3.select(this.$node.find('.comment-content ul').get(0))
                    .selectAll('.comment-0')
                    .data(commentsTree)

            this.renderCommentLevel(commentsTreeResponse.maxDepth, 0, selection);
            this.dataRequest('user', 'getUserNames', commentsTreeResponse.userIds)
                .done(function(users) {
                    var usersById = _.object(commentsTreeResponse.userIds, users);
                    self.$node.find('.user').each(function() {
                        $(this).text(usersById[$(this).data('userId')]);
                    })
                });

            this.$node.find('.badge').text(
                F.number.pretty(commentsTreeResponse.total)
            );
            this.$node.find('.collapsible-header').toggle(commentsTreeResponse.total > 0);
        };

        this.onEditProperty = function(event, data) {
            this.onEditComment(event, { path: data.path, comment: data.property });
        };

        this.onDeleteProperty = function(event, data) {
            var self = this;
            this.dataRequest(this.type, 'deleteProperty',
                this.attr.data.id, data.property
            ).then(function() {
                self.hidePropertyInfo($(event.target));
            });
        };

        this.onEditComment = function(event, data) {
            var root = $('<div class="underneath">'),
                comment = data && data.comment,
                path = data && data.path,
                sourceInfo = data && data.sourceInfo,
                commentRow = (comment || path) && $(event.target).closest('li').children('ul');

            if (commentRow && commentRow.length) {
                root.insertBefore(commentRow)
                if (path) {
                    commentRow.removeClass('collapsed')
                }
            } else {
                root.appendTo(this.$node.find('.comment-content'));
            }

            this.$node.find('.collapsible').addClass('expanded');

            CommentForm.teardownAll();
            CommentForm.attachTo(root, {
                data: this.attr.data,
                type: this.type,
                path: path,
                sourceInfo: sourceInfo,
                comment: comment
            });
        };

    }
});
