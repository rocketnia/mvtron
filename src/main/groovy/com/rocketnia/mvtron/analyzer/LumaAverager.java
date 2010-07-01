// LumaAverager.java
//
// Copyright 2009, 2010 Ross Angle

package com.rocketnia.mvtron.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LumaAverager implements IIntArrayReferenceListener
{
	private List< IDoubleListener > doubleListeners =
		Collections.synchronizedList(
			new ArrayList< IDoubleListener >() );
	
	private static final double maxAverageLuma = 0xFF * 3;
	
	public LumaAverager() {}
	
	public static RgbArrayListenerTool makeTool()
		{ return new RgbArrayListenerTool( new LumaAverager() ); }
	
	public RgbArrayListenerTool newTool()
		{ return new RgbArrayListenerTool( this ); }
	
	@Override
	public void onIntArrayReference( int[] rgb )
		{ propagateDouble( calculate( rgb ) ); }
	
	public static double calculate( int[] rgb )
	{
		int thisAverageLuma = 0;
		
		for ( int pixel: rgb )
		{
			// The pixels are in ARGB format.
			
//			assert 0xFF == (0xFF & pixel >>> 24);  // alpha
			
			thisAverageLuma +=
				(0xFF & pixel >>> 16) +  // R
				(0xFF & pixel >>>  8) +  // G
				(0xFF & pixel       );   // B
		}
		
		return thisAverageLuma / maxAverageLuma / rgb.length;
	}
	
	public void addListener( IDoubleListener listener )
		{ doubleListeners.add( listener ); }
	
	protected void propagateDouble( double d )
	{
		for ( IDoubleListener listener: doubleListeners )
			listener.onDouble( d );
	}
}
