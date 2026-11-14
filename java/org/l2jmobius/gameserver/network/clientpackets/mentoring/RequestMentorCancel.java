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
package org.l2jmobius.gameserver.network.clientpackets.mentoring;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.sql.PlayerInfoTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.Mentee;
import org.l2jmobius.gameserver.managers.MentorManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerMenteeLeft;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerMenteeRemove;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Handles the request from a player to cancel their current mentoring status.<br>
 * This packet notifies the server to remove the {@code Mentee} relationship.<br>
 * It triggers the necessary cleanup and events for both the mentor and the mentee.
 * @author UnAfraid
 */
public class RequestMentorCancel extends ClientPacket
{
	private int _confirmed;
	private String _name;
	
	@Override
	protected void readImpl()
	{
		_confirmed = readInt();
		_name = readString();
	}
	
	@Override
	protected void runImpl()
	{
		if (_confirmed != 1)
		{
			return;
		}
		
		final Player player = getPlayer();
		final int objectId = PlayerInfoTable.getInstance().getIdByName(_name);
		if (player != null)
		{
			if (player.isMentor())
			{
				final int playerObjectId = player.getObjectId();
				final Mentee mentee = MentorManager.getInstance().getMentee(playerObjectId, objectId);
				if (mentee != null)
				{
					MentorManager.getInstance().cancelAllMentoringBuffs(mentee.getPlayer());
					
					if (MentorManager.getInstance().isAllMenteesOffline(playerObjectId, mentee.getObjectId()))
					{
						MentorManager.getInstance().cancelAllMentoringBuffs(player);
					}
					
					player.sendPacket(new SystemMessage(SystemMessageId.S1_S_MENTORING_CONTRACT_IS_CANCELLED_THE_MENTOR_CANNOT_BOND_WITH_ANOTHER_MENTEE_FOR_2_DAYS).addString(_name));
					MentorManager.getInstance().setPenalty(playerObjectId, PlayerConfig.MENTOR_PENALTY_FOR_MENTEE_LEAVE);
					MentorManager.getInstance().deleteMentor(playerObjectId, mentee.getObjectId());
					
					// Notify to scripts
					if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_MENTEE_REMOVE, player))
					{
						EventDispatcher.getInstance().notifyEventAsync(new OnPlayerMenteeRemove(player, mentee), player);
					}
				}
			}
			else if (player.isMentee())
			{
				final int playerObjectId = player.getObjectId();
				final Mentee mentor = MentorManager.getInstance().getMentor(playerObjectId);
				if ((mentor != null) && (mentor.getObjectId() == objectId))
				{
					MentorManager.getInstance().cancelAllMentoringBuffs(player);
					
					if (MentorManager.getInstance().isAllMenteesOffline(mentor.getObjectId(), playerObjectId))
					{
						MentorManager.getInstance().cancelAllMentoringBuffs(mentor.getPlayer());
					}
					
					MentorManager.getInstance().setPenalty(mentor.getObjectId(), PlayerConfig.MENTOR_PENALTY_FOR_MENTEE_LEAVE);
					MentorManager.getInstance().deleteMentor(mentor.getObjectId(), playerObjectId);
					
					// Notify to scripts
					if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_MENTEE_LEFT, player))
					{
						EventDispatcher.getInstance().notifyEventAsync(new OnPlayerMenteeLeft(mentor, player), player);
					}
					
					mentor.getPlayer().sendPacket(new SystemMessage(SystemMessageId.S1_S_MENTORING_CONTRACT_IS_CANCELLED_THE_MENTOR_CANNOT_BOND_WITH_ANOTHER_MENTEE_FOR_2_DAYS).addString(_name));
				}
			}
		}
	}
}
