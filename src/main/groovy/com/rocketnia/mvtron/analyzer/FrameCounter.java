// FrameCounter.java
//
// Copyright 2009, 2010 Ross Angle

package com.rocketnia.mvtron.analyzer;

public class FrameCounter implements IIntArrayReferenceListener
{
	private int count = 0;
	
	public FrameCounter() {}
	
	public static RgbArrayListenerTool makeTool()
		{ return new RgbArrayListenerTool( new FrameCounter() ); }
	
	public RgbArrayListenerTool newTool()
		{ return new RgbArrayListenerTool( this ); }
	
	@Override
	public void onIntArrayReference( int[] rgb )
	{
		count++;
	}
	
	public int getData() { return count; }
}
