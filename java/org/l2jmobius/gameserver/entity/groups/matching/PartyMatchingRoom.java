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
package org.l2jmobius.gameserver.entity.groups.matching;

import java.util.Set;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.managers.MatchingRoomManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.network.serverpackets.ExClosePartyRoom;
import org.l2jmobius.gameserver.network.serverpackets.ExPartyRoomMember;
import org.l2jmobius.gameserver.network.serverpackets.ListPartyWaiting;
import org.l2jmobius.gameserver.network.serverpackets.PartyRoomInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Represents a specific room where players can gather to form a {@code Party}.<br>
 * It manages the logic for grouping players together during the matchmaking process.
 * @author Sdw
 */
public class PartyMatchingRoom extends MatchingRoom
{
	/**
	 * Creates a new {@code PartyMatchingRoom} with specific settings.<br>
	 * This constructor initializes the room requirements and assigns a leader.
	 * @param title The display name of the matching room.
	 * @param loot The amount of loot available in this room.
	 * @param minLevel The minimum level required to join.
	 * @param maxLevel The maximum level allowed to join.
	 * @param maxmem The maximum number of members allowed in the room.
	 * @param leader The {@code Player} who will lead this matching room.
	 */
	public PartyMatchingRoom(String title, int loot, int minLevel, int maxLevel, int maxmem, Player leader)
	{
		super(title, loot, minLevel, maxLevel, maxmem, leader);
	}
	
	/**
	 * This method is called when a new room is created.<br>
	 * It handles the initial actions for the player who started the room.<br>
	 * The player receives a system message and updated information about waiting parties.
	 * @param player The {@code Player} who created the room.
	 */
	@Override
	protected void onRoomCreation(Player player)
	{
		player.broadcastUserInfo(UserInfoType.CLAN);
		player.sendPacket(new ListPartyWaiting(PartyMatchingRoomLevelType.ALL, -1, 1, player.getLevel()));
		player.sendPacket(SystemMessageId.YOU_HAVE_CREATED_A_PARTY_ROOM);
	}
	
	/**
	 * Sends a notification to the player.<br>
	 * This happens when they do not meet the requirements for a party room.<br>
	 * The method sends the {@code SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIREMENTS_TO_ENTER_A_PARTY_ROOM} packet.
	 * @param player The {@code Player} who failed to join the room.
	 */
	@Override
	protected void notifyInvalidCondition(Player player)
	{
		player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIREMENTS_TO_ENTER_A_PARTY_ROOM);
	}
	
	/**
	 * Sends notifications to all members when a new {@code Player} joins the room.<br>
	 * It updates existing members about the new arrival.<br>
	 * It also sends the initial room information to the joining {@code Player}.
	 * @param player The {@code Player} who just joined the matching room.
	 */
	@Override
	protected void notifyNewMember(Player player)
	{
		// Update other players
		final Set<Player> members = getMembers();
		for (Player member : members)
		{
			if (member != player)
			{
				member.sendPacket(new ExPartyRoomMember(member, this));
			}
		}
		
		// Send SystemMessage to other players.
		final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_ENTERED_THE_PARTY_ROOM);
		sm.addPcName(player);
		for (Player member : members)
		{
			if (member != player)
			{
				member.sendPacket(sm);
			}
		}
		
		// Update new player
		player.sendPacket(new PartyRoomInfo(this));
		player.sendPacket(new ExPartyRoomMember(player, this));
	}
	
	/**
	 * Sends notifications to all members when a player leaves the room.<br>
	 * It handles both voluntary exits and forced kicks.<br>
	 * This method updates the party information for everyone remaining.
	 * @param player The {@code Player} who is being removed from the room.
	 * @param kicked Set to {@code true} if the player was forcibly removed, otherwise {@code false}.
	 * @param leaderChanged Set to {@code true} if the removal caused a change in leadership.
	 */
	@Override
	protected void notifyRemovedMember(Player player, boolean kicked, boolean leaderChanged)
	{
		final SystemMessage sm = new SystemMessage(kicked ? SystemMessageId.C1_HAS_BEEN_KICKED_FROM_THE_PARTY_ROOM : SystemMessageId.C1_HAS_LEFT_THE_PARTY_ROOM);
		sm.addPcName(player);
		
		getMembers().forEach(p ->
		{
			// The room settings did not change, so resending them would only rebuild the window and lose the chat written in it.
			p.sendPacket(new ExPartyRoomMember(p, this));
			p.sendPacket(sm);
			if (leaderChanged)
			{
				p.sendPacket(SystemMessageId.THE_LEADER_OF_THE_PARTY_ROOM_HAS_CHANGED);
			}
		});
		
		player.sendPacket(new SystemMessage(kicked ? SystemMessageId.YOU_HAVE_BEEN_OUSTED_FROM_THE_PARTY_ROOM : SystemMessageId.YOU_HAVE_EXITED_THE_PARTY_ROOM));
		player.sendPacket(ExClosePartyRoom.STATIC_PACKET);
	}
	
	/**
	 * Closes the current matching room and removes all members.<br>
	 * Each member receives a system message and is returned to the waiting list.<br>
	 * The room is then removed from the {@code MatchingRoomManager}.
	 */
	@Override
	public void disbandRoom()
	{
		final Set<Player> members = getMembers();
		members.forEach(p ->
		{
			p.sendPacket(SystemMessageId.THE_PARTY_ROOM_HAS_BEEN_DISBANDED);
			p.sendPacket(ExClosePartyRoom.STATIC_PACKET);
			p.setMatchingRoom(null);
			p.broadcastUserInfo(UserInfoType.CLAN);
			MatchingRoomManager.getInstance().addToWaitingList(p);
		});
		
		members.clear();
		
		MatchingRoomManager.getInstance().removeMatchingRoom(this);
	}
	
	/**
	 * Retrieves the type of this matching room.<br>
	 * This method identifies the room as a party-based match.
	 * @return the {@code MatchingRoomType} for this room.
	 */
	@Override
	public MatchingRoomType getRoomType()
	{
		return MatchingRoomType.PARTY;
	}
	
	/**
	 * Determines the role of a {@code Player} within this matching room.<br>
	 * It checks if the player is the leader, a party member, or waiting alone.
	 * @param player The {@code Player} to check.
	 * @return The {@code MatchingMemberType} assigned to the player.
	 */
	@Override
	public MatchingMemberType getMemberType(Player player)
	{
		if (isLeader(player))
		{
			return MatchingMemberType.PARTY_LEADER;
		}
		
		final Party leaderParty = getLeader().getParty();
		final Party playerParty = player.getParty();
		if ((leaderParty != null) && (playerParty != null) && (playerParty == leaderParty))
		{
			return MatchingMemberType.PARTY_MEMBER;
		}
		
		return MatchingMemberType.WAITING_PLAYER;
	}
}
