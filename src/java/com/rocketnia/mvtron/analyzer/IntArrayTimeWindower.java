package com.rocketnia.mvtron.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class IntArrayTimeWindower implements IIntArrayReferenceListener
{
	protected boolean fullWindow;
	protected LinkedList< int[] > frames;
	protected int numberOfFrames;
	
	private List< IIntArrayWindowListener > windowListeners =
		Collections.synchronizedList(
			new ArrayList< IIntArrayWindowListener >()
		);
	
	public IntArrayTimeWindower( int numberOfFrames )
	{
		frames = new LinkedList< int[] >();
		this.numberOfFrames = numberOfFrames;
		fullWindow = (numberOfFrames == 0);
	}
	
	public RgbArrayListenerTool newTool()
		{ return new RgbArrayListenerTool( this ); }
	
	@Override
	public void onIntArrayReference( int[] array )
	{
		if ( fullWindow )
		{
			int[] storedArray = frames.removeFirst();
			System.arraycopy( array, 0, storedArray, 0, array.length );
			frames.addLast( storedArray );
			
			propagateMiddleWindow( frames );
		}
		else
		{
			frames.addLast( array.clone() );
			
			if ( frames.size() == numberOfFrames )
			{
				fullWindow = true;
				propagateMiddleWindow( frames );
			}
			else
			{
				propagateEarlyWindow( frames );
			}
		}
	}
	
	public void finish()
	{
		try
		{
			while ( true )
			{
				frames.removeFirst();
				
				if ( frames.isEmpty() )
					break;
				
				propagateLateWindow( frames );
			}
		}
		finally
		{
			fullWindow = (numberOfFrames == frames.size());
		}
	}
	
	public void addListener( IIntArrayWindowListener listener )
		{ windowListeners.add( listener ); }
	
	protected void propagateEarlyWindow( List< int[] > window )
	{
		for ( IIntArrayWindowListener listener: windowListeners )
			listener.onEarlyWindow( window );
	}
	
	protected void propagateMiddleWindow( List< int[] > window )
	{
		for ( IIntArrayWindowListener listener: windowListeners )
			listener.onMiddleWindow( window );
	}
	
	protected void propagateLateWindow( List< int[] > window )
	{
		for ( IIntArrayWindowListener listener: windowListeners )
			listener.onLateWindow( window );
	}
}