/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.tools.dashboard.ui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.PatternSyntaxException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import org.l2jmobius.tools.dashboard.runtime.DashboardWorkloadCoordinator;
import org.l2jmobius.tools.dashboard.runtime.DashboardWorkloadCoordinator.WorkloadPermit;
import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;
import org.l2jmobius.tools.dashboard.tools.SearchService;

/**
 * Searches distribution files in a cancellable background task while respecting the global maintenance workload policy.
 * @author BazookaRpm
 */
public final class SearchPage extends JPanel implements DashboardPageLifecycle
{
	private final SearchService _service = new SearchService();
	private final DashboardWorkloadCoordinator _workloads;
	private final AtomicBoolean _cancelled = new AtomicBoolean();
	private final JTextField _root = new JTextField();
	private final JTextField _expression = new JTextField();
	private final JCheckBox _caseInsensitive = new JCheckBox("Case insensitive", true);
	private final JCheckBox _ini = new JCheckBox("INI", true);
	private final JCheckBox _xml = new JCheckBox("XML", true);
	private final JCheckBox _java = new JCheckBox("Java", true);
	private final JCheckBox _sql = new JCheckBox("SQL", true);
	private final JCheckBox _html = new JCheckBox("HTML", true);
	private final DefaultTableModel _model = new DefaultTableModel(new String[]
	{
		"File",
		"Matches"
	}, 0)
	{
		@Override
		public boolean isCellEditable(int row, int column)
		{
			return false;
		}
	};
	private final JTable _table = new JTable(_model);
	private final JProgressBar _progress = new JProgressBar(0, 100);
	private final JLabel _summary = DashboardUiFactory.label("Ready.", Font.PLAIN, 10, ControlCenterTheme.MUTED);
	private final JButton _browse = new DashboardActionButton("Browse", null, ControlCenterTheme.ACTION_PRIMARY, false);
	private final JButton _search = new DashboardActionButton("Search", ControlCenterIcons.create(GlyphIcon.Kind.SEARCH, 14, ControlCenterTheme.TEXT), ControlCenterTheme.ACTION_PRIMARY, true);
	private final Runnable _availabilityListener = () -> SwingUtilities.invokeLater(this::refreshAvailability);
	private SwingWorker<SearchService.SearchSummary, SearchService.SearchMatch> _worker;
	private WorkloadPermit _permit;
	private boolean _stopping;
	
	public SearchPage(ControlCenterSettings settings, DashboardWorkloadCoordinator workloads)
	{
		_workloads = workloads;
		setOpaque(false);
		setLayout(new BorderLayout(0, 8));
		_root.setText(settings.getDistributionRoot().resolve("game").toString());
		add(DashboardUiFactory.title("Search", "Search distribution files without blocking the Dashboard interface."), BorderLayout.NORTH);
		final JPanel content = DashboardUiFactory.panel(new BorderLayout(0, 8));
		content.add(buildControls(), BorderLayout.NORTH);
		_table.setAutoCreateRowSorter(true);
		_table.getColumnModel().getColumn(1).setMaxWidth(90);
		_table.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent event)
			{
				if ((event.getClickCount() == 2) && (_table.getSelectedRow() >= 0))
				{
					openSelected();
				}
			}
		});
		final JScrollPane scroll = DashboardUiFactory.scroll(_table);
		content.add(scroll, BorderLayout.CENTER);
		final JPanel footer = DashboardUiFactory.panel(new BorderLayout(8, 0));
		footer.add(_summary, BorderLayout.CENTER);
		footer.add(_progress, BorderLayout.SOUTH);
		content.add(footer, BorderLayout.SOUTH);
		add(content, BorderLayout.CENTER);
		_search.addActionListener(event -> toggleSearch());
		refreshAvailability();
	}
	
	@Override
	public void pageShown()
	{
		_workloads.addListener(_availabilityListener);
		refreshAvailability();
	}
	
	@Override
	public void pageHidden()
	{
		_workloads.removeListener(_availabilityListener);
	}
	
	private JPanel buildControls()
	{
		final SurfacePanel section = DashboardUiFactory.section("Search configuration");
		final JPanel rows = DashboardUiFactory.panel(new BorderLayout(0, 6));
		final JPanel rootRow = DashboardUiFactory.panel(new BorderLayout(6, 0));
		rootRow.add(DashboardUiFactory.label("Root", Font.BOLD, 11, ControlCenterTheme.MUTED), BorderLayout.WEST);
		rootRow.add(_root, BorderLayout.CENTER);
		_browse.addActionListener(event -> chooseRoot());
		rootRow.add(_browse, BorderLayout.EAST);
		rows.add(rootRow, BorderLayout.NORTH);
		final JPanel expressionRow = DashboardUiFactory.panel(new BorderLayout(6, 0));
		expressionRow.add(DashboardUiFactory.label("Expression", Font.BOLD, 11, ControlCenterTheme.MUTED), BorderLayout.WEST);
		expressionRow.add(_expression, BorderLayout.CENTER);
		expressionRow.add(_search, BorderLayout.EAST);
		rows.add(expressionRow, BorderLayout.CENTER);
		final JPanel options = DashboardUiFactory.panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		options.add(_caseInsensitive);
		options.add(_ini);
		options.add(_xml);
		options.add(_java);
		options.add(_sql);
		options.add(_html);
		rows.add(options, BorderLayout.SOUTH);
		section.add(rows, BorderLayout.CENTER);
		return section;
	}
	
	private void chooseRoot()
	{
		final JFileChooser chooser = new JFileChooser(_root.getText());
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			_root.setText(chooser.getSelectedFile().getAbsolutePath());
		}
	}
	
	private void toggleSearch()
	{
		if ((_worker != null) && !_worker.isDone())
		{
			_cancelled.set(true);
			_stopping = true;
			_search.setText("Stopping...");
			_search.setEnabled(false);
			return;
		}
		
		startSearch();
	}
	
	private void startSearch()
	{
		final Path root;
		final String expression = _expression.getText();
		final boolean caseInsensitive = _caseInsensitive.isSelected();
		final Set<String> extensions = selectedExtensions();
		try
		{
			root = Path.of(_root.getText().trim());
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, e.getMessage(), "Search failed", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		_permit = _workloads.tryAcquireHeavy("Search");
		if (_permit == null)
		{
			JOptionPane.showMessageDialog(this, _workloads.maintenanceBlockReason(), "Search unavailable", JOptionPane.WARNING_MESSAGE);
			refreshAvailability();
			return;
		}
		
		_cancelled.set(false);
		_stopping = false;
		_model.setRowCount(0);
		_progress.setValue(0);
		_summary.setText("Searching...");
		_search.setText("Stop");
		_search.setEnabled(true);
		setSearchControlsEnabled(false);
		_worker = new SwingWorker<>()
		{
			@Override
			protected SearchService.SearchSummary doInBackground() throws Exception
			{
				return _service.search(root, expression, extensions, caseInsensitive, _cancelled, this::publish, value -> setProgress(value));
			}
			
			@Override
			protected void process(List<SearchService.SearchMatch> chunks)
			{
				for (SearchService.SearchMatch match : chunks)
				{
					_model.addRow(new Object[]
					{
						match.file().toAbsolutePath().toString(),
						match.matches()
					});
				}
			}
			
			@Override
			protected void done()
			{
				_progress.setValue(getProgress());
				try
				{
					final SearchService.SearchSummary result = get();
					_summary.setText((_cancelled.get() ? "Stopped. " : "Complete. ") + result.filesSearched() + " files searched, " + result.totalMatches() + " matches in " + result.filesWithMatches() + " files.");
				}
				catch (Exception e)
				{
					final Throwable cause = e.getCause() == null ? e : e.getCause();
					_summary.setText("Search failed: " + cause.getMessage());
					if (!(cause instanceof PatternSyntaxException))
					{
						JOptionPane.showMessageDialog(SearchPage.this, cause.getMessage(), "Search failed", JOptionPane.ERROR_MESSAGE);
					}
				}
				finally
				{
					releasePermit();
					_stopping = false;
					_search.setText("Search");
					setSearchControlsEnabled(true);
					refreshAvailability();
				}
			}
		};
		_worker.addPropertyChangeListener(event ->
		{
			if ("progress".equals(event.getPropertyName()))
			{
				_progress.setValue((Integer) event.getNewValue());
			}
		});
		_worker.execute();
	}
	
	private void refreshAvailability()
	{
		if ((_worker != null) && !_worker.isDone())
		{
			_search.setEnabled(!_stopping);
			return;
		}
		
		_search.setEnabled(_workloads.isMaintenanceAvailable());
		final String reason = _workloads.maintenanceBlockReason();
		_search.setToolTipText(reason.isEmpty() ? null : reason);
	}
	
	private void setSearchControlsEnabled(boolean enabled)
	{
		_root.setEnabled(enabled);
		_expression.setEnabled(enabled);
		_caseInsensitive.setEnabled(enabled);
		_ini.setEnabled(enabled);
		_xml.setEnabled(enabled);
		_java.setEnabled(enabled);
		_sql.setEnabled(enabled);
		_html.setEnabled(enabled);
		_browse.setEnabled(enabled);
	}
	
	private void releasePermit()
	{
		if (_permit != null)
		{
			_permit.close();
			_permit = null;
		}
	}
	
	private Set<String> selectedExtensions()
	{
		final Set<String> extensions = new HashSet<>();
		if (_ini.isSelected())
		{
			extensions.add(".ini");
		}
		
		if (_xml.isSelected())
		{
			extensions.add(".xml");
		}
		
		if (_java.isSelected())
		{
			extensions.add(".java");
		}
		
		if (_sql.isSelected())
		{
			extensions.add(".sql");
		}
		
		if (_html.isSelected())
		{
			extensions.add(".htm");
			extensions.add(".html");
		}
		
		return extensions;
	}
	
	private void openSelected()
	{
		final int viewRow = _table.getSelectedRow();
		if (viewRow < 0)
		{
			return;
		}
		
		final int row = _table.convertRowIndexToModel(viewRow);
		final Path file = Path.of(String.valueOf(_model.getValueAt(row, 0)));
		try
		{
			if (!Desktop.isDesktopSupported())
			{
				throw new IllegalStateException("Desktop integration is unavailable.");
			}
			
			Desktop.getDesktop().open(file.toFile());
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, e.getMessage(), "Open file", JOptionPane.ERROR_MESSAGE);
		}
	}
}
