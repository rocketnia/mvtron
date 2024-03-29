// MVTronAnalyzerJavaSandbox.java
//
// Copyright 2009, 2010, 2021 Ross Angle

package com.rocketnia.mvtron.analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Scanner;
import java.util.TreeSet;

import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pad;
import org.freedesktop.gstreamer.PadProbeReturn;
import org.freedesktop.gstreamer.PadProbeType;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.Version;
import org.freedesktop.gstreamer.elements.AppSink;

import com.rocketnia.mvtron.analyzer.scenedetectors.
	EuclideanSceneDetector;
import com.rocketnia.mvtron.analyzer.scenedetectors.
	TaxicabSceneDetector;

public class MvTronAnalyzerJavaSandbox
{
	public static void main2( String[] args )
	{
		Scanner in = new Scanner( System.in );
		
		prn( "This sandbox will loop through a video, printing the"
			+ " distances found" );
		prn( "between each frame and the frames one and two behind"
			+ " it, scaled to the" );
		prn( "range 0.0..1.0. Then, it will print the number of"
			+ " frames and how long" );
		prn( "the loop took. Finally, it will print the top ten"
			+ " \"unblurred\"" );
		prn( "distances (calculated from the single and double"
			+ " distances), which" );
		prn( "should be good places to look for scene breaks." );
		prn();
		pr( "What file should be used? " );
		String filename = in.nextLine();
		prn();
		
		Gst.init( Version.BASELINE, "MVTronAnalyzerJavaSandbox.main2()" );
		
		String pipelineSpec =
			"filesrc name=src ! decodebin ! videoconvert "
			+ "! video/x-raw, format=xRGB ! appsink name=dst";
		Pipeline pipe = (Pipeline) Gst.parseLaunch( pipelineSpec );
		Element src = pipe.getElementByName( "src" );
		AppSink dstAppSink = (AppSink) pipe.getElementByName( "dst" );
		dstAppSink.set( "sync", false );
		Pad dst = dstAppSink.getStaticPad( "sink" );
		
		src.set( "location", filename );
		
		
		SceneDetectorTool singleDetector =
			TaxicabSceneDetector.makeTool();
		IntArrayTimeWindower singleWindower =
			singleDetector.newWindower( 1 );
		dst.addProbe( PadProbeType.BUFFER,
			new RgbArrayListenerTool( singleWindower ) );
		final List< Double > singleDistances =
			new ArrayList< Double >();
		singleDistances.add( (double)0 );
		singleDetector.addListener( new IDoubleListener() {
			
			public void onDouble( double d )
			{
				singleDistances.add( d );
				prn( "single distance: " + d );
			}
		} );
		
		SceneDetectorTool doubleDetector =
			TaxicabSceneDetector.makeTool();
		IntArrayTimeWindower doubleWindower =
			doubleDetector.newWindower( 2 );
		dst.addProbe( PadProbeType.BUFFER,
			new RgbArrayListenerTool( doubleWindower ) );
		final List< Double > doubleDistances =
			new ArrayList< Double >();
		doubleDistances.add( (double)0 );
		doubleDistances.add( (double)0 );
		doubleDetector.addListener( new IDoubleListener() {
			
			public void onDouble( double d )
			{
				doubleDistances.add( d );
				prn( "double distance: " + d );
			}
		} );
		
		
		// Since we added at least one probe, we add another to be
		// processed after the others, which formally declares that
		// the probes are done with this buffer.
		dst.addProbe( PadProbeType.BUFFER,
			(Pad.PROBE) (( pad, info ) -> {
				return PadProbeReturn.HANDLED;
			}) );
		
		pipe.getBus()
			.connect( (Bus.ERROR) (( source, code, message ) -> {
				System.err.println( message );
				Gst.quit();
			}) );
		pipe.getBus().connect( (Bus.EOS) (( source ) -> {
			Gst.quit();
		}) );
		
		long start = System.currentTimeMillis();
		pipe.play();
		Gst.main();
		// The windowers made above would be closed now, but that
		// doesn't actually do anything in this case.
		long time = System.currentTimeMillis() - start;
		
		
		int numberOfFrames = singleDistances.size();
		assert numberOfFrames == doubleDistances.size();
		
		singleDistances.add( (double)0 );
		doubleDistances.add(
			singleDistances.get( numberOfFrames - 1 ) );
		
		
		prn();
		prn( "Done! There were " + numberOfFrames +
			" frames, and they were processed in " + time + "ms." );
		prn( "Now for the scene detection:" );
		prn();
		
		
		final List< Double > unblurredDistances =
			new ArrayList< Double >();
		if ( numberOfFrames != 0 )
			unblurredDistances.add( (double)0 );
		for ( int i = 1; i < numberOfFrames; i++ )
		{
			unblurredDistances.add( or(
				mockBool( singleDistances.get( i ) ),
				and(
					mockBool( doubleDistances.get( i ) ),
					not( mockBool( singleDistances.get( i - 1 ) ) )
				),
				and(
					mockBool( doubleDistances.get( i + 1 ) ),
					not( mockBool( singleDistances.get( i + 1 ) ) )
				)
			) );
		}
		
		
		NavigableSet< Double > topUnblurredDistances =
			new TreeSet< Double >( unblurredDistances ).
				descendingSet();
		
		int i = 0;
		for ( double distance: topUnblurredDistances )
		{
			if ( 10 <= i )
				break;
			
			// TODO: There's a chance two frames will have exactly the
			// same distance, but this is good enough for a demo. If
			// this turns into something more than a demo, fix it.
			int frame = unblurredDistances.indexOf( distance );
			
			prn( "Frame " + frame + " has unblurred distance "
				+ distance + "." );
			
			i++;
		}
		
		prn();
		prn( "Done!" );
		
		// Close the file opened by GStreamer.
		//
		// TODO: See if there's a better way to do this.
		//
		System.exit( 0 );
	}
	
	public static void main( String[] args )
	{
		Scanner in = new Scanner( System.in );
		
		prn( System.getenv( "PATH" ) );
		prn( "This sandbox will loop through a video, printing the"
			+ " distances found" );
		prn( "between each frame and the frame behind it, scaled to"
			+ " the range" );
		prn( "0.0..1.0. Then, it will print the number of frames and"
			+ " how long the" );
		prn( "loop took. Finally, it will print the top \"unblurred\""
			+ " distances" );
		prn( "greater than half the maximum unblurred distance, which"
			+ " should be good" );
		prn( "places to look for scene breaks." );
		prn();
		pr( "What file should be used? " );
		String filename = in.nextLine();
		prn();
		
		Gst.init( Version.BASELINE, "MVTronAnalyzerJavaSandbox.main()" );
		
		String pipelineSpec =
			"filesrc name=src ! decodebin ! videoconvert "
			+ "! video/x-raw, format=xRGB ! appsink name=dst";
		Pipeline pipe = (Pipeline) Gst.parseLaunch( pipelineSpec );
		Element src = pipe.getElementByName( "src" );
		AppSink dstAppSink = (AppSink) pipe.getElementByName( "dst" );
		dstAppSink.set( "sync", false );
		Pad dst = dstAppSink.getStaticPad( "sink" );
		
		src.set( "location", filename );
		
		
		SceneDetectorTool detector =
			EuclideanSceneDetector.makeTool();
		IntArrayTimeWindower windower = detector.newWindower( 1 );
		dst.addProbe( PadProbeType.BUFFER,
			new RgbArrayListenerTool( windower ) );
		final List< Double > distances = new ArrayList< Double >();
		distances.add( (double)0 );
		detector.addListener( new IDoubleListener() {
			
			public void onDouble( double d )
			{
				distances.add( d );
				prn( "distance: " + d );
			}
		} );
		
		
		// Since we added at least one probe, we add another to be
		// processed after the others, which formally declares that
		// the probes are done with this buffer.
		dst.addProbe( PadProbeType.BUFFER,
			(Pad.PROBE) (( pad, info ) -> {
				return PadProbeReturn.HANDLED;
			}) );
		
		pipe.getBus()
			.connect( (Bus.ERROR) (( source, code, message ) -> {
				System.err.println( message );
				Gst.quit();
			}) );
		pipe.getBus().connect( (Bus.EOS) (( source ) -> {
			Gst.quit();
		}) );
		
		long start = System.currentTimeMillis();
		pipe.play();
		Gst.main();
		// The windower made above would be closed now, but that
		// doesn't actually do anything in this case.
		long time = System.currentTimeMillis() - start;
		
		
		int numberOfFrames = distances.size();
		
		distances.add( (double)0 );
		
		
		prn();
		prn( "Done! There were " + numberOfFrames +
			" frames, and they were processed in " + time + "ms." );
		prn( "Now for the scene detection:" );
		prn();
		
		
		final List< Double > unblurredDistances =
			new ArrayList< Double >();
		if ( numberOfFrames != 0 )
			unblurredDistances.add( (double)0 );
		for ( int i = 1; i < numberOfFrames; i++ )
		{
			double thisDistance = distances.get( i );
			
			unblurredDistances.add( or(
				mockBool( thisDistance ),
				Math.min(
					mockBool( thisDistance ),
					Math.max(
						distances.get( i - 1 ),
						distances.get( i + 1 )
					)
				)
			) );
		}
		
		
		TreeSet< Double > worstUnblurredDistances =
			new TreeSet< Double >( unblurredDistances );
		
		if ( numberOfFrames != 0 )
		{
			for ( double distance: worstUnblurredDistances.
					tailSet(
						worstUnblurredDistances.last() / 2, true ).
					descendingSet() )
			{
				// TODO: There's a chance two frames will have exactly
				// the same distance, but this is good enough for a
				// demo. If this turns into something more than a
				// demo, fix it.
				int frame = unblurredDistances.indexOf( distance );
				
				prn( "Frame " + frame + " has unblurred distance "
					+ distance + "." );
			}
		}
		
		prn();
		prn( "Done!" );
		
		// Close the file opened by GStreamer.
		//
		// TODO: See if there's a better way to do this.
		//
		System.exit( 0 );
	}
	
	private static void prn() { System.out.println(); }
	private static void prn( Object s ) { System.out.println( s ); }
	private static void pr ( Object s ) { System.out.print( s ); }
	
	private static double mockBool( double a )
		{ return a; }  // the best?
//		{ return or( a, a ); }
//			// extremely good, but still not worth computing
//		{ return Math.sqrt( a ); }
//			// good, but not as good as { return a; }
//		{ return Math.sqrt( or( a, a ) ); }  // not very good at all
//		{ return and( a, a ); }
//			// extremely poor; incorrect on faded breaks
/*	{
		final double power = 3;
		
		double normalized = a * 2 - 1;
		
		if ( normalized < 0 )
			return (-Math.pow( -normalized, power ) + 1) / 2;
		
		return (Math.pow( normalized, power ) + 1) / 2;
	}*/  // not very good with any power
	
	private static double not( double a )
		{ return 1 - a; }
	
	private static double and( double a, double b )
		{ return a * b; }
	
	private static double and( double a, double b, double c )
		{ return a * b * c; }
	
	private static double or( double a, double b )
		{ return not( and( not( a ), not( b ) ) ); }
	
	private static double or( double a, double b, double c )
		{ return not( and( not( a ), not( b ), not( c ) ) ); }
}
