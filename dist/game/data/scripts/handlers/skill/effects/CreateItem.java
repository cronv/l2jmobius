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
 * This effect allows a {@code Player} to craft dwarven recipe items.<br>
 * It restricts the creation of items based on a specific level limit.
 * @author Nik
 */
public class CreateItem extends AbstractEffect
{
	private final int _recipeLevel;
	
	/**
	 * Creates a new instance of the {@code CreateItem} effect.<br>
	 * This constructor initializes the effect using provided parameters.<br>
	 * It extracts the recipe level from the input data.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public CreateItem(StatSet params)
	{
		_recipeLevel = params.getInt("value");
	}
	
	/**
	 * Checks if the effect can be started based on the target.<br>
	 * This method returns {@code true} only if the {@code effected} creature is a {@code Player}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
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
	 * It updates the creation level for the player who triggered the skill.<br>
	 * The {@code effected} creature must be a {@code Player}.
	 * @param effector The creature that performs the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} involved in the creation process.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Player player = effected.asPlayer();
		if (player != null)
		{
			player.setCreateItemLevel(_recipeLevel);
		}
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It resets the item creation level for the player.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature who received the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		final Player player = effected.asPlayer();
		if (player != null)
		{
			player.setCreateItemLevel(0);
		}
	}
}
