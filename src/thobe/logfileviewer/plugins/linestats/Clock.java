/*
 *  Copyright (C) 2014, Thomas Obenaus. All rights reserved.
 *  Licensed under the New BSD License (3-clause lic)
 *  See attached license-file.
 *
 *	Author: 	Thomas Obenaus
 *	EMail:		obenaus.thomas@gmail.com
 *  Project:    LogFileViewer
 */

package thobe.logfileviewer.plugins.linestats;

import java.util.regex.Pattern;

import thobe.logfileviewer.plugin.source.logline.ILogLine;
import thobe.logfileviewer.plugin.util.PatternMatch;

/**
 * @author Thomas Obenaus
 * @source Clock.java
 * @date Apr 27, 2015
 */
public class Clock
{

	private Pattern	pat;
	private long	start;
	private long	currentTime;

	public Clock( Pattern pat )
	{

		this.pat = pat;
		this.currentTime = 0;
		this.start = 0;
	}

	public long getCurrentTime( )
	{
		return currentTime;
	}

	public long getStart( )
	{
		return start;
	}

	public Pattern getClockFilter( )
	{
		return pat;
	}

	public long getElapsed( )
	{
		return this.getCurrentTime( ) - this.getStart( );
	}

	public boolean updateTime( ILogLine ll ) throws ClockDetectedException
	{
		boolean result = false;
		if ( PatternMatch.matches( this.pat, ll ) )
		{
			result = true;
			long tsOfCurrentLL = ll.getTimeStamp( );

			if ( this.start == 0 )
			{
				this.start = tsOfCurrentLL;
			}

			if ( tsOfCurrentLL < this.currentTime )
			{
				this.start = tsOfCurrentLL;
				this.currentTime = tsOfCurrentLL;
				throw new ClockDetectedException( "Reverse clock detected", ll.getData( ), this.pat.toString( ) );
			}

			this.currentTime = tsOfCurrentLL;
		}

		return result;
	}

	public void reset( Pattern pat )
	{
		this.pat = pat;
		this.start = 0;
		this.currentTime = 0;
	}

}
