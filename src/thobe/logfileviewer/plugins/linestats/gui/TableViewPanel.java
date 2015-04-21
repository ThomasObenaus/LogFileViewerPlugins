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

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableRowSorter;

import thobe.logfileviewer.plugins.linestats.LineStatistics;

/**
 * @author Thomas Obenaus
 * @source TableViewPanel.java
 * @date Apr 19, 2015
 */
@SuppressWarnings ( "serial")
public class TableViewPanel extends JPanel implements ILineStatsPanelListener
{
	private LS_TableModel	model;
	private JTable			ta_stats;

	public TableViewPanel( )
	{
		this.buildGUI( );
	}

	private void buildGUI( )
	{
		this.setLayout( new BorderLayout( ) );
		this.model = new LS_TableModel( );

		this.ta_stats = new JTable( this.model );
		JScrollPane scrpa_table = new JScrollPane( this.ta_stats );
		this.add( scrpa_table, BorderLayout.CENTER );
		this.add( this.ta_stats.getTableHeader( ), BorderLayout.NORTH );

		TableRowSorter<LS_TableModel> sorter = new TableRowSorter<LS_TableModel>( this.model );
		List<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>( );
		sortKeys.add( new RowSorter.SortKey( 1, SortOrder.DESCENDING ) );
		sorter.setSortKeys( sortKeys );
		sorter.setSortsOnUpdates( true );
		this.ta_stats.setRowSorter( sorter );
	}

	public List<LineStatistics> getSelectedStats( )
	{
		int selectedRowsInView[] = this.ta_stats.getSelectedRows( );

		int selectedRowsInModel[] = new int[selectedRowsInView.length];
		for ( int i = 0; i < selectedRowsInView.length; ++i )
		{
			selectedRowsInModel[i] = this.ta_stats.convertRowIndexToModel( selectedRowsInView[i] );
		}

		return this.model.getStatsAt( selectedRowsInModel );
	}

	@Override
	public void onStatsAdd( List<LineStatistics> stats )
	{
		this.model.addEntries( stats );
	}

	@Override
	public void onUpdateView( List<LineStatistics> stats )
	{
		this.model.updateAllEntries( stats );
	}

	@Override
	public void onStatAdd( LineStatistics stat )
	{
		this.model.addEntry( stat );
	}

	@Override
	public void onStatsRemoved( List<LineStatistics> stats )
	{
		this.model.removeEntries( stats );
	}
}
