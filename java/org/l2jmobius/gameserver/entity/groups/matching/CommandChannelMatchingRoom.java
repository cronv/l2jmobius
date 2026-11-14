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
import org.l2jmobius.gameserver.entity.groups.CommandChannel;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.managers.MatchingRoomManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ExManagePartyRoomMemberType;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.network.serverpackets.ExDissmissMPCCRoom;
import org.l2jmobius.gameserver.network.serverpackets.ExMPCCRoomInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExMPCCRoomMember;
import org.l2jmobius.gameserver.network.serverpackets.ExManageMpccRoomMember;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Represents a matching room specifically for {@code CommandChannel} groups.<br>
 * It handles the logic for grouping players together based on command channel criteria.
 * @author Sdw
 */
public class CommandChannelMatchingRoom extends MatchingRoom
{
	/**
	 * Creates a new {@code CommandChannelMatchingRoom} instance.<br>
	 * This constructor initializes the room with specific requirements and a leader.
	 * @param title The display name of the matching room.
	 * @param loot The amount of loot available in the room.
	 * @param minLevel The minimum level required to join.
	 * @param maxLevel The maximum level allowed in the room.
	 * @param maxmem The maximum memory capacity for members.
	 * @param leader The {@code Player} who will lead the room.
	 */
	public CommandChannelMatchingRoom(String title, int loot, int minLevel, int maxLevel, int maxmem, Player leader)
	{
		super(title, loot, minLevel, maxLevel, maxmem, leader);
	}
	
	/**
	 * Handles the logic when a new room is created.<br>
	 * This method sends a system message to the {@code player}.
	 * @param player The {@code Player} who initiated the room creation.
	 */
	@Override
	protected void onRoomCreation(Player player)
	{
		player.sendPacket(SystemMessageId.THE_COMMAND_CHANNEL_MATCHING_ROOM_WAS_CREATED);
	}
	
	/**
	 * Sends a system message to the player.<br>
	 * This happens when the {@code Player} does not meet the room requirements.
	 * @param player The {@code Player} who failed the requirement check.
	 */
	@Override
	protected void notifyInvalidCondition(Player player)
	{
		player.sendPacket(SystemMessageId.YOU_CANNOT_ENTER_THE_COMMAND_CHANNEL_MATCHING_ROOM_BECAUSE_YOU_DO_NOT_MEET_THE_REQUIREMENTS);
	}
	
	/**
	 * This method notifies all existing members when a new {@code Player} joins the room.<br>
	 * It sends an update packet to current members and a system message.<br>
	 * The new {@code Player} also receives updated room information packets.
	 * @param player The {@code Player} who has just joined the matching room.
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
				member.sendPacket(new ExManageMpccRoomMember(member, this, ExManagePartyRoomMemberType.ADD_MEMBER));
			}
		}
		
		// Send SystemMessage to other players.
		final SystemMessage sm = new SystemMessage(SystemMessageId.C1_ENTERED_THE_COMMAND_CHANNEL_MATCHING_ROOM);
		sm.addPcName(player);
		for (Player member : members)
		{
			if (member != player)
			{
				member.sendPacket(sm);
			}
		}
		
		// Update new player
		player.sendPacket(new ExMPCCRoomInfo(this));
		player.sendPacket(new ExMPCCRoomMember(player, this));
	}
	
	/**
	 * Notifies all members that a player has left the matching room.<br>
	 * Updates the room information for everyone remaining.<br>
	 * Sends a specific system message to the departing player based on their exit status.
	 * @param player The {@code Player} who is being removed from the room.
	 * @param kicked Whether the player was forcibly removed with {@code true} or left voluntarily with {@code false}.
	 * @param leaderChanged Whether the leadership of the room changed during this removal.
	 */
	@Override
	protected void notifyRemovedMember(Player player, boolean kicked, boolean leaderChanged)
	{
		getMembers().forEach(p ->
		{
			p.sendPacket(new ExMPCCRoomInfo(this));
			p.sendPacket(new ExMPCCRoomMember(player, this));
		});
		
		player.sendPacket(new SystemMessage(kicked ? SystemMessageId.YOU_WERE_EXPELLED_FROM_THE_COMMAND_CHANNEL_MATCHING_ROOM : SystemMessageId.YOU_EXITED_FROM_THE_COMMAND_CHANNEL_MATCHING_ROOM));
	}
	
	/**
	 * Closes the current matching room.<br>
	 * Notifies all members that the room was cancelled.<br>
	 * Removes all members from the room and returns them to the waiting list.<br>
	 * Clears the member list and removes this room from {@code MatchingRoomManager}.
	 */
	@Override
	public void disbandRoom()
	{
		final Set<Player> members = getMembers();
		members.forEach(p ->
		{
			p.sendPacket(SystemMessageId.THE_COMMAND_CHANNEL_MATCHING_ROOM_WAS_CANCELLED);
			p.sendPacket(ExDissmissMPCCRoom.STATIC_PACKET);
			p.setMatchingRoom(null);
			p.broadcastUserInfo(UserInfoType.CLAN);
			MatchingRoomManager.getInstance().addToWaitingList(p);
		});
		
		members.clear();
		
		MatchingRoomManager.getInstance().removeMatchingRoom(this);
	}
	
	/**
	 * Retrieves the type of this matching room.<br>
	 * This method returns a constant value representing the command channel type.
	 * @return the {@code MatchingRoomType} for this room.
	 */
	@Override
	public MatchingRoomType getRoomType()
	{
		return MatchingRoomType.COMMAND_CHANNEL;
	}
	
	/**
	 * Determines the membership status of a {@code Player} within this room.<br>
	 * It checks if the player is the leader, in a party with the leader, or waiting alone.
	 * @param player The {@code Player} to check.
	 * @return The {@code MatchingMemberType} corresponding to the player's status.
	 */
	@Override
	public MatchingMemberType getMemberType(Player player)
	{
		if (isLeader(player))
		{
			return MatchingMemberType.COMMAND_CHANNEL_LEADER;
		}
		
		final Party playerParty = player.getParty();
		if (playerParty == null)
		{
			return MatchingMemberType.WAITING_PLAYER_NO_PARTY;
		}
		
		final Party leaderParty = getLeader().getParty();
		if (leaderParty != null)
		{
			final CommandChannel cc = leaderParty.getCommandChannel();
			if ((leaderParty == playerParty) || ((cc != null) && cc.getParties().contains(playerParty)))
			{
				return MatchingMemberType.COMMAND_CHANNEL_PARTY_MEMBER;
			}
		}
		
		return MatchingMemberType.WAITING_PARTY;
	}
}
