/*
 *  Copyright (C) 2015, Thomas Obenaus. All rights reserved.
 *  Licensed under the New BSD License (3-clause lic)
 *  See attached license-file.
 *
 *	Author: 	Thomas Obenaus
 *	EMail:		obenaus.thomas@gmail.com
 *  Project:    LineStats
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

	public void onStatsAdd( List<LineStatistics> stats );

	public void onStatsRemoved( List<LineStatistics> stats );
}
