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
package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.ArenaParticipantsHolder;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.managers.games.BlockCheckerManager;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExCubeGameChangePoints;
import org.l2jmobius.gameserver.network.serverpackets.ExCubeGameExtendedChangePoints;
import org.l2jmobius.gameserver.network.serverpackets.NpcInfo;

/**
 * Represents a physical block entity within the game world.<br>
 * This class handles the behavior and properties of static obstacles or interactable blocks.<br>
 * It extends {@code Monster} to provide basic entity functionality.
 * @author BiggBoss
 */
public class Block extends Monster
{
	private int _colorEffect;
	
	/**
	 * Creates a new {@code Block} instance.<br>
	 * This constructor initializes the object using the provided template.
	 * @param template The {@code NpcTemplate} used to define the block properties.
	 */
	public Block(NpcTemplate template)
	{
		super(template);
	}
	
	/**
	 * Toggles the color effect of this block.<br>
	 * Updates the internal {@code _colorEffect} value and broadcasts the change to all players.<br>
	 * Awards points to the correct team based on the interaction.<br>
	 * Handles a random chance to drop specific items for the attacker.
	 * @param attacker The {@code Player} who interacted with the block.
	 * @param holder The {@code ArenaParticipantsHolder} containing the current event data.
	 * @param team The integer ID of the team to receive points.
	 */
	public void changeColor(Player attacker, ArenaParticipantsHolder holder, int team)
	{
		// Do not update color while sending old info.
		synchronized (this)
		{
			final BlockCheckerManager event = holder.getEvent();
			if (_colorEffect == 0x53)
			{
				// Change color
				_colorEffect = 0x00;
				
				// BroadCast to all known players.
				broadcastPacket(new NpcInfo(this));
				increaseTeamPointsAndSend(attacker, team, event);
			}
			else
			{
				// Change color
				_colorEffect = 0x53;
				
				// BroadCast to all known players.
				broadcastPacket(new NpcInfo(this));
				increaseTeamPointsAndSend(attacker, team, event);
			}
			
			// 30% chance to drop the event items
			final int random = Rnd.get(100);
			
			// Bond
			if ((random > 69) && (random <= 84))
			{
				dropItem(13787, event, attacker);
			}
			else if (random > 84)
			{
				dropItem(13788, event, attacker);
			}
		}
	}
	
	/**
	 * Sets the color effect of the {@code Block}.<br>
	 * This method updates the internal color based on the provided boolean.<br>
	 * It uses {@code 0x53} if {@code isRed} is {@code true}.<br>
	 * Otherwise, it sets the color to {@code 0x00}.
	 * @param isRed The value used to determine the color effect.
	 */
	public void setRed(boolean isRed)
	{
		_colorEffect = isRed ? 0x53 : 0x00;
	}
	
	/**
	 * Retrieves the current color effect of the block.<br>
	 * This value is used to determine the visual state of the entity.
	 * @return The integer value representing the current {@code _colorEffect}.
	 */
	@Override
	public int getColorEffect()
	{
		return _colorEffect;
	}
	
	/**
	 * Checks if the {@code Block} can be attacked by a specific creature.<br>
	 * Players can only attack if they are part of an active arena.<br>
	 * All other creatures can attack freely.
	 * @param attacker The {@code Creature} attempting to perform the attack.
	 * @return {@code true} if the attack is allowed, {@code false} otherwise.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if (attacker.isPlayer())
		{
			return attacker.isPlayer() && (attacker.asPlayer().getBlockCheckerArena() > -1);
		}
		
		return true;
	}
	
	/**
	 * Handles the logic for when this block is destroyed.<br>
	 * It currently returns {@code false}.
	 * @param killer The {@code Creature} that caused the death.
	 * @return Always returns {@code false}.
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		return false;
	}
	
	/**
	 * Handles the logic when a {@code Player} interacts with this block.<br>
	 * It checks if the player can target the block.<br>
	 * If valid, it sets this block as the player's last folk NPC.<br>
	 * The method also ensures the player targets this block and wakes up its AI.
	 * @param player The {@code Player} who performed the action.
	 * @param interact A boolean indicating if the player clicked to interact.
	 */
	@Override
	public void onAction(Player player, boolean interact)
	{
		if (!canTarget(player))
		{
			return;
		}
		
		player.setLastFolkNPC(this);
		
		if (player.getTarget() != this)
		{
			player.setTarget(this);
			getAI(); // wake up ai
		}
		else if (interact)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	/**
	 * Updates the score for a specific team.<br>
	 * Sends updated point information to all players in the game.
	 * @param player The {@code Player} who earned the points.
	 * @param team The ID of the team to update.
	 * @param eng The {@code BlockCheckerManager} handling the game state.
	 */
	private void increaseTeamPointsAndSend(Player player, int team, BlockCheckerManager eng)
	{
		eng.increasePlayerPoints(player, team);
		
		final int timeLeft = (int) ((eng.getStarterTime() - System.currentTimeMillis()) / 1000);
		final boolean isRed = eng.getHolder().getRedPlayers().contains(player);
		final ExCubeGameChangePoints changePoints = new ExCubeGameChangePoints(timeLeft, eng.getBluePoints(), eng.getRedPoints());
		final ExCubeGameExtendedChangePoints secretPoints = new ExCubeGameExtendedChangePoints(timeLeft, eng.getBluePoints(), eng.getRedPoints(), isRed, player, eng.getPlayerPoints(player, isRed));
		final ArenaParticipantsHolder holder = eng.getHolder();
		holder.broadCastPacketToTeam(changePoints);
		holder.broadCastPacketToTeam(secretPoints);
	}
	
	/**
	 * Creates and drops an item at a random location near this block.<br>
	 * The dropped item is then registered with the provided manager.
	 * @param id The unique identifier of the item to drop.
	 * @param eng The {@code BlockCheckerManager} used to track new drops.
	 * @param player The {@code Player} who triggered the drop event.
	 */
	private void dropItem(int id, BlockCheckerManager eng, Player player)
	{
		final Item drop = ItemManager.createItem(ItemProcessType.LOOT, id, 1, player, this);
		final int x = getX() + Rnd.get(50);
		final int y = getY() + Rnd.get(50);
		final int z = getZ();
		drop.dropMe(this, x, y, z);
		eng.addNewDrop(drop);
	}
}
