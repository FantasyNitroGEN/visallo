
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
            this.$node.html('<div class="audio-preview"></div>')

            var model = this.attr.model;

            AudioScrubber.attachTo(this.select('previewSelector'), {
                rawUrl: F.vertex.raw(model)
            });
        });
    }
});
