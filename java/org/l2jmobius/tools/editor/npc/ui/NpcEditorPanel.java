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
package org.l2jmobius.tools.editor.npc.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;

import org.l2jmobius.tools.editor.npc.model.NpcDefinition;
import org.l2jmobius.tools.editor.npc.model.NpcEditorProfile;
import org.l2jmobius.tools.editor.npc.model.NpcField;
import org.l2jmobius.tools.editor.npc.model.NpcField.ValueKind;
import org.l2jmobius.tools.editor.npc.model.NpcIndex;
import org.l2jmobius.tools.editor.npc.model.NpcMinionEntry;
import org.l2jmobius.tools.editor.npc.model.NpcMinionSet;
import org.l2jmobius.tools.editor.npc.model.NpcParameterEntry;
import org.l2jmobius.tools.editor.npc.model.NpcParameterSkillEntry;
import org.l2jmobius.tools.editor.npc.model.NpcRecord;
import org.l2jmobius.tools.editor.npc.model.NpcReferenceSection;
import org.l2jmobius.tools.editor.npc.model.NpcSkillEntry;
import org.l2jmobius.tools.editor.npc.model.NpcSkillInfo;
import org.l2jmobius.tools.editor.npc.model.NpcStructuredData;
import org.l2jmobius.tools.editor.npc.service.NpcDefinitionXmlEditor;
import org.l2jmobius.tools.editor.npc.service.NpcProjectScanner;

/**
 * Reusable NPC/Mob definition editor panel for the Dashboard and standalone tool. <br>
 * Exposes scalar NPC values once and provides controlled editors for existing skills, parameters, parameter skills, and minions.
 * @author BazookaRpm
 */
public final class NpcEditorPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	private static final Dimension HEADER_BADGE_SIZE = new Dimension(150, 24);
	
	private final Path _startPath;
	private final NpcEditorProfile _profile = NpcEditorProfile.current();
	private final NpcProjectScanner _scanner = new NpcProjectScanner();
	private final NpcDefinitionXmlEditor _xmlEditor = new NpcDefinitionXmlEditor();
	private final NpcTableModel _npcModel = new NpcTableModel();
	private final JTable _npcTable = new JTable(_npcModel);
	private final TableRowSorter<NpcTableModel> _sorter = new TableRowSorter<>(_npcModel);
	private final JTextField _search = new JTextField();
	private final JTextField _minLevel = new JTextField();
	private final JTextField _maxLevel = new JTextField();
	private final JComboBox<String> _typeFilter = new JComboBox<>();
	private final JTabbedPane _tabs = new JTabbedPane();
	private final NpcEditorBadge _profileBadge = new NpcEditorBadge("", NpcEditorPalette.surfaceAlt());
	private final NpcEditorBadge _dirtyBadge = new NpcEditorBadge("CLEAN", NpcEditorPalette.GREEN);
	private final JLabel _status = new JLabel("Ready to load L2JMobius NPC data.");
	private final JLabel _counter = new JLabel("0 NPCs");
	private final JLabel _fieldCounter = new JLabel("0 fields");
	private final JButton _openDrops = new JButton("Open Drops");
	private final JButton _validate = new JButton("Validate NPC");
	private final JButton _reload = new JButton("Reload NPC");
	private final JButton _revert = new JButton("Revert");
	private final JButton _save = new JButton("Save XML");
	private final Timer _filterTimer;
	private final Map<NpcField, JComponent> _fieldEditors = new LinkedHashMap<>();
	private NpcIndex _index;
	private NpcDefinition _currentDefinition;
	private NpcRecord _currentNpc;
	private Consumer<Boolean> _activityListener;
	private IntConsumer _openDropsAction;
	private int _activeOperations;
	private boolean _started;
	private boolean _buildingFields;
	private boolean _suppressSelection;
	
	public NpcEditorPanel(Path startPath)
	{
		_startPath = startPath.toAbsolutePath().normalize();
		_filterTimer = new Timer(180, event -> applyFilters());
		_filterTimer.setRepeats(false);
		buildUi();
		bindEvents();
		updateActions();
	}
	
	public void start()
	{
		if (_started)
		{
			return;
		}
		
		_started = true;
		scanProject();
	}
	
	public void setActivityListener(Consumer<Boolean> listener)
	{
		_activityListener = listener;
	}
	
	public void setOpenDropsAction(IntConsumer action)
	{
		_openDropsAction = action;
		_openDrops.setVisible(action != null);
		updateActions();
	}
	
	public boolean isBusy()
	{
		return _activeOperations > 0;
	}
	
	public boolean hasUnsavedChanges()
	{
		return (_currentDefinition != null) && _currentDefinition.isDirty();
	}
	
	public void discardUnsavedChanges()
	{
		if (_currentDefinition != null)
		{
			_currentDefinition.revert();
			rebuildEditor();
		}
	}
	
	public boolean confirmDiscardOrSave()
	{
		if (!hasUnsavedChanges())
		{
			return true;
		}
		
		final Object[] options =
		{
			"Save XML",
			"Discard",
			"Cancel"
		};
		final int result = JOptionPane.showOptionDialog(this, "The selected NPC has unsaved definition changes.", "Unsaved NPC changes", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
		if (result == 0)
		{
			return saveCurrentNpc();
		}
		
		if (result == 1)
		{
			discardUnsavedChanges();
			return true;
		}
		
		return false;
	}
	
	private void buildUi()
	{
		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder());
		add(buildTopPanel(), BorderLayout.NORTH);
		
		configureTable(_npcTable);
		_npcTable.setRowSorter(_sorter);
		_npcTable.getColumnModel().getColumn(0).setPreferredWidth(72);
		_npcTable.getColumnModel().getColumn(0).setMaxWidth(92);
		
		_tabs.setBorder(BorderFactory.createEmptyBorder());
		final JPanel browser = buildNpcSection();
		final JPanel editor = buildEditorSection();
		
		final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, browser, editor);
		split.setDividerLocation(320);
		split.setResizeWeight(0.22);
		split.setContinuousLayout(true);
		split.setDividerSize(7);
		split.setBorder(null);
		add(split, BorderLayout.CENTER);
		
		_status.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, NpcEditorPalette.border()), BorderFactory.createEmptyBorder(6, 4, 0, 4)));
		_status.setForeground(NpcEditorPalette.muted());
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
		final JLabel title = new JLabel("NPC / Mob Editor");
		title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
		title.setForeground(UIManager.getColor("Label.foreground"));
		final JLabel subtitle = new JLabel("Edit existing NPC definitions directly in the active L2JMobius datapack.");
		subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
		subtitle.setForeground(UIManager.getColor("Label.disabledForeground"));
		titles.add(title, BorderLayout.NORTH);
		titles.add(subtitle, BorderLayout.SOUTH);
		panel.add(titles, BorderLayout.WEST);
		
		_profileBadge.setBadge(_profile.getDisplayName(), NpcEditorPalette.surfaceAlt());
		_profileBadge.setPreferredSize(HEADER_BADGE_SIZE);
		_dirtyBadge.setPreferredSize(HEADER_BADGE_SIZE);
		final JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 3));
		badges.add(_profileBadge);
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
		_search.setPreferredSize(new Dimension(260, 27));
		_search.setToolTipText("Search by NPC id, name, title, or type.");
		panel.add(_search);
		panel.add(new JLabel("Min Lv:"));
		_minLevel.setPreferredSize(new Dimension(62, 27));
		panel.add(_minLevel);
		panel.add(new JLabel("Max Lv:"));
		_maxLevel.setPreferredSize(new Dimension(62, 27));
		panel.add(_maxLevel);
		panel.add(new JLabel("Type:"));
		_typeFilter.setPreferredSize(new Dimension(190, 27));
		panel.add(_typeFilter);
		return panel;
	}
	
	private JPanel buildNpcSection()
	{
		final JScrollPane scroll = new JScrollPane(_npcTable);
		styleScrollPane(scroll);
		final JPanel panel = buildTableCard("1. NPC list", _counter, scroll);
		panel.setMinimumSize(new Dimension(260, 300));
		panel.setPreferredSize(new Dimension(320, 600));
		return panel;
	}
	
	private JPanel buildEditorSection()
	{
		final JPanel panel = createCard(new BorderLayout(0, 7));
		panel.add(buildSectionHeader("2. NPC definition", _fieldCounter), BorderLayout.NORTH);
		panel.add(_tabs, BorderLayout.CENTER);
		panel.add(buildFileActions(), BorderLayout.SOUTH);
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
		count.setForeground(NpcEditorPalette.muted());
		panel.add(label, BorderLayout.WEST);
		panel.add(count, BorderLayout.EAST);
		return panel;
	}
	
	private JPanel buildFileActions()
	{
		final JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		panel.setOpaque(false);
		styleSecondaryButton(_openDrops);
		styleSecondaryButton(_validate);
		styleSecondaryButton(_reload);
		styleSecondaryButton(_revert);
		stylePrimaryButton(_save, NpcEditorPalette.GREEN);
		_openDrops.setVisible(false);
		panel.add(_openDrops);
		panel.add(_validate);
		panel.add(_reload);
		panel.add(_revert);
		panel.add(_save);
		return panel;
	}
	
	private JPanel createCard(LayoutManager layout)
	{
		final JPanel panel = new JPanel(layout);
		panel.setOpaque(true);
		panel.setBackground(NpcEditorPalette.surface());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(NpcEditorPalette.border()), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
		return panel;
	}
	
	private void styleScrollPane(JScrollPane scrollPane)
	{
		scrollPane.setBorder(BorderFactory.createLineBorder(NpcEditorPalette.border()));
		scrollPane.getViewport().setBackground(NpcEditorPalette.surface());
		scrollPane.getVerticalScrollBar().setUnitIncrement(18);
	}
	
	private void stylePrimaryButton(JButton button, Color background)
	{
		button.setFocusPainted(false);
		button.setOpaque(true);
		button.setContentAreaFilled(true);
		button.setBackground(background);
		button.setForeground(NpcEditorPalette.badgeForeground(background));
		button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(NpcEditorPalette.blend(background, Color.WHITE, 0.18f)), BorderFactory.createEmptyBorder(5, 10, 5, 10)));
	}
	
	private void styleSecondaryButton(JButton button)
	{
		button.setFocusPainted(false);
		button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(NpcEditorPalette.border()), BorderFactory.createEmptyBorder(5, 10, 5, 10)));
	}
	
	private void configureTable(JTable table)
	{
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setFillsViewportHeight(true);
		table.setAutoCreateRowSorter(false);
		table.setRowHeight(Math.max(24, table.getRowHeight() + 5));
		table.setIntercellSpacing(new Dimension(1, 1));
		table.setShowVerticalLines(true);
		table.setShowHorizontalLines(true);
		final JTableHeader header = table.getTableHeader();
		header.setReorderingAllowed(false);
		header.setFont(header.getFont().deriveFont(Font.BOLD));
	}
	
	private void bindEvents()
	{
		bindFilter(_search);
		bindFilter(_minLevel);
		bindFilter(_maxLevel);
		_typeFilter.addActionListener(event -> applyFilters());
		_npcTable.getSelectionModel().addListSelectionListener(event ->
		{
			if (!event.getValueIsAdjusting() && !_suppressSelection)
			{
				selectionChanged();
			}
		});
		_openDrops.addActionListener(event -> openCurrentDrops());
		_validate.addActionListener(event -> validateCurrentNpc());
		_reload.addActionListener(event -> reloadCurrentNpc());
		_revert.addActionListener(event -> discardUnsavedChanges());
		_save.addActionListener(event -> saveCurrentNpc());
	}
	
	private void bindFilter(JTextField field)
	{
		field.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				_filterTimer.restart();
			}
			
			@Override
			public void removeUpdate(DocumentEvent event)
			{
				_filterTimer.restart();
			}
			
			@Override
			public void changedUpdate(DocumentEvent event)
			{
				_filterTimer.restart();
			}
		});
	}
	
	private void scanProject()
	{
		setBusy(true);
		_status.setText("Scanning NPC and skill XML...");
		new SwingWorker<NpcIndex, Void>()
		{
			@Override
			protected NpcIndex doInBackground() throws Exception
			{
				return _scanner.scan(_startPath);
			}
			
			@Override
			protected void done()
			{
				try
				{
					_index = get();
					_npcModel.setNpcs(_index.getNpcs());
					populateTypeFilter(_index.getTypes());
					_counter.setText(_index.getNpcs().size() + " NPCs");
					_status.setText("Select an NPC to edit its existing XML fields and structured data. Lazy skill lookup ready across " + _index.getSkillFileCount() + " XML files.");
				}
				catch (Exception e)
				{
					showError("NPC scan failed", e);
				}
				finally
				{
					setBusy(false);
				}
			}
		}.execute();
	}
	
	private void populateTypeFilter(Set<String> types)
	{
		final List<String> values = new ArrayList<>(types);
		values.sort(String.CASE_INSENSITIVE_ORDER);
		final DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
		model.addElement("All types");
		for (String type : values)
		{
			model.addElement(type);
		}
		
		_typeFilter.setModel(model);
	}
	
	private void applyFilters()
	{
		final String query = _search.getText().trim().toLowerCase(Locale.ROOT);
		final String type = (String) _typeFilter.getSelectedItem();
		final Integer minLevel = parseOptionalInt(_minLevel.getText());
		final Integer maxLevel = parseOptionalInt(_maxLevel.getText());
		if (((minLevel == null) && !_minLevel.getText().trim().isEmpty()) || ((maxLevel == null) && !_maxLevel.getText().trim().isEmpty()))
		{
			_status.setText("Invalid level filter.");
			_sorter.setRowFilter(new RowFilter<NpcTableModel, Integer>()
			{
				@Override
				public boolean include(Entry<? extends NpcTableModel, ? extends Integer> entry)
				{
					return false;
				}
			});
			_counter.setText("0 NPCs");
			return;
		}
		
		_sorter.setRowFilter(new RowFilter<NpcTableModel, Integer>()
		{
			@Override
			public boolean include(Entry<? extends NpcTableModel, ? extends Integer> entry)
			{
				final NpcRecord npc = _npcModel.getNpc(entry.getIdentifier());
				if ((type != null) && !"All types".equals(type) && !type.equals(npc.getType()))
				{
					return false;
				}
				
				if ((minLevel != null) && (npc.getLevel() < minLevel.intValue()))
				{
					return false;
				}
				
				if ((maxLevel != null) && (npc.getLevel() > maxLevel.intValue()))
				{
					return false;
				}
				
				if (query.isEmpty())
				{
					return true;
				}
				
				return npc.matchesSearch(query);
			}
		});
		_counter.setText(_npcTable.getRowCount() + " NPCs");
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
	
	private void selectionChanged()
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
		
		if (hasUnsavedChanges() && !confirmDiscardOrSave())
		{
			reselectCurrentNpc();
			return;
		}
		
		loadNpc(selected);
	}
	
	private void loadNpc(NpcRecord npc)
	{
		setBusy(true);
		_status.setText("Loading " + npc.getFile().getFileName() + "...");
		new SwingWorker<NpcDefinition, Void>()
		{
			@Override
			protected NpcDefinition doInBackground() throws Exception
			{
				return _xmlEditor.load(npc);
			}
			
			@Override
			protected void done()
			{
				try
				{
					_currentNpc = npc;
					_currentDefinition = get();
					rebuildEditor();
				}
				catch (Exception e)
				{
					showError("NPC load failed", e);
				}
				finally
				{
					setBusy(false);
				}
			}
		}.execute();
	}
	
	private void rebuildEditor()
	{
		final String selectedTab = getSelectedTabTitle();
		_tabs.removeAll();
		_fieldEditors.clear();
		if (_currentDefinition == null)
		{
			_fieldCounter.setText("0 fields");
			updateActions();
			return;
		}
		
		_buildingFields = true;
		try
		{
			final Map<String, List<NpcField>> byTab = new LinkedHashMap<>();
			for (NpcField field : _currentDefinition.getFields())
			{
				byTab.computeIfAbsent(field.getTab(), key -> new ArrayList<>()).add(field);
			}
			
			final String[] tabOrder =
			{
				"General",
				"Stats",
				"Combat",
				"Elements",
				"Movement",
				"AI / Aggro",
				"Collision",
				"Rewards",
				"Equipment / Shots",
				"Advanced"
			};
			for (String tab : tabOrder)
			{
				final List<NpcField> fields = byTab.get(tab);
				if ((fields != null) && !fields.isEmpty())
				{
					_tabs.addTab(tab, buildFieldTab(fields));
				}
			}
			
			final NpcStructuredData structuredData = _currentDefinition.getStructuredData();
			if (structuredData.hasSkillList())
			{
				_tabs.addTab("Skills", buildSkillsTab(structuredData));
			}
			
			if (structuredData.hasParameters())
			{
				if (!structuredData.getParameters().isEmpty() || !structuredData.getParameterSkills().isEmpty())
				{
					_tabs.addTab("Parameters", buildParametersTab(structuredData));
				}
				
				if (!structuredData.getMinionSets().isEmpty())
				{
					_tabs.addTab("Minions", buildMinionsTab(structuredData));
				}
			}
			
			if (!_currentDefinition.getReferences().isEmpty())
			{
				_tabs.addTab("References", buildReferenceTab(_currentDefinition.getReferences()));
			}
			
			restoreSelectedTab(selectedTab);
		}
		finally
		{
			_buildingFields = false;
		}
		
		refreshDirtyState();
	}
	
	private String getSelectedTabTitle()
	{
		final int index = _tabs.getSelectedIndex();
		return index < 0 ? null : _tabs.getTitleAt(index);
	}
	
	private void restoreSelectedTab(String title)
	{
		if (title == null)
		{
			return;
		}
		
		for (int index = 0; index < _tabs.getTabCount(); index++)
		{
			if (title.equals(_tabs.getTitleAt(index)))
			{
				_tabs.setSelectedIndex(index);
				return;
			}
		}
	}
	
	private JScrollPane buildFieldTab(List<NpcField> fields)
	{
		final JPanel content = new JPanel();
		content.setOpaque(false);
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		final Map<String, List<NpcField>> groups = new LinkedHashMap<>();
		for (NpcField field : fields)
		{
			groups.computeIfAbsent(field.getGroup(), key -> new ArrayList<>()).add(field);
		}
		
		for (Entry<String, List<NpcField>> entry : groups.entrySet())
		{
			content.add(buildFieldGroup(entry.getKey(), entry.getValue()));
			content.add(Box.createVerticalStrut(7));
		}
		
		content.add(Box.createVerticalGlue());
		final JScrollPane scroll = new JScrollPane(content);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.getViewport().setBackground(NpcEditorPalette.surface());
		scroll.getVerticalScrollBar().setUnitIncrement(18);
		return scroll;
	}
	
	private JPanel buildFieldGroup(String title, List<NpcField> fields)
	{
		final JPanel panel = createCard(new BorderLayout(0, 7));
		final JLabel header = new JLabel(title);
		header.setFont(header.getFont().deriveFont(Font.BOLD));
		panel.add(header, BorderLayout.NORTH);
		
		final JPanel form = new JPanel(new GridBagLayout());
		form.setOpaque(false);
		int row = 0;
		for (NpcField field : fields)
		{
			final GridBagConstraints labelConstraints = new GridBagConstraints();
			labelConstraints.gridx = 0;
			labelConstraints.gridy = row;
			labelConstraints.anchor = GridBagConstraints.LINE_START;
			labelConstraints.insets = new Insets(4, 4, 4, 12);
			final JLabel label = new JLabel(field.getLabel());
			label.setForeground(NpcEditorPalette.muted());
			form.add(label, labelConstraints);
			
			final GridBagConstraints editorConstraints = new GridBagConstraints();
			editorConstraints.gridx = 1;
			editorConstraints.gridy = row++;
			editorConstraints.weightx = 1;
			editorConstraints.fill = GridBagConstraints.HORIZONTAL;
			editorConstraints.insets = new Insets(4, 0, 4, 4);
			final JComponent editor = createFieldEditor(field);
			_fieldEditors.put(field, editor);
			form.add(editor, editorConstraints);
		}
		
		panel.add(form, BorderLayout.CENTER);
		return panel;
	}
	
	private JComponent createFieldEditor(NpcField field)
	{
		if (field.isReadOnly())
		{
			final JTextField text = new JTextField(field.getValue());
			text.setEditable(false);
			return text;
		}
		
		if (field.getKind() == ValueKind.BOOLEAN)
		{
			final JComboBox<String> combo = new JComboBox<>(new String[]
			{
				"true",
				"false"
			});
			combo.setSelectedItem(field.getValue().toLowerCase());
			combo.addActionListener(event ->
			{
				if (!_buildingFields)
				{
					field.setValue((String) combo.getSelectedItem());
					refreshDirtyState();
				}
			});
			
			return combo;
		}
		
		if ("@type".equals(field.getKey()) && (_index != null))
		{
			return createObservedCombo(field, _index.getTypes());
		}
		
		if ("race#text".equals(field.getKey()) && (_index != null))
		{
			return createObservedCombo(field, _index.getRaces());
		}
		
		if ("sex#text".equals(field.getKey()) && (_index != null))
		{
			return createObservedCombo(field, _index.getSexes());
		}
		
		final JTextField text = new JTextField(field.getValue());
		text.getDocument().addDocumentListener(new DocumentListener()
		{
			private void update()
			{
				if (!_buildingFields)
				{
					field.setValue(text.getText());
					refreshDirtyState();
				}
			}
			
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				update();
			}
			
			@Override
			public void removeUpdate(DocumentEvent event)
			{
				update();
			}
			
			@Override
			public void changedUpdate(DocumentEvent event)
			{
				update();
			}
		});
		
		return text;
	}
	
	private JComboBox<String> createObservedCombo(NpcField field, Set<String> observed)
	{
		final List<String> values = new ArrayList<>(observed);
		if (!values.contains(field.getValue()))
		{
			values.add(field.getValue());
		}
		
		values.sort(String.CASE_INSENSITIVE_ORDER);
		final JComboBox<String> combo = new JComboBox<>(values.toArray(new String[0]));
		combo.setSelectedItem(field.getValue());
		combo.addActionListener(event ->
		{
			if (!_buildingFields)
			{
				field.setValue((String) combo.getSelectedItem());
				refreshDirtyState();
			}
		});
		
		return combo;
	}
	
	private JPanel buildSkillsTab(NpcStructuredData data)
	{
		final SkillTableModel model = new SkillTableModel(data.getSkills());
		final JTable table = new JTable(model);
		configureTable(table);
		table.getColumnModel().getColumn(0).setPreferredWidth(90);
		table.getColumnModel().getColumn(0).setMaxWidth(120);
		table.getColumnModel().getColumn(1).setPreferredWidth(80);
		table.getColumnModel().getColumn(1).setMaxWidth(110);
		
		final JButton add = new JButton("Add Skill");
		final JButton edit = new JButton("Edit");
		final JButton remove = new JButton("Remove");
		stylePrimaryButton(add, NpcEditorPalette.BLUE);
		styleSecondaryButton(edit);
		styleSecondaryButton(remove);
		add.addActionListener(event ->
		{
			final NpcSkillEntry skill = promptSkill(null);
			if (skill != null)
			{
				data.addSkill(skill);
				rebuildEditor();
			}
		});
		edit.addActionListener(event ->
		{
			final int row = requireSelectedRow(table, "skill");
			if (row < 0)
			{
				return;
			}
			
			if (promptSkill(model.getSkill(row)) != null)
			{
				rebuildEditor();
			}
		});
		remove.addActionListener(event ->
		{
			final int row = requireSelectedRow(table, "skill");
			if (row < 0)
			{
				return;
			}
			
			if (data.getSkills().size() <= 1)
			{
				showWarning("Skill List must contain at least one skill.");
				return;
			}
			
			final NpcSkillEntry skill = model.getSkill(row);
			if (confirmRemove("Remove skill " + skill.getId() + " (" + skillName(skill.getId()) + ")?"))
			{
				data.removeSkill(skill);
				rebuildEditor();
			}
		});
		
		final JScrollPane scroll = new JScrollPane(table);
		styleScrollPane(scroll);
		return buildEditableTableCard("NPC Skills", data.getSkills().size() + " entries", scroll, add, edit, remove);
	}
	
	private JScrollPane buildParametersTab(NpcStructuredData data)
	{
		final JPanel content = new JPanel();
		content.setOpaque(false);
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		
		if (!data.getParameters().isEmpty())
		{
			final ParameterTableModel model = new ParameterTableModel(data.getParameters());
			final JTable table = new JTable(model);
			configureTable(table);
			final JButton add = new JButton("Add Parameter");
			final JButton edit = new JButton("Edit");
			final JButton remove = new JButton("Remove");
			stylePrimaryButton(add, NpcEditorPalette.BLUE);
			styleSecondaryButton(edit);
			styleSecondaryButton(remove);
			add.addActionListener(event ->
			{
				final NpcParameterEntry parameter = promptParameter(null);
				if (parameter != null)
				{
					data.addParameter(parameter);
					rebuildEditor();
				}
			});
			edit.addActionListener(event ->
			{
				final int row = requireSelectedRow(table, "parameter");
				if (row < 0)
				{
					return;
				}
				
				if (promptParameter(model.getParameter(row)) != null)
				{
					rebuildEditor();
				}
			});
			remove.addActionListener(event ->
			{
				final int row = requireSelectedRow(table, "parameter");
				if (row < 0)
				{
					return;
				}
				
				if (data.getActiveParameterChildCount() <= 1)
				{
					showWarning("Parameters must contain at least one param, skill, or minions entry.");
					return;
				}
				
				final NpcParameterEntry parameter = model.getParameter(row);
				if (confirmRemove("Remove parameter '" + parameter.getName() + "'?"))
				{
					data.removeParameter(parameter);
					rebuildEditor();
				}
			});
			final JScrollPane scroll = new JScrollPane(table);
			styleScrollPane(scroll);
			final JPanel holder = buildEditableTableCard("Parameters", data.getParameters().size() + " entries", scroll, add, edit, remove);
			holder.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
			content.add(holder);
			content.add(Box.createVerticalStrut(7));
		}
		
		if (!data.getParameterSkills().isEmpty())
		{
			final ParameterSkillTableModel model = new ParameterSkillTableModel(data.getParameterSkills());
			final JTable table = new JTable(model);
			configureTable(table);
			table.getColumnModel().getColumn(1).setPreferredWidth(90);
			table.getColumnModel().getColumn(1).setMaxWidth(120);
			table.getColumnModel().getColumn(2).setPreferredWidth(70);
			table.getColumnModel().getColumn(2).setMaxWidth(100);
			final JButton add = new JButton("Add Parameter Skill");
			final JButton edit = new JButton("Edit");
			final JButton remove = new JButton("Remove");
			stylePrimaryButton(add, NpcEditorPalette.BLUE);
			styleSecondaryButton(edit);
			styleSecondaryButton(remove);
			add.addActionListener(event ->
			{
				final NpcParameterSkillEntry skill = promptParameterSkill(null);
				if (skill != null)
				{
					data.addParameterSkill(skill);
					rebuildEditor();
				}
			});
			edit.addActionListener(event ->
			{
				final int row = requireSelectedRow(table, "parameter skill");
				if (row < 0)
				{
					return;
				}
				
				if (promptParameterSkill(model.getSkill(row)) != null)
				{
					rebuildEditor();
				}
			});
			remove.addActionListener(event ->
			{
				final int row = requireSelectedRow(table, "parameter skill");
				if (row < 0)
				{
					return;
				}
				
				if (data.getActiveParameterChildCount() <= 1)
				{
					showWarning("Parameters must contain at least one param, skill, or minions entry.");
					return;
				}
				
				final NpcParameterSkillEntry skill = model.getSkill(row);
				if (confirmRemove("Remove parameter skill '" + skill.getName() + "' (" + skill.getId() + ")?"))
				{
					data.removeParameterSkill(skill);
					rebuildEditor();
				}
			});
			final JScrollPane scroll = new JScrollPane(table);
			styleScrollPane(scroll);
			final JPanel holder = buildEditableTableCard("Parameter Skills", data.getParameterSkills().size() + " entries", scroll, add, edit, remove);
			holder.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
			content.add(holder);
			content.add(Box.createVerticalStrut(7));
		}
		
		content.add(Box.createVerticalGlue());
		final JScrollPane outer = new JScrollPane(content);
		outer.setBorder(BorderFactory.createEmptyBorder());
		outer.getViewport().setBackground(NpcEditorPalette.surface());
		outer.getVerticalScrollBar().setUnitIncrement(18);
		return outer;
	}
	
	private JPanel buildMinionsTab(NpcStructuredData data)
	{
		final MinionTableModel model = new MinionTableModel(data.getMinionSets());
		final JTable table = new JTable(model);
		configureTable(table);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		final JButton add = new JButton("Add Minion");
		final JButton edit = new JButton("Edit");
		final JButton remove = new JButton("Remove");
		final JButton renameSet = new JButton("Rename Set");
		stylePrimaryButton(add, NpcEditorPalette.BLUE);
		styleSecondaryButton(edit);
		styleSecondaryButton(remove);
		styleSecondaryButton(renameSet);
		add.addActionListener(event ->
		{
			final NpcMinionSet set = chooseMinionSet(data.getMinionSets());
			if (set == null)
			{
				return;
			}
			
			final NpcMinionEntry minion = promptMinion(set, null);
			if (minion != null)
			{
				set.addMinion(minion);
				rebuildEditor();
			}
		});
		edit.addActionListener(event ->
		{
			final int row = requireSelectedRow(table, "minion");
			if (row < 0)
			{
				return;
			}
			
			final MinionRow minionRow = model.getRow(row);
			if (promptMinion(minionRow._set, minionRow._minion) != null)
			{
				rebuildEditor();
			}
		});
		remove.addActionListener(event ->
		{
			final int row = requireSelectedRow(table, "minion");
			if (row < 0)
			{
				return;
			}
			
			final MinionRow minionRow = model.getRow(row);
			if (minionRow._set.getMinions().size() <= 1)
			{
				showWarning("Minion set '" + minionRow._set.getName() + "' must contain at least one NPC.");
				return;
			}
			
			if (confirmRemove("Remove minion " + minionRow._minion.getId() + " (" + npcName(minionRow._minion.getId()) + ") from set '" + minionRow._set.getName() + "'?"))
			{
				minionRow._set.removeMinion(minionRow._minion);
				rebuildEditor();
			}
		});
		renameSet.addActionListener(event ->
		{
			NpcMinionSet set = null;
			final int selectedRow = table.getSelectedRow();
			if (selectedRow >= 0)
			{
				set = model.getRow(selectedRow)._set;
			}
			else
			{
				set = chooseMinionSet(data.getMinionSets());
			}
			
			if ((set != null) && renameMinionSet(set))
			{
				rebuildEditor();
			}
		});
		
		final JScrollPane scroll = new JScrollPane(table);
		styleScrollPane(scroll);
		return buildEditableTableCard("Minions", model.getRowCount() + " entries in " + data.getMinionSets().size() + " set" + (data.getMinionSets().size() == 1 ? "" : "s"), scroll, add, edit, remove, renameSet);
	}
	
	private JPanel buildEditableTableCard(String title, String countText, JScrollPane scroll, JButton... buttons)
	{
		final JPanel panel = createCard(new BorderLayout(0, 7));
		panel.add(buildSectionHeader(title, new JLabel(countText)), BorderLayout.NORTH);
		panel.add(scroll, BorderLayout.CENTER);
		final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		actions.setOpaque(false);
		for (JButton button : buttons)
		{
			actions.add(button);
		}
		
		panel.add(actions, BorderLayout.SOUTH);
		return panel;
	}
	
	private int requireSelectedRow(JTable table, String label)
	{
		final int row = table.getSelectedRow();
		if (row < 0)
		{
			showWarning("Select a " + label + " first.");
		}
		
		return row;
	}
	
	private boolean confirmRemove(String message)
	{
		return JOptionPane.showConfirmDialog(this, message, "Confirm removal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
	}
	
	private void showWarning(String message)
	{
		JOptionPane.showMessageDialog(this, message, "NPC Editor", JOptionPane.WARNING_MESSAGE);
	}
	
	private NpcSkillEntry promptSkill(NpcSkillEntry existing)
	{
		final JTextField idField = new JTextField(existing == null ? "" : Integer.toString(existing.getId()), 14);
		final JTextField levelField = new JTextField(existing == null ? "1" : Integer.toString(existing.getLevel()), 14);
		final JLabel skillName = new JLabel();
		bindSkillName(idField, skillName);
		final JPanel form = createDialogForm();
		addDialogRow(form, 0, "Skill ID", idField);
		addDialogRow(form, 1, "Level", levelField);
		addDialogRow(form, 2, "Skill Name", skillName);
		while (true)
		{
			final int result = JOptionPane.showConfirmDialog(this, form, existing == null ? "Add NPC Skill" : "Edit NPC Skill", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (result != JOptionPane.OK_OPTION)
			{
				return null;
			}
			
			try
			{
				final int id = parsePositiveInt(idField.getText(), "Skill ID");
				final int level = parsePositiveInt(levelField.getText(), "Skill level");
				final NpcSkillInfo info = _index == null ? null : _index.getSkill(id);
				if (info == null)
				{
					throw new IllegalArgumentException("Skill " + id + " does not exist in the active chronicle's data/stats/skills XML.");
				}
				
				if (!info.supportsLevel(level))
				{
					throw new IllegalArgumentException("Skill " + id + " (" + info.getName() + ") supports levels " + info.getMinLevel() + "-" + info.getMaxLevel() + ", not level " + level + ".");
				}
				
				if (existing == null)
				{
					return NpcSkillEntry.create(id, level);
				}
				
				existing.setId(id);
				existing.setLevel(level);
				return existing;
			}
			catch (IllegalArgumentException e)
			{
				showWarning(e.getMessage());
			}
		}
	}
	
	private NpcParameterEntry promptParameter(NpcParameterEntry existing)
	{
		final JTextField nameField = new JTextField(existing == null ? "" : existing.getName(), 24);
		final JTextField valueField = new JTextField(existing == null ? "" : existing.getValue(), 32);
		final JPanel form = createDialogForm();
		addDialogRow(form, 0, "Name", nameField);
		addDialogRow(form, 1, "Value", valueField);
		while (true)
		{
			final int result = JOptionPane.showConfirmDialog(this, form, existing == null ? "Add Parameter" : "Edit Parameter", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (result != JOptionPane.OK_OPTION)
			{
				return null;
			}
			
			final String name = nameField.getText().trim();
			if (name.isEmpty())
			{
				showWarning("Parameter name cannot be empty.");
				continue;
			}
			
			if (existing == null)
			{
				return NpcParameterEntry.create(name, valueField.getText());
			}
			
			existing.setName(name);
			existing.setValue(valueField.getText());
			return existing;
		}
	}
	
	private NpcParameterSkillEntry promptParameterSkill(NpcParameterSkillEntry existing)
	{
		final JTextField nameField = new JTextField(existing == null ? "" : existing.getName(), 24);
		final JTextField idField = new JTextField(existing == null ? "" : Integer.toString(existing.getId()), 14);
		final JTextField levelField = new JTextField(existing == null ? "1" : Integer.toString(existing.getLevel()), 14);
		final JLabel skillName = new JLabel();
		bindSkillName(idField, skillName);
		final JPanel form = createDialogForm();
		addDialogRow(form, 0, "Parameter", nameField);
		addDialogRow(form, 1, "Skill ID", idField);
		addDialogRow(form, 2, "Level", levelField);
		addDialogRow(form, 3, "Skill Name", skillName);
		while (true)
		{
			final int result = JOptionPane.showConfirmDialog(this, form, existing == null ? "Add Parameter Skill" : "Edit Parameter Skill", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (result != JOptionPane.OK_OPTION)
			{
				return null;
			}
			
			try
			{
				final String name = nameField.getText().trim();
				if (name.isEmpty())
				{
					throw new IllegalArgumentException("Parameter skill name cannot be empty.");
				}
				
				final int id = parsePositiveInt(idField.getText(), "Skill ID");
				final int level = parsePositiveInt(levelField.getText(), "Skill level");
				final NpcSkillInfo info = _index == null ? null : _index.getSkill(id);
				if ((info == null) && ((existing == null) || (id != existing.getOriginalId())))
				{
					throw new IllegalArgumentException("Skill " + id + " does not exist in the active chronicle's data/stats/skills XML. Existing historical references may be preserved, but a new skill ID must resolve.");
				}
				
				if (existing == null)
				{
					return NpcParameterSkillEntry.create(name, id, level);
				}
				
				existing.setName(name);
				existing.setId(id);
				existing.setLevel(level);
				return existing;
			}
			catch (IllegalArgumentException e)
			{
				showWarning(e.getMessage());
			}
		}
	}
	
	private NpcMinionSet chooseMinionSet(List<NpcMinionSet> sets)
	{
		if (sets.isEmpty())
		{
			return null;
		}
		
		if (sets.size() == 1)
		{
			return sets.get(0);
		}
		
		final String[] labels = new String[sets.size()];
		for (int index = 0; index < sets.size(); index++)
		{
			labels[index] = sets.get(index).getName() + " [set " + (index + 1) + "]";
		}
		
		final String selected = (String) JOptionPane.showInputDialog(this, "Choose the existing minion set:", "Minion Set", JOptionPane.PLAIN_MESSAGE, null, labels, labels[0]);
		if (selected == null)
		{
			return null;
		}
		
		for (int index = 0; index < labels.length; index++)
		{
			if (selected.equals(labels[index]))
			{
				return sets.get(index);
			}
		}
		
		return null;
	}
	
	private NpcMinionEntry promptMinion(NpcMinionSet set, NpcMinionEntry existing)
	{
		final boolean allowMax = minionSetUsesMax(set);
		final boolean allowWeight = minionSetUsesWeight(set);
		final JTextField setField = new JTextField(set.getName(), 24);
		setField.setEditable(false);
		final JTextField idField = new JTextField(existing == null ? "" : Integer.toString(existing.getId()), 14);
		final JLabel npcName = new JLabel();
		bindNpcName(idField, npcName);
		final JTextField countField = new JTextField(existing == null ? "1" : Integer.toString(existing.getCount()), 14);
		final JTextField maxField = new JTextField((existing == null) || (existing.getMax() == null) ? "" : existing.getMax().toString(), 14);
		final JTextField respawnField = new JTextField(existing == null ? "0" : Integer.toString(existing.getRespawnTime()), 14);
		final JTextField weightField = new JTextField((existing == null) || (existing.getWeightPoint() == null) ? "" : existing.getWeightPoint().toString(), 14);
		final JPanel form = createDialogForm();
		int row = 0;
		addDialogRow(form, row++, "Set", setField);
		addDialogRow(form, row++, "NPC ID", idField);
		addDialogRow(form, row++, "NPC Name", npcName);
		addDialogRow(form, row++, "Count", countField);
		if (allowMax)
		{
			addDialogRow(form, row++, "Max", maxField);
		}
		
		addDialogRow(form, row++, "Respawn Time", respawnField);
		if (allowWeight)
		{
			addDialogRow(form, row, "Weight Point", weightField);
		}
		while (true)
		{
			final int result = JOptionPane.showConfirmDialog(this, form, existing == null ? "Add Minion" : "Edit Minion", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (result != JOptionPane.OK_OPTION)
			{
				return null;
			}
			
			try
			{
				final int id = parsePositiveInt(idField.getText(), "NPC ID");
				if (((_index == null) || (_index.getNpc(id) == null)) && ((existing == null) || (id != existing.getOriginalId())))
				{
					throw new IllegalArgumentException("NPC " + id + " does not exist in the active NPC datapack. Existing historical references may be preserved, but a new NPC ID must resolve.");
				}
				
				final int count = parsePositiveInt(countField.getText(), "Count");
				final Integer max = allowMax ? parseOptionalPositiveInt(maxField.getText(), "Max") : null;
				final int respawn = parseNonNegativeInt(respawnField.getText(), "Respawn Time");
				final Integer weight = allowWeight ? parseOptionalIntValue(weightField.getText(), "Weight Point") : null;
				if (existing == null)
				{
					return NpcMinionEntry.create(id, count, max, respawn, weight);
				}
				
				existing.setId(id);
				existing.setCount(count);
				existing.setMax(max);
				existing.setRespawnTime(respawn);
				existing.setWeightPoint(weight);
				return existing;
			}
			catch (IllegalArgumentException e)
			{
				showWarning(e.getMessage());
			}
		}
	}
	
	private boolean renameMinionSet(NpcMinionSet set)
	{
		final JTextField name = new JTextField(set.getName(), 26);
		while (true)
		{
			final int result = JOptionPane.showConfirmDialog(this, name, "Rename Minion Set", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (result != JOptionPane.OK_OPTION)
			{
				return false;
			}
			
			final String value = name.getText().trim();
			if (value.isEmpty())
			{
				showWarning("Minion set name cannot be empty.");
				continue;
			}
			
			set.setName(value);
			return true;
		}
	}
	
	private boolean minionSetUsesMax(NpcMinionSet set)
	{
		for (NpcMinionEntry minion : set.getMinions())
		{
			if (minion.getMax() != null)
			{
				return true;
			}
		}
		
		return false;
	}
	
	private boolean minionSetUsesWeight(NpcMinionSet set)
	{
		for (NpcMinionEntry minion : set.getMinions())
		{
			if (minion.getWeightPoint() != null)
			{
				return true;
			}
		}
		
		return false;
	}
	
	private JPanel createDialogForm()
	{
		final JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		return panel;
	}
	
	private void addDialogRow(JPanel panel, int row, String labelText, JComponent component)
	{
		final GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridx = 0;
		labelConstraints.gridy = row;
		labelConstraints.anchor = GridBagConstraints.LINE_START;
		labelConstraints.insets = new Insets(4, 4, 4, 12);
		panel.add(new JLabel(labelText), labelConstraints);
		
		final GridBagConstraints valueConstraints = new GridBagConstraints();
		valueConstraints.gridx = 1;
		valueConstraints.gridy = row;
		valueConstraints.weightx = 1;
		valueConstraints.fill = GridBagConstraints.HORIZONTAL;
		valueConstraints.insets = new Insets(4, 0, 4, 4);
		panel.add(component, valueConstraints);
	}
	
	private void bindSkillName(JTextField idField, JLabel nameLabel)
	{
		final DocumentListener listener = new DocumentListener()
		{
			private void update()
			{
				try
				{
					final int id = Integer.parseInt(idField.getText().trim());
					nameLabel.setText(skillName(id));
				}
				catch (NumberFormatException e)
				{
					nameLabel.setText("Enter a valid skill ID");
				}
			}
			
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				update();
			}
			
			@Override
			public void removeUpdate(DocumentEvent event)
			{
				update();
			}
			
			@Override
			public void changedUpdate(DocumentEvent event)
			{
				update();
			}
		};
		idField.getDocument().addDocumentListener(listener);
		try
		{
			nameLabel.setText(skillName(Integer.parseInt(idField.getText().trim())));
		}
		catch (NumberFormatException e)
		{
			nameLabel.setText("Enter a valid skill ID");
		}
	}
	
	private void bindNpcName(JTextField idField, JLabel nameLabel)
	{
		final DocumentListener listener = new DocumentListener()
		{
			private void update()
			{
				try
				{
					final int id = Integer.parseInt(idField.getText().trim());
					nameLabel.setText(npcName(id));
				}
				catch (NumberFormatException e)
				{
					nameLabel.setText("Enter a valid NPC ID");
				}
			}
			
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				update();
			}
			
			@Override
			public void removeUpdate(DocumentEvent event)
			{
				update();
			}
			
			@Override
			public void changedUpdate(DocumentEvent event)
			{
				update();
			}
		};
		idField.getDocument().addDocumentListener(listener);
		try
		{
			nameLabel.setText(npcName(Integer.parseInt(idField.getText().trim())));
		}
		catch (NumberFormatException e)
		{
			nameLabel.setText("Enter a valid NPC ID");
		}
	}
	
	private String skillName(int skillId)
	{
		if (_index == null)
		{
			return "Skill index unavailable";
		}
		
		final NpcSkillInfo info = _index.getSkill(skillId);
		return info == null ? "[Not found in active skill XML]" : info.getName();
	}
	
	private String npcName(int npcId)
	{
		if (_index == null)
		{
			return "NPC index unavailable";
		}
		
		final NpcRecord npc = _index.getNpc(npcId);
		return npc == null ? "[Not found in active NPC XML]" : npc.getName();
	}
	
	private int parsePositiveInt(String value, String label)
	{
		final int parsed = parseIntValue(value, label);
		if (parsed <= 0)
		{
			throw new IllegalArgumentException(label + " must be positive.");
		}
		
		return parsed;
	}
	
	private int parseNonNegativeInt(String value, String label)
	{
		final int parsed = parseIntValue(value, label);
		if (parsed < 0)
		{
			throw new IllegalArgumentException(label + " cannot be negative.");
		}
		
		return parsed;
	}
	
	private int parseIntValue(String value, String label)
	{
		try
		{
			return Integer.parseInt(value.trim());
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException(label + " must be a valid integer.", e);
		}
	}
	
	private Integer parseOptionalPositiveInt(String value, String label)
	{
		if (value.trim().isEmpty())
		{
			return null;
		}
		
		return Integer.valueOf(parsePositiveInt(value, label));
	}
	
	private Integer parseOptionalIntValue(String value, String label)
	{
		if (value.trim().isEmpty())
		{
			return null;
		}
		
		return Integer.valueOf(parseIntValue(value, label));
	}
	
	private JScrollPane buildReferenceTab(List<NpcReferenceSection> sections)
	{
		final JPanel content = new JPanel();
		content.setOpaque(false);
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		for (NpcReferenceSection section : sections)
		{
			final DefaultTableModel model = new DefaultTableModel(section.getColumns(), 0)
			{
				private static final long serialVersionUID = 1L;
				
				@Override
				public boolean isCellEditable(int row, int column)
				{
					return false;
				}
			};
			for (String[] row : section.getRows())
			{
				model.addRow(row);
			}
			
			final JTable table = new JTable(model);
			table.setRowHeight(24);
			table.setFillsViewportHeight(true);
			configureTable(table);
			final JScrollPane scroll = new JScrollPane(table);
			styleScrollPane(scroll);
			scroll.setPreferredSize(new Dimension(600, Math.min(240, 65 + (section.getRows().size() * 24))));
			final JPanel holder = buildTableCard(section.getTitle(), new JLabel(section.getRows().size() + " entries"), scroll);
			holder.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
			content.add(holder);
			content.add(Box.createVerticalStrut(7));
		}
		
		content.add(Box.createVerticalGlue());
		final JScrollPane outer = new JScrollPane(content);
		outer.setBorder(BorderFactory.createEmptyBorder());
		outer.getViewport().setBackground(NpcEditorPalette.surface());
		outer.getVerticalScrollBar().setUnitIncrement(18);
		return outer;
	}
	
	private void refreshDirtyState()
	{
		if (_currentDefinition == null)
		{
			_fieldCounter.setText("0 fields");
			_dirtyBadge.setBadge("CLEAN", NpcEditorPalette.GREEN);
			updateActions();
			return;
		}
		
		for (Entry<NpcField, JComponent> entry : _fieldEditors.entrySet())
		{
			final JComponent component = entry.getValue();
			if (entry.getKey().isChanged())
			{
				component.setBackground(NpcEditorPalette.changed());
			}
			else
			{
				component.setBackground(UIManager.getColor("TextField.background"));
			}
		}
		
		final NpcStructuredData structuredData = _currentDefinition.getStructuredData();
		final int changes = _currentDefinition.getChangedCount();
		final StringBuilder counter = new StringBuilder();
		counter.append(_currentDefinition.getFields().size()).append(" fields");
		if (structuredData.hasSkillList())
		{
			counter.append(" · ").append(structuredData.getSkills().size()).append(" skills");
		}
		
		if (structuredData.hasParameters())
		{
			counter.append(" · ").append(structuredData.getParameters().size()).append(" params");
			if (!structuredData.getParameterSkills().isEmpty())
			{
				counter.append(" · ").append(structuredData.getParameterSkills().size()).append(" param skills");
			}
			
			if (!structuredData.getMinionSets().isEmpty())
			{
				counter.append(" · ").append(structuredData.getMinionCount()).append(" minions");
			}
		}
		
		_fieldCounter.setText(counter.toString());
		_dirtyBadge.setBadge(changes == 0 ? "CLEAN" : changes + " CHANGED", changes == 0 ? NpcEditorPalette.GREEN : NpcEditorPalette.ORANGE);
		if (_index != null)
		{
			final Path relative;
			try
			{
				relative = _index.getLayout().getSelectedRoot().relativize(_currentNpc.getFile());
			}
			catch (IllegalArgumentException e)
			{
				_status.setText(_currentNpc.getFile().toString());
				updateActions();
				return;
			}
			
			_status.setText(relative + (changes == 0 ? "" : " · " + changes + " unsaved change" + (changes == 1 ? "" : "s")));
		}
		
		updateActions();
	}
	
	private void updateActions()
	{
		final boolean selected = _currentDefinition != null;
		_openDrops.setEnabled(selected && (_openDropsAction != null) && !isBusy());
		_validate.setEnabled(selected && !isBusy());
		_reload.setEnabled(selected && !isBusy());
		_revert.setEnabled(selected && hasUnsavedChanges() && !isBusy());
		_save.setEnabled(selected && hasUnsavedChanges() && !isBusy());
	}
	
	private void openCurrentDrops()
	{
		if ((_currentNpc == null) || (_openDropsAction == null) || isBusy())
		{
			return;
		}
		
		if (hasUnsavedChanges() && !confirmDiscardOrSave())
		{
			return;
		}
		
		_openDropsAction.accept(_currentNpc.getId());
	}
	
	private void validateCurrentNpc()
	{
		if ((_currentDefinition == null) || (_index == null) || (_currentNpc == null) || isBusy())
		{
			return;
		}
		
		final NpcDefinition definition = _currentDefinition;
		final NpcIndex index = _index;
		final int npcId = _currentNpc.getId();
		setBusy(true);
		_status.setText("Validating NPC " + npcId + "...");
		new SwingWorker<String, Void>()
		{
			@Override
			protected String doInBackground() throws Exception
			{
				_xmlEditor.validate(definition, index);
				final NpcStructuredData data = definition.getStructuredData();
				final StringBuilder message = new StringBuilder();
				message.append("XML / XSD: PASS");
				message.append("\nScalar values: PASS");
				message.append("\nExternal modification: NONE");
				message.append("\nNPC skills: ").append(data.hasSkillList() ? data.getSkills().size() + " PASS" : "N/A");
				message.append("\nParameters: ").append(data.hasParameters() ? data.getParameters().size() + " PASS" : "N/A");
				message.append("\nParameter skills: ").append(data.getParameterSkills().isEmpty() ? "N/A" : data.getParameterSkills().size() + " PASS (new IDs resolve)");
				message.append("\nMinions: ").append(data.getMinionSets().isEmpty() ? "N/A" : data.getMinionCount() + " PASS (new IDs resolve)");
				return message.toString();
			}
			
			@Override
			protected void done()
			{
				try
				{
					JOptionPane.showMessageDialog(NpcEditorPanel.this, get(), "NPC validation - " + npcId, JOptionPane.INFORMATION_MESSAGE);
					_status.setText("NPC " + npcId + " validation passed.");
				}
				catch (Exception e)
				{
					showError("NPC validation failed", e);
				}
				finally
				{
					setBusy(false);
				}
			}
		}.execute();
	}
	
	private void reloadCurrentNpc()
	{
		if (_currentNpc == null)
		{
			return;
		}
		
		if (hasUnsavedChanges() && !confirmDiscardOrSave())
		{
			return;
		}
		
		loadNpc(_currentNpc);
	}
	
	private boolean saveCurrentNpc()
	{
		if ((_currentDefinition == null) || (_index == null) || !_currentDefinition.isDirty() || isBusy())
		{
			return !hasUnsavedChanges();
		}
		
		try
		{
			setBusy(true);
			final Path backup = _xmlEditor.save(_currentDefinition, _index);
			final NpcRecord reloadNpc = _currentNpc;
			_currentDefinition = _xmlEditor.load(reloadNpc);
			rebuildEditor();
			_status.setText("Saved " + reloadNpc.getFile().getFileName() + (backup == null ? "" : " · backup: " + backup));
			return true;
		}
		catch (Exception e)
		{
			showError("NPC save failed", e);
			return false;
		}
		finally
		{
			setBusy(false);
		}
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
			for (int row = 0; row < _npcModel.getRowCount(); row++)
			{
				if (_npcModel.getNpc(row).getId() == _currentNpc.getId())
				{
					final int view = _npcTable.convertRowIndexToView(row);
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
	
	private void setBusy(boolean busy)
	{
		if (busy)
		{
			_activeOperations++;
		}
		else
		{
			_activeOperations = Math.max(0, _activeOperations - 1);
		}
		
		if (_activityListener != null)
		{
			_activityListener.accept(isBusy());
		}
		
		updateActions();
	}
	
	private void showError(String title, Exception exception)
	{
		final Throwable cause = exception.getCause() == null ? exception : exception.getCause();
		JOptionPane.showMessageDialog(this, cause.getMessage() == null ? cause.toString() : cause.getMessage(), title, JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * Provides the NPC skill table view with skill names resolved from the active chronicle.
	 */
	private final class SkillTableModel extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;
		private final List<NpcSkillEntry> _skills;
		
		private SkillTableModel(List<NpcSkillEntry> skills)
		{
			_skills = new ArrayList<>(skills);
		}
		
		public NpcSkillEntry getSkill(int row)
		{
			return _skills.get(row);
		}
		
		@Override
		public int getRowCount()
		{
			return _skills.size();
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
			
			if (column == 1)
			{
				return "Level";
			}
			
			return "Skill Name";
		}
		
		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			final NpcSkillEntry skill = _skills.get(rowIndex);
			if (columnIndex == 0)
			{
				return skill.getId();
			}
			
			if (columnIndex == 1)
			{
				return skill.getLevel();
			}
			
			return skillName(skill.getId());
		}
		
		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			return columnIndex < 2 ? Integer.class : String.class;
		}
	}
	
	/**
	 * Provides the NPC parameter name and value table view.
	 */
	private static final class ParameterTableModel extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;
		private final List<NpcParameterEntry> _parameters;
		
		private ParameterTableModel(List<NpcParameterEntry> parameters)
		{
			_parameters = new ArrayList<>(parameters);
		}
		
		public NpcParameterEntry getParameter(int row)
		{
			return _parameters.get(row);
		}
		
		@Override
		public int getRowCount()
		{
			return _parameters.size();
		}
		
		@Override
		public int getColumnCount()
		{
			return 2;
		}
		
		@Override
		public String getColumnName(int column)
		{
			return column == 0 ? "Name" : "Value";
		}
		
		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			final NpcParameterEntry parameter = _parameters.get(rowIndex);
			return columnIndex == 0 ? parameter.getName() : parameter.getValue();
		}
	}
	
	/**
	 * Provides the NPC parameter-skill table view with skill names resolved from the active chronicle.
	 */
	private final class ParameterSkillTableModel extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;
		private final List<NpcParameterSkillEntry> _skills;
		
		private ParameterSkillTableModel(List<NpcParameterSkillEntry> skills)
		{
			_skills = new ArrayList<>(skills);
		}
		
		public NpcParameterSkillEntry getSkill(int row)
		{
			return _skills.get(row);
		}
		
		@Override
		public int getRowCount()
		{
			return _skills.size();
		}
		
		@Override
		public int getColumnCount()
		{
			return 4;
		}
		
		@Override
		public String getColumnName(int column)
		{
			switch (column)
			{
				case 0:
				{
					return "Parameter";
				}
				case 1:
				{
					return "ID";
				}
				case 2:
				{
					return "Level";
				}
				default:
				{
					return "Skill Name";
				}
			}
		}
		
		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			final NpcParameterSkillEntry skill = _skills.get(rowIndex);
			switch (columnIndex)
			{
				case 0:
				{
					return skill.getName();
				}
				case 1:
				{
					return skill.getId();
				}
				case 2:
				{
					return skill.getLevel();
				}
				default:
				{
					return skillName(skill.getId());
				}
			}
		}
		
		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			return (columnIndex == 1) || (columnIndex == 2) ? Integer.class : String.class;
		}
	}
	
	/**
	 * Provides the minion table view while retaining each entry's owning minion set.
	 */
	private final class MinionTableModel extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;
		private final List<MinionRow> _rows = new ArrayList<>();
		private final boolean _showMax;
		private final boolean _showWeight;
		
		private MinionTableModel(List<NpcMinionSet> sets)
		{
			boolean showMax = false;
			boolean showWeight = false;
			for (NpcMinionSet set : sets)
			{
				for (NpcMinionEntry minion : set.getMinions())
				{
					_rows.add(new MinionRow(set, minion));
					showMax |= minion.getMax() != null;
					showWeight |= minion.getWeightPoint() != null;
				}
			}
			
			_showMax = showMax;
			_showWeight = showWeight;
		}
		
		public MinionRow getRow(int row)
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
			return 5 + (_showMax ? 1 : 0) + (_showWeight ? 1 : 0);
		}
		
		@Override
		public String getColumnName(int column)
		{
			int current = 0;
			if (column == current++)
			{
				return "Set";
			}
			
			if (column == current++)
			{
				return "NPC ID";
			}
			
			if (column == current++)
			{
				return "NPC Name";
			}
			
			if (column == current++)
			{
				return "Count";
			}
			
			if (_showMax && (column == current++))
			{
				return "Max";
			}
			
			if (column == current++)
			{
				return "Respawn";
			}
			
			return "Weight";
		}
		
		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			final MinionRow row = _rows.get(rowIndex);
			int current = 0;
			if (columnIndex == current++)
			{
				return row._set.getName();
			}
			
			if (columnIndex == current++)
			{
				return row._minion.getId();
			}
			
			if (columnIndex == current++)
			{
				return npcName(row._minion.getId());
			}
			
			if (columnIndex == current++)
			{
				return row._minion.getCount();
			}
			
			if (_showMax && (columnIndex == current++))
			{
				return row._minion.getMax();
			}
			
			if (columnIndex == current++)
			{
				return row._minion.getRespawnTime();
			}
			
			return row._minion.getWeightPoint();
		}
	}
	
	/**
	 * Associates one displayed minion entry with its owning minion set.
	 */
	private static final class MinionRow
	{
		private final NpcMinionSet _set;
		private final NpcMinionEntry _minion;
		
		private MinionRow(NpcMinionSet set, NpcMinionEntry minion)
		{
			_set = set;
			_minion = minion;
		}
	}
	
	/**
	 * Provides the NPC browser table model used by the editor.
	 */
	private static final class NpcTableModel extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;
		private final List<NpcRecord> _npcs = new ArrayList<>();
		
		public void setNpcs(List<NpcRecord> npcs)
		{
			_npcs.clear();
			_npcs.addAll(npcs);
			_npcs.sort(new Comparator<NpcRecord>()
			{
				@Override
				public int compare(NpcRecord first, NpcRecord second)
				{
					return Integer.compare(first.getId(), second.getId());
				}
			});
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
			return 2;
		}
		
		@Override
		public String getColumnName(int column)
		{
			return column == 0 ? "ID" : "NPC";
		}
		
		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			final NpcRecord npc = _npcs.get(rowIndex);
			return columnIndex == 0 ? npc.getId() : npc.getName();
		}
		
		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			return columnIndex == 0 ? Integer.class : String.class;
		}
	}
}
