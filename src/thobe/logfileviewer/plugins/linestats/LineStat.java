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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Thomas Obenaus
 * @source LineStat.java
 * @date Apr 13, 2015
 */
public class LineStat
{
	private Map<LinesInLastNMilliseconds, IntervalAccumulator>	linesInLastNSeconds;
	private long												accumulatedLines;
	private long												startTimeStamp;
	private long												elapsedTime;

	public LineStat( )
	{
		this.accumulatedLines = 0;
		this.startTimeStamp = 0;
		this.elapsedTime = 0;
		this.linesInLastNSeconds = new HashMap<LinesInLastNMilliseconds, IntervalAccumulator>( );

		this.linesInLastNSeconds.put( LinesInLastNMilliseconds.LINES_IN_LAST_SECOND, new IntervalAccumulator( LinesInLastNMilliseconds.LINES_IN_LAST_SECOND ) );
		this.linesInLastNSeconds.put( LinesInLastNMilliseconds.LINES_IN_LAST_10_SECONDS, new IntervalAccumulator( LinesInLastNMilliseconds.LINES_IN_LAST_10_SECONDS ) );
		this.linesInLastNSeconds.put( LinesInLastNMilliseconds.LINES_IN_LAST_MINUTE, new IntervalAccumulator( LinesInLastNMilliseconds.LINES_IN_LAST_MINUTE ) );
	}

	public void reset( long startTimeStamp )
	{
		this.startTimeStamp = startTimeStamp;
		this.accumulatedLines = 0;
		this.elapsedTime = 0;
		for ( Map.Entry<LinesInLastNMilliseconds, IntervalAccumulator> e : this.linesInLastNSeconds.entrySet( ) )
		{
			e.getValue( ).reset( startTimeStamp );
		}
	}

	public long getElapsedTime( )
	{
		return this.elapsedTime;
	}

	public void addLines( long lines, long newTimeStamp )
	{
		this.accumulatedLines += lines;
		this.elapsedTime = newTimeStamp - startTimeStamp;
		for ( Map.Entry<LinesInLastNMilliseconds, IntervalAccumulator> e : this.linesInLastNSeconds.entrySet( ) )
		{
			e.getValue( ).addLines( lines, newTimeStamp );
		}

	}

	public long getAccumulatedLines( )
	{
		return this.accumulatedLines;
	}

	public long getLinesInLast( LinesInLastNMilliseconds lastNMilliseconds )
	{
		long result = 0;
		IntervalAccumulator acc = this.linesInLastNSeconds.get( lastNMilliseconds );
		if ( acc != null )
		{
			result = acc.getLinesInLastInterval( );
		}

		return result;
	}

	private class IntervalAccumulator
	{
		private LinesInLastNMilliseconds	intervalInMs;
		private long						accumulatedLines;
		private long						linesInLastInterval;
		private long						startTimeStamp;
		private boolean						initThresholdMet;

		public IntervalAccumulator( LinesInLastNMilliseconds intervalInMs )
		{
			this.intervalInMs = intervalInMs;
			this.accumulatedLines = 0;
			this.linesInLastInterval = 0;
			this.startTimeStamp = 0;
			this.initThresholdMet = false;
		}

		public void addLines( long lines, long newTimeStamp )
		{
			this.accumulatedLines += lines;
			if ( ( newTimeStamp - this.startTimeStamp ) >= this.intervalInMs.getMilliseconds( ) )
			{
				this.initThresholdMet = ( this.startTimeStamp != 0 );
				this.reset( newTimeStamp );

			}

			if ( !this.initThresholdMet )
			{
				this.linesInLastInterval = this.accumulatedLines;
			}

		}

		public void reset( long timeStamp )
		{
			this.linesInLastInterval = this.accumulatedLines;
			this.startTimeStamp = timeStamp;
			this.accumulatedLines = 0;
		}

		public long getLinesInLastInterval( )
		{
			return linesInLastInterval;
		}
	}
}
