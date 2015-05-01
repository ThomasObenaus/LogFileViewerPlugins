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
												{ "Filter", "LPS", "Max LPS", "Min LPS >0", "LPS 10s", "# 10s", "LPS 30s", "# 30s", "LPS 60s", "# 60s", "#lines" };

	private List<LineStatistics>	data;

	public LS_TableModel( )
	{
		this.data = new ArrayList<LineStatistics>( );
	}

	public void clear( )
	{
		int numEntries = 0;
		synchronized ( this.data )
		{
			numEntries = this.data.size( );

			if ( numEntries > 0 )
			{
				this.data.clear( );

			}
		}

		this.fireTableRowsDeleted( 0, numEntries - 1 );
	}

	public void removeEntries( List<LineStatistics> stats )
	{
		boolean sthRemoved = false;
		synchronized ( this.data )
		{
			sthRemoved = this.data.removeAll( stats );
		}

		if ( sthRemoved )
		{
			this.fireTableDataChanged( );
		}
	}

	public void addEntry( LineStatistics ls )
	{
		int newSize = 0;
		boolean entryAdded = false;
		synchronized ( this.data )
		{
			if ( !this.data.contains( ls ) )
			{
				this.data.add( ls );
				entryAdded = true;
			}

			newSize = this.data.size( );
		}

		if ( entryAdded && ( newSize > 0 ) )
		{
			this.fireTableRowsInserted( newSize - 1, newSize - 1 );
		}
	}

	public void addEntries( List<LineStatistics> ls )
	{
		int numEntries = this.data.size( );
		int newSize = 0;
		boolean sthAdded = false;
		synchronized ( this.data )
		{
			for ( LineStatistics l : ls )
			{
				if ( !this.data.contains( l ) )
				{
					this.data.add( l );
					sthAdded = true;
				}
			}

			newSize = this.data.size( );
		}
		if ( sthAdded && ( newSize > 0 ) )
		{
			this.fireTableRowsInserted( numEntries, newSize - 1 );
		}
	}

	public List<LineStatistics> getStatsAt( int rows[] )
	{
		List<LineStatistics> result = new ArrayList<LineStatistics>( );

		synchronized ( this.data )
		{
			for ( int r : rows )
			{
				result.add( this.data.get( r ) );
			}
		}

		return result;
	}

	public void updateAllEntries( List<LineStatistics> entries )
	{

		int currentNumberOfRows = 0;
		int newNumberOfRows = 0;
		int newSize = 0;
		synchronized ( this.data )
		{
			currentNumberOfRows = this.getRowCount( );
			newNumberOfRows = entries.size( );

			this.data.clear( );
			this.data.addAll( entries );
			newSize = this.data.size( );
		}

		if ( currentNumberOfRows != newNumberOfRows )
		{
			this.fireTableDataChanged( );
		}
		else
		{
			if ( newSize > 0 )
			{
				this.fireTableRowsUpdated( 0, newSize - 1 );
			}
		}
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
			return Double.class;
		if ( columnIndex == 4 )
			return Double.class;
		if ( columnIndex == 5 )
			return Long.class;
		if ( columnIndex == 6 )
			return Double.class;
		if ( columnIndex == 7 )
			return Long.class;
		if ( columnIndex == 8 )
			return Double.class;
		if ( columnIndex == 9 )
			return Long.class;
		if ( columnIndex == 10 )
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
			return ls.getFilterName( );
		}
		else if ( columnIndex == 1 )
		{
			return ls.getLPS( );
		}
		else if ( columnIndex == 2 )
		{
			return ls.getPeakLPS( );
		}
		else if ( columnIndex == 3 )
		{
			return ls.getLowLPS( );
		}
		else if ( columnIndex == 4 )
		{
			return ls.getLPSInLast( LinesInLastNMilliseconds.LINES_IN_LAST_10_SECONDS );
		}
		else if ( columnIndex == 5 )
		{
			return ls.getLinesInLast( LinesInLastNMilliseconds.LINES_IN_LAST_10_SECONDS );
		}
		else if ( columnIndex == 6 )
		{
			return ls.getLPSInLast( LinesInLastNMilliseconds.LINES_IN_LAST_10_SECONDS );
		}
		else if ( columnIndex == 7 )
		{
			return ls.getLinesInLast( LinesInLastNMilliseconds.LINES_IN_LAST_30_SECONDS );
		}
		else if ( columnIndex == 8 )
		{
			return ls.getLPSInLast( LinesInLastNMilliseconds.LINES_IN_LAST_60_SECONDS );
		}
		else if ( columnIndex == 9 )
		{
			return ls.getLinesInLast( LinesInLastNMilliseconds.LINES_IN_LAST_60_SECONDS );
		}
		else if ( columnIndex == 10 )
		{
			return ls.getAccumulatedLines( );
		}

		return "N/A";
	}

}
