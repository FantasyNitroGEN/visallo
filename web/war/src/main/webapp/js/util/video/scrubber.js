
define([
    'flight/lib/component',
    'videojs',
    'tpl!./scrubber',
    'tpl!./video',
    'util/withDataRequest'
], function(defineComponent, videojs, template, videoTemplate, withDataRequest) {
    'use strict';

    var NUMBER_FRAMES = 0, // Populated by config
        POSTER = 1,
        FRAMES = 2;

    videojs.options.flash.swf = '../libs/video.js/dist/video-js/video-js.swf';

    return defineComponent(VideoScrubber, withDataRequest);

    function toPixels(str) {
        return str + 'px';
    }

    function VideoScrubber() {

        this.showing = 0;
        this.currentFrame = -1;

        this.defaultAttrs({
            allowPlayback: false,
            backgroundPosterSelector: '.background-poster',
            backgroundScrubberSelector: '.background-scrubber',
            scrubbingLineSelector: '.scrubbing-line',
            videoSelector: 'video'
        });

        this.after('initialize', function() {
            this.$node
                .toggleClass('disableScrubbing', true)
                .toggleClass('allowPlayback', false);

            this.setVideoPreviewBackgroundImage = _.once(this.setVideoPreviewBackgroundImage.bind(this));

            Promise.all([
                this.dataRequest('config', 'properties'),
                this.loadPosterFrame()
            ])
                .then(function(results) {
                    var properties = results.shift(),
                        posterDimensions = results.shift();

                    NUMBER_FRAMES = parseInt(properties['video.preview.frames.count'], 10);
                    return posterDimensions;
                })
                .then(this.setupVideo.bind(this))
                .catch(function(e) {
                    throw e;
                })
        });

        this.getVideoDimensions = function() {
            var dim = this.attr.videoDimensions;
            if (!dim || isNaN(dim[0]) || isNaN(dim[1])) {
                this.attr.videoDimensions = dim = [360, 240];
            }
            return dim;
        };

        this.updateCss = function(applyTemplate) {
            var dim = this.posterFrameDimensions,
                maxHeight = dim[1] > dim[0] ? 360 : 240,
                dimContainer = [this.$node.width(), Math.min(dim[1], maxHeight)],
                ratioImage = dim[0] / dim[1],
                ratioContainer = dimContainer[0] / dimContainer[1],
                scaled = (
                    ratioContainer > ratioImage ?
                    [dim[0] * (dimContainer[1] / dim[1]), dimContainer[1]] :
                    [dimContainer[0], dim[1] * (dimContainer[0] / dim[0])]
                ).map(function(v) {
                    return Math.floor(v);
                });

            this.scaledDimensions = scaled;

            this.$node
                .toggleClass('disableScrubbing', !this.attr.videoPreviewImageUrl)
                .toggleClass('allowPlayback', this.attr.allowPlayback)
                .css('height', scaled[1]);

            if (applyTemplate) {
                this.$node.html(template({}));
            }

            var sizeCss = {
                width: scaled[0] + 'px',
                height: scaled[1] + 'px',
                left: '50%',
                top: '50%',
                marginLeft: '-' + scaled[0] / 2 + 'px',
                marginTop: '-' + scaled[1] / 2 + 'px',
                position: 'absolute'
            };
            this.videoPreviewMarginLeft = scaled[0] / 2;
            this.select('backgroundScrubberSelector').css(sizeCss);
            this.select('backgroundPosterSelector').css(_.extend({}, sizeCss, {
                backgroundRepeat: 'no-repeat',
                backgroundPosition: 'center',
                backgroundSize: scaled.map(toPixels).join(' '),
                backgroundImage: 'url(' + this.attr.posterFrameUrl + ')'
            }));
        };

        this.onGraphPaddingUpdated = function(e, d) {
            if (e.type === 'graphPaddingUpdated') {
                if (this.previousDetailPadding && d.padding.r === this.previousDetailPadding) return;
                this.previousDetailPadding = d.padding.r;
            }

            this.updateCss();

            if (this.showing === POSTER) {
                this.showPoster();
            } else {
                var frame = this.currentFrame;
                this.currentFrame = null;
                this.showFrames(frame);
            }
        };

        this.setupVideo = function(dim) {
            var self = this;

            // FIXME: needed?
            this.posterFrameDimensions = dim;

            this.updateCss(true);
            this.showPoster();

            var throttledFrameUpdate = _.throttle(this.onGraphPaddingUpdated.bind(this), 100);
            this.on(document, 'graphPaddingUpdated', throttledFrameUpdate);
            this.on(document, 'windowResize', throttledFrameUpdate);
            this.on('videoPlayerInitialized', function(e) {
                this.off('mousemove');
                this.off('mouseleave');
                this.off('click');
            });
            this.on('click', this.onClick);
            this.on('seekToTime', this.onSeekToTime);

            this.loadVideoPreview()
                .then(function(previewDimensions) {
                    self.videoPreviewImageDimensions = previewDimensions;
                    self.videoPreviewFrameImageDimensions = [previewDimensions[0] / NUMBER_FRAMES, previewDimensions[1]];

                    self.on('mousemove', {
                        scrubbingLineSelector: function(e) {
                            e.stopPropagation();
                        }
                    });
                    self.$node
                        .on('mouseenter mousemove', function(e) {
                            var $target = $(e.target);
                            if ($target.is('.scrubbing-play-button') || $target.is(this)) {
                                e.stopPropagation();
                                self.showPoster();
                            } else {
                                var left = e.pageX - $target.offset().left,
                                    percent = left / $target.width();

                                if (percent <= 1.0 && percent >= 0.0) {
                                    var index = Math.round(percent * NUMBER_FRAMES);
                                    self.scrubPercent = index / NUMBER_FRAMES;
                                    self.showFrames(index);
                                } else {
                                    self.showPoster();
                                }
                            }
                        })
                        .on('mouseleave', function(e) {
                            self.showPoster();
                        })
                })
                .catch(function() { })
        };

        this.loadPosterFrame = function() {
            return this.loadImageUrl(this.attr.posterFrameUrl);
        };

        this.loadVideoPreview = function() {
            return this.loadImageUrl(this.attr.videoPreviewImageUrl);
        };

        this.loadImageUrl = function(url) {
            if (url) {
                return new Promise(function(f, r) {
                    var i = new Image();
                    i.onload = function() {
                        f([i.width, i.height]);
                    }
                    i.onerror = r;
                    i.src = url;
                });
            }

            return Promise.reject();
        }

        this.setVideoPreviewBackgroundImage = function() {
            this.select('backgroundScrubberSelector').css({
                backgroundImage: 'url(' + this.attr.videoPreviewImageUrl + ')'
            });
        };

        this.showFrames = function(index) {
            if (index === this.currentFrame || !this.attr.videoPreviewImageUrl) {
                return;
            }

            var css = {
                backgroundRepeat: 'repeat-x',
                backgroundSize: 'auto 100%',
                backgroundPosition: [
                    this.videoPreviewFrameImageDimensions[0] *
                    this.scaledDimensions[1] / this.videoPreviewFrameImageDimensions[1] *
                    (index || 0) * -1,
                    '0'
                ].map(toPixels).join(' ')
            };

            this.setVideoPreviewBackgroundImage();

            var $preview = this.select('backgroundScrubberSelector').css(css).show();
            this.select('backgroundPosterSelector').hide();
            this.showing = FRAMES;
            this.currentFrame = index;

            this.select('scrubbingLineSelector').css({
                left: $preview.position().left -
                    this.videoPreviewMarginLeft +
                    (index / NUMBER_FRAMES) * $preview.width()
            }).show();

            this.trigger('scrubberFrameChange', {
               index: index,
               numberOfFrames: NUMBER_FRAMES
            });
        };

        this.showPoster = function() {
            this.select('scrubbingLineSelector').hide();
            this.select('backgroundScrubberSelector').hide();
            this.select('backgroundPosterSelector').show();

            this.showing = POSTER;
            this.currentFrame = -1;

            this.trigger('scrubberFrameChange', {
               index: 0,
               numberOfFrames: NUMBER_FRAMES
            });
        };

        this.onClick = function(event) {
            if (this.attr.allowPlayback !== true || this.select('videoSelector').length) {
                return;
            }
            event.preventDefault();

            var userClickedPlayButton = $(event.target).is('.scrubbing-play-button');

            if (userClickedPlayButton) {
                this.startVideo();
            } else {
                this.startVideo({
                    percentSeek: this.scrubPercent
                })
            }
        };

        this.startVideo = function(opts) {
            var self = this,
                options = opts || {},
                $video = this.select('videoSelector'),
                videoPlayer = $video.length && $video[0];

            if (videoPlayer && videoPlayer.readyState === 4) {
                videoPlayer.currentTime = Math.max(0.0,
                       (options.percentSeek ?
                            options.percentSeek * videoPlayer.duration :
                            options.seek ? options.seek : 0.0
                       ) - 1.0
                );
                videoPlayer.play();
            } else {
                var players = videojs.players,
                    video = $(videoTemplate(
                        _.tap(this.attr, function(attrs) {
                            var url = attrs.rawUrl;
                            if (~url.indexOf('?')) {
                                url += '&';
                            } else {
                                url += '?';
                            }
                            url += 'playback=true';
                            attrs.url = url;
                        })
                    ));

                this.$node.html(video);
                Object.keys(players).forEach(function(player) {
                    if (players[player]) {
                        players[player].dispose();
                        delete players[player];
                    }
                });

                this.trigger('videoPlayerInitialized');

                _.defer(videojs, video[0], {
                    controls: true,
                    autoplay: true,
                    preload: 'auto'
                }, function() {
                    /*eslint consistent-this:0*/
                    var $videoel = this;

                    if (options.seek || options.percentSeek) {
                        $videoel.on('durationchange', durationchange);
                        $videoel.on('loadedmetadata', durationchange);
                    }
                    $videoel.on('timeupdate', timeupdate);

                    function timeupdate(event) {
                        self.trigger('playerTimeUpdate', {
                            currentTime: $videoel.currentTime(),
                            duration: $videoel.duration()
                        });
                    }

                    function durationchange(event) {
                        var duration = $videoel.duration();
                        if (duration > 0.0) {
                            $videoel.off('durationchange', durationchange);
                            $videoel.off('loadedmetadata', durationchange);
                            $videoel.currentTime(
                                Math.max(0.0,
                                    (options.percentSeek ?
                                        duration * self.scrubPercent :
                                        options.seek) - 1.0
                                )
                            );
                        }
                    }
                });
            }
        };

        this.onSeekToTime = function(event, data) {
            this.startVideo({
                seek: data.seekTo / 1000
            });
        };

    }
});
