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
import java.util.regex.PatternSyntaxException;

import thobe.logfileviewer.plugin.Plugin;
import thobe.logfileviewer.plugin.api.IPluginAccess;
import thobe.logfileviewer.plugin.api.IPluginPreferences;
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
	private static final int				MINOR_VERSION	= 3;
	private static final int				BUGFIX_VERSION	= 0;

	private static final Pattern			ALL_FILTER		= Pattern.compile( ".*" );

	private Map<String, LineStatistics>		countsForCurrentRun;
	private Map<Pattern, Long>				patLineCounter;
	private long							startOfCurrentRun;

	private List<ILogLine>					llBuffer;
	private Semaphore						eventSemaphore;
	private boolean							tracingRunning;

	private List<ILineStatsPluginListener>	listeners;

	private LineStatsPanel					pa_lineStats;
	private LineStatPreferences				lineStatPrefs;

	private Clock							clock;
	private List<IClockListener>			clockListeners;

	public LineStatsPlugin( )
	{
		super( L_NAME, L_NAME );
		this.clockListeners = new ArrayList<IClockListener>( );
		this.clock = new Clock( Pattern.compile( ".*iMX6.*" ) );
		this.lineStatPrefs = new LineStatPreferences( LOG( ) );
		this.listeners = new ArrayList<ILineStatsPluginListener>( );
		this.pa_lineStats = new LineStatsPanel( LOG( ), this );
		this.addListener( this.pa_lineStats );
		this.addClockListener( this.pa_lineStats );

		this.tracingRunning = false;
		this.countsForCurrentRun = new HashMap<String, LineStatistics>( );
		this.patLineCounter = new HashMap<Pattern, Long>( );

		// add the all filter for the remaining log-lines
		this.countsForCurrentRun.put( ALL_FILTER.toString( ), new LineStatistics( ALL_FILTER ) );
		this.patLineCounter.put( ALL_FILTER, new Long( 0 ) );

		this.llBuffer = new ArrayList<>( );
		this.eventSemaphore = new Semaphore( 0, true );

	}

	public void addClockListener( IClockListener l )
	{
		this.clockListeners.add( l );
	}

	public void addListener( ILineStatsPluginListener l )
	{
		this.listeners.add( l );
	}

	public boolean isTracingRunning( )
	{
		return this.tracingRunning;
	}

	@Override
	public void run( )
	{
		LOG( ).info( this.getPluginName( ) + " entered main-loop" );

		List<ILogLine> llBlocks = new ArrayList<>( );
		while ( !this.isQuitRequested( ) )
		{
			llBlocks.clear( );

			// collect all lines
			synchronized ( this.llBuffer )
			{
				if ( !this.llBuffer.isEmpty( ) )
				{
					llBlocks.addAll( this.llBuffer );
					this.llBuffer.clear( );
				}
			}// synchronized ( this.llBuffer )

			this.updateCounters( llBlocks );

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

	/**
	 * Adds a new filter and returns the newly created filter. The method returns null if the filter is already present.
	 * @param filter
	 * @return
	 */
	public LineStatistics addFilter( Pattern filter )
	{
		LineStatistics added = new LineStatistics( filter );
		added.reset( );

		synchronized ( this.countsForCurrentRun )
		{
			if ( this.countsForCurrentRun.put( filter.toString( ), added ) == null )
			{
				this.patLineCounter.put( filter, new Long( 0 ) );
			}
			else
			{
				added = null;
			}
		}
		return added;
	}

	public List<LineStatistics> addFilters( List<Pattern> filters )
	{
		List<LineStatistics> tmp = new ArrayList<LineStatistics>( );
		for ( Pattern pat : filters )
		{
			LineStatistics added = new LineStatistics( pat );
			added.reset( );
			tmp.add( added );
		}

		List<LineStatistics> result = new ArrayList<LineStatistics>( );
		synchronized ( this.countsForCurrentRun )
		{
			for ( LineStatistics stat : tmp )
			{
				if ( this.countsForCurrentRun.put( stat.getFilter( ).toString( ), stat ) == null )
				{
					this.patLineCounter.put( stat.getFilter( ), new Long( 0 ) );
					result.add( stat );
				}
			}
		}
		return result;
	}

	public void removeFilters( List<LineStatistics> filters )
	{
		synchronized ( this.countsForCurrentRun )
		{
			for ( LineStatistics stat : filters )
			{
				this.countsForCurrentRun.remove( stat.getFilter( ).toString( ) );
				this.patLineCounter.remove( stat.getFilter( ) );
			}
		}
	}

	public List<LineStatistics> getLineStats( )
	{
		List<LineStatistics> stats = new ArrayList<>( );
		synchronized ( countsForCurrentRun )
		{
			for ( Map.Entry<String, LineStatistics> e : this.countsForCurrentRun.entrySet( ) )
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
			this.clock.reset( );
			this.fireClockReset( );
			this.tracingRunning = true;
			this.startOfCurrentRun = System.currentTimeMillis( );

			for ( Map.Entry<Pattern, Long> entry : this.patLineCounter.entrySet( ) )
			{
				this.countsForCurrentRun.get( entry.getKey( ).toString( ) ).reset( );
			}

			LOG( ).info( this.getPluginName( ) + " Tracing started at " + this.startOfCurrentRun );
		}// synchronized ( countsForCurrentRun )

		this.fireTracingStarted( );
		this.eventSemaphore.release( );
	}

	public void stopTracing( )
	{
		synchronized ( countsForCurrentRun )
		{
			this.tracingRunning = false;
			LOG( ).info( this.getPluginName( ) + " Tracing stopped at " + System.currentTimeMillis( ) );
		}
		this.fireTracingStopped( );
		this.eventSemaphore.release( );
	}

	private void updateCounters( List<ILogLine> block )
	{
		for ( Map.Entry<Pattern, Long> e : this.patLineCounter.entrySet( ) )
		{
			e.setValue( new Long( 0 ) );
		}

		long start = this.clock.getCurrentTime( );

		for ( ILogLine ll : block )
		{
			try
			{
				if ( this.clock.updateTime( ll ) )
				{
					for ( Map.Entry<Pattern, Long> entry : this.patLineCounter.entrySet( ) )
					{
						if ( PatternMatch.matches( entry.getKey( ), ll ) )
						{
							entry.setValue( entry.getValue( ) + 1 );

						}// if ( PatternMatch.matches( entry.getKey( ), ll ) )

					}// for ( Map.Entry<Pattern, Long> entry : this.patLineCounter.entrySet( ) )
				}// if ( this.clock.updateTime( ll ) )
			}
			catch ( ClockDetectedException e1 )
			{
				start = this.clock.getCurrentTime( );
				LOG( ).warning( e1.getLocalizedMessage( ) );
				this.fireClockError( e1.getLocalizedMessage( ) );
			}

		}// for ( ILogLine ll : block )

		TimeRange timeRange = new TimeRange( start, this.clock.getCurrentTime( ) );
		this.fireClockTimeUpdated( this.clock.getCurrentTime( ), this.clock.getElapsed( ) );

		// Now update the counts
		synchronized ( countsForCurrentRun )
		{
			for ( Map.Entry<Pattern, Long> entry : this.patLineCounter.entrySet( ) )
			{
				Long value = entry.getValue( );
				this.countsForCurrentRun.get( entry.getKey( ).toString( ) ).addLines( value, timeRange );
			}// for ( Map.Entry<Pattern, Long> entry : this.patLineCounter.entrySet( ) )
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
			if ( tracing && ( !blockOfLines.isEmpty( ) ) )
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
		// persist filters
		List<LineStatistics> lineStats = this.getLineStats( );
		List<String> filters = new ArrayList<String>( );
		for ( LineStatistics ls : lineStats )
		{
			filters.add( ls.getFilter( ).toString( ) );
		}
		this.lineStatPrefs.setFilters( filters );

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

	private void fireTracingStopped( )
	{
		for ( ILineStatsPluginListener l : this.listeners )
		{
			l.onStopTracing( );
		}
	}

	private void fireTracingStarted( )
	{
		for ( ILineStatsPluginListener l : this.listeners )
		{
			l.onStartTracing( );
		}
	}

	public LineStatPreferences getPrefs( )
	{
		return this.lineStatPrefs;
	}

	@Override
	public IPluginPreferences getPluginPreferences( )
	{
		return this.lineStatPrefs;
	}

	@Override
	public boolean onRegistered( IPluginAccess pluginAccess )
	{
		// add persisted filters
		List<String> persistedFilters = this.lineStatPrefs.getFilters( );
		List<Pattern> filters = new ArrayList<Pattern>( );
		for ( String filterStr : persistedFilters )
		{
			try
			{
				Pattern filter = Pattern.compile( filterStr );
				filters.add( filter );
			}
			catch ( PatternSyntaxException e )
			{
				LOG( ).severe( "Ignore persisted filter '" + filterStr + "': " + e.getLocalizedMessage( ) );
			}
		}// for ( String filterStr : persistedFilters )

		this.pa_lineStats.addFilters( filters );
		return true;
	}

	private void fireClockTimeUpdated( long currentTime, long elapsedTime )
	{
		for ( IClockListener cl : this.clockListeners )
		{
			cl.onTimeUpdated( currentTime, elapsedTime );
		}
	}

	private void fireClockReset( )
	{
		for ( IClockListener cl : this.clockListeners )
		{
			cl.onReset( );
		}
	}

	private void fireClockError( String err )
	{
		for ( IClockListener cl : this.clockListeners )
		{
			cl.onError( err );
		}
	}

}
