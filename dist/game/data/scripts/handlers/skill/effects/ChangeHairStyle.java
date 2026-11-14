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
 * This class handles the logic for changing a character's hair style.<br>
 * It is an implementation of {@code AbstractEffect} used by skills to modify player appearance.
 * @author Zoey76
 */
public class ChangeHairStyle extends AbstractEffect
{
	private final int _value;
	
	/**
	 * This constructor initializes a new {@code ChangeHairStyle} effect.<br>
	 * It extracts the required value from the provided {@code StatSet}.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public ChangeHairStyle(StatSet params)
	{
		_value = params.getInt("value", 0);
	}
	
	/**
	 * Checks if the effect happens immediately.<br>
	 * This method returns {@code true} for this specific effect.
	 * @return {@code true} if the effect is instant, otherwise {@code false}.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies the hair style change effect immediately.<br>
	 * This method updates the appearance of a {@code Player}.<br>
	 * It broadcasts the new information to other players.
	 * @param effector The creature who performs the action.
	 * @param effected The creature who receives the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the effect.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isPlayer())
		{
			return;
		}
		
		final Player player = effected.asPlayer();
		player.getAppearance().setHairStyle(_value);
		player.broadcastUserInfo();
	}
}
