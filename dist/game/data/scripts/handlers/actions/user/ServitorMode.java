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
package handlers.actions.user;

import org.l2jmobius.gameserver.ai.SummonAI;
import org.l2jmobius.gameserver.data.holders.ActionDataHolder;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.handler.IActionUserHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Handles the player action for toggling a servitor into passive or defend mode.<br>
 * This class manages the logic required to change the behavior of a summoned entity.
 * @author Nik, Mobius
 */
public class ServitorMode implements IActionUserHandler
{
	/**
	 * Handles the action for switching servitor modes.<br>
	 * This method updates whether servitors are in passive or defending mode.<br>
	 * It checks if the {@code Player} owns any servitors before proceeding.
	 * @param player The {@code Player} who is performing the action.
	 * @param data The {@code ActionDataHolder} containing the selected option ID.
	 * @param ctrlPressed Indicates if the {@code Ctrl} key is being held down.
	 * @param shiftPressed Indicates if the {@code Shift} key is being held down.
	 */
	@Override
	public void onAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		if (!player.hasServitors())
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_SERVITOR);
			return;
		}
		
		switch (data.getOptionId())
		{
			case 1: // Passive mode
			{
				player.getServitors().values().forEach(s ->
				{
					if (s.isBetrayed())
					{
						player.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
						return;
					}
					
					((SummonAI) s.getAI()).setDefending(false);
				});
				break;
			}
			case 2: // Defending mode
			{
				player.getServitors().values().forEach(s ->
				{
					if (s.isBetrayed())
					{
						player.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
						return;
					}
					
					((SummonAI) s.getAI()).setDefending(true);
				});
				break;
			}
			case 3: // Siege weapon mode change
			{
				player.getServitors().values().forEach(s ->
				{
					if (s.getTemplate().getRace() == Race.SIEGE_WEAPON)
					{
						s.asServitor().switchSiegeMode();
					}
				});
				break;
			}
		}
	}
	
	/**
	 * Checks if the current action is related to a pet.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the action is a pet action.
	 */
	@Override
	public boolean isPetAction()
	{
		return true;
	}
}
