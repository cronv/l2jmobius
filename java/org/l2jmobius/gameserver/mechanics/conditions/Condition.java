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
package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.holders.player.DailyMissionDataHolder;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Represents a base condition used to evaluate specific game logic or requirements.<br>
 * This class serves as the foundation for checking various states within the {@code Creature} system.
 * @author mkizub
 */
public abstract class Condition implements ConditionListener
{
	private ConditionListener _listener;
	private String _msg;
	private int _msgId;
	private boolean _addName = false;
	private boolean _result;
	
	/**
	 * Sets the message for this {@code Condition}.<br>
	 * This updates the internal {@code _msg} field.
	 * @param msg The new message string to set.
	 */
	public void setMessage(String msg)
	{
		_msg = msg;
	}
	
	/**
	 * Retrieves the current message associated with this condition.
	 * @return the {@code String} message.
	 */
	public String getMessage()
	{
		return _msg;
	}
	
	/**
	 * Sets the unique identifier for the message.<br>
	 * This value is used to identify specific messages within a {@code Condition}.
	 * @param msgId The new ID to assign to the message.
	 */
	public void setMessageId(int msgId)
	{
		_msgId = msgId;
	}
	
	/**
	 * Retrieves the unique identifier for the message.<br>
	 * This ID is used to identify specific messages associated with this {@code Condition}.
	 * @return the current {@code int} value of the message ID.
	 */
	public int getMessageId()
	{
		return _msgId;
	}
	
	/**
	 * Enables the name addition flag.<br>
	 * This sets the {@code _addName} field to {@code true}.
	 */
	public void addName()
	{
		_addName = true;
	}
	
	/**
	 * Checks if the name should be added.<br>
	 * This method returns the current state of the {@code _addName} flag.
	 * @return {@code true} if the name is set to be added, {@code false} otherwise.
	 */
	public boolean isAddName()
	{
		return _addName;
	}
	
	/**
	 * Sets the {@code ConditionListener} for this condition.<br>
	 * This method also triggers a change notification.
	 * @param listener the new {@code ConditionListener} to assign
	 */
	void setListener(ConditionListener listener)
	{
		_listener = listener;
		notifyChanged();
	}
	
	/**
	 * Retrieves the current {@code ConditionListener} associated with this condition.<br>
	 * This method returns the listener object stored in the internal field.
	 * @return the {@code ConditionListener} instance, or {@code null} if no listener is set.
	 */
	ConditionListener getListener()
	{
		return _listener;
	}
	
	/**
	 * Checks if the condition is met for a specific action.<br>
	 * This method evaluates the interaction between the {@code caster}, the {@code target}, and the {@code skill}.<br>
	 * It returns {@code true} if the condition passes.
	 * @param caster The creature performing the action.
	 * @param target The creature receiving the action.
	 * @param skill The skill being used.
	 * @return {@code true} if the condition is satisfied, otherwise {@code false}.
	 */
	public boolean test(Creature caster, Creature target, Skill skill)
	{
		return test(caster, target, skill, null);
	}
	
	/**
	 * Checks if the condition is met for a specific action.<br>
	 * This method evaluates the interaction between two creatures and an item.
	 * @param caster The creature performing the action.
	 * @param target The creature receiving the action.
	 * @param item The item involved in the action.
	 * @return {@code true} if the condition is met, {@code false} otherwise.
	 */
	public boolean test(Creature caster, Creature target, ItemTemplate item)
	{
		return test(caster, target, null, null);
	}
	
	/**
	 * Checks if the condition is met for a specific daily mission reward.<br>
	 * This method validates the requirements based on the {@code caster} and the {@code onewayreward}.
	 * @param caster The {@code Creature} performing the action.
	 * @param onewayreward The {@code DailyMissionDataHolder} containing the reward data.
	 * @return {@code true} if the condition is satisfied, {@code false} otherwise.
	 */
	public boolean test(Creature caster, DailyMissionDataHolder onewayreward)
	{
		return test(caster, null, null, null);
	}
	
	/**
	 * Checks if the condition is met for a specific action.<br>
	 * This method evaluates the interaction between the caster, target, skill, and item.<br>
	 * It updates the internal result and notifies listeners if the state changes.
	 * @param caster The {@code Creature} performing the action.
	 * @param target The {@code Creature} receiving the action.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code ItemTemplate} involved in the action.
	 * @return {@code true} if the condition is met, {@code false} otherwise.
	 */
	public boolean test(Creature caster, Creature target, Skill skill, ItemTemplate item)
	{
		final boolean res = testImpl(caster, target, skill, item);
		if ((_listener != null) && (res != _result))
		{
			_result = res;
			notifyChanged();
		}
		
		return res;
	}
	
	/**
	 * Test the condition.
	 * @param effector the effector
	 * @param effected the effected
	 * @param skill the skill
	 * @param item the item
	 * @return {@code true} if successful, {@code false} otherwise
	 */
	public abstract boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item);
	
	/**
	 * Notifies the listener that this condition has changed.<br>
	 * This method checks if a {@code ConditionListener} is assigned.<br>
	 * If it exists, it calls its {@code notifyChanged()} method.
	 */
	@Override
	public void notifyChanged()
	{
		if (_listener != null)
		{
			_listener.notifyChanged();
		}
	}
}
