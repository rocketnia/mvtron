package com.rocketnia.mvtron.analyzer;

public interface IRgbArrayListener
{
	// For performance reasons, the array received here isn't guaranteed to be
	// an new array each time. Any listener that needs to store the contents
	// of this array should defensively copy it inside its own onRgb method.
	public void onRgb( int[] rgb );
}