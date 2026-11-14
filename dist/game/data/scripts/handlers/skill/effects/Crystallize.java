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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemGrade;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This effect allows a {@code Player} to crystallize items.<br>
 * It restricts the crystallization process to a specific maximum {@code ItemGrade}.
 * @author Nik
 */
public class Crystallize extends AbstractEffect
{
	private final ItemGrade _grade;
	
	/**
	 * Initializes a new {@code Crystallize} effect.<br>
	 * This constructor sets the required item grade from the provided parameters.
	 * @param params The {@code StatSet} containing the configuration data for this effect.
	 */
	public Crystallize(StatSet params)
	{
		_grade = params.getEnum("grade", ItemGrade.class);
	}
	
	/**
	 * Checks if the effect can be applied to a target.<br>
	 * This method returns {@code true} only if the {@code effected} creature is a {@code Player}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used to trigger the effect.
	 * @return {@code true} if the target is a player, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return effected.isPlayer();
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It sets the crystallization grade for the player who was affected.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature that received the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} being processed by the effect.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Player player = effected.asPlayer();
		if (player != null)
		{
			player.setCrystallizeGrade(_grade);
		}
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It resets the crystallization grade for the player.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature who received the effect.
	 * @param skill The skill used to trigger this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		final Player player = effected.asPlayer();
		if (player != null)
		{
			player.setCrystallizeGrade(null);
		}
	}
}
