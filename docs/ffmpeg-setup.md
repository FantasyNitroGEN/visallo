# FFmpeg Setup

## Linux

1. Follow the [CentOS](https://trac.ffmpeg.org/wiki/CompilationGuide/Centos) or [Ubuntu](https://trac.ffmpeg.org/wiki/CompilationGuide/Ubuntu) instructions to build and install FFmpeg with the necessary plugins
2. Build and install qt-faststart (qt-faststart ships with FFmpeg -- no separate download is required)

*in the ffmpeg directory:*

        cd tools
        make qt-faststart
        cp qt-faststart /usr/local/bin


## Mac OS X

1. Install FFmpeg and qt-faststart with `brew`:

        brew install ffmpeg --with-libvorbis --with-libvpx --with-fdk-aac --with-opus --with-theora
        brew install qtfaststart
