define([
    'flight/lib/component',
    'util/video/scrubber',
    'util/vertex/formatters'
], function(
    defineComponent,
    VideoScrubber,
    F) {
    'use strict';

    return defineComponent(Video);

    function Video() {

        this.attributes({
            model: null,
            previewSelector: '.video-preview',
            currentTranscriptSelector: '.currentTranscript'
        })

        this.after('initialize', function() {
            this.$node
                .addClass('org-visallo-video')
                .html('<div class="video-preview"></div><div class="currentTranscript"></div>')

            var model = this.attr.model;

            VideoScrubber.attachTo(this.select('previewSelector'), {
                rawUrl: F.vertex.raw(model),
                posterFrameUrl: F.vertex.image(model),
                videoPreviewImageUrl: F.vertex.imageFrames(model),
                duration: undefined,
                allowPlayback: true
            });
        });
    }
});
