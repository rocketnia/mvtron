package com.rocketnia.mvtron.analyzer;

import java.awt.image.BufferedImage;
import java.util.Scanner;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.ToolFactory;

public class MvTronAnalyzerJavaSandbox
{
	public static void main( String[] args )
	{
		Scanner in = new Scanner( System.in );
		
		prn( "This sandbox will loop through a video, printing the average" );
		prn( "luma of each frame processed, scaled to the range 0.0..1.0." );
		prn( "Then, it will print the number of frames and how long the loop" );
		prn( "took." );
		prn();
		pr( "What file should be used? " );
		String filename = in.nextLine();
		prn();
		
		IMediaReader reader = ToolFactory.makeReader( filename );
		
		reader.setBufferedImageTypeToGenerate( BufferedImage.TYPE_3BYTE_BGR );
		
		LumaAverager lumaAverager = new LumaAverager();
		reader.addListener( lumaAverager.newTool() );
		
		FrameCounter frameCounter = new FrameCounter();
		reader.addListener( frameCounter.newTool() );
		
		lumaAverager.addListener( new IFloatListener() {
			
			public void onFloat( float f )
			{
				prn( f );
			}
		} );
		
		
		long start = System.currentTimeMillis();
		while ( reader.readPacket() == null ) {}
		long time = System.currentTimeMillis() - start;
		
		prn();
		prn( "Done! There were " + frameCounter.getData() +
			" frames, and they were processed in" );
		prn( time + "ms." );
	}
	
	private static void prn() { System.out.println(); }
	private static void prn( Object s ) { System.out.println( s.toString() ); }
	private static void pr ( Object s ) { System.out.print  ( s.toString() ); }
}