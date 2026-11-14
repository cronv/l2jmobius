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
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.l2jmobius.tools.editor.drop.model.DataIndex;
import org.l2jmobius.tools.editor.drop.model.DropEditorProfile;
import org.l2jmobius.tools.editor.drop.model.DropGroup;
import org.l2jmobius.tools.editor.drop.model.DropItem;
import org.l2jmobius.tools.editor.drop.model.DropSection;
import org.l2jmobius.tools.editor.drop.model.DropType;
import org.l2jmobius.tools.editor.drop.model.ItemRecord;
import org.l2jmobius.tools.editor.drop.model.NpcDrops;

/**
 * Modal editor used to add a new direct or grouped drop item.
 * @author BazookaRpm
 */
final class DropItemDialog extends JDialog
{
	private static final long serialVersionUID = 1L;
	
	private final DataIndex _index;
	private final NpcDrops _drops;
	private final DropEditorProfile _profile;
	private final JComboBox<DropType> _type;
	private final JComboBox<String> _structure = new JComboBox<>();
	private final JComboBox<GroupOption> _existingGroup = new JComboBox<>();
	private final JTextField _groupChance = new JTextField("100", 10);
	private final JTextField _itemId = new JTextField(10);
	private final JLabel _itemName = new JLabel(" ");
	private final JTextField _min = new JTextField("1", 10);
	private final JTextField _max = new JTextField("1", 10);
	private final JTextField _chance = new JTextField("100", 10);
	private final JTextField _minLevel = new JTextField(10);
	private final JTextField _maxLevel = new JTextField(10);
	private final JTextField _dailyLimit = new JTextField(10);
	private boolean _accepted;
	
	DropItemDialog(Window owner, DataIndex index, NpcDrops drops, DropEditorProfile profile)
	{
		super(owner, "Add drop", Dialog.ModalityType.APPLICATION_MODAL);
		_index = index;
		_drops = drops;
		_profile = profile;
		_type = new JComboBox<>(_profile.getSupportedTypes());
		buildUi();
		bindEvents();
		refreshGroups();
		refreshStructureOptions();
		refreshTypeState();
		pack();
		setMinimumSize(new Dimension(580, getPreferredSize().height));
		setLocationRelativeTo(owner);
	}
	
	private void buildUi()
	{
		final JPanel content = new JPanel(new BorderLayout(0, 10));
		content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		content.add(buildHeader(), BorderLayout.NORTH);
		final JPanel fields = new JPanel(new BorderLayout(0, 9));
		fields.add(buildPlacementPanel(), BorderLayout.NORTH);
		fields.add(buildItemPanel(), BorderLayout.CENTER);
		content.add(fields, BorderLayout.CENTER);
		content.add(buildButtons(), BorderLayout.SOUTH);
		setContentPane(content);
	}
	
	private JPanel buildHeader()
	{
		final JPanel panel = new JPanel(new BorderLayout(8, 0));
		final JPanel titles = new JPanel(new BorderLayout(0, 2));
		final JLabel title = new JLabel("Create drop entry");
		title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 3f));
		final JLabel subtitle = new JLabel("Choose a valid source type, placement, item, quantity, and chance.");
		subtitle.setForeground(DropEditorPalette.muted());
		titles.add(title, BorderLayout.NORTH);
		titles.add(subtitle, BorderLayout.SOUTH);
		panel.add(titles, BorderLayout.WEST);
		final JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
		badges.add(new DropEditorBadge(_profile.getDisplayName(), DropEditorPalette.BLUE));
		badges.add(new DropEditorBadge(_profile.getSupportedTypesLabel(), DropEditorPalette.ACCENT));
		panel.add(badges, BorderLayout.EAST);
		return panel;
	}
	
	private JPanel buildPlacementPanel()
	{
		final JPanel panel = createCard("Placement");
		final GridBagConstraints constraints = createConstraints();
		int row = 0;
		addRow(panel, constraints, row++, "Drop type", _type);
		addRow(panel, constraints, row++, "Structure", _structure);
		addRow(panel, constraints, row++, "Existing group", _existingGroup);
		addRow(panel, constraints, row, "New group chance", _groupChance);
		return panel;
	}
	
	private JPanel buildItemPanel()
	{
		final JPanel panel = createCard("Item values");
		final GridBagConstraints constraints = createConstraints();
		int row = 0;
		addRow(panel, constraints, row++, "Item ID", _itemId);
		addRow(panel, constraints, row++, "Resolved item", _itemName);
		addRow(panel, constraints, row++, "Min amount", _min);
		addRow(panel, constraints, row++, "Max amount", _max);
		addRow(panel, constraints, row++, "Chance %", _chance);
		if (_profile.hasExtendedAttributes())
		{
			addRow(panel, constraints, row++, "Min level", _minLevel);
			addRow(panel, constraints, row++, "Max level", _maxLevel);
			addRow(panel, constraints, row, "Daily limit", _dailyLimit);
		}
		
		return panel;
	}
	
	private JPanel createCard(String title)
	{
		final JPanel panel = new JPanel(new GridBagLayout());
		panel.setOpaque(true);
		panel.setBackground(DropEditorPalette.panel());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(DropEditorPalette.border()), title), BorderFactory.createEmptyBorder(2, 5, 4, 5)));
		return panel;
	}
	
	private JPanel buildButtons()
	{
		final JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		final JButton cancel = new JButton("Cancel");
		final JButton add = new JButton("Add Drop");
		styleSecondaryButton(cancel);
		stylePrimaryButton(add, DropEditorPalette.ACCENT);
		panel.add(cancel);
		panel.add(add);
		cancel.addActionListener(e -> dispose());
		add.addActionListener(e -> accept());
		return panel;
	}
	
	private GridBagConstraints createConstraints()
	{
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(5, 7, 5, 7);
		constraints.anchor = GridBagConstraints.WEST;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		return constraints;
	}
	
	private void bindEvents()
	{
		_type.addActionListener(e ->
		{
			refreshGroups();
			refreshStructureOptions();
			refreshTypeState();
		});
		_structure.addActionListener(e -> refreshTypeState());
		_itemId.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				refreshItemName();
			}
			
			@Override
			public void removeUpdate(DocumentEvent e)
			{
				refreshItemName();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e)
			{
				refreshItemName();
			}
		});
	}
	
	private void addRow(JPanel panel, GridBagConstraints constraints, int row, String label, Component field)
	{
		constraints.gridx = 0;
		constraints.gridy = row;
		constraints.weightx = 0;
		final JLabel fieldLabel = new JLabel(label + ":");
		fieldLabel.setForeground(DropEditorPalette.muted());
		panel.add(fieldLabel, constraints);
		constraints.gridx = 1;
		constraints.weightx = 1;
		panel.add(field, constraints);
	}
	
	private void refreshGroups()
	{
		_existingGroup.removeAllItems();
		final DropType type = (DropType) _type.getSelectedItem();
		if (type == null)
		{
			return;
		}
		
		final DropSection section = findSection(type);
		if (section == null)
		{
			return;
		}
		
		for (int i = 0; i < section.getGroups().size(); i++)
		{
			final DropGroup group = section.getGroups().get(i);
			_existingGroup.addItem(new GroupOption(i, group));
		}
	}
	
	private void refreshStructureOptions()
	{
		_structure.removeAllItems();
		final DropType type = (DropType) _type.getSelectedItem();
		if (type == null)
		{
			_structure.setEnabled(false);
			return;
		}
		
		if (_profile.supportsUngrouped(type))
		{
			_structure.addItem("Ungrouped");
		}
		
		if (_profile.supportsGroups(type))
		{
			if (_existingGroup.getItemCount() > 0)
			{
				_structure.addItem("Existing group");
			}
			
			_structure.addItem("New group");
		}
		
		_structure.setEnabled(_structure.getItemCount() > 1);
	}
	
	private void refreshTypeState()
	{
		final DropType type = (DropType) _type.getSelectedItem();
		final String structure = (String) _structure.getSelectedItem();
		_existingGroup.setEnabled("Existing group".equals(structure));
		_groupChance.setEnabled("New group".equals(structure));
		final boolean extended = (type != null) && _profile.supportsExtendedAttributes(type);
		_minLevel.setEnabled(extended);
		_maxLevel.setEnabled(extended);
		_dailyLimit.setEnabled(extended);
		if (extended && _profile.requiresExtendedAttributes(type) && _minLevel.getText().trim().isEmpty() && _maxLevel.getText().trim().isEmpty() && _dailyLimit.getText().trim().isEmpty())
		{
			_minLevel.setText("1");
			_maxLevel.setText("999");
			_dailyLimit.setText("100");
		}
	}
	
	private void refreshItemName()
	{
		try
		{
			final ItemRecord item = _index.getItem(Integer.parseInt(_itemId.getText().trim()));
			_itemName.setText(item == null ? "Unknown item" : item.getName());
			_itemName.setForeground(item == null ? DropEditorPalette.RED : DropEditorPalette.text());
		}
		catch (NumberFormatException e)
		{
			_itemName.setText(" ");
			_itemName.setForeground(DropEditorPalette.text());
		}
	}
	
	private void accept()
	{
		try
		{
			final DropType type = (DropType) _type.getSelectedItem();
			if (type == null)
			{
				throw new IllegalArgumentException("Drop type is required.");
			}
			
			if (!_profile.supports(type))
			{
				throw new IllegalArgumentException("Drop type " + type + " is not enabled for " + _profile.getDisplayName() + ".");
			}
			
			final int itemId = Integer.parseInt(_itemId.getText().trim());
			final ItemRecord record = _index.getItem(itemId);
			if (record == null)
			{
				throw new IllegalArgumentException("Item " + itemId + " does not exist in data/stats/items.");
			}
			
			if (_profile.usesIntAmounts(type))
			{
				validateInt(_min.getText(), "Min amount");
				validateInt(_max.getText(), "Max amount");
			}
			else
			{
				validateLong(_min.getText(), "Min amount");
				validateLong(_max.getText(), "Max amount");
			}
			
			validateDouble(_chance.getText(), "Chance");
			final DropItem item = new DropItem(itemId, _min.getText().trim(), _max.getText().trim(), _chance.getText().trim());
			item.setComment(record.getName());
			if (_profile.supportsExtendedAttributes(type))
			{
				final String minLevel = _minLevel.getText().trim();
				final String maxLevel = _maxLevel.getText().trim();
				final String dailyLimit = _dailyLimit.getText().trim();
				final boolean hasExtended = !minLevel.isEmpty() || !maxLevel.isEmpty() || !dailyLimit.isEmpty();
				if (_profile.requiresExtendedAttributes(type) || hasExtended)
				{
					if (minLevel.isEmpty() || maxLevel.isEmpty() || dailyLimit.isEmpty())
					{
						throw new IllegalArgumentException("Min level, max level, and daily limit must be provided together.");
					}
					
					validateInt(minLevel, "Min level");
					validateInt(maxLevel, "Max level");
					validateInt(dailyLimit, "Daily limit");
					item.setMinLevel(minLevel);
					item.setMaxLevel(maxLevel);
					item.setDailyLimit(dailyLimit);
				}
			}
			
			final DropSection section = _drops.getOrCreateSection(type);
			final String structure = (String) _structure.getSelectedItem();
			if (structure == null)
			{
				throw new IllegalArgumentException("A valid drop structure is required.");
			}
			
			if ("Existing group".equals(structure))
			{
				if (!_profile.supportsGroups(type))
				{
					throw new IllegalArgumentException(type + " does not use grouped drops in " + _profile.getDisplayName() + ".");
				}
				
				final GroupOption option = (GroupOption) _existingGroup.getSelectedItem();
				if (option == null)
				{
					throw new IllegalArgumentException("No existing group is available for this drop type.");
				}
				
				option._group.getItems().add(item);
			}
			else if ("New group".equals(structure))
			{
				if (!_profile.supportsGroups(type))
				{
					throw new IllegalArgumentException(type + " does not use grouped drops in " + _profile.getDisplayName() + ".");
				}
				
				validateDouble(_groupChance.getText(), "Group chance");
				final DropGroup group = new DropGroup(_groupChance.getText().trim());
				group.getItems().add(item);
				section.getGroups().add(group);
			}
			else
			{
				if (!_profile.supportsUngrouped(type))
				{
					throw new IllegalArgumentException(type + " does not use ungrouped drops in " + _profile.getDisplayName() + ".");
				}
				
				section.getItems().add(item);
			}
			
			_accepted = true;
			dispose();
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, e.getMessage(), "Invalid drop", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private DropSection findSection(DropType type)
	{
		for (DropSection section : _drops.getSections())
		{
			if (section.getType() == type)
			{
				return section;
			}
		}
		
		return null;
	}
	
	private void stylePrimaryButton(JButton button, Color background)
	{
		button.setFocusPainted(false);
		button.setOpaque(true);
		button.setContentAreaFilled(true);
		button.setBackground(background);
		button.setForeground(DropEditorPalette.badgeForeground(background));
		button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DropEditorPalette.blend(background, Color.WHITE, 0.18f)), BorderFactory.createEmptyBorder(5, 11, 5, 11)));
	}
	
	private void styleSecondaryButton(JButton button)
	{
		button.setFocusPainted(false);
		button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DropEditorPalette.border()), BorderFactory.createEmptyBorder(5, 11, 5, 11)));
	}
	
	private void validateLong(String value, String label)
	{
		if (Long.parseLong(value.trim()) < 0)
		{
			throw new IllegalArgumentException(label + " cannot be negative.");
		}
	}
	
	private void validateInt(String value, String label)
	{
		if (Integer.parseInt(value.trim()) < 0)
		{
			throw new IllegalArgumentException(label + " cannot be negative.");
		}
	}
	
	private void validateDouble(String value, String label)
	{
		final double parsed = Double.parseDouble(value.trim());
		if (!Double.isFinite(parsed) || (parsed < 0d))
		{
			throw new IllegalArgumentException(label + " must be a finite, non-negative number.");
		}
	}
	
	boolean isAccepted()
	{
		return _accepted;
	}
	
	/**
	 * Represents one selectable drop group in the add-item dialog.
	 */
	private static final class GroupOption
	{
		private final int _index;
		private final DropGroup _group;
		
		private GroupOption(int index, DropGroup group)
		{
			_index = index;
			_group = group;
		}
		
		@Override
		public String toString()
		{
			return "G" + (_index + 1) + "  •  " + _group.getItems().size() + " items  •  " + _group.getChance() + "%";
		}
	}
}
