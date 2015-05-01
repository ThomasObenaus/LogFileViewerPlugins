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
import java.util.regex.Pattern;

/**
 * @author Thomas Obenaus
 * @source LineStatistics.java
 * @date Apr 13, 2015
 */
public class LineStatistics
{
	private Map<LinesInLastNMilliseconds, IntervalAccumulator>	linesInLastNSeconds;
	private long												accumulatedLines;
	private long												startTimeStamp;
	private long												elapsedTime;
	private Pattern												filter;

	public LineStatistics( Pattern filter )
	{
		this.filter = filter;
		this.accumulatedLines = 0;
		this.startTimeStamp = 0;
		this.elapsedTime = 0;
		this.linesInLastNSeconds = new HashMap<LinesInLastNMilliseconds, IntervalAccumulator>( );

		this.linesInLastNSeconds.put( LinesInLastNMilliseconds.LINES_IN_LAST_SECOND, new IntervalAccumulator( LinesInLastNMilliseconds.LINES_IN_LAST_SECOND ) );
		this.linesInLastNSeconds.put( LinesInLastNMilliseconds.LINES_IN_LAST_10_SECONDS, new IntervalAccumulator( LinesInLastNMilliseconds.LINES_IN_LAST_10_SECONDS ) );
		this.linesInLastNSeconds.put( LinesInLastNMilliseconds.LINES_IN_LAST_30_SECONDS, new IntervalAccumulator( LinesInLastNMilliseconds.LINES_IN_LAST_30_SECONDS ) );
		this.linesInLastNSeconds.put( LinesInLastNMilliseconds.LINES_IN_LAST_60_SECONDS, new IntervalAccumulator( LinesInLastNMilliseconds.LINES_IN_LAST_60_SECONDS ) );
	}

	public String getFilterName( )
	{
		String result = "UNKNOWN";

		if ( this.filter != null )
		{
			result = this.filter.toString( );

			if ( result.startsWith( ".*" ) )
			{
				result = result.substring( 2, result.length( ) );
			}

			if ( result.endsWith( ".*" ) )
			{
				result = result.substring( 0, result.length( ) - 2 );
			}

			if ( result.trim( ).isEmpty( ) )
			{
				result = "ALL";
			}
		}// if ( this.filter != null )

		return result;
	}

	public Pattern getFilter( )
	{
		return filter;
	}

	/**
	 * Resets the complete stattistic and takes the new start-time for the next interval.
	 * @param startTimeStamp
	 */
	public void reset( )
	{
		this.startTimeStamp = 0;
		this.accumulatedLines = 0;
		this.elapsedTime = 0;
		for ( Map.Entry<LinesInLastNMilliseconds, IntervalAccumulator> e : this.linesInLastNSeconds.entrySet( ) )
		{
			e.getValue( ).resetCompleteData( );
		}
	}

	public double getLPS( )
	{
		long accLines = this.getAccumulatedLines( );
		double elapsedTime = this.getElapsedTime( ) / 1000.0;
		if ( elapsedTime == 0d )
			elapsedTime = 1d;
		return accLines / elapsedTime;
	}

	public long getElapsedTime( )
	{
		return this.elapsedTime;
	}

	public void addLines( long lines, TimeRange timeRange )
	{
		if ( this.startTimeStamp == 0 )
		{
			this.startTimeStamp = timeRange.getStart( );
		}

		this.accumulatedLines += lines;
		this.elapsedTime = timeRange.getEnd( ) - startTimeStamp;

		for ( Map.Entry<LinesInLastNMilliseconds, IntervalAccumulator> e : this.linesInLastNSeconds.entrySet( ) )
		{
			e.getValue( ).addLines( lines, timeRange );
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
			result = acc.getLinesInLastInterVal( );
		}

		return result;
	}

	public float getLPSInLast( LinesInLastNMilliseconds lastNMilliseconds )
	{
		float result = 0;
		IntervalAccumulator acc = this.linesInLastNSeconds.get( lastNMilliseconds );
		if ( acc != null )
		{
			result = acc.getLPSInLastInterval( );
		}

		return result;
	}

	@Override
	public String toString( )
	{
		return this.getFilter( ).toString( );
	}

	@Override
	public int hashCode( )
	{
		int seed = 372;

		if ( this.getFilter( ) != null )
		{
			seed += this.getFilter( ).toString( ).hashCode( );
		}
		return seed;
	}

	@Override
	public boolean equals( Object obj )
	{
		if ( !( obj instanceof LineStatistics ) )
		{
			return false;
		}

		if ( obj == this )
			return true;

		LineStatistics ls = ( LineStatistics ) obj;
		if ( ( ls.getFilter( ) != null ) && ( this.getFilter( ) == null ) )
			return false;
		if ( ( ls.getFilter( ) == null ) && ( this.getFilter( ) != null ) )
			return false;
		if ( ( ls.getFilter( ) == null ) && ( this.getFilter( ) == null ) )
			return true;

		if ( ls.getFilter( ).toString( ).equals( this.getFilter( ).toString( ) ) )
		{
			return true;
		}

		return false;
	}

	private class IntervalAccumulator
	{
		private LinesInLastNMilliseconds	intervalInMs;
		private long						linesInLastInterVal;
		private long						accumulatedLines;
		private long						startTimeStamp;
		private float						LPSInLastInterval;

		public IntervalAccumulator( LinesInLastNMilliseconds intervalInMs )
		{
			this.intervalInMs = intervalInMs;
			this.accumulatedLines = 0;
			this.linesInLastInterVal = 0;
			this.startTimeStamp = 0;
			this.LPSInLastInterval = 0;
		}

		public void addLines( long lines, TimeRange timeRange )
		{
			this.accumulatedLines += lines;
			if ( this.startTimeStamp == 0 )
			{
				this.startTimeStamp = timeRange.getStart( );
			}

			if ( ( timeRange.getEnd( ) - this.startTimeStamp ) >= this.intervalInMs.getMilliseconds( ) )
			{
				float elapsedTimeInS = ( timeRange.getEnd( ) - this.startTimeStamp ) / 1000f;
				this.LPSInLastInterval = this.accumulatedLines / ( float ) elapsedTimeInS;
				this.linesInLastInterVal = this.accumulatedLines;
				this.newIntervalStarts( );
			}
		}

		public void newIntervalStarts( )
		{
			this.startTimeStamp = 0;
			this.accumulatedLines = 0;
		}

		public void resetCompleteData( )
		{
			this.startTimeStamp = 0;
			this.accumulatedLines = 0;
			this.linesInLastInterVal = 0;
			this.LPSInLastInterval = 0;
		}

		public long getLinesInLastInterVal( )
		{
			return linesInLastInterVal;
		}

		public float getLPSInLastInterval( )
		{
			return LPSInLastInterval;
		}

	}
}
