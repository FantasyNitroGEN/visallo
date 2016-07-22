define([
    'flight/lib/component',
    'util/withDropdown',
    'tpl!./termForm',
    'tpl!util/alert',
    'util/vertex/formatters',
    'util/ontology/conceptSelect',
    'util/vertex/vertexSelect',
    'util/withDataRequest'
], function(
    defineComponent,
    withDropdown,
    dropdownTemplate,
    alertTemplate,
    F,
    ConceptSelector,
    VertexSelector,
    withDataRequest) {
    'use strict';

    return defineComponent(TermForm, withDropdown, withDataRequest);

    function TermForm() {

        this.defaultAttrs({
            entityConceptMenuSelector: '.underneath .dropdown-menu a',
            actionButtonSelector: '.btn.btn-small.btn-primary',
            buttonDivSelector: '.buttons',
            visibilitySelector: '.visibility',
            conceptContainerSelector: '.concept-container',
            vertexContainerSelector: '.vertex-container',
            helpSelector: '.help',
            addNewPropertiesSelector: '.none'
        });

        this.after('teardown', function() {
            if (this.promoted && this.promoted.length) {
                this.demoteSpanToTextVertex(this.promoted);
            }

            // Remove extra textNodes
            if (this.node.parentNode) {
                this.node.parentNode.normalize();
            }
        });

        this.after('initialize', function() {
            this.deferredConcepts = $.Deferred();
            this.setupContent();
            this.registerEvents();
        });

        this.showTypeahead = function(options) {
            if (!this.unresolve) {
                this.select('vertexContainerSelector').trigger('showTypeahead', options);
            }
        };

        this.reset = function() {
            this.currentGraphVertexId = null;
            this.select('helpSelector').show();
            this.select('visibilitySelector').hide();
            this.select('conceptContainerSelector').hide();
            this.select('actionButtonSelector').hide();
        };

        this.onVertexSelected = function(event, data) {
            this.sign = data.sign;
            this.graphVertexChanged(data.vertexId, data.item);
        };

        this.graphVertexChanged = function(newGraphVertexId, item, initial) {
            var self = this;

            this.currentGraphVertexId = newGraphVertexId;
            if (!initial || newGraphVertexId) {
                var info = _.isObject(item) ? item.properties || item : $(this.attr.mentionNode).data('info');

                this.trigger(this.select('conceptContainerSelector'), 'enableConcept', {
                    enable: !newGraphVertexId
                });

                var conceptType = _.isArray(info) ?
                    _.findWhere(info, { name: 'http://visallo.org#conceptType' }) :
                    (info && (info['http://visallo.org#conceptType'] || info.concept));
                conceptType = conceptType && conceptType.value || conceptType || '';

                if (conceptType === '' && self.attr.restrictConcept) {
                    conceptType = self.attr.restrictConcept;
                }
                this.selectedConceptId = conceptType;

                this.deferredConcepts.done(function() {
                    self.trigger(self.select('conceptContainerSelector').show(), 'selectConceptId', {
                        conceptId: conceptType
                    });
                    self.checkValid()
                });

                if (this.unresolve) {
                    this.select('actionButtonSelector')
                        .text(i18n('detail.resolve.form.button.unresolve'))
                        .show();
                    this.$node.find('input,select').attr('disabled', true);
                } else {
                    this.select('actionButtonSelector')
                        .text(newGraphVertexId && !initial && !this.attr.coords ?
                              i18n('detail.resolve.form.button.resolve.existing') :
                              i18n('detail.resolve.form.button.resolve.new'))
                        .show();
                }
                this.select('helpSelector').hide();
                this.select('visibilitySelector').show();

                require(['util/visibility/edit'], function(Visibility) {
                    Visibility.attachTo(self.$node.find('.visibility'), {
                        value: '',
                        readonly: self.unresolve
                    });
                });

                if (!this.unresolve) {
                    require(['detail/dropdowns/propertyForm/justification'], function(Justification) {
                        Justification.attachTo(self.$node.find('.justification'));
                    });
                }
            } else if (this.attr.restrictConcept) {
                this.deferredConcepts.done(function() {
                    self.trigger(self.select('conceptContainerSelector'), 'selectConceptId', {
                        conceptId: self.attr.restrictConcept
                    })
                });
            }
        };

        this.onButtonClicked = function(event) {
            if (!this.attr.detectedObject) {
                this.termModification(event);
            } else {
                this.detectedObjectModification(event);
            }
        };

        this.termModification = function(event) {
            var self = this,
                $mentionNode = $(this.attr.mentionNode),
                newObjectSign = $.trim(this.sign),
                mentionStart,
                mentionEnd;

            if (this.attr.existing) {
                var dataInfo = $mentionNode.data('info');
                mentionStart = dataInfo.start;
                mentionEnd = dataInfo.end;
            } else {
                mentionStart = this.selectedStart;
                mentionEnd = this.selectedEnd;
            }
            var parameters = {
                sign: newObjectSign,
                propertyKey: this.attr.propertyKey,
                propertyName: this.attr.propertyName,
                conceptId: this.selectedConceptId,
                mentionStart: mentionStart,
                mentionEnd: mentionEnd,
                artifactId: this.attr.artifactId,
                visibilitySource: this.visibilitySource ? this.visibilitySource.value : ''
            };

            if (this.currentGraphVertexId) {
                parameters.resolvedVertexId = this.currentGraphVertexId;
                parameters.edgeId = $mentionNode.data('info') ? $mentionNode.data('info').edgeId : null;
            }

            _.defer(this.buttonLoading.bind(this));

            if (!parameters.conceptId || parameters.conceptId.length === 0) {
                this.select('conceptContainerSelector').find('select').focus();
                return;
            }

            if (newObjectSign.length) {
                parameters.objectSign = newObjectSign;
                $mentionNode.attr('title', newObjectSign);
            }

            if (!this.unresolve) {
                if (self.attr.snippet) {
                    parameters.sourceInfo = {
                        vertexId: parameters.artifactId,
                        textPropertyKey: parameters.propertyKey,
                        textPropertyName: parameters.propertyName,
                        startOffset: parameters.mentionStart,
                        endOffset: parameters.mentionEnd,
                        snippet: self.attr.snippet
                    };
                }

                if (this.justification && this.justification.justificationText) {
                    parameters.justificationText = this.justification.justificationText;
                }

                this.dataRequest('vertex', 'resolveTerm', parameters)
                    .then(function(data) {
                        self.highlightTerm(data);
                        self.trigger('termCreated', data);

                        self.trigger(document, 'loadEdges');
                        self.trigger('closeDropdown');

                        _.defer(self.teardown.bind(self));
                    })
                    .catch(this.requestFailure.bind(this))
            } else {
                parameters.termMentionId = this.termMentionId;
                this.dataRequest('vertex', 'unresolveTerm', parameters)
                    .then(function(data) {
                        self.highlightTerm(data);

                        self.trigger(document, 'loadEdges');
                        self.trigger('closeDropdown');

                        _.defer(self.teardown.bind(self));
                    })
                    .catch(this.requestFailure.bind(this))
            }
        };

        this.requestFailure = function(request, message, error) {
            this.markFieldErrors(error);
            _.defer(this.clearLoading.bind(this));
        };

        this.detectedObjectModification = function(event) {
            var self = this,
                newSign = $.trim(this.sign),
                parameters = {
                    title: newSign,
                    conceptId: this.selectedConceptId,
                    originalPropertyKey: this.attr.dataInfo.originalPropertyKey,
                    graphVertexId: this.attr.dataInfo.resolvedVertexId ?
                        this.attr.dataInfo.resolvedVertexId :
                        this.currentGraphVertexId,
                    artifactId: this.attr.artifactData.id,
                    x1: parseFloat(this.attr.dataInfo.x1),
                    y1: parseFloat(this.attr.dataInfo.y1),
                    x2: parseFloat(this.attr.dataInfo.x2),
                    y2: parseFloat(this.attr.dataInfo.y2),
                    visibilitySource: this.visibilitySource ? this.visibilitySource.value : ''
                };

            if (this.justification && this.justification.justificationText) {
                parameters.justificationText = this.justification.justificationText;
            }

            _.defer(this.buttonLoading.bind(this));
            if (this.unresolve) {
                self.unresolveDetectedObject({
                    vertexId: this.attr.artifactData.id,
                    multiValueKey: this.attr.dataInfo.key || this.attr.dataInfo.propertyKey
                });
            } else {
                self.resolveDetectedObject(parameters);
            }
        };

        this.resolveDetectedObject = function(parameters) {
            var self = this;
            this.dataRequest('vertex', 'resolveDetectedObject', parameters)
                .then(function(data) {
                    self.trigger('termCreated', data);
                    self.trigger(document, 'loadEdges');
                    self.trigger('closeDropdown');
                    _.defer(self.teardown.bind(self));
                })
                .catch(this.requestFailure.bind(this))
        };

        this.unresolveDetectedObject = function(parameters) {
            var self = this;
            this.dataRequest('vertex', 'unresolveDetectedObject', parameters)
                .then(function(data) {
                    self.trigger(document, 'loadEdges');
                    self.trigger('closeDropdown');
                    _.defer(self.teardown.bind(self));
                })
                .catch(this.requestFailure.bind(this))
        };

        this.onConceptSelected = function(event, data) {
            this.selectedConceptId = data && data.concept && data.concept.id || '';
            if (this.selectedConceptId) {
                this.select('vertexContainerSelector').trigger('setConcept', { conceptId: this.selectedConceptId });
            } else {
                this.select('vertexContainerSelector').trigger('setConcept');
            }
            this.checkValid();
        };

        this.onVisibilityChange = function(event, data) {
            this.visibilitySource = data;
            this.checkValid();
        };

        this.onJustificationChange = function(event, data) {
            this.justification = data;
            this.checkValid();
        };

        this.checkValid = function() {
            var button = this.select('actionButtonSelector');
            var visibilityValid = this.visibilitySource && this.visibilitySource.valid;

            if (!this.unresolve) {
                this.select('visibilitySelector').find('input').toggleClass('invalid', !visibilityValid);
                if (this.justification && this.justification.valid && this.selectedConceptId && visibilityValid) {
                    button.removeAttr('disabled');
                } else {
                    button.attr('disabled', true);
                }
            } else {
                button.removeAttr('disabled');
            }
        };

        this.setupContent = function() {

            var self = this,
                vertex = this.$node,
                existingEntity,
                objectSign = '',
                data, graphVertexId, title;

            if (!this.attr.detectedObject) {
                var mentionVertex = $(this.attr.mentionNode);
                data = mentionVertex.data('info');
                existingEntity = this.attr.existing ? mentionVertex.addClass('focused').hasClass('resolved') : false;
                title = $.trim(data && data.title || '');

                if (this.attr.selection && !existingEntity) {
                    this.trigger(document, 'ignoreSelectionChanges.detail');
                    this.promoted = this.promoteSelectionToSpan();

                    _.defer(function() {
                        self.trigger(document, 'resumeSelectionChanges.detail');
                    });
                }

                if (existingEntity && mentionVertex.hasClass('resolved')) {
                    objectSign = title;
                    this.unresolve = this.attr.unresolve;
                    graphVertexId = this.unresolve && data && (data.resolvedToVertexId || data.resolvedVertexId);
                    this.termMentionId = data && data.id;
                } else {
                    objectSign = this.attr.sign || mentionVertex.text();
                }
            } else {
                data = this.attr.dataInfo;
                objectSign = data && data.title;
                existingEntity = this.attr.existing;
                this.unresolve = this.attr.unresolve;
                graphVertexId = this.unresolve && data && (data.resolvedToVertexId || data.resolvedVertexId);
            }

            vertex.html(dropdownTemplate({
                sign: $.trim(objectSign),
                graphVertexId: graphVertexId,
                objectSign: $.trim(objectSign) || '',
                buttonText: existingEntity ?
                    i18n('detail.resolve.form.button.resolve.existing') :
                    i18n('detail.resolve.form.button.resolve.new')
            }));

            ConceptSelector.attachTo(this.select('conceptContainerSelector').toggle(!!graphVertexId), {
                restrictConcept: this.attr.restrictConcept
            });

            VertexSelector.attachTo(this.select('vertexContainerSelector'), {
                value: objectSign || '',
                filterResultsToTitleField: true,
                allowNew: true,
                defaultText: i18n('detail.resolve.form.entity_search.placeholder'),
                allowNewText: i18n('detail.resolve.form.entity_search.resolve_as_new')
            });

            this.graphVertexChanged(graphVertexId, data, true);

            if (!this.unresolve && objectSign) {
                this.select('vertexContainerSelector').trigger('disableAndSearch', {
                    query: objectSign
                })
            }

            this.sign = objectSign;
            this.startSign = objectSign;
        };

        this.conceptForConceptType = function(conceptType, allConcepts) {
            return _.findWhere(allConcepts, { id: conceptType });
        };

        this.registerEvents = function() {

            this.on('visibilitychange', this.onVisibilityChange);
            this.on('justificationchange', this.onJustificationChange);

            this.on('conceptSelected', this.onConceptSelected);
            this.on('resetTypeahead', this.reset);
            this.on('vertexSelected', this.onVertexSelected);

            this.on('click', {
                entityConceptMenuSelector: this.onEntityConceptSelected,
                actionButtonSelector: this.onButtonClicked,
                helpSelector: function() {
                    this.showTypeahead({
                        focus: true
                    });
                }
            });

            this.on('opened', function() {
                var self = this;

                this.loadConcepts()
                    .then(function() {
                        self.deferredConcepts.resolve(self.allConcepts);
                    })
            });
        };

        this.loadConcepts = function() {
            var self = this;
            self.allConcepts = [];
            return this.dataRequest('ontology', 'concepts')
                .then(function(concepts) {
                    var vertexInfo;

                    if (self.attr.detectedObject) {
                        vertexInfo = self.attr.dataInfo;
                    } else {
                        var mentionVertex = $(self.attr.mentionNode);
                        vertexInfo = mentionVertex.data('info');
                    }

                    self.allConcepts = _.filter(concepts.byTitle, function(c) {
                        return c.userVisible !== false;
                    });

                    self.selectedConceptId = vertexInfo && (
                        vertexInfo['http://visallo.org#conceptType'] ||
                        (
                            vertexInfo.properties &&
                            vertexInfo.properties['http://visallo.org#conceptType'].value
                        )
                    ) || '';

                    self.trigger(self.select('conceptContainerSelector'), 'selectConceptId', {
                        conceptId: self.selectedConceptId
                    });

                    if (!self.selectedConceptId) {
                        self.select('actionButtonSelector').attr('disabled', true);
                    }
                });
        };

        this.highlightTerm = function(data) {
            var mentionVertex = $(this.attr.mentionNode),
                updatingEntity = this.attr.existing;

            if (updatingEntity) {
                mentionVertex.removeClass();
                if (data.cssClasses) {
                    mentionVertex.addClass(data.cssClasses.join(' '));
                }
                mentionVertex.data('info', data.info).removeClass('focused');

            } else if (this.promoted) {
                this.promoted.data('info', data.info)
                    .addClass((data.cssClasses && data.cssClasses.join(' ')) || '')
                    .removeClass('focused');
                this.promoted = null;
            }
        };

        this.promoteSelectionToSpan = function() {
            var isTranscript = this.$node.closest('.av-times').length,
                range = this.attr.selection.range,
                el,
                tempTextNode,
                transcriptIndex = 0,
                span = document.createElement('span');

            span.className = 'vertex focused';

            var newRange = document.createRange();
            newRange.setStart(range.startContainer, range.startOffset);
            newRange.setEnd(range.endContainer, range.endOffset);

            var r = range.cloneRange();

            if (isTranscript) {
                var dd = this.$node.closest('dd');
                r.selectNodeContents(dd.get(0));
                transcriptIndex = dd.data('index');
            } else {
                var $text = this.$node.closest('.text');
                if ($text.length) {
                    r.selectNodeContents($text.get(0));
                }
            }
            r.setEnd(range.startContainer, range.startOffset);
            var l = r.toString().length;

            this.selectedStart = l;
            this.selectedEnd = l + range.toString().length;

            if (isTranscript) {
                this.selectedStart = F.number.compactOffsetValues(transcriptIndex, this.selectedStart);
                this.selectedEnd = F.number.compactOffsetValues(transcriptIndex, this.selectedEnd);
            }

            // Special case where the start/end is inside an inner span
            // (surroundsContents will fail so expand the selection
            if (/vertex/.test(range.startContainer.parentNode.className)) {
                el = range.startContainer.parentNode;
                var previous = el.previousSibling;

                if (previous && previous.nodeType === 3) {
                    newRange.setStart(previous, previous.textContent.length);
                } else {
                    tempTextNode = document.createTextNode('');
                    el.parentNode.insertBefore(tempTextNode, el);
                    newRange.setStart(tempTextNode, 0);
                }
            }
            if (/vertex/.test(range.endContainer.parentNode.className)) {
                el = range.endContainer.parentNode;
                var next = el.nextSibling;

                if (next && next.nodeType === 3) {
                    newRange.setEnd(next, 0);
                } else {
                    tempTextNode = document.createTextNode('');
                    if (next) {
                        el.parentNode.insertBefore(tempTextNode, next);
                    } else {
                        el.appendChild(tempTextNode);
                    }
                    newRange.setEnd(tempTextNode, 0);
                }
            }
            newRange.surroundContents(span);

            return $(span).find('.vertex').addClass('focused').end();
        };

        this.demoteSpanToTextVertex = function(vertex) {

            while (vertex[0].childNodes.length) {
                $(vertex[0].childNodes[0]).removeClass('focused');
                vertex[0].parentNode.insertBefore(vertex[0].childNodes[0], vertex[0]);
            }
            vertex.remove();
        };
    }
});
