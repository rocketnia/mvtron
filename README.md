# MVTron

[![CI](https://github.com/rocketnia/mvtron/actions/workflows/ci.yml/badge.svg)](https://github.com/rocketnia/mvtron/actions/workflows/ci.yml)

MVTron is a project whose ultimate purpose is to be a tool for people editing video (particularly AMV makers) to use in order to find scenes that synchronize especially well with an audio track, which they might have overlooked had they tried sifting through all their footage manually. On the side, MVTron should also be able to compose music videos completely on its own, given an audio track and some footage.

Heuristics are going to play a large role in how MVTron makes its selections, but analytical methods, statistical AI techniques, and manual configuration points potentially have their roles too.

So far, MVTron can detect scenes and motion intensity in video streams and calculate the FFT and MFCCs of audio streams. The next priority is to use these data streams together to coordinate the video with the audio.

For the moment, these features take form as a scattered collection of command-line demos.


## Installation and use

To run the MVTron demos, you'll need a few things:

  * A distribution of the JDK. We've used Eclipse Temurin 17.0.1+12 from Adoptium, but MVTron should work on some older JVM versions as well.

  * A system-wide installation of GStreamer that can be linked against. You'll need to add GStreamer's bin directory to your `PATH`, which isn't something the GStreamer Windows installer does by default.

  * Your own video file to put through MVTron's analysis. Many formats are supported.

Then, edit build.gradle to uncomment just the line with the MVTron demo entrypoint you want to test:

```groovy
application {
//	mainClass = 'com.rocketnia.mvtron.analyzer.MvTronAnalyzerJavaSandbox'
	mainClass = 'com.rocketnia.mvtron.analyzer.MvTronAnalyzerSandbox'
//	mainClass = 'com.rocketnia.mvtron.analyzer.MvTronAudioSandbox'
//	mainClass = 'com.rocketnia.mvtron.analyzer.MvTronBeatSandbox'
}
```

Then, use the Gradle wrapper to build an installation directory in build/install/mvtron/:

```bash
./gradlew installDist
```

And run the application:

```bash
build/install/mvtron/bin/mvtron
```

Each of these demos will interactively ask for the path to the video file you'd like to analyze.
