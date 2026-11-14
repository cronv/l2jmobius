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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExRotation;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Handles the client response for a couple action request.<br>
 * This packet processes the interaction between two players when they perform a social action together.
 * @author JIV
 */
public class AnswerCoupleAction extends ClientPacket
{
	private int _objectId;
	private int _actionId;
	private int _answer;
	
	@Override
	protected void readImpl()
	{
		_actionId = readInt();
		_answer = readInt();
		_objectId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		final Player target = World.getPlayer(_objectId);
		if ((player == null) || (target == null))
		{
			return;
		}
		
		final int objectId = player.getObjectId();
		if ((target.getMultiSocialTarget() != objectId) || (target.getMultiSociaAction() != _actionId))
		{
			return;
		}
		
		switch (_answer)
		{
			case 0: // Cancel.
			{
				target.sendPacket(SystemMessageId.THE_COUPLE_ACTION_REQUEST_HAS_BEEN_DENIED);
				break;
			}
			case 1: // Approve.
			{
				final int distance = (int) player.calculateDistance2D(target);
				final int targetObjectId = target.getObjectId();
				if ((distance > 125) || (distance < 15) || (objectId == targetObjectId))
				{
					player.sendPacket(SystemMessageId.THE_REQUEST_CANNOT_BE_COMPLETED_BECAUSE_THE_TARGET_DOES_NOT_MEET_LOCATION_REQUIREMENTS);
					target.sendPacket(SystemMessageId.THE_REQUEST_CANNOT_BE_COMPLETED_BECAUSE_THE_TARGET_DOES_NOT_MEET_LOCATION_REQUIREMENTS);
					return;
				}
				
				int heading = LocationUtil.calculateHeadingFrom(player, target);
				player.broadcastPacket(new ExRotation(objectId, heading));
				player.setHeading(heading);
				heading = LocationUtil.calculateHeadingFrom(target, player);
				target.setHeading(heading);
				target.broadcastPacket(new ExRotation(targetObjectId, heading));
				player.broadcastPacket(new SocialAction(objectId, _actionId));
				target.broadcastPacket(new SocialAction(_objectId, _actionId));
				break;
			}
			case -1: // Refused.
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_SET_TO_REFUSE_COUPLE_ACTIONS_AND_CANNOT_BE_REQUESTED_FOR_A_COUPLE_ACTION);
				sm.addPcName(player);
				target.sendPacket(sm);
				break;
			}
		}
		
		target.setMultiSocialAction(0, 0);
	}
}
