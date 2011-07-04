// MVTronAnalyzerJavaSandbox.java
//
// Copyright 2009, 2011 Ross Angle

package com.rocketnia.mvtron.analyzer


// TODO: See if this even works. It was found neglected in an old working copy
// of MVTron, and currently not even the other parts of MVTron are linking
// correctly for me (Ross Angle). It probably wasn't committed since it didn't
// do exactly what it set out to do, which was "culminating in a guess at the
// beat of the audio."


// TODO: Decide what to do about the fact that some audio frames have different
// numbers of samples in them. For instance, it might be useful to build a
// listener that collects samples and meters them out in equally-sized chunks.
// What about audio frame timestamps though? The sample rate the audio frames
// report seems to stay constant, but maybe they're lying.

// TODO: Resolve all the rest of the TODOs.


def consoleInput = System.in.newReader();
def read = { -> System.out.flush(); consoleInput.readLine(); };

println "This sandbox will loop through a video (or an audio clip?) and perform"
println "running calculations on the samples it gets, culminating in a guess"
println "at the beat of the audio."
println ""
print   "What file should be used? "; def filename = read();
println ""

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D
import java.awt.image.BufferedImage
import javax.swing.JFrame
import com.xuggle.mediatool.*
import com.xuggle.mediatool.event.*
import com.xuggle.xuggler.*
import com.xuggle.xuggler.video.IConverter
import com.rocketnia.mvtron.analyzer.scenedetectors.EuclideanSceneDetector


def reader = ToolFactory.makeReader( filename );


def hertzToMel = { 1127 * Math.log( it / 700 + 1 ) };


def sampleCountList = [];
def meanAbsoluteGainList = [];
def rootMeanSquareGainList = [];
def meanAbsoluteRealHertzPowerList = [];
def rootMeanSquareRealHertzPowerList = [];
def meanAbsoluteRealMelPowerList = [];
def rootMeanSquareRealMelPowerList = [];
def expectedRealHertzFrequencyList = [];
def varianceRealHertzFrequencyList = [];
def expectedRealMelFrequencyList = [];
def varianceRealMelFrequencyList = [];
def expectedRealHertzAbsolutesList = [];
def varianceRealHertzAbsolutesList = [];
def expectedRealMelAbsolutesList = [];
def varianceRealMelAbsolutesList = [];
def expectedRealHertzSquaresList = [];
def varianceRealHertzSquaresList = [];
def expectedRealMelSquaresList = [];
def varianceRealMelSquaresList = [];
def meanAbsoluteRealHertzDiff = [];
def rootMeanSquareRealHertzDiff = [];
def meanAbsoluteRealMelDiff = [];
def rootMeanSquareRealMelDiff = [];

List previousBins = null;

reader.addListener( [
	onAudioSamples: { IAudioSamplesEvent event ->
		
		def sampleRepresentative = event.audioSamples;
		
		// TODO: Handle really big numbers of samples.
		int numberOfSamples =
			Math.min( Integer.MAX_VALUE, sampleRepresentative.numSamples );
		def format = sampleRepresentative.format;
		int sampleRate = sampleRepresentative.sampleRate;
		
		// TODO: Handle numbers of channel other than one.
		assert 0 < sampleRepresentative.channels;
		
		// Collect the samples given by the event. We're only going to look
		// at audio channel zero.
		List samples = (0..<numberOfSamples).
			collect { sampleRepresentative.getSample( it, 0, format ) };
		
		// TODO: Handle sample formats other than S16.
		assert format == IAudioSamples.Format.FMT_S16;
		
		// Measure some values.
		sampleCountList.add( numberOfSamples );
		meanAbsoluteGainList.add(
			samples*.abs().sum( (long)0 ) / (double)numberOfSamples / 0x10000
		);
		rootMeanSquareGainList.add( Math.sqrt(
			samples.collect { it * it }.sum( (long)0 ) / (double)numberOfSamples
		) / 0x10000 );
		
		// Calculate the FFT, which gives powers for each of several frequency
		// bins, the first one smaller than the rest.
		//
		// We're ignoring the complex component of the FFT and ignoring the
		// negative half--since it's real numbers we're plugging in, the
		// negative half is the complex conjugate of the positive half--so the
		// first bin (the one starting at 0Hz) is half the size of the rest.
		//
		// We're also normalizing the samples to the range [-0.5,0.5) by
		// dividing by 0x10000, the number of possible 16-bit integer values.
		double[] fftLibraryResult = samples*.div( 0x10000 );
		new DoubleFFT_1D( numberOfSamples ).realForward( fftLibraryResult );
		List complexBins = [] + (fftLibraryResult as List);
		if ( 1 < numberOfSamples )
		{
			complexBins.add( complexBins[ 1 ] );
			complexBins[ 1 ] = (double)0;
		}
		int numberOfBins = ((complexBins.size() + 1) >>> 1);
		List bins = (0..<numberOfBins).collect { complexBins[ it << 1 ] };
		List hertzBinEndsList = [ (double)0 ] + (1..numberOfBins).
			collect { (it - (double)0.5) * sampleRate / numberOfSamples };
		def hertzBinEnds = { hertzBinEndsList[ it ] };
		def melBinEnds = { hertzToMel( hertzBinEndsList[ it ] ) };
		double totalWidthInHertz = hertzBinEndsList.last();
		// Note that the following hertzToMel use is only valid because the
		// interval begins with zero Hertz, which is the same as zero mel.
		double totalWidthInMel = hertzToMel( totalWidthInHertz );
		def hertzBinWidth = { hertzBinEnds( it + 1 ) - hertzBinEnds( it ) };
		def melBinWidth = { melBinEnds( it + 1 ) - melBinEnds( it ) };
		
		// Measure some more values.
		// TODO: These are silly values; they treat the power at a bin as though
		// it's the power at every frequency in the bin, rather than as a
		// definite integral of the range of the frequency-power function
		// encompassing the bin. Correcting this would make
		// meanAbsoluteRealHertzPowerList and meanAbsoluteRealMelPowerList
		// proportional by a constant times sampleCountList, and a few other
		// things would probably cancel out too, but I haven't thought about it
		// much. This is high-priority.
		List hertzWeightedBins =
			(0..<numberOfBins).collect { bins[ it ] * hertzBinWidth( it ) };
		meanAbsoluteRealHertzPowerList.add(
			hertzWeightedBins*.abs().sum( (double)0 ) / totalWidthInHertz
		);
		rootMeanSquareRealHertzPowerList.add( Math.sqrt(
			(0..<numberOfBins).collect { bins[ it ] * hertzWeightedBins[ it ] }.
				sum( (double)0 ) / totalWidthInHertz
		) );
		List melWeightedBins =
			(0..<numberOfBins).collect { bins[ it ] * melBinWidth( it ) };
		meanAbsoluteRealMelPowerList.add(
			melWeightedBins*.abs().sum( (double)0 ) / totalWidthInMel
		);
		rootMeanSquareRealMelPowerList.add( Math.sqrt(
			(0..<numberOfBins).collect { bins[ it ] * melWeightedBins[ it ] }.
				sum( (double)0 ) / totalWidthInMel
		) );
		double binsSum = bins.sum();
		
		// This is how the expected value and variance calculations have been
		// derived (both for the Hertz scale and for the mel scale):
		//
		// E( Freq )
		// Int[ freq * binValueForFreq( freq ) / weightedBinsSum, freq in freqs ]
		// Int[ freq * binValueForFreq( freq ), freq in freqs ] / weightedBinsSum
		// Sum[ Int[ freq * binValueForFreq( freq ), freq in bin i ], i in bin indices ] / weightedBinsSum
		// Sum[ Int[ freq * bins[ i ], freq in bin i ], i in bin indices ] / weightedBinsSum
		// Sum[ bins[ i ] * Int[ freq, freq in bin i ], i in bin indices ] / weightedBinsSum
		// Sum[ bins[ i ] * (binStop[ i ] ^ 2 / 2 - binStart[ i ] ^ 2 / 2), i in bin indices ] / weightedBinsSum
		// Sum[ bins[ i ] * (binStop[ i ] ^ 2 - binStart[ i ] ^ 2), i in bin indices ] / weightedBinsSum / 2
		// Sum[ bins[ i ] * (binStop[ i ] ^ 2 - binStart[ i ] ^ 2), i in bin indices ] / Sum[ bins[ i ] * (binStop[ i ] - binStart[ i ]), i in bin indices ] / 2
		// Sum[ bins[ i ] * (binStop[ i ] - binStart[ i ]) * (binStop[ i ] + binStart[ i ]), i in bin indices ] / Sum[ bins[ i ] * (binStop[ i ] - binStart[ i ]), i in bin indices ] / 2
		//
		// Var( Freq )
		// E( (Freq - E( Freq )) ^ 2 )
		// Int[ (freq - E( Freq )) ^ 2 * binValueForFreq( freq ) / weightedBinsSum, freq in freqs ]
		// Int[ (freq - E( Freq )) ^ 2 * binValueForFreq( freq ), freq in freqs ] / weightedBinsSum
		// Sum[ Int[ (freq - E( Freq )) ^ 2 * binValueForFreq( freq ), freq in bin i ], i in bin indices ] / weightedBinsSum
		// Sum[ Int[ (freq - E( Freq )) ^ 2 * bins[ i ], freq in bin i ], i in bin indices ] / weightedBinsSum
		// Sum[ bins[ i ] * Int[ (freq - E( Freq )) ^ 2, freq in bin i ], i in bin indices ] / weightedBinsSum
		// Sum[ bins[ i ] * ((binStop[ i ] - E( Freq )) ^ 3 / 3 - (binStart[ i ] - E( Freq )) ^ 3 / 3), i in bin indices ] / weightedBinsSum
		// Sum[ bins[ i ] * ((binStop[ i ] - E( Freq )) ^ 3 - (binStart[ i ] - E( Freq )) ^ 3), i in bin indices ] / weightedBinsSum / 3
		// Sum[ bins[ i ] * ((binStop[ i ] - E( Freq )) ^ 3 - (binStart[ i ] - E( Freq )) ^ 3), i in bin indices ] / Sum[ bins[ i ] * (binStop[ i ] - binStart[ i ]), i in bin indices ] / 3
		//
		// This numerator term in Var( Freq ) can also have a
		// (binStop[ i ] - binStart[ i ]) factored out...
		//   (a - m) ^ 3 - (b - m) ^ 3
		//   a^3 - 3ma^2 + 3am^2 - m^3 - (b^3 - 3mb^2 + 3bm^2 - m^3)
		//   (a^3 - b^3) - 3(a^2 - b^2)m + 3(a - b)m^2
		//   (a - b) * ((a^2 + ab + b^2) - 3(a + b)m + 3m^2)
		//   (a - b) * ((a + b)^2 - 4(a + b)m + 4m^2 - m^2 + (a + b)m - ab)
		//   (a - b) * ((a + b - 2m)^2 - m^2 + (a + b)m - ab)
		//   (a - b) * ((a + b - 2m)^2 - (a - m) * (b - m))
		//   (a - b) * ((a - m + b - m)^2 - (a - m) * (b - m))
		// ...but it doesn't save on much computation in this case (one multiply
		// less, in favor of an addition and two subtractions, and a little bit
		// less chance of catastrophic cancellation), much less cancel things
		// out, so it would probably just make things more confusing than they
		// have to be.
		//
		// (Yeah, the above factoring could be a much simpler process if the
		// (a - m) and (b - m) terms are never decomposed, but this way the
		// fully expanded form is more visible in the intermediate steps.)
		def weightedExpectedValueAndVariance = { myBins, ends ->
			
			def binIndices = 0..<myBins.size();
			
			List weightedBins = binIndices.
				collect { myBins[ it ] * (ends( it + 1 ) - ends( it )) };
			
			double myBinsSum = myBins.sum( (double)0 );
			double weightedBinsSum = weightedBins.sum( (double)0 );
			
			double expectedValue = binIndices.
				collect { weightedBins[ it ] * (ends( it ) + ends( it + 1 )) }.
				sum( (double)0 ) / weightedBinsSum / 2;
			
			double variance = binIndices.collect {
				
				double startDeviation = ends( it ) - expectedValue;
				double stopDeviation = ends( it + 1 ) - expectedValue;
				
				return (
					stopDeviation * stopDeviation * stopDeviation -
					startDeviation * startDeviation * startDeviation
				) * myBins[ it ];
			}.sum( (double)0 ) / weightedBinsSum / 3;
			
			return [ expectedValue, variance ];
		};
		
		def ( double expectedHertzFrequency, double varianceHertzFrequency ) =
			weightedExpectedValueAndVariance( bins, hertzBinEnds );
		def ( double expectedMelFrequency, double varianceMelFrequency ) =
			weightedExpectedValueAndVariance( bins, melBinEnds );
		def absoluteBins = bins*.abs();
		def ( double expectedHertzAbsolutes, double varianceHertzAbsolutes ) =
			weightedExpectedValueAndVariance( absoluteBins, hertzBinEnds );
		def ( double expectedMelAbsolutes, double varianceMelAbsolutes ) =
			weightedExpectedValueAndVariance( absoluteBins, melBinEnds );
		def squareBins = bins.collect { it * it };
		def ( double expectedHertzSquares, double varianceHertzSquares ) =
			weightedExpectedValueAndVariance( squareBins, hertzBinEnds );
		def ( double expectedMelSquares, double varianceMelSquares ) =
			weightedExpectedValueAndVariance( squareBins, melBinEnds );
		
		expectedRealHertzFrequencyList.add( expectedHertzFrequency );
		varianceRealHertzFrequencyList.add( varianceHertzFrequency );
		expectedRealMelFrequencyList.add( expectedMelFrequency );
		varianceRealMelFrequencyList.add( varianceMelFrequency );
		expectedRealHertzAbsolutesList.add( expectedHertzAbsolutes );
		varianceRealHertzAbsolutesList.add( varianceHertzAbsolutes );
		expectedRealMelAbsolutesList.add( expectedMelAbsolutes );
		varianceRealMelAbsolutesList.add( varianceMelAbsolutes );
		expectedRealHertzSquaresList.add( expectedHertzSquares );
		varianceRealHertzSquaresList.add( varianceHertzSquares );
		expectedRealMelSquaresList.add( expectedMelSquares );
		varianceRealMelSquaresList.add( varianceMelSquares );
		
		
		if ( previousBins != null )
		{
			if ( previousBins.size() == numberOfBins )
			{
				// Calculate the intensity of change from the previous FFT
				// result to this one in several ways.
				
				List diff = (0..<numberOfBins).
					collect { bins[ it ] - previousBins[ it ] };
				List squareDiff = diff.collect { it * it };
				
				meanAbsoluteRealHertzDiff.add( (0..<numberOfBins).
					collect { diff[ it ].abs() * hertzBinWidth( it ) }.
					sum( (double)0 ) / totalWidthInHertz
				);
				rootMeanSquareRealHertzDiff.add( Math.sqrt( (0..<numberOfBins).
					collect { squareDiff[ it ] * hertzBinWidth( it ) }.
					sum( (double)0 ) / totalWidthInHertz
				) );
				meanAbsoluteRealMelDiff.add( (0..<numberOfBins).
					collect { diff[ it ].abs() * melBinWidth( it ) }.
					sum( (double)0 ) / totalWidthInMel
				);
				rootMeanSquareRealMelDiff.add( Math.sqrt( (0..<numberOfBins).
					collect { squareDiff[ it ] * melBinWidth( it ) }.
					sum( (double)0 ) / totalWidthInMel
				) );
			}
			else
			{
				// Sadly, the intensity is going to have some hiccups, since not
				// all audio frames are necessarily the same size.
				meanAbsoluteRealHertzDiff.add( (double)0 );
				rootMeanSquareRealHertzDiff.add( (double)0 );
				meanAbsoluteRealMelDiff.add( (double)0 );
				rootMeanSquareRealMelDiff.add( (double)0 );
			}
		}
		
		previousBins = bins;
	}
] as MediaToolAdapter );


long millisecondsBetweenCheckpoints = 5000;
long start = System.currentTimeMillis();
long lastCheckpointTime = start;
while ( reader.readPacket() == null )
{
	long now = System.currentTimeMillis();
	if ( millisecondsBetweenCheckpoints < now - lastCheckpointTime )
	{
		lastCheckpointTime = now;
		println "Processed ${sampleCountList.size()} audio frames in " +
			"${(now - start)/1000} seconds...";
	}
}
long time = System.currentTimeMillis() - start;


int numberOfFrames = rootMeanSquareGainList.size();


println ""
println "Done! There were ${numberOfFrames} audio frames, and they were" +
	" processed in"
println "${time}ms. Now for the calculated values:"
println ""


def blurList = { List list, int blurRadius ->
	
	int blurSize = blurRadius * 2 + 1;
	int listSize = list.size();
	
	if ( listSize <= blurSize )
		return [ list.sum( (double)0 ) / listSize ] * listSize;
	
	def blurredList = [];
	
	(0..<blurRadius).each {
		
		int blurStop = it + blurRadius + 1;
		
		blurredList.add(
			list[ 0..<blurStop ].sum( (double)0 ) / blurStop
		);
	};
	
	(blurSize..listSize).each {
		
		blurredList.add(
			list[ (it - blurSize)..<it ].sum( (double)0 ) / blurSize
		);
	};
	
	((listSize - blurRadius)..<listSize).each {
		
		int blurStart = it - blurRadius;
		
		blurredList.add(
			list[ blurStart..<listSize ].sum( (double)0 ) /
			(listSize - blurStart)
		);
	};
	
	assert blurredList.size() == listSize;
	
	return blurredList;
};

int numberOfDistances = numberOfFrames - 1;

/*
def blurredDistances = blurList( rootMeanSquareRealMelDiff, 10 );
def unblurredDistances = (0..<numberOfDistances).collect
	{ (rootMeanSquareRealMelDiff[ it ] - blurredDistances[ it ]).abs() };

// TODO: Figure out whether sort or reverse really modifies the collection. That
// just seems weird. However, removing the "[] +" here messes things up.
def bestUnblurredDistances = ([] + unblurredDistances).sort().reverse();

if ( numberOfFrames != 0 )
{
	bestUnblurredDistances.findAll { bestUnblurredDistances.head() / 2 <= it }.
		each { distance ->
		
		unblurredDistances.findIndexValues { it == distance }.each
			{ println "Frame $it has unblurred distance $distance."; };
	}
}
*/

println "sampleCountList: $sampleCountList"
println ""
println "meanAbsoluteGainList: $meanAbsoluteGainList"
println "rootMeanSquareGainList: $rootMeanSquareGainList"
println ""
print   "Enter anything to continue. (to see: power, stats012, diffs) > ";
read();
println ""
println "meanAbsoluteRealHertzPowerList: $meanAbsoluteRealHertzPowerList"
println "rootMeanSquareRealHertzPowerList: $rootMeanSquareRealHertzPowerList"
println "meanAbsoluteRealMelPowerList: $meanAbsoluteRealMelPowerList"
println "rootMeanSquareRealMelPowerList: $rootMeanSquareRealMelPowerList"
println ""
print   "Enter anything to continue. (to see: stats012, diffs) > "; read();
println ""
println "expectedRealHertzFrequencyList: $expectedRealHertzFrequencyList"
println "varianceRealHertzFrequencyList: $varianceRealHertzFrequencyList"
println "expectedRealMelFrequencyList: $expectedRealMelFrequencyList"
println "varianceRealMelFrequencyList: $varianceRealMelFrequencyList"
println ""
println ""
print   "Enter anything to continue. (to see: stats12, diffs) > "; read();
println ""
println "expectedRealHertzAbsolutesList: $expectedRealHertzAbsolutesList"
println "varianceRealHertzAbsolutesList: $varianceRealHertzAbsolutesList"
println "expectedRealMelAbsolutesList: $expectedRealMelAbsolutesList"
println "varianceRealMelAbsolutesList: $varianceRealMelAbsolutesList"
println ""
println ""
print   "Enter anything to continue. (to see: stats2, diffs) > "; read();
println ""
println "expectedRealHertzSquaresList: $expectedRealHertzSquaresList"
println "varianceRealHertzSquaresList: $varianceRealHertzSquaresList"
println "expectedRealMelSquaresList: $expectedRealMelSquaresList"
println "varianceRealMelSquaresList: $varianceRealMelSquaresList"
println ""
print   "Enter anything to continue. (to see: diffs) > "; read();
println ""
println "meanAbsoluteRealHertzDiff: $meanAbsoluteRealHertzDiff"
println "rootMeanSquareRealHertzDiff: $rootMeanSquareRealHertzDiff"
println "meanAbsoluteRealMelDiff: $meanAbsoluteRealMelDiff"
println "rootMeanSquareRealMelDiff: $rootMeanSquareRealMelDiff"
println ""
println "Done!"
