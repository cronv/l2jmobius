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

import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.MountType;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for feeding a {@code Pet} or {@code MountType}.<br>
 * This effect manages the consumption of an {@code Item} to restore resources.<br>
 * It ensures that only valid entities can be fed by checking if the target is not {@code null}.
 * @author Sdw
 */
public class Feed extends AbstractEffect
{
	private final int _normal;
	private final int _ride;
	private final int _wyvern;
	
	/**
	 * Creates a new {@code Feed} effect using the provided statistics.<br>
	 * This constructor initializes the values for normal, ride, and wyvern types.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public Feed(StatSet params)
	{
		_normal = params.getInt("normal", 0);
		_ride = params.getInt("ride", 0);
		_wyvern = params.getInt("wyvern", 0);
	}
	
	/**
	 * Checks if the effect occurs immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies the instant effect of a skill to a target creature.<br>
	 * This method updates the feed level for pets or mounts based on the skill used.
	 * @param effector The {@code Creature} who performs the action.
	 * @param effected The {@code Creature} who receives the effect.
	 * @param skill The {@code Skill} being executed.
	 * @param item The {@code Item} used to trigger the effect.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isPet())
		{
			final Pet pet = effected.asPet();
			pet.setCurrentFed(pet.getCurrentFed() + (_normal * RatesConfig.PET_FOOD_RATE));
		}
		else if (effected.isPlayer())
		{
			final Player player = effected.asPlayer();
			if (player.getMountType() == MountType.WYVERN)
			{
				player.setCurrentFeed(player.getCurrentFeed() + _wyvern);
			}
			else
			{
				player.setCurrentFeed(player.getCurrentFeed() + _ride);
			}
		}
	}
}
