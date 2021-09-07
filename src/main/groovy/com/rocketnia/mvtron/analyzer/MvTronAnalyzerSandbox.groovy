// MVTronAnalyzerSandbox.groovy
//
// Copyright 2009, 2010, 2021 Ross Angle

package com.rocketnia.mvtron.analyzer

import org.freedesktop.gstreamer.Bus
import org.freedesktop.gstreamer.Gst
import org.freedesktop.gstreamer.Pad
import org.freedesktop.gstreamer.PadProbeReturn
import org.freedesktop.gstreamer.PadProbeType
import org.freedesktop.gstreamer.Pipeline
import org.freedesktop.gstreamer.Version
import org.freedesktop.gstreamer.elements.AppSink

import com.rocketnia.mvtron.analyzer.scenedetectors.EuclideanSceneDetector


def read = System.in.newReader().&readLine


print '''\
This sandbox will loop through a video, printing the distances found
between each frame and the frame behind it, scaled to the range
0.0..1.0. Then, it will print the number of frames and how long the
loop took. Finally, it will print the top "unblurred" distances
greater than half the maximum unblurred distance, which should be good
places to look for scene breaks.

What file should be used? '''; def filename = read()
println ""


Gst.init Version.BASELINE, "MVTronAnalyzerSandbox"

def pipelineSpec = """
	filesrc name=src ! decodebin ! videoconvert
	! video/x-raw, format=xRGB ! appsink name=dst
"""
def pipe = (Pipeline) Gst.parseLaunch( pipelineSpec )
def src = pipe.getElementByName( "src" )
def dstAppSink = (AppSink) pipe.getElementByName( "dst" )
dstAppSink.set "sync", false
def dst = dstAppSink.getStaticPad( "sink" )

src.set "location", filename


SceneDetectorTool detector = EuclideanSceneDetector.makeTool()
IntArrayTimeWindower windower = detector.newWindower( 1 )
dst.addProbe( PadProbeType.BUFFER,
	new RgbArrayListenerTool( windower ) )
List< Double > distances = new ArrayList< Double >()
distances.add( (double)0 )
detector.addListener( {
	
	distances.add it
	println "distance: $it"
} as IDoubleListener )


// Since we added at least one probe, we add another to be processed
// after the others, which formally declares that the probes are done
// with this buffer.
dst.addProbe( PadProbeType.BUFFER, (Pad.PROBE) { pad, info ->
	return PadProbeReturn.HANDLED
} )

pipe.getBus().connect( (Bus.ERROR) { source, code, message ->
	System.err.println message
	Gst.quit()
} )
pipe.getBus().connect( (Bus.EOS) { source ->
	Gst.quit()
} )

long start = System.currentTimeMillis()
pipe.play()
Gst.main()
// The windower made above would be closed now, but that doesn't
// actually do anything in this case.
long time = System.currentTimeMillis() - start


int numberOfFrames = distances.size()

distances.add( (double)0 )

println ""
println "Done! There were $numberOfFrames frames, and they were" +
	" processed in ${time}ms."
println "Now for the scene detection:"
println ""


def mockBool = { double arg -> arg }
def not = { double arg -> 1 - arg }
def and = { double... args -> args.inject 1, { a, b -> a * b } }
def or =
	{ double... args -> not and( args.collect( not ) as double[] ) }

def unblurredDistances = numberOfFrames == 0 ? [] :
	[ 0 ] + (1..<numberOfFrames).collect { or(
		mockBool( distances[ it ] ),
		Math.min(
			mockBool( distances[ it ] ),
			Math.max( distances[ it - 1 ], distances[ it + 1 ] )
		)
	) }


if ( numberOfFrames != 0 )
{
	def bestUnblurredDistances =
		([] + unblurredDistances).sort().reverse()
	def threshold = bestUnblurredDistances.head() / 2
	
	bestUnblurredDistances.every { distance ->
		
		if ( distance < threshold )
			return false  // break
		
		// TODO: Fix this. It looks like it will display N duplicates
		// N^2 times.
		unblurredDistances.findIndexValues { it == distance }.each {
			
			println "Frame $it has unblurred distance $distance."
		}
		
		return true  // continue
	}
}

println ""
println "Done!"


// Close the file opened by GStreamer.
//
// TODO: See if there's a better way to do this.
//
System.exit 0
