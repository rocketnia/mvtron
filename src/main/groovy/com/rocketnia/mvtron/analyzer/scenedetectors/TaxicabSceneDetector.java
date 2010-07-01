// TaxicabSceneDetector.java
//
// Copyright 2009, 2010 Ross Angle

package com.rocketnia.mvtron.analyzer.scenedetectors;

import com.rocketnia.mvtron.analyzer.ISceneDetector;
import com.rocketnia.mvtron.analyzer.IntArrayTimeWindower;
import com.rocketnia.mvtron.analyzer.SceneDetectorTool;

public class TaxicabSceneDetector implements ISceneDetector
{
	private static TaxicabSceneDetector instance =
		new TaxicabSceneDetector();
	private TaxicabSceneDetector() {}
	public static TaxicabSceneDetector getInstance()
		{ return instance; }
	
	public static SceneDetectorTool makeTool()
		{ return new SceneDetectorTool( instance ); }
	
	public static IntArrayTimeWindower makeWindower( int spacing )
		{ return makeTool().newWindower( spacing ); }
	
	public SceneDetectorTool newTool()
		{ return new SceneDetectorTool( this ); }
	
	public IntArrayTimeWindower newWindower( int spacing )
		{ return newTool().newWindower( spacing ); }
	
	@Override
	public double measureDistance( int[] p, int[] n )
	{
		int length = p.length;
		
		long integerDistance = 0;
		double distanceFactor = (double)1 / ((long)length * 0xFF * 3);
		
		for ( int i = 0; i < length; i++ )
		{
			int pi = p[ i ];
			int ni = n[ i ];
			
			int rDiff = (0xFF & pi >>>  16) - (0xFF & ni >>>  16);
			int gDiff = (0xFF & pi >>>   8) - (0xFF & ni >>>   8);
			int bDiff = (0xFF & pi        ) - (0xFF & ni        );
			
			// This is the the taxicab norm of
			// [ rDiff, gDiff, bDiff ].
			integerDistance +=
				(rDiff < 0 ? -rDiff : rDiff) +
				(gDiff < 0 ? -gDiff : gDiff) +
				(bDiff < 0 ? -bDiff : bDiff);
		}
		
		return integerDistance * distanceFactor;
	}
}
