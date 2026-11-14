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
package org.l2jmobius.tools.editor.drop.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import org.l2jmobius.tools.editor.drop.model.DataIndex;
import org.l2jmobius.tools.editor.drop.model.DropEditorProfile;
import org.l2jmobius.tools.editor.drop.model.DropGroup;
import org.l2jmobius.tools.editor.drop.model.DropItem;
import org.l2jmobius.tools.editor.drop.model.DropSection;
import org.l2jmobius.tools.editor.drop.model.ItemRecord;
import org.l2jmobius.tools.editor.drop.model.NpcDrops;
import org.l2jmobius.tools.editor.drop.model.NpcRecord;
import org.l2jmobius.tools.editor.drop.service.NpcXmlEditor;
import org.l2jmobius.tools.editor.drop.service.ProjectScanner;

/**
 * Reusable Drop List Editor panel for filtering NPCs and editing their drop lists. <br>
 * The panel is shared by the standalone utility and the Control Dashboard.
 * @author BazookaRpm
 */
public final class DropEditorPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final Dimension HEADER_BADGE_SIZE = new Dimension(150, 24);
	
	private final Path _startPath;
	private final DropEditorProfile _profile = DropEditorProfile.current();
	private final JTextField _searchField = new JTextField(22);
	private final JTextField _minLevelField = new JTextField(4);
	private final JTextField _maxLevelField = new JTextField(4);
	private final JCheckBox _monsterFilter = new JCheckBox("Monster");
	private final JCheckBox _raidFilter = new JCheckBox("RaidBoss");
	private final JCheckBox _grandBossFilter = new JCheckBox("GrandBoss");
	private final JCheckBox _questFilter = new JCheckBox("Quest");
	private final JButton _reloadDataButton = new JButton("Reload Data");
	private final NpcTableModel _npcModel = new NpcTableModel();
	private final JTable _npcTable = new JTable(_npcModel);
	private final DropBlockTableModel _blockModel = new DropBlockTableModel();
	private final JTable _blockTable = new JTable(_blockModel);
	private final DropItemTableModel _dropModel = new DropItemTableModel();
	private final JTable _dropTable = new JTable(_dropModel);
	private final JLabel _npcCount = new JLabel("0 NPCs");
	private final JLabel _blockCount = new JLabel("0 groups");
	private final JLabel _itemCount = new JLabel("0 items");
	private final JLabel _npcTitle = new JLabel("Select an NPC");
	private final JLabel _npcPath = new JLabel(" ");
	private final JLabel _groupContext = new JLabel("Select a drop group.");
	private final JLabel _selectedItemTitle = new JLabel("Select an item");
	private final JLabel _selectedItemMeta = new JLabel("Item details will appear here.");
	private final JLabel _selectedItemExtra = new JLabel(" ");
	private final DropEditorBadge _typesBadge = new DropEditorBadge("", DropEditorPalette.surfaceAlt());
	private final DropEditorBadge _dirtyBadge = new DropEditorBadge("CLEAN", DropEditorPalette.GREEN);
	private final DropEditorBadge _itemChanceBadge = new DropEditorBadge("--", DropEditorPalette.BLUE);
	private final JButton _addButton = new JButton("Add Drop");
	private final JButton _editButton = new JButton("Edit");
	private final JButton _removeButton = new JButton("Remove");
	private final JButton _reloadButton = new JButton("Reload NPC");
	private final JButton _saveButton = new JButton("Save XML");
	private final JLabel _status = new JLabel("Ready to load L2JMobius NPC and item data.");
	private final Timer _filterTimer;
	private final AtomicInteger _npcLoadGeneration = new AtomicInteger();
	private DataIndex _index;
	private NpcRecord _currentNpc;
	private NpcDrops _currentDrops;
	private Integer _pendingNpcId;
	private boolean _dirty;
	private boolean _suppressSelection;
	private boolean _started;
	private int _activeOperations;
	private Consumer<Boolean> _activityListener;
	
	public DropEditorPanel(Path startPath)
	{
		_startPath = startPath.toAbsolutePath().normalize();
		_filterTimer = new Timer(180, e -> applyFilters());
		_filterTimer.setRepeats(false);
		buildUi();
		bindEvents();
		setDropActionsEnabled(false);
		updateDirtyBadge();
	}
	
	/**
	 * Starts the initial NPC and item scan once.
	 */
	public void start()
	{
		if (_started)
		{
			return;
		}
		
		_started = true;
		scanProject();
	}
	
	/**
	 * Registers a listener that receives background activity state changes.
	 * @param listener activity listener, or {@code null}
	 */
	public void setActivityListener(Consumer<Boolean> listener)
	{
		_activityListener = listener;
	}
	
	public void selectNpc(int npcId)
	{
		_pendingNpcId = Integer.valueOf(npcId);
		_searchField.setText(Integer.toString(npcId));
		if ((_index != null) && !isBusy())
		{
			selectPendingNpc();
		}
	}
	
	/**
	 * Returns whether a background load or save operation is active.
	 * @return {@code true} while editor work is active
	 */
	public boolean isBusy()
	{
		return _activeOperations > 0;
	}
	
	/**
	 * Returns whether the selected NPC has unsaved drop changes.
	 * @return {@code true} when changes are pending
	 */
	public boolean hasUnsavedChanges()
	{
		return _dirty;
	}
	
	/**
	 * Discards the current in-memory modifications without touching the XML file.
	 */
	public void discardUnsavedChanges()
	{
		_dirty = false;
		updateDirtyBadge();
	}
	
	private void buildUi()
	{
		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder());
		add(buildTopPanel(), BorderLayout.NORTH);
		
		configureTable(_npcTable, true);
		configureTable(_blockTable, false);
		configureTable(_dropTable, false);
		configureColumnWidths();
		configureDropColumns();
		configureSemanticRenderers();
		
		final JPanel npcPanel = buildNpcSection();
		final JPanel blockPanel = buildGroupSection();
		final JPanel itemPanel = buildItemSection();
		
		final JSplitPane navigationSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, npcPanel, blockPanel);
		navigationSplit.setResizeWeight(0.56);
		navigationSplit.setDividerLocation(0.56);
		navigationSplit.setContinuousLayout(true);
		navigationSplit.setBorder(null);
		navigationSplit.setDividerSize(7);
		
		final JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navigationSplit, itemPanel);
		mainSplit.setResizeWeight(0.56);
		mainSplit.setDividerLocation(0.56);
		mainSplit.setContinuousLayout(true);
		mainSplit.setBorder(null);
		mainSplit.setDividerSize(7);
		add(mainSplit, BorderLayout.CENTER);
		
		_status.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DropEditorPalette.border()), BorderFactory.createEmptyBorder(6, 4, 0, 4)));
		_status.setForeground(DropEditorPalette.muted());
		add(_status, BorderLayout.SOUTH);
	}
	
	private JPanel buildTopPanel()
	{
		final JPanel panel = new JPanel(new BorderLayout(0, 8));
		panel.add(buildHeroPanel(), BorderLayout.NORTH);
		panel.add(buildFilterPanel(), BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel buildHeroPanel()
	{
		final JPanel panel = new JPanel(new BorderLayout(12, 0));
		panel.setBorder(BorderFactory.createEmptyBorder());
		final JPanel titles = new JPanel(new BorderLayout(0, 1));
		final JLabel title = new JLabel("Drop List Editor");
		title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
		title.setForeground(UIManager.getColor("Label.foreground"));
		final JLabel subtitle = new JLabel("Edit NPC drop groups and item chances directly in the active L2JMobius datapack.");
		subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
		subtitle.setForeground(UIManager.getColor("Label.disabledForeground"));
		titles.add(title, BorderLayout.NORTH);
		titles.add(subtitle, BorderLayout.SOUTH);
		panel.add(titles, BorderLayout.WEST);
		
		_typesBadge.setBadge(_profile.getSupportedTypesLabel(), DropEditorPalette.surfaceAlt());
		_typesBadge.setPreferredSize(HEADER_BADGE_SIZE);
		_dirtyBadge.setPreferredSize(HEADER_BADGE_SIZE);
		final JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 3));
		badges.add(_typesBadge);
		badges.add(_dirtyBadge);
		panel.add(badges, BorderLayout.EAST);
		return panel;
	}
	
	private JPanel buildFilterPanel()
	{
		final JPanel panel = createCard(new FlowLayout(FlowLayout.LEFT, 7, 6));
		final JLabel label = new JLabel("NPC filters");
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		panel.add(label);
		panel.add(new JLabel("NPC / ID:"));
		_searchField.setToolTipText("Search by NPC ID, name, title, or type.");
		panel.add(_searchField);
		panel.add(new JLabel("Min Lv:"));
		panel.add(_minLevelField);
		panel.add(new JLabel("Max Lv:"));
		panel.add(_maxLevelField);
		panel.add(_monsterFilter);
		panel.add(_raidFilter);
		panel.add(_grandBossFilter);
		panel.add(_questFilter);
		styleSecondaryButton(_reloadDataButton);
		panel.add(_reloadDataButton);
		return panel;
	}
	
	private JPanel buildNpcSection()
	{
		final JScrollPane scroll = new JScrollPane(_npcTable);
		styleScrollPane(scroll);
		return buildTableCard("1. NPC list", _npcCount, scroll);
	}
	
	private JPanel buildGroupSection()
	{
		final JPanel panel = createCard(new BorderLayout(0, 7));
		panel.add(buildSectionHeader("2. Drop groups", _blockCount), BorderLayout.NORTH);
		final JPanel center = new JPanel(new BorderLayout(0, 7));
		center.setOpaque(false);
		center.add(buildNpcHeader(), BorderLayout.NORTH);
		final JScrollPane scroll = new JScrollPane(_blockTable);
		styleScrollPane(scroll);
		center.add(scroll, BorderLayout.CENTER);
		panel.add(center, BorderLayout.CENTER);
		_groupContext.setBorder(BorderFactory.createEmptyBorder(3, 2, 0, 2));
		_groupContext.setForeground(DropEditorPalette.muted());
		panel.add(_groupContext, BorderLayout.SOUTH);
		return panel;
	}
	
	private JPanel buildItemSection()
	{
		final JPanel panel = createCard(new BorderLayout(0, 7));
		panel.add(buildSectionHeader("3. Items in selected group", _itemCount), BorderLayout.NORTH);
		final JPanel center = new JPanel(new BorderLayout(0, 7));
		center.setOpaque(false);
		final JScrollPane scroll = new JScrollPane(_dropTable);
		styleScrollPane(scroll);
		center.add(scroll, BorderLayout.CENTER);
		center.add(buildSelectedItemPanel(), BorderLayout.SOUTH);
		panel.add(center, BorderLayout.CENTER);
		panel.add(buildDropActions(), BorderLayout.SOUTH);
		return panel;
	}
	
	private JPanel buildTableCard(String title, JLabel count, JScrollPane scroll)
	{
		final JPanel panel = createCard(new BorderLayout(0, 7));
		panel.add(buildSectionHeader(title, count), BorderLayout.NORTH);
		panel.add(scroll, BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel buildSectionHeader(String title, JLabel count)
	{
		final JPanel panel = new JPanel(new BorderLayout(6, 0));
		panel.setOpaque(false);
		final JLabel label = new JLabel(title);
		label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D() + 1f));
		count.setHorizontalAlignment(SwingConstants.RIGHT);
		count.setForeground(DropEditorPalette.muted());
		panel.add(label, BorderLayout.WEST);
		panel.add(count, BorderLayout.EAST);
		return panel;
	}
	
	private JPanel buildNpcHeader()
	{
		final JPanel panel = new JPanel(new GridBagLayout());
		panel.setOpaque(true);
		panel.setBackground(DropEditorPalette.surface());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DropEditorPalette.border()), BorderFactory.createEmptyBorder(7, 8, 7, 8)));
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.weightx = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		_npcTitle.setFont(_npcTitle.getFont().deriveFont(Font.BOLD, _npcTitle.getFont().getSize2D() + 1f));
		panel.add(_npcTitle, constraints);
		constraints.gridy = 1;
		constraints.insets = new Insets(3, 0, 0, 0);
		_npcPath.setFont(_npcPath.getFont().deriveFont(Math.max(10f, _npcPath.getFont().getSize2D() - 1f)));
		_npcPath.setForeground(DropEditorPalette.muted());
		panel.add(_npcPath, constraints);
		return panel;
	}
	
	private JPanel buildSelectedItemPanel()
	{
		final JPanel panel = new JPanel(new BorderLayout(8, 0));
		panel.setOpaque(true);
		panel.setBackground(DropEditorPalette.surface());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DropEditorPalette.border()), BorderFactory.createEmptyBorder(8, 9, 8, 9)));
		final JPanel text = new JPanel(new BorderLayout(0, 2));
		text.setOpaque(false);
		_selectedItemTitle.setFont(_selectedItemTitle.getFont().deriveFont(Font.BOLD));
		_selectedItemMeta.setForeground(DropEditorPalette.muted());
		_selectedItemExtra.setForeground(DropEditorPalette.muted());
		text.add(_selectedItemTitle, BorderLayout.NORTH);
		final JPanel meta = new JPanel(new BorderLayout(0, 1));
		meta.setOpaque(false);
		meta.add(_selectedItemMeta, BorderLayout.NORTH);
		meta.add(_selectedItemExtra, BorderLayout.SOUTH);
		text.add(meta, BorderLayout.CENTER);
		panel.add(text, BorderLayout.CENTER);
		panel.add(_itemChanceBadge, BorderLayout.EAST);
		return panel;
	}
	
	private JPanel buildDropActions()
	{
		final JPanel panel = new JPanel(new BorderLayout(6, 0));
		panel.setOpaque(false);
		final JPanel editActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		editActions.setOpaque(false);
		stylePrimaryButton(_addButton, DropEditorPalette.ACCENT);
		styleSecondaryButton(_editButton);
		stylePrimaryButton(_removeButton, DropEditorPalette.RED);
		editActions.add(_addButton);
		editActions.add(_editButton);
		editActions.add(_removeButton);
		final JPanel fileActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		fileActions.setOpaque(false);
		styleSecondaryButton(_reloadButton);
		stylePrimaryButton(_saveButton, DropEditorPalette.GREEN);
		fileActions.add(_reloadButton);
		fileActions.add(_saveButton);
		panel.add(editActions, BorderLayout.WEST);
		panel.add(fileActions, BorderLayout.EAST);
		return panel;
	}
	
	private JPanel createCard(LayoutManager layout)
	{
		final JPanel panel = new JPanel(layout);
		panel.setOpaque(true);
		panel.setBackground(DropEditorPalette.surface());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DropEditorPalette.border()), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
		return panel;
	}
	
	private void styleScrollPane(JScrollPane scrollPane)
	{
		scrollPane.setBorder(BorderFactory.createLineBorder(DropEditorPalette.border()));
		scrollPane.getViewport().setBackground(DropEditorPalette.surface());
		scrollPane.getVerticalScrollBar().setUnitIncrement(18);
	}
	
	private void stylePrimaryButton(JButton button, Color background)
	{
		button.setFocusPainted(false);
		button.setOpaque(true);
		button.setContentAreaFilled(true);
		button.setBackground(background);
		button.setForeground(DropEditorPalette.badgeForeground(background));
		button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DropEditorPalette.blend(background, Color.WHITE, 0.18f)), BorderFactory.createEmptyBorder(5, 10, 5, 10)));
	}
	
	private void styleSecondaryButton(JButton button)
	{
		button.setFocusPainted(false);
		button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DropEditorPalette.border()), BorderFactory.createEmptyBorder(5, 10, 5, 10)));
	}
	
	private void configureTable(JTable table, boolean sortable)
	{
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setFillsViewportHeight(true);
		table.setAutoCreateRowSorter(sortable);
		table.setRowHeight(Math.max(24, table.getRowHeight() + 5));
		table.setIntercellSpacing(new Dimension(1, 1));
		table.setShowVerticalLines(true);
		table.setShowHorizontalLines(true);
		final JTableHeader header = table.getTableHeader();
		header.setReorderingAllowed(false);
		header.setFont(header.getFont().deriveFont(Font.BOLD));
	}
	
	private void configureColumnWidths()
	{
		_npcTable.getColumnModel().getColumn(0).setPreferredWidth(62);
		_npcTable.getColumnModel().getColumn(1).setPreferredWidth(165);
		_npcTable.getColumnModel().getColumn(2).setPreferredWidth(42);
		_npcTable.getColumnModel().getColumn(3).setPreferredWidth(92);
		_npcTable.getColumnModel().getColumn(4).setPreferredWidth(48);
		_blockTable.getColumnModel().getColumn(0).setPreferredWidth(70);
		_blockTable.getColumnModel().getColumn(1).setPreferredWidth(72);
		_blockTable.getColumnModel().getColumn(2).setPreferredWidth(48);
		_blockTable.getColumnModel().getColumn(3).setPreferredWidth(78);
	}
	
	private void configureDropColumns()
	{
		_dropTable.getColumnModel().getColumn(0).setPreferredWidth(72);
		_dropTable.getColumnModel().getColumn(1).setPreferredWidth(210);
		_dropTable.getColumnModel().getColumn(2).setPreferredWidth(54);
		_dropTable.getColumnModel().getColumn(3).setPreferredWidth(54);
		_dropTable.getColumnModel().getColumn(4).setPreferredWidth(78);
		if (!_profile.hasExtendedAttributes())
		{
			removeColumn("Min Lv");
			removeColumn("Max Lv");
			removeColumn("Daily");
		}
	}
	
	private void configureSemanticRenderers()
	{
		final PercentageCellRenderer percentageRenderer = new PercentageCellRenderer();
		_blockTable.getColumn("Group %").setCellRenderer(percentageRenderer);
		_dropTable.getColumn("Chance %").setCellRenderer(new PercentageCellRenderer());
		final DefaultTableCellRenderer centered = new DefaultTableCellRenderer();
		centered.setHorizontalAlignment(SwingConstants.CENTER);
		_npcTable.getColumnModel().getColumn(0).setCellRenderer(centered);
		_npcTable.getColumnModel().getColumn(2).setCellRenderer(centered);
		_blockTable.getColumnModel().getColumn(1).setCellRenderer(centered);
		_blockTable.getColumnModel().getColumn(2).setCellRenderer(centered);
		_dropTable.getColumnModel().getColumn(0).setCellRenderer(centered);
		_dropTable.getColumnModel().getColumn(2).setCellRenderer(centered);
		_dropTable.getColumnModel().getColumn(3).setCellRenderer(centered);
	}
	
	private void removeColumn(String name)
	{
		_dropTable.removeColumn(_dropTable.getColumn(name));
	}
	
	private void bindEvents()
	{
		_reloadDataButton.addActionListener(e -> scanProject());
		_addButton.addActionListener(e -> addDrop());
		_editButton.addActionListener(e -> editDrop());
		_removeButton.addActionListener(e -> removeDrop());
		_reloadButton.addActionListener(e -> reloadCurrentNpc());
		_saveButton.addActionListener(e -> saveCurrentNpc());
		_dropTable.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if ((e.getClickCount() == 2) && (_dropTable.getSelectedRow() >= 0))
				{
					editDrop();
				}
			}
		});
		_npcTable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (!e.getValueIsAdjusting() && !_suppressSelection)
				{
					onNpcSelectionChanged();
				}
			}
		});
		_blockTable.getSelectionModel().addListSelectionListener(e -> onBlockSelectionChanged());
		_dropTable.getSelectionModel().addListSelectionListener(e -> onDropSelectionChanged());
		bindFilter(_searchField);
		bindFilter(_minLevelField);
		bindFilter(_maxLevelField);
		_monsterFilter.addActionListener(e -> scheduleFilter());
		_raidFilter.addActionListener(e -> scheduleFilter());
		_grandBossFilter.addActionListener(e -> scheduleFilter());
		_questFilter.addActionListener(e -> scheduleFilter());
	}
	
	private void bindFilter(JTextField field)
	{
		field.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				scheduleFilter();
			}
			
			@Override
			public void removeUpdate(DocumentEvent e)
			{
				scheduleFilter();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e)
			{
				scheduleFilter();
			}
		});
	}
	
	private void scanProject()
	{
		if (!confirmDiscardOrSave())
		{
			return;
		}
		
		beginOperation();
		setBusy(true, "Scanning NPC and item XML...");
		_currentNpc = null;
		_currentDrops = null;
		_dirty = false;
		updateDirtyBadge();
		_npcModel.setNpcs(new ArrayList<>());
		_blockModel.setBlocks(new ArrayList<>());
		_dropModel.setRows(new ArrayList<>());
		_npcTitle.setText("Select an NPC");
		_npcPath.setText(" ");
		_groupContext.setText("Select a drop group.");
		clearSelectedItem();
		updateCounters();
		setDropActionsEnabled(false);
		new SwingWorker<DataIndex, Void>()
		{
			@Override
			protected DataIndex doInBackground() throws Exception
			{
				return new ProjectScanner().scan(_startPath);
			}
			
			@Override
			protected void done()
			{
				try
				{
					_index = get();
					_npcModel.setNpcs(new ArrayList<>());
					updateCounters();
					_status.setText("Loaded " + _index.getNpcs().size() + " NPCs. Lazy item lookup ready across " + _index.getItemFileCount() + " XML files.");
					selectPendingNpc();
				}
				catch (Exception e)
				{
					_index = null;
					_npcModel.setNpcs(new ArrayList<>());
					updateCounters();
					showError("Unable to load L2JMobius data", rootMessage(e));
				}
				finally
				{
					setBusy(false, null);
					endOperation();
				}
			}
		}.execute();
	}
	
	private void scheduleFilter()
	{
		_filterTimer.restart();
	}
	
	private void applyFilters()
	{
		if (_index == null)
		{
			return;
		}
		
		final Integer minLevel = parseOptionalInt(_minLevelField.getText());
		final Integer maxLevel = parseOptionalInt(_maxLevelField.getText());
		if (((minLevel == null) && !_minLevelField.getText().trim().isEmpty()) || ((maxLevel == null) && !_maxLevelField.getText().trim().isEmpty()))
		{
			_status.setText("Invalid level filter.");
			_npcModel.setNpcs(new ArrayList<>());
			updateCounters();
			return;
		}
		
		final String query = _searchField.getText().trim().toLowerCase(Locale.ROOT);
		final boolean categoryFilter = _monsterFilter.isSelected() || _raidFilter.isSelected() || _grandBossFilter.isSelected() || _questFilter.isSelected();
		if (query.isEmpty() && (minLevel == null) && (maxLevel == null) && !categoryFilter)
		{
			_npcModel.setNpcs(new ArrayList<>());
			updateCounters();
			_status.setText("Enter an NPC/ID, level range, or category filter to display NPCs.");
			return;
		}
		
		final List<NpcRecord> filtered = new ArrayList<>();
		for (NpcRecord npc : _index.getNpcs())
		{
			if ((minLevel != null) && (npc.getLevel() < minLevel.intValue()))
			{
				continue;
			}
			
			if ((maxLevel != null) && (npc.getLevel() > maxLevel.intValue()))
			{
				continue;
			}
			
			if (categoryFilter && !((_monsterFilter.isSelected() && npc.isMonster()) || (_raidFilter.isSelected() && npc.isRaidBoss()) || (_grandBossFilter.isSelected() && npc.isGrandBoss()) || (_questFilter.isSelected() && npc.isQuestMonster())))
			{
				continue;
			}
			
			if (!query.isEmpty())
			{
				if (!npc.matchesSearch(query))
				{
					continue;
				}
			}
			
			filtered.add(npc);
		}
		
		_npcModel.setNpcs(filtered);
		updateCounters();
		_status.setText("Showing " + filtered.size() + " of " + _index.getNpcs().size() + " NPCs.");
	}
	
	private void selectPendingNpc()
	{
		if ((_pendingNpcId == null) || (_index == null))
		{
			return;
		}
		
		final int npcId = _pendingNpcId.intValue();
		_searchField.setText(Integer.toString(npcId));
		applyFilters();
		for (int modelRow = 0; modelRow < _npcModel.getRowCount(); modelRow++)
		{
			if (_npcModel.getNpc(modelRow).getId() == npcId)
			{
				final int viewRow = _npcTable.convertRowIndexToView(modelRow);
				if (viewRow >= 0)
				{
					_pendingNpcId = null;
					_npcTable.setRowSelectionInterval(viewRow, viewRow);
					_npcTable.scrollRectToVisible(_npcTable.getCellRect(viewRow, 0, true));
					return;
				}
			}
		}
		
		_status.setText("NPC " + npcId + " was not found in the active datapack.");
		_pendingNpcId = null;
	}
	
	private Integer parseOptionalInt(String value)
	{
		final String trimmed = value.trim();
		if (trimmed.isEmpty())
		{
			return null;
		}
		
		try
		{
			return Integer.valueOf(trimmed);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}
	
	private void onNpcSelectionChanged()
	{
		final int viewRow = _npcTable.getSelectedRow();
		if (viewRow < 0)
		{
			return;
		}
		
		final NpcRecord selected = _npcModel.getNpc(_npcTable.convertRowIndexToModel(viewRow));
		if ((_currentNpc != null) && (_currentNpc.getId() == selected.getId()))
		{
			return;
		}
		
		if (_dirty && !confirmDiscardOrSave())
		{
			reselectCurrentNpc();
			return;
		}
		
		loadNpcDrops(selected);
	}
	
	private void loadNpcDrops(NpcRecord npc)
	{
		if (_index == null)
		{
			return;
		}
		
		final int generation = _npcLoadGeneration.incrementAndGet();
		beginOperation();
		setDropActionsEnabled(false);
		_status.setText("Loading NPC " + npc.getId() + "...");
		new SwingWorker<NpcDrops, Void>()
		{
			@Override
			protected NpcDrops doInBackground() throws Exception
			{
				return new NpcXmlEditor().loadDrops(npc, _index);
			}
			
			@Override
			protected void done()
			{
				try
				{
					if (generation != _npcLoadGeneration.get())
					{
						return;
					}
					
					_currentDrops = get();
					_currentNpc = npc;
					_dirty = false;
					updateDirtyBadge();
					refreshDropViews();
					_npcTitle.setText(npc.getId() + " - " + npc.getName() + "  •  Lv " + npc.getLevel() + "  •  " + npc.getType() + (npc.isQuestMonster() ? "  •  Quest" : ""));
					_npcPath.setText(npc.getFile().toString());
					setDropActionsEnabled(true);
					_status.setText("Loaded NPC " + npc.getId() + ". Changes are kept in memory until Save XML.");
				}
				catch (Exception e)
				{
					showError("Unable to load NPC drops", rootMessage(e));
				}
				finally
				{
					endOperation();
				}
			}
		}.execute();
	}
	
	private void refreshDropViews()
	{
		final List<DropBlock> blocks = new ArrayList<>();
		if (_currentDrops != null)
		{
			for (DropSection section : _currentDrops.getSections())
			{
				for (int groupIndex = 0; groupIndex < section.getGroups().size(); groupIndex++)
				{
					final DropGroup group = section.getGroups().get(groupIndex);
					if (!group.getItems().isEmpty())
					{
						blocks.add(new DropBlock(section, group, groupIndex));
					}
				}
				
				if (!section.getItems().isEmpty())
				{
					blocks.add(new DropBlock(section, null, -1));
				}
			}
		}
		
		_blockModel.setBlocks(blocks);
		_blockTable.clearSelection();
		_dropModel.setRows(new ArrayList<>());
		clearSelectedItem();
		if (!blocks.isEmpty())
		{
			_blockTable.getSelectionModel().setSelectionInterval(0, 0);
		}
		else
		{
			_groupContext.setText("No drops available for this NPC.");
		}
		
		updateCounters();
		updateDropSelectionActions();
	}
	
	private void onBlockSelectionChanged()
	{
		final int row = _blockTable.getSelectedRow();
		if (row < 0)
		{
			_dropModel.setRows(new ArrayList<>());
			_groupContext.setText(_blockModel.getRowCount() == 0 ? "No drops available for this NPC." : "Select a drop group.");
			clearSelectedItem();
			updateCounters();
			updateDropSelectionActions();
			return;
		}
		
		final DropBlock block = _blockModel.getBlock(row);
		final List<DropRow> rows = new ArrayList<>();
		final List<DropItem> items = block.getItems();
		for (DropItem item : items)
		{
			rows.add(new DropRow(block._section, block._group, block._groupIndex, item));
		}
		
		_dropModel.setRows(rows);
		_dropTable.clearSelection();
		clearSelectedItem();
		if (block._group == null)
		{
			_groupContext.setText(block._section.getType().name() + "  •  Direct items  •  " + items.size() + " items");
		}
		else
		{
			_groupContext.setText(block._section.getType().name() + "  •  Group " + (block._groupIndex + 1) + "  •  Group chance " + block._group.getChance() + "%  •  " + items.size() + " items");
		}
		
		updateCounters();
		updateDropSelectionActions();
	}
	
	private void onDropSelectionChanged()
	{
		updateDropSelectionActions();
		final int row = _dropTable.getSelectedRow();
		if (row < 0)
		{
			clearSelectedItem();
			return;
		}
		
		final DropRow dropRow = _dropModel.getRow(row);
		final DropItem item = dropRow._item;
		final ItemRecord record = _index == null ? null : _index.getItem(item.getItemId());
		_selectedItemTitle.setText(item.getItemId() + " - " + (record == null ? "Unknown item" : record.getName()));
		final String groupName = dropRow._group == null ? "Direct" : "G" + (dropRow._groupIndex + 1);
		_selectedItemMeta.setText("Type: " + dropRow._section.getType().name() + "  •  Group: " + groupName + "  •  Amount: " + item.getMin() + " - " + item.getMax());
		if (_profile.supportsExtendedAttributes(dropRow._section.getType()))
		{
			_selectedItemExtra.setText("Min Lv: " + safeValue(item.getMinLevel()) + "  •  Max Lv: " + safeValue(item.getMaxLevel()) + "  •  Daily: " + safeValue(item.getDailyLimit()));
		}
		else
		{
			_selectedItemExtra.setText(dropRow._group == null ? "Ungrouped drop entry." : "Group chance: " + dropRow._group.getChance() + "%");
		}
		
		final Double percentage = parsePercentage(item.getChance());
		_itemChanceBadge.setBadge(item.getChance() + "%", percentage == null ? DropEditorPalette.BLUE : DropEditorPalette.percentageFill(percentage.doubleValue()));
	}
	
	private String safeValue(String value)
	{
		return (value == null) || value.isEmpty() ? "-" : value;
	}
	
	private Double parsePercentage(String value)
	{
		try
		{
			final double parsed = Double.parseDouble(value.trim());
			return Double.isFinite(parsed) ? Double.valueOf(parsed) : null;
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	private void clearSelectedItem()
	{
		_selectedItemTitle.setText("Select an item");
		_selectedItemMeta.setText("Item details will appear here.");
		_selectedItemExtra.setText(" ");
		_itemChanceBadge.setBadge("--", DropEditorPalette.BLUE);
	}
	
	private void addDrop()
	{
		if ((_index == null) || (_currentDrops == null))
		{
			return;
		}
		
		final DropItemDialog dialog = new DropItemDialog(ownerWindow(), _index, _currentDrops, _profile);
		dialog.setVisible(true);
		if (dialog.isAccepted())
		{
			_dirty = true;
			updateDirtyBadge();
			refreshDropViews();
			updateDirtyStatus();
		}
	}
	
	private void editDrop()
	{
		if ((_index == null) || (_currentDrops == null))
		{
			return;
		}
		
		final int row = _dropTable.getSelectedRow();
		if (row < 0)
		{
			return;
		}
		
		final DropRow dropRow = _dropModel.getRow(row);
		final DropEditDialog dialog = new DropEditDialog(ownerWindow(), _index, dropRow._section.getType(), dropRow._group, dropRow._item);
		dialog.setVisible(true);
		if (dialog.isAccepted())
		{
			_dirty = true;
			updateDirtyBadge();
			refreshDropViews();
			updateDirtyStatus();
		}
	}
	
	private void removeDrop()
	{
		if (_currentDrops == null)
		{
			return;
		}
		
		final int row = _dropTable.getSelectedRow();
		if (row < 0)
		{
			return;
		}
		
		final DropRow dropRow = _dropModel.getRow(row);
		final ItemRecord record = _index.getItem(dropRow._item.getItemId());
		final String itemName = record == null ? "" : " - " + record.getName();
		if (JOptionPane.showConfirmDialog(this, "Remove item " + dropRow._item.getItemId() + itemName + " from this NPC?", "Remove drop", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)
		{
			return;
		}
		
		if (dropRow._group != null)
		{
			dropRow._group.getItems().remove(dropRow._item);
			if (dropRow._group.getItems().isEmpty())
			{
				dropRow._section.getGroups().remove(dropRow._group);
			}
		}
		else
		{
			dropRow._section.getItems().remove(dropRow._item);
		}
		
		_currentDrops.removeEmptySections();
		_dirty = true;
		updateDirtyBadge();
		refreshDropViews();
		updateDirtyStatus();
	}
	
	private void reloadCurrentNpc()
	{
		if (_currentNpc == null)
		{
			return;
		}
		
		if (_dirty && (JOptionPane.showConfirmDialog(this, "Discard unsaved drop changes and reload the XML?", "Reload NPC", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION))
		{
			return;
		}
		
		_dirty = false;
		updateDirtyBadge();
		loadNpcDrops(_currentNpc);
	}
	
	private boolean saveCurrentNpc()
	{
		if ((_index == null) || (_currentNpc == null) || (_currentDrops == null))
		{
			return true;
		}
		
		if (!_dirty)
		{
			_status.setText("No pending changes for NPC " + _currentNpc.getId() + ".");
			return true;
		}
		
		beginOperation();
		setBusy(true, "Validating and saving " + _currentNpc.getFile().getFileName() + "...");
		try
		{
			final Path backup = new NpcXmlEditor().saveDrops(_currentNpc, _currentDrops, _index);
			_dirty = false;
			updateDirtyBadge();
			_status.setText("Saved NPC " + _currentNpc.getId() + ". Backup: " + (backup == null ? "not required" : backup));
			return true;
		}
		catch (Exception e)
		{
			showError("Unable to save NPC XML", rootMessage(e));
			return false;
		}
		finally
		{
			setBusy(false, null);
			setDropActionsEnabled(true);
			endOperation();
		}
	}
	
	public boolean confirmDiscardOrSave()
	{
		if (!_dirty)
		{
			return true;
		}
		
		final Object[] options =
		{
			"Save XML",
			"Discard",
			"Cancel"
		};
		final int result = JOptionPane.showOptionDialog(this, "NPC " + (_currentNpc == null ? "" : _currentNpc.getId()) + " has unsaved drop changes.", "Unsaved changes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
		if (result == 0)
		{
			return saveCurrentNpc();
		}
		
		if (result == 1)
		{
			_dirty = false;
			updateDirtyBadge();
			return true;
		}
		
		return false;
	}
	
	private void reselectCurrentNpc()
	{
		if (_currentNpc == null)
		{
			return;
		}
		
		_suppressSelection = true;
		try
		{
			for (int i = 0; i < _npcModel.getRowCount(); i++)
			{
				if (_npcModel.getNpc(i).getId() == _currentNpc.getId())
				{
					final int view = _npcTable.convertRowIndexToView(i);
					if (view >= 0)
					{
						_npcTable.getSelectionModel().setSelectionInterval(view, view);
					}
					break;
				}
			}
		}
		finally
		{
			_suppressSelection = false;
		}
	}
	
	private void updateDirtyStatus()
	{
		if (_currentNpc != null)
		{
			_status.setText("NPC " + _currentNpc.getId() + " has unsaved changes. Save XML writes directly to its stats/npcs file.");
		}
	}
	
	private void updateDirtyBadge()
	{
		if (_dirty)
		{
			_dirtyBadge.setBadge("UNSAVED", DropEditorPalette.ORANGE);
		}
		else
		{
			_dirtyBadge.setBadge("CLEAN", DropEditorPalette.GREEN);
		}
	}
	
	private void updateDropSelectionActions()
	{
		final boolean selected = _dropTable.getSelectedRow() >= 0;
		_editButton.setEnabled((_currentDrops != null) && selected);
		_removeButton.setEnabled((_currentDrops != null) && selected);
	}
	
	private void updateCounters()
	{
		_npcCount.setText(_npcModel.getRowCount() + " NPCs");
		_blockCount.setText(_blockModel.getRowCount() + " groups");
		_itemCount.setText(_dropModel.getRowCount() + " items");
	}
	
	private void setDropActionsEnabled(boolean enabled)
	{
		_addButton.setEnabled(enabled);
		_reloadButton.setEnabled(enabled);
		_saveButton.setEnabled(enabled);
		_editButton.setEnabled(enabled && (_dropTable.getSelectedRow() >= 0));
		_removeButton.setEnabled(enabled && (_dropTable.getSelectedRow() >= 0));
	}
	
	private void setBusy(boolean busy, String message)
	{
		setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
		_reloadDataButton.setEnabled(!busy);
		if (busy && (message != null))
		{
			_status.setText(message);
		}
	}
	
	private Window ownerWindow()
	{
		return SwingUtilities.getWindowAncestor(this);
	}
	
	private void beginOperation()
	{
		_activeOperations++;
		if ((_activeOperations == 1) && (_activityListener != null))
		{
			_activityListener.accept(true);
		}
	}
	
	private void endOperation()
	{
		if (_activeOperations > 0)
		{
			_activeOperations--;
		}
		
		if ((_activeOperations == 0) && (_activityListener != null))
		{
			_activityListener.accept(false);
		}
	}
	
	private void showError(String title, String message)
	{
		_status.setText(title + ": " + message);
		JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
	}
	
	private String rootMessage(Throwable throwable)
	{
		Throwable current = throwable;
		while ((current.getCause() != null) && (current.getCause() != current))
		{
			current = current.getCause();
		}
		
		return current.getMessage() == null ? current.toString() : current.getMessage();
	}
	
	/**
	 * Provides NPC browser rows for the Drop List Editor.
	 */
	private static final class NpcTableModel extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;
		private final String[] _columns =
		{
			"ID",
			"Name",
			"Lv",
			"Type",
			"Quest"
		};
		private List<NpcRecord> _npcs = new ArrayList<>();
		
		public void setNpcs(List<NpcRecord> npcs)
		{
			_npcs = npcs;
			fireTableDataChanged();
		}
		
		public NpcRecord getNpc(int row)
		{
			return _npcs.get(row);
		}
		
		@Override
		public int getRowCount()
		{
			return _npcs.size();
		}
		
		@Override
		public int getColumnCount()
		{
			return _columns.length;
		}
		
		@Override
		public String getColumnName(int column)
		{
			return _columns[column];
		}
		
		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			if ((columnIndex == 0) || (columnIndex == 2))
			{
				return Integer.class;
			}
			
			if (columnIndex == 4)
			{
				return Boolean.class;
			}
			
			return String.class;
		}
		
		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			final NpcRecord npc = _npcs.get(rowIndex);
			switch (columnIndex)
			{
				case 0:
				{
					return npc.getId();
				}
				case 1:
				{
					return npc.getName();
				}
				case 2:
				{
					return npc.getLevel();
				}
				case 3:
				{
					return npc.getType();
				}
				case 4:
				{
					return npc.isQuestMonster();
				}
				default:
				{
					return "";
				}
			}
		}
	}
	
	/**
	 * Provides direct and grouped drop-block rows for the selected NPC.
	 */
	private static final class DropBlockTableModel extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;
		private final String[] _columns =
		{
			"Type",
			"Group",
			"Items",
			"Group %"
		};
		private List<DropBlock> _blocks = new ArrayList<>();
		
		public void setBlocks(List<DropBlock> blocks)
		{
			_blocks = blocks;
			fireTableDataChanged();
		}
		
		public DropBlock getBlock(int row)
		{
			return _blocks.get(row);
		}
		
		@Override
		public int getRowCount()
		{
			return _blocks.size();
		}
		
		@Override
		public int getColumnCount()
		{
			return _columns.length;
		}
		
		@Override
		public String getColumnName(int column)
		{
			return _columns[column];
		}
		
		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			final DropBlock block = _blocks.get(rowIndex);
			switch (columnIndex)
			{
				case 0:
				{
					return block._section.getType().name();
				}
				case 1:
				{
					return block._group == null ? "Direct" : "G" + (block._groupIndex + 1);
				}
				case 2:
				{
					return block.getItems().size();
				}
				case 3:
				{
					return block._group == null ? "-" : block._group.getChance();
				}
				default:
				{
					return "";
				}
			}
		}
	}
	
	/**
	 * Provides drop-item rows for the currently selected drop block.
	 */
	private final class DropItemTableModel extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;
		private final String[] _columns =
		{
			"Item ID",
			"Item",
			"Min",
			"Max",
			"Chance %",
			"Min Lv",
			"Max Lv",
			"Daily"
		};
		private List<DropRow> _rows = new ArrayList<>();
		
		public void setRows(List<DropRow> rows)
		{
			_rows = rows;
			fireTableDataChanged();
		}
		
		public DropRow getRow(int row)
		{
			return _rows.get(row);
		}
		
		@Override
		public int getRowCount()
		{
			return _rows.size();
		}
		
		@Override
		public int getColumnCount()
		{
			return _columns.length;
		}
		
		@Override
		public String getColumnName(int column)
		{
			return _columns[column];
		}
		
		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			final DropRow row = _rows.get(rowIndex);
			switch (columnIndex)
			{
				case 0:
				{
					return row._item.getItemId();
				}
				case 1:
				{
					final ItemRecord item = _index == null ? null : _index.getItem(row._item.getItemId());
					return item == null ? "Unknown" : item.getName();
				}
				case 2:
				{
					return row._item.getMin();
				}
				case 3:
				{
					return row._item.getMax();
				}
				case 4:
				{
					return row._item.getChance();
				}
				case 5:
				{
					return row._item.getMinLevel() == null ? "" : row._item.getMinLevel();
				}
				case 6:
				{
					return row._item.getMaxLevel() == null ? "" : row._item.getMaxLevel();
				}
				case 7:
				{
					return row._item.getDailyLimit() == null ? "" : row._item.getDailyLimit();
				}
				default:
				{
					return "";
				}
			}
		}
	}
	
	/**
	 * Associates one drop section with an optional group and its display index.
	 */
	private static final class DropBlock
	{
		private final DropSection _section;
		private final DropGroup _group;
		private final int _groupIndex;
		
		private DropBlock(DropSection section, DropGroup group, int groupIndex)
		{
			_section = section;
			_group = group;
			_groupIndex = groupIndex;
		}
		
		private List<DropItem> getItems()
		{
			return _group == null ? _section.getItems() : _group.getItems();
		}
	}
	
	/**
	 * Associates one drop item with its owning section and optional group.
	 */
	private static final class DropRow
	{
		private final DropSection _section;
		private final DropGroup _group;
		private final int _groupIndex;
		private final DropItem _item;
		
		private DropRow(DropSection section, DropGroup group, int groupIndex, DropItem item)
		{
			_section = section;
			_group = group;
			_groupIndex = groupIndex;
			_item = item;
		}
	}
}
