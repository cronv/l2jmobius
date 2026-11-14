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
package handlers.skill.conditions;

import java.util.List;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a specific {@code Npc} exists in the world.<br>
 * This condition is used to verify the presence of an NPC before executing a {@code Skill}.
 * @author UnAfraid, Mobius
 */
public class OpExistNpcSkillCondition implements ISkillCondition
{
	private final List<Integer> _npcIds;
	private final int _range;
	private final boolean _isAround;
	
	/**
	 * Creates a new condition to check for the existence of specific NPCs.<br>
	 * This constructor initializes the required NPC IDs, range, and proximity settings.
	 * @param params The {@code StatSet} containing all configuration values.
	 */
	public OpExistNpcSkillCondition(StatSet params)
	{
		_npcIds = params.getList("npcIds", Integer.class);
		_range = params.getInt("range");
		_isAround = params.getBoolean("isAround");
	}
	
	/**
	 * Checks if the skill can be cast based on the presence of specific NPCs.<br>
	 * It verifies if any NPC from a predefined list is within a certain range.<br>
	 * The result depends on whether the condition requires an NPC to be nearby or not.
	 * @param caster The {@code Creature} attempting to use the skill.
	 * @param skill The {@code Skill} being cast.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if the condition is met, {@code false} otherwise.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if (World.getFirstVisibleObjectInRange(caster, Npc.class, _range, npc -> _npcIds.contains(npc.getId())) != null)
		{
			return _isAround;
		}
		
		return !_isAround;
	}
}
