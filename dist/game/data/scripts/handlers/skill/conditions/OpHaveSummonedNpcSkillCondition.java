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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the player has currently summoned an {@code Npc}.<br>
 * This condition is used to verify active summons before executing a skill.
 * @author Mobius, Liamxroy
 */
public class OpHaveSummonedNpcSkillCondition implements ISkillCondition
{
	private final Set<Integer> _npcIds;
	
	/**
	 * Creates a new condition to check if an NPC has been summoned.<br>
	 * This constructor extracts the required ID from the provided parameters.
	 * @param params The {@code StatSet} containing the {@code npcId}.
	 */
	public OpHaveSummonedNpcSkillCondition(StatSet params)
	{
		final String npcIdString = params.getString("npcId");
		_npcIds = Arrays.stream(npcIdString.split(",")).map(Integer::parseInt).collect(Collectors.toSet());
	}
	
	/**
	 * Checks if the caster has a specific summoned {@code Npc}.<br>
	 * This method verifies if any of the caster's active summons match the required ID.
	 * @param caster The {@code Creature} attempting to use the skill.
	 * @param skill The {@code Skill} being cast.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if a matching summoned {@code Npc} exists, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		for (Npc npc : caster.getSummonedNpcs())
		{
			if (_npcIds.contains(npc.getId()))
			{
				return true;
			}
		}
		
		return false;
	}
}
