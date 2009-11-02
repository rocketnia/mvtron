package com.rocketnia.mvtron.analyzer

def read = System.in.newReader().&readLine;

println "This sandbox will loop through a video, printing the average luma of"
println "each frame processed, scaled to the range 0.0..1.0. Then, it will"
println "print the number of frames and how long the loop took."
println ""
print   "What file should be used? "; def filename = read();
println ""

import java.awt.image.BufferedImage
import com.xuggle.mediatool.event.IVideoPictureEvent
import com.xuggle.mediatool.event.VideoPictureEvent
import com.xuggle.mediatool.MediaToolAdapter
import com.xuggle.mediatool.ToolFactory
import com.xuggle.xuggler.video.IConverter


// TODO: Figure out whether these will be needed.
/*
class ClosureImageListener extends MediaToolAdapter
{
	private Closure closure;
	
	public ClosureImageListener( Closure closure )
		{ this.closure = closure; }
	
	public void onVideoPicture( IVideoPictureEvent event )
	{
		def result = closure( event.image ?: event.picture );
		
		// Let this event fall through to any listeners.
		super.onVideoPicture( event );
	}
}

class ConverterSwapTool extends MediaToolAdapter
{
	private IConverter converter;
	
	public ConverterSwapTool( IConverter converter )
		{ this.converter = converter; }
	
	public ConverterSwapTool make( IConverter converter )
		{ new ConverterSwapTool( converter ) }
	
	public void onVideoPicture( IVideoPictureEvent event )
	{
		def oldImage = event.image;
		def oldPicture = event.picture;
		
		def timeStamp = event.timeStamp;
		
		def newPicture = oldImage == null ? null :
			converter.toPicture( oldImage, timeStamp );
		
		def newImage = oldPicture == null ? null :
			converter.toImage( oldPicture );
		
		def newEvent = new VideoPictureEvent(
			this,
			newPicture,
			newImage,
			timeStamp,
			event.timeUnit,
			event.streamIndex
		);
		
		listeners.each { it.onVideoPicture( newEvent ) };
	}
}

def il = { new ClosureImageListener( it ) };
*/

def reader = ToolFactory.makeReader( filename );

reader.setBufferedImageTypeToGenerate( BufferedImage.TYPE_3BYTE_BGR );

def lumaAverager = new LumaAverager();
reader.addListener( lumaAverager.newTool() );

def frameCounter = new FrameCounter();
reader.addListener( frameCounter.newTool() );

lumaAverager.addListener( { println it; } as IFloatListener );


long start = System.currentTimeMillis();
while ( reader.readPacket() == null ) {}
long time = System.currentTimeMillis() - start;

println ""
println "Done! There were ${frameCounter.data} frames, and they were" +
	" processed in ${time}ms."