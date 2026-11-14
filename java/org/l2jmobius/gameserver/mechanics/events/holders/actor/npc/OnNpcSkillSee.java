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
package org.l2jmobius.gameserver.mechanics.events.holders.actor.npc;

import java.util.Collection;
import java.util.Collections;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class represents an event triggered when an {@code Npc} perceives a skill being cast.<br>
 * It holds the necessary data for processing interactions between NPCs and skills.
 * @author UnAfraid, Mobius
 */
public class OnNpcSkillSee implements IBaseEvent
{
	private final Npc _npc;
	private final Player _caster;
	private final Skill _skill;
	private final Collection<WorldObject> _targets;
	private final boolean _isSummon;
	
	/**
	 * This constructor initializes a new {@code OnNpcSkillSee} event.<br>
	 * It stores the data for an NPC using a skill on specific targets.
	 * @param npc The {@code Npc} that is performing the action.
	 * @param caster The {@code Player} who cast the skill.
	 * @param skill The {@code Skill} being used in this event.
	 * @param isSummon A boolean indicating if the skill is a summon.
	 * @param targets A {@code Collection} of {@code WorldObject} items affected by the skill.
	 */
	public OnNpcSkillSee(Npc npc, Player caster, Skill skill, boolean isSummon, Collection<WorldObject> targets)
	{
		_npc = npc;
		_caster = caster;
		_skill = skill;
		_isSummon = isSummon;
		_targets = targets;
	}
	
	/**
	 * Creates a new event for when an {@code Npc} sees a skill.<br>
	 * This constructor handles cases where there is a single {@code Creature} target.
	 * @param npc The {@code Npc} that sees the skill.
	 * @param caster The {@code Player} who casts the skill.
	 * @param skill The {@code Skill} being used.
	 * @param isSummon Whether the skill is a summon type.
	 * @param target The specific {@code Creature} targeted by the skill.
	 */
	public OnNpcSkillSee(Npc npc, Player caster, Skill skill, boolean isSummon, Creature target)
	{
		_npc = npc;
		_caster = caster;
		_skill = skill;
		_isSummon = isSummon;
		_targets = Collections.singleton(target);
	}
	
	/**
	 * Retrieves the {@code Npc} associated with this event.<br>
	 * This method returns the primary NPC involved in the skill action.
	 * @return The {@code Npc} object.
	 */
	public Npc getTarget()
	{
		return _npc;
	}
	
	/**
	 * Retrieves the player who cast the skill.<br>
	 * This method returns the {@code Player} object associated with this event.
	 * @return the {@code Player} who performed the action.
	 */
	public Player getCaster()
	{
		return _caster;
	}
	
	/**
	 * Retrieves the {@code Skill} associated with this event.
	 * @return the {@code Skill} object.
	 */
	public Skill getSkill()
	{
		return _skill;
	}
	
	/**
	 * Retrieves the list of targets for this event.<br>
	 * Returns all {@code WorldObject} instances affected by the skill.
	 * @return a {@code Collection} of {@code WorldObject} targets.
	 */
	public Collection<WorldObject> getTargets()
	{
		return _targets;
	}
	
	/**
	 * Checks if the skill used by the {@code Npc} is a summon.
	 * @return {@code true} if it is a summon, {@code false} otherwise.
	 */
	public boolean isSummon()
	{
		return _isSummon;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * It returns a constant value from {@code EventType}.
	 * @return the {@code EventType} associated with this event.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_NPC_SKILL_SEE;
	}
}
