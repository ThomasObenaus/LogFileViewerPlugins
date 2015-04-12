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

import java.util.List;

import thobe.logfileviewer.plugin.Plugin;
import thobe.logfileviewer.plugin.source.logline.ILogLine;

/**
 * @author Thomas Obenaus
 * @source LineStats.java
 * @date Apr 13, 2015
 */
public class LineStats extends Plugin
{
	private static final String	L_NAME			= "thobe.logfileviewer.plugins.linestats";
	private static final int	MAJOR_VERSION	= 0;
	private static final int	MINOR_VERSION	= 1;
	private static final int	BUGFIX_VERSION	= 0;

	public LineStats( )
	{
		super( "LineStats", L_NAME );
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
		return 0;
	}

	@Override
	public void freeMemory( )
	{}

	@Override
	public void onNewBlockOfLines( List<ILogLine> blockOfLines )
	{
		System.out.println( "LineStats.onNewBlockOfLines(" + blockOfLines.size( ) + ")" );
	}

}
