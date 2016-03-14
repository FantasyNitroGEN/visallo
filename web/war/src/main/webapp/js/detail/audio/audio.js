
define([
    'flight/lib/component',
    'util/audio/scrubber',
    'util/vertex/formatters'
], function(
    defineComponent,
    AudioScrubber,
    F) {
    'use strict';

    return defineComponent(Audio);

    function Audio() {

        this.attributes({
            model: null,
            previewSelector: '.audio-preview'
        })

        this.after('initialize', function() {
            this.model = this.attr.model;
            this.on('updateModel', function(event, data) {
                this.model = data.model;
                this.render();
            })
            this.render();
        });

        this.isAudioReady = function() {
            return (
                (F.vertex.props(this.model, 'http://visallo.org#audio-ogg') || [])
                .concat(
                    (F.vertex.props(this.model, 'http://visallo.org#audio-mp4') || [])
                )
            ).length >= 2;
        };

        this.render = function() {
            if (!this.isAudioReady()) {
                this.rendered = false;
                return this.$node.empty();
            }
            if (!this.rendered) {
                var rawUrl = F.vertex.raw(this.model);
                if (rawUrl) {
                    this.$node.html('<div class="audio-preview"></div>')
                    AudioScrubber.attachTo(this.select('previewSelector'), {
                        rawUrl: rawUrl
                    });
                    this.rendered = true;
                }
            }
        };
    }
});
