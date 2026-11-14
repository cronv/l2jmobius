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

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.ClassBalanceConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.holders.player.HuntPass;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.managers.DuelManager;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.managers.PcCafePointsManager;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.StatusUpdateType;
import org.l2jmobius.gameserver.network.serverpackets.ExAskModifyPartyLooting;
import org.l2jmobius.gameserver.network.serverpackets.ExCloseMPCC;
import org.l2jmobius.gameserver.network.serverpackets.ExOpenMPCC;
import org.l2jmobius.gameserver.network.serverpackets.ExPartyPetWindowAdd;
import org.l2jmobius.gameserver.network.serverpackets.ExPartyPetWindowDelete;
import org.l2jmobius.gameserver.network.serverpackets.ExSetPartyLooting;
import org.l2jmobius.gameserver.network.serverpackets.ExTacticalSign;
import org.l2jmobius.gameserver.network.serverpackets.PartyMemberPosition;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowAdd;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowAll;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowDelete;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowDeleteAll;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.StatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * This class serves as a container for player parties.<br>
 * It manages the grouping of {@code Player} entities and handles party-related logic.
 * @author nuocnam, Mobius
 */
public class Party extends AbstractPlayerGroup
{
	private static final Logger LOGGER = Logger.getLogger(Party.class.getName());
	
	// @formatter:off
	private static final double[] BONUS_EXP_SP =
	{
		1.0, 1.3, 1.4, 1.5, 1.6, 1.7, 2.0
	};
	// @formatter:on
	
	private static final Duration PARTY_POSITION_BROADCAST_INTERVAL = Duration.ofSeconds(12);
	private static final Duration PARTY_DISTRIBUTION_TYPE_REQUEST_TIMEOUT = Duration.ofSeconds(15);
	
	private final List<Player> _members = new CopyOnWriteArrayList<>();
	private boolean _pendingInvitation = false;
	private long _pendingInviteTimeout;
	private int _partyLvl = 0;
	private PartyDistributionType _distributionType = PartyDistributionType.FINDERS_KEEPERS;
	private PartyDistributionType _changeRequestDistributionType;
	private Future<?> _changeDistributionTypeRequestTask = null;
	private Set<Integer> _changeDistributionTypeAnswers = null;
	private int _itemLastLoot = 0;
	private CommandChannel _commandChannel = null;
	private Future<?> _positionBroadcastTask = null;
	protected PartyMemberPosition _positionPacket;
	private boolean _disbanding = false;
	private Object _ucState = null;
	private Map<Integer, Creature> _tacticalSigns = null;
	private static final int[] TACTICAL_SYS_STRINGS =
	{
		0,
		2664,
		2665,
		2666,
		2667
	};
	
	/**
	 * Creates a new {@code Party} instance.<br>
	 * This constructor initializes the party with a leader and a distribution type.<br>
	 * It also sets the initial party level based on the leader's level.<br>
	 * Finally, it increments the global party count in the {@code World}.
	 * @param leader The {@code Player} who will lead the party.
	 * @param partyDistributionType The type of distribution to use for the party.
	 */
	public Party(Player leader, PartyDistributionType partyDistributionType)
	{
		_members.add(leader);
		_partyLvl = leader.getLevel();
		_distributionType = partyDistributionType;
		World.incrementParty();
	}
	
	/**
	 * Checks if there is a pending invitation for this party.
	 * @return {@code true} if an invitation is waiting, {@code false} otherwise.
	 */
	public boolean getPendingInvitation()
	{
		return _pendingInvitation;
	}
	
	/**
	 * Updates the status of a pending invitation.<br>
	 * This method sets whether an invitation is currently waiting.<br>
	 * It also resets the timeout for the request based on current game ticks.
	 * @param value The new status to set for the pending invitation.
	 */
	public void setPendingInvitation(boolean value)
	{
		_pendingInvitation = value;
		_pendingInviteTimeout = GameTimeTaskManager.getInstance().getGameTicks() + (Player.REQUEST_TIMEOUT * GameTimeTaskManager.TICKS_PER_SECOND);
	}
	
	/**
	 * Checks if the current invitation request has timed out.<br>
	 * It compares the pending invite timeout against the current game ticks.
	 * @return {@code true} if the request is expired, {@code false} otherwise.
	 */
	public boolean isInvitationRequestExpired()
	{
		return (_pendingInviteTimeout <= GameTimeTaskManager.getInstance().getGameTicks());
	}
	
	/**
	 * Selects a random party member who can receive an item.<br>
	 * The member must have enough inventory space for the {@code itemId}.<br>
	 * The member must also be within the allowed range of the {@code target}.
	 * @param itemId The unique identifier of the item to check.
	 * @param target The creature used to calculate the distance range.
	 * @return A random {@code Player} from the valid members, or {@code null} if none are found.
	 */
	private Player getCheckedRandomMember(int itemId, Creature target)
	{
		final List<Player> availableMembers = new ArrayList<>();
		for (Player member : _members)
		{
			if (!member.isDead() && member.getInventory().validateCapacityByItemId(itemId) && LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, target, member, true))
			{
				availableMembers.add(member);
			}
		}
		
		return !availableMembers.isEmpty() ? availableMembers.get(Rnd.get(availableMembers.size())) : null;
	}
	
	/**
	 * Finds the next eligible party member to receive a specific item.<br>
	 * It iterates through members in a round-robin sequence.<br>
	 * A member is selected if they have inventory space and are within range of the target.
	 * @param itemId The unique identifier of the item to be looted.
	 * @param target The creature being looted from.
	 * @return The {@code Player} who qualifies as the next looter, or {@code null} if no one is eligible.
	 */
	private Player getCheckedNextLooter(int itemId, Creature target)
	{
		for (int i = 0; i < getMemberCount(); i++)
		{
			if (++_itemLastLoot >= getMemberCount())
			{
				_itemLastLoot = 0;
			}
			
			Player member;
			try
			{
				member = _members.get(_itemLastLoot);
				if (!member.isDead() && member.getInventory().validateCapacityByItemId(itemId) && LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, target, member, true))
				{
					return member;
				}
			}
			catch (Exception e)
			{
				// continue, take another member if this just logged off
			}
		}
		
		return null;
	}
	
	/**
	 * Determines the specific {@code Player} who should receive a loot item.<br>
	 * It selects a member based on the current distribution type and spoil status.<br>
	 * If no valid looter is found, it defaults to the provided {@code player}.
	 * @param player The player who initiated the action.
	 * @param itemId The unique identifier of the item being looted.
	 * @param spoil Whether the loot should be distributed even if it is a spoil item.
	 * @param target The creature from which the loot is being taken.
	 * @return The {@code Player} selected to receive the loot.
	 */
	private Player getActualLooter(Player player, int itemId, boolean spoil, Creature target)
	{
		Player looter = null;
		
		switch (_distributionType)
		{
			case RANDOM:
			{
				if (!spoil)
				{
					looter = getCheckedRandomMember(itemId, target);
				}
				break;
			}
			case RANDOM_INCLUDING_SPOIL:
			{
				looter = getCheckedRandomMember(itemId, target);
				break;
			}
			case BY_TURN:
			{
				if (!spoil)
				{
					looter = getCheckedNextLooter(itemId, target);
				}
				break;
			}
			case BY_TURN_INCLUDING_SPOIL:
			{
				looter = getCheckedNextLooter(itemId, target);
				break;
			}
		}
		
		return looter != null ? looter : player;
	}
	
	/**
	 * Notifies all party members about a new leader.<br>
	 * This method clears the old party window for every member.<br>
	 * It then sends an updated {@code PartySmallWindowAll} packet to each member.<br>
	 * Finally, it triggers a broadcast of user information to everyone in the group.
	 */
	public void broadcastToPartyMembersNewLeader()
	{
		for (Player member : _members)
		{
			if (member != null)
			{
				member.sendPacket(PartySmallWindowDeleteAll.STATIC_PACKET);
				member.sendPacket(new PartySmallWindowAll(member, this));
				member.broadcastUserInfo();
			}
		}
	}
	
	/**
	 * Sends a {@code ServerPacket} to all members of the party.<br>
	 * The packet is sent to everyone except the original {@code Player}.
	 * @param player The player who initiated the broadcast.
	 * @param packet The packet to be sent to other party members.
	 */
	public void broadcastToPartyMembers(Player player, ServerPacket packet)
	{
		for (Player member : _members)
		{
			if ((member != null) && (member.getObjectId() != player.getObjectId()))
			{
				member.sendPacket(packet);
			}
		}
	}
	
	/**
	 * Adds a new {@code Player} to the party.<br>
	 * This method updates the member list and synchronizes data for all members.<br>
	 * It also handles pet synchronization and adjusts the party level if necessary.
	 * @param player The {@code Player} to be added to the party.
	 */
	public void addPartyMember(Player player)
	{
		if (_members.contains(player))
		{
			return;
		}
		
		if (_changeRequestDistributionType != null)
		{
			finishLootRequest(false); // cancel on invite
		}
		
		// add player to party
		_members.add(player);
		
		// Sends the new member party window for all members; performing all actions before adding the member to the list speeds things up slightly.
		player.sendPacket(new PartySmallWindowAll(player, this));
		
		// sends pets/summons of party members
		for (Player pMember : _members)
		{
			if (pMember != null)
			{
				final Summon pet = pMember.getPet();
				if (pet != null)
				{
					player.sendPacket(new ExPartyPetWindowAdd(pet));
				}
				
				pMember.getServitors().values().forEach(s -> player.sendPacket(new ExPartyPetWindowAdd(s)));
			}
		}
		
		SystemMessage msg = new SystemMessage(SystemMessageId.YOU_HAVE_JOINED_A_PARTY);
		player.sendPacket(msg);
		
		msg = new SystemMessage(SystemMessageId.C1_HAS_JOINED_THE_PARTY);
		msg.addString(player.getAppearance().getVisibleName());
		broadcastToPartyMembers(player, msg);
		
		for (Player member : _members)
		{
			if (member != player)
			{
				member.sendPacket(new PartySmallWindowAdd(player, this));
			}
		}
		
		// send the position of all party members to the new party member
		// player.sendPacket(new PartyMemberPosition(this));
		// send the position of the new party member to all party members (except the new one - he knows his own position)
		// broadcastToPartyMembers(player, new PartyMemberPosition(this));
		
		// if member has pet/summon add it to other as well
		final Summon pet = player.getPet();
		if (pet != null)
		{
			broadcastPacket(new ExPartyPetWindowAdd(pet));
		}
		
		player.getServitors().values().forEach(s -> broadcastPacket(new ExPartyPetWindowAdd(s)));
		
		// adjust party level
		final int playerLevel = player.getLevel();
		if (playerLevel > _partyLvl)
		{
			_partyLvl = playerLevel;
		}
		
		// status update for hp bar display
		final StatusUpdate su = new StatusUpdate(player);
		su.addUpdate(StatusUpdateType.MAX_HP, (int) player.getMaxHp());
		su.addUpdate(StatusUpdateType.CUR_HP, (int) player.getCurrentHp());
		
		// update partySpelled
		Summon summon;
		for (Player member : _members)
		{
			if (member != null)
			{
				member.updateEffectIcons(true); // update party icons only
				summon = member.getPet();
				member.broadcastUserInfo();
				if (summon != null)
				{
					summon.updateEffectIcons();
				}
				
				member.getServitors().values().forEach(Summon::updateEffectIcons);
				
				// send hp status update
				member.sendPacket(su);
			}
		}
		
		// open the CCInformationwindow
		if (isInCommandChannel())
		{
			player.sendPacket(ExOpenMPCC.STATIC_PACKET);
		}
		
		if (_positionBroadcastTask == null)
		{
			_positionBroadcastTask = ThreadPool.scheduleAtFixedRate(() ->
			{
				if (_positionPacket == null)
				{
					_positionPacket = new PartyMemberPosition(this);
				}
				else
				{
					_positionPacket.reuse(this);
				}
				
				broadcastPacket(_positionPacket);
			}, PARTY_POSITION_BROADCAST_INTERVAL.toMillis() / 2, PARTY_POSITION_BROADCAST_INTERVAL.toMillis());
		}
		
		applyTacticalSigns(player, false);
		World.incrementPartyMember();
	}
	
	/**
	 * Retrieves the map of tactical signs for this party.<br>
	 * It ensures that the internal {@code _tacticalSigns} map is initialized before returning it.
	 * @return a {@code Map<Integer, Creature>} containing the tactical signs.
	 */
	private Map<Integer, Creature> getTacticalSigns()
	{
		if (_tacticalSigns == null)
		{
			synchronized (this)
			{
				if (_tacticalSigns == null)
				{
					_tacticalSigns = new ConcurrentHashMap<>(1);
				}
			}
		}
		
		return _tacticalSigns;
	}
	
	/**
	 * Updates the tactical signs for a specific {@code Player}.<br>
	 * This method sends packets to the player based on the current tactical signs.<br>
	 * Use {@code true} to remove signs and {@code false} to apply them.
	 * @param player The {@code Player} who will receive the update.
	 * @param remove Set to {@code true} to clear signs, or {@code false} to show them.
	 */
	public void applyTacticalSigns(Player player, boolean remove)
	{
		if (_tacticalSigns == null)
		{
			return;
		}
		
		_tacticalSigns.entrySet().forEach(entry -> player.sendPacket(new ExTacticalSign(entry.getValue(), remove ? 0 : entry.getKey())));
	}
	
	/**
	 * Assigns a tactical sign to a specific target for the player.<br>
	 * This method updates the sign mapping and notifies all party members.<br>
	 * It handles cases where signs are added, replaced, or removed.
	 * @param player The {@code Player} who is placing the tactical sign.
	 * @param tacticalSignId The unique identifier for the tactical sign.
	 * @param target The {@code Creature} that will receive the tactical sign.
	 */
	public void addTacticalSign(Player player, int tacticalSignId, Creature target)
	{
		final Creature tacticalTarget = getTacticalSigns().get(tacticalSignId);
		if (tacticalTarget == null)
		{
			// if the new sign is applied to an existing target, remove the old sign from map
			_tacticalSigns.values().remove(target);
			
			// Add the new sign.
			_tacticalSigns.put(tacticalSignId, target);
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_USED_S3_ON_C2);
			sm.addPcName(player);
			sm.addString(target.getName());
			sm.addSystemString(TACTICAL_SYS_STRINGS[tacticalSignId]);
			
			_members.forEach(m ->
			{
				m.sendPacket(new ExTacticalSign(target, tacticalSignId));
				m.sendPacket(sm);
			});
		}
		else if (tacticalTarget == target)
		{
			// If the sign is already assigned to the same target, remove it.
			_tacticalSigns.remove(tacticalSignId);
			_members.forEach(m -> m.sendPacket(new ExTacticalSign(tacticalTarget, 0)));
		}
		else
		{
			// Otherwise, delete the old sign, and apply it to the new target.
			_tacticalSigns.replace(tacticalSignId, target);
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_USED_S3_ON_C2);
			sm.addPcName(player);
			sm.addString(target.getName());
			sm.addSystemString(TACTICAL_SYS_STRINGS[tacticalSignId]);
			
			_members.forEach(m ->
			{
				m.sendPacket(new ExTacticalSign(tacticalTarget, 0));
				m.sendPacket(new ExTacticalSign(target, tacticalSignId));
				m.sendPacket(sm);
			});
		}
	}
	
	/**
	 * Sets the target for a {@code Player} based on a specific tactical sign.<br>
	 * This method checks if the sign exists and is valid before updating the player's target.<br>
	 * It ensures the target is not invisible and that the player is allowed to target it.
	 * @param player The {@code Player} who will have their target updated.
	 * @param tacticalSignId The unique identifier for the tactical sign to use as a target.
	 */
	public void setTargetBasedOnTacticalSignId(Player player, int tacticalSignId)
	{
		if (_tacticalSigns == null)
		{
			return;
		}
		
		final Creature tacticalTarget = _tacticalSigns.get(tacticalSignId);
		if ((tacticalTarget != null) && !tacticalTarget.isInvisible() && tacticalTarget.isTargetable() && !player.isTargetingDisabled())
		{
			player.setTarget(tacticalTarget);
		}
	}
	
	/**
	 * Removes a member from the party based on their name.<br>
	 * This method finds the {@code Player} by the provided {@code name}.<br>
	 * It then calls the overloaded version of this method using that player.
	 * @param name The name of the player to remove from the party.
	 * @param type The type of message to send during removal.
	 */
	public void removePartyMember(String name, PartyMessageType type)
	{
		removePartyMember(getPlayerByName(name), type);
	}
	
	/**
	 * Removes a {@code Player} from the party based on the specified {@code PartyMessageType}.<br>
	 * This method handles disbanding logic if necessary.<br>
	 * It also updates party levels and notifies other members of the change.<br>
	 * Any active skills or duel statuses for the player are handled during removal.
	 * @param player The {@code Player} to be removed from the group.
	 * @param type The reason for removal such as {@code PartyMessageType.LEFT} or {@code PartyMessageType.EXPELLED}.
	 */
	public void removePartyMember(Player player, PartyMessageType type)
	{
		if (_members.contains(player))
		{
			final boolean isLeader = isLeader(player);
			if (!_disbanding && ((_members.size() == 2) || (isLeader && !PlayerConfig.ALT_LEAVE_PARTY_LEADER && (type != PartyMessageType.DISCONNECTED))))
			{
				disbandParty();
				return;
			}
			
			_members.remove(player);
			recalculatePartyLevel();
			
			if (player.isInDuel())
			{
				DuelManager.getInstance().onRemoveFromParty(player);
			}
			
			try
			{
				// Channeling a player!
				if (player.isChanneling() && (player.getSkillChannelizer().hasChannelized()))
				{
					player.abortCast();
				}
				else if (player.isChannelized())
				{
					player.getSkillChannelized().abortChannelization();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "removePartyMember: ", e);
			}
			
			SystemMessage msg;
			if (type == PartyMessageType.EXPELLED)
			{
				player.sendPacket(SystemMessageId.YOU_ARE_DISMISSED_FROM_THE_PARTY);
				msg = new SystemMessage(SystemMessageId.C1_IS_DISMISSED_FROM_THE_PARTY);
				msg.addString(player.getAppearance().getVisibleName());
				broadcastPacket(msg);
			}
			else if ((type == PartyMessageType.LEFT) || (type == PartyMessageType.DISCONNECTED))
			{
				player.sendPacket(SystemMessageId.YOU_HAVE_LEFT_THE_PARTY);
				msg = new SystemMessage(SystemMessageId.C1_HAS_LEFT_THE_PARTY);
				msg.addString(player.getAppearance().getVisibleName());
				broadcastPacket(msg);
			}
			
			World.decrementPartyMember();
			
			// UI update.
			player.sendPacket(PartySmallWindowDeleteAll.STATIC_PACKET);
			player.setParty(null);
			broadcastPacket(new PartySmallWindowDelete(player));
			final Summon pet = player.getPet();
			if (pet != null)
			{
				broadcastPacket(new ExPartyPetWindowDelete(pet));
			}
			
			player.getServitors().values().forEach(s -> player.sendPacket(new ExPartyPetWindowDelete(s)));
			
			// Close the CCInfoWindow
			if (isInCommandChannel())
			{
				player.sendPacket(ExCloseMPCC.STATIC_PACKET);
			}
			
			if (isLeader && (_members.size() > 1) && (PlayerConfig.ALT_LEAVE_PARTY_LEADER || (type == PartyMessageType.DISCONNECTED)))
			{
				msg = new SystemMessage(SystemMessageId.C1_HAS_BECOME_THE_PARTY_LEADER);
				msg.addString(getLeader().getAppearance().getVisibleName());
				broadcastPacket(msg);
				broadcastToPartyMembersNewLeader();
				if (isInCommandChannel() && _commandChannel.isLeader(player))
				{
					_commandChannel.setLeader(getLeader());
					msg = new SystemMessage(SystemMessageId.COMMAND_CHANNEL_AUTHORITY_HAS_BEEN_TRANSFERRED_TO_C1);
					msg.addString(_commandChannel.getLeader().getAppearance().getVisibleName());
					_commandChannel.broadcastPacket(msg);
				}
			}
			else if (_members.size() == 1)
			{
				if (isInCommandChannel())
				{
					// delete the whole command channel when the party who opened the channel is disbanded
					if (_commandChannel.getLeader().getObjectId() == getLeader().getObjectId())
					{
						_commandChannel.disbandChannel();
					}
					else
					{
						_commandChannel.removeParty(this);
					}
				}
				
				final Player leader = getLeader();
				if (leader != null)
				{
					applyTacticalSigns(leader, true);
					
					leader.setParty(null);
					if (leader.isInDuel())
					{
						DuelManager.getInstance().onRemoveFromParty(leader);
					}
				}
				
				if (_changeDistributionTypeRequestTask != null)
				{
					_changeDistributionTypeRequestTask.cancel(true);
					_changeDistributionTypeRequestTask = null;
				}
				
				if (_positionBroadcastTask != null)
				{
					_positionBroadcastTask.cancel(false);
					_positionBroadcastTask = null;
				}
				
				_members.clear();
			}
			
			applyTacticalSigns(player, true);
		}
	}
	
	/**
	 * Dissolves the current party and removes all members.<br>
	 * This method sets the {@code _disbanding} flag to {@code true}.<br>
	 * It broadcasts a system message to all members.<br>
	 * Finally, it updates the global party count in {@code World}.
	 */
	public void disbandParty()
	{
		_disbanding = true;
		broadcastPacket(new SystemMessage(SystemMessageId.THE_PARTY_IS_DISBANDED));
		
		// The command channel closes when the party of its creator is disbanded.
		if (isInCommandChannel() && _commandChannel.isLeader(getLeader()))
		{
			_commandChannel.disbandChannel();
		}
		
		for (Player member : _members)
		{
			if (member != null)
			{
				removePartyMember(member, PartyMessageType.NONE);
			}
		}
		
		World.decrementParty();
	}
	
	/**
	 * Updates the current leader of the party.<br>
	 * This method finds a {@code Player} by their name and sets them as the new leader.
	 * @param name The name of the player who will become the new leader.
	 */
	public void changePartyLeader(String name)
	{
		setLeader(getPlayerByName(name));
	}
	
	/**
	 * Sets the specified {@code Player} as the new leader of the party.<br>
	 * This method checks if the player is already a member and not in a duel.<br>
	 * It swaps leadership roles between the old leader and the new leader.<br>
	 * If the player is not a member, an error message is sent to them.
	 * @param player The {@code Player} who will become the new party leader.
	 */
	@Override
	public void setLeader(Player player)
	{
		if ((player != null) && !player.isInDuel())
		{
			if (_members.contains(player))
			{
				if (isLeader(player))
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_DELEGATE_PRIVILEGES_TO_YOURSELF);
				}
				else
				{
					// Swap party members
					final Player temp = getLeader();
					final int p1 = _members.indexOf(player);
					_members.set(0, player);
					_members.set(p1, temp);
					SystemMessage msg = new SystemMessage(SystemMessageId.C1_HAS_BECOME_THE_PARTY_LEADER);
					
					final Player leader = getLeader();
					msg.addString(leader.getName());
					broadcastPacket(msg);
					broadcastToPartyMembersNewLeader();
					if (isInCommandChannel() && _commandChannel.isLeader(temp))
					{
						_commandChannel.setLeader(leader);
						msg = new SystemMessage(SystemMessageId.COMMAND_CHANNEL_AUTHORITY_HAS_BEEN_TRANSFERRED_TO_C1);
						msg.addString(_commandChannel.getLeader().getAppearance().getVisibleName());
						_commandChannel.broadcastPacket(msg);
					}
				}
			}
			else
			{
				player.sendPacket(SystemMessageId.YOU_MAY_ONLY_TRANSFER_PARTY_LEADERSHIP_TO_ANOTHER_MEMBER_OF_THE_PARTY);
			}
		}
	}
	
	/**
	 * Finds a {@code Player} within the party by their name.<br>
	 * The search is case-insensitive.<br>
	 * Returns {@code null} if no matching player is found.
	 * @param name The name of the player to search for.
	 * @return The matching {@code Player} object or {@code null}.
	 */
	public Player getPlayerByName(String name)
	{
		for (Player member : _members)
		{
			if (member.getAppearance().getVisibleName().equalsIgnoreCase(name))
			{
				return member;
			}
		}
		
		return null;
	}
	
	/**
	 * Distributes an {@code Item} to the correct player.<br>
	 * If the item is adena, it distributes the amount to the {@code Player}.<br>
	 * Otherwise, it identifies the actual looter and adds the item to their inventory.<br>
	 * It also broadcasts a message to party members when an item is obtained.
	 * @param player The {@code Player} who triggered the distribution.
	 * @param item The {@code Item} to be distributed.
	 */
	public void distributeItem(Player player, Item item)
	{
		if (item.getId() == Inventory.ADENA_ID)
		{
			distributeAdena(player, item.getCount(), player);
			ItemManager.destroyItem(ItemProcessType.LOOT, item, player, null);
			return;
		}
		
		final Player target = getActualLooter(player, item.getId(), false, player);
		target.addItem(ItemProcessType.LOOT, item, player, true);
		
		// Send messages to other party members about reward.
		if (item.getCount() > 1)
		{
			final SystemMessage msg = new SystemMessage(SystemMessageId.C1_HAS_OBTAINED_S2_X_S3);
			msg.addString(target.getAppearance().getVisibleName());
			msg.addItemName(item);
			msg.addLong(item.getCount());
			broadcastToPartyMembers(target, msg);
		}
		else if (item.getEnchantLevel() > 0)
		{
			final SystemMessage msg = new SystemMessage(SystemMessageId.C1_HAS_OBTAINED_S2_S3);
			msg.addString(target.getAppearance().getVisibleName());
			msg.addInt(item.getEnchantLevel());
			msg.addItemName(item);
			broadcastToPartyMembers(target, msg);
		}
		else
		{
			final SystemMessage msg = new SystemMessage(SystemMessageId.C1_HAS_OBTAINED_S2);
			msg.addString(target.getAppearance().getVisibleName());
			msg.addItemName(item);
			broadcastToPartyMembers(target, msg);
		}
	}
	
	/**
	 * Distributes an item to a player based on loot rules.<br>
	 * This method handles special logic for {@code Inventory.ADENA_ID}.<br>
	 * It identifies the correct looter and adds the item to their inventory.<br>
	 * A message is broadcasted to party members if the quantity is greater than 1.
	 * @param player The player who triggered the distribution action.
	 * @param itemId The unique identifier of the item to be distributed.
	 * @param itemCount The total amount of the item to distribute.
	 * @param spoil Whether the item was obtained using a sweeper.
	 * @param target The {@code Attackable} entity that was defeated.
	 */
	public void distributeItem(Player player, int itemId, long itemCount, boolean spoil, Attackable target)
	{
		if (itemId == Inventory.ADENA_ID)
		{
			distributeAdena(player, itemCount, target);
			return;
		}
		
		final Player looter = getActualLooter(player, itemId, spoil, target);
		looter.addItem(spoil ? ItemProcessType.SWEEP : ItemProcessType.LOOT, itemId, itemCount, target, true);
		
		// Send messages to other party members about reward.
		if (itemCount > 1)
		{
			final SystemMessage msg = spoil ? new SystemMessage(SystemMessageId.C1_HAS_OBTAINED_S2_X_S3_BY_USING_SWEEPER) : new SystemMessage(SystemMessageId.C1_HAS_OBTAINED_S2_X_S3);
			msg.addString(looter.getAppearance().getVisibleName());
			msg.addItemName(itemId);
			msg.addLong(itemCount);
			broadcastToPartyMembers(looter, msg);
		}
		else
		{
			final SystemMessage msg = spoil ? new SystemMessage(SystemMessageId.C1_HAS_OBTAINED_S2_BY_USING_SWEEPER) : new SystemMessage(SystemMessageId.C1_HAS_OBTAINED_S2);
			msg.addString(looter.getAppearance().getVisibleName());
			msg.addItemName(itemId);
			broadcastToPartyMembers(looter, msg);
		}
	}
	
	/**
	 * Distributes an item to the appropriate party members.<br>
	 * This method handles loot distribution based on the provided rules.
	 * @param player The {@code Player} who triggered the action.
	 * @param item The {@code ItemHolder} containing the item to be distributed.
	 * @param spoil Whether the {@code spoil} flag is active for this distribution.
	 * @param target The {@code Attackable} entity that was defeated.
	 */
	public void distributeItem(Player player, ItemHolder item, boolean spoil, Attackable target)
	{
		distributeItem(player, item.getId(), item.getCount(), spoil, target);
	}
	
	/**
	 * Distributes a specific amount of adena among party members.<br>
	 * Only members within the allowed range of the target receive rewards.<br>
	 * The total amount is split as evenly as possible among eligible players.
	 * @param player The player who initiated the distribution.
	 * @param adena The total amount of adena to be distributed.
	 * @param target The creature used to determine the reward range.
	 */
	public void distributeAdena(Player player, long adena, Creature target)
	{
		// List to collect party members eligible to receive adena.
		final List<Player> toReward = new ArrayList<>();
		
		// Iterate over all party members to find those within the allowed party range of the target.
		for (Player member : _members)
		{
			if (LocationUtil.checkIfInRange(PlayerConfig.ALT_PARTY_RANGE, target, member, true))
			{
				toReward.add(member); // Add eligible member to the reward list.
			}
		}
		
		// If no members are eligible or there is no adena to distribute, exit early.
		final int toRewardSize = toReward.size();
		if (toRewardSize == 0 || (adena <= 0))
		{
			return;
		}
		
		final long baseShare = adena / toRewardSize; // Calculate the base share of adena each eligible member will receive.
		final long remainder = adena % toRewardSize; // Calculate the leftover adena that can't be evenly split (remainder).
		final Map<Player, Long> adenaDistribution = new HashMap<>(); // Map to track how much adena each member will receive.
		
		// Assign the base share to every eligible member initially.
		for (Player member : toReward)
		{
			adenaDistribution.put(member, baseShare);
		}
		
		// Distribute the leftover adena (1 adena each) to some members starting from a random index.
		if (remainder > 0)
		{
			final int randomIndex = Rnd.get(toRewardSize);
			for (int i = 0; i < remainder; i++)
			{
				final Player member = toReward.get((randomIndex + i) % toRewardSize);
				adenaDistribution.put(member, adenaDistribution.get(member) + 1); // Increase the member's share by 1 to include remainder.
			}
		}
		
		// Finally, give each member their total calculated adena amount.
		for (Entry<Player, Long> entry : adenaDistribution.entrySet())
		{
			final long amount = entry.getValue();
			if (amount > 0)
			{
				// Add adena to the player's inventory and send message.
				entry.getKey().addAdena(ItemProcessType.LOOT, amount, player, true);
			}
		}
	}
	
	/**
	 * Distributes experience and skill points to a list of players.<br>
	 * This method calculates rewards based on the target's world and party size.<br>
	 * It applies penalties for servitors and adjusts values based on player levels.<br>
	 * Rewards are also updated in clan hunting points and PC Cafe points.
	 * @param xpRewardValue The base amount of experience to distribute.
	 * @param spRewardValue The base amount of skill points to distribute.
	 * @param rewardedMembers The list of players eligible for rewards.
	 * @param topLvl The maximum level cap for reward distribution.
	 * @param target The {@code Attackable} entity that was defeated.
	 */
	public void distributeXpAndSp(double xpRewardValue, double spRewardValue, List<Player> rewardedMembers, int topLvl, Attackable target)
	{
		final List<Player> validMembers = getValidMembers(rewardedMembers, topLvl, target);
		double xpReward = xpRewardValue * getExpBonus(validMembers.size(), target.getInstanceWorld());
		double spReward = spRewardValue * getSpBonus(validMembers.size(), target.getInstanceWorld());
		int sqLevelSum = 0;
		for (Player member : validMembers)
		{
			final int level = member.getLevel();
			sqLevelSum += (level * level);
		}
		
		for (Player member : rewardedMembers)
		{
			if (member.isDead())
			{
				continue;
			}
			
			// Calculate and add the EXP and SP reward to the member.
			if (validMembers.contains(member))
			{
				// The servitor penalty
				float penalty = 1;
				for (Summon summon : member.getServitors().values())
				{
					final float expMultiplier = summon.asServitor().getExpMultiplier();
					if (expMultiplier > 1)
					{
						penalty = expMultiplier;
						break;
					}
				}
				
				final int level = member.getLevel();
				final double sqLevel = level * level;
				final double preCalculation = (sqLevel / sqLevelSum) * penalty;
				
				// Add the XP/SP points to the requested party member.
				double exp = member.getStat().getValue(Stat.EXPSP_RATE, xpReward * preCalculation);
				final double sp = member.getStat().getValue(Stat.EXPSP_RATE, spReward * preCalculation);
				exp = calculateExpSpPartyCutoff(member.asPlayer(), topLvl, exp, sp, target.useVitalityRate());
				if (exp > 0)
				{
					member.updateVitalityPoints(target.getVitalityPoints(member.getLevel(), exp, target.isRaid()), true, false);
					PcCafePointsManager.getInstance().givePcCafePoint(member, exp);
					if (member.getSymbolSealPoints() > 0)
					{
						member.setSymbolSealPoints(member.getSymbolSealPoints() - PlayerConfig.CONSUME_SYMBOL_SEAL_POINTS);
						if (member.getSymbolSealPoints() == 0)
						{
							member.removeSymbolSealSkills();
						}
					}
					
					final HuntPass huntpass = member.getHuntPass();
					if (huntpass != null)
					{
						huntpass.addPassPoint();
					}
				}
			}
			else
			{
				member.addExpAndSp(0, 0);
			}
		}
	}
	
	/**
	 * Calculates and applies experience and skill point rewards for a party member.<br>
	 * It adjusts the values based on class multipliers and premium status.<br>
	 * The method also handles level difference penalties if configured.
	 * @param player The {@code Player} receiving the rewards.
	 * @param topLvl The highest level among all party members.
	 * @param addExpValue The base experience amount to be added.
	 * @param addSpValue The base skill point amount to be added.
	 * @param vit Whether the reward should be distributed based on vitality.
	 * @return The final amount of experience awarded to the player.
	 */
	private double calculateExpSpPartyCutoff(Player player, int topLvl, double addExpValue, double addSpValue, boolean vit)
	{
		final int playerClassId = player.getPlayerClass().getId();
		double addExp = addExpValue * ClassBalanceConfig.EXP_AMOUNT_MULTIPLIERS[playerClassId];
		double addSp = addSpValue * ClassBalanceConfig.SP_AMOUNT_MULTIPLIERS[playerClassId];
		
		// Premium rates
		if (player.hasPremiumStatus())
		{
			addExp *= PremiumSystemConfig.PREMIUM_RATE_XP;
			addSp *= PremiumSystemConfig.PREMIUM_RATE_SP;
		}
		
		double xp = addExp;
		double sp = addSp;
		if (PlayerConfig.PARTY_XP_CUTOFF_METHOD == PartyExpType.HIGHFIVE)
		{
			int i = 0;
			final int levelDiff = topLvl - player.getLevel();
			for (int[] gap : PlayerConfig.PARTY_XP_CUTOFF_GAPS)
			{
				if ((levelDiff >= gap[0]) && (levelDiff <= gap[1]))
				{
					xp = (addExp * PlayerConfig.PARTY_XP_CUTOFF_GAP_PERCENTS[i]) / 100;
					sp = (addSp * PlayerConfig.PARTY_XP_CUTOFF_GAP_PERCENTS[i]) / 100;
					player.addExpAndSp(xp, sp, vit);
					break;
				}
				
				i++;
			}
		}
		else
		{
			player.addExpAndSp(addExp, addSp, vit);
		}
		
		return xp;
	}
	
	/**
	 * Updates the party level based on its members.<br>
	 * This method finds the highest level among all current {@code Player} objects in the group.<br>
	 * The result is stored in the {@code _partyLvl} field.
	 */
	public void recalculatePartyLevel()
	{
		int newLevel = 0;
		for (Player member : _members)
		{
			if (member == null)
			{
				_members.remove(member);
				continue;
			}
			
			final int memberLevel = member.getLevel();
			if (memberLevel > newLevel)
			{
				newLevel = memberLevel;
			}
		}
		
		_partyLvl = newLevel;
	}
	
	/**
	 * Filters a list of players to find those eligible for experience rewards.<br>
	 * This method checks members against the target based on configured XP cutoff rules.<br>
	 * It supports various methods like level differences, percentages, and automatic scaling.
	 * @param members The list of {@code Player} objects to evaluate.
	 * @param topLvl The highest level among the group for comparison.
	 * @param target The {@code Attackable} entity being targeted by the players.
	 * @return A list of {@code Player} objects that meet the criteria.
	 */
	private List<Player> getValidMembers(List<Player> members, int topLvl, Attackable target)
	{
		final List<Player> validMembers = new ArrayList<>();
		switch (PlayerConfig.PARTY_XP_CUTOFF_METHOD)
		{
			case LEVEL:
			{
				for (Player member : members)
				{
					if ((target.getInstanceId() == member.getInstanceId()) && ((topLvl - member.getLevel()) <= PlayerConfig.PARTY_XP_CUTOFF_LEVEL))
					{
						validMembers.add(member);
					}
				}
				break;
			}
			case PERCENTAGE:
			{
				int sqLevelSum = 0;
				for (Player member : members)
				{
					if (target.getInstanceId() == member.getInstanceId())
					{
						sqLevelSum += (member.getLevel() * member.getLevel());
					}
				}
				
				for (Player member : members)
				{
					final int sqLevel = member.getLevel() * member.getLevel();
					if ((target.getInstanceId() == member.getInstanceId()) && ((sqLevel * 100) >= (sqLevelSum * PlayerConfig.PARTY_XP_CUTOFF_PERCENT)))
					{
						validMembers.add(member);
					}
				}
				break;
			}
			case AUTO:
			{
				int sqLevelSum = 0;
				for (Player member : members)
				{
					sqLevelSum += (member.getLevel() * member.getLevel());
				}
				
				final int listSize = members.size();
				int i = listSize - 1;
				if (i < 1)
				{
					return members;
				}
				
				if (i >= BONUS_EXP_SP.length)
				{
					i = BONUS_EXP_SP.length - 1;
				}
				
				for (Player member : members)
				{
					final int sqLevel = member.getLevel() * member.getLevel();
					if ((target.getInstanceId() == member.getInstanceId()) && (sqLevel >= (sqLevelSum / (listSize * listSize))))
					{
						validMembers.add(member);
					}
				}
				break;
			}
			case HIGHFIVE:
			{
				for (Player member : members)
				{
					if (target.getInstanceId() == member.getInstanceId())
					{
						validMembers.add(member);
					}
				}
				break;
			}
			case NONE:
			{
				for (Player member : members)
				{
					if (target.getInstanceId() == member.getInstanceId())
					{
						validMembers.add(member);
					}
				}
				break;
			}
		}
		
		return validMembers;
	}
	
	/**
	 * Calculates the experience point bonus based on the number of party members.<br>
	 * It returns a multiplier from the {@code BONUS_EXP_SP} array.<br>
	 * If there is only one member, it returns 1.<br>
	 * If the count exceeds the maximum index, it uses the last available bonus.
	 * @param membersCount The total number of members in the party.
	 * @return The experience point multiplier as a {@code double}.
	 */
	private double getBaseExpSpBonus(int membersCount)
	{
		int i = membersCount - 1;
		if (i < 1)
		{
			return 1;
		}
		
		if (i >= BONUS_EXP_SP.length)
		{
			i = BONUS_EXP_SP.length - 1;
		}
		
		return BONUS_EXP_SP[i];
	}
	
	/**
	 * Calculates the experience bonus for a group of members.<br>
	 * It checks if the {@code Instance} provides a specific party rate.<br>
	 * If no instance exists, it uses the default rate from {@code RatesConfig}.<br>
	 * The result depends on whether there are at least 2 members.
	 * @param membersCount The number of members in the group.
	 * @param instance The current {@code Instance} object.
	 * @return The calculated experience bonus as a {@code double}.
	 */
	private double getExpBonus(int membersCount, Instance instance)
	{
		final float rateMul = instance != null ? instance.getExpPartyRate() : RatesConfig.RATE_PARTY_XP;
		return (membersCount < 2) ? (getBaseExpSpBonus(membersCount)) : (getBaseExpSpBonus(membersCount) * rateMul);
	}
	
	/**
	 * Calculates the SP bonus for a group of members.<br>
	 * It checks if the {@code instance} is null to determine the multiplier.<br>
	 * The result depends on whether the {@code membersCount} is less than 2.
	 * @param membersCount The number of members in the group.
	 * @param instance The current {@code Instance} object.
	 * @return The calculated SP bonus as a {@code double}.
	 */
	private double getSpBonus(int membersCount, Instance instance)
	{
		final float rateMul = instance != null ? instance.getSPPartyRate() : RatesConfig.RATE_PARTY_SP;
		return (membersCount < 2) ? (getBaseExpSpBonus(membersCount)) : (getBaseExpSpBonus(membersCount) * rateMul);
	}
	
	/**
	 * Retrieves the current level of the party.
	 * @return The integer value representing the party level.
	 */
	@Override
	public int getLevel()
	{
		return _partyLvl;
	}
	
	/**
	 * Retrieves the current distribution type of the party.<br>
	 * This determines how loot or other rewards are shared among members.
	 * @return the {@code PartyDistributionType} of this party.
	 */
	public PartyDistributionType getDistributionType()
	{
		return _distributionType;
	}
	
	/**
	 * Checks if the current entity is associated with a command channel.<br>
	 * It returns {@code true} if the internal command channel is not {@code null}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if in a command channel, {@code false} otherwise.
	 */
	public boolean isInCommandChannel()
	{
		return _commandChannel != null;
	}
	
	/**
	 * Retrieves the current {@code CommandChannel}.
	 * @return The {@code CommandChannel} associated with this object.
	 */
	public CommandChannel getCommandChannel()
	{
		return _commandChannel;
	}
	
	/**
	 * Sets the command channel for this object.<br>
	 * This defines which channel is used to receive commands.
	 * @param channel The {@code CommandChannel} to be assigned.
	 */
	public void setCommandChannel(CommandChannel channel)
	{
		_commandChannel = channel;
	}
	
	/**
	 * Retrieves the leader of the party.<br>
	 * This method returns the first member in the internal list.<br>
	 * It returns {@code null} if there are no members or an error occurs.
	 * @return the {@code Player} who is the leader, or {@code null} if none exists.
	 */
	@Override
	public Player getLeader()
	{
		if (_members.isEmpty())
		{
			return null;
		}
		
		final Player leader;
		try
		{
			leader = _members.get(0);
		}
		catch (Exception e)
		{
			return null;
		}
		
		return leader;
	}
	
	/**
	 * Retrieves the current state of the UC system.
	 * @return The current {@code Object} representing the UC state.
	 */
	public Object getUCState()
	{
		return _ucState;
	}
	
	/**
	 * Updates the current state of the unit control.<br>
	 * This method sets the {@code _ucState} field to the provided value.
	 * @param uc The new state to set for the unit control.
	 */
	public void setUCState(Object uc)
	{
		_ucState = uc;
	}
	
	/**
	 * Requests a change to the party loot distribution type.<br>
	 * This method initiates a request and broadcasts it to all members.<br>
	 * It also schedules a task to complete the request if no response is received.
	 * @param partyDistributionType The new {@code PartyDistributionType} to request.
	 */
	public synchronized void requestLootChange(PartyDistributionType partyDistributionType)
	{
		if (_changeRequestDistributionType != null)
		{
			return;
		}
		
		_changeRequestDistributionType = partyDistributionType;
		_changeDistributionTypeAnswers = new HashSet<>();
		_changeDistributionTypeRequestTask = ThreadPool.schedule(() -> finishLootRequest(false), PARTY_DISTRIBUTION_TYPE_REQUEST_TIMEOUT.toMillis());
		broadcastToPartyMembers(getLeader(), new ExAskModifyPartyLooting(getLeader().getAppearance().getVisibleName(), partyDistributionType));
		
		final SystemMessage sm = new SystemMessage(SystemMessageId.REQUESTING_APPROVAL_FOR_CHANGING_PARTY_LOOT_TO_S1);
		sm.addSystemString(partyDistributionType.getSysStringId());
		getLeader().sendPacket(sm);
	}
	
	/**
	 * Processes a member's response to a loot distribution change request.<br>
	 * If the answer is {@code false}, it cancels the current request.<br>
	 * If enough members agree, it completes the request successfully.
	 * @param member The {@code Player} who is responding to the request.
	 * @param answer The response provided by the player.
	 */
	public synchronized void answerLootChangeRequest(Player member, boolean answer)
	{
		if (_changeRequestDistributionType == null)
		{
			return;
		}
		
		final int objectId = member.getObjectId();
		if (_changeDistributionTypeAnswers.contains(objectId))
		{
			return;
		}
		
		if (!answer)
		{
			finishLootRequest(false);
			return;
		}
		
		_changeDistributionTypeAnswers.add(objectId);
		if (_changeDistributionTypeAnswers.size() >= (getMemberCount() - 1))
		{
			finishLootRequest(true);
		}
	}
	
	/**
	 * Finalizes a request to change the party loot distribution type.<br>
	 * This method handles the results of an asynchronous task.<br>
	 * If successful, it updates the distribution type and notifies all members.<br>
	 * It also clears any pending request data regardless of the outcome.
	 * @param success Indicates whether the distribution change was successful.
	 */
	protected synchronized void finishLootRequest(boolean success)
	{
		if (_changeRequestDistributionType == null)
		{
			return;
		}
		
		if (_changeDistributionTypeRequestTask != null)
		{
			_changeDistributionTypeRequestTask.cancel(false);
			_changeDistributionTypeRequestTask = null;
		}
		
		if (success)
		{
			broadcastPacket(new ExSetPartyLooting(1, _changeRequestDistributionType));
			_distributionType = _changeRequestDistributionType;
			final SystemMessage sm = new SystemMessage(SystemMessageId.PARTY_LOOTING_METHOD_WAS_CHANGED_TO_S1);
			sm.addSystemString(_changeRequestDistributionType.getSysStringId());
			broadcastPacket(sm);
		}
		else
		{
			broadcastPacket(new ExSetPartyLooting(0, _distributionType));
			broadcastPacket(new SystemMessage(SystemMessageId.PARTY_LOOT_CHANGE_WAS_CANCELLED));
		}
		
		_changeRequestDistributionType = null;
		_changeDistributionTypeAnswers = null;
	}
	
	/**
	 * Retrieves all members currently in the party.
	 * @return a {@code List} of {@code Player} objects.
	 */
	@Override
	public List<Player> getMembers()
	{
		return _members;
	}
}
