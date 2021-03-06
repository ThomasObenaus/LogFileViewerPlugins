/*
 *  Copyright (C) 2015, Thomas Obenaus. All rights reserved.
 *  Licensed under the New BSD License (3-clause lic)
 *  See attached license-file.
 *
 *	Author: 	Thomas Obenaus
 *	EMail:		obenaus.thomas@gmail.com
 *  Project:    LineStats
 */

package thobe.logfileviewer.plugins.linestats.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
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
import javax.swing.JToggleButton;
import javax.swing.filechooser.FileNameExtensionFilter;

import thobe.logfileviewer.plugin.api.IPluginUIComponent;
import thobe.logfileviewer.plugins.linestats.IClockListener;
import thobe.logfileviewer.plugins.linestats.ILineStatsPluginListener;
import thobe.logfileviewer.plugins.linestats.LineStatistics;
import thobe.logfileviewer.plugins.linestats.LineStatsPlugin;
import thobe.logfileviewer.plugins.linestats.gui.icons.LS_IconLib;
import thobe.logfileviewer.plugins.linestats.gui.icons.LS_IconType;
import thobe.widgets.icons.IconSize;
import thobe.widgets.textfield.RestrictedTextFieldAdapter;
import thobe.widgets.textfield.RestrictedTextFieldRegexp;
import thobe.widgets.textfield.RestrictedTextfieldListener;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Thomas Obenaus
 * @source LineStatsPanel.java
 * @date Apr 19, 2015
 */
@SuppressWarnings ( "serial")
public class LineStatsPanel extends JPanel implements IPluginUIComponent, ILineStatsPluginListener, IClockListener
{
	private static final String				STATS_FILE_EXTENSION	= "csv";
	private static final Pattern			ALL_FILTER				= Pattern.compile( ".*" );
	private final static String				CLOCK_PREFIX			= "Elapsed: ";
	private LineStatsPlugin					lineStats;

	private RestrictedTextFieldRegexp		rtf_filter;

	private RestrictedTextFieldRegexp		rtf_clockFilter;

	private JButton							bu_addFilter;
	private JButton							bu_removeSelectedFilters;
	private JButton							bu_addFiltersFromFile;
	private JButton							bu_clockFilter;
	private JButton							bu_exportFilters;
	private JButton							bu_exportStats;

	private TableViewPanel					pa_tableView;

	private JToggleButton					bu_startStop;

	private UpdateTask						updateTask;

	private List<ILineStatsPanelListener>	listeners;

	private Logger							log;
	private JLabel							l_clock;
	private JLabel							l_clockErrCount;
	private List<String>					clockErrors;
	private RestrictedTextfieldListener		rtf_listener;

	public LineStatsPanel( Logger log, LineStatsPlugin lineStats )
	{
		this.clockErrors = new ArrayList<String>( );
		this.log = log;
		this.lineStats = lineStats;
		this.listeners = new ArrayList<>( );
		this.buildGUI( );

		this.updateTask = new UpdateTask( );
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
		FormLayout fla_filter = new FormLayout( "3dlu,50dlu,3dlu,fill:default:grow,3dlu,default,3dlu,default,3dlu,default,3dlu,default,3dlu", "3dlu,default,3dlu" );
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
		this.bu_exportFilters = new JButton( LS_IconLib.get( ).getIcon( LS_IconType.EXPORT_FILTERS_TO_FILE, true, IconSize.S16x16 ) );
		pa_filter.add( this.bu_exportFilters, cc_filter.xy( 12, 2 ) );
		this.bu_exportFilters.setToolTipText( "Export filters to file" );
		this.bu_exportFilters.addActionListener( new ActionListener( )
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				exportFiltersToFile( );
			}
		} );

		// main view
		JTabbedPane tab_main = new JTabbedPane( JTabbedPane.BOTTOM );
		this.add( tab_main, BorderLayout.CENTER );
		this.pa_tableView = new TableViewPanel( );
		tab_main.addTab( "TableView", this.pa_tableView );
		this.addListener( pa_tableView );

		// buttons
		FormLayout fla_buttons = new FormLayout( "3dlu,default,3dlu,fill:default,3dlu,80dlu,3dlu,fill:default:grow,3dlu,default,3dlu,default,3dlu", "3dlu,30dlu,3dlu" );
		CellConstraints cc_buttons = new CellConstraints( );
		final JPanel pa_buttons = new JPanel( fla_buttons );
		this.add( pa_buttons, BorderLayout.SOUTH );

		this.bu_clockFilter = new JButton( LS_IconLib.get( ).getIcon( LS_IconType.CLOCK, true, IconSize.S16x16 ) );
		pa_buttons.add( bu_clockFilter, cc_buttons.xy( 2, 2 ) );
		bu_clockFilter.addActionListener( new ActionListener( )
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				rtf_clockFilter.setVisible( !rtf_clockFilter.isVisible( ) );
				pa_buttons.repaint( );
				pa_buttons.revalidate( );
			}
		} );

		this.rtf_clockFilter = new RestrictedTextFieldRegexp( 10 );
		pa_buttons.add( this.rtf_clockFilter, cc_buttons.xy( 4, 2 ) );
		this.rtf_clockFilter.setValue( ".*" );
		this.rtf_clockFilter.setVisible( false );
		this.rtf_listener = new RestrictedTextFieldAdapter( )
		{
			public void valueChangeCommitted( )
			{
				stopStatTracing( );
				startStatTracing( );
			};
		};
		this.rtf_clockFilter.addListener( this.rtf_listener );
		this.l_clock = new JLabel( CLOCK_PREFIX + "0s" );
		pa_buttons.add( this.l_clock, cc_buttons.xy( 6, 2 ) );

		this.l_clockErrCount = new JLabel( );
		pa_buttons.add( this.l_clockErrCount, cc_buttons.xy( 8, 2 ) );

		this.bu_startStop = new JToggleButton( LS_IconLib.get( ).getIcon( LS_IconType.START_TRACING, true, IconSize.S16x16 ) );
		pa_buttons.add( this.bu_startStop, cc_buttons.xy( 10, 2 ) );
		bu_startStop.setToolTipText( "Start tracing" );
		this.bu_startStop.addActionListener( new ActionListener( )
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				if ( bu_startStop.isSelected( ) )
				{
					startStatTracing( );
				}
				else
				{
					stopStatTracing( );
				}
			}
		} );

		this.bu_exportStats = new JButton( LS_IconLib.get( ).getIcon( LS_IconType.EXPORT_STATS, true, IconSize.S16x16 ) );
		pa_buttons.add( this.bu_exportStats, cc_buttons.xy( 12, 2 ) );
		bu_exportStats.setToolTipText( "Export statistics to file" );
		this.bu_exportStats.addActionListener( new ActionListener( )
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				exportStatisticsToFile( );
			}
		} );
	}

	private void exportStatisticsToFile( )
	{
		JFileChooser fc = new JFileChooser( this.lineStats.getPrefs( ).getExportStatsFilePath( ) );
		fc.setFileSelectionMode( JFileChooser.FILES_ONLY );
		fc.setFileFilter( new FileNameExtensionFilter( "CSV-Files", STATS_FILE_EXTENSION ) );
		fc.setDialogTitle( "Export statistics to file" );
		if ( fc.showSaveDialog( this ) == JFileChooser.APPROVE_OPTION )
		{
			File f = fc.getSelectedFile( );
			if ( !f.getName( ).endsWith( "." + STATS_FILE_EXTENSION ) )
			{
				f = new File( f.getAbsolutePath( ) + "." + STATS_FILE_EXTENSION );
			}

			this.lineStats.exportStatistics( f );
			this.lineStats.getPrefs( ).setExportStatsFilePath( f.getParentFile( ) );
		}// f ( fc.showSaveDialog( this ) == JFileChooser.APPROVE_OPTION )
	}

	private void exportFiltersToFile( )
	{
		JFileChooser fc = new JFileChooser( this.lineStats.getPrefs( ).getFileFilterPath( ) );
		fc.setFileSelectionMode( JFileChooser.FILES_ONLY );
		fc.setDialogTitle( "Export filters to file" );
		if ( fc.showSaveDialog( this ) == JFileChooser.APPROVE_OPTION )
		{
			this.lineStats.exportFiltersToFile( fc.getSelectedFile( ) );
			this.lineStats.getPrefs( ).setFileFilterPath( fc.getSelectedFile( ).getParentFile( ) );
		}
	}

	public void setClockFilter( Pattern clockFilter )
	{
		if ( clockFilter != null )
		{
			this.rtf_clockFilter.removeListener( this.rtf_listener );

			String clockFilterString = ".*";
			clockFilterString = clockFilter.toString( );

			if ( clockFilterString.startsWith( ".*" ) )
			{
				clockFilterString = clockFilterString.substring( 2, clockFilterString.length( ) );
			}

			if ( clockFilterString.endsWith( ".*" ) )
			{
				clockFilterString = clockFilterString.substring( 0, clockFilterString.length( ) - 2 );
			}

			if ( clockFilterString.trim( ).isEmpty( ) )
			{
				clockFilterString = ".*";
			}

			this.rtf_clockFilter.setValue( clockFilterString );
			bu_clockFilter.setToolTipText( "<html>Define a clock-filter to ensure that in the logfiles to be considered only one clock is available.<br>Each log-line that does not matches the clock-filter will be ignored.<br>Current Clock-filter: <b>" + this.rtf_clockFilter.getValue( ) + "</b><br></html>" );
			bu_clockFilter.setText( "Clock: '" + this.rtf_clockFilter.getValue( ) + "'" );
			this.rtf_clockFilter.addListener( this.rtf_listener );
		}// if ( clockFilter != null )
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

	private void startStatTracing( )
	{
		bu_clockFilter.setText( "Clock: '" + this.rtf_clockFilter.getValue( ) + "'" );
		bu_clockFilter.setToolTipText( "<html>Define a clock-filter to ensure that in the logfiles to be considered only one clock is available.<br>Each log-line that does not matches the clock-filter will be ignored.<br>Current Clock-filter: <b>" + rtf_clockFilter.getValue( ) + "</b><br></html>" );
		bu_startStop.setIcon( LS_IconLib.get( ).getIcon( LS_IconType.STOP_TRACING, true, IconSize.S16x16 ) );
		bu_startStop.setToolTipText( "Stop tracing" );
		this.lineStats.setTracingEnabled( true );
		this.lineStats.startStatTracing( );
	}

	public Pattern getClockFilter( )
	{
		Pattern result = null;
		String clockFilterStr = this.rtf_clockFilter.getValue( );

		if ( ( clockFilterStr != null ) && ( !clockFilterStr.trim( ).isEmpty( ) ) )
		{
			clockFilterStr = addLeadingAndTralingAllPattern( clockFilterStr );
			try
			{
				result = Pattern.compile( clockFilterStr );
			}
			catch ( PatternSyntaxException e )
			{

				LOG( ).warning( "Ignore filter for clock '" + clockFilterStr + "' since it is invalid: " + e.getLocalizedMessage( ) );
			}
		}// if ( ( clockFilterStr != null ) && ( !clockFilterStr.trim( ).isEmpty( ) ) )

		if ( result == null )
		{
			result = ALL_FILTER;
		}

		return result;
	}

	private void stopStatTracing( )
	{
		bu_startStop.setIcon( LS_IconLib.get( ).getIcon( LS_IconType.START_TRACING, true, IconSize.S16x16 ) );
		bu_startStop.setToolTipText( "Start tracing" );
		this.lineStats.setTracingEnabled( false );
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
		private Semaphore		eventSemaphore;

		private AtomicBoolean	fireEnabled;

		public UpdateTask( )
		{
			super( "LineStatsUpdateTimer" );
			this.cancelRequested = new AtomicBoolean( false );
			this.eventSemaphore = new Semaphore( 0, true );
			this.fireEnabled = new AtomicBoolean( false );
		}

		public void updateNow( )
		{
			this.eventSemaphore.release( );
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
					this.eventSemaphore.acquire( );
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
		bu_startStop.setIcon( LS_IconLib.get( ).getIcon( LS_IconType.STOP_TRACING, true, IconSize.S16x16 ) );
		bu_startStop.setToolTipText( "Stop tracing" );
		bu_startStop.setSelected( true );

		this.l_clock.setText( CLOCK_PREFIX + "0s" );
		this.clockErrors.clear( );
		this.l_clockErrCount.setText( "" );
		this.updateTask.setFireEnabled( true );
		this.updateTask.updateNow( );

	}

	@Override
	public void onStopTracing( )
	{
		this.updateTask.setFireEnabled( false );
	}

	@Override
	public void onTimeUpdated( long currentTime, long elapsedTime )
	{
		this.updateTask.updateNow( );
		this.l_clock.setText( CLOCK_PREFIX + String.format( "%.2f", elapsedTime / 1000f ) + "s" );
	}

	@Override
	public void onError( String err )
	{
		this.updateTask.updateNow( );
		this.clockErrors.add( err );
		this.l_clockErrCount.setText( "<html><font color=\"#FF0000\">Statistics reset because of Clock error (" + err + ") #Errors=" + this.clockErrors.size( ) + ". Please define a clock-filter." + "</font></html>" );
	}

}
