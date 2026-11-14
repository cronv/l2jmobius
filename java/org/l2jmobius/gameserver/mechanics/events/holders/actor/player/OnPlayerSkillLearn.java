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
package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.AcquireSkillType;

/**
 * This class represents an event triggered when a {@code Player} learns a new {@code Skill}.<br>
 * It holds the necessary data regarding the skill acquisition for processing by event listeners.
 * @author UnAfraid
 */
public class OnPlayerSkillLearn implements IBaseEvent
{
	private final Npc _trainer;
	private final Player _player;
	private final Skill _skill;
	private final AcquireSkillType _type;
	
	/**
	 * Creates a new event for when a {@code Player} learns a {@code Skill}.<br>
	 * This constructor initializes the required data for the learning process.
	 * @param trainer The {@code Npc} who taught the skill.
	 * @param player The {@code Player} who learned the skill.
	 * @param skill The specific {@code Skill} that was acquired.
	 * @param type The {@code AcquireSkillType} used to obtain the skill.
	 */
	public OnPlayerSkillLearn(Npc trainer, Player player, Skill skill, AcquireSkillType type)
	{
		_trainer = trainer;
		_player = player;
		_skill = skill;
		_type = type;
	}
	
	/**
	 * Retrieves the {@code Npc} that provided the skill.<br>
	 * This is the trainer associated with this event.
	 * @return the {@code Npc} trainer.
	 */
	public Npc getTrainer()
	{
		return _trainer;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who learned a skill.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the {@code Skill} associated with this event.<br>
	 * This method returns the skill that was learned by the player.
	 * @return the {@code Skill} object.
	 */
	public Skill getSkill()
	{
		return _skill;
	}
	
	/**
	 * Retrieves the method used to acquire the skill.<br>
	 * This returns the {@code AcquireSkillType} associated with this event.
	 * @return the {@code AcquireSkillType} of the learned skill.
	 */
	public AcquireSkillType getAcquireType()
	{
		return _type;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * It returns a constant value from {@code EventType}.
	 * @return The {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_SKILL_LEARN;
	}
}
