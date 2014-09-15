<img src="./MediaPlayerDemo/src/main/ic_launcher-web.png" width="100" height="100" alt="ITEC MediaPlayer Icon"/>

ITEC MediaPlayer
================

The ITEC MediaPlayer library is a video player library specialized in video (frame) inspection, supporting
exact seeking to frames, playback speed adjustment, shader support, zooming & panning, frame extraction
and a lot of media source protocols and formats. It strives to be an API-compatible direct replacement 
for the Android `MediaPlayer` and `VideoView` components and builds upon the Android `MediaExtractor` 
and `MediaCodec` components. It is very lightweight, easy to use, and makes native code / NDK fiddling 
unnecessary. 

A [demo](https://play.google.com/store/apps/details?id=at.aau.itec.android.mediaplayerdemo) is available on the Google Play Store.


Features
--------

 * Direct replacement for Android components
 * Frame-exact seeking
 * Playback speed adjustment
 * GLES shader support
 * Picture zooming/panning support
 * Frame extraction
 * Local files and network sources
 * Supports all Android [network protocols and media formats](http://developer.android.com/guide/appendix/media-formats.html)
 * DASH support
 * Lightweight (all components total to ~100kB)


Requirements
------------

 * Android API 16+ (Android 4.1 Jelly Bean)
 * optional: Adreno GPU
 * optional: OpenGL ES 3.0


Components
----------

The library is split into several logical components, comprising the base MediaPlayer and additional optional 
components that extend the functionality of the base.

### MediaPlayer ###

The base component provides the `MediaPlayer`, which can be used as a replacement for the Android 
[MediaPlayer](http://developer.android.com/reference/android/media/MediaPlayer.html), and the `VideoView`,
which can be used as a replacement for the Android [VideoView](http://developer.android.com/reference/android/widget/VideoView.html).
To load a video, use either the compatibility methods known from the Android API to specify a file or URI, or supply a `UriSource`.

### MediaPlayer-DASH ###

Extends the MediaPlayer base with DASH support. To play a DASH stream, supply the MediaPlayer or VideoView a
`DashSource` with the URI of the MPD file or an externally parsed/constructed `MPD` object, and an 
`AdaptationLogic`. MPDs can be externally parsed with the `DashParser`. This component comes with
two basic AdaptationLogic implementations, `ConstantPropertyBasedLogic` which selects a specified 
constant representation mode, and `SimpleRateBasedAdaptationLogic`, which monitors the bandwidth and 
tries to choose the best representation accordingly. It supports MP4, fragmented MP4 and WebM 
containers, with both file and byte-range requests.

MediaPlayer-DASH has external dependencies on [OkHttp](https://github.com/square/okhttp), 
[Okio](https://github.com/square/okio), and [ISO Parser](https://github.com/sannies/mp4parser).

### MediaPlayer-GLES ###

Extends the MediaPlayer base with a GLES surface and GLSL shader support. It provides the `GLVideoView`, 
a VideoView with a GL surface and a simple interface for custom shader effects. Effects implement
the `Effect` interface and can be dynamically parameterized. It also provides the `GLCameraView`, 
which is a camera preview widget with effect support. It comes with a few simple effects, e.g. 
a sobel edge detector, a simple toon effect and some 9x9 kernel effects. The GLES views can be zoomed
and panned with the typical touchscreen gestures.

### MediaPlayer-GLES-FlowAbs ###

This module adds the [FlowAbs](https://code.google.com/p/flowabs/) shader effect to the GLES component 
and demonstrates the possibility to construct and use very elaborate shaders. It also offers various
sub-effects that the flowabs-effect is composed of, including (flow-based) difference of Gaussians, 
color quantization and a tangent flow map.

### MediaPlayer-GLES-QrMarker ###

This module is another example of an effect composed of multiple shaders. It is taken from 
[QrMarker](https://github.com/thHube/QrMarker-ComputerVision) and provides a rather pointless and 
extremely slow QR marker identification effect, and a nice Canny edge detection effect.

### MediaPlayerDemo ###

This module is a demo app that incorporates all the main functionality of the MediaPlayer modules
and serves as an example on how they can be used and extended. It is available for download as 
[ITEC MediaPlayer Demo](https://play.google.com/store/apps/details?id=at.aau.itec.android.mediaplayerdemo) on the Google Play Store.


Known Issues
------------

* Video only, no audio support (the library was originally developed for frame-exact video inspection)
* DASH MPD parser is very basic and only tested for a few selected files (see the test URLs below)
* DASH SimpleRateBasedAdaptationLogic doesn't work too well in cases where the connection's max 
  bandwidth equals a representation's bandwidth requirement
* The FlowAbs OrientationAlignedBilateralFilterShaderProgram / FlowAbsBilateralFilterEffect does 
  not work correctly (wrong result) and is deactivated in the FlowAbs effect, making the effect 
  slightly less fancy
* GLCameraView's preview aspect ratio is slightly off
* GLES components work correctly on Adreno 320 (Nexus 7 2013) and Adreno 330 (Nexus 5), 
  but not on Tegra 3 (Nexus 7 2012) and Tegra 4 (Transformer TF701T)
    * NVidia Tegra does not seem to support framebuffers with 16bit color channel texture attachments 
      (as required by some flowabs effects) and crashes on the bilateral filtering shader for unknown reasons
* Similarity to Google's [ExoPlayer](https://github.com/google/ExoPlayer) library that was 
  released during the development of this library
* Exception handling needs to be improved


Online Streaming Test URLs
--------------------------

These URLs have been tested and definitely work in the demo app:

* HTTP streaming:
    * http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4
    * http://www-itec.uni-klu.ac.at/dash/js/content/bunny_4000.webm
* DASH streaming:
    * all DASH MPDs on the [ITEC DASH-JS](http://www-itec.uni-klu.ac.at/dash/?page_id=746) page:
        * MP4 3 rep 50kbps to 150kbps: http://www-itec.uni-klu.ac.at/dash/js/content/bunny_ibmff_240.mpd
        * MP4 7 rep 200kbps to 700kbps: http://www-itec.uni-klu.ac.at/dash/js/content/bunny_ibmff_360.mpd
        * MP4 4 rep 900kbps to 2000kbps: http://www-itec.uni-klu.ac.at/dash/js/content/bunny_ibmff_720.mpd
        * MP4 6 rep 2500kbps to 8000kbps: http://www-itec.uni-klu.ac.at/dash/js/content/bunny_ibmff_1080.mpd
        * WebM 5 rep 200 kbps to 2000 kbps: http://www-itec.uni-klu.ac.at/dash/js/content/sintel_multi_rep.mpd
        * WebM 7 rep 200 kbps to 4700 kbps: http://www-itec.uni-klu.ac.at/dash/js/content/bigbuckbunny.mpd
        * WebM 7 rep 1000 kbps to 8000 kbps: http://www-itec.uni-klu.ac.at/dash/js/content/bigbuckbunny_1080p.mpd
    * MP4 8 rep 250 to 6000 kbps: http://dj9wk94416cg5.cloudfront.net/sintel2/sintel2.mpd


Usage
-----

To use this library in your own project, check out the repository and either include the required
gradle modules directly, or run `gradlew publishDebugSnapshotPublicationToMavenLocal` to compile and 
install the modules to your local Maven repository and add one or more of the following dependencies:

    repositories {
        ...
        mavenLocal()
    }
    
    dependencies {
        compile 'at.aau.itec.android.mediaplayer:mediaplayer:1.0-SNAPSHOT'
        compile 'at.aau.itec.android.mediaplayer:mediaplayer-dash:1.0-SNAPSHOT'
        compile 'at.aau.itec.android.mediaplayer:mediaplayer-gles:1.0-SNAPSHOT'
        compile 'at.aau.itec.android.mediaplayer:mediaplayer-gles-flowabs:1.0-SNAPSHOT'
        compile 'at.aau.itec.android.mediaplayer:mediaplayer-gles-qrmarker:1.0-SNAPSHOT'
    }

The DASH module needs a recent version of the isoparser library from the 
[mp4parser](https://github.com/sannies/mp4parser) project. You can install it to your local maven 
repository by checking out my [dashfix branch](https://github.com/protyposis/mp4parser/tree/dashfix) and 
running `mvn install` in the `isoparser` subdirectory. Alternatively, you can check out a more recent 
version from the original repository where the dashfix is already merged 
([55e0e6c04f](https://github.com/sannies/mp4parser/tree/55e0e6c04f61b39d2af248daa2c3cde914ccc15f) and up), 
but then you need to adjust the version in the gradle dependency.


License
-------

Copyright (C) 2014 Mario Guggenberger, Institute of Information Technology, Alpen-Adria-Universität Klagenfurt <mario.guggenberger@aau.at>. 
This project is released under the terms of the GNU General Public License. See `LICENSE` for details.
