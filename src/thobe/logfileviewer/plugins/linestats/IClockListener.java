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
 * @source IClockListener.java
 * @date Apr 27, 2015
 */
public interface IClockListener
{
	public void onTimeUpdated( long currentTime,long elapsedTime );

	public void onError( String err );
}
