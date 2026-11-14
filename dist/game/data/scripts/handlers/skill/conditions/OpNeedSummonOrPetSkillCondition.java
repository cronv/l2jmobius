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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the player has a {@code Summon} or pet skill available.<br>
 * This condition ensures that the required summon is active before executing a skill.
 * @author UnAfraid, Mobius
 */
public class OpNeedSummonOrPetSkillCondition implements ISkillCondition
{
	private final Set<Integer> _npcIds = new HashSet<>();
	
	/**
	 * Creates a new condition that checks for specific summons or pets.<br>
	 * It initializes the required NPC IDs from the provided configuration.
	 * @param params The {@code StatSet} containing the list of allowed NPC IDs.
	 */
	public OpNeedSummonOrPetSkillCondition(StatSet params)
	{
		final List<Integer> npcIds = params.getList("npcIds", Integer.class);
		if (npcIds != null)
		{
			_npcIds.addAll(npcIds);
		}
	}
	
	/**
	 * Checks if the caster has a valid pet or summon.<br>
	 * The creature must have a {@code Summon} with an ID present in the allowed list.
	 * @param caster The {@code Creature} attempting to use the skill.
	 * @param skill The {@code Skill} being cast.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if a valid pet or summon is found, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		final Summon pet = caster.getPet();
		if ((pet != null) && _npcIds.contains(pet.getId()))
		{
			return true;
		}
		
		for (Summon summon : caster.getServitors().values())
		{
			if (_npcIds.contains(summon.getId()))
			{
				return true;
			}
		}
		
		return false;
	}
}
