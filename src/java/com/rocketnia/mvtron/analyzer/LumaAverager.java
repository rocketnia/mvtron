package com.rocketnia.mvtron.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LumaAverager implements IIntArrayReferenceListener
{
	private List< IFloatListener > floatListeners =
		Collections.synchronizedList( new ArrayList< IFloatListener >() );
	
	private static final float maxAverageLuma = 0xFF * 3;
	
	public LumaAverager() {}
	
	public static RgbArrayListenerTool makeTool()
		{ return new RgbArrayListenerTool( new LumaAverager() ); }
	
	public RgbArrayListenerTool newTool()
		{ return new RgbArrayListenerTool( this ); }
	
	@Override
	public void onIntArrayReference( int[] rgb )
		{ propagateFloat( calculate( rgb ) ); }
	
	public static float calculate( int[] rgb )
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
	
	public void addListener( IFloatListener listener )
		{ floatListeners.add( listener ); }
	
	protected void propagateFloat( float f )
	{
		for ( IFloatListener listener: floatListeners )
			listener.onFloat( f );
	}
}