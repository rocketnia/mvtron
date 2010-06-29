package com.rocketnia.mvtron.analyzer;

// The array received by one of these listeners may be reused by the dispatcher
// for other purposes and may have been originally used for something completely
// different. Any listener that needs to store the contents of this array should
// defensively copy them itself rather than relying on the reference.
// Modification of the original array by a listener may result in undefined
// behavior including the modified data arriving at unknown other listeners
// observing the same event.
public interface IIntArrayReferenceListener
{
	public void onIntArrayReference( int[] array );
}