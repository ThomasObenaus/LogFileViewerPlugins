/*
 *  Copyright (C) 2015, Thomas Obenaus. All rights reserved.
 *  Licensed under the New BSD License (3-clause lic)
 *  See attached license-file.
 *
 *	Author: 	Thomas Obenaus
 *	EMail:		obenaus.thomas@gmail.com
 *  Project:    LineStats
 */

package thobe.logfileviewer.plugins.linestats;

import java.util.logging.Logger;
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
	private Logger	log;

	public Clock( Logger log, Pattern pat )
	{
		this.log = log;
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
				LOG( ).info( "Clock [" + pat.toString( ) + "] started (startOfClock=" + this.start + ")" );
			}

			if ( tsOfCurrentLL < this.currentTime )
			{
				this.start = tsOfCurrentLL;
				this.currentTime = tsOfCurrentLL;
				throw new ClockDetectedException( "Reverse clock detected (startOfClock=" + this.start + ", currentClock=" + this.currentTime + ", newCurrent=" + tsOfCurrentLL + ")", ll.getData( ), this.pat.toString( ) );
			}

			this.currentTime = tsOfCurrentLL;
		}// if ( PatternMatch.matches( this.pat, ll ) )

		return result;
	}

	public void reset( Pattern pat )
	{
		LOG( ).info( "Clock [" + pat.toString( ) + "] reset (startOfClock=" + this.start + ", currentClock=" + this.currentTime + ")" );
		this.pat = pat;
		this.start = 0;
		this.currentTime = 0;
	}

	protected Logger LOG( )
	{
		return log;
	}

}
