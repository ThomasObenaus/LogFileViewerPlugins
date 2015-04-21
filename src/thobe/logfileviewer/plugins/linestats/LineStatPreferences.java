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
import java.util.prefs.Preferences;

import thobe.logfileviewer.plugin.api.IPluginPreferences;

/**
 * @author Thomas Obenaus
 * @source LineStatPreferences.java
 * @date Apr 21, 2015
 */
public class LineStatPreferences implements IPluginPreferences
{
	private static final String	PRP_FILTER_FILE_PATH	= "filterFilePath";

	private File				fileFilterPath;

	public LineStatPreferences( )
	{
		this.fileFilterPath = new File( "" );
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
	}

	@Override
	public void save( Preferences pluginPrefRoot )
	{
		pluginPrefRoot.put( PRP_FILTER_FILE_PATH, this.fileFilterPath.getAbsolutePath( ) );
	}

}
