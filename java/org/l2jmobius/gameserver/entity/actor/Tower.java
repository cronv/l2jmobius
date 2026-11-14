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
package org.l2jmobius.gameserver.entity.actor;

import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Siege;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

/**
 * This class serves as the base implementation for tower entities.<br>
 * It provides common functionality for {@code ControlTower} and {@code FlameTower}.
 * @author Zoey76, Mobius
 */
public abstract class Tower extends Npc
{
	/**
	 * Creates a new {@code Tower} instance.<br>
	 * This constructor initializes the tower using a provided template.<br>
	 * It sets the invulnerability status to {@code false}.
	 * @param template The {@code NpcTemplate} used to define the tower properties.
	 */
	public Tower(NpcTemplate template)
	{
		super(template);
		setInvul(false);
	}
	
	/**
	 * Checks if the tower is currently available to be attacked.<br>
	 * This only returns {@code true} during an active siege.<br>
	 * The castle must have a valid residence ID.
	 * @return {@code true} if the tower can be attacked, {@code false} otherwise.
	 */
	@Override
	public boolean canBeAttacked()
	{
		// Attackable during siege by attacker only.
		final Castle castle = getCastle();
		if (castle != null)
		{
			if (castle.getResidenceId() > 0)
			{
				return castle.getSiege().isInProgress();
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if the tower can be attacked by a specific creature.<br>
	 * This is only true during an active siege.<br>
	 * The attacker must be a {@code Player} belonging to an attacking clan.
	 * @param attacker The {@code Creature} attempting to attack.
	 * @return {@code true} if the conditions for auto-attacking are met, otherwise {@code false}.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		// Attackable during siege by attacker only.
		if ((attacker != null) && attacker.isPlayer())
		{
			final Castle castle = getCastle();
			if (castle != null)
			{
				if (castle.getResidenceId() > 0)
				{
					final Siege siege = castle.getSiege();
					if (siege.isInProgress())
					{
						return siege.checkIsAttacker(attacker.asPlayer().getClan());
					}
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Handles what happens when a {@code Player} interacts with this tower.<br>
	 * It checks if the player can target the tower.<br>
	 * If valid, it may set the tower as the player's target or trigger an attack intention.
	 * @param player The {@code Player} who performed the action.
	 * @param interact A boolean indicating if the interaction was intentional.
	 */
	@Override
	public void onAction(Player player, boolean interact)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		if (this != player.getTarget())
		{
			// Set the target of the Player player.
			player.setTarget(this);
		}
		else if (interact && isAutoAttackable(player) && (Math.abs(player.getZ() - getZ()) < 100) && GeoEngine.getInstance().canSeeTarget(player, this))
		{
			// Notify the Player AI with INTERACT.
			player.getAI().setIntentionAttack(this);
		}
		
		// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet.
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * This method is called when a {@code Player} is forced to attack this tower.<br>
	 * It triggers the default action for the interaction.
	 * @param player The {@code Player} who is performing the attack.
	 */
	@Override
	public void onForcedAttack(Player player)
	{
		onAction(player);
	}
}
