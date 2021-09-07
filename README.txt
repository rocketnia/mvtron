                                MVTron
                                ======

MVTron is a project whose ultimate purpose is to be a tool for people
editing video (particularly AMV makers) to use in order to find scenes
that synchronize especially well with an audio track, which they might
have overlooked had they tried sifting through all their footage
manually. On the side, MVTron should also be able to compose music
videos completely on its own, given an audio track and some footage.

Heuristics are going to play a large role in how MVTron makes its
selections, but analytical methods, statistical AI techniques, and
manual configuration points potentially have their roles too.

So far, MVTron can detect scenes and motion intensity in video streams
and calculate the FFT and MFCCs of audio streams. The next priority is
to use these data streams together to coordinate the video with the
audio.

For the moment, these features take form as a scattered collection of
demos, JVM classes with "Sandbox" in their names, which can be run
from the command line. You'll need a video of your own handy; we're
not keeping those in the repository. ^_^

To use MVTron, you'll need an environment for JDK development with
Groovy and Gradle, with which you can obtain further dependencies from
Maven that are declared in build.gradle. You'll also need GStreamer
installed, and you'll need to add GStreamer's bin directory to your
`PATH`, which isn't something the GStreamer Windows installer does by
default.

2010, 2021 Ross Angle
