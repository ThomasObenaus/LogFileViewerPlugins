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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import thobe.logfileviewer.plugin.Plugin;
import thobe.logfileviewer.plugin.api.IPluginUIComponent;
import thobe.logfileviewer.plugin.source.logline.ILogLine;
import thobe.logfileviewer.plugin.util.PatternMatch;
import thobe.logfileviewer.plugin.util.SizeOf;
import thobe.logfileviewer.plugins.linestats.gui.LineStatsPanel;

/**
 * @author Thomas Obenaus
 * @source LineStatsPlugin.java
 * @date Apr 13, 2015
 */
public class LineStatsPlugin extends Plugin
{
	private static final String				L_NAME			= "thobe.logfileviewer.plugins.linestats";
	private static final int				MAJOR_VERSION	= 0;
	private static final int				MINOR_VERSION	= 1;
	private static final int				BUGFIX_VERSION	= 0;

	private static final Pattern			ALL_FILTER		= Pattern.compile( ".*" );

	private Map<Pattern, LineStatistics>	countsForCurrentRun;
	private Map<Pattern, Long>				patLineCounter;
	private long							startOfCurrentRun;

	private List<ILogLine>					llBuffer;
	private Semaphore						eventSemaphore;
	private boolean							tracingRunning;

	private LineStatsPanel					pa_lineStats;

	public LineStatsPlugin( )
	{
		super( "LineStats", L_NAME );
		this.pa_lineStats = new LineStatsPanel( this );

		this.tracingRunning = false;
		this.countsForCurrentRun = new HashMap<Pattern, LineStatistics>( );
		this.patLineCounter = new HashMap<Pattern, Long>( );

		// add the all filter for the remaining log-lines
		this.countsForCurrentRun.put( ALL_FILTER, new LineStatistics( ALL_FILTER ) );
		this.patLineCounter.put( ALL_FILTER, new Long( 0 ) );

		this.llBuffer = new ArrayList<ILogLine>( );
		this.eventSemaphore = new Semaphore( 0, true );

	}

	public boolean isTracingRunning( )
	{
		return this.tracingRunning;
	}

	@Override
	public void run( )
	{
		LOG( ).info( this.getPluginName( ) + " entered main-loop" );

		List<ILogLine> block = new ArrayList<>( );
		while ( !this.isQuitRequested( ) )
		{

			block.clear( );

			// collect all lines
			synchronized ( this.llBuffer )
			{
				block.addAll( this.llBuffer );
				this.llBuffer.clear( );
			}

			this.updateCounters( block );

			//			synchronized ( this.countsForCurrentRun )
			//			{
			//				if ( this.isTracingRunning( ) )
			//				{
			//					LineStatistics ls = this.getLineStatForPattern( ALL_FILTER );
			//
			//					long accLines = ls.getAccumulatedLines( );
			//					float elapsedTime = ls.getElapsedTime( ) / 1000.f;
			//					float lps = 0;
			//					if ( elapsedTime > 0 )
			//					{
			//						lps = accLines / elapsedTime;
			//					}
			//
			//					LOG( ).info( "Lines=" + accLines + " (in " + elapsedTime + "s => " + lps + " lps), linesInLastSec=" + ls.getLinesInLast( LinesInLastNMilliseconds.LINES_IN_LAST_SECOND ) + ", linesInLast10Sec=" + ls.getLinesInLast( LinesInLastNMilliseconds.LINES_IN_LAST_10_SECONDS ) + ", linesInLastMin=" + ls.getLinesInLast( LinesInLastNMilliseconds.LINES_IN_LAST_MINUTE ) );
			//				}
			//			}

			try
			{
				this.eventSemaphore.tryAcquire( 500, TimeUnit.MILLISECONDS );
			}
			catch ( InterruptedException e )
			{
				LOG( ).severe( "Exception caught in llstats-plugin main-loop: " + e.getLocalizedMessage( ) );
			}
		}// while ( !this.isQuitRequested( ) ).

		LOG( ).info( this.getPluginName( ) + " left main-loop" );
	}

	public LineStatistics addFilter( Pattern filter )
	{
		LineStatistics added = new LineStatistics( filter );
		synchronized ( this.countsForCurrentRun )
		{
			this.countsForCurrentRun.put( filter, added );
			this.patLineCounter.put( filter, new Long( 0 ) );
		}
		return added;
	}

	public void removeFilter( List<Pattern> filters )
	{
		synchronized ( this.countsForCurrentRun )
		{
			for ( Pattern filter : filters )
			{
				this.countsForCurrentRun.remove( filter );
				this.patLineCounter.remove( filter );
			}
		}
	}

	public List<LineStatistics> getLineStats( )
	{
		List<LineStatistics> stats = new ArrayList<>( );
		synchronized ( countsForCurrentRun )
		{
			for ( Map.Entry<Pattern, LineStatistics> e : this.countsForCurrentRun.entrySet( ) )
			{
				stats.add( e.getValue( ) );
			}
		}

		return stats;
	}

	public LineStatistics getLineStatForPattern( Pattern pat )
	{
		LineStatistics result = null;
		synchronized ( this.countsForCurrentRun )
		{
			result = this.countsForCurrentRun.get( pat );
		}
		return result;
	}

	public void startStatTracing( )
	{
		synchronized ( countsForCurrentRun )
		{
			this.tracingRunning = true;
			this.startOfCurrentRun = System.currentTimeMillis( );

			for ( Map.Entry<Pattern, Long> entry : this.patLineCounter.entrySet( ) )
			{
				this.countsForCurrentRun.get( entry.getKey( ) ).reset( startOfCurrentRun );
			}

			LOG( ).info( this.getPluginName( ) + " Tracing started at " + this.startOfCurrentRun );
		}// synchronized ( countsForCurrentRun )

		this.eventSemaphore.release( );
	}

	public void stopTracing( )
	{
		synchronized ( countsForCurrentRun )
		{
			this.tracingRunning = false;
			LOG( ).info( this.getPluginName( ) + " Tracing stopped at " + System.currentTimeMillis( ) );
		}
		this.eventSemaphore.release( );
	}

	private void updateCounters( List<ILogLine> block )
	{
		long currentTime = System.currentTimeMillis( );

		for ( Map.Entry<Pattern, Long> e : this.patLineCounter.entrySet( ) )
		{
			e.setValue( new Long( 0 ) );
		}

		for ( ILogLine l : block )
		{
			for ( Map.Entry<Pattern, Long> entry : this.patLineCounter.entrySet( ) )
			{
				if ( PatternMatch.matches( entry.getKey( ), l ) )
				{
					entry.setValue( entry.getValue( ) + 1 );
				}
			}// for ( Map.Entry<Pattern, Long> entry : this.countsForCurrentRun.entrySet( ) )
		}// for ( ILogLine l : block )

		// Now update the counts
		synchronized ( countsForCurrentRun )
		{
			for ( Map.Entry<Pattern, Long> entry : this.patLineCounter.entrySet( ) )
			{
				this.countsForCurrentRun.get( entry.getKey( ) ).addLines( entry.getValue( ), currentTime );
			}
		}// synchronized ( countsForCurrentRun )
	}

	@Override
	public int getMajorVersion( )
	{
		return MAJOR_VERSION;
	}

	@Override
	public int getMinorVersion( )
	{
		return MINOR_VERSION;
	}

	@Override
	public int getBugfixVersion( )
	{
		return BUGFIX_VERSION;
	}

	@Override
	public long getMemory( )
	{
		final long maxPatternSize = SizeOf.STRING( "1234567890123456789012345678901234567890" );

		int numEntries = 0;
		synchronized ( this.countsForCurrentRun )
		{
			numEntries = this.countsForCurrentRun.size( );
		}

		long mem = ( SizeOf.LONG + SizeOf.REFERENCE + maxPatternSize ) * numEntries;
		return mem;
	}

	@Override
	public void freeMemory( )
	{
		synchronized ( this.llBuffer )
		{
			this.llBuffer.clear( );
		}

		this.eventSemaphore.release( );
	}

	@Override
	public void onNewBlockOfLines( List<ILogLine> blockOfLines )
	{
		boolean tracing = false;
		synchronized ( this.countsForCurrentRun )
		{
			tracing = this.tracingRunning;
		}

		synchronized ( this.llBuffer )
		{
			if ( tracing )
			{
				this.llBuffer.addAll( blockOfLines );
			}
		}

		if ( tracing )
		{
			this.eventSemaphore.release( );
		}
	}

	@Override
	public void onLogStreamOpened( )
	{
		this.startStatTracing( );
	}

	@Override
	public void onPrepareCloseLogStream( )
	{
		this.stopTracing( );
	}

	@Override
	public void onUnRegistered( )
	{
		this.eventSemaphore.release( );
	}

	@Override
	public boolean onStopped( )
	{
		this.eventSemaphore.release( );
		return true;
	}

	@Override
	public IPluginUIComponent getUIComponent( )
	{
		return this.pa_lineStats;
	}
}
