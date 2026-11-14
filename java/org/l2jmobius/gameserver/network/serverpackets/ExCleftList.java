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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.AerialCleftConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.cleft.AerialCleftTeam;
import org.l2jmobius.gameserver.mechanics.cleft.CleftListType;
import org.l2jmobius.gameserver.mechanics.cleft.CleftPlayer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet handles the registration window of the Aerial Cleft.<br>
 * It sends the waiting list of both teams and every change made to it.
 * @author Altur
 */
public class ExCleftList extends ServerPacket
{
	private final CleftListType _type;
	private AerialCleftTeam _blueTeam;
	private AerialCleftTeam _redTeam;
	private Player _player;
	private int _objectId;
	private int _teamId;
	private int _oldTeamId;
	
	/**
	 * Creates a packet that closes the registration window.
	 * @param type The {@code CleftListType.CLOSE} operation.
	 */
	public ExCleftList(CleftListType type)
	{
		_type = type;
	}
	
	/**
	 * Creates a packet that sends the whole waiting list.
	 * @param type The {@code CleftListType.TOTAL} operation.
	 * @param blueTeam The blue team.
	 * @param redTeam The red team.
	 */
	public ExCleftList(CleftListType type, AerialCleftTeam blueTeam, AerialCleftTeam redTeam)
	{
		_type = type;
		_blueTeam = blueTeam;
		_redTeam = redTeam;
	}
	
	/**
	 * Creates a packet that adds a player to the waiting list.
	 * @param type The {@code CleftListType.ADD} operation.
	 * @param player The registered player.
	 * @param teamId The team the player joined.
	 */
	public ExCleftList(CleftListType type, Player player, int teamId)
	{
		_type = type;
		_player = player;
		_teamId = teamId;
	}
	
	/**
	 * Creates a packet that removes a player from the waiting list.
	 * @param type The {@code CleftListType.REMOVE} operation.
	 * @param objectId The object id of the player that left.
	 * @param teamId The team the player was registered in.
	 */
	public ExCleftList(CleftListType type, int objectId, int teamId)
	{
		_type = type;
		_objectId = objectId;
		_teamId = teamId;
	}
	
	/**
	 * Creates a packet that moves a player from one team to the other.
	 * @param type The {@code CleftListType.TEAM_CHANGE} operation.
	 * @param objectId The object id of the moved player.
	 * @param oldTeamId The team the player is leaving.
	 * @param newTeamId The team the player is joining.
	 */
	public ExCleftList(CleftListType type, int objectId, int oldTeamId, int newTeamId)
	{
		_type = type;
		_objectId = objectId;
		_oldTeamId = oldTeamId;
		_teamId = newTeamId;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_CLEFT_LIST.writeId(this, buffer);
		buffer.writeInt(_type.getType());
		switch (_type)
		{
			case TOTAL:
			{
				buffer.writeInt(AerialCleftConfig.CLEFT_MIN_TEAM_PLAYERS);
				buffer.writeInt(0xffffffff);
				buffer.writeInt(_blueTeam.getPlayerCount());
				for (CleftPlayer cleftPlayer : _blueTeam.getPlayers())
				{
					buffer.writeInt(cleftPlayer.getObjectId());
					buffer.writeString(cleftPlayer.getPlayer().getName());
				}
				
				buffer.writeInt(_redTeam.getPlayerCount());
				for (CleftPlayer cleftPlayer : _redTeam.getPlayers())
				{
					buffer.writeInt(cleftPlayer.getObjectId());
					buffer.writeString(cleftPlayer.getPlayer().getName());
				}
				break;
			}
			case ADD:
			{
				buffer.writeInt(_teamId);
				buffer.writeInt(_player.getObjectId());
				buffer.writeString(_player.getName());
				break;
			}
			case REMOVE:
			{
				buffer.writeInt(_teamId);
				buffer.writeInt(_objectId);
				break;
			}
			case TEAM_CHANGE:
			{
				buffer.writeInt(_objectId);
				buffer.writeInt(_oldTeamId);
				buffer.writeInt(_teamId);
				break;
			}
			default:
			{
				break;
			}
		}
	}
}
