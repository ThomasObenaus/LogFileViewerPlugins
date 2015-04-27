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

	private File				fileFilterPath;
	private List<String>		filters;
	private Logger				log;

	public LineStatPreferences( Logger log )
	{
		this.log = log;
		this.fileFilterPath = new File( "" );
		this.filters = new ArrayList<String>( );
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

		// load persisted filters
		this.filters.clear( );
		Preferences filtersNode = pluginPrefRoot.node( NODE_FILTERS );
		try
		{
			String childNames[] = filtersNode.keys( );
			for ( String childName : childNames )
			{
				this.filters.add( filtersNode.get( childName, "" ) );
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