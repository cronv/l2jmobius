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

import org.l2jmobius.gameserver.ai.Action;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.ai.NextAction;
import org.l2jmobius.gameserver.data.holders.ActionDataHolder;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.MountType;
import org.l2jmobius.gameserver.entity.actor.instance.StaticObject;
import org.l2jmobius.gameserver.handler.IActionUserHandler;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.network.serverpackets.ChairSit;

/**
 * Handles the logic for players performing sit and stand actions.<br>
 * This class manages state changes when a {@code Player} interacts with chairs or stands up.
 * @author UnAfraid
 */
public class SitStand implements IActionUserHandler
{
	/**
	 * Handles the logic for a player attempting to sit or stand.<br>
	 * It checks if the {@code Player} is currently moving or in a specific state.<br>
	 * If the player is moving, it schedules the action for when they arrive at their destination.
	 * @param player The {@code Player} who initiated the action.
	 * @param data The {@code ActionDataHolder} containing details about the action.
	 * @param ctrlPressed True if the {@code Ctrl} key is held down, false otherwise.
	 * @param shiftPressed True if the {@code Shift} key is held down, false otherwise.
	 */
	@Override
	public void onAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		if (player.isSitting() || !player.isMoving() || player.isFakeDeath())
		{
			useSit(player, player.getTarget());
		}
		else
		{
			// Sit when arrive using next action.
			// Creating next action class.
			final NextAction nextAction = new NextAction(Action.ARRIVED, Intention.MOVE_TO, () -> useSit(player, player.getTarget()));
			
			// Binding next action to AI.
			player.getAI().setNextAction(nextAction);
		}
	}
	
	/**
	 * Checks if the player can sit on a target or toggle their sitting state.<br>
	 * This method handles chair interactions and basic sit/stand logic.
	 * @param player The {@code Player} performing the action.
	 * @param target The {@code WorldObject} being interacted with.
	 * @return {@code true} if the action was successfully processed, {@code false} otherwise.
	 */
	private boolean useSit(Player player, WorldObject target)
	{
		if (player.getMountType() != MountType.NONE)
		{
			return false;
		}
		
		if (!player.isSitting() && (target instanceof StaticObject) && (((StaticObject) target).getType() == 1) && player.isInsideRadius2D(target, StaticObject.INTERACTION_DISTANCE) && !isThroneTaken(target))
		{
			final int throneId = target.getId();
			final ChairSit cs = new ChairSit(player, throneId);
			player.sendPacket(cs);
			player.sitDown();
			player.setThroneId(throneId);
			player.broadcastPacket(cs);
			return true;
		}
		
		if (player.isFakeDeath())
		{
			player.stopEffects(EffectFlag.FAKE_DEATH);
		}
		else if (player.isSitting())
		{
			player.standUp();
		}
		else
		{
			player.sitDown();
		}
		
		return true;
	}
	
	/**
	 * Checks whether a seated player already holds the throne; a later sitter then sits down beside it.
	 * @param throne The throne static object.
	 * @return {@code true} if a seated player holds the throne.
	 */
	private static boolean isThroneTaken(WorldObject throne)
	{
		final int throneId = throne.getId();
		for (Player sitter : World.getVisibleObjectsInRange(throne, Player.class, StaticObject.INTERACTION_DISTANCE))
		{
			if (sitter.isSitting() && (sitter.getThroneId() == throneId))
			{
				return true;
			}
		}
		
		return false;
	}
}
