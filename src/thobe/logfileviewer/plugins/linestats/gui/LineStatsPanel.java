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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import thobe.logfileviewer.plugin.api.IPluginUIComponent;
import thobe.logfileviewer.plugins.linestats.ILineStatsPluginListener;
import thobe.logfileviewer.plugins.linestats.LineStatistics;
import thobe.logfileviewer.plugins.linestats.LineStatsPlugin;
import thobe.logfileviewer.plugins.linestats.gui.icons.LS_IconLib;
import thobe.logfileviewer.plugins.linestats.gui.icons.LS_IconType;
import thobe.widgets.icons.IconSize;
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
public class LineStatsPanel extends JPanel implements IPluginUIComponent, ILineStatsPluginListener
{
	private LineStatsPlugin					lineStats;

	private RestrictedTextFieldRegexp		rtf_filter;
	private JButton							bu_addFilter;
	private JButton							bu_removeSelectedFilters;
	private JButton							bu_addFiltersFromFile;

	private TableViewPanel					pa_tableView;

	private JButton							bu_start;
	private JButton							bu_stop;
	private JButton							bu_startInterval;

	private UpdateTask						updateTask;

	private List<ILineStatsPanelListener>	listeners;

	private Logger							log;

	public LineStatsPanel( Logger log, LineStatsPlugin lineStats )
	{
		this.log = log;
		this.lineStats = lineStats;
		this.listeners = new ArrayList<>( );
		this.buildGUI( );

		this.updateTask = new UpdateTask( 2000 );
		this.updateTask.start( );
	}

	public void addListener( ILineStatsPanelListener l )
	{
		this.listeners.add( l );
	}

	private void buildGUI( )
	{
		this.setLayout( new BorderLayout( ) );

		// Add filter panel
		FormLayout fla_filter = new FormLayout( "3dlu,50dlu,3dlu,fill:default:grow,3dlu,default,3dlu,default,3dlu,default,3dlu", "3dlu,default,3dlu" );
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

			public void valueChangeCommitted( )
			{
				addNewFilter( );
			};
		} );
		this.bu_addFilter = new JButton( LS_IconLib.get( ).getIcon( LS_IconType.ADD, true, IconSize.S16x16 ) );
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

		this.bu_addFiltersFromFile = new JButton( LS_IconLib.get( ).getIcon( LS_IconType.ADD_FROM_FILE, true, IconSize.S16x16 ) );
		pa_filter.add( this.bu_addFiltersFromFile, cc_filter.xy( 8, 2 ) );
		this.bu_addFiltersFromFile.setToolTipText( "Add a list of Filters from file." );
		this.bu_addFiltersFromFile.addActionListener( new ActionListener( )
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				addFiltersFromFile( );
			}

		} );
		this.bu_removeSelectedFilters = new JButton( LS_IconLib.get( ).getIcon( LS_IconType.REMOVE_SELECTED_FILTERS, true, IconSize.S16x16 ) );
		pa_filter.add( this.bu_removeSelectedFilters, cc_filter.xy( 10, 2 ) );
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

	private void addFiltersFromFile( )
	{
		JFileChooser fc = new JFileChooser( this.lineStats.getPrefs( ).getFileFilterPath( ) );
		fc.setFileSelectionMode( JFileChooser.FILES_ONLY );
		if ( fc.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION )
		{
			File f = fc.getSelectedFile( );

			this.lineStats.getPrefs( ).setFileFilterPath( f.getParentFile( ) );

			LOG( ).info( "Reading filters from '" + f.getAbsolutePath( ) + "'" );
			List<Pattern> patternsFromFile = new ArrayList<Pattern>( );
			try
			{
				BufferedReader br = new BufferedReader( new FileReader( f ) );

				String line = null;
				while ( ( line = br.readLine( ) ) != null )
				{
					line = line.trim( );
					if ( !line.isEmpty( ) )
					{
						String patStr = addLeadingAndTralingAllPattern( line );
						try
						{
							Pattern filter = Pattern.compile( patStr );
							patternsFromFile.add( filter );
							LOG( ).info( "Filter '" + filter.toString( ) + "' read from file." );
						}
						catch ( PatternSyntaxException e )
						{
							LOG( ).warning( "Ignore filter '" + patStr + "': " + e.getLocalizedMessage( ) );
						}
					}
				}// while ( ( line = br.readLine( ) ) != null )
				br.close( );
			}
			catch ( IOException e )
			{
				LOG( ).warning( "Problem on processing selected file '" + f.getAbsolutePath( ) + "': " + e.getLocalizedMessage( ) );
			}

			this.addFilters( patternsFromFile );

		}// if ( fc.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION )
	}

	public void addFilters( List<Pattern> filters )
	{
		List<LineStatistics> added = this.lineStats.addFilters( filters );
		this.fireStatsAdded( added );
	}

	public void quit( )
	{
		if ( this.updateTask != null )
		{
			this.updateTask.quit( );
		}
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
		List<LineStatistics> selected = this.pa_tableView.getSelectedStats( );
		this.lineStats.removeFilters( selected );
		this.fireStatsRemoved( selected );
	}

	private void addNewFilter( )
	{
		String patStr = addLeadingAndTralingAllPattern( this.rtf_filter.getValue( ) );

		Pattern filter = Pattern.compile( patStr );
		LineStatistics added = this.lineStats.addFilter( filter );
		if ( added != null )
		{
			this.fireStatAdded( added );
		}
	}

	private static String addLeadingAndTralingAllPattern( String patStr )
	{
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
		return patStr;
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

	private void fireStatsAdded( List<LineStatistics> lStat )
	{
		for ( ILineStatsPanelListener l : this.listeners )
		{
			l.onStatsAdd( lStat );
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

	private class UpdateTask extends Thread
	{
		private AtomicBoolean	cancelRequested;
		private int				interval;
		private Semaphore		eventSemaphore;

		private AtomicBoolean	fireEnabled;

		public UpdateTask( int interval )
		{
			super( "LineStatsUpdateTimer" );
			this.interval = interval;
			this.cancelRequested = new AtomicBoolean( false );
			this.eventSemaphore = new Semaphore( 0, true );
			this.fireEnabled = new AtomicBoolean( false );
		}

		public void setFireEnabled( boolean fireEnabled )
		{
			this.fireEnabled.set( fireEnabled );
			this.eventSemaphore.release( );
		}

		public void quit( )
		{
			this.cancelRequested.set( true );
			this.eventSemaphore.release( );
		}

		@Override
		public void run( )
		{
			while ( !this.cancelRequested.get( ) )
			{
				if ( this.fireEnabled.get( ) )
				{
					List<LineStatistics> stats = lineStats.getLineStats( );
					fireUpdateView( stats );
				}

				try
				{
					this.eventSemaphore.tryAcquire( this.interval, TimeUnit.MILLISECONDS );
				}
				catch ( InterruptedException e )
				{
					break;
				}

			}
		}

	}

	protected Logger LOG( )
	{
		return this.log;
	}

	@Override
	public void onStartTracing( )
	{
		this.updateTask.setFireEnabled( true );
	}

	@Override
	public void onStopTracing( )
	{
		this.updateTask.setFireEnabled( false );
	}

}
