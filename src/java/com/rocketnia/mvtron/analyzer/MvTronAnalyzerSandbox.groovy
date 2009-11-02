package com.rocketnia.mvtron.analyzer


def read = System.in.newReader().&readLine;

println "This sandbox will loop through a video, printing the distances found"
println "between each frame and the frame behind it, scaled to the range"
println "0.0..1.0. Then, it will print the number of frames and how long the"
println "loop took. Finally, it will print the top \"unblurred\" distances"
println "greater than half the maximum unblurred distance, which should be"
println "good places to look for scene breaks."
println ""
print   "What file should be used? "; def filename = read();
println ""

import java.awt.image.BufferedImage
import com.xuggle.mediatool.event.IVideoPictureEvent
import com.xuggle.mediatool.event.VideoPictureEvent
import com.xuggle.mediatool.MediaToolAdapter
import com.xuggle.mediatool.ToolFactory
import com.xuggle.xuggler.video.IConverter
import com.rocketnia.mvtron.analyzer.scenedetectors.EuclideanSceneDetector


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


SceneDetectorTool detector = EuclideanSceneDetector.makeTool();
IntArrayTimeWindower windower = detector.newWindower( 1 );
reader.addListener( windower.newTool() );
final List< Double > distances = new ArrayList< Double >();
distances.add( (double)0 );
detector.addListener( {
	
	distances.add( it );
	println "distance: $it";
} as IDoubleListener );


long start = System.currentTimeMillis();
while ( reader.readPacket() == null ) {}
// The windower made above would be closed now, but that doesn't
// actually do anything in this case.
long time = System.currentTimeMillis() - start;


int numberOfFrames = distances.size();

distances.add( (double)0 );


println ""
println "Done! There were ${numberOfFrames} frames, and they were" +
	" processed in ${time}ms."
println "Now for the scene detection:"
println ""


def mockBool = { double arg -> arg };
def not = { double arg -> 1 - arg };
def and = { double... args -> args.inject( 1, { a, b -> a * b } ) };
def or =
	{ double... args -> not( and( args.collect { not( it ) } as Object[] ) ) };

def unblurredDistances =
	numberOfFrames == 0 ? [] : [ 0 ] + (1..<numberOfFrames).collect { or(
		mockBool( distances[ it ] ),
		Math.min(
			mockBool( distances[ it ] ),
			Math.max( distances[ it - 1 ], distances[ it + 1 ] )
		)
	) };


def bestUnblurredDistances = unblurredDistances.sort().reverse();

if ( numberOfFrames != 0 )
{
	bestUnblurredDistances.findAll { bestUnblurredDistances.head() / 2 <= it }.
		each
	{ distance ->
		
		unblurredDistances.findIndexValues { it == distance }.each {
			
			println "Frame " + it + " has unblurred distance " + distance + "."
		};
	}
}

println ""
println "Done!"