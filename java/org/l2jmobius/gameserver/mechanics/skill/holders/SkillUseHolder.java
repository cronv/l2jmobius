/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.mechanics.skill.holders;

import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class represents a holder for skills that can be actively used by an entity.<br>
 * It provides the necessary context and data required during the execution of a {@code Skill}.
 * @author UnAfraid
 */
public class SkillUseHolder extends SkillHolder
{
	private final Item _item;
	private final boolean _ctrlPressed;
	private final boolean _shiftPressed;
	
	/**
	 * Creates a new {@code SkillUseHolder} instance.<br>
	 * This constructor initializes the skill, the associated item, and the modifier keys.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} required for this action.
	 * @param ctrlPressed Whether the {@code Ctrl} key is held down.
	 * @param shiftPressed Whether the {@code Shift} key is held down.
	 */
	public SkillUseHolder(Skill skill, Item item, boolean ctrlPressed, boolean shiftPressed)
	{
		super(skill);
		_item = item;
		_ctrlPressed = ctrlPressed;
		_shiftPressed = shiftPressed;
	}
	
	/**
	 * Retrieves the {@code Item} associated with this skill holder.<br>
	 * This method returns the specific item used to trigger the skill.
	 * @return The {@code Item} object.
	 */
	public Item getItem()
	{
		return _item;
	}
	
	/**
	 * Checks if the {@code Ctrl} key was pressed.<br>
	 * This is used to determine specific skill usage conditions.
	 * @return {@code true} if the key was pressed, {@code false} otherwise.
	 */
	public boolean isCtrlPressed()
	{
		return _ctrlPressed;
	}
	
	/**
	 * Checks if the Shift key was pressed during a skill action.<br>
	 * This method returns the state of the {@code _shiftPressed} variable.
	 * @return {@code true} if the Shift key was held down, {@code false} otherwise.
	 */
	public boolean isShiftPressed()
	{
		return _shiftPressed;
	}
}
