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
package org.l2jmobius.gameserver.mechanics.skill;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;

/**
 * This class stores information regarding a skill cast triggered by an external source.<br>
 * It captures the context of the {@code Creature} that initiated the action and the target {@code WorldObject}.
 * @author Mobius
 */
public class TriggerCastInfo
{
	private final Creature _creature;
	private final WorldObject _target;
	private final Skill _skill;
	private final Item _item;
	private final boolean _ignoreTargetType;
	
	/**
	 * Creates a new {@code TriggerCastInfo} object.<br>
	 * This constructor stores the details of a skill cast.<br>
	 * It captures the caster, target, skill, and item used.
	 * @param creature The {@code Creature} performing the action.
	 * @param target The {@code WorldObject} being targeted.
	 * @param skill The {@code Skill} being executed.
	 * @param item The {@code Item} involved in the cast.
	 * @param ignoreTargetType Whether to ignore the target type, set to {@code true} or {@code false}.
	 */
	public TriggerCastInfo(Creature creature, WorldObject target, Skill skill, Item item, boolean ignoreTargetType)
	{
		_creature = creature;
		_target = target;
		_skill = skill;
		_item = item;
		_ignoreTargetType = ignoreTargetType;
	}
	
	/**
	 * Retrieves the {@code Creature} associated with this trigger.<br>
	 * This method returns the entity that initiated the cast.
	 * @return the {@code Creature} object.
	 */
	public Creature getCreature()
	{
		return _creature;
	}
	
	/**
	 * Retrieves the target of the skill cast.<br>
	 * This method returns the {@code WorldObject} associated with this trigger.
	 * @return the {@code WorldObject} being targeted.
	 */
	public WorldObject getTarget()
	{
		return _target;
	}
	
	/**
	 * Retrieves the {@code Skill} associated with this trigger.
	 * @return the {@code Skill} object.
	 */
	public Skill getSkill()
	{
		return _skill;
	}
	
	/**
	 * Retrieves the {@code Item} associated with this trigger.<br>
	 * Returns the item used for the skill cast.
	 * @return the {@code Item} object.
	 */
	public Item getItem()
	{
		return _item;
	}
	
	/**
	 * Checks if the target type should be ignored.<br>
	 * This determines whether specific restrictions on target types apply.
	 * @return {@code true} if the target type is ignored, {@code false} otherwise.
	 */
	public boolean isIgnoreTargetType()
	{
		return _ignoreTargetType;
	}
}
