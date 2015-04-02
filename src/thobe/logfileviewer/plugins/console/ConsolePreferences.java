/*
 *  Copyright (C) 2014, Thomas Obenaus. All rights reserved.
 *  Licensed under the New BSD License (3-clause lic)
 *  See attached license-file.
 *
 *	Author: 	Thomas Obenaus
 *	EMail:		obenaus.thomas@gmail.com
 *  Project:    LogFileViewer
 */

package thobe.logfileviewer.plugins.console;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import thobe.logfileviewer.plugin.api.IPluginPreferences;

/**
 * @author Thomas Obenaus
 * @source ConsolePreferences.java
 * @date Apr 2, 2015
 */
public class ConsolePreferences implements IPluginPreferences
{
	private static final int	MAX_FILTERS			= 10;

	private static final String	NODE_RECENT_FILTERS	= "RecentFilters";
	private static final String	PRP_FILTER			= "filter_";

	private Logger				log;
	private List<String>		recentFilters;

	public ConsolePreferences( Logger log )
	{
		this.log = log;
		this.recentFilters = new ArrayList<>( );

	}

	public void addRecentFilter( String filter )
	{
		if ( !this.recentFilters.contains( filter ) )
		{
			this.recentFilters.add( filter );
		}

		if ( this.recentFilters.size( ) > MAX_FILTERS )
		{
			this.recentFilters.remove( 0 );
		}
	}

	public String getRecentFilterMatching( String filter )
	{
		String result = "";

		for ( String rF : this.recentFilters )
		{
			if ( rF.matches( filter ) )
			{
				result = rF;
			}
		}

		return result;
	}

	@Override
	public void load( Preferences pluginPrefRoot )
	{
		// load recent filters
		this.recentFilters.clear( );
		Preferences recFilterNode = pluginPrefRoot.node( NODE_RECENT_FILTERS );
		try
		{
			String keys[] = recFilterNode.keys( );
			for ( String key : keys )
			{
				this.addRecentFilter( key );
			}// for ( String key : keys )
		}
		catch ( BackingStoreException e )
		{
			LOG( ).severe( "Exception while loading preferences: " + e.getLocalizedMessage( ) );
		}
	}

	@Override
	public void save( Preferences pluginPrefRoot )
	{
		// save recent filters
		Preferences recFilterNode = pluginPrefRoot.node( NODE_RECENT_FILTERS );
		try
		{
			recFilterNode.clear( );
			for ( int i = 0; i < this.recentFilters.size( ); ++i )
			{
				recFilterNode.put( PRP_FILTER + i, this.recentFilters.get( i ) );
			}// for ( int i = 0; i < this.recentFilters.size( ); ++i )
		}
		catch ( BackingStoreException e )
		{
			LOG( ).severe( "Exception while saving preferences: " + e.getLocalizedMessage( ) );
		}
	}

	protected Logger LOG( )
	{
		return log;
	}

}
