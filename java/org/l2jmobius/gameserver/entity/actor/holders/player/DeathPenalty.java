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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Manages the Death Penalty debuff of a {@code Player}, called Shilen's Breath since Ertheia.<br>
 * It reads the current level from the effect list and raises or lowers it through the skill.
 * @author Mobius
 */
public class DeathPenalty
{
	private static final Logger LOGGER = Logger.getLogger(DeathPenalty.class.getName());
	
	private final Player _player;
	
	/**
	 * Creates a new {@code DeathPenalty} for a specific player.
	 * @param player The {@code Player} object associated with this instance.
	 */
	public DeathPenalty(Player player)
	{
		_player = player;
	}
	
	/**
	 * Retrieves the current level of the Shilens Breath debuff.<br>
	 * It checks the active effect list for the specific skill ID.<br>
	 * If no such buff exists, it returns 0.
	 * @return The integer level of the Shilens Breath skill or 0 if not active.
	 */
	public int getLevel()
	{
		final BuffInfo buff = _player.getEffectList().getBuffInfoBySkillId(CommonSkill.SHILENS_BREATH.getId());
		return buff == null ? 0 : buff.getSkill().getLevel();
	}
	
	/**
	 * Calculates the level of the Shilen's Breath debuff based on a kill.<br>
	 * This method checks if the player is eligible for death penalties.<br>
	 * It applies various reduction stats depending on whether the killer is a raid, monster, or playable character.<br>
	 * If conditions are met, it triggers an increase to the debuff level.
	 * @param killer The {@code Creature} that killed the player.
	 */
	public void calculate(Creature killer)
	{
		if (killer == null)
		{
			LOGGER.warning(_player + " called DeathPenalty.calculate with killer null!");
			return;
		}
		
		if (_player.isResurrectSpecialAffected() || _player.isLucky() || _player.isBlockedFromDeathPenalty() || _player.isInsideZone(ZoneId.PVP) || _player.isInsideZone(ZoneId.SIEGE) || _player.isGM())
		{
			return;
		}
		
		double percent = 1.0;
		if (killer.isRaid())
		{
			percent *= _player.getStat().getValue(Stat.REDUCE_DEATH_PENALTY_BY_RAID, 1);
		}
		else if (killer.isMonster())
		{
			percent *= _player.getStat().getValue(Stat.REDUCE_DEATH_PENALTY_BY_MOB, 1);
		}
		else if (killer.isPlayable())
		{
			percent *= _player.getStat().getValue(Stat.REDUCE_DEATH_PENALTY_BY_PVP, 1);
		}
		
		if (((killer.isNpc() && killer.asNpc().getTemplate().isDeathPenalty()) || (Rnd.get(1, 100) <= ((PlayerConfig.DEATH_PENALTY_CHANCE) * percent))) && (!killer.isPlayable() || (_player.getReputation() < 0)))
		{
			increase();
		}
	}
	
	/**
	 * Increases the level of the {@code ShilensBreath} debuff on the actor.<br>
	 * This method calculates the next level and applies it if it is within the limit of 5.<br>
	 * It sends a system message to notify the player of the new affliction level.
	 */
	public void increase()
	{
		int nextLv = getLevel() + 1;
		if (nextLv > 5)
		{
			nextLv = 5;
		}
		
		final Skill skill = SkillData.getInstance().getSkill(CommonSkill.SHILENS_BREATH.getId(), nextLv);
		if (skill != null)
		{
			skill.applyEffects(_player, _player);
			_player.sendPacket(new SystemMessage(SystemMessageId.YOU_VE_BEEN_AFFLICTED_BY_SHILLIEN_S_BREATH_LEVEL_S1).addInt(nextLv));
		}
	}
	
	/**
	 * Reduces the level of the {@code ShilensBreathDebuff} on the actor.<br>
	 * If the new level is greater than {@code 0}, it applies the corresponding skill effects.<br>
	 * If the debuff reaches level {@code 0}, it sends a purification message to the player.
	 */
	public void decrease()
	{
		final int nextLv = getLevel() - 1;
		if (nextLv > 0)
		{
			final Skill skill = SkillData.getInstance().getSkill(CommonSkill.SHILENS_BREATH.getId(), nextLv);
			skill.applyEffects(_player, _player);
			_player.sendPacket(new SystemMessage(SystemMessageId.YOU_VE_BEEN_AFFLICTED_BY_SHILLIEN_S_BREATH_LEVEL_S1).addInt(nextLv));
		}
		else
		{
			_player.sendPacket(SystemMessageId.SHILLIEN_S_BREATH_HAS_BEEN_PURIFIED);
		}
	}
	
	/**
	 * Applies the Shilen's Breath debuff to the actor.<br>
	 * This method checks if the provided {@code level} is greater than {@code 0}.<br>
	 * It retrieves the skill from {@code SkillData} and applies its effects.<br>
	 * A system message is sent to notify the player of the affliction.
	 * @param level The intensity level of the Shilen's Breath debuff.
	 */
	public void setLevel(int level)
	{
		if (level > 0)
		{
			final Skill skill = SkillData.getInstance().getSkill(CommonSkill.SHILENS_BREATH.getId(), level);
			skill.applyEffects(_player, _player);
			_player.sendPacket(new SystemMessage(SystemMessageId.YOU_VE_BEEN_AFFLICTED_BY_SHILLIEN_S_BREATH_LEVEL_S1).addInt(level));
		}
	}
}
