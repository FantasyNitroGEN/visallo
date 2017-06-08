define([
    'flight/lib/component',
    'util/video/scrubber',
    'util/vertex/formatters',
    'util/withDataRequest',
    'util/requirejs/promise!util/service/propertiesPromise',
    './transcriptEntry.hbs',
    'sf'
], function(
    defineComponent,
    VideoScrubber,
    F,
    withDataRequest,
    config,
    transcriptEntryTemplate,
    sf) {
    'use strict';

    return defineComponent(Video, withDataRequest);

    function Video() {

        this.attributes({
            model: null,
            previewSelector: '.video-preview',
            currentTranscriptSelector: '.currentTranscript',
            ignoreUpdateModelNotImplemented: true
        })

        this.after('initialize', function() {
            var self = this;

            this.on('scrubberFrameChange', this.onScrubberFrameChange);
            this.on('playerTimeUpdate', this.onPlayerTimeUpdate);
            this.on('updateModel', this.onUpdateModel);
            this.on(this.$node.parents('.type-content'), 'avLinkClicked', this.onAVLinkClicked);

            this.model = this.attr.model;
            this.render();
        });

        this.render = function() {
            var self = this;

            const formatsReady = this.isVideoReady();
            if (!_.any(formatsReady)) {
                this.videoRendered = false;
                return this.$node.empty();
            }
            if (!this.videoRendered) {
                this.$node.html('<div class="video-preview"></div><div class="currentTranscript"></div>')
                var durationProperty = _.findWhere(this.model.properties, { name: config['ontology.intent.property.videoDuration'] });
                if (durationProperty) {
                    this.duration = durationProperty.value * 1000;
                }
                VideoScrubber.attachTo(this.select('previewSelector'), {
                    rawUrl: F.vertex.raw(this.model),
                    posterFrameUrl: F.vertex.image(this.model),
                    formatsReady,
                    videoPreviewImageUrl: F.vertex.imageFrames(this.model),
                    duration: this.duration,
                    allowPlayback: true
                });
                this.videoRendered = true;
            }

            if (this.videoRendered && !this.transcriptRendered) {
                this.renderTranscript();
            }
        };

        this.renderTranscript = function(key, time) {
            var self = this,
                currentTime = time || 0,
                transcriptProperties = _.where(this.model.properties, { name: 'http://visallo.org#videoTranscript' });

            if (!transcriptProperties.length) return;

            var transcriptKey = key ? key : transcriptProperties[0].key;

            this.dataRequest('vertex', 'highlighted-text', this.model.id, transcriptKey)
                .catch(function() {
                    return '';
                })
                .then(function(artifactText) {
                    self.currentTranscriptKey = transcriptKey;
                    self.currentTranscript = processArtifactText(artifactText);
                    self.updateCurrentTranscript(currentTime);
                });
        };

        this.onUpdateModel = function(event, data) {
            this.model = data.model;
            this.render();
        };

        this.isVideoReady = function() {
            const formats = ['mp4', 'webm'].map(format => [
                format,
                F.vertex.props(this.model, `http://visallo.org#video-${format}`).length > 0
            ]);
            return _.object(formats);
        };

        this.onPlayerTimeUpdate = function(evt, data) {
            var time = data.currentTime * 1000;
            this.updateCurrentTranscript(time);
        };

        this.onScrubberFrameChange = function(evt, data) {
            if (!this.duration) {
                if (!this._noDurationWarned) {
                    console.warn('No duration property for artifact, unable to sync transcript');
                    this._noDurationWarned = true;
                }
                return;
            }
            var frameIndex = data.index,
                numberOfFrames = data.numberOfFrames,
                time = (this.duration / numberOfFrames) * frameIndex;

            this.updateCurrentTranscript(time);
        };

        this.updateCurrentTranscript = function(time) {
            var entry = this.findTranscriptEntryForTime(time),
                html = '';

            if (entry) {
                var timeLabel = (_.isUndefined(entry.start) ? '' : formatTimeOffset(entry.start)) +
                    ' - ' +
                    (_.isUndefined(entry.end) ? '' : formatTimeOffset(entry.end));
                html = transcriptEntryTemplate({
                    time: timeLabel,
                    text: entry.text
                });
            }
            this.select('currentTranscriptSelector').html(html);
        };

        this.findTranscriptEntryForTime = function(time) {
            if (!this.currentTranscript || !this.currentTranscript.entries) {
                return null;
            }
            var bestMatch = this.currentTranscript.entries[0];
            for (var i = 0; i < this.currentTranscript.entries.length; i++) {
                if (this.currentTranscript.entries[i].start <= time) {
                    bestMatch = this.currentTranscript.entries[i];
                }
            }
            return bestMatch;
        };

        this.onAVLinkClicked = function(event, data) {
            this.trigger(this.select('previewSelector'), 'seekToTime', data);
            if (data.transcriptKey !== this.currentTranscriptKey) {
                this.renderTranscript(data.transcriptKey, data.seekTo);
            }
        };
    }

    function processArtifactText(text) {
        // Looks like JSON ?
        if (/^\s*{/.test(text)) {
            var json;
            try {
                json = JSON.parse(text);
            } catch(e) { /*eslint no-empty:0*/ }

            if (json && !_.isEmpty(json.entries)) {
                return json;
            }
        }
        return null;
    }

    function formatTimeOffset(time) {
        return sf('{0:h:mm:ss}', new sf.TimeSpan(time));
    }
});
