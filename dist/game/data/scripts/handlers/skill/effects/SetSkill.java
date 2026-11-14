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

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for applying a skill effect that sets a specific skill.<br>
 * It allows for the modification of character abilities based on defined script data.
 * @author Zoey76
 */
public class SetSkill extends AbstractEffect
{
	private final int _skillId;
	private final int _skillLevel;
	
	/**
	 * Creates a new {@code SetSkill} effect.<br>
	 * This constructor initializes the skill ID and level from the provided data.
	 * @param params The {@code StatSet} containing the required configuration values.
	 */
	public SetSkill(StatSet params)
	{
		_skillId = params.getInt("skillId", 0);
		_skillLevel = params.getInt("skillLevel", 1);
	}
	
	/**
	 * Checks if the effect occurs instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies a skill to a player immediately.<br>
	 * This method checks if the target is a player before adding the skill.<br>
	 * It retrieves the specific {@code Skill} based on the internal ID and level.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used for the effect.
	 * @param item The item associated with this effect.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isPlayer())
		{
			return;
		}
		
		final Skill setSkill = SkillData.getInstance().getSkill(_skillId, _skillLevel);
		if (setSkill == null)
		{
			return;
		}
		
		effected.asPlayer().addSkill(setSkill, true);
	}
}
