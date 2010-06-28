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
not keeping those in the repository. ^_^ You'll probably also need to
get version 3.3.940 of Xuggler from
http://xuggle.com/xuggler/downloads/releases.jsp. The .jar is in this
repo, but Xuggler depends on native libraries as well. And by the way,
you'll have to figure out how to compile MVTron's Java and Groovy code
against the .jars yourself, since there isn't any sort of handy build
script....

Yeah, this may make MVTron a pain to install! MVTron is a tool
primarily targeted at myself and secondarily targeted at programmers
and programming-tolerant artists. Ease-of-installation isn't a
priority yet.

For more information about the design goals and mechanisms of MVTron,
you can see my blog at http://rocketnia.wordpress.com/.

2010 Ross Angle
