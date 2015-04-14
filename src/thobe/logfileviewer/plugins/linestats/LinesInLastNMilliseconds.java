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

/**
 * @author Thomas Obenaus
 * @source LinesInLastNMilliseconds.java
 * @date Apr 13, 2015
 */
public enum LinesInLastNMilliseconds
{
	LINES_IN_LAST_SECOND( 1000 ), LINES_IN_LAST_10_SECONDS( 10000 ), LINES_IN_LAST_MINUTE( 60000 );

	private long	milliseconds;

	private LinesInLastNMilliseconds( long milliseconds )
	{
		this.milliseconds = milliseconds;
	}

	public long getMilliseconds( )
	{
		return milliseconds;
	}
}
