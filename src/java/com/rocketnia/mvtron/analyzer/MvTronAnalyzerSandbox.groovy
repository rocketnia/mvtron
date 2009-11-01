package com.rocketnia.mvtron.analyzer

def read = System.in.newReader().&readLine;

println "This sandbox will loop through a video, printing the class of each"
println "object processed. Then, it will print the number of frames and how"
println "long the loop took."
println ""
print   "What file should be used? "; def filename = read();
println ""

import java.awt.image.BufferedImage
import com.xuggle.mediatool.event.IVideoPictureEvent
import com.xuggle.mediatool.event.VideoPictureEvent
import com.xuggle.mediatool.MediaToolAdapter
import com.xuggle.mediatool.ToolFactory
import com.xuggle.xuggler.video.IConverter

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

// TODO: Figure out whether this will be needed.
/*
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
*/

def il = { new ClosureImageListener( it ) };

// image byte array listener
// This works by modifying a single byte array over and over, so make sure not
// to let the parameter escape the closure without keeping that in mind.
def bl = { closure ->
	
	int[] bytes = null;
	
	return il {
		
		int width = it.width;
		
		bytes =
			it.getRGB( it.minX, it.minY, width, it.height, bytes, 0, width );
		
		return closure( bytes );
	};
};

def reader = ToolFactory.makeReader( filename );

reader.setBufferedImageTypeToGenerate( BufferedImage.TYPE_3BYTE_BGR );

int numberOfFrames = 0;

reader.addListener( bl { println( it.class ); numberOfFrames++; } );


long start = System.currentTimeMillis();
while ( reader.readPacket() == null ) {}
long time = System.currentTimeMillis() - start;

println();
println "Done! There were " + numberOfFrames +
	" frames, and they were processed in " + time + "ms.";