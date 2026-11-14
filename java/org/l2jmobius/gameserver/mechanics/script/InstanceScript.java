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
package org.l2jmobius.gameserver.mechanics.script;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.instancezone.InstanceReenterType;
import org.l2jmobius.gameserver.entity.instancezone.InstanceTemplate;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Abstract base for instance-zone creation, entry and lifecycle handling.
 * @author FallenAngel, Mobius
 */
public abstract class InstanceScript extends Script
{
	// Instance Configuration.
	private final Set<Integer> _templateIds = new HashSet<>();
	
	/**
	 * Creates an instance script bound to the given template ids.
	 * @param templateIds The instance template ids to manage.
	 * @throws IllegalStateException If no template ids are provided.
	 */
	protected InstanceScript(int... templateIds)
	{
		if (templateIds.length == 0)
		{
			throw new IllegalStateException("No template ids were provided!");
		}
		
		for (Integer id : templateIds)
		{
			_templateIds.add(id);
		}
	}
	
	/**
	 * Retrieves the set of managed template ids.
	 * @return A {@code Set} of managed template ids.
	 */
	public Set<Integer> getTemplateId()
	{
		return _templateIds;
	}
	
	/**
	 * Determines whether the instance is managed by this script.
	 * @param instance The instance to test.
	 * @return {@code true} if non-null and its template id is managed, {@code false} otherwise.
	 */
	public boolean isInInstance(Instance instance)
	{
		return (instance != null) && _templateIds.contains(instance.getTemplateId());
	}
	
	/**
	 * Retrieves the active instance for the player.
	 * @param player The player to query.
	 * @return The active instance, or {@code null} if none.
	 */
	public Instance getPlayerInstance(Player player)
	{
		return InstanceManager.getInstance().getPlayerInstance(player, false);
	}
	
	/**
	 * Broadcasts an on-screen message to all players in the instance.
	 * @param instance The target instance.
	 * @param npcStringId The message string id.
	 * @param position The screen position.
	 * @param timeInMilliseconds The display duration in milliseconds.
	 * @param parameters The message placeholder values.
	 */
	public void showOnScreenMsg(Instance instance, NpcStringId npcStringId, int position, int timeInMilliseconds, String... parameters)
	{
		instance.broadcastPacket(new ExShowScreenMessage(npcStringId, position, timeInMilliseconds, parameters));
	}
	
	/**
	 * Broadcasts an on-screen message, optionally with a visual effect, to all players in the instance.
	 * @param instance The target instance.
	 * @param npcStringId The message string id.
	 * @param position The screen position.
	 * @param timeInMilliseconds The display duration in milliseconds.
	 * @param showVisualEffect {@code true} to show a visual effect.
	 * @param parameters The message placeholder values.
	 */
	public void showOnScreenMsg(Instance instance, NpcStringId npcStringId, int position, int timeInMilliseconds, boolean showVisualEffect, String... parameters)
	{
		instance.broadcastPacket(new ExShowScreenMessage(npcStringId, position, timeInMilliseconds, showVisualEffect, parameters));
	}
	
	/**
	 * Validates entry conditions and moves the player group into a new or existing instance.
	 * @param player The player attempting entry.
	 * @param npc The npc triggering entry.
	 * @param templateId The instance template id.
	 */
	protected void enterInstance(Player player, Npc npc, int templateId)
	{
		Instance instance = getPlayerInstance(player);
		if (instance != null) // Player has already any instance active.
		{
			if (instance.getTemplateId() != templateId)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_ENTER_AS_C1_IS_IN_ANOTHER_INSTANCE_ZONE).addString(player.getName()));
				return;
			}
			
			onEnter(player, instance, false);
		}
		else
		{
			// Get instance template.
			final InstanceManager instanceManager = InstanceManager.getInstance();
			final InstanceTemplate instanceTemplate = instanceManager.getInstanceTemplate(templateId);
			if (instanceTemplate == null)
			{
				LOGGER.warning(player + " wants to create instance with unknown template id " + templateId + "!");
				return;
			}
			
			// Get instance enter scope.
			final List<Player> group = instanceTemplate.getEnterGroup(player);
			
			// When nobody can enter.
			if (group == null)
			{
				LOGGER.warning("Instance " + instanceTemplate.getName() + " (" + templateId + ") has invalid group size limits!");
				return;
			}
			
			// Validate conditions for group.
			if (!player.isGM() && (!instanceTemplate.validateConditions(group, npc, this::showHtmlFile) || !validateConditions(group, npc, instanceTemplate)))
			{
				return;
			}
			
			// Check if maximum world count limit is exceeded.
			final int maxWorlds = instanceTemplate.getMaxWorlds();
			if ((maxWorlds != -1 /* unlimited instances */) && (instanceManager.getWorldCount(templateId) >= maxWorlds))
			{
				player.sendPacket(SystemMessageId.THE_NUMBER_OF_INSTANCE_ZONES_THAT_CAN_BE_CREATED_HAS_BEEN_EXCEEDED_PLEASE_TRY_AGAIN_LATER);
				return;
			}
			
			// Check if any player from enter group has active instance.
			for (Player member : group)
			{
				final String memberName = member.getName();
				if (getPlayerInstance(member) != null)
				{
					for (Player p : group)
					{
						p.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_ENTER_AS_C1_IS_IN_ANOTHER_INSTANCE_ZONE).addString(memberName));
					}
					
					return;
				}
				
				// Check if any player from the group has already finished the instance.
				if (InstanceManager.getInstance().getInstanceTime(member, templateId) > 0)
				{
					for (Player p : group)
					{
						p.sendPacket(new SystemMessage(SystemMessageId.C1_CANNOT_ENTER_YET).addString(memberName));
					}
					
					return;
				}
			}
			
			// Apply condition success effects, so entry fees are taken before anyone is moved inside.
			instanceTemplate.applyConditionEffects(group);
			
			// Create new instance for enter player group.
			instance = instanceManager.createInstance(instanceTemplate, player);
			
			// Move each player from enter group to instance.
			for (Player member : group)
			{
				instance.addAllowed(member);
				onEnter(member, instance, true);
			}
			
			// Set re-enter for instances with re-enter on start.
			if (instance.getReenterType() == InstanceReenterType.ON_ENTER)
			{
				instance.setReenterTime();
			}
		}
	}
	
	/**
	 * Handles a player entering the instance.
	 * @param player The entering player.
	 * @param instance The entered instance.
	 * @param isFirstEntry {@code true} on first entry.
	 */
	protected void onEnter(Player player, Instance instance, boolean isFirstEntry)
	{
		teleportPlayerIn(player, instance);
	}
	
	/**
	 * Teleports the player to the instance enter location, logging a warning if absent.
	 * @param player The player to teleport.
	 * @param instance The destination instance.
	 */
	protected void teleportPlayerIn(Player player, Instance instance)
	{
		final Location location = instance.getEnterLocation();
		if (location != null)
		{
			player.teleToLocation(location, instance);
		}
		else
		{
			LOGGER.warning("Missing start location for instance " + instance.getName() + " (" + instance.getId() + ")");
		}
	}
	
	/**
	 * Ejects the player from the instance.
	 * @param player The player to eject.
	 * @param instance The instance to leave.
	 */
	protected void teleportPlayerOut(Player player, Instance instance)
	{
		instance.ejectPlayer(player);
	}
	
	/**
	 * Finishes the instance the player is in.
	 * @param player The player whose instance is finished.
	 */
	protected void finishInstance(Player player)
	{
		final Instance instance = player.getInstanceWorld();
		if (instance != null)
		{
			instance.finishInstance();
		}
	}
	
	/**
	 * Finishes the instance the player is in after a delay.
	 * @param player The player whose instance is finished.
	 * @param delayInMinutes The minutes to wait before finishing.
	 */
	protected void finishInstance(Player player, int delayInMinutes)
	{
		final Instance instance = player.getInstanceWorld();
		if (instance != null)
		{
			instance.finishInstance(delayInMinutes);
		}
	}
	
	/**
	 * Validates whether the group may enter the instance.
	 * @param group The players attempting entry.
	 * @param npc The npc triggering entry.
	 * @param template The instance template.
	 * @return {@code true} if conditions are met, {@code false} otherwise.
	 */
	protected boolean validateConditions(List<Player> group, Npc npc, InstanceTemplate template)
	{
		return true;
	}
}
