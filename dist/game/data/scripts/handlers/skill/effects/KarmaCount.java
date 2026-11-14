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
 * This effect handles the modification of a creature's karma count.<br>
 * It is used to decrease or reset the {@code karma} value when an item is used.
 * @author Nik
 */
public class KarmaCount extends AbstractEffect
{
	private final int _amount;
	private final int _mode;
	
	/**
	 * Creates a new {@code KarmaCount} effect.<br>
	 * This constructor initializes the amount and mode from the provided parameters.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public KarmaCount(StatSet params)
	{
		_amount = params.getInt("amount", 0);
		switch (params.getString("mode", "DIFF"))
		{
			case "DIFF":
			{
				_mode = 0;
				break;
			}
			case "RESET":
			{
				_mode = 1;
				break;
			}
			default:
			{
				throw new IllegalArgumentException("Mode should be DIFF or RESET skill id:" + params.getInt("id"));
			}
		}
	}
	
	/**
	 * Checks if the effect happens immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies the karma count effect instantly to a target.<br>
	 * This method updates the reputation of the {@code effected} player.<br>
	 * It only executes if the target is a {@code Player} with negative reputation.
	 * @param effector The creature who performs the action.
	 * @param effected The creature who receives the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the effect.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Player player = effected.asPlayer();
		if (player == null)
		{
			return;
		}
		
		// Check if player has no karma.
		final int reputation = player.getReputation();
		if (reputation >= 0)
		{
			return;
		}
		
		switch (_mode)
		{
			case 0: // diff
			{
				final int newReputation = Math.min(reputation + _amount, 0);
				player.setReputation(newReputation);
				break;
			}
			case 1: // reset
			{
				player.setReputation(0);
				break;
			}
		}
	}
}
