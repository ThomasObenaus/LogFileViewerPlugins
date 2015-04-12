package thobe.logfileviewer.plugins.examples.llcounter;

import java.util.List;

import thobe.logfileviewer.plugin.Plugin;
import thobe.logfileviewer.plugin.source.logline.ILogLine;

/*
 *  Copyright (C) 2014, Thomas Obenaus. All rights reserved.
 *  Licensed under the New BSD License (3-clause lic)
 *  See attached license-file.
 *
 *	Author: 	Thomas Obenaus
 *	EMail:		obenaus.thomas@gmail.com
 *  Project:    MyPlugin
 */

/**
 * @author Thomas Obenaus
 * @source LogLineCounter.java
 * @date Apr 12, 2015
 */
public class LogLineCounter extends Plugin
{
	private long	numLogLines;

	/* Constructor */
	public LogLineCounter( )
	{ // LogLineCounter - the name of the plugin and the corresponding thread
		// LogLineCounter - the name of the log-channel for the plugin 
		super( "LogLineCounter", "LogLineCounter.logging" );
	}

	/* Common information methods */
	@Override
	public int getMajorVersion( )
	{
		return 0;
	}

	@Override
	public int getMinorVersion( )
	{
		return 1;
	}

	@Override
	public int getBugfixVersion( )
	{
		return 0;
	}

	@Override
	public long getMemory( )
	{
		// nothing to do since we don't consume mem
		return 0;
	}

	@Override
	public void freeMemory( )
	{
		// nothing to do since we don't consume mem
	}

	/* Methods for obtaining the log-lines*/
	@Override
	public void onNewBlockOfLines( List<ILogLine> blockOfLines )
	{
		// here we can count and print out the number of lines
		this.numLogLines += blockOfLines.size( );
		System.out.println( "#LLs=" + this.numLogLines );
	}

	/* Plugin life-cycle methods*/
	@Override
	public void onLogStreamOpened( )
	{
		// called whenever a new logstream (file or over ip) is opened
		this.numLogLines = 0;
	}
}
