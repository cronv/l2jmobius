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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.BlockList;
import org.l2jmobius.gameserver.entity.actor.holders.player.ClientSettings;
import org.l2jmobius.gameserver.entity.actor.request.PartyRequest;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.groups.PartyDistributionType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.AskJoinParty;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Handles a request from the client to join a specific party.<br>
 * This packet is processed by the server to validate and execute the joining logic.
 */
public class RequestJoinParty extends ClientPacket
{
	private String _name;
	private int _partyDistributionTypeId;
	
	@Override
	protected void readImpl()
	{
		_name = readString();
		_partyDistributionTypeId = readInt();
	}
	
	/**
	 * Sends a denial message to the player.<br>
	 * This method handles cases where a party is full or the request was declined.<br>
	 * It also completes the current transaction for the {@code Player}.
	 * @param player The {@code Player} who requested to join the party.
	 */
	private void scheduleDeny(Player player)
	{
		if (player != null)
		{
			if (player.getParty() == null)
			{
				player.sendPacket(SystemMessageId.THE_PARTY_IS_DISBANDED);
			}
			else
			{
				player.sendPacket(SystemMessageId.THE_PLAYER_HAS_DECLINED_TO_JOIN_YOUR_PARTY);
			}
			
			player.onTransactionResponse();
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player requestor = getPlayer();
		if (requestor == null)
		{
			return;
		}
		
		final ClientSettings clientSettings = requestor.getClientSettings();
		if (clientSettings.getPartyContributionType() != _partyDistributionTypeId)
		{
			requestor.getClientSettings().setPartyContributionType(_partyDistributionTypeId);
		}
		
		if (FakePlayerData.getInstance().isTalkable(_name))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_BEEN_INVITED_TO_THE_PARTY);
			sm.addString(FakePlayerData.getInstance().getProperName(_name));
			requestor.sendPacket(sm);
			if (!requestor.isProcessingRequest())
			{
				ThreadPool.schedule(() -> scheduleDeny(requestor), 10000);
				requestor.blockRequest();
			}
			else
			{
				requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
			}
			return;
		}
		
		final Player target;
		if (requestor.isInsideZone(ZoneId.CONQUEST))
		{
			final WorldObject requestorTarget = requestor.getTarget();
			if ((requestorTarget != null) && requestorTarget.isPlayer())
			{
				target = requestorTarget.asPlayer();
			}
			else // The Conquest name contains a character at start.
			{
				Player found = null;
				for (Player player : World.getPlayers())
				{
					final String conquestName = player.getConquestName();
					if ((conquestName != null) && _name.equalsIgnoreCase(conquestName.substring(1)))
					{
						found = player;
						break;
					}
				}
				target = found;
			}
			
			if ((target == null) || !target.isInsideZone(ZoneId.CONQUEST))
			{
				requestor.sendPacket(SystemMessageId.THE_TARGET_CANNOT_BE_INVITED);
				return;
			}
		}
		else
		{
			target = World.getPlayer(_name);
		}
		
		if (target == null)
		{
			requestor.sendPacket(SystemMessageId.SELECT_A_PLAYER_YOU_WANT_TO_INVITE_TO_YOUR_PARTY);
			return;
		}
		
		final GameClient client = target.getClient();
		if ((client == null) || client.isDetached())
		{
			requestor.sendMessage("Player is in offline mode.");
			return;
		}
		
		if (requestor.isPartyBanned())
		{
			requestor.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REPORTED_AS_AN_ILLEGAL_PROGRAM_USER_SO_PARTICIPATING_IN_A_PARTY_IS_NOT_ALLOWED);
			requestor.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (target.isPartyBanned())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_BEEN_REPORTED_AS_AN_ILLEGAL_PROGRAM_USER_AND_CANNOT_JOIN_A_PARTY);
			sm.addString(target.getAppearance().getVisibleName());
			requestor.sendPacket(sm);
			return;
		}
		
		if (requestor.isRegisteredOnEvent() || target.isRegisteredOnEvent())
		{
			if (GeneralConfig.ALLOW_PARTY_IN_SAME_EVENT)
			{
				if (!((requestor.getInstanceId() == target.getInstanceId()) && requestor.isRegisteredOnEvent() && target.isRegisteredOnEvent()))
				{
					requestor.sendMessage("Event paticipants cannot be invited to parties.");
					return;
				}
				
				if (!requestor.getTeam().equals(target.getTeam()))
				{
					requestor.sendMessage("You cannot be invited to a party of another team.");
					return;
				}
			}
			else
			{
				requestor.sendMessage("Event paticipants cannot be invited to parties.");
				return;
			}
		}
		
		SystemMessage sm;
		if (target.isInParty())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_A_MEMBER_OF_ANOTHER_PARTY_AND_CANNOT_BE_INVITED);
			sm.addString(target.getAppearance().getVisibleName());
			requestor.sendPacket(sm);
			return;
		}
		
		// Nobody can invite to a party while blocking everything, or invite a player who blocks everything.
		if (requestor.getMessageRefusal())
		{
			requestor.sendPacket(SystemMessageId.WHILE_BLOCKING_EVERYTHING_IT_IS_NOT_POSSIBLE_TO_SEND_INVITATIONS_FOR_ORGANIZING_PARTIES);
			return;
		}
		
		if (target.getMessageRefusal())
		{
			requestor.sendPacket(new SystemMessage(SystemMessageId.S1_IS_BLOCKING_EVERYTHING).addString(target.getName()));
			return;
		}
		
		if (BlockList.isBlocked(target, requestor))
		{
			sm = new SystemMessage(SystemMessageId.C1_HAS_ADDED_YOU_TO_THEIR_IGNORE_LIST);
			sm.addString(target.getAppearance().getVisibleName());
			requestor.sendPacket(sm);
			return;
		}
		
		if (target == requestor)
		{
			requestor.sendPacket(SystemMessageId.THE_TARGET_CANNOT_BE_INVITED);
			return;
		}
		
		if (checkInviteByIgnoredSettings(target, requestor))
		{
			requestor.sendPacket(new SystemMessage(SystemMessageId.C1_IS_SET_TO_REFUSE_PARTY_REQUESTS_AND_CANNOT_RECEIVE_A_PARTY_REQUEST).addPcName(target));
			target.sendPacket(new SystemMessage(SystemMessageId.PARTY_INVITATION_IS_SET_UP_TO_BE_REJECTED_AT_PREFERENCES_THE_PARTY_INVITATION_OF_C1_IS_AUTOMATICALLY_REJECTED).addPcName(requestor));
			return;
		}
		
		// Removed in version Prelude of War - 02 August 2019.
		// if (target.isCursedWeaponEquipped() || requestor.isCursedWeaponEquipped())
		// {
		// requestor.sendPacket(SystemMessageId.INVALID_TARGET);
		// return;
		// }
		
		if (target.isJailed() || requestor.isJailed())
		{
			requestor.sendMessage("You cannot invite a player while is in Jail.");
			return;
		}
		
		final boolean targetInOlympiad = target.isInOlympiadMode();
		final boolean requestorInOlympiad = requestor.isInOlympiadMode();
		if ((targetInOlympiad || requestorInOlympiad) && ((targetInOlympiad != requestorInOlympiad) || (target.getOlympiadGameId() != requestor.getOlympiadGameId()) || (target.getOlympiadSide() != requestor.getOlympiadSide())))
		{
			requestor.sendPacket(SystemMessageId.A_USER_CURRENTLY_PARTICIPATING_IN_THE_OLYMPIAD_CANNOT_SEND_PARTY_AND_FRIEND_INVITATIONS);
			return;
		}
		
		if (requestor.isProcessingRequest())
		{
			requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
			return;
		}
		
		if (target.isProcessingRequest())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER);
			sm.addString(target.getName());
			requestor.sendPacket(sm);
			return;
		}
		
		final Party party = requestor.getParty();
		if ((party != null) && !party.isLeader(requestor))
		{
			requestor.sendPacket(SystemMessageId.ONLY_THE_LEADER_CAN_GIVE_OUT_INVITATIONS);
			return;
		}
		
		sm = new SystemMessage(SystemMessageId.C1_HAS_BEEN_INVITED_TO_THE_PARTY);
		sm.addString(target.getAppearance().getVisibleName());
		requestor.sendPacket(sm);
		
		if (!requestor.isInParty())
		{
			createNewParty(target, requestor);
		}
		else
		{
			addTargetToParty(target, requestor);
		}
	}
	
	/**
	 * Sends a party invitation to another player.<br>
	 * This method checks if the requestor is the leader and if the party has space.<br>
	 * It also verifies if the target is currently busy with another task.
	 * @param target The {@code Player} who will receive the invitation.
	 * @param requestor The {@code Player} who is sending the invitation.
	 */
	private void addTargetToParty(Player target, Player requestor)
	{
		final Party party = requestor.getParty();
		
		// Summary of people already in party and people that get invitation.
		if (!party.isLeader(requestor))
		{
			requestor.sendPacket(SystemMessageId.ONLY_THE_LEADER_CAN_GIVE_OUT_INVITATIONS);
		}
		else if (party.getMemberCount() >= PlayerConfig.ALT_PARTY_MAX_MEMBERS)
		{
			requestor.sendPacket(SystemMessageId.THE_PARTY_IS_FULL);
		}
		else if (party.getPendingInvitation() && !party.isInvitationRequestExpired())
		{
			requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
		}
		else if (!target.hasRequest(PartyRequest.class))
		{
			final PartyRequest request = new PartyRequest(requestor, target, party);
			request.scheduleTimeout(30 * 1000);
			requestor.addRequest(request);
			target.addRequest(request);
			target.sendPacket(new AskJoinParty(requestor.getAppearance().getVisibleName(), party.getDistributionType()));
			party.setPendingInvitation(true);
		}
		else
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER);
			sm.addString(target.getAppearance().getVisibleName());
			requestor.sendPacket(sm);
		}
	}
	
	/**
	 * Creates a new {@code Party} for the requestor based on the distribution type.<br>
	 * It sends an invitation to the target player if no pending request exists.<br>
	 * If a request is already pending, it notifies the requestor to wait.
	 * @param target The player who receives the party invitation.
	 * @param requestor The player who initiates the new party creation.
	 */
	private void createNewParty(Player target, Player requestor)
	{
		final PartyDistributionType partyDistributionType = PartyDistributionType.findById(_partyDistributionTypeId);
		if (partyDistributionType == null)
		{
			return;
		}
		
		if (!target.hasRequest(PartyRequest.class))
		{
			final Party party = new Party(requestor, partyDistributionType);
			party.setPendingInvitation(true);
			final PartyRequest request = new PartyRequest(requestor, target, party);
			request.scheduleTimeout(30 * 1000);
			requestor.addRequest(request);
			target.addRequest(request);
			target.sendPacket(new AskJoinParty(requestor.getAppearance().getVisibleName(), partyDistributionType));
		}
		else
		{
			requestor.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
		}
	}
	
	/**
	 * Checks if a party invitation is blocked by the target's client settings.<br>
	 * It verifies if requests are restricted from others while allowing friends or clan members.
	 * @param target The {@code Player} who receives the invitation request.
	 * @param requestor The {@code Player} who is sending the invitation request.
	 * @return {@code true} if the invitation should be blocked, {@code false} otherwise.
	 */
	private boolean checkInviteByIgnoredSettings(Player target, Player requestor)
	{
		final ClientSettings targetClientSettings = target.getClientSettings();
		boolean condition = targetClientSettings.isPartyRequestRestrictedFromOthers();
		final boolean clanCheck = (target.getClan() != null) && (requestor.getClan() != null) && (target.getClan() == requestor.getClan());
		if (condition && ((!targetClientSettings.isPartyRequestRestrictedFromFriends() && target.getFriends().contains(requestor.getObjectId())) || (!targetClientSettings.isPartyRequestRestrictedFromClan() && clanCheck)))
		{
			condition = false;
		}
		
		return condition;
	}
}
