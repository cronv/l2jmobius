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
package org.l2jmobius.gameserver.mechanics.events.holders.actor.creature;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This event is triggered instantly when a {@code Creature} finishes casting a {@code Skill}.<br>
 * It provides a hook to execute logic immediately after the skill completion.
 * @author Nik
 */
public class OnCreatureSkillFinishCast implements IBaseEvent
{
	private Creature _caster;
	private WorldObject _target;
	private Skill _skill;
	private boolean _simultaneously;
	
	/**
	 * Creates a new instance of the {@code OnCreatureSkillFinishCast} event.<br>
	 * This event is triggered when a creature finishes casting a skill.
	 */
	public OnCreatureSkillFinishCast()
	{
	}
	
	/**
	 * Retrieves the creature that cast the skill.<br>
	 * This method returns the {@code Creature} associated with this event.
	 * @return the {@code Creature} who performed the action
	 */
	public Creature getCaster()
	{
		return _caster;
	}
	
	/**
	 * Sets the {@code Creature} that performed the skill.<br>
	 * This method updates the internal {@code _caster} field.
	 * @param caster The {@code Creature} instance to set as the caster.
	 */
	public synchronized void setCaster(Creature caster)
	{
		_caster = caster;
	}
	
	/**
	 * Retrieves the target of this skill event.<br>
	 * Returns the {@code WorldObject} that was affected by the skill.
	 * @return the {@code WorldObject} target or {@code null} if no target exists.
	 */
	public WorldObject getTarget()
	{
		return _target;
	}
	
	/**
	 * Sets the {@code WorldObject} that is being targeted by this event.<br>
	 * This method updates the internal target reference.
	 * @param target The {@code WorldObject} to set as the target.
	 */
	public synchronized void setTarget(WorldObject target)
	{
		_target = target;
	}
	
	/**
	 * Retrieves the {@code Skill} associated with this event.<br>
	 * This method returns the skill used by the caster.
	 * @return the {@code Skill} object.
	 */
	public Skill getSkill()
	{
		return _skill;
	}
	
	/**
	 * Sets the {@code Skill} associated with this event.<br>
	 * This method updates the internal {@code _skill} field.
	 * @param skill The {@code Skill} to be assigned.
	 */
	public synchronized void setSkill(Skill skill)
	{
		_skill = skill;
	}
	
	/**
	 * Checks if the skill was executed simultaneously.<br>
	 * This indicates whether the event happens at the same time as other actions.
	 * @return {@code true} if the action is simultaneous, {@code false} otherwise.
	 */
	public boolean isSimultaneously()
	{
		return _simultaneously;
	}
	
	/**
	 * Sets whether the event occurs simultaneously.<br>
	 * This updates the {@code _simultaneously} field.
	 * @param simultaneously The value to set for simultaneous execution.
	 */
	public synchronized void setSimultaneously(boolean simultaneously)
	{
		_simultaneously = simultaneously;
	}
	
	/**
	 * Returns the type of this event.<br>
	 * This method identifies the specific {@code EventType} associated with the skill finish cast.
	 * @return The {@code EventType} for this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_CREATURE_SKILL_FINISH_CAST;
	}
}
