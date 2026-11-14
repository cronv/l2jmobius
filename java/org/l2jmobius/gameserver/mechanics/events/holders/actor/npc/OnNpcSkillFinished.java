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
package org.l2jmobius.gameserver.mechanics.events.holders.actor.npc;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class represents an event triggered when an {@code Npc} completes a {@code Skill}.<br>
 * It holds the necessary data for systems to respond to the end of an NPC skill execution.
 * @author UnAfraid
 */
public class OnNpcSkillFinished implements IBaseEvent
{
	private final Npc _caster;
	private final Player _target;
	private final Skill _skill;
	
	/**
	 * This constructor initializes a new {@code OnNpcSkillFinished} event.<br>
	 * It stores the NPC that performed the action, the player who was hit, and the skill used.
	 * @param caster The {@code Npc} that cast the skill.
	 * @param target The {@code Player} who was affected by the skill.
	 * @param skill The {@code Skill} object representing the ability used.
	 */
	public OnNpcSkillFinished(Npc caster, Player target, Skill skill)
	{
		_caster = caster;
		_target = target;
		_skill = skill;
	}
	
	/**
	 * Retrieves the {@code Player} who was targeted by the skill.<br>
	 * This method returns the target associated with this event.
	 * @return the {@code Player} object that was targeted.
	 */
	public Player getTarget()
	{
		return _target;
	}
	
	/**
	 * Retrieves the {@code Npc} that cast the skill.<br>
	 * This method returns the caster associated with this event.
	 * @return the {@code Npc} who performed the action
	 */
	public Npc getCaster()
	{
		return _caster;
	}
	
	/**
	 * Retrieves the {@code Skill} object associated with this event.<br>
	 * This method returns the skill that was just completed by the NPC.
	 * @return the {@code Skill} used in this event
	 */
	public Skill getSkill()
	{
		return _skill;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with the skill completion.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_NPC_SKILL_FINISHED;
	}
}
