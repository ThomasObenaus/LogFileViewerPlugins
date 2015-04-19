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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import thobe.logfileviewer.plugin.api.IPluginUIComponent;
import thobe.logfileviewer.plugins.linestats.LineStatistics;
import thobe.logfileviewer.plugins.linestats.LineStatsPlugin;
import thobe.widgets.textfield.RestrictedTextFieldAdapter;
import thobe.widgets.textfield.RestrictedTextFieldRegexp;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Thomas Obenaus
 * @source LineStatsPanel.java
 * @date Apr 19, 2015
 */
@SuppressWarnings ( "serial")
public class LineStatsPanel extends JPanel implements IPluginUIComponent
{
	private LineStatsPlugin					lineStats;

	private RestrictedTextFieldRegexp		rtf_filter;
	private JButton							bu_addFilter;
	private JButton							bu_removeSelectedFilters;

	private TableViewPanel					pa_tableView;

	private JButton							bu_start;
	private JButton							bu_stop;
	private JButton							bu_startInterval;

	private Timer							statsUpdateTimer;

	private long							updateInterval;

	private List<ILineStatsPanelListener>	listeners;

	public LineStatsPanel( LineStatsPlugin lineStats )
	{
		this.updateInterval = 2000;
		this.lineStats = lineStats;
		this.listeners = new ArrayList<>( );
		this.buildGUI( );

		this.statsUpdateTimer = new Timer( "LineStatsUpdateTimer" );
		this.statsUpdateTimer.schedule( new UpdateTask( ), updateInterval, updateInterval );
	}

	public void addListener( ILineStatsPanelListener l )
	{
		this.listeners.add( l );
	}

	private void buildGUI( )
	{
		this.setLayout( new BorderLayout( ) );

		// Add filter panel
		FormLayout fla_filter = new FormLayout( "3dlu,50dlu,3dlu,fill:default:grow,3dlu,40dlu,3dlu,default,3dlu", "3dlu,default,3dlu" );
		CellConstraints cc_filter = new CellConstraints( );
		JPanel pa_filter = new JPanel( fla_filter );
		this.add( pa_filter, BorderLayout.NORTH );

		String tt_filter = "<html><h4>Define a filter for loglines that should be counted:</h4>";
		tt_filter += "</br>";
		tt_filter += "<ul>";
		tt_filter += "<li>. - any character</li>";
		tt_filter += "<li>.* - any character multiple times</li>";
		tt_filter += "<li>| - this is a OR. E.g. 'Info|Debug' will match all lines containing 'Info' OR 'Debug'</li>";
		tt_filter += "<li>^ - start of line</li>";
		tt_filter += "<li>$ - end of line</li>";
		tt_filter += "<li>[0-9] - any number between 0 and 9</li>";
		tt_filter += "<li>[0-9]* - any number between 0 and 9 multiple times</li>";
		tt_filter += "<li>[0-9]{3,} - any number between 0 and 9 at min 3 times in a row</li>";
		tt_filter += "<li>[0-9]{,3} - any number between 0 and 9 at max 3 times in a row</li>";
		tt_filter += "<li>\\[ - [ ... (since [ and ] are control characters they have to be escaped)</li>";
		tt_filter += "<li>\\. - . ... (since . is a control character it has to be escaped)</li>";
		tt_filter += "</ul>";
		tt_filter += "</html>";
		JLabel l_filter = new JLabel( "Filter" );
		pa_filter.add( l_filter, cc_filter.xy( 2, 2 ) );
		l_filter.setToolTipText( tt_filter );
		this.rtf_filter = new RestrictedTextFieldRegexp( 20 );
		pa_filter.add( this.rtf_filter, cc_filter.xy( 4, 2 ) );
		this.rtf_filter.setToolTipText( tt_filter );
		this.rtf_filter.addListener( new RestrictedTextFieldAdapter( )
		{
			@Override
			public void valueChanged( )
			{
				bu_addFilter.setEnabled( !rtf_filter.getValue( ).isEmpty( ) );
			}
		} );
		this.bu_addFilter = new JButton( "+" );
		pa_filter.add( this.bu_addFilter, cc_filter.xy( 6, 2 ) );
		this.bu_addFilter.setToolTipText( "Add the given filter" );
		this.bu_addFilter.addActionListener( new ActionListener( )
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				addNewFilter( );
			}
		} );
		this.bu_addFilter.setEnabled( false );
		this.bu_removeSelectedFilters = new JButton( "Remove Selected Filter" );
		pa_filter.add( this.bu_removeSelectedFilters, cc_filter.xy( 8, 2 ) );
		this.bu_removeSelectedFilters.setToolTipText( "Removes the selected filters from the statistics" );
		this.bu_removeSelectedFilters.addActionListener( new ActionListener( )
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				removeSelectedFilters( );
			}

		} );

		// main view
		JTabbedPane tab_main = new JTabbedPane( JTabbedPane.BOTTOM );
		this.add( tab_main, BorderLayout.CENTER );
		this.pa_tableView = new TableViewPanel( );
		tab_main.addTab( "TableView", this.pa_tableView );
		this.addListener( pa_tableView );

		// buttons
		JPanel pa_buttons = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
		this.add( pa_buttons, BorderLayout.SOUTH );
		this.bu_start = new JButton( "start" );
		pa_buttons.add( this.bu_start );
		this.bu_start.addActionListener( new ActionListener( )
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				startStatTracing( false );
			}
		} );
		this.bu_stop = new JButton( "stop" );
		pa_buttons.add( this.bu_stop );
		this.bu_stop.addActionListener( new ActionListener( )
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				stopStatTracing( );
			}
		} );

		this.bu_startInterval = new JButton( "start I" );
		pa_buttons.add( this.bu_startInterval );
		this.bu_startInterval.addActionListener( new ActionListener( )
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				startStatTracing( true );
			}
		} );

	}

	private void startStatTracing( boolean startWithInterval )
	{
		if ( startWithInterval )
		{

		}

		this.lineStats.startStatTracing( );
	}

	private void stopStatTracing( )
	{
		this.lineStats.stopTracing( );
	}

	private void removeSelectedFilters( )
	{
		// TODO Auto-generated method stub

	}

	private void addNewFilter( )
	{
		String patStr = this.rtf_filter.getValue( );

		if ( ( patStr != null ) && ( !patStr.isEmpty( ) ) )
		{
			if ( !patStr.endsWith( ".*" ) )
			{
				patStr += ".*";
			}
			if ( !patStr.startsWith( ".*" ) )
			{
				patStr = ".*" + patStr;
			}
		}

		Pattern filter = Pattern.compile( patStr );
		LineStatistics added = this.lineStats.addFilter( filter );
		this.fireStatAdded( added );
	}

	@Override
	public JComponent getVisualComponent( )
	{
		return this;
	}

	@Override
	public void onClosing( )
	{}

	@Override
	public void onClosed( )
	{}

	@Override
	public String getTitle( )
	{
		return "LineStats";
	}

	@Override
	public String getTooltip( )
	{
		return "Statistics about the frequency of LogLines";
	}

	@Override
	public boolean isCloseable( )
	{
		return false;
	}

	private void fireStatAdded( LineStatistics lStat )
	{
		for ( ILineStatsPanelListener l : this.listeners )
		{
			l.onStatAdd( lStat );
		}
	}

	private void fireStatsRemoved( List<LineStatistics> lStats )
	{
		for ( ILineStatsPanelListener l : this.listeners )
		{
			l.onStatsRemoved( lStats );
		}
	}

	private void fireUpdateView( List<LineStatistics> lStats )
	{
		for ( ILineStatsPanelListener l : this.listeners )
		{
			l.onUpdateView( lStats );
		}
	}

	private class UpdateTask extends TimerTask
	{

		@Override
		public void run( )
		{
			List<LineStatistics> stats = lineStats.getLineStats( );
			fireUpdateView( stats );
		}

	}

}
