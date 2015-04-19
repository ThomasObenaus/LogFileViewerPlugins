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

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import thobe.logfileviewer.plugins.linestats.LineStatistics;
import thobe.logfileviewer.plugins.linestats.LinesInLastNMilliseconds;

/**
 * @author Thomas Obenaus
 * @source LS_TableModel.java
 * @date Apr 19, 2015
 */
@SuppressWarnings ( "serial")
public class LS_TableModel extends AbstractTableModel
{
	private static final String[]	columnNames	= new String[]
												{ "Filter", "LPS", "LPS 10s", "# 10s", "LPS 30s", "# 30s", "LPS 60s", "# 60s", "#lines" };

	private List<LineStatistics>	data;

	public LS_TableModel( )
	{
		this.data = new ArrayList<LineStatistics>( );
	}

	public void clear( )
	{
		int numEntries = this.data.size( );

		if ( numEntries > 0 )
		{
			this.data.clear( );
			this.fireTableRowsDeleted( 0, numEntries - 1 );
		}
	}

	public void removeEntries( List<LineStatistics> stats )
	{
		if ( this.data.removeAll( stats ) )
		{
			this.fireTableDataChanged( );
		}
	}

	public void addEntry( LineStatistics ls )
	{
		this.data.add( ls );
		this.fireTableRowsInserted( this.data.size( ) - 1, this.data.size( ) - 1 );
	}

	public void updateAllEntries( List<LineStatistics> entries )
	{
		this.data.clear( );
		this.data.addAll( entries );
		this.fireTableDataChanged( );
	}

	@Override
	public boolean isCellEditable( int rowIndex, int columnIndex )
	{
		return false;
	}

	@Override
	public Class<?> getColumnClass( int columnIndex )
	{
		if ( columnIndex == 0 )
			return String.class;
		if ( columnIndex == 1 )
			return Double.class;
		if ( columnIndex == 2 )
			return Double.class;
		if ( columnIndex == 3 )
			return Long.class;
		if ( columnIndex == 4 )
			return Double.class;
		if ( columnIndex == 5 )
			return Long.class;
		if ( columnIndex == 6 )
			return Double.class;
		if ( columnIndex == 7 )
			return Long.class;
		if ( columnIndex == 8 )
			return Long.class;
		return super.getColumnClass( columnIndex );
	}

	@Override
	public String getColumnName( int column )
	{
		return columnNames[column];
	}

	@Override
	public int getRowCount( )
	{
		return data.size( );
	}

	@Override
	public int getColumnCount( )
	{
		return columnNames.length;
	}

	@Override
	public Object getValueAt( int rowIndex, int columnIndex )
	{
		LineStatistics ls = this.data.get( rowIndex );
		double elapsedTimeInS = ls.getElapsedTime( ) / 1000d;
		if ( elapsedTimeInS == 0 )
		{
			elapsedTimeInS = 1d;
		}

		if ( columnIndex == 0 )
		{
			return ls.getFilter( ).toString( );
		}
		else if ( columnIndex == 1 )
		{
			return ls.getLPS( );
		}
		else if ( columnIndex == 2 )
		{
			long llIn10 = ls.getLinesInLast( LinesInLastNMilliseconds.LINES_IN_LAST_10_SECONDS );
			return ( llIn10 / elapsedTimeInS );
		}
		else if ( columnIndex == 3 )
		{
			return ls.getLinesInLast( LinesInLastNMilliseconds.LINES_IN_LAST_10_SECONDS );
		}
		else if ( columnIndex == 4 )
		{
			long llIn10 = ls.getLinesInLast( LinesInLastNMilliseconds.LINES_IN_LAST_10_SECONDS );
			return ( llIn10 / elapsedTimeInS );
		}
		else if ( columnIndex == 5 )
		{
			return ls.getLinesInLast( LinesInLastNMilliseconds.LINES_IN_LAST_30_SECONDS );
		}
		else if ( columnIndex == 6 )
		{
			long llIn10 = ls.getLinesInLast( LinesInLastNMilliseconds.LINES_IN_LAST_60_SECONDS );
			return ( llIn10 / elapsedTimeInS );
		}
		else if ( columnIndex == 7 )
		{
			return ls.getLinesInLast( LinesInLastNMilliseconds.LINES_IN_LAST_60_SECONDS );
		}
		else if ( columnIndex == 8 )
		{
			return ls.getAccumulatedLines( );
		}

		return "N/A";
	}

}
