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
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the visual effect for setting a hair accessory on a character.<br>
 * This class manages how the {@code HairAccessorySet} skill modifies the player's appearance.
 * @author UnAfraid
 */
public class HairAccessorySet extends AbstractEffect
{
	/**
	 * Creates a new {@code HairAccessorySet} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the required parameters.
	 */
	public HairAccessorySet(StatSet params)
	{
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
	 * Toggles the hair accessory visibility for a player.<br>
	 * This method checks if both creatures are players and that the target is alive.<br>
	 * It updates the appearance state and sends a notification to the player.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the effect.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effector.isPlayer() || !effected.isPlayer() || effected.isAlikeDead())
		{
			return;
		}
		
		final Player player = effected.asPlayer();
		final boolean hairAccessoryEnabled = player.isHairAccessoryEnabled();
		player.setHairAccessoryEnabled(!hairAccessoryEnabled);
		player.broadcastUserInfo(UserInfoType.APPAREANCE);
		player.sendPacket(hairAccessoryEnabled ? SystemMessageId.HEAD_ACCESSORIES_ARE_VISIBLE_FROM_NOW_ON : SystemMessageId.HEAD_ACCESSORIES_ARE_NO_LONGER_SHOWN);
	}
}
