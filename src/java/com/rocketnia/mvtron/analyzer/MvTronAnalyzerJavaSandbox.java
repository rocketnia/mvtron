package com.rocketnia.mvtron.analyzer;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Scanner;
import java.util.TreeSet;


import com.rocketnia.mvtron.analyzer.scenedetectors.EuclideanSceneDetector;
import com.rocketnia.mvtron.analyzer.scenedetectors.TaxicabSceneDetector;
import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.ToolFactory;

public class MvTronAnalyzerJavaSandbox
{
	public static void main2( String[] args )
	{
		Scanner in = new Scanner( System.in );
		
		prn( "This sandbox will loop through a video, printing the distances" );
		prn( "found between each frame and the frames one and two behind it," );
		prn( "scaled to the range 0.0..1.0. Then, it will print the number" );
		prn( "of frames and how long the loop took. Finally, it will print" );
		prn( "the top ten \"unblurred\" distances (calculated from the" );
		prn( "single and double distances), which should be good places to" );
		prn( "look for scene breaks." );
		prn();
		pr( "What file should be used? " );
		String filename = in.nextLine();
		prn();
		
		IMediaReader reader = ToolFactory.makeReader( filename );
		
		reader.setBufferedImageTypeToGenerate( BufferedImage.TYPE_3BYTE_BGR );
		
		
		SceneDetectorTool singleDetector = TaxicabSceneDetector.makeTool();
		IntArrayTimeWindower singleWindower = singleDetector.newWindower( 1 );
		reader.addListener( singleWindower.newTool() );
		final List< Double > singleDistances = new ArrayList< Double >();
		singleDistances.add( (double)0 );
		singleDetector.addListener( new IDoubleListener() {
			
			public void onDouble( double d )
			{
				singleDistances.add( d );
				prn( "single distance: " + d );
			}
		} );
		
		SceneDetectorTool doubleDetector = TaxicabSceneDetector.makeTool();
		IntArrayTimeWindower doubleWindower = doubleDetector.newWindower( 2 );
		reader.addListener( doubleWindower.newTool() );
		final List< Double > doubleDistances = new ArrayList< Double >();
		doubleDistances.add( (double)0 );
		doubleDistances.add( (double)0 );
		doubleDetector.addListener( new IDoubleListener() {
			
			public void onDouble( double d )
			{
				doubleDistances.add( d );
				prn( "double distance: " + d );
			}
		} );
		
		
		long start = System.currentTimeMillis();
		while ( reader.readPacket() == null ) {}
		// The windowers made above would be closed now, but that doesn't
		// actually do anything in this case.
		long time = System.currentTimeMillis() - start;
		
		
		int numberOfFrames = singleDistances.size();
		assert numberOfFrames == doubleDistances.size();
		
		singleDistances.add( (double)0 );
		doubleDistances.add( singleDistances.get( numberOfFrames - 1 ) );
		
		
		prn();
		prn( "Done! There were " + numberOfFrames +
			" frames, and they were processed in" );
		prn( time + "ms. Now for the scene detection:" );
		prn();
		
		
		final List< Double > unblurredDistances = new ArrayList< Double >();
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
			new TreeSet< Double >( unblurredDistances ).descendingSet();
		
		int i = 0;
		for ( double distance: topUnblurredDistances )
		{
			if ( 10 <= i )
				break;
			
			// TODO: There's a chance two frames will have exactly the same
			// distance, but this is good enough for a demo. If this turns into
			// something more than a demo, fix it.
			int frame = unblurredDistances.indexOf( distance );
			
			prn(
				"Frame " + frame + " has unblurred distance " + distance + "."
			);
			
			i++;
		}
		
		prn();
		prn( "Done!" );
	}
	
	public static void main( String[] args )
	{
		Scanner in = new Scanner( System.in );
		
		prn( "This sandbox will loop through a video, printing the distances" );
		prn( "found between each frame and the frame behind it, scaled to" );
		prn( "the range 0.0..1.0. Then, it will print the number of frames" );
		prn( "and how long the loop took. Finally, it will print the top" );
		prn( "\"unblurred\" distances greater than half the maximum" );
		prn( "unblurred distance, which should be good places to look for" );
		prn( "scene breaks." );
		prn();
		pr( "What file should be used? " );
		String filename = in.nextLine();
		prn();
		
		IMediaReader reader = ToolFactory.makeReader( filename );
		
		reader.setBufferedImageTypeToGenerate( BufferedImage.TYPE_3BYTE_BGR );
		
		
		SceneDetectorTool detector = EuclideanSceneDetector.makeTool();
		IntArrayTimeWindower windower = detector.newWindower( 1 );
		reader.addListener( windower.newTool() );
		final List< Double > distances = new ArrayList< Double >();
		distances.add( (double)0 );
		detector.addListener( new IDoubleListener() {
			
			public void onDouble( double d )
			{
				distances.add( d );
				prn( "distance: " + d );
			}
		} );
		
		
		long start = System.currentTimeMillis();
		while ( reader.readPacket() == null ) {}
		// The windower made above would be closed now, but that doesn't
		// actually do anything in this case.
		long time = System.currentTimeMillis() - start;
		
		
		int numberOfFrames = distances.size();
		
		distances.add( (double)0 );
		
		
		prn();
		prn( "Done! There were " + numberOfFrames +
			" frames, and they were processed in" );
		prn( time + "ms. Now for the scene detection:" );
		prn();
		
		
		final List< Double > unblurredDistances = new ArrayList< Double >();
		if ( numberOfFrames != 0 )
			unblurredDistances.add( (double)0 );
		for ( int i = 1; i < numberOfFrames; i++ )
		{
			double thisDistance = distances.get( i );
			
			unblurredDistances.add( or(
				mockBool( thisDistance ),
				Math.min(
					mockBool( thisDistance ),
					Math.max( distances.get( i - 1 ), distances.get( i + 1 ) )
				)
			) );
		}
		
		
		TreeSet< Double > worstUnblurredDistances =
			new TreeSet< Double >( unblurredDistances );
		
		if ( numberOfFrames != 0 )
		{
			for ( double distance: worstUnblurredDistances.
					tailSet( worstUnblurredDistances.last() / 2, true ).
					descendingSet() )
			{
				// TODO: There's a chance two frames will have exactly the same
				// distance, but this is good enough for a demo. If this turns
				// into something more than a demo, fix it.
				int frame = unblurredDistances.indexOf( distance );
				
				prn( "Frame " + frame + " has unblurred distance " + distance +
					"." );
			}
		}
		
		prn();
		prn( "Done!" );
	}
	
	private static void prn() { System.out.println(); }
	private static void prn( Object s ) { System.out.println( s.toString() ); }
	private static void pr ( Object s ) { System.out.print  ( s.toString() ); }
	
	private static double mockBool( double a )
		{ return a; }  // the best?
//		{ return or( a, a ); }  // extremely good, but still not worth computing
//		{ return Math.sqrt( a ); }  // good, but not as good as { return a; }
//		{ return Math.sqrt( or( a, a ) ); }  // not very good at all
//		{ return and( a, a ); }  // extremely poor; incorrect on faded breaks
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