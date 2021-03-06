/*
 *  Copyright (C) 2014, Thomas Obenaus. All rights reserved.
 *  Licensed under the New BSD License (3-clause lic)
 *  See attached license-file.
 *
 *	Author: 	Thomas Obenaus
 *	EMail:		obenaus.thomas@gmail.com
 *  Project:    LogFileViewer
 */

package thobe.logfileviewer.plugins.console;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import thobe.logfileviewer.plugin.api.IPluginUI;
import thobe.logfileviewer.plugin.api.IPluginUIComponent;
import thobe.logfileviewer.plugin.source.logline.ILogLine;
import thobe.logfileviewer.plugin.util.FontHelper;
import thobe.logfileviewer.plugin.util.SizeOf;
import thobe.logfileviewer.plugins.console.events.CEvtClear;
import thobe.logfileviewer.plugins.console.events.CEvt_Scroll;
import thobe.logfileviewer.plugins.console.events.CEvt_ScrollToLast;
import thobe.logfileviewer.plugins.console.events.CEvt_SetAutoScrollMode;
import thobe.logfileviewer.plugins.console.events.ConsoleEvent;
import thobe.logfileviewer.plugins.console.icons.C_IconLib;
import thobe.logfileviewer.plugins.console.icons.C_IconType;
import thobe.widgets.icons.IconSize;
import thobe.widgets.textfield.RestrictedTextFieldAdapter;
import thobe.widgets.textfield.RestrictedTextFieldRegexp;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Thomas Obenaus
 * @source ConsoleUI.java
 * @date Jul 24, 2014
 */
@SuppressWarnings ( "serial")
public class SubConsole extends Thread implements ConsoleDataListener, IPluginUIComponent
{
	private static final int			DEFAULT_WIDTH_COLUMN_0			= 60;
	private static final int			DEFAULT_WIDTH_COLUMN_1			= 110;

	private static final int			LINE_NUMBER_COLUMN_IDX			= 0;
	private static final int			TIME_STAMP_COLUMN_IDX			= 1;
	private static final int			DATA_COLUMN_IDX					= 2;

	/**
	 * Max time spent waiting for completion of the next block of {@link LogLine}s (in MS)
	 */
	private static long					MAX_TIME_PER_BLOCK_IN_MS		= 1000;

	/**
	 * Max amount of {@link LogLine} waiting for completion of one block until the block will be drawn.
	 */
	private static long					MAX_LINES_PER_BLOCK				= 100;

	/**
	 * Time in ms to wait for the next update of the console plugins display-values
	 */
	private static long					UPDATE_DISPLAY_VALUES_INTERVAL	= 1000;

	/**
	 * Queue containing all scroll-events
	 */
	private Deque<CEvt_Scroll>			scrollEventQueue;

	/**
	 * Queue for misc console-events.
	 */
	private Deque<ConsoleEvent>			eventQueue;

	/**
	 * Queue containing all incoming {@link LogLine}s
	 */
	private Deque<ILogLine>				lineBuffer;

	/**
	 * The internal {@link TableModel}
	 */
	private ConsoleTableModel			tableModel;

	/**
	 * The plugin-panel (returned by {@link IPluginUI#getUIComponent()}.
	 */
	private JPanel						pa_logPanel;

	/**
	 * True if autoscrolling is enabled, false otherwise
	 */
	private boolean						autoScrollingEnabled;

	/**
	 * The table.
	 */
	private JTable						ta_logTable;

	/**
	 * Semaphore for the internal event-main-loop
	 */
	private Semaphore					eventSemaphore;

	private JButton						bu_clear;
	private JButton						bu_settings;
	private JToggleButton				bu_enableAutoScroll;
	private JButton						bu_createFilter;

	private JLabel						l_statusline;

	private ConsoleFilterCellRenderer	rowFilter;

	/**
	 * Timestamp of next time the console's display-values will be updated
	 */
	private long						nextUpdateOfDisplayValues;

	/**
	 * The {@link RestrictedTextFieldRegexp} containing the filter for filtering the log-file.
	 */
	private RestrictedTextFieldRegexp	rtf_filter;

	private ISubConsoleFactoryAccess	subConsoleFactoryAccess;

	private Logger						log;

	private Pattern						linePattern;

	private AtomicBoolean				quitRequested;

	private String						parentConsolePattern;

	private boolean						closeable;

	private String						title;

	private String						description;

	private JScrollPane					scrpa_main;

	/**
	 * Preferred with of the currently longest log-line
	 */
	private AtomicInteger				currentMaxPrefWidth;

	public SubConsole( String parentConsolePattern, String pattern, ISubConsoleFactoryAccess subConsoleFactoryAccess, Logger log, boolean closeable )
	{
		this.closeable = closeable;
		this.currentMaxPrefWidth = new AtomicInteger( 0 ); 
		this.linePattern = Pattern.compile( pattern );
		this.parentConsolePattern = parentConsolePattern;

		// create and set the thread-name
		this.setName( "SubConsole {pattern='" + this.getFullPattern( ) + "'}" );

		this.subConsoleFactoryAccess = subConsoleFactoryAccess;
		this.quitRequested = new AtomicBoolean( false );
		this.log = log;
		this.eventSemaphore = new Semaphore( 1, true );
		this.lineBuffer = new ConcurrentLinkedDeque<>( );
		this.scrollEventQueue = new ConcurrentLinkedDeque<>( );
		this.eventQueue = new ConcurrentLinkedDeque<>( );
		this.autoScrollingEnabled = true;
		this.nextUpdateOfDisplayValues = 0;
		this.title = this.subConsoleFactoryAccess.createTitle( this );
		this.description = this.subConsoleFactoryAccess.createDescription( this );
		this.buildGUI( );
	}

	protected String getFullPattern( )
	{
		String pattern = "";
		if ( this.parentConsolePattern != null )
			pattern += this.parentConsolePattern + " AND ";
		pattern += this.linePattern;
		return pattern;
	}

	private void buildGUI( )
	{
		this.pa_logPanel = new JPanel( new BorderLayout( 0, 0 ) );
		this.tableModel = new ConsoleTableModel( );

		this.ta_logTable = new JTable( this.tableModel );
		this.scrpa_main = new JScrollPane( ta_logTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		this.pa_logPanel.add( scrpa_main, BorderLayout.CENTER );
		this.ta_logTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

		CellConstraints cc_settings = new CellConstraints( );
		JPanel pa_settings = new JPanel( new FormLayout( "3dlu,fill:default,10dlu,default,1dlu,default,0dlu,16dlu,default:grow,default,3dlu", "3dlu,pref,3dlu" ) );
		this.pa_logPanel.add( pa_settings, BorderLayout.NORTH );

		this.l_statusline = new JLabel( "Lines: 0/" + this.tableModel.getMaxNumberOfConsoleEntries( ) );
		pa_settings.add( this.l_statusline, cc_settings.xy( 2, 2 ) );

		JLabel l_filter = new JLabel( "Filter: " );
		pa_settings.add( l_filter, cc_settings.xy( 4, 2 ) );
		String tt_filter = "<html><h4>Filter the Console using regular expressions (matching lines will be colored):</h4>";
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

		l_filter.setToolTipText( tt_filter );

		this.rtf_filter = new RestrictedTextFieldRegexp( 60 );
		pa_settings.add( rtf_filter, cc_settings.xy( 6, 2 ) );
		rtf_filter.setToolTipText( tt_filter );
		rtf_filter.addListener( new RestrictedTextFieldAdapter( )
		{
			@Override
			public void valueChangeCommitted( )
			{
				String value = rtf_filter.getValue( );
				bu_createFilter.setEnabled( ( value != null ) && ( !value.trim( ).isEmpty( ) ) );

				rowFilter.setFilterRegex( rtf_filter.getValue( ) );
				// repaint/ filter the whole log on commit
				tableModel.fireTableDataChanged( );
			}

			@Override
			public void valueChanged( )
			{
				String value = rtf_filter.getValue( );
				rowFilter.setFilterRegex( value );
				bu_createFilter.setEnabled( ( value != null ) && ( !value.trim( ).isEmpty( ) ) );
			};
		} );

		this.bu_createFilter = new JButton( C_IconLib.get( ).getIcon( C_IconType.CREATE_FILTER, true, IconSize.S16x16 ) );
		pa_settings.add( bu_createFilter, cc_settings.xy( 8, 2 ) );
		this.bu_createFilter.addActionListener( new ActionListener( )
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				SubConsole consoleUI = subConsoleFactoryAccess.createNewSubConsole( getFullPattern( ), rtf_filter.getValue( ), true );
				subConsoleFactoryAccess.registerSubConsole( consoleUI, true );
			}
		} );
		this.bu_createFilter.setEnabled( false );

		JPanel pa_buttons = new JPanel( new FlowLayout( FlowLayout.RIGHT, 0, 0 ) );
		pa_settings.add( pa_buttons, cc_settings.xy( 10, 2 ) );

		this.bu_enableAutoScroll = new JToggleButton( C_IconLib.get( ).getIcon( C_IconType.SCROLL_LOCK, true, IconSize.S16x16 ), true );
		pa_buttons.add( this.bu_enableAutoScroll );
		this.bu_enableAutoScroll.addActionListener( new ActionListener( )
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				toggleAutoScroll( );
			}
		} );

		this.bu_clear = new JButton( C_IconLib.get( ).getIcon( C_IconType.CLEAR, true, IconSize.S16x16 ) );
		pa_buttons.add( this.bu_clear );
		this.bu_clear.addActionListener( new ActionListener( )
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				clear( );
			}
		} );

		this.bu_settings = new JButton( C_IconLib.get( ).getIcon( C_IconType.SETTINGS, true, IconSize.S16x16 ) );
		pa_buttons.add( this.bu_settings );

		// adjust column-sizes
		ta_logTable.getColumnModel( ).getColumn( LINE_NUMBER_COLUMN_IDX ).setMinWidth( DEFAULT_WIDTH_COLUMN_0 );
		ta_logTable.getColumnModel( ).getColumn( LINE_NUMBER_COLUMN_IDX ).setWidth( DEFAULT_WIDTH_COLUMN_0 );
		ta_logTable.getColumnModel( ).getColumn( LINE_NUMBER_COLUMN_IDX ).setPreferredWidth( DEFAULT_WIDTH_COLUMN_0 );
		ta_logTable.getColumnModel( ).getColumn( LINE_NUMBER_COLUMN_IDX ).setResizable( true );
		ta_logTable.getColumnModel( ).getColumn( TIME_STAMP_COLUMN_IDX ).setMinWidth( DEFAULT_WIDTH_COLUMN_1 );
		ta_logTable.getColumnModel( ).getColumn( TIME_STAMP_COLUMN_IDX ).setPreferredWidth( DEFAULT_WIDTH_COLUMN_1 );
		ta_logTable.getColumnModel( ).getColumn( TIME_STAMP_COLUMN_IDX ).setWidth( DEFAULT_WIDTH_COLUMN_1 );
		ta_logTable.getColumnModel( ).getColumn( TIME_STAMP_COLUMN_IDX ).setResizable( false );

		// enable one-click cell-selection
		( ( DefaultCellEditor ) ta_logTable.getDefaultEditor( ta_logTable.getColumnClass( DATA_COLUMN_IDX ) ) ).setClickCountToStart( 1 );

		// listener that is responsible to scroll the table to the last entry 
		this.tableModel.addTableModelListener( new TableModelListener( )
		{
			@Override
			public void tableChanged( final TableModelEvent e )
			{
				if ( e.getType( ) == TableModelEvent.INSERT && autoScrollingEnabled )
				{
					addScrollEvent( new CEvt_ScrollToLast( ) );
				}
			}
		} );

		this.ta_logTable.addMouseListener( new MouseAdapter( )
		{
			@Override
			public void mouseClicked( MouseEvent e )
			{
				addScrollEvent( new CEvt_SetAutoScrollMode( false ) );
			}
		} );

		this.ta_logTable.addComponentListener( new ComponentAdapter( )
		{
			@Override
			public void componentShown( ComponentEvent e )
			{
				updateTableColumSize( currentMaxPrefWidth.get( ) );
			}
		} );

		this.scrpa_main.addComponentListener( new ComponentAdapter( )
		{
			@Override
			public void componentResized( ComponentEvent e )
			{
				updateTableColumSize( currentMaxPrefWidth.get( ) );
			}
		} );

		// Listener that disables autoscrolling if the user scrolls upwards
		scrpa_main.addMouseWheelListener( new MouseWheelListener( )
		{
			@Override
			public void mouseWheelMoved( MouseWheelEvent evt )
			{
				// disable autoscrolling if the wheel rotates up
				if ( evt.getWheelRotation( ) == -1 )
				{
					addScrollEvent( new CEvt_SetAutoScrollMode( false ) );
				}// if ( evt.getWheelRotation( ) == -1 ).
			}
		} );

		// listener keeping track of disabling/enabling autoscrolling
		scrpa_main.getVerticalScrollBar( ).addAdjustmentListener( new AdjustmentListener( )
		{
			@Override
			public void adjustmentValueChanged( AdjustmentEvent evt )
			{
				// Disable auto-scrolling if the scrollbar is moved 
				int extent = scrpa_main.getVerticalScrollBar( ).getModel( ).getExtent( );
				int currentScrollPos = scrpa_main.getVerticalScrollBar( ).getValue( ) + extent;

				// bottom is reached in case the current-scrolling pos is greater than 98% of the maximum-scroll position of the table
				// if bottom is reached we want to enable autoscrolling 
				boolean bottomReached = currentScrollPos >= ( scrpa_main.getVerticalScrollBar( ).getMaximum( ) * 0.98 );

				// in case we have changed the autoscrolling-mode (disable/enable) we generate the according event to force the modification
				if ( ( evt.getValueIsAdjusting( ) == true ) && ( evt.getAdjustmentType( ) == AdjustmentEvent.TRACK ) && ( autoScrollingEnabled != bottomReached ) )
				{
					addScrollEvent( new CEvt_SetAutoScrollMode( bottomReached ) );
				}// if ( ( evt.getValueIsAdjusting( ) == true ) && ( evt.getAdjustmentType( ) == ..
			}
		} );

		this.rowFilter = new ConsoleFilterCellRenderer( this.tableModel );

		// set the cell-renderer that should be used for filtering
		this.ta_logTable.getColumnModel( ).getColumn( 0 ).setCellRenderer( this.rowFilter );
		this.ta_logTable.getColumnModel( ).getColumn( 1 ).setCellRenderer( this.rowFilter );
		this.ta_logTable.getColumnModel( ).getColumn( DATA_COLUMN_IDX ).setCellRenderer( this.rowFilter );

		Font consoleFont = FontHelper.getConsoleFont( );
		LOG( ).info( "Setting console-font to " + consoleFont );
		this.ta_logTable.setFont( consoleFont );
	}

	public void clear( )
	{
		this.eventQueue.add( new CEvtClear( ) );
		this.eventSemaphore.release( );
	}

	public void toggleAutoScroll( )
	{
		this.addScrollEvent( new CEvt_SetAutoScrollMode( !this.autoScrollingEnabled ) );
	}

	private void addScrollEvent( CEvt_Scroll evt )
	{
		this.scrollEventQueue.add( evt );
		eventSemaphore.release( );
	}

	private void updateDisplayValues( )
	{
		if ( this.nextUpdateOfDisplayValues <= System.currentTimeMillis( ) )
		{
			this.nextUpdateOfDisplayValues = System.currentTimeMillis( ) + UPDATE_DISPLAY_VALUES_INTERVAL;
			this.l_statusline.setText( "Lines: " + this.tableModel.getRowCount( ) + "/" + this.tableModel.getMaxNumberOfConsoleEntries( ) );
		}// if ( this.nextUpdateOfDisplayValues <= System.currentTimeMillis( ) ) .
	}

	@Override
	public void run( )
	{
		List<ILogLine> block = new ArrayList<>( );
		while ( !this.isQuitRequested( ) )
		{
			// process next event from the event-queue
			processScrollEvents( );

			// process misc events
			processEvents( );

			// update display-values
			updateDisplayValues( );

			long startTime = System.currentTimeMillis( );
			boolean timeThresholdHurt = false;
			boolean blockSizeThresholdHurt = false;
			block.clear( );

			// collect some lines
			synchronized ( this.lineBuffer )
			{
				while ( ( !this.lineBuffer.isEmpty( ) ) && !timeThresholdHurt && !blockSizeThresholdHurt )
				{
					ILogLine ll = this.lineBuffer.pollFirst( );
					// add only matching lines
					if ( matches( this.linePattern, ll.getData( ) ) )
					{
						block.add( ll );
					}
					blockSizeThresholdHurt = block.size( ) > MAX_LINES_PER_BLOCK;
					timeThresholdHurt = ( System.currentTimeMillis( ) - startTime ) > MAX_TIME_PER_BLOCK_IN_MS;
				}// while ( ( !this.lineBuffer.isEmpty( ) ) && !timeThresholdHurt && !blockSizeThresholdHurt ).
			}// synchronized ( this.lineBuffer ).

			// Add the block if we have collected some lines
			if ( !block.isEmpty( ) )
			{
				this.tableModel.addBlock( block );
			}// if ( !block.isEmpty( ) ).

			try
			{
				this.eventSemaphore.tryAcquire( 2, TimeUnit.SECONDS );
			}
			catch ( InterruptedException e )
			{
				LOG( ).severe( "Exception caught in console-plugin main-loop: " + e.getLocalizedMessage( ) );
			}

		}// while ( !this.isQuitRequested( ) ).

		this.tableModel.quit( );
		LOG( ).info( this + ": Leaving main-loop." );
	}

	private void processEvents( )
	{
		ConsoleEvent evt = null;
		synchronized ( this.eventQueue )
		{

			while ( ( evt = this.eventQueue.poll( ) ) != null )
			{
				switch ( evt.getType( ) )
				{
				case CLEAR:
					this.currentMaxPrefWidth.set( 0 );
					updateTableColumSize( 0 );
					this.tableModel.clear( );
					break;
				default:
					LOG( ).warning( "Unknown event: " + evt );
					break;
				}// switch ( evt.getType( ) ) .
			}// while ( ( evt = this.eventQueue.poll( ) ) != null ) .
		}// synchronized ( this.eventQueue ) .
	}

	private void processScrollEvents( )
	{
		CEvt_Scroll evt = null;
		boolean hasScrollToLast = false;
		synchronized ( this.scrollEventQueue )
		{
			while ( ( evt = this.scrollEventQueue.poll( ) ) != null )
			{
				switch ( evt.getType( ) )
				{
				case SCROLL_TO_LAST:
					hasScrollToLast = true;
					break;
				case SET_AUTOSCROLL_MODE:
					boolean prefAutoScrollValue = this.autoScrollingEnabled;
					this.autoScrollingEnabled = ( ( CEvt_SetAutoScrollMode ) evt ).isEnable( );
					this.bu_enableAutoScroll.setSelected( this.autoScrollingEnabled );

					// scroll to last only if the autoscrolling was toggled to true
					hasScrollToLast = ( this.autoScrollingEnabled && ( prefAutoScrollValue != this.autoScrollingEnabled ) );
					break;
				default:
					LOG( ).warning( "Unknown event: " + evt );
					break;
				}
			}
		}// synchronized ( this.scrollEventQueue ) .

		// scroll to the last line if needed
		if ( hasScrollToLast && this.autoScrollingEnabled )
		{
			SwingUtilities.invokeLater( new Runnable( )
			{
				public void run( )
				{
					int viewRow = ta_logTable.convertRowIndexToView( ta_logTable.getRowCount( ) - 1 );
					Rectangle visibleRect = ta_logTable.getVisibleRect( );
					Rectangle rectWithLastRowVisible = ta_logTable.getCellRect( viewRow, 0, true );

					// adjust height and y to show the last row, but keep x and width to keep current horizontal scrollbar position
					visibleRect.y = rectWithLastRowVisible.y;
					visibleRect.height = rectWithLastRowVisible.height;

					ta_logTable.scrollRectToVisible( visibleRect );
				}
			} );
		}
	}

	public long getCurrentMemory( )
	{
		long memInLineBuffer = 0;
		for ( ILogLine ll : this.lineBuffer )
			memInLineBuffer += ll.getMemory( ) + SizeOf.REFERENCE + SizeOf.HOUSE_KEEPING_ARRAY;
		long memInEventQueue = this.scrollEventQueue.size( ) * SizeOf.REFERENCE * SizeOf.HOUSE_KEEPING;
		memInEventQueue += SizeOf.REFERENCE + SizeOf.HOUSE_KEEPING_ARRAY;

		return memInLineBuffer + this.tableModel.getMem( ) + memInEventQueue;
	}

	public void setAutoScrollingEnabled( boolean autoScrollingEnabled )
	{
		this.autoScrollingEnabled = autoScrollingEnabled;
		this.eventSemaphore.release( );
	}

	public void freeMemory( )
	{
		synchronized ( this.lineBuffer )
		{
			this.tableModel.clear( );
			this.lineBuffer.clear( );
		}
		this.eventSemaphore.release( );
	}

	protected Logger LOG( )
	{
		return this.log;
	}

	public boolean isQuitRequested( )
	{
		return quitRequested.get( );
	}

	private void updateTableColumSize( int newPreferredWidth )
	{
		// adjust the width the table size to enable horizotal scrolling
		synchronized ( currentMaxPrefWidth )
		{
			TableColumn dataColumn = this.ta_logTable.getColumnModel( ).getColumn( DATA_COLUMN_IDX );

			int parentWitdh = this.ta_logTable.getParent( ).getWidth( ) - DEFAULT_WIDTH_COLUMN_0 - DEFAULT_WIDTH_COLUMN_1 - ( this.ta_logTable.getIntercellSpacing( ).width * 2 );
			this.currentMaxPrefWidth.set( Math.max( newPreferredWidth, currentMaxPrefWidth.get( ) ) );

			int newWidth = 0;

			// use parent width if parent is greater or if child is 0  
			if ( currentMaxPrefWidth.get( ) == 0 || ( parentWitdh > currentMaxPrefWidth.get( ) ) )
				newWidth = parentWitdh;
			else newWidth = currentMaxPrefWidth.get( );

			dataColumn.setPreferredWidth( newWidth );
		}// synchronized ( currentMaxPrefWidth ).
	}

	/**
	 * {@link TableCellRenderer} for this {@link SubConsole}.
	 */
	private class ConsoleFilterCellRenderer extends DefaultTableCellRenderer
	{
		private ConsoleTableModel	tableModel;
		private Pattern				pattern;

		public ConsoleFilterCellRenderer( ConsoleTableModel tableModel )
		{
			this.pattern = null;
			this.tableModel = tableModel;
		}

		public void setFilterRegex( String filterRegex )
		{
			try
			{
				this.pattern = Pattern.compile( filterRegex );
			}
			catch ( PatternSyntaxException e )
			{}
		}

		@Override
		public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column )
		{
			Component result = super.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );

			if ( !isSelected )
			{
				if ( this.tableModel.matches( row, this.pattern ) )
				{
					result.setBackground( Color.orange );
				}
				else
				{
					result.setBackground( ta_logTable.getBackground( ) );

				}
			}

			// adjust the width the table size to enable horizotal scrolling
			updateTableColumSize( result.getPreferredSize( ).width );
			return result;
		}
	}

	@Override
	public void onNewData( List<ILogLine> blockOfLines )
	{
		this.lineBuffer.addAll( blockOfLines );
		this.eventSemaphore.release( );
	}

	private static boolean matches( final Pattern pattern, final String line )
	{
		if ( pattern == null || pattern.pattern( ).trim( ).isEmpty( ) )
			return false;

		boolean result = false;

		try
		{
			Matcher m = pattern.matcher( line );
			result = m.find( );
		}
		catch ( PatternSyntaxException e )
		{}
		return result;
	}

	@Override
	public JComponent getVisualComponent( )
	{
		return this.pa_logPanel;
	}

	@Override
	public String getTitle( )
	{
		return this.title;
	}

	@Override
	public void onClosed( )
	{
		LOG( ).info( this.title + ": closed." );
	}

	@Override
	public void onClosing( )
	{
		LOG( ).info( this.title + ": closing --> unregister" );
		this.subConsoleFactoryAccess.unRegisterSubConsole( this );
	}

	@Override
	public boolean isCloseable( )
	{
		return this.closeable;
	}

	@Override
	public String getTooltip( )
	{
		return this.description;
	}

	public void quit( )
	{
		this.quitRequested.set( true );
		this.eventSemaphore.release( );
	}
}
