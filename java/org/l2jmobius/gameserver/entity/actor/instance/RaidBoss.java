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

import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.player.MountType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;

/**
 * Manages the behavior of {@code RaidBoss} entities within the game world.<br>
 * This class represents the master entity in a group mob, which controls several slave entities called Minions.
 * @author Mobius
 */
public class RaidBoss extends Monster
{
	private boolean _useRaidCurse = true;
	private volatile long _lastAttackedTime;
	private ScheduledFuture<?> _resetTask;
	
	/**
	 * Creates a new {@code RaidBoss} instance.<br>
	 * This constructor initializes the boss with specific raid properties.<br>
	 * It sets the instance type to {@code InstanceType.RaidBoss}.<br>
	 * It marks the entity as a raid boss and makes it non-lethalable.
	 * @param template The {@code NpcTemplate} used to define the boss's base stats and properties.
	 */
	public RaidBoss(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.RaidBoss);
		setIsRaid(true);
		setLethalable(false);
	}
	
	/**
	 * This method is called when the {@code RaidBoss} instance is created.<br>
	 * It disables random walking for the boss.<br>
	 * It plays a spawn music sound to all nearby players.
	 */
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		setRandomWalking(false);
		broadcastPacket(new PlaySound(1, getParameters().getString("RaidSpawnMusic", "Rm01_A"), 0, 0, 0, 0, 0));
	}
	
	/**
	 * Calculates the vitality points for a character.<br>
	 * This method returns the negative value of the base vitality points.
	 * @param level The current level of the character.
	 * @param exp The experience points of the character.
	 * @param isBoss A boolean indicating if the character is a boss.
	 * @return The calculated vitality points as an {@code int}.
	 */
	@Override
	public int getVitalityPoints(int level, double exp, boolean isBoss)
	{
		return -super.getVitalityPoints(level, exp, isBoss);
	}
	
	/**
	 * Checks if the vitality rate should be used for raid bosses.<br>
	 * This value is retrieved from the {@code PlayerConfig} class.
	 * @return {@code true} if vitality rate is enabled, {@code false} otherwise.
	 */
	@Override
	public boolean useVitalityRate()
	{
		return PlayerConfig.RAIDBOSS_USE_VITALITY;
	}
	
	/**
	 * Sets whether the {@code RaidBoss} should apply a raid curse.<br>
	 * This determines if the boss uses its special curse ability during combat.
	 * @param value The boolean flag to enable or disable the raid curse.
	 */
	public void setUseRaidCurse(boolean value)
	{
		_useRaidCurse = value;
	}
	
	/**
	 * Checks if the raid curse is currently enabled.<br>
	 * This method returns the state of the {@code _useRaidCurse} flag.
	 * @return {@code true} if the curse is active, {@code false} otherwise.
	 */
	@Override
	public boolean giveRaidCurse()
	{
		return _useRaidCurse;
	}
	
	/**
	 * Records the attack time and starts the reset check if raid boss reset is enabled and the check is not running.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param damage The amount of damage to apply.
	 * @param skill The {@code Skill} used for the attack, or {@code null} if no skill was used.
	 */
	@Override
	public void addDamage(Creature attacker, int damage, Skill skill)
	{
		super.addDamage(attacker, damage, skill);
		_lastAttackedTime = System.currentTimeMillis();
		if ((NpcConfig.RAID_BOSS_RESET_TIME > 0) && (_resetTask == null))
		{
			_resetTask = ThreadPool.schedule(this::checkReset, NpcConfig.RAID_BOSS_RESET_TIME);
		}
		
		// A strider rider gets Hinder Strider when not already under it.
		if ((attacker != null) && attacker.isPlayer() && (attacker.asPlayer().getMountType() == MountType.STRIDER) && !isDead() && !isCastingNow() && !attacker.isAffectedBySkill(CommonSkill.HINDER_STRIDER.getId()))
		{
			final Skill hinderStrider = CommonSkill.HINDER_STRIDER.getSkill();
			if (hinderStrider != null)
			{
				setTarget(attacker);
				doCast(hinderStrider);
			}
		}
	}
	
	/**
	 * Resets the boss once it has not been attacked for {@code NpcConfig.RAID_BOSS_RESET_TIME}: full HP/MP, aggro cleared and back to its spawn.<br>
	 * While attacks keep coming the check is scheduled again for the remaining time.
	 */
	private void checkReset()
	{
		if (isDead() || !isSpawned())
		{
			_resetTask = null;
			return;
		}
		
		final long idleTime = System.currentTimeMillis() - _lastAttackedTime;
		if (idleTime < NpcConfig.RAID_BOSS_RESET_TIME)
		{
			_resetTask = ThreadPool.schedule(this::checkReset, NpcConfig.RAID_BOSS_RESET_TIME - idleTime);
			return;
		}
		
		_resetTask = null;
		setCurrentHpMp(getMaxHp(), getMaxMp());
		returnHome();
	}
}
