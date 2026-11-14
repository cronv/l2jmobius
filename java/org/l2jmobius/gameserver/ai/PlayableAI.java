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
package org.l2jmobius.gameserver.ai;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * This class manages the artificial intelligence for {@code Playable} entities.<br>
 * It serves as a base for specific behaviors such as {@code SummonAI} and {@code PlayerAI}.
 * @author JIV, Mobius
 */
public abstract class PlayableAI extends CreatureAI
{
	/**
	 * Initializes a new instance of {@code PlayableAI}.<br>
	 * This constructor links the AI to a specific {@code Playable} entity.
	 * @param playable The {@code Playable} object that this AI will control.
	 */
	protected PlayableAI(Playable playable)
	{
		super(playable);
	}
	
	/**
	 * Sets the target for an attack action.<br>
	 * This method validates if the {@code target} is a valid objective based on level, reputation, and protection buffs.<br>
	 * If the target is invalid, it sends an error message to the player.
	 * @param target The {@code WorldObject} to be attacked.
	 */
	@Override
	public void setIntentionAttack(WorldObject target)
	{
		if ((target != null) && target.isPlayable())
		{
			final Player player = _actor.asPlayer();
			final Player targetPlayer = target.asPlayer();
			final int playerLevel = player.getLevel();
			final int targetPlayerLevel = targetPlayer.getLevel();
			
			if (targetPlayer.isProtectionBlessingAffected() && ((playerLevel - targetPlayerLevel) >= 10) && (player.getReputation() < 0) && !(target.isInsideZone(ZoneId.PVP)))
			{
				// If attacker have karma and have level >= 10 than his target and target have Newbie Protection Buff.
				player.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
				clientActionFailed();
				return;
			}
			
			if (player.isProtectionBlessingAffected() && ((targetPlayerLevel - playerLevel) >= 10) && (targetPlayer.getReputation() < 0) && !(target.isInsideZone(ZoneId.PVP)))
			{
				// If target have karma and have level >= 10 than his target and actor have Newbie Protection Buff.
				player.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
				clientActionFailed();
				return;
			}
			
			if (targetPlayer.isCursedWeaponEquipped() && (playerLevel <= 20))
			{
				player.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
				clientActionFailed();
				return;
			}
			
			if (player.isCursedWeaponEquipped() && (targetPlayerLevel <= 20))
			{
				player.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
				clientActionFailed();
				return;
			}
		}
		
		super.setIntentionAttack(target);
	}
	
	/**
	 * Sets the current casting intention for the actor.<br>
	 * This method validates if the target is valid based on level and protection rules.<br>
	 * It prevents certain actions against players with Newbie Protection or specific weapon restrictions.
	 * @param skill The {@code Skill} to be cast.
	 * @param target The {@code WorldObject} being targeted.
	 * @param item The {@code Item} used for the cast, if any.
	 * @param forceUse Set to {@code true} to force the use of the skill.
	 * @param dontMove Set to {@code true} to prevent movement during the cast.
	 */
	@Override
	public void setIntentionCast(Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove)
	{
		if ((target != null) && (target.isPlayable()) && skill.hasNegativeEffect())
		{
			final Player player = _actor.asPlayer();
			final Player targetPlayer = target.asPlayer();
			final int playerLevel = player.getLevel();
			final int targetPlayerLevel = targetPlayer.getLevel();
			if (targetPlayer.isProtectionBlessingAffected() && ((playerLevel - targetPlayerLevel) >= 10) && (player.getReputation() < 0) && !target.isInsideZone(ZoneId.PVP))
			{
				// If attacker have karma and have level >= 10 than his target and target have Newbie Protection Buff.
				player.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
				clientActionFailed();
				return;
			}
			
			if (player.isProtectionBlessingAffected() && ((targetPlayerLevel - playerLevel) >= 10) && (targetPlayer.getReputation() < 0) && !target.isInsideZone(ZoneId.PVP))
			{
				// If target have karma and have level >= 10 than his target and actor have Newbie Protection Buff.
				player.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
				clientActionFailed();
				return;
			}
			
			if (targetPlayer.isCursedWeaponEquipped() && ((playerLevel <= 20) || (targetPlayerLevel <= 20)))
			{
				player.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
				clientActionFailed();
				return;
			}
		}
		
		super.setIntentionCast(skill, target, item, forceUse, dontMove);
	}
}
