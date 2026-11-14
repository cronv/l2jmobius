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
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExDuelAskStart;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This packet is sent by the client to request the start of a duel.<br>
 * It handles the initial request from a {@code Player} to begin a combat encounter.
 * @author -Wooden-, BazookaRpm
 */
public class RequestDuelStart extends ClientPacket
{
	private String _player;
	private int _partyDuel;
	
	@Override
	protected void readImpl()
	{
		_player = readString();
		_partyDuel = readInt();
	}
	
	/**
	 * Sends a notification to a player that their duel request was declined.<br>
	 * This method handles the response for a declined challenge.
	 * @param player The {@code Player} who sent the initial request.
	 * @param name The name of the player who declined the duel.
	 */
	private void scheduleDeny(Player player, String name)
	{
		final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_DECLINED_YOUR_CHALLENGE_TO_A_DUEL);
		sm.addString(name);
		player.sendPacket(sm);
		player.onTransactionResponse();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (FakePlayerData.getInstance().isTalkable(_player))
		{
			final String name = FakePlayerData.getInstance().getProperName(_player);
			if (player.isInsideZone(ZoneId.PVP) || player.isInsideZone(ZoneId.PEACE) || player.isInsideZone(ZoneId.SIEGE))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_IN_AN_AREA_WHERE_DUEL_IS_NOT_ALLOWED_AND_YOU_CANNOT_APPLY_FOR_A_DUEL);
				sm.addString(name);
				player.sendPacket(sm);
				return;
			}
			
			final boolean npcInRange = World.getFirstVisibleObjectInRange(player, Npc.class, 250, npc -> npc.getName().equals(name)) != null;
			if (!npcInRange)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_TOO_FAR_AWAY_TO_RECEIVE_A_DUEL_CHALLENGE);
				sm.addString(name);
				player.sendPacket(sm);
				return;
			}
			
			if (player.isProcessingRequest())
			{
				final SystemMessage msg = new SystemMessage(SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER);
				msg.addString(name);
				player.sendPacket(msg);
				return;
			}
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_BEEN_CHALLENGED_TO_A_DUEL);
			sm.addString(name);
			player.sendPacket(sm);
			ThreadPool.schedule(() -> scheduleDeny(player, name), 10000);
			player.blockRequest();
			return;
		}
		
		final Player targetChar = World.getPlayer(_player);
		if (targetChar == null)
		{
			player.sendPacket(SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL);
			return;
		}
		
		if (player == targetChar)
		{
			player.sendPacket(SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL);
			return;
		}
		
		// Check if duel is possible.
		if (!player.canDuel())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_UNABLE_TO_REQUEST_A_DUEL_AT_THIS_TIME);
			return;
		}
		else if (!targetChar.canDuel())
		{
			player.sendPacket(targetChar.getNoDuelReason());
			return;
		}
		// Players may not be too far apart.
		else if (!player.isInsideRadius2D(targetChar, 250))
		{
			final SystemMessage msg = new SystemMessage(SystemMessageId.C1_IS_TOO_FAR_AWAY_TO_RECEIVE_A_DUEL_CHALLENGE);
			msg.addString(targetChar.getName());
			player.sendPacket(msg);
			return;
		}
		
		// Duel is a party duel.
		if (_partyDuel == 1)
		{
			// Player must be in a party & the party leader.
			final Party party = player.getParty();
			if ((party == null) || !party.isLeader(player))
			{
				player.sendMessage("You have to be the leader of a party in order to request a party duel.");
				return;
			}
			// Target must be in a party.
			else if (!targetChar.isInParty())
			{
				player.sendPacket(SystemMessageId.SINCE_THE_PERSON_YOU_CHALLENGED_IS_NOT_CURRENTLY_IN_A_PARTY_THEY_CANNOT_DUEL_AGAINST_YOUR_PARTY);
				return;
			}
			// Target may not be of the same party.
			else if (party.containsPlayer(targetChar))
			{
				player.sendMessage("This player is a member of your own party.");
				return;
			}
			
			// Check if every player is ready for a duel.
			for (Player temp : party.getMembers())
			{
				if (!temp.canDuel())
				{
					player.sendMessage("Not all the members of your party are ready for a duel.");
					return;
				}
			}
			
			final Party targetParty = targetChar.getParty();
			for (Player temp : targetParty.getMembers())
			{
				if (!temp.canDuel())
				{
					player.sendPacket(SystemMessageId.THE_OPPOSING_PARTY_IS_CURRENTLY_UNABLE_TO_ACCEPT_A_CHALLENGE_TO_A_DUEL);
					return;
				}
			}
			
			// Send request to targetChar's party leader.
			final Player partyLeader = targetParty.getLeader();
			if (partyLeader != null)
			{
				if (!partyLeader.isProcessingRequest())
				{
					player.onTransactionRequest(partyLeader);
					final String playerName = player.getName();
					partyLeader.sendPacket(new ExDuelAskStart(playerName, _partyDuel));
					SystemMessage msg = new SystemMessage(SystemMessageId.C1_S_PARTY_HAS_BEEN_CHALLENGED_TO_A_DUEL);
					msg.addString(partyLeader.getName());
					player.sendPacket(msg);
					
					msg = new SystemMessage(SystemMessageId.C1_S_PARTY_HAS_CHALLENGED_YOUR_PARTY_TO_A_DUEL);
					msg.addString(playerName);
					targetChar.sendPacket(msg);
				}
				else
				{
					final SystemMessage msg = new SystemMessage(SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER);
					msg.addString(partyLeader.getName());
					player.sendPacket(msg);
				}
			}
		}
		else // 1vs1 duel
		{
			if (!targetChar.isProcessingRequest())
			{
				player.onTransactionRequest(targetChar);
				final String playerName = player.getName();
				targetChar.sendPacket(new ExDuelAskStart(playerName, _partyDuel));
				SystemMessage msg = new SystemMessage(SystemMessageId.C1_HAS_BEEN_CHALLENGED_TO_A_DUEL);
				msg.addString(targetChar.getName());
				player.sendPacket(msg);
				
				msg = new SystemMessage(SystemMessageId.C1_HAS_CHALLENGED_YOU_TO_A_DUEL);
				msg.addString(playerName);
				targetChar.sendPacket(msg);
			}
			else
			{
				final SystemMessage msg = new SystemMessage(SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER);
				msg.addString(targetChar.getName());
				player.sendPacket(msg);
			}
		}
	}
}
