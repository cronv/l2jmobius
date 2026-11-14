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

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for teleporting a {@code Creature} to the location of a specific {@code Player}.<br>
 * This effect is typically triggered by skills that target another player.<br>
 * It ensures the destination coordinates are valid before moving the entity.
 * @author Mobius
 */
public class TeleportToPlayer extends AbstractEffect
{
	/**
	 * Creates a new effect that teleports an entity to another player.<br>
	 * This constructor initializes the effect using the provided configuration.
	 * @param params The {@code StatSet} containing the necessary parameters for this effect.
	 */
	public TeleportToPlayer(StatSet params)
	{
	}
	
	/**
	 * Retrieves the type of effect associated with this action.<br>
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} for teleporting to a target.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.TELEPORT_TO_TARGET;
	}
	
	/**
	 * Checks if the effect happens immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the teleport effect immediately.<br>
	 * This method checks if the target is valid for teleportation before moving them.<br>
	 * It sends a system message to the {@code effector} if any restrictions are met.
	 * @param effector The creature casting the skill.
	 * @param effected The creature being affected by the skill.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the skill.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final WorldObject targetObj = effector.getTarget();
		if ((targetObj != null) && (targetObj != effector) && targetObj.isPlayer())
		{
			final Player target = targetObj.asPlayer();
			if (target.isAlikeDead())
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED_OR_TELEPORTED);
				sm.addPcName(target);
				effector.sendPacket(sm);
				return;
			}
			
			if (target.isInStoreMode())
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_CURRENTLY_TRADING_OR_OPERATING_A_PRIVATE_STORE_AND_CANNOT_BE_SUMMONED_OR_TELEPORTED);
				sm.addPcName(target);
				effector.sendPacket(sm);
				return;
			}
			
			if (target.isRooted() || target.isInCombat())
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_ENGAGED_IN_COMBAT_AND_CANNOT_BE_SUMMONED_OR_TELEPORTED);
				sm.addPcName(target);
				effector.sendPacket(sm);
				return;
			}
			
			if (target.isInOlympiadMode())
			{
				effector.sendPacket(SystemMessageId.A_USER_PARTICIPATING_IN_THE_OLYMPIAD_CANNOT_USE_SUMMONING_OR_TELEPORTING);
				return;
			}
			
			if (target.isFlyingMounted() || target.isCombatFlagEquipped() || target.isInTraingCamp())
			{
				effector.sendPacket(SystemMessageId.YOU_CANNOT_USE_SUMMONING_OR_TELEPORTING_IN_THIS_AREA);
				return;
			}
			
			if (target.getObserverMode().isActive() || OlympiadManager.getInstance().isRegisteredInComp(target))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_IN_AN_AREA_WHERE_SUMMONING_OR_TELEPORT_IS_BLOCKED_2);
				sm.addString(target.getName());
				effector.sendPacket(sm);
				return;
			}
			
			if (target.isInsideZone(ZoneId.NO_SUMMON_FRIEND) || target.isInsideZone(ZoneId.JAIL))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_IN_AN_AREA_WHERE_SUMMONING_OR_TELEPORT_IS_BLOCKED);
				sm.addString(target.getName());
				effector.sendPacket(sm);
				return;
			}
			
			final Instance instance = target.getInstanceWorld();
			if (((instance != null) && !instance.isPlayerSummonAllowed()) || target.isInTimedHuntingZone())
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_IN_AN_AREA_WHERE_SUMMONING_OR_TELEPORT_IS_BLOCKED);
				sm.addString(target.getName());
				effector.sendPacket(sm);
				return;
			}
			
			effector.teleToLocation(target, true, null);
		}
	}
}
