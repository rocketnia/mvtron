// RgbArrayListenerTool.java
//
// Copyright 2009, 2010, 2021 Ross Angle

package com.rocketnia.mvtron.analyzer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.freedesktop.gstreamer.Buffer;
import org.freedesktop.gstreamer.Caps;
import org.freedesktop.gstreamer.Pad;
import org.freedesktop.gstreamer.PadProbeInfo;
import org.freedesktop.gstreamer.PadProbeReturn;
import org.freedesktop.gstreamer.Structure;

// This class will pass along any events it receives to an
// IRgbArrayListener object. It uses the same int array over and over
// for efficency's sake, but interfacing code should not rely on that
// behavior.
//
// TODO: See if there's a way we can detect whether the pad is giving
// us data in RGB format. For now, we just assume it is.
//
public class RgbArrayListenerTool implements Pad.PROBE
{
	private IIntArrayReferenceListener delegate;
	private int[] arrayBuffer = null;
	
	public RgbArrayListenerTool( IIntArrayReferenceListener delegate )
		{ this.delegate = delegate; }
	
	// @Override
	public PadProbeReturn probeCallback( Pad pad, PadProbeInfo info ) {
		Caps caps = pad.getCurrentCaps();
		if ( caps == null ) {
			throw new NullPointerException();
		}
		Structure capsStructure = caps.getStructure( 0 );
		int width = capsStructure.getInteger( "width" );
		int height = capsStructure.getInteger( "height" );
		int arrayBufferSize = width * height;
		if ( arrayBuffer == null ) {
			arrayBuffer = new int[ arrayBufferSize ];
		} else if ( arrayBuffer.length != arrayBufferSize ) {
			throw new RuntimeException();
		}
		
		Buffer gstBuffer = info.getBuffer();
		boolean isWritable = false;
		ByteBuffer nioByteBuffer = gstBuffer.map( isWritable );
		try {
			IntBuffer nioIntBuffer = nioByteBuffer.asIntBuffer();
			if ( nioIntBuffer.remaining() != arrayBufferSize ) {
				throw new RuntimeException();
			}
			nioIntBuffer.get( arrayBuffer );
		} finally {
			gstBuffer.unmap();
		}
		
		delegate.onIntArrayReference( arrayBuffer );
		
		return PadProbeReturn.OK;
	}
}
