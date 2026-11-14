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

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.CommandChannel;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Handles the client request to accept joining a specific {@code CommandChannel}.<br>
 * This packet is used when a player confirms their entry into a group or channel.
 * @author -Wooden-, Mobius
 */
public class RequestExAcceptJoinMPCC extends ClientPacket
{
	private static final int STRATEGY_GUIDE = 8871;
	private static final int CLAN_IMPERIUM = 391;
	
	private int _response;
	
	@Override
	protected void readImpl()
	{
		_response = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player != null)
		{
			final Player requestor = player.getActiveRequester();
			SystemMessage sm;
			if (requestor == null)
			{
				return;
			}
			
			if (_response == 1)
			{
				boolean newCc = false;
				if (!requestor.getParty().isInCommandChannel())
				{
					new CommandChannel(requestor); // Create new CC
					sm = new SystemMessage(SystemMessageId.THE_COMMAND_CHANNEL_HAS_BEEN_FORMED);
					requestor.sendPacket(sm);
					newCc = true;
					
					// A Strategy Guide is used up when it was the only reason the leader could form the channel.
					final boolean isClanLeaderRight = requestor.isClanLeader() && (requestor.getClan().getLevel() >= 5);
					final boolean isImperiumRight = (requestor.getPledgeClass() >= 5) && (requestor.getKnownSkill(CLAN_IMPERIUM) != null);
					if (!isClanLeaderRight && !isImperiumRight)
					{
						requestor.destroyItemByItemId(ItemProcessType.DESTROY, STRATEGY_GUIDE, 1, requestor, true);
					}
				}
				
				requestor.getParty().getCommandChannel().addParty(player.getParty());
				if (!newCc)
				{
					sm = new SystemMessage(SystemMessageId.YOU_HAVE_JOINED_THE_COMMAND_CHANNEL);
					player.sendPacket(sm);
				}
			}
			else
			{
				requestor.sendMessage("The player declined to join your Command Channel.");
			}
			
			player.setActiveRequester(null);
			requestor.onTransactionResponse();
		}
	}
}
