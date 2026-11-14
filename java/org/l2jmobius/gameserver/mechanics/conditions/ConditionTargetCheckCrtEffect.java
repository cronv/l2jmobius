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
package org.l2jmobius.gameserver.mechanics.conditions;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class checks if a target is affected by a critical hit effect.<br>
 * It evaluates whether the {@code Creature} meets specific criteria for critical hits.
 * @author Sdw
 */
public class ConditionTargetCheckCrtEffect extends Condition
{
	private final boolean _isCrtEffect;
	
	/**
	 * Creates a new instance of {@code ConditionTargetCheckCrtEffect}.<br>
	 * This constructor sets whether the condition applies to a critical effect.
	 * @param isCrtEffect The boolean value indicating if this is a critical effect.
	 */
	public ConditionTargetCheckCrtEffect(boolean isCrtEffect)
	{
		_isCrtEffect = isCrtEffect;
	}
	
	/**
	 * Checks if the target can be affected by a critical hit.<br>
	 * This logic applies specifically to {@code Creature} objects that are NPCs.<br>
	 * It compares the NPC's template properties against the internal state of this condition.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 * @return {@code true} if the condition is met, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if (effected.isNpc())
		{
			return effected.asNpc().getTemplate().canBeCrt() == _isCrtEffect;
		}
		
		return true;
	}
}
