// FrameCounter.java
//
// Copyright 2009, 2010, 2021 Ross Angle

package com.rocketnia.mvtron.analyzer;

public class FrameCounter implements IIntArrayReferenceListener
{
	private int count = 0;
	
	public FrameCounter() {}
	
	// @Override
	public void onIntArrayReference( int[] rgb )
	{
		count++;
	}
	
	public int getData() { return count; }
}
