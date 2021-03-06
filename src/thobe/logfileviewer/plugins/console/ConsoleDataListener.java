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

import java.util.List;

import thobe.logfileviewer.plugin.source.logline.ILogLine;

/**
 * @author Thomas Obenaus
 * @source ConsoleDataListener.java
 * @date Jul 27, 2014
 */
public interface ConsoleDataListener
{
	public void onNewData( List<ILogLine> blockOfLines );

	public void freeMemory( );

	public long getCurrentMemory( );
}
