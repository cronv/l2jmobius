/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.bypass.npc;

import java.util.logging.Level;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.BroadcastingTower;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

/**
 * Handles the observation bypass logic for {@code Npc} entities.<br>
 * This class processes specific player interactions related to observation mechanics.
 */
public class Observation implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"observesiege",
		"observeoracle",
		"observe"
	};
	
	private static final int[][] LOCATIONS =
	{
		// @formatter:off
		// Gludio
		{-18347, 114000, -2360, 0},
		{-18347, 113255, -2447, 0},
		// Dion
		{22321, 155785, -2604, 0},
		{22321, 156492, -2627, 0},
		// Giran
		{112000, 144864, -2445, 0},
		{112657, 144864, -2525, 0},
		// Innadril
		{116260, 244600, -775, 0},
		{116260, 245264, -721, 0},
		// Oren
		{78100, 36950, -2242, 0},
		{78744, 36950, -2244, 0},
		// Aden
		{147457, 9601, -233, 0},
		{147457, 8720, -252, 0},
		// Goddard
		{147542, -43543, -1328, 0},
		{147465, -45259, -1328, 0},
		// Rune
		{20598, -49113, -300, 0},
		{18702, -49150, -600, 0},
		// Schuttgart
		{77541, -147447, 353, 0},
		{77541, -149245, 353, 0},
		// Coliseum
		{148416, 46724, -3000, 0},
		{149500, 46724, -3000, 0},
		{150511, 46724, -3000, 0},
		// Dusk
		{-77200, 88500, -4800, 0},
		{-75320, 87135, -4800, 0},
		{-76840, 85770, -4800, 0},
		{-76840, 85770, -4800, 0},
		{-79950, 85165, -4800, 0},
		// Dawn
		{-79185, 112725, -4300, 0},
		{-76175, 113330, -4300, 0},
		{-74305, 111965, -4300, 0},
		{-75915, 110600, -4300, 0},
		{-78930, 110005, -4300, 0}
		// @formatter:on
	};
	
	/**
	 * Processes the observation command for a specific creature.<br>
	 * Checks if the player is eligible to perform the action.<br>
	 * Validates the command parameters and executes the corresponding observation logic.
	 * @param command The full command string sent by the player.
	 * @param player The {@code Player} instance executing the command.
	 * @param target The {@code Creature} being targeted for observation.
	 * @return {@code true} if the command was successfully processed, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		if (!(target instanceof BroadcastingTower))
		{
			return false;
		}
		
		if (player.hasSummon())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THE_SPECTATOR_MODE_WITH_A_PET_OR_SERVITOR_SUMMONED);
			return false;
		}
		
		if (player.isRegisteredOnEvent())
		{
			player.sendMessage("Cannot use while registered on an event.");
			return false;
		}
		
		final String _command = command.split(" ")[0].toLowerCase();
		final int param;
		try
		{
			param = Integer.parseInt(command.split(" ")[1]);
		}
		catch (NumberFormatException nfe)
		{
			LOGGER.log(Level.WARNING, "Exception in " + getClass().getSimpleName(), nfe);
			return false;
		}
		
		if ((param < 0) || (param > (LOCATIONS.length - 1)))
		{
			return false;
		}
		
		final int[] locCost = LOCATIONS[param];
		final Location loc = new Location(locCost[0], locCost[1], locCost[2]);
		final long cost = locCost[3];
		
		switch (_command)
		{
			case "observesiege":
			{
				if (SiegeManager.getInstance().getSiege(loc) != null)
				{
					doObserve(player, target.asNpc(), loc, cost);
				}
				else
				{
					player.sendPacket(SystemMessageId.SPECTATOR_MODE_IS_ONLY_AVAILABLE_DURING_A_SIEGE);
				}
				
				return true;
			}
			case "observeoracle": // Oracle Dusk/Dawn
			{
				doObserve(player, target.asNpc(), loc, cost);
				return true;
			}
			case "observe": // Observe
			{
				doObserve(player, target.asNpc(), loc, cost);
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Handles the logic for a player entering observation mode.<br>
	 * It checks if the {@code Player} can afford the {@code cost}.<br>
	 * If successful, it sets the observer position and updates the UI.<br>
	 * A failure packet is sent regardless of the outcome.
	 * @param player The {@code Player} attempting to observe.
	 * @param npc The {@code Npc} providing the observation service.
	 * @param pos The {@code Location} where the player will be placed.
	 * @param cost The amount of currency required for the action.
	 */
	private void doObserve(Player player, Npc npc, Location pos, long cost)
	{
		if (player.reduceAdena(ItemProcessType.FEE, cost, npc, true))
		{
			// enter mode
			player.getObserverMode().enter(pos);
			player.sendItemList();
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Retrieves the list of valid commands for this handler.<br>
	 * These commands allow players to use observation features.
	 * @return an array of {@code String} containing all available commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
