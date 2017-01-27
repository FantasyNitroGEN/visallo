define([
    'util/deferredImage',
    'util/video/scrubber',
    'util/vertex/formatters'
], function(
    deferredImage,
    VideoScrubber,
    F) {
    'use strict';

    return WithPreview;

    function WithPreview() {

        this.after('initialize', function() {
            this.$node.addClass('has-preview');

            this.on('loadPreview', this.onLoadPreview);
        });

        this.before('teardown', function() {
            this.$node.removeClass('has-preview non_concept_preview video_preview loading');
        });

        this.onLoadPreview = function(event) {
            if (!this.previewLoaded) {
                this.previewLoaded = true;

                var self = this,
                    preview = this.preview = this.$node.find('.preview'),
                    activePreview = this.activePreview = this.$node.find('.active-preview'),
                    vertex = this.vertex,
                    image = vertex && F.vertex.image(vertex, null, 80, 80),
                    videoPreview = vertex && F.vertex.imageFrames(vertex),
                    nonConceptClsName = 'non_concept_preview';

                if (videoPreview) {
                    this.on('itemActivated', this.onVideoPreviewActivated);
                    this.on('itemDeactivated', this.onVideoPreviewDeactivated);
                    this.$node.addClass('video_preview ' + nonConceptClsName);
                    preview.css('display', 'block');

                    var div = preview.find('div');
                    if (!div.length) {
                        div = $('<div>').appendTo(preview);
                    }

                    VideoScrubber.attachTo(div, {
                        posterFrameUrl: image,
                        videoPreviewImageUrl: videoPreview
                    });
                } else {
                    preview.find('div').remove();
                    var concept = F.vertex.concept(vertex),
                        conceptImage = concept.glyphIconHref,
                        activeConceptImage = concept.glyphIconSelectedHref || conceptImage;

                    if ((preview.css('background-image') || '').indexOf(image) >= 0) {
                        return;
                    }

                    this.$node.removeClass(nonConceptClsName).addClass('loading');

                    deferredImage(conceptImage)
                    .always(function() {
                        preview.css('background-image', 'url(' + conceptImage + ')')
                        activePreview.css('background-image', 'url(' + activeConceptImage + ')')
                    })
                    .done(function() {
                        let imageIsFromConcept = !vertex || F.vertex.imageIsFromConcept(vertex);
                        if (!image || conceptImage === image) {
                            self.$node.toggleClass(nonConceptClsName, !imageIsFromConcept)
                            .removeClass('loading');
                        } else {
                            _.delay(function() {
                                deferredImage(image).always(function() {
                                    preview.css('background-image', 'url(' + image + ')');
                                    activePreview.css('background-image', 'url(' + image + ')');
                                    self.$node.toggleClass(nonConceptClsName, !imageIsFromConcept)
                                    .removeClass('loading');
                                })
                            }, 500);
                        }
                    });
                }
            }
        };

        this.onVideoPreviewActivated = function() {
            moveScrubber(this.preview, this.activePreview);
        };

        this.onVideoPreviewDeactivated = function() {
            moveScrubber(this.activePreview, this.preview);
        }
    }

    function moveScrubber(source, dest) {
            var scrubber = source.find('.org-visallo-video-scrubber');
            if (scrubber) {
                scrubber.appendTo(dest);
                dest.css('display', 'block');
                source.css('display', 'none');
            }
    }
});
