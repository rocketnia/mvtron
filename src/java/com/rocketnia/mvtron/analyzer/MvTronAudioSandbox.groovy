package com.rocketnia.mvtron.analyzer


def read = System.in.newReader().&readLine;

println "This sandbox will loop through a video (or an audio clip?) and perform"
println "running calculations on the samples it gets. After some silence, it"
println "will output the number of audio frames, how long it took to process"
println "them, and what the calculation results are."
println ""
print   "What file should be used? "; def filename = read();
println ""

import java.awt.image.BufferedImage
import javax.swing.JFrame
import com.xuggle.mediatool.*
import com.xuggle.mediatool.event.*
import com.xuggle.xuggler.*
import com.xuggle.xuggler.video.IConverter
import com.rocketnia.mvtron.analyzer.scenedetectors.EuclideanSceneDetector


def reader = ToolFactory.makeReader( filename );


// This commented-out section implements playing a modified version of the
// stream with the volume raising and lowering between no volume and double
// volume according to a sine wave with a period of pi seconds.
//
/*
class MyMediaToolProxy extends MediaToolAdapter
{
	public void onAudioSamples( IAudioSamplesEvent event )
	{
		def samples = event.audioSamples;
		
		long numSamples = samples.numSamples;
		int channels = samples.channels;
		def format = samples.format;
		
		assert format == IAudioSamples.Format.FMT_S16;
		
		// NOTE: Apparently, making a new AudioSamplesEvent doesn't work so
		// well. The player will throw an UnsupportedOperationException if the
		// new event's source is this object instead of the original source, and
		// that's not really the issue, 'cause it'll still throw a
		// NullPointerException if the event is different at all. Because of
		// this, the code here now destructively modifies the IAudioSamples
		// object behind the original event. It works great for now, but I think
		// it could give surprising behavior if the modification is picked up by
		// a listener running in /parallel/ to this one, such as one that's
		// listening to the same source. In short, this listener hogs the road.
//		def newSamples = IAudioSamples.make( numSamples, channels, format );
		
		double volumeFactor = Math.sin( event.timeStamp / 1000000.0 * 2 ) + 1;
		for ( long s = 0; s < numSamples; s++ )
		for ( c in 0..<channels )
		{
			int original = samples.getSample( s, c, format );
			int newValue = volumeFactor * original;
			samples.setSample( s, c, format, newValue );
		}
		
//		def newEvent = new AudioSamplesEvent(
//			event.source,
//			newSamples,
//			event.streamIndex
//		);
		
		super.onAudioSamples( event );
	}
}

def readerProxy = new MyMediaToolProxy();
reader.addListener( readerProxy );


def viewer = ToolFactory.makeViewer( true, JFrame.EXIT_ON_CLOSE );
readerProxy.addListener( viewer );
*/


def maxAbsolutes = [];
def minAbsolutes = [];
def meanAbsolutes = [];
def rootMeanSquares = [];
reader.addListener( [
	onAudioSamples: { IAudioSamplesEvent event ->
		
		def samples = event.audioSamples;
		
		long numSamples = samples.numSamples;
		int channels = samples.channels;
		def format = samples.format;
		
		assert format == IAudioSamples.Format.FMT_S16;
		
		int maxAbsolute = 0;
		int minAbsolute = 0x10000;
		long sumAbsolute = 0;
		long sumSquare = 0;
		for ( long s = 0; s < numSamples; s++ )
		for ( c in 0..<channels )
		{
			int sample = samples.getSample( s, c, format );
			int absolute = sample.abs();
			maxAbsolute = Math.max( maxAbsolute, absolute );
			minAbsolute = Math.min( minAbsolute, absolute );
			sumAbsolute += absolute;
			sumSquare += sample * sample;
		}
		
		maxAbsolutes.add( (double)maxAbsolute / 0x10000 );
		minAbsolutes.add( (double)minAbsolute / 0x10000 );
		meanAbsolutes.add( (double)sumAbsolute / numSamples / 0x10000 );
		rootMeanSquares.add(
			Math.sqrt( (double)sumSquare / numSamples ) / 0x10000
		);
	}
] as MediaToolAdapter );


long start = System.currentTimeMillis();
while ( reader.readPacket() == null ) {}
long time = System.currentTimeMillis() - start;


int numberOfFrames = rootMeanSquares.size();


println ""
println "Done! There were ${numberOfFrames} audio frames, and they were" +
	" processed in"
println "${time}ms. Now for the calculated values:"
println ""

println( "maxAbsolutes max: " + maxAbsolutes.max() );
println( "minAbsolutes min: " + minAbsolutes.min() );
println( "meanAbsolutes mean: " + meanAbsolutes.sum() / numberOfFrames );
println( "rootMeanSquares rms: " +
	Math.sqrt( rootMeanSquares.collect { it * it }.sum() / numberOfFrames ) );

println ""
println "Done!"