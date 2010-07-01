// MVTronAnalyzerSandbox.groovy
//
// Copyright 2009, 2010 Ross Angle

package com.rocketnia.mvtron.analyzer


// TODO: Decide what to do about the fact that some audio frames have
// different numbers of samples in them. For instance, it might be
// useful to build a listener that collects samples and meters them
// out in equally-sized chunks. What about audio frame timestamps
// though? The sample rate the audio frames report seems to stay
// constant, but maybe they're lying.

// TODO: Resolve all the rest of the TODOs.


def read = System.in.newReader().&readLine

print '''\
This sandbox will loop through a video (or an audio clip?) and perform
running calculations on the samples it gets, culminating in a
mel-frequency cepstrum. After some silence, it will output the number
of audio frames, how long it took to process them, and some summaries
of the calculation results.

Right now, the mel conversion is a bit slow.

What file should be used? '''; def filename = read()
println ""
// TODO: Either change the mel conversion to a faster form of
// resampling (such as looping through the corrected bins and setting
// them each to linear combinations of the nearest two original bins),
// or move it to a library.

import edu.emory.mathcs.jtransforms.dct.DoubleDCT_1D
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D
import java.awt.image.BufferedImage
import javax.swing.JFrame
import com.xuggle.mediatool.*
import com.xuggle.mediatool.event.*
import com.xuggle.xuggler.*
import com.xuggle.xuggler.video.IConverter
import com.rocketnia.mvtron.analyzer.scenedetectors.
	EuclideanSceneDetector


def reader = ToolFactory.makeReader( filename )


// This commented-out section implements playing a modified version of
// the stream with the volume raising and lowering between no volume
// and double volume according to a sine wave with a period of pi
// seconds.
//
/*
class MyMediaToolProxy extends MediaToolAdapter
{
	public void onAudioSamples( IAudioSamplesEvent event )
	{
		def samples = event.audioSamples
		
		long numSamples = samples.numSamples
		int channels = samples.channels
		def format = samples.format
		
		assert format == IAudioSamples.Format.FMT_S16
		
		// NOTE: Apparently, making a new AudioSamplesEvent doesn't
		// work so well. The player will throw an
		// UnsupportedOperationException if the new event's source is
		// this object instead of the original source, and that's not
		// really the issue, 'cause it'll still throw a
		// NullPointerException if the event is different at all.
		// Because of this, the code here now destructively modifies
		// the IAudioSamples object behind the original event. It
		// works great for now, but I think it could give surprising
		// behavior if the modification is picked up by a listener
		// running in /parallel/ to this one, such as one that's
		// listening to the same source. In short, this listener hogs
		// the road.
//		def newSamples =
//			IAudioSamples.make( numSamples, channels, format )
		
		double volumeFactor =
			Math.sin( event.timeStamp / 1000000.0 * 2 ) + 1
		for ( long s = 0; s < numSamples; s++ )
		for ( c in 0..<channels )
		{
			int original = samples.getSample( s, c, format )
			int newValue = volumeFactor * original
			samples.setSample s, c, format, newValue
		}
		
//		def newEvent = new AudioSamplesEvent(
//			event.source,
//			newSamples,
//			event.streamIndex
//		)
		
		super.onAudioSamples event
	}
}

def readerProxy = new MyMediaToolProxy()
reader.addListener readerProxy


def viewer = ToolFactory.makeViewer( true, JFrame.EXIT_ON_CLOSE )
readerProxy.addListener viewer
*/


def hertzToMel = { 1127 * Math.log( it / 700 + 1 ) }


def maxAbsolutes = []
def minAbsolutes = []
def meanAbsolutes = []
def rootMeanSquares = []
def peakHertzFrequencyHistogram = [:]
def peakMelFrequencyHistogram = [:]
def peakMfccHistogram = [:]
reader.addListener( [ onAudioSamples: { IAudioSamplesEvent event ->
	
	def samples = event.audioSamples
	
	long numSamples = samples.numSamples
	int channels = samples.channels
	def format = samples.format
	
	assert format == IAudioSamples.Format.FMT_S16
	
	int maxAbsolute = 0
	int minAbsolute = 0x10000
	long sumAbsolute = 0
	long sumSquare = 0
	for ( long s = 0; s < numSamples; s++ )
	for ( c in 0..<channels )
	{
		int sample = samples.getSample( s, c, format )
		int absolute = sample.abs()
		maxAbsolute = Math.max( maxAbsolute, absolute )
		minAbsolute = Math.min( minAbsolute, absolute )
		sumAbsolute += absolute
		sumSquare += sample * sample
	}
	
	if ( channels != 0 )
	{
		int sampleRate = samples.sampleRate
		int numSamplesUsed = Math.min( Integer.MAX_VALUE, numSamples )
		
		// Collect the samples given by the event. We're only going to
		// look at audio channel zero.
		List sampleList = (0..<numSamplesUsed).
			collect { samples.getSample it, 0, format }
		
		// Calculate the FFT, which gives powers for each of several
		// frequency bins, the first one smaller than the rest.
		//
		// We're ignoring the complex component of the FFT and
		// ignoring the negative half--since it's real numbers we're
		// plugging in, the negative half is the complex conjugate of
		// the positive half--so the first bin (the one at 0Hz) is
		// half the size of the rest.
		double[] fftLibraryResult = sampleList
		new DoubleFFT_1D( numSamplesUsed ).
			realForward fftLibraryResult
		List complexBins = [] + (fftLibraryResult as List)
		if ( 1 < numSamplesUsed )
		{
			complexBins.add complexBins[ 1 ]
			complexBins[ 1 ] = (double)0
		}
		int numberOfBins = (complexBins.size() + 1) >>> 1
		List bins =
			(0..<numberOfBins).collect { complexBins[ it << 1 ] }
		List binEndsInHertz = [ (double)0 ] + (1..numberOfBins).
			collect { (it - (double)0.5) * sampleRate /
				numSamplesUsed }
		
		// Resample the frequency spectrum by converting the
		// frequencies to mel (a logarithmic transformation of Hz
		// meant to simulate human frequency evaluation) and
		// reorganizing them in new, equally-spaced bins in
		// preparation for sending them through another FFT. Wikipedia
		// doesn't say how big these new bins should be, so we're
		// choosing for now that the number of mel in a bin here will
		// be just the right size so that we have the same number of
		// bins as before.
		//
		// (Actually, what Wikipedia says to do is to use "triangular
		// overlapping windows" somehow. I think what that's supposed
		// to mean is that the original bins should be interpolated
		// linearly in order to determine the continuous function to
		// re-approximate. To explain a little more, each bin value's
		// contribution in linear interpolation would be shaped like a
		// triangle, and these triangles would overlap. However, this
		// isn't exactly what we're doing here. Instead of
		// interpolating the function and taking new sample values at
		// regular intervals, we're essentially treating the original
		// bins as a zero-slope bar graph, drawing a bunch of new
		// lines on the bar graph, and measuring how much area is
		// between the new lines.)
		List binEndsInMel = binEndsInHertz.collect( hertzToMel )
		double correctedBinSizeInMel =
			binEndsInMel.last() / numberOfBins
		List correctedBinEndsInMel =
			(0..numberOfBins)*.multiply( correctedBinSizeInMel )
		
		// We'll start the corrected bins at values of zero and
		// apportion each of the original bins one at a time.
		List correctedBins = [ 0 ] * numberOfBins
		numberOfBins.times {
			
			double binValue = bins[ it ]
			double binStartInMel = binEndsInMel[ it ]
			double binStopInMel = binEndsInMel[ it + 1 ]
			double binSizeInMel = binStopInMel - binStartInMel
			double binValuePerMel = binValue / binSizeInMel
			
			int startPartialBinStopIndex = correctedBinEndsInMel.
				findIndexOf { binStartInMel <= it }
			assert startPartialBinStopIndex in 0..numberOfBins
			double startPartialBinStop =
				correctedBinEndsInMel[ startPartialBinStopIndex ]
			double startPartialBinSizeInMel =
				startPartialBinStop - binStartInMel
			
			int stopPartialBinStartIndex = correctedBinEndsInMel.
				findLastIndexOf { it <= binStopInMel }
			assert stopPartialBinStartIndex in 0..numberOfBins
			double stopPartialBinStart =
				correctedBinEndsInMel[ stopPartialBinStartIndex ]
			double stopPartialBinSizeInMel =
				binStopInMel - stopPartialBinStart
			
			if ( stopPartialBinStartIndex < startPartialBinStopIndex )
			{
				// If this bin is wholly inside one of the corrected
				// bins, contribute its whole value to that bin.
				
				assert stopPartialBinStartIndex + 1 ==
					startPartialBinStopIndex
				
				correctedBins[ stopPartialBinStartIndex ] += binValue
			}
			else
			{
				// If this bin's start partially overlaps one of the
				// corrected bins, fill that corrected bin
				// appropriately.
				if ( startPartialBinSizeInMel != 0 )
				{
					assert (startPartialBinStopIndex - 1) in
						0..<numberOfBins
					
					correctedBins[ startPartialBinStopIndex - 1 ] +=
						binValuePerMel * startPartialBinSizeInMel
				}
				
				// Fill all the corrected bins this bin wholly
				// envelops.
				(startPartialBinStopIndex..<stopPartialBinStartIndex).
					each {
					
					correctedBins[ it ] +=
						binValuePerMel * correctedBinSizeInMel
				}
				
				// If this bin's stop partially overlaps one of the
				// corrected bins, fill that corrected bin
				// appropriately.
				if ( stopPartialBinSizeInMel != 0 )
				{
					assert stopPartialBinStartIndex in
						0..<numberOfBins
					
					correctedBins[ stopPartialBinStartIndex ] +=
						binValuePerMel * stopPartialBinSizeInMel
				}
			}
		}
		
		
		// Take the discrete cosine transform (specifically, the
		// DCT-II) of the logs of the mel-frequency powers in order to
		// get the mel-frequency cepstral coefficients (MFCCs), which
		// make up the mel-frequency cepstrum (MFC). Wikipedia doesn't
		// say the log of the *magnitude* should be taken, but it
		// mentions that kind of thing on the cepstrum page, so we're
		// doing that instead. However, we've been ignoring the
		// imaginary parts, so we might be skewing the result somewhat
		// by treating them as though they're zero.
		double[] melFrequencyCepstrumArray =
			correctedBins*.abs().collect( Math.&log )
		new DoubleDCT_1D( numberOfBins ).
			forward melFrequencyCepstrumArray, false
		List melFrequencyCepstrum =
			[] + (melFrequencyCepstrumArray as List)
		// NOTE: If the corrected bin values are all zero, their logs
		// will all be the IEEE negative infinity value, and the MFCCs
		// will all be NaN. I don't know what happens if just some of
		// them are zero, but it's probably the same thing.
		
		
		// Now that the interesting raw stuff has been calculated,
		// take measurements of it.
		
		double thisPeakHertzValue = Double.NEGATIVE_INFINITY
		Set thesePeakHertzRanges = []
		for ( binIndex in 0..<numberOfBins )
		{
			double thisCandidate = bins[ binIndex ]
			
			if ( thisCandidate < thisPeakHertzValue )
				continue
			
			if ( thisPeakHertzValue < thisCandidate )
				thesePeakHertzRanges = []
			
			thesePeakHertzRanges.add(
				((int)binEndsInHertz[ binIndex ])..
				((int)binEndsInHertz[ binIndex + 1 ])
			)
			thisPeakHertzValue = thisCandidate
		}
		
		assert !thesePeakHertzRanges.isEmpty()
		peakHertzFrequencyHistogram[ thesePeakHertzRanges ] =
			(peakHertzFrequencyHistogram[ thesePeakHertzRanges ]
				?: 0) + 1
		
		double thisPeakMelValue = Double.NEGATIVE_INFINITY
		Set thesePeakMelRanges = []
		for ( binIndex in 0..<numberOfBins )
		{
			double thisCandidate = correctedBins[ binIndex ]
			
			if ( thisCandidate < thisPeakMelValue )
				continue
			
			if ( thisPeakMelValue < thisCandidate )
				thesePeakMelRanges = []
			
			thesePeakMelRanges.add(
				((int)correctedBinEndsInMel[ binIndex ])..
				((int)correctedBinEndsInMel[ binIndex + 1 ])
			)
			thisPeakMelValue = thisCandidate
		}
		
		assert !thesePeakMelRanges.isEmpty()
		peakMelFrequencyHistogram[ thesePeakMelRanges ] =
			(peakMelFrequencyHistogram[ thesePeakMelRanges ] ?: 0) + 1
		
		double thisPeakMfccValue = Double.NEGATIVE_INFINITY
		Set thesePeakMfccIndices = []
		for ( binIndex in 0..<numberOfBins )
		{
			double thisCandidate = melFrequencyCepstrum[ binIndex ]
			
			if ( thisCandidate < thisPeakMfccValue )
				continue
			
			if ( thisPeakMfccValue < thisCandidate )
				thesePeakMfccIndices = []
			
			thesePeakMfccIndices.add binIndex
			thisPeakMfccValue = thisCandidate
		}
		
		assert !thesePeakMfccIndices.isEmpty()
		peakMfccHistogram[ thesePeakMfccIndices ] =
			(peakMfccHistogram[ thesePeakMfccIndices ] ?: 0) + 1
	}
	
	maxAbsolutes.add( (double)maxAbsolute / 0x10000 )
	minAbsolutes.add( (double)minAbsolute / 0x10000 )
	meanAbsolutes.add( (double)sumAbsolute / numSamples / 0x10000 )
	rootMeanSquares.add(
		Math.sqrt( (double)sumSquare / numSamples ) / 0x10000 )
} ] as MediaToolAdapter )


long start = System.currentTimeMillis()
while ( reader.readPacket() == null ) {}
long time = System.currentTimeMillis() - start


int numberOfFrames = rootMeanSquares.size()

def rangeCompare = { Range a, Range b ->
	
	int fromDifference = a.from - b.from
	
	return fromDifference == 0 ? a.to - b.to : fromDifference
}

def setCompare = { elementCompare = { a, b -> a <=> b } -> return {
	Set a, Set b ->
	
	List sortedA = ([] + a).sort( elementCompare )
	List sortedB = ([] + b).sort( elementCompare )
	
	int aSize = a.size()
	int bSize = b.size()
	
	for ( i in 0..<Math.min( aSize, bSize ) )
	{
		def potentialResult =
			elementCompare( sortedA[ i ], sortedB[ i ] )
		if ( potentialResult != 0 )
			return potentialResult
	}
	
	return aSize - bSize
} }


println ""
println "Done! There were $numberOfFrames audio frames, and they were" +
	" processed in"
println "${time}ms. Now for the calculated values:"
println ""

println "maxAbsolutes max: " + maxAbsolutes.max()
println "minAbsolutes min: " + minAbsolutes.min()
println "meanAbsolutes mean: " + meanAbsolutes.sum() / numberOfFrames
println "rootMeanSquares rms: " + Math.sqrt(
	rootMeanSquares.collect { it * it }.sum() / numberOfFrames )

println ""
println "peakHertzFrequencyHistogram:"

([] + peakHertzFrequencyHistogram.keySet()).
	sort( setCompare( rangeCompare ) ).each { key ->
	
	println "${([] + key).sort( rangeCompare )} -> " +
		peakHertzFrequencyHistogram[ key ]
}

println ""
println "peakMelFrequencyHistogram:"

([] + peakMelFrequencyHistogram.keySet()).
	sort( setCompare( rangeCompare ) ).each { key ->
	
	println "${([] + key).sort( rangeCompare )} -> " +
		peakMelFrequencyHistogram[ key ]
}

println """
peakMfccHistogram (which should be quite trivial; this isn't a very
evocative demonstration of the MFCC):"""
// TODO: Find a better demonstration of the MFCC.

([] + peakMfccHistogram.keySet()).sort().each { key ->
	
	println "$key -> $peakMfccHistogram[ key ]"
}

println ""
println "Done!"
