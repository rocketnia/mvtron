// RgbArrayListenerTool.java
//
// Copyright 2009, 2010 Ross Angle

package com.rocketnia.mvtron.analyzer;

import java.awt.image.BufferedImage;

import com.xuggle.mediatool.event.IAddStreamEvent;
import com.xuggle.mediatool.event.IAudioSamplesEvent;
import com.xuggle.mediatool.event.ICloseCoderEvent;
import com.xuggle.mediatool.event.ICloseEvent;
import com.xuggle.mediatool.event.IFlushEvent;
import com.xuggle.mediatool.event.IOpenCoderEvent;
import com.xuggle.mediatool.event.IOpenEvent;
import com.xuggle.mediatool.event.IReadPacketEvent;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.mediatool.event.IWriteHeaderEvent;
import com.xuggle.mediatool.event.IWritePacketEvent;
import com.xuggle.mediatool.event.IWriteTrailerEvent;
import com.xuggle.mediatool.IMediaListener;

// This class will pass along any BufferedImage events it receives to
// an IRgbArrayListener object. It uses the same int array over and
// over for efficency's sake, but interfacing code should not rely on
// that behavior.
public class RgbArrayListenerTool implements IMediaListener
{
	private IIntArrayReferenceListener delegate;
	private int[] rgb = null;
	private int minX = 0;
	private int minY = 0;
	private int width = 0;
	private int height = 0;
	
	public RgbArrayListenerTool( IIntArrayReferenceListener delegate )
		{ this.delegate = delegate; }
	
	// NOTE: All of these would have @Override if the Groovy-Eclipse
	// plugin could understand that they actually overrode things.
	public void onAddStream( IAddStreamEvent event ) {}
	public void onAudioSamples( IAudioSamplesEvent event ) {}
	public void onClose( ICloseEvent event ) {}
	public void onCloseCoder( ICloseCoderEvent event ) {}
	public void onFlush( IFlushEvent event ) {}
	public void onOpen( IOpenEvent event ) {}
	public void onOpenCoder( IOpenCoderEvent event ) {}
	public void onReadPacket( IReadPacketEvent event ) {}
	
	// @Override
	public void onVideoPicture( IVideoPictureEvent event )
	{
		BufferedImage image = event.getImage();
		
		if ( image == null ) return;
		
		if ( rgb == null )
		{
			minX = image.getMinX();
			minY = image.getMinY();
			width = image.getWidth();
			height = image.getHeight();
		}
		
		rgb =
			image.getRGB( minX, minY, width, height, rgb, 0, width );
		
		delegate.onIntArrayReference( rgb );
	}
	
	// NOTE: All of these would have @Override if the Groovy-Eclipse
	// plugin could understand that they actually overrode things.
	public void onWriteHeader( IWriteHeaderEvent event ) {}
	public void onWritePacket( IWritePacketEvent event ) {}
	public void onWriteTrailer( IWriteTrailerEvent event ) {}
}
