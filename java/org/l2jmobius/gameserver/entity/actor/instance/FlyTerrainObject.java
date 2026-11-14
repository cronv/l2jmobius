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
package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

/**
 * Represents a non-player character that can fly and exists within the game world.<br>
 * This class extends {@code Npc} to provide specific behavior for flying terrain objects.
 */
public class FlyTerrainObject extends Npc
{
	/**
	 * Creates a new {@code FlyTerrainObject} based on the provided template.<br>
	 * This constructor sets the instance type to {@code InstanceType.FlyTerrainObject}.<br>
	 * It also ensures that the object is set to flying status.
	 * @param template The {@code NpcTemplate} used to initialize this object.
	 */
	public FlyTerrainObject(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.FlyTerrainObject);
		setFlying(true);
	}
	
	/**
	 * Handles the action performed by a {@code Player} on this object.<br>
	 * This method sends an {@code ActionFailed} packet to the player.
	 * @param player The {@code Player} who triggered the action.
	 * @param interact A boolean indicating if the interaction was successful.
	 */
	@Override
	public void onAction(Player player, boolean interact)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Handles the action shift logic for a {@code Player}.<br>
	 * This method checks if the player has GM privileges.<br>
	 * If they do, it calls the superclass method.<br>
	 * Otherwise, it sends an {@code ActionFailed.STATIC_PACKET} to the player.
	 * @param player The {@code Player} attempting to perform the action shift.
	 */
	@Override
	public void onActionShift(Player player)
	{
		if (player.isGM())
		{
			super.onActionShift(player);
		}
		else
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
}
