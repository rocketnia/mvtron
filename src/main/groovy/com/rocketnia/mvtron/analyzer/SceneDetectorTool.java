// SceneDetectorTool.java
//
// Copyright 2009, 2010 Ross Angle

package com.rocketnia.mvtron.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SceneDetectorTool implements IIntArrayWindowListener
{
	private List< IDoubleListener > doubleListeners =
		Collections.synchronizedList(
			new ArrayList< IDoubleListener >() );
	
	private ISceneDetector detector;
	
	public SceneDetectorTool( ISceneDetector detector )
	{
		if ( detector == null )
			throw new NullPointerException();
		
		this.detector = detector;
	}
	
	public IntArrayTimeWindower newWindower( int spacing )
	{
		if ( spacing < 0 )
			throw new IllegalArgumentException();
		
		IntArrayTimeWindower windower =
			new IntArrayTimeWindower( spacing + 1 );
		
		windower.addListener( this );
		
		return windower;
	}
	
	@Override public void onEarlyWindow( List< int[] > window ) {}
	
	@Override
	public void onMiddleWindow( List< int[] > window )
	{
		propagateDouble( detector.measureDistance(
			window.get( 0 ),
			window.get( window.size() - 1 )
		) );
	}
	
	@Override public void onLateWindow( List< int[] > window ) {}
	
	// Whoops, these are calculations that are better off done *after*
	// the video is scrubbed.
	/*
	public static boolean calculateEarly( List< int[] > window )
	{
		if ( window.size() != 3 )
			throw new IllegalArgumentException();
		
		int[] p = (int[])window.get( 0 );
		int[] n = (int[])window.get( 1 );
		int[] nn = (int[])window.get( 2 );
		
		return (
			sceneDetect( p, n )
			|| (sceneDetect( p, nn ) && !sceneDetect( n, nn ))
		);
	}
	
	public static boolean calculateMiddle( List< int[] > window )
	{
		if ( window.size() != 4 )
			throw new IllegalArgumentException();
		
		int[] pp = (int[])window.get( 0 );
		int[] p = (int[])window.get( 1 );
		int[] n = (int[])window.get( 2 );
		int[] nn = (int[])window.get( 3 );
		
		return (
			sceneDetect( p, n )
			|| (sceneDetect( pp, n ) && !sceneDetect( pp, p ))
			|| (sceneDetect( p, nn ) && !sceneDetect( n, nn ))
		);
	}
	
	public static boolean calculateLate( List< int[] > window )
	{
		if ( window.size() != 3 )
			throw new IllegalArgumentException();
		
		int[] pp = (int[])window.get( 0 );
		int[] p = (int[])window.get( 1 );
		int[] n = (int[])window.get( 2 );
		
		return (
			sceneDetect( p, n )
			|| (sceneDetect( pp, n ) && !sceneDetect( pp, p ))
		);
	}
	*/
	
	public void addListener( IDoubleListener listener )
		{ doubleListeners.add( listener ); }
	
	protected void propagateDouble( double d )
	{
		for ( IDoubleListener listener: doubleListeners )
			listener.onDouble( d );
	}
}
