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

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.Action;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.ai.NextAction;
import org.l2jmobius.gameserver.data.holders.ActionDataHolder;
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IActionUserHandler;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerSocialAction;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExAskCoupleAction;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;

/**
 * This class handles social actions performed by players.<br>
 * It manages interactions such as requesting to be a couple or other social behaviors.
 * @author Nik, Mobius
 */
public class SocialAction implements IActionUserHandler
{
	/**
	 * Handles the execution of social actions for a {@code Player}.<br>
	 * This method determines which social animation or interaction to trigger.<br>
	 * It checks the option ID provided in the {@code ActionDataHolder}.
	 * @param player The {@code Player} performing the action.
	 * @param data The {@code ActionDataHolder} containing the specific action details.
	 * @param ctrlPressed Indicates if the {@code Ctrl} key is currently held down.
	 * @param shiftPressed Indicates if the {@code Shift} key is currently held down.
	 */
	@Override
	public void onAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		switch (data.getOptionId())
		{
			case 2: // Greeting
			case 3: // Victory
			case 4: // Advance
			case 5: // No
			case 6: // Yes
			case 7: // Bow
			case 8: // Unaware
			case 9: // Social Waiting
			case 10: // Laugh
			case 11: // Applaud
			case 12: // Dance
			case 13: // Sorrow
			case 14: // Charm
			case 15: // Shyness
			case 28: // Propose
			case 29: // Provoke
			{
				useSocial(player, data.getOptionId());
				break;
			}
			case 30: // Beauty Shop
			{
				if (useSocial(player, data.getOptionId()))
				{
					player.broadcastInfo();
				}
				break;
			}
			case 16: // Exchange Bows
			case 17: // High Five
			case 18: // Couple Dance
			{
				useCoupleSocial(player, data.getOptionId());
				break;
			}
		}
	}
	
	/**
	 * Checks if a {@code Player} can perform a social action.<br>
	 * It prevents the action if the player is currently fishing.<br>
	 * If allowed, it broadcasts the action and notifies any active listeners.
	 * @param player The {@code Player} attempting to use the social action.
	 * @param id The unique identifier for the specific social action.
	 * @return {@code true} if the action was processed successfully, or {@code false} if blocked by fishing.
	 */
	private boolean useSocial(Player player, int id)
	{
		if (player.isFishing())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_FISHING_3);
			return false;
		}
		
		// Social actions are impossible while riding.
		if (player.isMounted())
		{
			return false;
		}
		
		if (player.canMakeSocialAction())
		{
			player.broadcastPacket(new org.l2jmobius.gameserver.network.serverpackets.SocialAction(player.getObjectId(), id));
			
			// Notify to scripts
			if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_SOCIAL_ACTION, player))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerSocialAction(player, id), player);
			}
		}
		
		return true;
	}
	
	/**
	 * Sends a denial message to the player.<br>
	 * Responds to the current transaction for the {@code Player}.
	 * @param player The {@code Player} who was denied the action.
	 */
	private void scheduleDeny(Player player)
	{
		if (player != null)
		{
			player.sendPacket(SystemMessageId.THE_COUPLE_ACTION_REQUEST_HAS_BEEN_DENIED);
			player.onTransactionResponse();
		}
	}
	
	/**
	 * Handles the logic for initiating a couple social action with another player.<br>
	 * It validates distances, combat states, and various activity restrictions for both players.<br>
	 * If all conditions are met, it links the players and sends a request packet to the partner.
	 * @param player The {@code Player} who is initiating the action.
	 * @param id The unique identifier for the social action.
	 */
	private void useCoupleSocial(Player player, int id)
	{
		if (player == null)
		{
			return;
		}
		
		final WorldObject target = player.getTarget();
		if (target == null)
		{
			player.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		if (FakePlayerData.getInstance().isTalkable(target.getName()))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_REQUESTED_A_COUPLE_ACTION_WITH_C1);
			sm.addString(target.getName());
			player.sendPacket(sm);
			if (!player.isProcessingRequest())
			{
				ThreadPool.schedule(() -> scheduleDeny(player), 10000);
				player.blockRequest();
			}
			return;
		}
		
		if (!target.isPlayer())
		{
			player.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		final int distance = (int) player.calculateDistance2D(target);
		if ((distance > 125) || (distance < 15) || (player.getObjectId() == target.getObjectId()))
		{
			player.sendPacket(SystemMessageId.THE_REQUEST_CANNOT_BE_COMPLETED_BECAUSE_THE_TARGET_DOES_NOT_MEET_LOCATION_REQUIREMENTS);
			return;
		}
		
		SystemMessage sm;
		if (player.isInStoreMode() || player.isCrafting())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_IN_PRIVATE_STORE_MODE_OR_IN_A_BATTLE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(player);
			player.sendPacket(sm);
			return;
		}
		
		if (player.isInCombat() || player.isInDuel() || AttackStanceTaskManager.getInstance().hasAttackStanceTask(player))
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_IN_A_BATTLE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(player);
			player.sendPacket(sm);
			return;
		}
		
		if (player.isFishing())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_FISHING_3);
			return;
		}
		
		if (player.getReputation() < 0)
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_IN_A_CHAOTIC_STATE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(player);
			player.sendPacket(sm);
			return;
		}
		
		if (player.isInOlympiadMode())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_PARTICIPATING_IN_THE_OLYMPIAD_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(player);
			player.sendPacket(sm);
			return;
		}
		
		if (player.isInSiege())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_IN_A_CASTLE_SIEGE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(player);
			player.sendPacket(sm);
			return;
		}
		
		if (player.isInHideoutSiege())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_PARTICIPATING_IN_A_CLAN_HALL_SIEGE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(player);
			player.sendPacket(sm);
		}
		
		if (player.isMounted() || player.isFlyingMounted() || player.isInBoat() || player.isInAirShip())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_RIDING_A_SHIP_STEED_OR_STRIDER_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(player);
			player.sendPacket(sm);
			return;
		}
		
		if (player.isTransformed())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_CURRENTLY_TRANSFORMING_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(player);
			player.sendPacket(sm);
			return;
		}
		
		if (player.isAlikeDead())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_CURRENTLY_DEAD_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(player);
			player.sendPacket(sm);
			return;
		}
		
		// Checks for partner.
		final Player partner = target.asPlayer();
		if (partner.isInStoreMode() || partner.isCrafting())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_IN_PRIVATE_STORE_MODE_OR_IN_A_BATTLE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			player.sendPacket(sm);
			return;
		}
		
		if (partner.isInCombat() || partner.isInDuel() || AttackStanceTaskManager.getInstance().hasAttackStanceTask(partner))
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_IN_A_BATTLE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			player.sendPacket(sm);
			return;
		}
		
		if (partner.getMultiSociaAction() > 0)
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_ALREADY_PARTICIPATING_IN_A_COUPLE_ACTION_AND_CANNOT_BE_REQUESTED_FOR_ANOTHER_COUPLE_ACTION);
			sm.addPcName(partner);
			player.sendPacket(sm);
			return;
		}
		
		if (partner.isFishing())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_FISHING_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			player.sendPacket(sm);
			return;
		}
		
		if (partner.getReputation() < 0)
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_IN_A_CHAOTIC_STATE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			player.sendPacket(sm);
			return;
		}
		
		if (partner.isInOlympiadMode())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_PARTICIPATING_IN_THE_OLYMPIAD_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			player.sendPacket(sm);
			return;
		}
		
		if (partner.isInHideoutSiege())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_PARTICIPATING_IN_A_CLAN_HALL_SIEGE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			player.sendPacket(sm);
			return;
		}
		
		if (partner.isInSiege())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_IN_A_CASTLE_SIEGE_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			player.sendPacket(sm);
			return;
		}
		
		if (partner.isMounted() || partner.isFlyingMounted() || partner.isInBoat() || partner.isInAirShip())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_RIDING_A_SHIP_STEED_OR_STRIDER_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			player.sendPacket(sm);
			return;
		}
		
		if (partner.isTeleporting())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_CURRENTLY_TELEPORTING_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			player.sendPacket(sm);
			return;
		}
		
		if (partner.isTransformed())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_CURRENTLY_TRANSFORMING_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			player.sendPacket(sm);
			return;
		}
		
		if (partner.isAlikeDead())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_CURRENTLY_DEAD_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
			sm.addPcName(partner);
			player.sendPacket(sm);
			return;
		}
		
		if (player.isAllSkillsDisabled() || partner.isAllSkillsDisabled())
		{
			player.sendPacket(SystemMessageId.THE_COUPLE_ACTION_WAS_CANCELLED);
			return;
		}
		
		player.setMultiSocialAction(id, partner.getObjectId());
		sm = new SystemMessage(SystemMessageId.YOU_HAVE_REQUESTED_A_COUPLE_ACTION_WITH_C1);
		sm.addPcName(partner);
		player.sendPacket(sm);
		
		if ((player.getAI().getIntention() != Intention.IDLE) || (partner.getAI().getIntention() != Intention.IDLE))
		{
			final NextAction nextAction = new NextAction(Action.ARRIVED, Intention.MOVE_TO, () -> partner.sendPacket(new ExAskCoupleAction(player.getObjectId(), id)));
			player.getAI().setNextAction(nextAction);
			return;
		}
		
		if (player.isCastingNow())
		{
			final NextAction nextAction = new NextAction(Action.FINISH_CASTING, Intention.CAST, () -> partner.sendPacket(new ExAskCoupleAction(player.getObjectId(), id)));
			player.getAI().setNextAction(nextAction);
			return;
		}
		
		partner.sendPacket(new ExAskCoupleAction(player.getObjectId(), id));
	}
}
