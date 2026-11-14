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
package handlers.skill.effects;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code Resist} skill effect.<br>
 * It manages how characters resist specific types of damage or status effects.
 * @author UnAfraid
 */
public class ResistSkill extends AbstractEffect
{
	private final List<SkillHolder> _skills = new ArrayList<>();
	
	/**
	 * Creates a new {@code ResistSkill} instance.<br>
	 * This constructor parses skill IDs and levels from the provided data.<br>
	 * It populates the internal list of skills based on the {@code StatSet}.
	 * @param params The {@code StatSet} containing the required skill configuration.
	 */
	public ResistSkill(StatSet params)
	{
		for (int i = 1;; i++)
		{
			final int skillId = params.getInt("skillId" + i, 0);
			if (skillId == 0)
			{
				break;
			}
			
			final int skillLevel = params.getInt("skillLevel" + i, 0);
			_skills.add(new SkillHolder(skillId, skillLevel));
		}
		
		if (_skills.isEmpty())
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + ": Without parameters!");
		}
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It applies skill immunities to the target creature.<br>
	 * The {@code effected} creature will ignore specific skills based on the internal list.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill that caused this effect.
	 * @param item The item used to trigger the effect, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		for (SkillHolder holder : _skills)
		{
			effected.addIgnoreSkillEffects(holder);
		}
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes all skill ignores from the {@code effected} creature.<br>
	 * The removal is based on the list of skills held by this effect.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature who was affected by the skill.
	 * @param skill The skill that triggered this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		for (SkillHolder holder : _skills)
		{
			effected.removeIgnoreSkillEffects(holder);
		}
	}
}
