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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import org.l2jmobius.tools.dashboard.database.DatabaseConfigurationService;
import org.l2jmobius.tools.dashboard.database.SqlBackupCoordinator;
import org.l2jmobius.tools.dashboard.runtime.DashboardWorkloadCoordinator;
import org.l2jmobius.tools.dashboard.runtime.DashboardWorkloadCoordinator.WorkloadPermit;
import org.l2jmobius.tools.dashboard.runtime.ServiceSupervisor;

/**
 * Displays the managed SQL backup archive and provides guarded offline restore and delete operations. <br>
 * Listing and opening the backup folder remain available while services are online; mutable operations follow the global maintenance policy.
 * @author BazookaRpm
 */
public final class BackupArchivePanel extends JPanel implements DashboardPageLifecycle
{
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
	
	private record BackupEntry(Path file, String database, String created, String size, String status)
	{
	}
	
	private record BackupSnapshot(List<BackupEntry> entries, String error)
	{
	}
	
	private final DatabaseConfigurationService _database;
	private final SqlBackupCoordinator _backups;
	private final DashboardWorkloadCoordinator _workloads;
	private final DefaultTableModel _model;
	private final JTable _table;
	private final List<Path> _files = new ArrayList<>();
	private final JLabel _pathLabel = new JLabel();
	private final DashboardActionButton _restore = new DashboardActionButton("Restore", ControlCenterIcons.create(GlyphIcon.Kind.DATABASE, ControlCenterTheme.TEXT), ControlCenterTheme.RED, false);
	private final DashboardActionButton _delete = new DashboardActionButton("Delete", ControlCenterIcons.create(GlyphIcon.Kind.TRASH, ControlCenterTheme.TEXT), ControlCenterTheme.RED, false);
	private final Runnable _availabilityListener = () -> SwingUtilities.invokeLater(this::refreshAvailability);
	private int _refreshGeneration;
	
	public BackupArchivePanel(DatabaseConfigurationService database, SqlBackupCoordinator backups, ServiceSupervisor login, ServiceSupervisor game, DashboardWorkloadCoordinator workloads)
	{
		_database = database;
		_backups = backups;
		_workloads = workloads;
		setOpaque(false);
		setLayout(new BorderLayout(0, 10));
		add(createHeader(), BorderLayout.NORTH);
		_model = new DefaultTableModel(new Object[]
		{
			"Database",
			"File",
			"Created",
			"Size",
			"Status"
		}, 0)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		_table = new JTable(_model);
		_table.setRowHeight(27);
		_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final JScrollPane scroll = new JScrollPane(_table);
		ControlCenterTheme.styleScrollPane(scroll);
		final SurfacePanel tableSurface = new SurfacePanel();
		tableSurface.setLayout(new BorderLayout());
		tableSurface.setBorder(new EmptyBorder(8, 8, 8, 8));
		tableSurface.add(scroll, BorderLayout.CENTER);
		add(tableSurface, BorderLayout.CENTER);
		add(createActions(), BorderLayout.SOUTH);
		refresh();
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
	
	private JPanel createHeader()
	{
		final JPanel header = new JPanel(new BorderLayout(0, 2));
		header.setOpaque(false);
		final JLabel title = new JLabel("Backups");
		title.setFont(ControlCenterTheme.font(Font.BOLD, 17));
		final JLabel subtitle = new JLabel("Database backup history and guarded offline restore operations");
		subtitle.setFont(ControlCenterTheme.font(Font.PLAIN, 10));
		subtitle.setForeground(ControlCenterTheme.MUTED);
		_pathLabel.setFont(ControlCenterTheme.font(Font.PLAIN, 9));
		_pathLabel.setForeground(ControlCenterTheme.TEXT_BRIGHT);
		final JPanel text = new JPanel(new BorderLayout());
		text.setOpaque(false);
		text.add(title, BorderLayout.NORTH);
		text.add(subtitle, BorderLayout.CENTER);
		text.add(_pathLabel, BorderLayout.SOUTH);
		header.add(text, BorderLayout.CENTER);
		return header;
	}
	
	private JPanel createActions()
	{
		final JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		actions.setOpaque(false);
		final DashboardActionButton refresh = new DashboardActionButton("Refresh", ControlCenterIcons.create(GlyphIcon.Kind.REFRESH, ControlCenterTheme.TEXT), ControlCenterTheme.ACCENT, false);
		final DashboardActionButton folder = new DashboardActionButton("Open folder", ControlCenterIcons.create(GlyphIcon.Kind.FOLDER, ControlCenterTheme.TEXT), ControlCenterTheme.ACCENT, false);
		refresh.addActionListener(event -> refresh());
		folder.addActionListener(event -> openFolder());
		_restore.addActionListener(event -> restoreSelected());
		_delete.addActionListener(event -> deleteSelected());
		actions.add(refresh);
		actions.add(folder);
		actions.add(_restore);
		actions.add(_delete);
		return actions;
	}
	
	public void refresh()
	{
		final Path directory = _backups.backupDirectory().toAbsolutePath().normalize();
		final int generation = ++_refreshGeneration;
		_pathLabel.setText(directory.toString());
		_files.clear();
		_model.setRowCount(0);
		_model.addRow(new Object[]
		{
			"",
			"Loading backups...",
			"",
			"",
			""
		});
		new SwingWorker<BackupSnapshot, Void>()
		{
			@Override
			protected BackupSnapshot doInBackground()
			{
				try
				{
					return loadSnapshot(directory);
				}
				catch (Exception e)
				{
					return new BackupSnapshot(List.of(), rootMessage(e));
				}
			}
			
			@Override
			protected void done()
			{
				if (generation != _refreshGeneration)
				{
					return;
				}
				
				_model.setRowCount(0);
				_files.clear();
				try
				{
					final BackupSnapshot snapshot = get();
					if (snapshot.error() != null)
					{
						_model.addRow(new Object[]
						{
							"",
							"Unable to read backup directory",
							"",
							"",
							snapshot.error()
						});
						
						return;
					}
					
					for (BackupEntry entry : snapshot.entries())
					{
						_files.add(entry.file());
						_model.addRow(new Object[]
						{
							entry.database(),
							entry.file().getFileName().toString(),
							entry.created(),
							entry.size(),
							entry.status()
						});
					}
				}
				catch (Exception e)
				{
					_model.addRow(new Object[]
					{
						"",
						"Unable to read backup directory",
						"",
						"",
						rootMessage(e)
					});
				}
			}
		}.execute();
	}
	
	private BackupSnapshot loadSnapshot(Path directory) throws Exception
	{
		Files.createDirectories(directory);
		final List<Path> files = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.sql"))
		{
			for (Path file : stream)
			{
				if (Files.isRegularFile(file))
				{
					files.add(file.toAbsolutePath().normalize());
				}
			}
		}
		
		files.sort(Comparator.comparingLong(this::modified).reversed());
		String configuredDatabase = "Unknown";
		try
		{
			configuredDatabase = _database.loadPrimary().address().database();
		}
		catch (Exception e)
		{
			// The backup list remains usable when Database.ini is temporarily unavailable.
		}
		
		final List<BackupEntry> entries = new ArrayList<>(files.size());
		for (Path file : files)
		{
			final BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
			entries.add(new BackupEntry(file, databaseFromName(file, configuredDatabase), DATE.format(attributes.lastModifiedTime().toInstant()), formatBytes(attributes.size()), attributes.size() > 0 ? "Ready" : "Invalid"));
		}
		
		return new BackupSnapshot(List.copyOf(entries), null);
	}
	
	private void openFolder()
	{
		try
		{
			final Path directory = _backups.backupDirectory().toAbsolutePath().normalize();
			Files.createDirectories(directory);
			if (!Desktop.isDesktopSupported())
			{
				throw new IllegalStateException("Desktop integration is not available.");
			}
			
			Desktop.getDesktop().open(directory.toFile());
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, "Unable to open the backup directory: " + rootMessage(e), "Backup folder", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void restoreSelected()
	{
		if (!_workloads.isMaintenanceAvailable())
		{
			showBlocked("Database restore");
			return;
		}
		
		final Path backup = selectedFile();
		if (backup == null)
		{
			return;
		}
		
		final int result = JOptionPane.showConfirmDialog(this, "Restore " + backup.getFileName() + " into the configured database? A preventive backup will be created first.", "Confirm database restore", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (result != JOptionPane.YES_OPTION)
		{
			return;
		}
		
		final WorkloadPermit permit = acquireHeavy("Database restore");
		if (permit == null)
		{
			return;
		}
		
		new SwingWorker<Void, Void>()
		{
			@Override
			protected Void doInBackground() throws Exception
			{
				_backups.restore(_database.loadPrimary(), backup);
				return null;
			}
			
			@Override
			protected void done()
			{
				try
				{
					get();
					refresh();
					JOptionPane.showMessageDialog(BackupArchivePanel.this, "Database restore completed.", "Database restore", JOptionPane.INFORMATION_MESSAGE);
				}
				catch (Exception e)
				{
					JOptionPane.showMessageDialog(BackupArchivePanel.this, "Database restore failed: " + rootMessage(e), "Database restore", JOptionPane.ERROR_MESSAGE);
				}
				finally
				{
					permit.close();
				}
			}
		}.execute();
	}
	
	private void deleteSelected()
	{
		if (!_workloads.isMaintenanceAvailable())
		{
			showBlocked("Delete backup");
			return;
		}
		
		final Path selected = selectedFile();
		if (selected == null)
		{
			return;
		}
		
		final Path directory = _backups.backupDirectory().toAbsolutePath().normalize();
		if (!selected.startsWith(directory) || !selected.getFileName().toString().toLowerCase().endsWith(".sql"))
		{
			JOptionPane.showMessageDialog(this, "The selected file is outside the managed backup directory.", "Delete blocked", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		final int result = JOptionPane.showConfirmDialog(this, "Permanently delete " + selected.getFileName() + "?", "Delete backup", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (result != JOptionPane.YES_OPTION)
		{
			return;
		}
		
		final WorkloadPermit permit = acquireHeavy("Delete backup");
		if (permit == null)
		{
			return;
		}
		
		new SwingWorker<Void, Void>()
		{
			@Override
			protected Void doInBackground() throws Exception
			{
				Files.delete(selected);
				return null;
			}
			
			@Override
			protected void done()
			{
				try
				{
					get();
					refresh();
				}
				catch (Exception e)
				{
					JOptionPane.showMessageDialog(BackupArchivePanel.this, "Unable to delete backup: " + rootMessage(e), "Delete backup", JOptionPane.ERROR_MESSAGE);
				}
				finally
				{
					permit.close();
				}
			}
		}.execute();
	}
	
	private WorkloadPermit acquireHeavy(String operation)
	{
		final WorkloadPermit permit = _workloads.tryAcquireHeavy(operation);
		if (permit == null)
		{
			showBlocked(operation);
		}
		
		return permit;
	}
	
	private void showBlocked(String operation)
	{
		JOptionPane.showMessageDialog(this, _workloads.maintenanceBlockReason(), operation + " unavailable", JOptionPane.WARNING_MESSAGE);
	}
	
	private void refreshAvailability()
	{
		final boolean available = _workloads.isMaintenanceAvailable();
		final String reason = _workloads.maintenanceBlockReason();
		_restore.setEnabled(available);
		_delete.setEnabled(available);
		_restore.setToolTipText(available ? null : reason);
		_delete.setToolTipText(available ? null : reason);
	}
	
	private Path selectedFile()
	{
		final int row = _table.getSelectedRow();
		if ((row < 0) || (row >= _files.size()))
		{
			JOptionPane.showMessageDialog(this, "Select a backup first.", "Backup selection", JOptionPane.INFORMATION_MESSAGE);
			return null;
		}
		
		return _files.get(row);
	}
	
	private long modified(Path file)
	{
		try
		{
			return Files.getLastModifiedTime(file).toMillis();
		}
		catch (Exception e)
		{
			return 0;
		}
	}
	
	private static String databaseFromName(Path file, String fallback)
	{
		final String name = file.getFileName().toString();
		final int marker = name.indexOf('_');
		if (marker > 0)
		{
			return name.substring(0, marker);
		}
		
		return fallback;
	}
	
	private static String formatBytes(long bytes)
	{
		if (bytes >= 1073741824L)
		{
			return String.format("%.2f GB", bytes / 1073741824.0);
		}
		
		if (bytes >= 1048576L)
		{
			return String.format("%.2f MB", bytes / 1048576.0);
		}
		
		return String.format("%.2f KB", bytes / 1024.0);
	}
	
	private static String rootMessage(Exception exception)
	{
		Throwable current = exception;
		while ((current.getCause() != null) && (current.getCause() != current))
		{
			current = current.getCause();
		}
		
		return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
	}
}
