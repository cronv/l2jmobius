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
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This effect allows a {@code Player} to craft common recipe items.<br>
 * It restricts the creation of these items based on a specific level limit.
 * @author Nik
 */
public class CreateCommonItem extends AbstractEffect
{
	private final int _recipeLevel;
	
	/**
	 * Creates a new instance of the {@code CreateCommonItem} effect.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It extracts the recipe level from the provided parameters.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public CreateCommonItem(StatSet params)
	{
		_recipeLevel = params.getInt("value");
	}
	
	/**
	 * Checks if the effect can be triggered.<br>
	 * This method verifies if the target is a {@code Player}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @return {@code true} if the target is a player, {@code false} otherwise.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return effected.isPlayer();
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It updates the {@code _recipeLevel} for the player who triggered the skill.
	 * @param effector The creature that performs the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} involved in the creation.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Player player = effected.asPlayer();
		if (player != null)
		{
			player.setCreateCommonItemLevel(_recipeLevel);
		}
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It resets the common item creation level for the player.<br>
	 * The {@code effected} creature is checked to see if it is a {@code Player}.<br>
	 * If it is a player, their level is set to {@code 0}.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature who received the effect.
	 * @param skill The {@code Skill} used to trigger the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		final Player player = effected.asPlayer();
		if (player != null)
		{
			player.setCreateCommonItemLevel(0);
		}
	}
}
