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
import org.l2jmobius.tools.editor.drop.model.DropType;
import org.l2jmobius.tools.editor.drop.model.ItemRecord;

/**
 * Modal editor for an existing drop item and its optional group chance.
 * @author BazookaRpm
 */
final class DropEditDialog extends JDialog
{
	private static final long serialVersionUID = 1L;
	
	private final DataIndex _index;
	private final DropEditorProfile _profile = DropEditorProfile.current();
	private final DropType _type;
	private final DropGroup _group;
	private final DropItem _item;
	private final JTextField _groupChance = new JTextField(10);
	private final JTextField _itemId = new JTextField(10);
	private final JLabel _itemName = new JLabel(" ");
	private final JTextField _min = new JTextField(10);
	private final JTextField _max = new JTextField(10);
	private final JTextField _chance = new JTextField(10);
	private final JTextField _minLevel = new JTextField(10);
	private final JTextField _maxLevel = new JTextField(10);
	private final JTextField _dailyLimit = new JTextField(10);
	private boolean _accepted;
	
	DropEditDialog(Window owner, DataIndex index, DropType type, DropGroup group, DropItem item)
	{
		super(owner, "Edit drop", Dialog.ModalityType.APPLICATION_MODAL);
		_index = index;
		_type = type;
		_group = group;
		_item = item;
		loadValues();
		buildUi();
		bindEvents();
		refreshItemName();
		pack();
		setMinimumSize(new Dimension(560, getPreferredSize().height));
		setLocationRelativeTo(owner);
	}
	
	private void loadValues()
	{
		_groupChance.setText(_group == null ? "" : _group.getChance());
		_itemId.setText(Integer.toString(_item.getItemId()));
		_min.setText(_item.getMin());
		_max.setText(_item.getMax());
		_chance.setText(_item.getChance());
		_minLevel.setText(_item.getMinLevel() == null ? "" : _item.getMinLevel());
		_maxLevel.setText(_item.getMaxLevel() == null ? "" : _item.getMaxLevel());
		_dailyLimit.setText(_item.getDailyLimit() == null ? "" : _item.getDailyLimit());
	}
	
	private void buildUi()
	{
		final JPanel content = new JPanel(new BorderLayout(0, 10));
		content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		content.add(buildHeader(), BorderLayout.NORTH);
		content.add(buildFields(), BorderLayout.CENTER);
		content.add(buildButtons(), BorderLayout.SOUTH);
		setContentPane(content);
	}
	
	private JPanel buildHeader()
	{
		final JPanel panel = new JPanel(new BorderLayout(8, 0));
		final JPanel titles = new JPanel(new BorderLayout(0, 2));
		final JLabel title = new JLabel("Edit drop entry");
		title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 3f));
		final JLabel subtitle = new JLabel("Update the selected item without changing its drop type or placement.");
		subtitle.setForeground(DropEditorPalette.muted());
		titles.add(title, BorderLayout.NORTH);
		titles.add(subtitle, BorderLayout.SOUTH);
		panel.add(titles, BorderLayout.WEST);
		panel.add(new DropEditorBadge(_type.name(), DropEditorPalette.ACCENT), BorderLayout.EAST);
		return panel;
	}
	
	private JPanel buildFields()
	{
		final JPanel panel = new JPanel(new GridBagLayout());
		panel.setOpaque(true);
		panel.setBackground(DropEditorPalette.panel());
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(DropEditorPalette.border()), "Drop values"), BorderFactory.createEmptyBorder(2, 5, 4, 5)));
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(5, 7, 5, 7);
		constraints.anchor = GridBagConstraints.WEST;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		int row = 0;
		addRow(panel, constraints, row++, "Drop type", new DropEditorBadge(_type.name(), DropEditorPalette.ACCENT));
		if (_group != null)
		{
			addRow(panel, constraints, row++, "Group chance", _groupChance);
		}
		
		addRow(panel, constraints, row++, "Item ID", _itemId);
		addRow(panel, constraints, row++, "Resolved item", _itemName);
		addRow(panel, constraints, row++, "Min amount", _min);
		addRow(panel, constraints, row++, "Max amount", _max);
		addRow(panel, constraints, row++, "Chance %", _chance);
		if (_profile.supportsExtendedAttributes(_type))
		{
			addRow(panel, constraints, row++, "Min level", _minLevel);
			addRow(panel, constraints, row++, "Max level", _maxLevel);
			addRow(panel, constraints, row, "Daily limit", _dailyLimit);
		}
		
		return panel;
	}
	
	private JPanel buildButtons()
	{
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		final JButton cancel = new JButton("Cancel");
		final JButton save = new JButton("Apply Changes");
		styleSecondaryButton(cancel);
		stylePrimaryButton(save, DropEditorPalette.BLUE);
		buttons.add(cancel);
		buttons.add(save);
		cancel.addActionListener(e -> dispose());
		save.addActionListener(e -> accept());
		return buttons;
	}
	
	private void bindEvents()
	{
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
	
	private void refreshItemName()
	{
		try
		{
			final int itemId = Integer.parseInt(_itemId.getText().trim());
			final ItemRecord item = _index.getItem(itemId);
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
			final int itemId = Integer.parseInt(_itemId.getText().trim());
			final ItemRecord record = _index.getItem(itemId);
			if (record == null)
			{
				throw new IllegalArgumentException("Item " + itemId + " does not exist in data/stats/items.");
			}
			
			if (_profile.usesIntAmounts(_type))
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
			if (_group != null)
			{
				validateDouble(_groupChance.getText(), "Group chance");
			}
			
			if (_profile.supportsExtendedAttributes(_type))
			{
				final String minLevel = _minLevel.getText().trim();
				final String maxLevel = _maxLevel.getText().trim();
				final String dailyLimit = _dailyLimit.getText().trim();
				final boolean hasExtended = !minLevel.isEmpty() || !maxLevel.isEmpty() || !dailyLimit.isEmpty();
				if (_profile.requiresExtendedAttributes(_type) || hasExtended)
				{
					if (minLevel.isEmpty() || maxLevel.isEmpty() || dailyLimit.isEmpty())
					{
						throw new IllegalArgumentException("Min level, max level, and daily limit must be provided together.");
					}
					
					validateInt(minLevel, "Min level");
					validateInt(maxLevel, "Max level");
					validateInt(dailyLimit, "Daily limit");
				}
			}
			
			_item.setItemId(itemId);
			_item.setMin(_min.getText().trim());
			_item.setMax(_max.getText().trim());
			_item.setChance(_chance.getText().trim());
			_item.setComment(record.getName());
			if (_group != null)
			{
				_group.setChance(_groupChance.getText().trim());
			}
			
			if (_profile.supportsExtendedAttributes(_type))
			{
				final String minLevel = _minLevel.getText().trim();
				final String maxLevel = _maxLevel.getText().trim();
				final String dailyLimit = _dailyLimit.getText().trim();
				if (minLevel.isEmpty() && maxLevel.isEmpty() && dailyLimit.isEmpty())
				{
					_item.setMinLevel(null);
					_item.setMaxLevel(null);
					_item.setDailyLimit(null);
				}
				else
				{
					_item.setMinLevel(minLevel);
					_item.setMaxLevel(maxLevel);
					_item.setDailyLimit(dailyLimit);
				}
			}
			
			_accepted = true;
			dispose();
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, e.getMessage(), "Invalid drop", JOptionPane.ERROR_MESSAGE);
		}
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
}
