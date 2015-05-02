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
 * @source ClockDetectedException.java
 * @date Apr 27, 2015
 */
@SuppressWarnings ( "serial")
public class ClockDetectedException extends Exception
{
	private String	fullCause;
	private String	nameOfClock;

	public ClockDetectedException( String cause, String fullCause, String nameOfClock )
	{
		super( cause );
		this.fullCause = fullCause;
		this.nameOfClock = nameOfClock;
	}

	public String getFullCause( )
	{
		return fullCause;
	}

	public String getNameOfClock( )
	{
		return nameOfClock;
	}
}
