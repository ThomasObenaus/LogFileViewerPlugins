/*
 *  Copyright (C) 2014, Thomas Obenaus. All rights reserved.
 *  Licensed under the New BSD License (3-clause lic)
 *  See attached license-file.
 *
 *	Author: 	Thomas Obenaus
 *	EMail:		obenaus.thomas@gmail.com
 *  Project:    LogFileViewer
 */

package thobe.logfileviewer.plugins.perfMon;

import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import thobe.logfileviewer.plugin.Plugin;
import thobe.logfileviewer.plugin.api.IPluginAccess;
import thobe.logfileviewer.plugin.api.IPluginUIComponent;
import thobe.logfileviewer.plugin.source.logline.ILogLine;

/**
 * @author Thomas Obenaus
 * @source PerformanceMonitor.java
 * @date Jul 7, 2014
 */
public class PerformanceMonitor extends Plugin
{
	public static final String	FULL_PLUGIN_NAME	= "thobe.logfileviewer.performance.Performance";

	private Logger				log;

	public PerformanceMonitor( )
	{
		super( FULL_PLUGIN_NAME, FULL_PLUGIN_NAME );
		this.log = Logger.getLogger( FULL_PLUGIN_NAME );
	}

	@Override
	public IPluginUIComponent getUIComponent( )
	{
		// TODO Auto-generated method stub
		return null;//new JButton( "Performance" );
	}

	@Override
	public boolean onStarted( )
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onRegistered( IPluginAccess pluginAccess )
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLogStreamOpened( )
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onPrepareCloseLogStream( )
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onLogStreamClosed( )
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onUnRegistered( )
	{
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onStopped( )
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getPluginDescription( )
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void freeMemory( )
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onNewBlockOfLines( List<ILogLine> blockOfLines )
	{
		// TODO Auto-generated method stub

	}

	@Override
	public Pattern getLineFilter( )
	{
		return Pattern.compile( ".*" );
	}

	protected Logger LOG( )
	{
		return this.log;
	}

	@Override
	public long getMemory( )
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getNameOfMemoryWatchable( )
	{
		return FULL_PLUGIN_NAME;
	}

	@Override
	public int getMajorVersion( )
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMinorVersion( )
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getBugfixVersion( )
	{
		// TODO Auto-generated method stub
		return 0;
	}

}
