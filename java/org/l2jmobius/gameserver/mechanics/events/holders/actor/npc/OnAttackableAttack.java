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

import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class represents an instantly executed event that occurs when an {@code Attackable} entity is attacked by a {@code Player}.<br>
 * It serves as a data holder for processing logic related to this specific interaction.
 * @author UnAfraid
 */
public class OnAttackableAttack implements IBaseEvent
{
	private final Player _attacker;
	private final Attackable _target;
	private final int _damage;
	private final Skill _skill;
	private final boolean _isSummon;
	
	/**
	 * Creates a new event for an attack on an {@code Attackable} entity.<br>
	 * This constructor initializes all necessary data for the attack action.
	 * @param attacker The {@code Player} who performs the attack.
	 * @param target The {@code Attackable} entity being hit.
	 * @param damage The amount of damage dealt during this attack.
	 * @param skill The {@code Skill} used to perform the attack.
	 * @param isSummon Whether the attack was performed by a summon.
	 */
	public OnAttackableAttack(Player attacker, Attackable target, int damage, Skill skill, boolean isSummon)
	{
		_attacker = attacker;
		_target = target;
		_damage = damage;
		_skill = skill;
		_isSummon = isSummon;
	}
	
	/**
	 * Retrieves the {@code Player} who initiated the attack.<br>
	 * This method returns the attacker associated with this event.
	 * @return the {@code Player} who performed the attack
	 */
	public Player getAttacker()
	{
		return _attacker;
	}
	
	/**
	 * Retrieves the {@code Attackable} entity that was attacked.
	 * @return the {@code Attackable} target of this event.
	 */
	public Attackable getTarget()
	{
		return _target;
	}
	
	/**
	 * Retrieves the amount of damage dealt during this attack.<br>
	 * This value represents the raw damage integer.
	 * @return The total damage as an {@code int}.
	 */
	public int getDamage()
	{
		return _damage;
	}
	
	/**
	 * Retrieves the {@code Skill} used during this attack.<br>
	 * This method returns the skill object associated with the event.
	 * @return the {@code Skill} instance.
	 */
	public Skill getSkill()
	{
		return _skill;
	}
	
	/**
	 * Checks if the attack was performed by a summon.<br>
	 * This helps identify if the damage source is a summoned entity.
	 * @return {@code true} if the attack is from a summon, {@code false} otherwise.
	 */
	public boolean isSummon()
	{
		return _isSummon;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * It returns a constant value from {@code EventType}.
	 * @return The {@code EventType} associated with this attack.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_ATTACKABLE_ATTACK;
	}
}
