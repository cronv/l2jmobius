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
package org.l2jmobius.tools.editor.item.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumnModel;

import org.l2jmobius.tools.editor.item.model.ItemCapsuleRecord;
import org.l2jmobius.tools.editor.item.model.ItemConditionRecord;
import org.l2jmobius.tools.editor.item.model.ItemDefinition;
import org.l2jmobius.tools.editor.item.model.ItemHandlerCatalog;
import org.l2jmobius.tools.editor.item.model.ItemIndex;
import org.l2jmobius.tools.editor.item.model.ItemMetadataIndex;
import org.l2jmobius.tools.editor.item.model.ItemMetadataRecord;
import org.l2jmobius.tools.editor.item.model.ItemSetProperty;
import org.l2jmobius.tools.editor.item.model.ItemSetProperty.Category;
import org.l2jmobius.tools.editor.item.model.ItemSetProperty.Kind;
import org.l2jmobius.tools.editor.item.model.ItemSkillRecord;
import org.l2jmobius.tools.editor.item.model.ItemStatRecord;
import org.l2jmobius.tools.editor.item.model.ItemValidationIssue;
import org.l2jmobius.tools.editor.item.model.ItemValidationReport;
import org.l2jmobius.tools.editor.item.service.ItemProjectScanner;
import org.l2jmobius.tools.editor.item.service.ItemSemanticValidator;
import org.l2jmobius.tools.editor.item.service.ItemStructuredEditor;
import org.l2jmobius.tools.editor.item.service.ItemXmlEditor;
import org.l2jmobius.tools.editor.item.service.ItemXmlEditor.SavedItem;
import org.l2jmobius.tools.editor.npc.util.NpcXmlUtil;

/**
 * Safe Dashboard Item Editor for existing Main Chronicle Weapon, Armor, and EtcItem XML definitions. Item ID/type and collection topology remain immutable in V1; only existing values are edited.
 * @author BazookaRpm
 */
public final class ItemEditorPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final Dimension HEADER_BADGE_SIZE = new Dimension(150, 24);
	private static final Color BADGE_GREEN = new Color(48, 126, 75);
	private static final Color BADGE_ORANGE = new Color(174, 92, 40);
	private static final Color BADGE_RED = new Color(161, 62, 68);
	
	private final transient Path _selectedRoot;
	private final transient ItemProjectScanner _scanner = new ItemProjectScanner();
	private final transient ItemStructuredEditor _structuredEditor = new ItemStructuredEditor();
	private final transient ItemSemanticValidator _semanticValidator = new ItemSemanticValidator();
	private final transient ItemXmlEditor _xmlEditor = new ItemXmlEditor(_semanticValidator);
	private final JTextField _search = new JTextField();
	private final ItemBrowserModel _browserModel = new ItemBrowserModel();
	private final JTable _browser = styleTable(new JTable(_browserModel));
	private final JTabbedPane _tabs = new JTabbedPane();
	private final JTextField _name = new JTextField();
	private final JTextField _additionalName = new JTextField();
	private final JLabel _idValue = valueLabel("-");
	private final JLabel _typeValue = valueLabel("-");
	private final JLabel _fileValue = valueLabel("-");
	private final ItemEditorBadge _xsdBadge = new ItemEditorBadge("XSD UNKNOWN", ItemEditorPalette.surfaceAlt());
	private final ItemEditorBadge _stateBadge = new ItemEditorBadge("IDLE", ItemEditorPalette.surfaceAlt());
	private final JLabel _handlerStatus = valueLabel("No handler property.");
	private final JLabel _validationTitle = valueLabel("Not validated yet.");
	private final JTextArea _changes = new JTextArea(4, 40);
	private final JLabel _footer = valueLabel("Select an item to begin.");
	private final JButton _validate = new JButton("Validate");
	private final JButton _reloadItem = new JButton("Reload Item");
	private final JButton _revert = new JButton("Revert");
	private final JButton _save = new JButton("Save XML");
	private final JButton _reloadData = new JButton("Reload Data");
	private final PropertyPanel _generalProperties = new PropertyPanel(Category.GENERAL);
	private final PropertyPanel _equipmentProperties = new PropertyPanel(Category.EQUIPMENT);
	private final PropertyPanel _typeProperties = new PropertyPanel(Category.TYPE_SPECIFIC);
	private final PropertyPanel _flagProperties = new PropertyPanel(Category.FLAGS);
	private final StatsModel _statsModel = new StatsModel();
	private final SkillsModel _skillsModel = new SkillsModel();
	private final CapsulesModel _capsulesModel = new CapsulesModel();
	private final JTable _statsTable = styleTable(new JTable(_statsModel));
	private final JTable _skillsTable = styleTable(new JTable(_skillsModel));
	private final JTable _capsulesTable = styleTable(new JTable(_capsulesModel));
	private final JList<ItemConditionRecord> _conditionList = new JList<>();
	private final JTextArea _conditionXml = xmlArea();
	private final JButton _applyCondition = new JButton("Apply Block");
	private final JButton _resetCondition = new JButton("Reset Block");
	private final JTextArea _rawXml = xmlArea();
	private final JPanel _rawXmlTab = surfacePanel(new BorderLayout(6, 6));
	private final Timer _rawXmlTimer = new Timer(300, event -> validateRawXml());
	private final JLabel _rawStatus = valueLabel("Raw XML is synchronized with structured edits.");
	private transient ItemIndex _index;
	private transient ItemDefinition _definition;
	private transient ItemValidationReport _semanticReport = new ItemValidationReport(Collections.emptyList());
	private transient Consumer<Boolean> _activityListener = event ->
	{
	};
	private boolean _started;
	private boolean _busy;
	private boolean _loadingItem;
	private long _loadGeneration;
	private boolean _refreshing;
	private boolean _rawValid = true;
	private boolean _explicitlyValidated;
	private String _conditionBaseXml = "";
	private int _conditionSelection = -1;
	private boolean _conditionSelectionGuard;
	
	public ItemEditorPanel(Path selectedRoot)
	{
		_selectedRoot = selectedRoot;
		setOpaque(true);
		setBackground(ItemEditorPalette.panel());
		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder());
		_rawXmlTimer.setRepeats(false);
		add(createHeader(), BorderLayout.NORTH);
		add(createWorkspace(), BorderLayout.CENTER);
		add(createFooter(), BorderLayout.SOUTH);
		configureActions();
		refreshEnabledState();
	}
	
	public void setActivityListener(Consumer<Boolean> listener)
	{
		_activityListener = listener == null ? event ->
		{
		} : listener;
	}
	
	public void start()
	{
		if (!_started)
		{
			_started = true;
			reloadData();
		}
	}
	
	public boolean isBusy()
	{
		return _busy || _loadingItem;
	}
	
	public boolean hasUnsavedChanges()
	{
		return ((_definition != null) && _definition.isDirty()) || hasPendingConditionDraft();
	}
	
	public void discardUnsavedChanges()
	{
		if (_definition != null)
		{
			discardChanges();
			refreshFromDefinition();
			setFooter("Unsaved item changes discarded.", ItemEditorPalette.muted());
		}
	}
	
	public boolean confirmDiscardOrSave()
	{
		if (!hasUnsavedChanges())
		{
			return true;
		}
		
		if (hasPendingConditionDraft())
		{
			final int result = JOptionPane.showConfirmDialog(this, "A condition block has unapplied edits. Discard all unsaved item changes and close?", "Unapplied condition edit", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (result == JOptionPane.YES_OPTION)
			{
				discardUnsavedChanges();
				return true;
			}
			
			return false;
		}
		
		final Object[] choices =
		{
			"Save XML",
			"Discard changes",
			"Cancel"
		};
		final int result = JOptionPane.showOptionDialog(this, "Item " + _definition.getItem().getId() + " has unsaved XML changes.", "Unsaved item changes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, choices, choices[2]);
		if (result == 0)
		{
			return saveCurrentSync();
		}
		
		if (result == 1)
		{
			discardUnsavedChanges();
			return true;
		}
		
		return false;
	}
	
	private JPanel createHeader()
	{
		final JPanel panel = new JPanel(new BorderLayout(8, 4));
		panel.setOpaque(false);
		final JPanel identity = new JPanel(new BorderLayout(0, 1));
		identity.setOpaque(false);
		final JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		title.setOpaque(false);
		final JLabel heading = new JLabel("Item Editor");
		heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
		heading.setForeground(UIManager.getColor("Label.foreground"));
		title.add(heading);
		final JLabel subtitle = new JLabel("Edit existing item definitions directly in the active L2JMobius datapack.");
		subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
		subtitle.setForeground(UIManager.getColor("Label.disabledForeground"));
		identity.add(title, BorderLayout.NORTH);
		identity.add(subtitle, BorderLayout.SOUTH);
		_xsdBadge.setPreferredSize(HEADER_BADGE_SIZE);
		_stateBadge.setPreferredSize(HEADER_BADGE_SIZE);
		final JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 3));
		badges.setOpaque(false);
		badges.add(_xsdBadge);
		badges.add(_stateBadge);
		panel.add(identity, BorderLayout.WEST);
		panel.add(badges, BorderLayout.EAST);
		return panel;
	}
	
	private JSplitPane createWorkspace()
	{
		final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createBrowserPanel(), createEditorPanel());
		split.setResizeWeight(0.24d);
		split.setDividerLocation(330);
		split.setContinuousLayout(true);
		split.setBorder(BorderFactory.createEmptyBorder());
		split.setBackground(ItemEditorPalette.panel());
		return split;
	}
	
	private JPanel createBrowserPanel()
	{
		final JPanel panel = surfacePanel(new BorderLayout(6, 6));
		panel.setBorder(compoundBorder(8));
		final JPanel top = new JPanel(new BorderLayout(4, 4));
		top.setOpaque(false);
		top.add(sectionLabel("Items"), BorderLayout.NORTH);
		_search.setToolTipText("Search by item ID, name, additional name, type, or XML file");
		styleInput(_search);
		top.add(_search, BorderLayout.CENTER);
		panel.add(top, BorderLayout.NORTH);
		_browser.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_browser.setAutoCreateRowSorter(true);
		_browser.setRowHeight(24);
		final TableColumnModel columns = _browser.getColumnModel();
		columns.getColumn(0).setPreferredWidth(65);
		columns.getColumn(1).setPreferredWidth(190);
		columns.getColumn(2).setPreferredWidth(75);
		panel.add(scroll(_browser), BorderLayout.CENTER);
		_reloadData.setToolTipText("Rescan item metadata from the datapack");
		panel.add(_reloadData, BorderLayout.SOUTH);
		return panel;
	}
	
	private JPanel createEditorPanel()
	{
		final JPanel panel = new JPanel(new BorderLayout(6, 6));
		panel.setOpaque(false);
		panel.add(createMetadataBar(), BorderLayout.NORTH);
		_tabs.addTab("General", createGeneralTab());
		_tabs.addTab("Equipment", _equipmentProperties);
		_tabs.addTab("Type Specific", createTypeSpecificTab());
		_tabs.addTab("Flags", _flagProperties);
		_tabs.addTab("Stats", createStatsTab());
		_tabs.addTab("Skills", createSkillsTab());
		_tabs.addTab("Capsules", createCapsulesTab());
		_tabs.addTab("Conditions", createConditionsTab());
		_tabs.addTab("Raw XML", createRawXmlTab());
		panel.add(_tabs, BorderLayout.CENTER);
		panel.add(createValidationPanel(), BorderLayout.SOUTH);
		return panel;
	}
	
	private JPanel createMetadataBar()
	{
		final JPanel panel = surfacePanel(new BorderLayout(8, 6));
		panel.setBorder(compoundBorder(8));
		final JPanel metadata = new JPanel(new GridBagLayout());
		metadata.setOpaque(false);
		final GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(2, 4, 2, 8);
		c.anchor = GridBagConstraints.WEST;
		c.gridy = 0;
		addMetadata(metadata, c, 0, "ID", _idValue);
		addMetadata(metadata, c, 2, "Type", _typeValue);
		addMetadata(metadata, c, 4, "XML", _fileValue);
		panel.add(metadata, BorderLayout.CENTER);
		final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		actions.setOpaque(false);
		actions.add(_validate);
		actions.add(_reloadItem);
		actions.add(_revert);
		actions.add(_save);
		panel.add(actions, BorderLayout.EAST);
		return panel;
	}
	
	private JPanel createGeneralTab()
	{
		final JPanel panel = surfacePanel(new BorderLayout(8, 8));
		panel.setBorder(compoundBorder(10));
		final JPanel identity = new JPanel(new GridBagLayout());
		identity.setOpaque(false);
		final GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(3, 4, 3, 8);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0;
		c.gridy = 0;
		identity.add(fieldLabel("Name"), c);
		c.gridx = 1;
		c.weightx = 1;
		styleInput(_name);
		identity.add(_name, c);
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0;
		identity.add(fieldLabel("Additional Name"), c);
		c.gridx = 1;
		c.weightx = 1;
		styleInput(_additionalName);
		identity.add(_additionalName, c);
		panel.add(identity, BorderLayout.NORTH);
		panel.add(_generalProperties, BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel createTypeSpecificTab()
	{
		final JPanel panel = surfacePanel(new BorderLayout(6, 6));
		panel.setBorder(compoundBorder(10));
		_handlerStatus.setForeground(ItemEditorPalette.muted());
		panel.add(_handlerStatus, BorderLayout.NORTH);
		panel.add(_typeProperties, BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel createStatsTab()
	{
		final JPanel panel = surfacePanel(new BorderLayout(6, 6));
		panel.setBorder(compoundBorder(10));
		panel.add(infoLine("Existing stats only. Stat type is immutable; Value is editable and must remain numeric."), BorderLayout.NORTH);
		_statsTable.setRowHeight(26);
		panel.add(scroll(_statsTable), BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel createSkillsTab()
	{
		final JPanel panel = surfacePanel(new BorderLayout(6, 6));
		panel.setBorder(compoundBorder(10));
		panel.add(infoLine("Existing references only. Skill IDs/levels are checked against the current Main chronicle skill index before save."), BorderLayout.NORTH);
		_skillsTable.setRowHeight(26);
		_skillsTable.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(new JComboBox<>(ItemStructuredEditor.itemSkillTypes().toArray(String[]::new))));
		panel.add(scroll(_skillsTable), BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel createCapsulesTab()
	{
		final JPanel panel = surfacePanel(new BorderLayout(6, 6));
		panel.setBorder(compoundBorder(10));
		panel.add(infoLine("Existing capsule rows only. Target item ID, min/max, chance, and enchant bounds are validated before save."), BorderLayout.NORTH);
		_capsulesTable.setRowHeight(26);
		panel.add(scroll(_capsulesTable), BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel createConditionsTab()
	{
		final JPanel panel = surfacePanel(new BorderLayout(6, 6));
		panel.setBorder(compoundBorder(10));
		panel.add(infoLine("Conservative mode: edit one existing <cond> block as XML, then Apply Block. No condition topology is generated."), BorderLayout.NORTH);
		_conditionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_conditionList.setBackground(ItemEditorPalette.surface());
		_conditionList.setForeground(ItemEditorPalette.surfaceText());
		_conditionXml.setRows(14);
		final JScrollPane conditionScroll = scroll(_conditionXml);
		conditionScroll.setRowHeaderView(new ItemEditorLineNumbers(_conditionXml));
		final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll(_conditionList), conditionScroll);
		split.setResizeWeight(0.25d);
		split.setDividerLocation(220);
		panel.add(split, BorderLayout.CENTER);
		final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		actions.setOpaque(false);
		actions.add(_resetCondition);
		actions.add(_applyCondition);
		panel.add(actions, BorderLayout.SOUTH);
		return panel;
	}
	
	private JPanel createRawXmlTab()
	{
		_rawXmlTab.setBorder(compoundBorder(10));
		_rawStatus.setForeground(ItemEditorPalette.muted());
		_rawXmlTab.add(_rawStatus, BorderLayout.NORTH);
		final JScrollPane scroll = scroll(_rawXml);
		scroll.setRowHeaderView(new ItemEditorLineNumbers(_rawXml));
		_rawXmlTab.add(scroll, BorderLayout.CENTER);
		return _rawXmlTab;
	}
	
	private JPanel createValidationPanel()
	{
		final JPanel panel = surfacePanel(new BorderLayout(8, 4));
		panel.setBorder(compoundBorder(8));
		final JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		top.add(sectionLabel("Validation / Changes"), BorderLayout.WEST);
		_validationTitle.setHorizontalAlignment(SwingConstants.RIGHT);
		top.add(_validationTitle, BorderLayout.EAST);
		panel.add(top, BorderLayout.NORTH);
		_changes.setEditable(false);
		_changes.setFocusable(false);
		_changes.setLineWrap(true);
		_changes.setWrapStyleWord(true);
		_changes.setBackground(ItemEditorPalette.surface());
		_changes.setForeground(ItemEditorPalette.surfaceText());
		_changes.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
		panel.add(_changes, BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel createFooter()
	{
		final JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		_footer.setForeground(ItemEditorPalette.muted());
		panel.add(_footer, BorderLayout.WEST);
		return panel;
	}
	
	private void configureActions()
	{
		_search.getDocument().addDocumentListener(documentListener(this::filterBrowser));
		_browser.getSelectionModel().addListSelectionListener(this::browserSelectionChanged);
		_validate.addActionListener(event -> validateCurrent());
		_reloadItem.addActionListener(event -> reloadSelectedItem());
		_revert.addActionListener(event -> revertCurrent());
		_save.addActionListener(event -> saveCurrentAsync(null));
		_reloadData.addActionListener(event -> reloadData());
		_name.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent event)
			{
				updateRootField("name", _name.getText());
			}
		});
		_conditionList.addListSelectionListener(this::conditionSelectionChanged);
		_conditionXml.getDocument().addDocumentListener(documentListener(this::conditionDraftChanged));
		_applyCondition.addActionListener(event -> applyCondition());
		_resetCondition.addActionListener(event -> resetCondition());
		_rawXml.getDocument().addDocumentListener(documentListener(this::rawXmlChanged));
		_tabs.addChangeListener(event -> tabChanged());
	}
	
	private void reloadData()
	{
		if (_busy || _loadingItem)
		{
			return;
		}
		
		if (hasUnsavedChanges() && !confirmDiscard("Reloading item data will discard the current item changes."))
		{
			return;
		}
		
		setBusy(true);
		setFooter("Scanning item datapack...", ItemEditorPalette.BLUE);
		new SwingWorker<ItemIndex, Void>()
		{
			@Override
			protected ItemIndex doInBackground() throws Exception
			{
				final ItemIndex index = _scanner.scan(_selectedRoot);
				
				// Skill files are walked here off the EDT and reused until the next Reload Data.
				_semanticValidator.reloadSkills(index.getLayout());
				return index;
			}
			
			@Override
			protected void done()
			{
				try
				{
					_index = get();
					_definition = null;
					refreshFromDefinition();
					final ItemMetadataIndex metadata = _index.getMetadata();
					_browserModel.setRecords(metadata.getRecords());
					final boolean schemaReady = Files.isRegularFile(_index.getLayout().getItemSchema());
					_xsdBadge.setBadge(schemaReady ? "XSD READY" : "XSD MISSING", schemaReady ? BADGE_GREEN : BADGE_RED);
					_browser.clearSelection();
					final int fileCount = metadata.getFileCount();
					setFooter(metadata.size() + " items indexed from " + fileCount + " XML files (cache " + metadata.getCachedFileCount() + "/" + fileCount + ").", ItemEditorPalette.GREEN);
				}
				catch (Exception e)
				{
					showError("Item datapack scan failed", e);
				}
				finally
				{
					setBusy(false);
					refreshEnabledState();
					if ((_index != null) && (_browserModel.getRowCount() > 0) && (_browser.getSelectedRow() < 0))
					{
						_browser.getSelectionModel().setSelectionInterval(0, 0);
					}
				}
			}
		}.execute();
	}
	
	private void filterBrowser()
	{
		if (_index == null)
		{
			return;
		}
		
		final String query = _search.getText().trim().toLowerCase(Locale.ROOT);
		if (query.isEmpty())
		{
			_browserModel.setRecords(_index.getMetadata().getRecords());
			return;
		}
		
		final List<ItemMetadataRecord> filtered = new ArrayList<>();
		for (ItemMetadataRecord item : _index.getMetadata().getRecords())
		{
			if (Integer.toString(item.getId()).contains(query) || item.getName().toLowerCase(Locale.ROOT).contains(query) || item.getAdditionalName().toLowerCase(Locale.ROOT).contains(query) || item.getType().toLowerCase(Locale.ROOT).contains(query) || item.getFile().getFileName().toString().toLowerCase(Locale.ROOT).contains(query))
			{
				filtered.add(item);
			}
		}
		
		_browserModel.setRecords(filtered);
	}
	
	private void browserSelectionChanged(ListSelectionEvent event)
	{
		if (event.getValueIsAdjusting() || _refreshing || _busy)
		{
			return;
		}
		
		final int row = _browser.getSelectedRow();
		if (row < 0)
		{
			return;
		}
		
		final ItemMetadataRecord selected = _browserModel.getRecord(_browser.convertRowIndexToModel(row));
		// Unsaved changes always belong to a loaded item.
		final ItemMetadataRecord current = _definition == null ? null : _definition.getItem();
		if (current != null)
		{
			if (current.hasSameSourceIdentity(selected) && !_loadingItem)
			{
				return;
			}
			
			if (hasUnsavedChanges())
			{
				if (!confirmDiscard("Changing items will discard unsaved changes for item " + current.getId() + "."))
				{
					reselectDefinition();
					return;
				}
				
				discardChanges();
			}
		}
		
		loadItem(selected);
	}
	
	private void loadItem(ItemMetadataRecord item)
	{
		final long generation = ++_loadGeneration;
		setItemLoading(true);
		new SwingWorker<ItemDefinition, Void>()
		{
			@Override
			protected ItemDefinition doInBackground() throws Exception
			{
				return _xmlEditor.load(item);
			}
			
			@Override
			protected void done()
			{
				if (generation != _loadGeneration)
				{
					return;
				}
				
				try
				{
					final ItemDefinition loaded = get();
					if (generation != _loadGeneration)
					{
						return;
					}
					
					_definition = loaded;
					refreshFromDefinition();
					setFooter("Loaded item " + item.getId() + " - " + item.getName() + ".", ItemEditorPalette.muted());
				}
				catch (Exception e)
				{
					if (generation == _loadGeneration)
					{
						showError("Item load failed", e);
						if (_definition != null)
						{
							refreshFromDefinition();
						}
					}
				}
				finally
				{
					if (generation == _loadGeneration)
					{
						setItemLoading(false);
					}
				}
			}
		}.execute();
	}
	
	private void refreshFromDefinition()
	{
		_refreshing = true;
		try
		{
			if (_definition == null)
			{
				clearEditor();
				return;
			}
			
			final String xml = _definition.getXml();
			final ItemMetadataRecord item = _definition.getItem();
			_idValue.setText(Integer.toString(item.getId()));
			_typeValue.setText(item.getType());
			_fileValue.setText(item.getFile().getFileName().toString());
			_name.setText(_structuredEditor.getRootAttribute(xml, "name"));
			_additionalName.setText(_structuredEditor.getRootAttribute(xml, "additionalName"));
			final List<ItemSetProperty> properties = _structuredEditor.inspectSetProperties(xml);
			_generalProperties.setProperties(properties);
			_equipmentProperties.setProperties(properties);
			_typeProperties.setProperties(properties);
			_flagProperties.setProperties(properties);
			_statsModel.setRows(_structuredEditor.inspectStats(xml));
			_skillsModel.setRows(_structuredEditor.inspectSkills(xml));
			_capsulesModel.setRows(_structuredEditor.inspectCapsules(xml));
			setConditions(_structuredEditor.inspectConditions(xml));
			_rawXml.setText(xml);
			_rawXml.setCaretPosition(0);
			_rawValid = true;
			_rawStatus.setText("Raw XML is well-formed. ID and type remain immutable at save time.");
			_rawStatus.setForeground(ItemEditorPalette.GREEN);
			_explicitlyValidated = false;
			updateSemanticState();
			updateHandlerStatus(properties);
			updateDocumentState();
		}
		catch (Exception e)
		{
			_rawValid = false;
			setValidation("Editor refresh failed: " + rootMessage(e), ItemEditorPalette.RED);
		}
		finally
		{
			_refreshing = false;
			if (!_loadingItem)
			{
				refreshEnabledState();
			}
		}
	}
	
	private void clearEditor()
	{
		_idValue.setText("-");
		_typeValue.setText("-");
		_fileValue.setText("-");
		_name.setText("");
		_additionalName.setText("");
		_generalProperties.setProperties(Collections.emptyList());
		_equipmentProperties.setProperties(Collections.emptyList());
		_typeProperties.setProperties(Collections.emptyList());
		_flagProperties.setProperties(Collections.emptyList());
		_statsModel.setRows(Collections.emptyList());
		_skillsModel.setRows(Collections.emptyList());
		_capsulesModel.setRows(Collections.emptyList());
		setConditions(Collections.emptyList());
		_rawXml.setText("");
		_rawValid = true;
		_semanticReport = new ItemValidationReport(Collections.emptyList());
		_explicitlyValidated = false;
		setValidation("Not validated yet.", ItemEditorPalette.muted());
		updateDocumentState();
	}
	
	private void updateRootField(String attribute, String value)
	{
		if (_refreshing || _busy || _loadingItem || (_definition == null))
		{
			return;
		}
		
		try
		{
			applyXml(_structuredEditor.updateRootAttribute(_definition.getXml(), attribute, value), "Updated " + attribute + ".");
		}
		catch (Exception e)
		{
			showError("Could not update item " + attribute, e);
			refreshFromDefinition();
		}
	}
	
	private void applyProperty(ItemSetProperty property, String value)
	{
		if (_busy || _loadingItem || (_definition == null))
		{
			return;
		}
		
		try
		{
			final String name = property.getName();
			final String error = ItemSemanticValidator.setPropertyError(property.getKind(), name, value);
			if (error != null)
			{
				throw new IllegalArgumentException(error);
			}
			
			applyXml(_structuredEditor.updateSetProperty(_definition.getXml(), property, value), "Updated " + name + ".");
		}
		catch (Exception e)
		{
			showError("Invalid item value", e);
			refreshFromDefinition();
		}
	}
	
	private void applyStat(ItemStatRecord record, String value)
	{
		if (_busy || _loadingItem || (_definition == null))
		{
			return;
		}
		
		try
		{
			Double.parseDouble(value.trim());
			applyXml(_structuredEditor.updateStat(_definition.getXml(), record, value.trim()), "Updated stat " + record.getType() + ".");
		}
		catch (Exception e)
		{
			showError("Invalid stat value", e);
			refreshFromDefinition();
		}
	}
	
	private void applySkill(ItemSkillRecord record, int column, Object value)
	{
		if (_busy || _loadingItem || (_definition == null))
		{
			return;
		}
		
		final String attribute;
		switch (column)
		{
			case 2:
			{
				attribute = "id";
				break;
			}
			case 3:
			{
				attribute = "level";
				break;
			}
			case 4:
			{
				attribute = "subLevel";
				break;
			}
			case 5:
			{
				attribute = "type";
				break;
			}
			case 6:
			{
				attribute = "type_chance";
				break;
			}
			case 7:
			{
				attribute = "type_value";
				break;
			}
			default:
			{
				attribute = null;
				break;
			}
		}
		
		if (attribute == null)
		{
			return;
		}
		
		try
		{
			final String text = value == null ? "" : value.toString().trim();
			if (((column == 2) || (column == 3)) && (Integer.parseInt(text) <= 0))
			{
				throw new IllegalArgumentException("Skill ID and level must be positive integers.");
			}
			
			final boolean blank = text.isBlank();
			if ((column == 4) && !blank && (Integer.parseInt(text) < 0))
			{
				throw new IllegalArgumentException("Skill subLevel cannot be negative.");
			}
			
			if (((column == 6) || (column == 7)) && !blank && (Integer.parseInt(text) <= 0))
			{
				throw new IllegalArgumentException("Optional skill chance/value must be positive when present.");
			}
			
			applyXml(_structuredEditor.updateSkill(_definition.getXml(), record, attribute, text), "Updated item skill reference.");
		}
		catch (Exception e)
		{
			showError("Invalid item skill value", e);
			refreshFromDefinition();
		}
	}
	
	private void applyCapsule(ItemCapsuleRecord record, int column, Object value)
	{
		if (_busy || _loadingItem || (_definition == null))
		{
			return;
		}
		
		final String attribute;
		switch (column)
		{
			case 1:
			{
				attribute = "id";
				break;
			}
			case 2:
			{
				attribute = "min";
				break;
			}
			case 3:
			{
				attribute = "max";
				break;
			}
			case 4:
			{
				attribute = "chance";
				break;
			}
			case 5:
			{
				attribute = "minEnchant";
				break;
			}
			case 6:
			{
				attribute = "maxEnchant";
				break;
			}
			default:
			{
				attribute = null;
				break;
			}
		}
		
		if (attribute == null)
		{
			return;
		}
		
		try
		{
			final String text = value == null ? "" : value.toString().trim();
			if ((column >= 1) && (column <= 3) && (Long.parseLong(text) <= 0))
			{
				throw new IllegalArgumentException("Capsule ID/min/max must be positive integers.");
			}
			
			if (column == 4)
			{
				final double chance = Double.parseDouble(text);
				if (!Double.isFinite(chance) || (chance <= 0) || (chance > 100))
				{
					throw new IllegalArgumentException("Capsule chance must be greater than 0 and at most 100.");
				}
			}
			
			if (((column == 5) || (column == 6)) && !text.isBlank() && (Integer.parseInt(text) < 0))
			{
				throw new IllegalArgumentException("Enchant bounds cannot be negative.");
			}
			
			applyXml(_structuredEditor.updateCapsule(_definition.getXml(), record, attribute, text), "Updated capsule row.");
		}
		catch (Exception e)
		{
			showError("Invalid capsule value", e);
			refreshFromDefinition();
		}
	}
	
	private void applyXml(String xml, String message)
	{
		_definition.setXml(xml);
		refreshFromDefinition();
		final boolean dirty = _definition.isDirty();
		setFooter(message + (dirty ? " Save XML to persist the change." : " Item matches the loaded source."), dirty ? ItemEditorPalette.ORANGE : ItemEditorPalette.muted());
	}
	
	private void rawXmlChanged()
	{
		if (_refreshing || _busy || _loadingItem || (_definition == null))
		{
			return;
		}
		
		_definition.setXml(_rawXml.getText());
		_explicitlyValidated = false;
		
		// Parsing and semantic validation wait for a typing pause instead of running on every keystroke.
		_rawXmlTimer.restart();
	}
	
	private void validateRawXml()
	{
		_rawXmlTimer.stop();
		if (_definition == null)
		{
			return;
		}
		
		try
		{
			NpcXmlUtil.parse(_definition.getXml());
			_rawValid = true;
			_rawStatus.setText("Raw XML is well-formed. Structured tabs will resynchronize when opened.");
			_rawStatus.setForeground(ItemEditorPalette.GREEN);
			updateSemanticState();
		}
		catch (Exception e)
		{
			_rawValid = false;
			_semanticReport = new ItemValidationReport(Collections.emptyList());
			_rawStatus.setText("INVALID XML: " + rootMessage(e));
			_rawStatus.setForeground(ItemEditorPalette.RED);
		}
		
		updateDocumentState();
		refreshEnabledState();
	}
	
	private void flushRawXmlValidation()
	{
		if (_rawXmlTimer.isRunning())
		{
			validateRawXml();
		}
	}
	
	private void tabChanged()
	{
		if (_refreshing || _loadingItem || (_definition == null))
		{
			return;
		}
		
		flushRawXmlValidation();
		final boolean structuredTab = _tabs.getSelectedComponent() != _rawXmlTab;
		if (!_rawValid && structuredTab)
		{
			SwingUtilities.invokeLater(() -> _tabs.setSelectedComponent(_rawXmlTab));
			setFooter("Raw XML is invalid. Correct it before returning to structured tabs.", ItemEditorPalette.RED);
			return;
		}
		
		if (structuredTab)
		{
			final boolean validated = _explicitlyValidated;
			refreshFromDefinition();
			_explicitlyValidated = validated;
			updateDocumentState();
		}
	}
	
	private void conditionSelectionChanged(ListSelectionEvent event)
	{
		if (event.getValueIsAdjusting() || _refreshing || _loadingItem || _conditionSelectionGuard)
		{
			return;
		}
		
		final int selected = _conditionList.getSelectedIndex();
		if (selected == _conditionSelection)
		{
			return;
		}
		
		if (hasPendingConditionDraft())
		{
			if (JOptionPane.showConfirmDialog(this, "The current condition block has unapplied edits. Discard that block draft?", "Unapplied condition edit", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)
			{
				_conditionSelectionGuard = true;
				_conditionList.setSelectedIndex(_conditionSelection);
				_conditionSelectionGuard = false;
				return;
			}
		}
		
		showCondition(selected);
	}
	
	private void showCondition(int selected)
	{
		_conditionSelection = selected;
		final boolean previousRefreshing = _refreshing;
		_refreshing = true;
		try
		{
			if (selected < 0)
			{
				_conditionBaseXml = "";
				_conditionXml.setText("");
			}
			else
			{
				_conditionBaseXml = _conditionList.getModel().getElementAt(selected).getXml();
				_conditionXml.setText(_conditionBaseXml);
				_conditionXml.setCaretPosition(0);
			}
		}
		finally
		{
			_refreshing = previousRefreshing;
		}
		
		refreshEnabledState();
	}
	
	private void conditionDraftChanged()
	{
		if (_refreshing || _busy || _loadingItem)
		{
			return;
		}
		
		updateDocumentState();
		refreshEnabledState();
	}
	
	private void applyCondition()
	{
		if (_busy || _loadingItem || (_definition == null) || (_conditionSelection < 0))
		{
			return;
		}
		
		try
		{
			applyXml(_structuredEditor.updateCondition(_definition.getXml(), _conditionList.getModel().getElementAt(_conditionSelection), _conditionXml.getText()), "Applied condition block " + (_conditionSelection + 1) + ".");
		}
		catch (Exception e)
		{
			showError("Invalid condition block", e);
		}
	}
	
	private void resetCondition()
	{
		final boolean previousRefreshing = _refreshing;
		_refreshing = true;
		try
		{
			_conditionXml.setText(_conditionBaseXml);
		}
		finally
		{
			_refreshing = previousRefreshing;
		}
		
		updateDocumentState();
		refreshEnabledState();
	}
	
	private boolean hasPendingConditionDraft()
	{
		return (_conditionSelection >= 0) && !_conditionBaseXml.equals(_conditionXml.getText());
	}
	
	private void setConditions(List<ItemConditionRecord> records)
	{
		_conditionList.setListData(records.toArray(ItemConditionRecord[]::new));
		if (records.isEmpty())
		{
			_conditionSelection = -1;
			_conditionBaseXml = "";
			_conditionXml.setText("");
			return;
		}
		
		final int selected = Math.max(0, Math.min(_conditionSelection, records.size() - 1));
		_conditionList.setSelectedIndex(selected);
		
		// An unapplied draft on an unchanged block survives the refresh.
		if (!hasPendingConditionDraft() || (selected != _conditionSelection) || !records.get(selected).getXml().equals(_conditionBaseXml))
		{
			showCondition(selected);
		}
	}
	
	private void updateSemanticState()
	{
		if ((_definition == null) || (_index == null) || !_rawValid)
		{
			return;
		}
		
		try
		{
			_semanticReport = _semanticValidator.validate(_index.getLayout(), _index.getMetadata(), _definition.getOriginalXml(), _definition.getXml());
		}
		catch (Exception e)
		{
			_semanticReport = new ItemValidationReport(List.of(new ItemValidationIssue(ItemValidationIssue.Severity.ERROR, "Semantic validation could not run: " + rootMessage(e))));
		}
	}
	
	private void updateHandlerStatus(List<ItemSetProperty> properties)
	{
		String handler = "";
		for (ItemSetProperty property : properties)
		{
			if ("handler".equals(property.getName()))
			{
				handler = property.getValue();
				break;
			}
		}
		
		if (handler.isBlank())
		{
			_handlerStatus.setText("No item handler property on this item.");
			_handlerStatus.setForeground(ItemEditorPalette.muted());
			return;
		}
		
		try
		{
			final ItemHandlerCatalog catalog = _semanticValidator.loadHandlerCatalog(_index.getLayout());
			if (!catalog.isAvailable())
			{
				_handlerStatus.setText("Handler: " + handler + " - registry unavailable");
				_handlerStatus.setForeground(ItemEditorPalette.ORANGE);
			}
			else if (catalog.contains(handler))
			{
				_handlerStatus.setText("REGISTERED handler: " + handler);
				_handlerStatus.setForeground(ItemEditorPalette.GREEN);
			}
			else
			{
				_handlerStatus.setText("UNREGISTERED handler: " + handler + " (unchanged legacy values are warnings; new invalid values are blocked)");
				_handlerStatus.setForeground(ItemEditorPalette.ORANGE);
			}
		}
		catch (Exception e)
		{
			_handlerStatus.setText("Handler validation unavailable: " + rootMessage(e));
			_handlerStatus.setForeground(ItemEditorPalette.ORANGE);
		}
	}
	
	private void updateDocumentState()
	{
		if (_definition == null)
		{
			setState("IDLE", ItemEditorPalette.surfaceAlt());
			_changes.setText("No item selected.");
			return;
		}
		
		if (!_rawValid || _semanticReport.hasErrors())
		{
			setState("INVALID", BADGE_RED);
		}
		else if (_definition.isDirty())
		{
			setState("MODIFIED", BADGE_ORANGE);
		}
		else
		{
			setState("CLEAN", BADGE_GREEN);
		}
		
		final StringBuilder text = new StringBuilder();
		if (hasPendingConditionDraft())
		{
			text.append("UNAPPLIED condition block draft - Apply Block or Reset Block before saving.");
		}
		
		if (_rawValid)
		{
			try
			{
				for (String change : _structuredEditor.summarizeChanges(_definition.getOriginalXml(), _definition.getXml(), 12))
				{
					if (text.length() > 0)
					{
						text.append('\n');
					}
					text.append(change);
				}
			}
			catch (Exception e)
			{
				if (text.length() > 0)
				{
					text.append('\n');
				}
				text.append("Change summary unavailable: ").append(rootMessage(e));
			}
		}
		
		for (ItemValidationIssue issue : _semanticReport.getIssues())
		{
			if (text.length() > 0)
			{
				text.append('\n');
			}
			text.append(issue.getSeverity()).append(": ").append(issue.getMessage());
		}
		
		if (text.length() == 0)
		{
			text.append("No staged changes.");
		}
		
		_changes.setText(text.toString());
		if (_explicitlyValidated)
		{
			if (_semanticReport.hasErrors())
			{
				setValidation("Validation failed: " + _semanticReport.getErrorCount() + " semantic error(s).", ItemEditorPalette.RED);
			}
			else
			{
				final int warnings = _semanticReport.getWarningCount();
				setValidation("XSD + semantic validation passed" + (warnings > 0 ? " with " + warnings + " warning(s)." : "."), warnings > 0 ? ItemEditorPalette.ORANGE : ItemEditorPalette.GREEN);
			}
		}
		else if (_semanticReport.hasErrors())
		{
			setValidation("Local semantic validation failed.", ItemEditorPalette.RED);
		}
		else
		{
			setValidation("Not validated yet.", ItemEditorPalette.muted());
		}
	}
	
	private void validateCurrent()
	{
		flushRawXmlValidation();
		if ((_definition == null) || _busy || _loadingItem || !_rawValid || hasPendingConditionDraft())
		{
			return;
		}
		
		setBusy(true);
		setFooter("Running source conflict, semantic, and items.xsd validation...", ItemEditorPalette.BLUE);
		final ItemIndex index = _index;
		final ItemDefinition definition = _definition;
		new SwingWorker<Void, Void>()
		{
			@Override
			protected Void doInBackground() throws Exception
			{
				_xmlEditor.validate(index.getLayout(), index.getMetadata(), definition);
				return null;
			}
			
			@Override
			protected void done()
			{
				try
				{
					get();
					_explicitlyValidated = true;
					updateSemanticState();
					updateDocumentState();
					setFooter("Validation passed. Source SHA-256, item identity, semantics, and items.xsd are valid.", ItemEditorPalette.GREEN);
				}
				catch (Exception e)
				{
					_explicitlyValidated = false;
					setValidation("Validation failed: " + rootMessage(e), ItemEditorPalette.RED);
					setFooter("Validation failed. XML was not written.", ItemEditorPalette.RED);
				}
				finally
				{
					setBusy(false);
					refreshEnabledState();
				}
			}
		}.execute();
	}
	
	private void saveCurrentAsync(Consumer<Boolean> onFinish)
	{
		flushRawXmlValidation();
		if (!canSave())
		{
			if (onFinish != null)
			{
				onFinish.accept(false);
			}
			return;
		}
		
		setBusy(true);
		setFooter("Validating and saving item XML atomically...", ItemEditorPalette.BLUE);
		final ItemIndex index = _index;
		final ItemDefinition definition = _definition;
		new SwingWorker<SavedItem, Void>()
		{
			private volatile ItemIndex _rescanned;
			
			@Override
			protected SavedItem doInBackground() throws Exception
			{
				final SavedItem saved = _xmlEditor.save(index.getLayout(), index.getMetadata(), definition);
				try
				{
					// The metadata cache parses only the saved file again, so the browser shows the new name.
					_rescanned = _scanner.scan(_selectedRoot);
				}
				catch (Exception e)
				{
					// The item is already written; the browser keeps its previous index until Reload Data.
				}
				
				return saved;
			}
			
			@Override
			protected void done()
			{
				boolean saved = false;
				try
				{
					final SavedItem result = get();
					if (result != null)
					{
						definition.markSaved(result.getXml(), result.getSha256());
					}
					
					if (_rescanned != null)
					{
						_index = _rescanned;
						filterBrowser();
						reselectDefinition();
					}
					
					refreshFromDefinition();
					_explicitlyValidated = true;
					updateDocumentState();
					setFooter(result == null ? "No changes to save." : "Item saved safely. Backup: " + result.getBackup(), ItemEditorPalette.GREEN);
					saved = true;
				}
				catch (Exception e)
				{
					showError("Item save failed", e);
					setFooter("Save failed. Review validation/conflict details before retrying.", ItemEditorPalette.RED);
				}
				finally
				{
					setBusy(false);
					refreshEnabledState();
					if (onFinish != null)
					{
						onFinish.accept(saved);
					}
				}
			}
		}.execute();
	}
	
	private boolean saveCurrentSync()
	{
		// A nested event loop keeps the EDT responsive while the worker saves; a save refused up front answers before the loop could start.
		final SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
		final AtomicBoolean result = new AtomicBoolean();
		final AtomicBoolean finished = new AtomicBoolean();
		saveCurrentAsync(saved ->
		{
			result.set(saved);
			finished.set(true);
			loop.exit();
		});
		if (!finished.get())
		{
			loop.enter();
		}
		
		return result.get();
	}
	
	private void reloadSelectedItem()
	{
		if ((_definition == null) || _busy || _loadingItem)
		{
			return;
		}
		
		if (hasUnsavedChanges() && !confirmDiscard("Reloading this item will discard its unsaved changes."))
		{
			return;
		}
		
		discardChanges();
		loadItem(_definition.getItem());
	}
	
	private void revertCurrent()
	{
		if (_busy || _loadingItem || (_definition == null) || !_definition.isDirty())
		{
			return;
		}
		
		discardChanges();
		refreshFromDefinition();
		setFooter("Reverted to the loaded item snapshot.", ItemEditorPalette.muted());
	}
	
	private boolean canEditCurrentItem()
	{
		return (_definition != null) && !_busy && !_loadingItem && !_refreshing;
	}
	
	private boolean canSave()
	{
		return (_definition != null) && _definition.isDirty() && !_busy && !_loadingItem && _rawValid && !_semanticReport.hasErrors() && !hasPendingConditionDraft() && (_index != null) && Files.isRegularFile(_index.getLayout().getItemSchema());
	}
	
	private void refreshEnabledState()
	{
		final boolean item = _definition != null;
		final boolean editable = item && !_busy && !_loadingItem;
		_search.setEnabled(!_busy);
		_browser.setEnabled(!_busy);
		_reloadData.setEnabled(!_busy && !_loadingItem);
		_validate.setEnabled(editable && _rawValid && !hasPendingConditionDraft());
		_reloadItem.setEnabled(editable);
		_revert.setEnabled(editable && _definition.isDirty());
		_save.setEnabled(canSave());
		_name.setEditable(editable);
		_additionalName.setEditable(false);
		_rawXml.setEditable(editable);
		_conditionXml.setEditable(editable && (_conditionSelection >= 0));
		_tabs.setEnabled(item);
		_applyCondition.setEnabled(editable && (_conditionSelection >= 0) && hasPendingConditionDraft());
		_resetCondition.setEnabled(editable && (_conditionSelection >= 0) && hasPendingConditionDraft());
	}
	
	private void setBusy(boolean busy)
	{
		if (_busy == busy)
		{
			return;
		}
		
		final boolean wasActive = isBusy();
		_busy = busy;
		refreshEnabledState();
		notifyActivityChange(wasActive);
	}
	
	private void setItemLoading(boolean loading)
	{
		if (_loadingItem == loading)
		{
			return;
		}
		
		final boolean wasActive = isBusy();
		_loadingItem = loading;
		if (!loading)
		{
			refreshEnabledState();
		}
		
		notifyActivityChange(wasActive);
	}
	
	private void notifyActivityChange(boolean wasActive)
	{
		final boolean active = isBusy();
		if (active != wasActive)
		{
			_activityListener.accept(active);
		}
	}
	
	private void discardChanges()
	{
		if (_definition != null)
		{
			_definition.revert();
		}
		
		// A refresh keeps an unchanged block draft, so every discard drops it here first.
		if (hasPendingConditionDraft())
		{
			resetCondition();
		}
		
		_explicitlyValidated = false;
	}
	
	private void reselectDefinition()
	{
		if (_definition == null)
		{
			return;
		}
		
		final List<ItemMetadataRecord> records = _browserModel.getRecords();
		for (int row = 0; row < records.size(); row++)
		{
			if (records.get(row).hasSameSourceIdentity(_definition.getItem()))
			{
				_refreshing = true;
				try
				{
					final int view = _browser.convertRowIndexToView(row);
					_browser.getSelectionModel().setSelectionInterval(view, view);
				}
				finally
				{
					_refreshing = false;
				}
				return;
			}
		}
	}
	
	private boolean confirmDiscard(String message)
	{
		return JOptionPane.showConfirmDialog(this, message, "Discard unsaved changes?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
	}
	
	private void setValidation(String text, Color color)
	{
		_validationTitle.setText(text);
		_validationTitle.setForeground(color);
	}
	
	private void setState(String text, Color color)
	{
		_stateBadge.setBadge(text, color);
	}
	
	private void setFooter(String text, Color color)
	{
		_footer.setText(text);
		_footer.setForeground(color);
	}
	
	private void showError(String title, Exception error)
	{
		JOptionPane.showMessageDialog(this, rootMessage(error), title, JOptionPane.ERROR_MESSAGE);
	}
	
	private static String rootMessage(Throwable error)
	{
		Throwable current = error;
		Throwable cause = current.getCause();
		while ((cause != null) && (cause != current))
		{
			current = cause;
			cause = current.getCause();
		}
		
		final String message = current.getMessage();
		return (message == null) || message.isBlank() ? current.getClass().getSimpleName() : message;
	}
	
	private static JPanel surfacePanel(LayoutManager layout)
	{
		final JPanel panel = new JPanel(layout);
		panel.setOpaque(true);
		panel.setBackground(ItemEditorPalette.surface());
		return panel;
	}
	
	private static Border compoundBorder(int padding)
	{
		return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ItemEditorPalette.border()), BorderFactory.createEmptyBorder(padding, padding, padding, padding));
	}
	
	private static JScrollPane scroll(Component component)
	{
		final JScrollPane scroll = new JScrollPane(component);
		scroll.setBorder(BorderFactory.createLineBorder(ItemEditorPalette.border()));
		scroll.getViewport().setBackground(ItemEditorPalette.surface());
		return scroll;
	}
	
	private static JTextArea xmlArea()
	{
		final JTextArea area = new JTextArea();
		area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		area.setTabSize(4);
		area.setBackground(ItemEditorPalette.inputBackground());
		area.setForeground(ItemEditorPalette.inputText());
		area.setCaretColor(ItemEditorPalette.inputText());
		area.setSelectionColor(ItemEditorPalette.selectionBackground());
		area.setSelectedTextColor(ItemEditorPalette.selectionText());
		return area;
	}
	
	private static JTable styleTable(JTable table)
	{
		table.setFillsViewportHeight(true);
		table.setBackground(ItemEditorPalette.surface());
		table.setForeground(ItemEditorPalette.surfaceText());
		table.setSelectionBackground(ItemEditorPalette.selectionBackground());
		table.setSelectionForeground(ItemEditorPalette.selectionText());
		table.setGridColor(ItemEditorPalette.grid());
		final JTableHeader header = table.getTableHeader();
		header.setBackground(ItemEditorPalette.headerBackground());
		header.setForeground(ItemEditorPalette.headerText());
		return table;
	}
	
	private static void styleInput(JTextField field)
	{
		field.setBackground(ItemEditorPalette.inputBackground());
		field.setForeground(ItemEditorPalette.inputText());
		field.setCaretColor(ItemEditorPalette.inputText());
		field.setSelectionColor(ItemEditorPalette.selectionBackground());
		field.setSelectedTextColor(ItemEditorPalette.selectionText());
	}
	
	private static JLabel sectionLabel(String text)
	{
		final JLabel label = new JLabel(text);
		label.setForeground(ItemEditorPalette.text());
		label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
		return label;
	}
	
	private static JLabel fieldLabel(String text)
	{
		final JLabel label = new JLabel(text);
		label.setForeground(ItemEditorPalette.muted());
		return label;
	}
	
	private static JLabel valueLabel(String text)
	{
		final JLabel label = new JLabel(text);
		label.setForeground(ItemEditorPalette.text());
		return label;
	}
	
	private static JLabel infoLine(String text)
	{
		final JLabel label = valueLabel(text);
		label.setForeground(ItemEditorPalette.muted());
		return label;
	}
	
	private static void addMetadata(JPanel panel, GridBagConstraints c, int x, String label, JLabel value)
	{
		c.gridx = x;
		c.weightx = 0;
		panel.add(fieldLabel(label), c);
		c.gridx = x + 1;
		c.weightx = 1;
		panel.add(value, c);
	}
	
	private static DocumentListener documentListener(Runnable action)
	{
		return new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				action.run();
			}
			
			@Override
			public void removeUpdate(DocumentEvent event)
			{
				action.run();
			}
			
			@Override
			public void changedUpdate(DocumentEvent event)
			{
				action.run();
			}
		};
	}
	
	private final class PropertyPanel extends JPanel
	{
		private static final long serialVersionUID = 1L;
		private final Category _category;
		private final PropertyModel _model = new PropertyModel();
		private final JTable _table;
		private final TableCellEditor _booleanEditor = new DefaultCellEditor(new JComboBox<>(new String[]
		{
			"true",
			"false"
		}));
		private final NumberSpinnerEditor _integerEditor = new NumberSpinnerEditor(Integer.MIN_VALUE, Integer.MAX_VALUE);
		private final NumberSpinnerEditor _longEditor = new NumberSpinnerEditor(Long.MIN_VALUE, Long.MAX_VALUE);
		
		private PropertyPanel(Category category)
		{
			_category = category;
			setLayout(new BorderLayout());
			setOpaque(false);
			_table = styleTable(new JTable(_model)
			{
				private static final long serialVersionUID = 1L;
				
				@Override
				public TableCellEditor getCellEditor(int row, int column)
				{
					if (column != 1)
					{
						return super.getCellEditor(row, column);
					}
					
					final ItemSetProperty property = _model.getProperty(convertRowIndexToModel(row));
					final Kind kind = property.getKind();
					if (kind == Kind.BOOLEAN)
					{
						return _booleanEditor;
					}
					
					if (kind == Kind.ENUM)
					{
						return new DefaultCellEditor(new JComboBox<>(property.getOptions().toArray(String[]::new)));
					}
					
					// A value outside the spinner range keeps the text editor, so it is never replaced by zero.
					final String value = property.getValue();
					if ((kind == Kind.INTEGER) && _integerEditor.accepts(value))
					{
						return _integerEditor;
					}
					
					if ((kind == Kind.LONG) && _longEditor.accepts(value))
					{
						return _longEditor;
					}
					
					return super.getCellEditor(row, column);
				}
			});
			_table.setRowHeight(26);
			final TableColumnModel columns = _table.getColumnModel();
			columns.getColumn(0).setPreferredWidth(220);
			columns.getColumn(1).setPreferredWidth(300);
			add(scroll(_table), BorderLayout.CENTER);
		}
		
		private void setProperties(List<ItemSetProperty> all)
		{
			final List<ItemSetProperty> selected = new ArrayList<>();
			for (ItemSetProperty property : all)
			{
				final Category category = property.getCategory();
				if ((category == _category) || ((_category == Category.GENERAL) && (category == Category.OTHER)))
				{
					selected.add(property);
				}
			}
			
			_model.setRows(selected);
		}
	}
	
	private final class PropertyModel extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;
		private transient List<ItemSetProperty> _rows = Collections.emptyList();
		
		private void setRows(List<ItemSetProperty> rows)
		{
			_rows = new ArrayList<>(rows);
			fireTableDataChanged();
		}
		
		private ItemSetProperty getProperty(int row)
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
			return 2;
		}
		
		@Override
		public String getColumnName(int column)
		{
			return column == 0 ? "Property" : "Value";
		}
		
		@Override
		public Object getValueAt(int row, int column)
		{
			final ItemSetProperty property = _rows.get(row);
			return column == 0 ? property.getName() : property.getValue();
		}
		
		@Override
		public boolean isCellEditable(int row, int column)
		{
			return canEditCurrentItem() && (column == 1);
		}
		
		@Override
		public void setValueAt(Object value, int row, int column)
		{
			if (!_refreshing && (column == 1) && (_definition != null))
			{
				applyProperty(_rows.get(row), value == null ? "" : value.toString());
			}
		}
	}
	
	private final class StatsModel extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;
		private transient List<ItemStatRecord> _rows = Collections.emptyList();
		
		private void setRows(List<ItemStatRecord> rows)
		{
			_rows = new ArrayList<>(rows);
			fireTableDataChanged();
		}
		
		@Override
		public int getRowCount()
		{
			return _rows.size();
		}
		
		@Override
		public int getColumnCount()
		{
			return 2;
		}
		
		@Override
		public String getColumnName(int column)
		{
			return column == 0 ? "Stat Type" : "Value";
		}
		
		@Override
		public Object getValueAt(int row, int column)
		{
			return column == 0 ? _rows.get(row).getType() : _rows.get(row).getValue();
		}
		
		@Override
		public boolean isCellEditable(int row, int column)
		{
			return canEditCurrentItem() && (column == 1);
		}
		
		@Override
		public void setValueAt(Object value, int row, int column)
		{
			if (!_refreshing && (column == 1))
			{
				applyStat(_rows.get(row), value == null ? "" : value.toString());
			}
		}
	}
	
	private final class SkillsModel extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;
		private final String[] _columns =
		{
			"Container",
			"#",
			"Skill ID",
			"Level",
			"SubLevel",
			"Type",
			"Chance",
			"Value"
		};
		private transient List<ItemSkillRecord> _rows = Collections.emptyList();
		
		private void setRows(List<ItemSkillRecord> rows)
		{
			_rows = new ArrayList<>(rows);
			fireTableDataChanged();
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
		public boolean isCellEditable(int row, int column)
		{
			return canEditCurrentItem() && (column >= 2);
		}
		
		@Override
		public Object getValueAt(int row, int column)
		{
			final ItemSkillRecord record = _rows.get(row);
			switch (column)
			{
				case 0:
				{
					return record.getContainer();
				}
				case 1:
				{
					return record.getOrdinal() + 1;
				}
				case 2:
				{
					return record.getId();
				}
				case 3:
				{
					return record.getLevel();
				}
				case 4:
				{
					final int subLevel = record.getSubLevel();
					return subLevel == 0 ? "" : subLevel;
				}
				case 5:
				{
					return record.getType();
				}
				case 6:
				{
					return record.getChance();
				}
				case 7:
				{
					return record.getValue();
				}
				default:
				{
					return "";
				}
			}
		}
		
		@Override
		public void setValueAt(Object value, int row, int column)
		{
			if (!_refreshing)
			{
				applySkill(_rows.get(row), column, value);
			}
		}
	}
	
	private final class CapsulesModel extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;
		private final String[] _columns =
		{
			"#",
			"Item ID",
			"Min",
			"Max",
			"Chance",
			"Min Enchant",
			"Max Enchant"
		};
		private transient List<ItemCapsuleRecord> _rows = Collections.emptyList();
		
		private void setRows(List<ItemCapsuleRecord> rows)
		{
			_rows = new ArrayList<>(rows);
			fireTableDataChanged();
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
		public boolean isCellEditable(int row, int column)
		{
			return canEditCurrentItem() && (column >= 1);
		}
		
		@Override
		public Object getValueAt(int row, int column)
		{
			final ItemCapsuleRecord record = _rows.get(row);
			switch (column)
			{
				case 0:
				{
					return record.getOrdinal() + 1;
				}
				case 1:
				{
					return record.getItemId();
				}
				case 2:
				{
					return record.getMin();
				}
				case 3:
				{
					return record.getMax();
				}
				case 4:
				{
					return record.getChance();
				}
				case 5:
				{
					return record.getMinEnchant();
				}
				case 6:
				{
					return record.getMaxEnchant();
				}
				default:
				{
					return "";
				}
			}
		}
		
		@Override
		public void setValueAt(Object value, int row, int column)
		{
			if (!_refreshing)
			{
				applyCapsule(_rows.get(row), column, value);
			}
		}
	}
	
	private static final class ItemBrowserModel extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;
		private transient List<ItemMetadataRecord> _records = Collections.emptyList();
		
		private void setRecords(List<ItemMetadataRecord> records)
		{
			_records = new ArrayList<>(records);
			fireTableDataChanged();
		}
		
		private List<ItemMetadataRecord> getRecords()
		{
			return _records;
		}
		
		private ItemMetadataRecord getRecord(int row)
		{
			return _records.get(row);
		}
		
		@Override
		public int getRowCount()
		{
			return _records.size();
		}
		
		@Override
		public int getColumnCount()
		{
			return 3;
		}
		
		@Override
		public String getColumnName(int column)
		{
			if (column == 0)
			{
				return "ID";
			}
			
			return column == 1 ? "Name" : "Type";
		}
		
		@Override
		public Object getValueAt(int row, int column)
		{
			final ItemMetadataRecord record = _records.get(row);
			if (column == 0)
			{
				return record.getId();
			}
			
			return column == 1 ? record.getName() : record.getType();
		}
		
		@Override
		public Class<?> getColumnClass(int column)
		{
			return column == 0 ? Integer.class : String.class;
		}
	}
	
	private static final class NumberSpinnerEditor extends AbstractCellEditor implements TableCellEditor
	{
		private static final long serialVersionUID = 1L;
		private final long _minimum;
		private final long _maximum;
		private final JSpinner _spinner;
		private final JFormattedTextField _text;
		
		private NumberSpinnerEditor(long minimum, long maximum)
		{
			_minimum = minimum;
			_maximum = maximum;
			_spinner = new JSpinner(new SpinnerNumberModel(Long.valueOf(0), Long.valueOf(minimum), Long.valueOf(maximum), Long.valueOf(1)));
			
			// Plain digits without grouping, so the typed text can be checked as one whole number.
			final JSpinner.NumberEditor editor = new JSpinner.NumberEditor(_spinner, "#");
			_spinner.setEditor(editor);
			_text = editor.getTextField();
		}
		
		private boolean accepts(String value)
		{
			try
			{
				final long number = Long.parseLong(value.trim());
				return (number >= _minimum) && (number <= _maximum);
			}
			catch (NumberFormatException e)
			{
				return false;
			}
		}
		
		@Override
		public boolean stopCellEditing()
		{
			// The number format stops at the first bad character and would keep 12 from "12abc", so the whole text must parse first.
			if (!accepts(_text.getText()))
			{
				return false;
			}
			
			try
			{
				// Typed digits stay in the text field until committed.
				_spinner.commitEdit();
			}
			catch (ParseException e)
			{
				return false;
			}
			
			return super.stopCellEditing();
		}
		
		@Override
		public Object getCellEditorValue()
		{
			return _spinner.getValue().toString();
		}
		
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean selected, int row, int column)
		{
			// PropertyPanel hands out this editor only for values accepts() confirmed.
			_spinner.setValue(Long.valueOf(value.toString().trim()));
			return _spinner;
		}
	}
}
