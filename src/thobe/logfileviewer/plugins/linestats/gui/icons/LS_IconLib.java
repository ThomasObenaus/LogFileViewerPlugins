/*
 *  Copyright (C) 2014, Thomas Obenaus. All rights reserved.
 *  Licensed under the New BSD License (3-clause lic)
 *  See attached license-file.
 *
 *	Author: 	Thomas Obenaus
 *	EMail:		obenaus.thomas@gmail.com
 *  Project:    LogFileViewer
 */

package thobe.logfileviewer.plugins.linestats.gui.icons;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import thobe.widgets.icons.IIconType;
import thobe.widgets.icons.IconContainer;
import thobe.widgets.icons.IconLib;

/**
 * @author Thomas Obenaus
 * @source LFV_IconLib.java
 * @date Apr 6, 2015
 */
public class LS_IconLib extends IconLib
{
	private final static String	L_NAME		= "thobe.logfileviewer.plugins.linestats.gui.icons";
	private static LS_IconLib	instance	= null;

	@Override
	protected Map<IIconType, IconContainer> getIconContainers( )
	{
		Map<IIconType, IconContainer> icons = new HashMap<>( );

		icons.put( LS_IconType.ADD, new IconContainer( LOG( ), LS_IconLib.class.getResource( "/thobe/logfileviewer/plugins/linestats/gui/icons/" ), "add", "png" ) );
		icons.put( LS_IconType.ADD_FROM_FILE, new IconContainer( LOG( ), LS_IconLib.class.getResource( "/thobe/logfileviewer/plugins/linestats/gui/icons/" ), "add_from_file", "png" ) );
		icons.put( LS_IconType.REMOVE_SELECTED_FILTERS, new IconContainer( LOG( ), LS_IconLib.class.getResource( "/thobe/logfileviewer/plugins/linestats/gui/icons/" ), "remove_selected_filters", "png" ) );
		icons.put( LS_IconType.CLOCK, new IconContainer( LOG( ), LS_IconLib.class.getResource( "/thobe/logfileviewer/plugins/linestats/gui/icons/" ), "clock", "png" ) );
		return icons;
	}

	public static LS_IconLib get( )
	{
		if ( instance == null )
		{
			instance = new LS_IconLib( );
		}
		return instance;
	}

	private LS_IconLib( )
	{
		super( Logger.getLogger( L_NAME ) );
	}

}
