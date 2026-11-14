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
package ai.areas.Gracia.vehicles.AirShipGludioGracia;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.AirShip;
import org.l2jmobius.gameserver.managers.AirShipManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Handles the controller npc dialogs and boarding for the scheduled Gludio - Gracia airship.<br>
 * The airship itself is spawned and moved by the {@code AirShipManager}.
 * @author Mobius
 */
public class AirShipGludioGracia extends Script
{
	private static final int[] CONTROLLERS =
	{
		32607,
		32609
	};
	
	private static final int AIRSHIP_ID = 0;
	
	/**
	 * Initializes the {@code AirShipGludioGracia} script.<br>
	 * This constructor registers the controller npcs for talk and boarding events.
	 */
	public AirShipGludioGracia()
	{
		addStartNpc(CONTROLLERS);
		addFirstTalkId(CONTROLLERS);
		addTalkId(CONTROLLERS);
	}
	
	/**
	 * Handles events for the airship script.<br>
	 * This method checks if a {@code Player} meets all requirements to board the ship.<br>
	 * It validates states like combat, transformation, and movement before adding the passenger.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} entity involved in the event.
	 * @param player The {@code Player} attempting to interact with the ship.
	 * @return A {@code null} value after processing the logic.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (player.isTransformed())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_TRANSFORMED);
			return null;
		}
		else if (player.hasBlockActions() && player.hasAbnormalType(AbnormalType.PARALYZE))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_PETRIFIED);
			return null;
		}
		else if (player.isDead() || player.isFakeDeath())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_DEAD);
			return null;
		}
		else if (player.isFishing())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_FISHING);
			return null;
		}
		else if (player.isInCombat())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_IN_BATTLE);
			return null;
		}
		else if (player.isInDuel())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_IN_A_DUEL);
			return null;
		}
		else if (player.isSitting())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_SITTING);
			return null;
		}
		else if (player.isCastingNow())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_CASTING);
			return null;
		}
		else if (player.isCursedWeaponEquipped())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHEN_A_CURSED_WEAPON_IS_EQUIPPED);
			return null;
		}
		else if (player.isCombatFlagEquipped())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_HOLDING_A_FLAG);
			return null;
		}
		else if (player.hasSummon() || player.isMounted())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_BOARD_AN_AIRSHIP_WHILE_A_PET_OR_A_SERVITOR_IS_SUMMONED);
			return null;
		}
		
		final AirShip ship = AirShipManager.getInstance().getAirShip(AIRSHIP_ID);
		if ((ship != null) && ship.isInDock() && (World.getFirstVisibleObject(player, AirShip.class, it -> it == ship) != null))
		{
			ship.addPassenger(player);
		}
		
		return null;
	}
	
	/**
	 * This method determines the dialogue file for a first interaction.<br>
	 * It returns the filename based on the {@code Npc} ID.
	 * @param npc The {@code Npc} entity that is being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".htm";
	}
}
