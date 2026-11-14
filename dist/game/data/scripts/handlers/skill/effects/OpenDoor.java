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

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code OpenDoor} skill effect.<br>
 * It allows a player to interact with and open a {@code Door} object in the game world.
 * @author Adry_85
 */
public class OpenDoor extends AbstractEffect
{
	private final int _chance;
	private final boolean _isItem;
	
	/**
	 * Creates a new {@code OpenDoor} effect instance.<br>
	 * This constructor initializes the chance and item status from the provided parameters.
	 * @param params The {@code StatSet} containing configuration values.
	 */
	public OpenDoor(StatSet params)
	{
		_chance = params.getInt("chance", 0);
		_isItem = params.getBoolean("isItem", false);
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
	 * Executes the logic to attempt opening a door.<br>
	 * This method checks if the target is a valid door and handles the success or failure of the action.<br>
	 * It sends appropriate system messages based on the result.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} or object being targeted.
	 * @param skill The {@code Skill} used to attempt the action.
	 * @param item The {@code Item} involved in the interaction, if any.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isDoor() || (effector.getInstanceWorld() != effected.getInstanceWorld()))
		{
			return;
		}
		
		final Door door = effected.asDoor();
		if ((!door.isOpenableBySkill() && !_isItem) || (door.getFort() != null))
		{
			effector.sendPacket(SystemMessageId.THIS_DOOR_CANNOT_BE_UNLOCKED);
			return;
		}
		
		if ((Rnd.get(100) < _chance) && !door.isOpen())
		{
			door.openMe();
		}
		else
		{
			effector.sendPacket(SystemMessageId.YOU_HAVE_FAILED_TO_UNLOCK_THE_DOOR);
		}
	}
}
