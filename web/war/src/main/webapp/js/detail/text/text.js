define([
    'flight/lib/component',
    'util/vertex/formatters',
    'configuration/plugins/registry',
    'util/css-stylesheet',
    'util/withDataRequest',
    'util/privileges',
    'util/jquery.withinScrollable',
    'colorjs',
    'hbs!./transcriptEntries',
    'tpl!util/alert',
    'require',
    'sf'
], function(
    defineComponent,
    F,
    registry,
    stylesheet,
    withDataRequest,
    Privileges,
    jqueryWithinScrollable,
    colorjs,
    transcriptEntriesTemplate,
    alertTemplate,
    require,
    sf) {
    'use strict';

    var HIGHLIGHT_STYLES = [
            { name: 'None', selector: 'none' },
            { name: 'Icons', selector: 'icons' },
            { name: 'Underline', selector: 'underline' },
            { name: 'Colors', selector: 'colors' }
        ],
        DEFAULT = 2,
        useDefaultStyle = true,
        TEXT_PROPERTIES = [
            'http://visallo.org#videoTranscript',
            'http://visallo.org#text'
        ],
        rangeUtils,
        d3;

    registry.documentExtensionPoint('org.visallo.detail.text', 'Replace Extracted Text with custom component', function(e) {
        return _.isFunction(e.shouldReplaceTextSectionForVertex) && _.isString(e.componentPath);
    })

    return defineComponent(Text, withDataRequest);

    function Text() {

        this.attributes({
            resolvableSelector: '.vertex, .property',
            resolvedSelector: '.vertex.resolved',
            textSelector: '.text',
            textContainerHeaderSelector: '.org-visallo-texts .text-section h1',
            model: null
        });

        this.after('teardown', function() {
            if (this.scrollNode) {
                this.scrollNode.off('scrollstop scroll');
            }
        });

        this.after('initialize', function() {
            var self = this;

            this.on('mousedown mouseup click dblclick contextmenu', this.trackMouse);

            this.updateEntityAndArtifactDraggablesNoDelay = this.updateEntityAndArtifactDraggables;
            this.updateEntityAndArtifactDraggables = _.throttle(this.updateEntityAndArtifactDraggables.bind(this), 250);

            this.model = this.attr.model;
            this.on('modelUpdated', function(event, data) {
                this.model = data.model;
                this.updateText();
            });
            this.on('click', {
                resolvableSelector: this.onResolvableClick,
                textContainerHeaderSelector: this.onTextHeaderClicked
            });
            this.on('focusOnSnippet', this.onFocusOnSnippet);
            //this.on('contextmenu', {
                //resolvedSelector: this.onResolvedContextClick
            //});
            this.on('copy cut', {
                textSelector: this.onCopyText
            });
            this.on('dropdownClosed', this.onDropdownClosed);
            this.on(document, 'textUpdated', this.onTextUpdated);

            this.scrollNode = self.$node.scrollParent()
                .css('position', 'relative')
                .on('scrollstop', self.updateEntityAndArtifactDraggables)
                .on('scroll', self.updateEntityAndArtifactDraggables);
            this.updateText();
            this.applyHighlightStyle();
            this.updateEntityAndArtifactDraggables();
        });

        this.onFocusOnSnippet = function(event, data) {
            var self = this;
            Promise.resolve(this.updatingPromise)
                .then(function() {
                    return self.openText(data.textPropertyKey)
                })
                .then(function() {
                    var $text = self.$node.find('.ts-' +
                            F.className.to(data.textPropertyKey) + ' .text'),
                        $transcript = $text.find('.av-times'),
                        focusOffsets = data.offsets;

                    if ($transcript.length) {
                        var start = F.number.offsetValues(focusOffsets[0]),
                            end = F.number.offsetValues(focusOffsets[1]),
                            $container = $transcript.find('dd').eq(start.index);

                        rangeUtils.highlightOffsets($container.get(0), [start.offset, end.offset]);
                    } else {
                        rangeUtils.highlightOffsets($text.get(0), focusOffsets);
                    }
                })
                .done();
        };

        this.onCopyText = function(event) {
            var selection = getSelection(),
                target = event.target;

            if (!selection.isCollapsed && selection.rangeCount === 1) {

                var data = this.transformSelection(selection);
                if (data.startOffset && data.endOffset) {
                    this.trigger('copydocumenttext', data);
                }
            }
        };

        this.onTextHeaderClicked = function(event) {
            var $section = $(event.target)
                .closest('.text-section')
                .siblings('.expanded').removeClass('expanded')
                .end(),
                propertyKey = $section.data('key');

            if ($section.hasClass('expanded')) {
                $section.removeClass('expanded');
            } else {
                this.openText(propertyKey);
            }
        };

        this.onTextUpdated = function(event, data) {
            if (data.vertexId === this.attr.model.id) {
                this.updateText();
            }
        };

        this.formatTimeOffset = function(time) {
            return sf('{0:h:mm:ss}', new sf.TimeSpan(time));
        };

        this.trackMouse = function(event) {
            var $target = $(event.target);

            if ($target.is('.resolved,.vertex')) {
                if (event.type === 'mousedown') {
                    rangeUtils.clearSelection();
                }
            }

            if (event.type === 'contextmenu') {
                event.preventDefault();
            }

            if (~'mouseup click dblclick contextmenu'.split(' ').indexOf(event.type)) {
                this.mouseDown = false;
            } else {
                this.mouseDown = true;
            }

            if ($(event.target).closest('.opens-dropdown').length === 0 &&
                $(event.target).closest('.underneath').length === 0 &&
                !($(event.target).parent().hasClass('currentTranscript')) &&
                !($(event.target).hasClass('alert alert-error'))) {
                if (event.type === 'mouseup' || event.type === 'dblclick') {
                    this.handleSelectionChange();
                }
            }
        };

        this.updateText = function() {
            var self = this;

            this.updatingPromise = Promise.resolve(this.internalUpdateText())
                .then(function() {
                    self.updateEntityAndArtifactDraggables();
                })
                .catch(function(e) {
                    console.error(e);
                    throw e;
                })

            return this.updatingPromise;
        }

        this.internalUpdateText = function internalUpdateText(_d3, _rangeUtils) {
            var self = this;

            if (!d3 && _d3) d3 = _d3;
            if (!rangeUtils && _rangeUtils) rangeUtils = _rangeUtils;

            if (!d3) {
                return Promise.all([
                    Promise.require('d3'),
                    Promise.require('util/range')
                ]).then(function(results) {
                    return internalUpdateText.apply(self, results);
                })
            }

            var scrollParent = this.$node.scrollParent(),
                scrollTop = scrollParent.scrollTop(),
                expandedKey = this.$node.find('.text-section.expanded').data('key'),
                textProperties = _.filter(this.model.properties, function(p) {
                    return _.some(TEXT_PROPERTIES, function(name) {
                        return name === p.name;
                    });
                });

            this.node.classList.add('org-visallo-texts')

            d3.select(self.node)
                .selectAll('section.text-section')
                .data(textProperties)
                .call(function() {
                    this.enter()
                        .append('section')
                        .attr('class', 'text-section collapsible')
                        .call(function() {
                            this.append('h1').attr('class', 'collapsible-header')
                                .call(function() {
                                    this.append('strong')
                                    this.append('span').attr('class', 'badge')
                                })
                            this.append('div').attr('class', 'text');
                        })

                    this.attr('data-key', function(p) {
                            return p.key;
                        })
                        .each(function() {
                            var p = d3.select(this).datum();
                            $(this).removePrefixedClasses('ts-').addClass('ts-' + F.className.to(p.key));
                        })
                    this.select('h1 strong').text(function(p) {
                        var textDescription = 'http://visallo.org#textDescription';
                        return p[textDescription] || p.metadata[textDescription] || p.key;
                    })

                    this.exit().remove();
                });

            if (textProperties.length) {
                if (this.attr.focus) {
                    return this.openText(this.attr.focus.textPropertyKey)
                        .then(function() {
                            var $text = self.$node.find('.ts-' +
                                    F.className.to(self.attr.focus.textPropertyKey) + ' .text'),
                                $transcript = $text.find('.av-times'),
                                focusOffsets = self.attr.focus.offsets;

                            if ($transcript.length) {
                                var start = F.number.offsetValues(focusOffsets[0]),
                                    end = F.number.offsetValues(focusOffsets[1]),
                                    $container = $transcript.find('dd').eq(start.index);

                                rangeUtils.highlightOffsets($container.get(0), [start.offset, end.offset]);
                            } else {
                                rangeUtils.highlightOffsets($text.get(0), focusOffsets);
                            }
                            self.attr.focus = null;
                        });
                } else if (expandedKey || textProperties.length === 1) {
                    return this.openText(expandedKey || textProperties[0].key, {
                        scrollToSection: textProperties.length !== 1
                    }).then(function() {
                        scrollParent.scrollTop(scrollTop);
                    });
                } else if (textProperties.length > 1) {
                    return this.openText(textProperties[0].key, {
                        expand: false
                    });
                }
            }
        };

        this.openText = function(propertyKey, options) {
            var self = this,
                expand = !options || options.expand !== false,
                $section = this.$node.find('.ts-' + F.className.to(propertyKey)),
                isExpanded = $section.is('.expanded'),
                $badge = $section.find('.badge'),
                selection = getSelection(),
                range = selection.rangeCount && selection.getRangeAt(0),
                hasSelection = isExpanded && range && !range.collapsed,
                hasOpenForm = isExpanded && $section.find('.underneath').length;

            if (hasSelection || hasOpenForm) {
                this.reloadText = this.openText.bind(this, propertyKey, options);
                return Promise.resolve();
            }

            if (expand) {
                $section.closest('.texts').find('.loading').removeClass('loading');
                $badge.addClass('loading');
            }

            if (this.openTextRequest && this.openTextRequest.abort) {
                this.openTextRequest.abort();
            }

            var extensions = _.filter(registry.extensionsForPoint('org.visallo.detail.text'), function(e) {
                    return e.shouldReplaceTextSectionForVertex(self.model);
                }),
                textPromise;

            if (extensions.length > 1) {
                console.warn('Multiple extensions wanting to override text', extensions);
            }

            if (extensions.length) {
                textPromise = Promise.require(extensions[0].componentPath)
                    .then(function(Text) {
                        Text.attachTo($section.find('.text'), {
                            vertex: self.model,
                            propertyKey: propertyKey
                        });
                    })
                    .catch(function() {
                        $section.find('.text').text('Error loading text');
                    })
            } else {
                this.openTextRequest = this.dataRequest('vertex', 'highlighted-text', this.model.id, propertyKey);

                textPromise = this.openTextRequest
                    .catch(function() {
                        return '';
                    })
                    .then(function(artifactText) {
                        var html = self.processArtifactText(artifactText);
                        if (expand) {
                            $section.find('.text').html(html);
                        }
                    });
            }

            return textPromise
                .then(function() {
                    if (expand) {
                        $section.addClass('expanded');
                        $badge.removeClass('loading');

                        self.updateEntityAndArtifactDraggablesNoDelay();
                        if (!options || options.scrollToSection !== false) {
                            self.scrollToRevealSection($section);
                        }
                    }
                })
        };

        this.processArtifactText = function(text) {
            var self = this,
                warningText = i18n('detail.text.none_available');

            // Looks like JSON ?
            if (/^\s*{/.test(text)) {
                var json;
                try {
                    json = JSON.parse(text);
                } catch(e) { /*eslint no-empty:0*/ }

                if (json && !_.isEmpty(json.entries)) {
                    //this.currentTranscript = json;
                    return transcriptEntriesTemplate({
                        entries: _.map(json.entries, function(e) {
                            return {
                                millis: e.start || e.end,
                                time: (_.isUndefined(e.start) ? '' : self.formatTimeOffset(e.start)) +
                                        ' - ' +
                                      (_.isUndefined(e.end) ? '' : self.formatTimeOffset(e.end)),
                                text: e.text
                            };
                        })
                    });
                } else if (json) {
                    text = null;
                    warningText = i18n('detail.transcript.none_available');
                }
            }

            return !text ? alertTemplate({ warning: warningText }) : this.normalizeString(text);
        };

        this.normalizeString = function(text) {
            return text.replace(/(\n+)/g, '<br><br>$1');
        };

        this.onDropdownClosed = function(event, data) {
            var self = this;
            _.defer(function() {
                self.disableSelection = false;
                self.checkIfReloadNeeded();
            })
        };

        this.checkIfReloadNeeded = function() {
            if (this.reloadText) {
                var func = this.reloadText;
                this.reloadText = null;
                func();
            }
        };

        this.onResolvableClick = function(event) {
            var self = this,
                $target = $(event.target);

            if ($target.is('.underneath') || $target.parents('.underneath').length) {
                return;
            }

            require(['util/actionbar/actionbar'], function(ActionBar) {
                ActionBar.teardownAll();

                var $text = $target.closest('.text'),
                    $textOffset = $text.closest('.nav-with-background').offset();

                if ($target.hasClass('resolved')) {
                    var info = $target.data('info');

                    ActionBar.attachTo($target, {
                        alignTo: 'node',
                        alignWithin: $text,
                        hideTopThreshold: $textOffset && $textOffset.top,
                        actions: $.extend({
                            Open: 'open',
                            Fullscreen: 'fullscreen'
                        }, Privileges.canEDIT && info.termMentionFor === 'VERTEX' && !F.vertex.isPublished(info) ? {
                            Unresolve: 'unresolve'
                        } : {})
                    });

                    self.off('open')
                    self.on('open', function(event) {
                        event.stopPropagation();
                        self.trigger('selectObjects', { vertexIds: $target.data('info').resolvedToVertexId });
                    })
                    self.off('fullscreen')
                    self.on('fullscreen', function(event) {
                        event.stopPropagation();
                        self.trigger('openFullscreen', { vertices: $target.data('info').resolvedToVertexId });
                    })
                    self.off('unresolve')
                    self.on('unresolve', function(event) {
                        event.stopPropagation();
                        _.defer(self.dropdownEntity.bind(self), false, $target);
                    });

                } else if (Privileges.canEDIT) {

                    ActionBar.attachTo($target, {
                        alignTo: 'node',
                        alignWithin: $text,
                        hideTopThreshold: $textOffset && $textOffset.top,
                        actions: {
                            Entity: 'resolve',
                            Property: 'property'
                        }
                    });

                    self.off('resolve');
                    self.on('resolve', function(event) {
                        _.defer(self.dropdownEntity.bind(self), false, $target);
                        event.stopPropagation();
                    })
                    self.off('property');
                    self.on('property', function(event) {
                        event.stopPropagation();
                        _.defer(function() {
                            self.dropdownProperty($target, null, $target.text());
                        })
                    })
                }
            });
        };

        this.handleSelectionChange = _.debounce(function() {
            var sel = window.getSelection(),
                text = sel && sel.rangeCount === 1 ? $.trim(sel.toString()) : '';

            if (this.disableSelection) {
                return;
            }
            if (text && text.length > 0) {
                var anchor = $(sel.anchorNode),
                    focus = $(sel.focusNode),
                    is = '.detail-pane .text';

                // Ignore outside content text
                if (anchor.parents(is).length === 0 || focus.parents(is).length === 0) {
                    this.checkIfReloadNeeded();
                    return;
                }

                // Ignore if too long of selection
                var wordLength = text.split(/\s+/).length;
                if (wordLength > 10) {
                    return;
                }

                if (sel.rangeCount === 0) {
                    this.checkIfReloadNeeded();
                    return;
                }

                var range = sel.getRangeAt(0),
                    // Avoid adding dropdown inside of entity
                    endContainer = range.endContainer;

                while (/vertex/.test(endContainer.parentNode.className)) {
                    if (/text/.test(endContainer.parentNode.className)) break;
                    endContainer = endContainer.parentNode;
                }

                var self = this,
                    selection = sel && {
                        anchor: sel.anchorNode,
                        focus: sel.focusNode,
                        anchorOffset: sel.anchorOffset,
                        focusOffset: sel.focusOffset,
                        range: sel.rangeCount && sel.getRangeAt(0).cloneRange()
                    };

                // Don't show action bar if dropdown opened
                if (this.$node.find('.text.dropdown').length) return;

                if (Privileges.missingEDIT) return;

                var $text = anchor.closest(is),
                    $textOffset = $text.closest('.nav-with-background').offset();
                require(['util/actionbar/actionbar'], function(ActionBar) {
                    ActionBar.teardownAll();
                    ActionBar.attachTo(self.node, {
                        alignTo: 'textselection',
                        alignWithin: $text,
                        hideTopThreshold: $textOffset && $textOffset.top,
                        actions: {
                            Entity: 'resolve',
                            Property: 'property',
                            Comment: 'comment'
                        }
                    });

                    self.off('comment')
                    self.on('comment', function(event) {
                        event.stopPropagation();

                        var data = self.transformSelection(sel);
                        if (data.startOffset && data.endOffset) {
                            self.trigger('commentOnSelection', data);
                        }
                    })
                    self.off('property')
                    self.on('property', function(event) {
                        event.stopPropagation();
                        _.defer(function() {
                            self.dropdownProperty(getNode(endContainer), sel, text);
                        })
                    })
                    self.off('resolve')
                    self.on('resolve', function(event) {
                        event.stopPropagation();

                        self.dropdownEntity(true, getNode(endContainer), selection, text);
                    });

                    function getNode(node) {
                        var isEndTextNode = node.nodeType === 1;
                        if (isEndTextNode) {
                            return node;
                        } else {

                            // Move to first space in end so as to not break up word when splitting
                            var i = Math.max(range.endOffset - 1, 0), character = '', whitespaceCheck = /^[^\s]$/;
                            do {
                                character = node.textContent.substring(++i, i + 1);
                            } while (whitespaceCheck.test(character));

                            if (i < node.length) {
                                node.splitText(i);
                            }
                            return node;
                        }
                    }
                });
            } else {
                this.checkIfReloadNeeded();
            }
        }, 250);

        this.tearDownDropdowns = function() {
            this.$node.find('.underneath').teardownAllComponents();
            this.disableSelection = false;
        };

        this.dropdownEntity = function(creating, insertAfterNode, selection, text) {
            this.tearDownDropdowns();
            this.disableSelection = true;

            var self = this,
                form = $('<div class="underneath"/>'),
                $node = $(insertAfterNode),
                $textSection = $node.closest('.text-section'),
                $textBody = $textSection.children('.text');

            $node.after(form);
            require(['../dropdowns/termForm/termForm'], function(TermForm) {
                TermForm.attachTo(form, {
                    sign: text,
                    propertyKey: $textSection.data('key'),
                    selection: selection,
                    mentionNode: insertAfterNode,
                    snippet: selection ?
                        rangeUtils.createSnippetFromRange(selection.range, undefined, $textBody[0]) :
                        rangeUtils.createSnippetFromNode(insertAfterNode[0], undefined, $textBody[0]),
                    existing: !creating,
                    artifactId: self.model.id
                });
            })
        };

        this.dropdownProperty = function(insertAfterNode, selection, text, vertex) {
            var self = this;

            if (vertex && _.isString(vertex)) {
                this.dataRequest('vertex', 'store', { vertexIds: vertex })
                    .done(function(vertex) {
                        self.dropdownProperty(insertAfterNode, selection, text, vertex);
                    });
                return;
            }

            this.tearDownDropdowns();
            this.disableSelection = true;

            var form = $('<div class="underneath"/>'),
                $node = $(insertAfterNode),
                $textSection = $node.closest('.text-section'),
                $textBody = $textSection.children('.text'),
                dataInfo = $node.data('info');

            $node.after(form);

            require(['../dropdowns/propertyForm/propForm'], function(PropertyForm) {
                PropertyForm.attachTo(form, {
                    data: vertex || undefined,
                    attemptToCoerceValue: text,
                    sourceInfo: selection ?
                        selection.snippet ?
                        selection :
                        self.transformSelection(selection) :
                        {
                            vertexId: self.model.id,
                            textPropertyKey: $textSection.data('key'),
                            startOffset: dataInfo.start,
                            endOffset: dataInfo.end,
                            snippet: rangeUtils.createSnippetFromNode($node[0], undefined, $textBody[0])
                        }
                });
            });
        };

        this.transformSelection = function(selection) {
            var $anchor = $(selection.anchorNode),
                $focus = $(selection.focusNode),
                isTranscript = $anchor.closest('.av-times').length,
                offsetsFunction = isTranscript ?
                    'offsetsForTranscript' :
                    'offsetsForText',
                offsets = this[offsetsFunction]([
                    {el: $anchor, offset: selection.anchorOffset},
                    {el: $focus, offset: selection.focusOffset}
                ], '.text', _.identity),
                range = selection.getRangeAt(0),
                contextHighlight = rangeUtils.createSnippetFromRange(
                    range, undefined, $anchor.closest('.text')[0]
                );

            return {
                startOffset: offsets && offsets[0],
                endOffset: offsets && offsets[1],
                snippet: contextHighlight,
                vertexId: this.model.id,
                textPropertyKey: $anchor.closest('.text-section').data('key'),
                text: selection.toString(),
                vertexTitle: F.vertex.title(this.model)
            };
        };

        this.offsetsForText = function(input, parentSelector, offsetTransform) {
            var offsets = [];
            input.forEach(function(node) {
                var parentInfo = node.el.closest('.vertex').data('info'),
                    offset = 0;

                if (parentInfo) {
                    offset = offsetTransform(parentInfo.start);
                } else {
                    var previousEntity = node.el.prevAll('.vertex').first(),
                    previousInfo = previousEntity.data('info'),
                    dom = previousInfo ?
                        previousEntity.get(0) :
                        node.el.closest(parentSelector)[0].childNodes[0],
                    el = node.el.get(0);

                    if (previousInfo) {
                        offset = offsetTransform(previousInfo.end);
                        dom = dom.nextSibling;
                    }

                    while (dom && dom !== el) {
                        if (dom.nodeType === 3) {
                            offset += dom.length;
                        } else {
                            offset += dom.textContent.length;
                        }
                        dom = dom.nextSibling;
                    }
                }

                offsets.push(offset + node.offset);
            });

            return _.sortBy(offsets, function(a, b) {
                return a - b
            });
        };

        this.offsetsForTranscript = function(input) {
            var self = this,
                index = input[0].el.closest('dd').data('index'),
                endIndex = input[1].el.closest('dd').data('index');

            if (index !== endIndex) {
                return console.warn('Unable to select across timestamps');
            }

            var rawOffsets = this.offsetsForText(input, 'dd', function(offset) {
                    return F.number.offsetValues(offset).offset;
                }),
                bitMaskedOffset = _.map(rawOffsets, _.partial(F.number.compactOffsetValues, index));

            return bitMaskedOffset;
        };

        this.updateEntityAndArtifactDraggables = function() {
            var self = this,
                scrollNode = this.scrollNode,
                words = this.select('resolvedSelector'),
                validWords = $(words);

            if (!scrollNode) {
                scrollNode = this.scrollNode = this.$node.scrollParent();
            }

            // Filter list to those in visible scroll area
            if (scrollNode && scrollNode.length) {
                validWords = validWords.withinScrollable(scrollNode);
            }

            if (validWords.length === 0) {
                return;
            }

            this.dataRequest('ontology', 'concepts')
                .done(function(concepts) {

                    validWords
                        .each(function() {
                            var $this = $(this),
                                info = $this.data('info'),
                                type = info && info['http://visallo.org#conceptType'],
                                concept = type && concepts.byId[type];

                            if (concept) {
                                $this.removePrefixedClasses('conceptId-').addClass(concept.className)
                            }
                        })
                        .draggable({
                            helper: 'clone',
                            revert: 'invalid',
                            revertDuration: 250,
                            scroll: false,
                            zIndex: 100,
                            distance: 10,
                            cursorAt: { left: -10, top: -10 },
                            start: function() {
                                $(this)
                                    .parents('.text').addClass('drag-focus');
                            },
                            stop: function() {
                                $(this)
                                    .parents('.text').removeClass('drag-focus');
                            }
                        });

                    if (Privileges.canEDIT) {

                        words.droppable({
                            activeClass: 'drop-target',
                            hoverClass: 'drop-hover',
                            tolerance: 'pointer',
                            accept: function(el) {
                                var item = $(el),
                                    isEntity = item.is('.vertex.resolved');

                                return isEntity;
                            },
                            drop: function(event, ui) {
                                var destTerm = $(this),
                                    form;

                                if (destTerm.hasClass('opens-dropdown')) {
                                    form = $('<div class="underneath"/>')
                                        .insertAfter(destTerm.closest('.detected-object-labels'));
                                } else {
                                    form = $('<div class="underneath"/>').insertAfter(destTerm);
                                }
                                self.tearDownDropdowns();
                                self.disableSelection = true;

                                require(['../dropdowns/statementForm/statementForm'], function(StatementForm) {
                                    StatementForm.attachTo(form, {
                                        sourceTerm: ui.draggable,
                                        destTerm: destTerm
                                    });
                                })
                            }
                        });
                    }
                });
        };

        this.removeHighlightClasses = function() {
            var content = this.highlightNode();
            content.removePrefixedClasses('highlight-');
        };

        this.highlightNode = function() {
            return this.$node;
        };

        this.getActiveStyle = function() {
            if (useDefaultStyle) {
                return DEFAULT;
            }

            var content = this.highlightNode(),
                index = 0;
            $.each(content.attr('class').split(/\s+/), function(_, item) {
                var match = item.match(/^highlight-(.+)$/);
                if (match) {
                    return HIGHLIGHT_STYLES.forEach(function(style, i) {
                        if (style.selector === match[1]) {
                            index = i;
                            return false;
                        }
                    });
                }
            });

            return index;
        };

        this.applyHighlightStyle = function() {
            var style = HIGHLIGHT_STYLES[this.getActiveStyle()];
            this.removeHighlightClasses();
            this.highlightNode().addClass('highlight-' + style.selector);

            if (!style.styleApplied) {
                this.dataRequest('ontology', 'concepts').done(function(concepts) {
                    var styleFile = 'tpl!detail/text/highlight-styles/' + style.selector + '.css',
                        detectedObjectStyleFile = 'tpl!detail/text/highlight-styles/detectedObject.css';

                    require([styleFile, detectedObjectStyleFile], function(tpl, doTpl) {
                        function apply(concept) {
                            if (concept.color) {
                                var STATES = {
                                        NORMAL: 0,
                                        HOVER: 1,
                                        DIM: 2,
                                        TERM: 3
                                    },
                                    className = concept.rawClassName ||
                                        (concept.className && ('vertex.' + concept.className)),
                                    definition = function(state, template) {
                                        return (template || tpl)({
                                            STATES: STATES,
                                            state: state,
                                            concept: concept,
                                            colorjs: colorjs
                                        });
                                    };

                                if (!className) {
                                    return;
                                }

                                // Dim
                                // (when dropdown is opened and it wasn't this entity)
                                stylesheet.addRule(
                                    '.highlight-' + style.selector + ' .dropdown .' + className + ',' +
                                    '.highlight-' + style.selector + ' .dropdown .resolved.' + className + ',' +
                                    '.highlight-' + style.selector + ' .drag-focus .' + className,
                                    definition(STATES.DIM)
                                );

                                stylesheet.addRule(
                                   '.highlight-' + style.selector + ' .' + className,
                                   definition(STATES.TERM)
                                );

                                // Default style (or focused)
                                stylesheet.addRule(
                                    '.highlight-' + style.selector + ' .resolved.' + className + ',' +
                                    '.highlight-' + style.selector + ' .drag-focus .resolved.' + className + ',' +
                                    '.highlight-' + style.selector + ' .dropdown .focused.' + className,
                                    definition(STATES.NORMAL)
                                );

                                // Drag-drop hover
                                stylesheet.addRule(
                                    '.highlight-' + style.selector + ' .drop-hover.' + className,
                                    definition(STATES.HOVER)
                                );

                                // Detected objects
                                stylesheet.addRule(
                                    '.highlight-' + style.selector + ' .detected-object.' + className + ',' +
                                    '.highlight-' + style.selector + ' .detected-object.resolved.' + className,
                                    definition(STATES.DIM, doTpl)
                                );
                                stylesheet.addRule(
                                    //'.highlight-' + style.selector + ' .detected-object.' + className + ',' +
                                    //'.highlight-' + style.selector + ' .detected-object.resolved.' + className + ',' +
                                    '.highlight-' + style.selector + ' .focused .detected-object.' + className + ',' +
                                    '.highlight-' + style.selector + ' .focused .detected-object.resolved.' + className,
                                    definition(STATES.NORMAL, doTpl)
                                );

                                stylesheet.addRule(
                                    '.concepticon-' + (concept.className || concept.rawClassName),
                                    'background-image: url(' + concept.glyphIconHref + ')'
                                );
                            }
                            if (concept.children) {
                                concept.children.forEach(apply);
                            }
                        }
                        apply(concepts.entityConcept);

                        // Artifacts
                        apply({
                            rawClassName: 'artifact',
                            color: 'rgb(255,0,0)',
                            glyphIconHref: '../img/glyphicons/glyphicons_036_file@2x.png'
                        });

                        style.styleApplied = true;
                    });
                });
            }
        };

        this.scrollToRevealSection = function($section) {
            var scrollIfWithinPixelsFromBottom = 150,
                y = $section.offset().top,
                scrollParent = $section.scrollParent(),
                scrollTop = scrollParent.scrollTop(),
                scrollHeight = scrollParent[0].scrollHeight,
                height = scrollParent.outerHeight(),
                maxScroll = height * 0.5,
                fromBottom = height - y;

            if (fromBottom < scrollIfWithinPixelsFromBottom) {
                scrollParent.animate({
                    scrollTop: Math.min(scrollHeight - scrollTop, maxScroll)
                }, 'fast');
            }
        };

    }
});
