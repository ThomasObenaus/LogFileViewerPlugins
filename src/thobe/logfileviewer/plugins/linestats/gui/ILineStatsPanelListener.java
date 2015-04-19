/*
 *  Copyright (C) 2014, Thomas Obenaus. All rights reserved.
 *  Licensed under the New BSD License (3-clause lic)
 *  See attached license-file.
 *
 *	Author: 	Thomas Obenaus
 *	EMail:		obenaus.thomas@gmail.com
 *  Project:    LogFileViewer
 */

package thobe.logfileviewer.plugins.linestats.gui;

import java.util.List;

import thobe.logfileviewer.plugins.linestats.LineStatistics;

/**
 * @author Thomas Obenaus
 * @source ILineStatsPanelListener.java
 * @date Apr 19, 2015
 */
public interface ILineStatsPanelListener
{
	public void onUpdateView( List<LineStatistics> stats );

	public void onStatAdd( LineStatistics stat );

	public void onStatsRemoved( List<LineStatistics> stats );
}
