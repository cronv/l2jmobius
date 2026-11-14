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
package handlers.actions.user;

import org.l2jmobius.gameserver.data.holders.ActionDataHolder;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IActionUserHandler;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

/**
 * Handles the logic for when a {@code Player} uses a tactical sign.<br>
 * This class processes the specific actions associated with setting up these signs.
 * @author Nik
 */
public class TacticalSignUse implements IActionUserHandler
{
	/**
	 * Handles the logic for using a tactical sign.<br>
	 * This method checks if the {@code Player} is in a party and has a valid creature target.<br>
	 * If requirements are met, it adds the sign to the party's data.
	 * @param player The {@code Player} performing the action.
	 * @param data The {@code ActionDataHolder} containing action details.
	 * @param ctrlPressed Whether the {@code Ctrl} key is held down.
	 * @param shiftPressed Whether the {@code Shift} key is held down.
	 */
	@Override
	public void onAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		if ((!player.isInParty() || (player.getTarget() == null) || !player.getTarget().isCreature()))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		player.getParty().addTacticalSign(player, data.getOptionId(), player.getTarget().asCreature());
	}
}
