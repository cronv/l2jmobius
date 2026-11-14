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
package handlers.skill.effects;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for resetting a player's entry into an instance.<br>
 * This effect is triggered when a character needs to be moved back to the starting point of an instance.
 * @author Mobius
 */
public class ResetInstanceEntry extends AbstractEffect
{
	private final Set<Integer> _instanceId;
	
	/**
	 * Creates a new {@code ResetInstanceEntry} using the provided parameters.<br>
	 * This constructor initializes the set of instance IDs from the {@code StatSet}.<br>
	 * If no IDs are found, it defaults to an empty set.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public ResetInstanceEntry(StatSet params)
	{
		final String instanceIds = params.getString("instanceId", null);
		if ((instanceIds != null) && !instanceIds.isEmpty())
		{
			_instanceId = new HashSet<>();
			for (String id : instanceIds.split(";"))
			{
				_instanceId.add(Integer.parseInt(id));
			}
		}
		else
		{
			_instanceId = Collections.<Integer> emptySet();
		}
	}
	
	/**
	 * Checks if the effect occurs immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Resets the instance entries for the specified effector.<br>
	 * This method removes the time remaining for all associated instances.<br>
	 * It also sends a confirmation message to the player.
	 * @param effector The {@code Creature} who triggers the effect.
	 * @param effected The {@code Creature} who is affected by the skill.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} involved in the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		for (int instanceId : _instanceId)
		{
			InstanceManager.getInstance().deleteInstanceTime(effector.asPlayer(), instanceId);
			final String instanceName = InstanceManager.getInstance().getInstanceName(instanceId);
			effector.sendMessage(instanceName + " entry has been reset.");
		}
	}
}
