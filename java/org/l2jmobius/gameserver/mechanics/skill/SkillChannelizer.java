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
package org.l2jmobius.gameserver.mechanics.skill;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillLaunched;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * This class handles the logic for skills that require a continuous channeling state.<br>
 * It manages the execution and timing of these skills over a period of time.
 * @author UnAfraid
 */
public class SkillChannelizer implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(SkillChannelizer.class.getName());
	
	private final Creature _channelizer;
	private List<Creature> _channelized;
	
	private Skill _skill;
	private ScheduledFuture<?> _task = null;
	
	/**
	 * Creates a new {@code SkillChannelizer} instance.<br>
	 * This constructor initializes the object with a specific creature.<br>
	 * The provided creature will act as the primary source of the channeling effect.
	 * @param channelizer The {@code Creature} that performs the skill channeling.
	 */
	public SkillChannelizer(Creature channelizer)
	{
		_channelizer = channelizer;
	}
	
	/**
	 * Gets the creature that is currently performing the channeling action.
	 * @return The {@code Creature} instance acting as the channelizer.
	 */
	public Creature getChannelizer()
	{
		return _channelizer;
	}
	
	/**
	 * Retrieves the list of creatures currently affected by channeling.
	 * @return a {@code List} of {@code Creature} objects.
	 */
	public List<Creature> getChannelized()
	{
		return _channelized;
	}
	
	/**
	 * Checks if there are any creatures currently being channelized.<br>
	 * It returns {@code true} if the list of channelized targets is not {@code null}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the skill has active targets, {@code false} otherwise.
	 */
	public boolean hasChannelized()
	{
		return _channelized != null;
	}
	
	/**
	 * Starts the channeling process for a specific skill.<br>
	 * This method checks if the character is already channeling before starting.<br>
	 * It schedules the task using the tick settings from the {@code Skill} object.
	 * @param skill The {@code Skill} to be channeled.
	 */
	public void startChanneling(Skill skill)
	{
		// Verify for same status.
		if (isChanneling())
		{
			LOGGER.warning("Character: " + toString() + " is attempting to channel skill but he already does!");
			return;
		}
		
		// Start channeling.
		_skill = skill;
		final long tickInterval = skill.getChannelingTickInterval();
		if (tickInterval > 0)
		{
			_task = ThreadPool.scheduleAtFixedRate(this, skill.getChannelingTickInitialDelay(), tickInterval);
		}
		else
		{
			// A skill without a tick interval channels once, a period of zero would be rejected by the pool.
			_task = ThreadPool.schedule(this, skill.getChannelingTickInitialDelay());
		}
	}
	
	/**
	 * Stops the current channeling process.<br>
	 * This method cancels the active {@code ScheduledFuture} task.<br>
	 * It removes all affected creatures from the channelized list.<br>
	 * The {@code _skill} reference is set to {@code null}.
	 */
	public void stopChanneling()
	{
		// Verify for same status.
		if (!isChanneling())
		{
			LOGGER.warning("Character: " + toString() + " is attempting to stop channel skill but he does not!");
			return;
		}
		
		// Cancel the task and unset it.
		_task.cancel(false);
		_task = null;
		
		// Cancel target channelization and unset it.
		if (_channelized != null)
		{
			for (Creature creature : _channelized)
			{
				creature.getSkillChannelized().removeChannelizer(_skill.getChannelingSkillId(), _channelizer);
			}
			
			_channelized = null;
		}
		
		// unset skill.
		_skill = null;
	}
	
	/**
	 * Retrieves the {@code Skill} currently being channeled.<br>
	 * Returns {@code null} if no skill is active.
	 * @return The current {@code Skill} object.
	 */
	public Skill getSkill()
	{
		return _skill;
	}
	
	/**
	 * Checks if the creature is currently performing a channeling action.<br>
	 * It returns {@code true} if a task is active.<br>
	 * It returns {@code false} otherwise.
	 * @return {@code true} if channeling, {@code false} otherwise.
	 */
	public boolean isChanneling()
	{
		return _task != null;
	}
	
	@Override
	public void run()
	{
		if (!isChanneling())
		{
			return;
		}
		
		final Skill skill = _skill;
		List<Creature> channelized = _channelized;
		
		try
		{
			final int mpPerChanneling = skill.getMpPerChanneling();
			if (mpPerChanneling > 0)
			{
				// Validate mana per tick.
				if (_channelizer.getCurrentMp() < mpPerChanneling)
				{
					if (_channelizer.isPlayer())
					{
						_channelizer.sendPacket(SystemMessageId.YOUR_SKILL_WAS_DEACTIVATED_DUE_TO_LACK_OF_MP);
					}
					
					_channelizer.abortCast();
					return;
				}
				
				// Reduce mana per tick.
				_channelizer.reduceCurrentMp(mpPerChanneling);
			}
			
			// Apply channeling skills on the targets.
			final List<Creature> targetList = new ArrayList<>();
			final WorldObject target = skill.getTarget(_channelizer, false, false, false);
			if (target != null)
			{
				skill.forEachTargetAffected(_channelizer, target, o ->
				{
					if (o.isCreature())
					{
						targetList.add(o.asCreature());
						o.asCreature().getSkillChannelized().addChannelizer(skill.getChannelingSkillId(), _channelizer);
					}
				});
			}
			
			if (targetList.isEmpty())
			{
				return;
			}
			
			channelized = targetList;
			for (Creature creature : channelized)
			{
				if (!LocationUtil.checkIfInRange(skill.getEffectRange(), _channelizer, creature, true))
				{
					continue;
				}
				else if (!GeoEngine.getInstance().canSeeTarget(_channelizer, creature))
				{
					continue;
				}
				
				final int channelingSkillId = skill.getChannelingSkillId();
				if (channelingSkillId > 0)
				{
					final int maxSkillLevel = SkillData.getInstance().getMaxLevel(channelingSkillId);
					final int skillLevel = Math.min(creature.getSkillChannelized().getChannerlizersSize(channelingSkillId), maxSkillLevel);
					if (skillLevel == 0)
					{
						continue;
					}
					
					final BuffInfo info = creature.getEffectList().getBuffInfoBySkillId(channelingSkillId);
					if ((info == null) || (info.getSkill().getLevel() < skillLevel))
					{
						final Skill channeledSkill = SkillData.getInstance().getSkill(channelingSkillId, skillLevel);
						if (channeledSkill == null)
						{
							LOGGER.warning(getClass().getSimpleName() + ": Non existent channeling skill requested: " + skill);
							_channelizer.abortCast();
							return;
						}
						
						// Update PvP status
						if (creature.isPlayable() && _channelizer.isPlayer())
						{
							_channelizer.asPlayer().updatePvPStatus(creature);
						}
						
						// Be warned, this method has the possibility to call doDie->abortCast->stopChanneling method. Variable cache above try{} is used in this case to avoid NPEs.
						channeledSkill.applyEffects(_channelizer, creature);
					}
					
					if (!skill.isToggle())
					{
						_channelizer.broadcastSkillPacket(new MagicSkillLaunched(_channelizer, skill.getId(), skill.getLevel(), SkillCastingType.NORMAL, creature), creature);
					}
				}
				else
				{
					skill.applyChannelingEffects(_channelizer, creature);
				}
				
				// Reduce shots.
				final boolean useSpiritShot = skill.useSpiritShot();
				if (useSpiritShot)
				{
					_channelizer.unchargeShot(_channelizer.isChargedShot(ShotType.BLESSED_SPIRITSHOTS) ? ShotType.BLESSED_SPIRITSHOTS : ShotType.SPIRITSHOTS);
				}
				else
				{
					_channelizer.unchargeShot(_channelizer.isChargedShot(ShotType.BLESSED_SOULSHOTS) ? ShotType.BLESSED_SOULSHOTS : ShotType.SOULSHOTS);
				}
				
				// Shots are re-charged every cast.
				_channelizer.rechargeShots(skill.useSoulShot(), useSpiritShot, false);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error while channelizing skill: " + skill + " channelizer: " + _channelizer + " channelized: " + channelized + "; ", e);
		}
	}
}
