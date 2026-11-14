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
package org.l2jmobius.gameserver.entity.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.holders.player.BlockList;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class serves as a base for managing collections of {@code Player} entities.<br>
 * It provides common functionality for handling groups of players within the game world.
 * @author Battlecruiser
 */
public abstract class AbstractPlayerGroup
{
	/**
	 * @return a list of all members of this group
	 */
	public abstract List<Player> getMembers();
	
	/**
	 * Retrieves the unique object IDs of all members in this group.<br>
	 * This method iterates through every {@code Player} in the group.<br>
	 * It collects their individual IDs into a list.
	 * @return a {@code List<Integer>} containing the object IDs of all members.
	 */
	public List<Integer> getMembersObjectId()
	{
		final List<Integer> ids = new ArrayList<>();
		forEachMember(m ->
		{
			ids.add(m.getObjectId());
			return true;
		});
		
		return ids;
	}
	
	/**
	 * @return the leader of this group
	 */
	public abstract Player getLeader();
	
	/**
	 * Change the leader of this group to the specified player.
	 * @param leader the player to set as the new leader of this group
	 */
	public abstract void setLeader(Player leader);
	
	/**
	 * Retrieves the unique object ID of the group leader.<br>
	 * Returns {@code 0} if no leader exists.
	 * @return The {@code int} object ID of the leader, or {@code 0} if null.
	 */
	public int getLeaderObjectId()
	{
		final Player leader = getLeader();
		if (leader == null)
		{
			return 0;
		}
		
		return leader.getObjectId();
	}
	
	/**
	 * Checks if a specific {@code Player} is the leader of this group.<br>
	 * It returns {@code false} if the {@code player} is {@code null}.<br>
	 * It also returns {@code false} if the group has no leader.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the {@code player} is the leader, otherwise {@code false}.
	 */
	public boolean isLeader(Player player)
	{
		if (player == null)
		{
			return false;
		}
		
		final Player leader = getLeader();
		if (leader == null)
		{
			return false;
		}
		
		return leader.getObjectId() == player.getObjectId();
	}
	
	/**
	 * Gets the total number of members in this group.<br>
	 * It returns the size of the member list.
	 * @return The count of players currently in the group.
	 */
	public int getMemberCount()
	{
		return getMembers().size();
	}
	
	/**
	 * Calculates the number of unique races within this group.<br>
	 * It checks every {@code Player} in the group to identify distinct {@code Race} types.
	 * @return The total count of unique races.
	 */
	public int getRaceCount()
	{
		final List<Race> partyRaces = new ArrayList<>();
		for (Player member : getMembers())
		{
			final Race race = member.getRace();
			if (!partyRaces.contains(race))
			{
				partyRaces.add(race);
			}
		}
		
		return partyRaces.size();
	}
	
	/**
	 * @return the level of this group
	 */
	public abstract int getLevel();
	
	/**
	 * Sends a packet to every member of this group.<br>
	 * It iterates through all members and calls {@code sendPacket} on each non-null player.
	 * @param packet The {@code ServerPacket} to be sent to the members.
	 */
	public void broadcastPacket(ServerPacket packet)
	{
		forEachMember(m ->
		{
			if (m != null)
			{
				m.sendPacket(packet);
			}
			
			return true;
		});
	}
	
	/**
	 * Sends a system message to all members of the group.<br>
	 * This method creates a new {@code SystemMessage} using the provided ID.<br>
	 * It then uses {@code broadcastPacket} to deliver it to everyone.
	 * @param message The unique identifier for the system message to be sent.
	 */
	public void broadcastMessage(SystemMessageId message)
	{
		broadcastPacket(new SystemMessage(message));
	}
	
	/**
	 * Sends a text message to all members of the group.<br>
	 * This method wraps the {@code text} in a {@code SystemMessage}.
	 * @param text The content of the message to be sent.
	 */
	public void broadcastString(String text)
	{
		broadcastPacket(new SystemMessage(text));
	}
	
	/**
	 * Sends a {@code CreatureSay} packet to all members of the group.<br>
	 * It skips players who have blocked the {@code broadcaster}.
	 * @param msg The {@code CreatureSay} packet to send.
	 * @param broadcaster The {@code Player} who is sending the message.
	 */
	public void broadcastCreatureSay(CreatureSay msg, Player broadcaster)
	{
		forEachMember(m ->
		{
			if ((m != null) && !BlockList.isBlocked(m, broadcaster))
			{
				m.sendPacket(msg);
			}
			
			return true;
		});
	}
	
	/**
	 * Checks if a specific {@code Player} is part of this group.<br>
	 * It returns {@code true} if the player is found in the member list.<br>
	 * It returns {@code false} otherwise.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if the player is a member, {@code false} otherwise.
	 */
	public boolean containsPlayer(Player player)
	{
		return getMembers().contains(player);
	}
	
	/**
	 * Selects a random member from the current group.<br>
	 * It picks one {@code Player} from the list of all members.
	 * @return a random {@code Player} object.
	 */
	public Player getRandomPlayer()
	{
		return getMembers().get(Rnd.get(getMembers().size()));
	}
	
	/**
	 * Performs an action on every member of the group.<br>
	 * It applies the provided {@code procedure} to each {@code Player}.<br>
	 * The method stops and returns {@code false} if any procedure returns {@code false}.<br>
	 * Otherwise, it returns {@code true}.
	 * @param procedure A function to apply to each member.
	 * @return {@code true} if all procedures succeeded, or {@code false} otherwise.
	 */
	public boolean forEachMember(Function<Player, Boolean> procedure)
	{
		for (Player player : getMembers())
		{
			if (!procedure.apply(player))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Compares this group to another object.<br>
	 * It checks if both objects are the same instance.<br>
	 * If they are different instances, it compares their leader IDs.
	 * @param obj The object to compare with this one.
	 * @return {@code true} if the objects are equal, {@code false} otherwise.
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		
		return (obj instanceof AbstractPlayerGroup) && (getLeaderObjectId() == ((AbstractPlayerGroup) obj).getLeaderObjectId());
	}
}
