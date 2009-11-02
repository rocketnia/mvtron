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
		prn( "luma of each frame processed, scaled to the range 0..0x300-3." );
		prn( "Then, it will print the number of frames and how long the loop" );
		prn( "took." );
		prn();
		pr( "What file should be used? " );
		String filename = in.nextLine();
		prn();
		
		IMediaReader reader = ToolFactory.makeReader( filename );
		
		reader.setBufferedImageTypeToGenerate( BufferedImage.TYPE_3BYTE_BGR );
		
		final int[] numberOfFrames = new int[] { 0 };
		
		final float maxAverageLuma = 0x300 - 3;
		
		reader.addListener( new RgbArrayListenerTool( new IRgbArrayListener() {
			
			public void onRgb( int[] rgb )
			{
				int thisAverageLuma = 0;
				
				for ( int pixel: rgb )
				{
					// The pixels are in ARGB format.
					
//					assert 0xFF == (0xFF & pixel >>> 24);  // alpha
					
					thisAverageLuma +=
						(0xFF & pixel >>> 16) +  // R
						(0xFF & pixel >>>  8) +  // G
						(0xFF & pixel       );   // B
				}
				
				prn( thisAverageLuma / maxAverageLuma / rgb.length );
				numberOfFrames[ 0 ]++;
			}
		} ) );
		
		
		long start = System.currentTimeMillis();
		while ( reader.readPacket() == null ) {}
		long time = System.currentTimeMillis() - start;
		
		prn();
		prn( "Done! There were " + numberOfFrames[ 0 ] +
			" frames, and they were processed in" );
		prn( time + "ms." );
	}
	
	private static void prn() { System.out.println(); }
	private static void prn( Object s ) { System.out.println( s.toString() ); }
	private static void pr ( Object s ) { System.out.print  ( s.toString() ); }
}