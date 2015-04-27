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
 * @source ClockDetectedException.java
 * @date Apr 27, 2015
 */
@SuppressWarnings ( "serial")
public class ClockDetectedException extends Exception
{
	public ClockDetectedException( String cause )
	{
		super( cause );
	}
}
