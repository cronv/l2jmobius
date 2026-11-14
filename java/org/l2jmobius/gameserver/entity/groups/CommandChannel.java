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
package org.l2jmobius.gameserver.entity.groups;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExCloseMPCC;
import org.l2jmobius.gameserver.network.serverpackets.ExMPCCPartyInfoUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ExOpenMPCC;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class serves as a container for command channels.<br>
 * It manages groups of players who are subscribed to specific commands.
 * @author chris_00, Mobius
 */
public class CommandChannel extends AbstractPlayerGroup
{
	// Looting rights on boss monsters (Queen Ant, Core, Orfen, Zaken...) and on Antharas.
	private static final int BOSS_LOOT_CC_SIZE = 36;
	private static final int ANTHARAS_LOOT_CC_SIZE = 225;
	private static final Set<Integer> ANTHARAS_IDS = Set.of(29019, 29066, 29067, 29068);
	
	private final Collection<Party> _parties = ConcurrentHashMap.newKeySet();
	private Player _commandLeader;
	private int _channelLvl;
	
	/**
	 * Creates a new {@code CommandChannel} instance.<br>
	 * This method sets the leader and adds their party to the channel.<br>
	 * It also initializes the channel level based on the leader's party.
	 * @param leader The {@code Player} who will lead this command channel.
	 */
	public CommandChannel(Player leader)
	{
		_commandLeader = leader;
		final Party party = leader.getParty();
		_parties.add(party);
		_channelLvl = party.getLevel();
		party.setCommandChannel(this);
		party.broadcastMessage(SystemMessageId.THE_COMMAND_CHANNEL_HAS_BEEN_FORMED);
		party.broadcastPacket(ExOpenMPCC.STATIC_PACKET);
	}
	
	/**
	 * Adds a {@code Party} to this command channel.<br>
	 * This method updates the channel level if necessary.<br>
	 * It also sends notification packets to the party members.
	 * @param party The {@code Party} object to be added.
	 */
	public void addParty(Party party)
	{
		if (party == null)
		{
			return;
		}
		
		// Update the CCinfo for existing players.
		broadcastPacket(new ExMPCCPartyInfoUpdate(party, 1));
		_parties.add(party);
		final int partyLevel = party.getLevel();
		if (partyLevel > _channelLvl)
		{
			_channelLvl = partyLevel;
		}
		
		party.setCommandChannel(this);
		party.broadcastPacket(new SystemMessage(SystemMessageId.YOU_HAVE_JOINED_THE_COMMAND_CHANNEL));
		party.broadcastPacket(ExOpenMPCC.STATIC_PACKET);
		
		if (_parties.size() == 5)
		{
			broadcastPacket(new SystemMessage(SystemMessageId.THE_COMMAND_CHANNEL_HAS_BEEN_ACTIVATED));
		}
	}
	
	/**
	 * Removes a specific {@code Party} from this command channel.<br>
	 * This method updates the channel level and notifies the removed party.<br>
	 * If fewer than two parties remain, the channel is disbanded automatically.
	 * @param party The {@code Party} object to remove from the collection.
	 */
	public void removeParty(Party party)
	{
		if (party == null)
		{
			return;
		}
		
		_parties.remove(party);
		_channelLvl = 0;
		for (Party pty : _parties)
		{
			final int ptyLevel = pty.getLevel();
			if (ptyLevel > _channelLvl)
			{
				_channelLvl = ptyLevel;
			}
		}
		
		party.setCommandChannel(null);
		party.broadcastPacket(ExCloseMPCC.STATIC_PACKET);
		if (_parties.size() < 2)
		{
			broadcastPacket(new SystemMessage(SystemMessageId.THE_COMMAND_CHANNEL_IS_DISBANDED));
			disbandChannel();
		}
		else
		{
			// Update the CCinfo for existing players.
			broadcastPacket(new ExMPCCPartyInfoUpdate(party, 0));
		}
	}
	
	/**
	 * Closes the current command channel.<br>
	 * This method removes all parties from the {@code _parties} collection.<br>
	 * It clears the internal list of parties to disband the group.
	 */
	public void disbandChannel()
	{
		if (_parties != null)
		{
			for (Party party : _parties)
			{
				if (party != null)
				{
					removeParty(party);
				}
			}
			
			_parties.clear();
		}
	}
	
	/**
	 * Calculates the total number of members in this command channel.<br>
	 * It sums up the member counts from all parties currently in the channel.
	 * @return The total count of players belonging to all parties.
	 */
	@Override
	public int getMemberCount()
	{
		int count = 0;
		for (Party party : _parties)
		{
			if (party != null)
			{
				count += party.getMemberCount();
			}
		}
		
		return count;
	}
	
	/**
	 * Retrieves all parties currently in this command channel.
	 * @return a {@code Collection} of {@code Party} objects.
	 */
	public Collection<Party> getParties()
	{
		return _parties;
	}
	
	/**
	 * Retrieves all players belonging to this command channel.<br>
	 * This method collects every {@code Player} from all parties currently in the group.
	 * @return a {@code List} of all {@code Player} objects in the channel.
	 */
	@Override
	public List<Player> getMembers()
	{
		final List<Player> members = new ArrayList<>();
		for (Party party : _parties)
		{
			members.addAll(party.getMembers());
		}
		
		return members;
	}
	
	/**
	 * Retrieves the current level of this command channel.<br>
	 * This value is stored in the {@code _channelLvl} field.
	 * @return the integer level of the channel.
	 */
	@Override
	public int getLevel()
	{
		return _channelLvl;
	}
	
	/**
	 * Sets the leader for this command channel.<br>
	 * This method also updates the channel level if the new leader has a higher level.
	 * @param leader The {@code Player} to be set as the new leader.
	 */
	@Override
	public void setLeader(Player leader)
	{
		_commandLeader = leader;
		
		final int leaderLevel = leader.getLevel();
		if (leaderLevel > _channelLvl)
		{
			_channelLvl = leaderLevel;
		}
	}
	
	/**
	 * Checks if the current channel meets the requirements to participate in a raid war.<br>
	 * It verifies that the target object is a raid creature.<br>
	 * Raid bosses need the configured member count, boss monsters 36 members and Antharas 225 members.
	 * @param obj The world object to check.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	public boolean meetRaidWarCondition(WorldObject obj)
	{
		if (!(obj.isCreature() && obj.asCreature().isRaid()))
		{
			return false;
		}
		
		if (obj instanceof GrandBoss)
		{
			return getMemberCount() >= (ANTHARAS_IDS.contains(obj.getId()) ? ANTHARAS_LOOT_CC_SIZE : BOSS_LOOT_CC_SIZE);
		}
		
		return (getMemberCount() >= PlayerConfig.LOOT_RAIDS_PRIVILEGE_CC_SIZE);
	}
	
	/**
	 * Retrieves the leader of this command channel.<br>
	 * Returns the {@code Player} object representing the leader.
	 * @return the current leader of the channel, or {@code null} if no leader exists.
	 */
	@Override
	public Player getLeader()
	{
		return _commandLeader;
	}
	
	/**
	 * Checks if a specific player is part of this command channel.<br>
	 * It looks through all parties associated with the channel.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the player is found, otherwise {@code false}.
	 */
	@Override
	public boolean containsPlayer(Player player)
	{
		if ((_parties != null) && !_parties.isEmpty())
		{
			for (Party party : _parties)
			{
				if (party.containsPlayer(player))
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Performs an action on every member of every party in this channel.<br>
	 * It applies the provided {@code Function} to each {@code Player}.<br>
	 * If any operation returns {@code false}, the process stops early.
	 * @param function a function to apply to each {@code Player}
	 * @return {@code true} if all operations succeeded, or {@code false} otherwise
	 */
	@Override
	public boolean forEachMember(Function<Player, Boolean> function)
	{
		if ((_parties != null) && !_parties.isEmpty())
		{
			for (Party party : _parties)
			{
				if (!party.forEachMember(function))
				{
					return false;
				}
			}
		}
		
		return true;
	}
}
