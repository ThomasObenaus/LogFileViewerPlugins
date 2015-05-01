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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import thobe.logfileviewer.plugin.api.IPluginPreferences;

/**
 * @author Thomas Obenaus
 * @source LineStatPreferences.java
 * @date Apr 21, 2015
 */
public class LineStatPreferences implements IPluginPreferences
{
	private static final String	NODE_FILTERS			= "filters";
	private static final String	PRP_FILTER_FILE_PATH	= "filterFilePath";
	private static final String	PRP_FILTER				= "filter";
	private static final String	PRP_TRACING_ENABLED		= "tracingEnabled";
	private static final String	PRP_CLOCK_FILTER		= "clock-filter";

	private File				fileFilterPath;
	private List<String>		filters;
	private Logger				log;
	private Pattern				clockFilter;
	private boolean				tracingEnabled;

	public LineStatPreferences( Logger log )
	{
		this.log = log;
		this.fileFilterPath = new File( "" );
		this.filters = new ArrayList<String>( );
		this.tracingEnabled = false;
	}

	public void setTracingEnabled( boolean tracingEnabled )
	{
		this.tracingEnabled = tracingEnabled;
	}

	public boolean isTracingEnabled( )
	{
		return this.tracingEnabled;
	}

	public void setClockFilter( Pattern clockFilter )
	{
		this.clockFilter = clockFilter;
	}

	public Pattern getClockFilter( )
	{
		return this.clockFilter;
	}

	public void setFilters( List<String> filters )
	{
		this.filters = filters;
	}

	public List<String> getFilters( )
	{
		return filters;
	}

	public void setFileFilterPath( File fileFilterPath )
	{
		this.fileFilterPath = fileFilterPath;
	}

	public File getFileFilterPath( )
	{
		return fileFilterPath;
	}

	@Override
	public void load( Preferences pluginPrefRoot )
	{
		String fileFilterPathStr = pluginPrefRoot.get( PRP_FILTER_FILE_PATH, "" );
		this.fileFilterPath = new File( fileFilterPathStr );

		this.tracingEnabled = pluginPrefRoot.getBoolean( PRP_TRACING_ENABLED, false );

		String clockFilterStr = pluginPrefRoot.get( PRP_CLOCK_FILTER, "" );
		this.clockFilter = null;
		if ( ( clockFilterStr != null ) && ( !clockFilterStr.trim( ).isEmpty( ) ) )
		{
			try
			{
				this.clockFilter = Pattern.compile( clockFilterStr );
			}
			catch ( PatternSyntaxException e )
			{
				LOG( ).warning( "Ignore clock-filter '" + clockFilterStr + "' since it is not a valid filter: " + e.getLocalizedMessage( ) );
			}
		}
		// load persisted filters
		this.filters.clear( );
		Preferences filtersNode = pluginPrefRoot.node( NODE_FILTERS );
		try
		{
			String childNames[] = filtersNode.keys( );
			for ( String childName : childNames )
			{
				String filterStr = filtersNode.get( childName, "" );
				if ( !this.filters.contains( filterStr ) )
				{
					this.filters.add( filterStr );
				}
			}
		}
		catch ( BackingStoreException e )
		{
			LOG( ).severe( "Error loading persisted filters: " + e.getLocalizedMessage( ) );
		}

	}

	@Override
	public void save( Preferences pluginPrefRoot )
	{
		pluginPrefRoot.put( PRP_FILTER_FILE_PATH, this.fileFilterPath.getAbsolutePath( ) );

		pluginPrefRoot.putBoolean( PRP_TRACING_ENABLED, this.tracingEnabled );

		String clockFilterStr = "";
		if ( this.clockFilter != null )
		{
			clockFilterStr = this.clockFilter.toString( );
		}
		pluginPrefRoot.put( PRP_CLOCK_FILTER, clockFilterStr );

		// persist filters
		Preferences filtersNode = pluginPrefRoot.node( NODE_FILTERS );
		try
		{
			filtersNode.clear( );
		}
		catch ( BackingStoreException e )
		{
			LOG( ).severe( "Error during clear of persisted filters: " + e.getLocalizedMessage( ) );
		}
		for ( int i = 0; i < this.filters.size( ); ++i )
		{
			filtersNode.put( PRP_FILTER + "_" + i, this.filters.get( i ) );
		}
	}

	protected Logger LOG( )
	{
		return this.log;
	}

}
