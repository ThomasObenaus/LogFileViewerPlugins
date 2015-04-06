/*
 *  Copyright (C) 2014, Thomas Obenaus. All rights reserved.
 *  Licensed under the New BSD License (3-clause lic)
 *  See attached license-file.
 *
 *	Author: 	Thomas Obenaus
 *	EMail:		obenaus.thomas@gmail.com
 *  Project:    LogFileViewer
 */

package thobe.logfileviewer.plugins.console.icons;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import thobe.widgets.icons.IIconType;
import thobe.widgets.icons.IconContainer;
import thobe.widgets.icons.IconLib;

/**
 * @author Thomas Obenaus
 * @source C_IconLib.java
 * @date Apr 6, 2015
 */
public class C_IconLib extends IconLib
{
	private final static String	L_NAME		= "thobe.logfileviewer.plugins.console.icons";
	private static C_IconLib	instance	= null;

	@Override
	protected Map<IIconType, IconContainer> getIconContainers( )
	{
		Map<IIconType, IconContainer> icons = new HashMap<>( );

		icons.put( C_IconType.SETTINGS, new IconContainer( LOG( ), C_IconLib.class.getResource( "/thobe/logfileviewer/plugins/console/icons/" ), "settings", "png" ) );
		icons.put( C_IconType.CLEAR, new IconContainer( LOG( ), C_IconType.class.getResource( "/thobe/logfileviewer/plugins/console/icons/" ), "clear", "png" ) );
		icons.put( C_IconType.SCROLL_LOCK, new IconContainer( LOG( ), C_IconType.class.getResource( "/thobe/logfileviewer/plugins/console/icons/" ), "scroll_lock", "png" ) );
		icons.put( C_IconType.CREATE_FILTER, new IconContainer( LOG( ), C_IconType.class.getResource( "/thobe/logfileviewer/plugins/console/icons/" ), "create_filter", "png" ) );

		return icons;
	}

	public static C_IconLib get( )
	{
		if ( instance == null )
		{
			instance = new C_IconLib( );
		}
		return instance;
	}

	private C_IconLib( )
	{
		super( Logger.getLogger( L_NAME ) );
	}

}
