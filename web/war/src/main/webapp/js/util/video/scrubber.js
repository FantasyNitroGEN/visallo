
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
            var dim = this.getVideoDimensions(),
                dimContainer = [this.$node.width(), Math.min(dim[1], 240)],
                ratioImage = dim[0] / dim[1],
                ratioContainer = dimContainer[0] / dimContainer[1],
                scaled = (
                    ratioContainer > ratioImage ?
                    [dim[0] * (dimContainer[1] / dim[1]), dimContainer[1]] :
                    [dimContainer[0], dim[1] * (dimContainer[0] / dim[0])]
                ).map(function(v) {
                    return Math.floor(v);
                });

            //console.log(dim, dimContainer, 'scaled=' + scaled.map(toPixels));

            this.$node
                .toggleClass('disableScrubbing', !this.attr.videoPreviewImageUrl)
                .toggleClass('allowPlayback', this.attr.allowPlayback)
                .css('height', scaled[1])
                .html(template({}));

            var sizeCss = {
                width: scaled[0] + 'px',
                height: scaled[1] + 'px',
                left: '50%',
                top: '50%',
                marginLeft: '-' + scaled[0] / 2 + 'px',
                marginTop: '-' + scaled[1] / 2 + 'px',
                position: 'absolute'
            };
            this.select('backgroundScrubberSelector').css(sizeCss);
            this.select('backgroundPosterSelector').css(_.extend({}, sizeCss, {
                backgroundRepeat: 'no-repeat',
                backgroundPosition: 'center',
                backgroundSize: scaled.map(toPixels).join(' '),
                backgroundImage: 'url(' + this.attr.posterFrameUrl + ')'
            }));

            this.dataRequest('config', 'properties')
                .then(function(properties) {
                    NUMBER_FRAMES = parseInt(properties['video.preview.frames.count'], 10);
                })
                .then(this.setupVideo.bind(this));
        });

        this.getVideoDimensions = function() {
            var dim = this.attr.videoDimensions;
            if (!dim || isNaN(dim[0]) || isNaN(dim[1])) {
                this.attr.videoDimensions = dim = [360, 240];
            }
            return dim;
        };

        this.setupVideo = function() {
            var self = this;

            this.showPoster();
            //var i = 0;
            //setTimeout(function() {
                //i = (i + 4) % 20;
                //console.log('showing frame', i)
                //self.showFrames(19);
            //}, 1000)
            //return;

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
                    console.log(previewDimensions)

                    self.on('mousemove', {
                        scrubbingLineSelector: function(e) {
                            e.stopPropagation();
                        }
                    });
                    self.$node
                        .on('mouseenter mousemove', function(e) {
                            if ($(e.target).is('.scrubbing-play-button')) {
                                e.stopPropagation();
                                self.showPoster();
                            } else {
                                var left = e.pageX - $(e.target).closest('.preview').offset().left,
                                    percent = left / this.offsetWidth,
                                    index = Math.round(percent * NUMBER_FRAMES);

                                self.scrubPercent = index / NUMBER_FRAMES;
                                self.showFrames(index);
                            }
                        })
                        .on('mouseleave', function(e) {
                            self.showPoster();
                        })
                })
                .catch(function() { })
        };

        this.loadVideoPreview = function() {
            var url = this.attr.videoPreviewImageUrl;

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

        this.showFrames = function(index) {
            if (index === this.currentFrame || !this.attr.videoPreviewImageUrl) {
                return;
            }

            var width = this.$node.width();

            this.select('scrubbingLineSelector').css({
                left: (index / NUMBER_FRAMES) * width
            }).show();

            var css = {
                backgroundRepeat: 'repeat-x',
                //backgroundSize: (width * NUMBER_FRAMES) + 'px auto',
                backgroundPosition: (320 * (index || 0) * -1) + 'px 0',
                backgroundImage: 'url(' + this.attr.videoPreviewImageUrl + ')'
            };

            this.select('backgroundScrubberSelector').css(css).show();
            this.select('backgroundPosterSelector').hide();
            this.showing = FRAMES;
            this.currentFrame = index;

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
