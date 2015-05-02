/*
 *  Copyright (C) 2015, Thomas Obenaus. All rights reserved.
 *  Licensed under the New BSD License (3-clause lic)
 *  See attached license-file.
 *
 *	Author: 	Thomas Obenaus
 *	EMail:		obenaus.thomas@gmail.com
 *  Project:    LineStats
 */

package thobe.logfileviewer.plugins.linestats;

/**
 * @author Thomas Obenaus
 * @source TimeRange.java
 * @date Apr 27, 2015
 */
public class TimeRange
{
	private long	start;
	private long	end;

	public TimeRange( long start, long end )
	{
		this.start = start;
		this.end = end;
	}

	public long getEnd( )
	{
		return end;
	}

	public long getStart( )
	{
		return start;
	}

	public long getElapsed( )
	{
		return this.end - this.start;
	}
}
