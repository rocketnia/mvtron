// EuclideanSceneDetector.java
//
// Copyright 2009, 2010 Ross Angle

package com.rocketnia.mvtron.analyzer.scenedetectors;

import com.rocketnia.mvtron.analyzer.ISceneDetector;
import com.rocketnia.mvtron.analyzer.IntArrayTimeWindower;
import com.rocketnia.mvtron.analyzer.SceneDetectorTool;

public class EuclideanSceneDetector implements ISceneDetector
{
	private static EuclideanSceneDetector instance =
		new EuclideanSceneDetector();
	private EuclideanSceneDetector() {}
	public static EuclideanSceneDetector getInstance()
		{ return instance; }
	
	public static SceneDetectorTool makeTool()
		{ return new SceneDetectorTool( instance ); }
	
	public static IntArrayTimeWindower makeWindower( int spacing )
		{ return makeTool().newWindower( spacing ); }
	
	public SceneDetectorTool newTool()
		{ return new SceneDetectorTool( this ); }
	
	public IntArrayTimeWindower newWindower( int spacing )
		{ return newTool().newWindower( spacing ); }
	
	// @Override
	public double measureDistance( int[] p, int[] n )
	{
		int length = p.length;
		
		long integerDistanceSquared = 0;
		double distanceSquaredFactor =
			(double)1 / ((long)length * 0xFF * 0xFF * 3);
		
		for ( int i = 0; i < length; i++ )
		{
			int pi = p[ i ];
			int ni = n[ i ];
			
			int rDiff = (0xFF & pi >>>  16) - (0xFF & ni >>>  16);
			int gDiff = (0xFF & pi >>>   8) - (0xFF & ni >>>   8);
			int bDiff = (0xFF & pi        ) - (0xFF & ni        );
			
			// This is the square of the Euclidean norm of
			// [ rDiff, gDiff, bDiff ].
			integerDistanceSquared +=
				rDiff * rDiff +
				gDiff * gDiff +
				bDiff * bDiff;
		}
		
		return Math.sqrt(
			integerDistanceSquared * distanceSquaredFactor );
	}
}
