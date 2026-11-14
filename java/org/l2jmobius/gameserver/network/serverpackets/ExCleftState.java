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
import org.l2jmobius.gameserver.mechanics.cleft.AerialCleftTeam;
import org.l2jmobius.gameserver.mechanics.cleft.CleftPlayer;
import org.l2jmobius.gameserver.mechanics.cleft.CleftStateType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet handles the scoreboard of the Aerial Cleft.<br>
 * It sends the remaining time, the score of both teams and every event that changes them.
 * @author Altur
 */
public class ExCleftState extends ServerPacket
{
	private final CleftStateType _type;
	private int _remainingTime;
	private AerialCleftTeam _blueTeam;
	private AerialCleftTeam _redTeam;
	private AerialCleftTeam _targetTeam;
	private CleftPlayer _killer;
	private CleftPlayer _killed;
	private int _killerTeamId;
	private int _killedTeamId;
	private int _compressorId;
	
	/**
	 * Creates a packet that sends the whole scoreboard.
	 * @param type The {@code CleftStateType.TOTAL} update.
	 * @param remainingTime The remaining battle time in seconds.
	 * @param blueTeam The blue team.
	 * @param redTeam The red team.
	 */
	public ExCleftState(CleftStateType type, int remainingTime, AerialCleftTeam blueTeam, AerialCleftTeam redTeam)
	{
		_type = type;
		_remainingTime = remainingTime;
		_blueTeam = blueTeam;
		_redTeam = redTeam;
	}
	
	/**
	 * Creates a packet that announces a destroyed compressor.
	 * @param type The {@code CleftStateType.TOWER_DESTROY} update.
	 * @param remainingTime The remaining battle time in seconds.
	 * @param blueTeam The blue team.
	 * @param redTeam The red team.
	 * @param targetTeam The team that destroyed the compressor.
	 * @param compressorId The npc id of the destroyed compressor.
	 * @param killer The record of the player that destroyed it.
	 */
	public ExCleftState(CleftStateType type, int remainingTime, AerialCleftTeam blueTeam, AerialCleftTeam redTeam, AerialCleftTeam targetTeam, int compressorId, CleftPlayer killer)
	{
		_type = type;
		_remainingTime = remainingTime;
		_blueTeam = blueTeam;
		_redTeam = redTeam;
		_targetTeam = targetTeam;
		_compressorId = compressorId;
		_killer = killer;
	}
	
	/**
	 * Creates a packet that announces the new Combat Aerial Target of a team.
	 * @param type The {@code CleftStateType.CAT_UPDATE} update.
	 * @param remainingTime The remaining battle time in seconds.
	 * @param targetTeam The team that picked a new target.
	 */
	public ExCleftState(CleftStateType type, int remainingTime, AerialCleftTeam targetTeam)
	{
		_type = type;
		_remainingTime = remainingTime;
		_targetTeam = targetTeam;
	}
	
	/**
	 * Creates a packet that announces a kill.
	 * @param type The {@code CleftStateType.PVP_KILL} update.
	 * @param remainingTime The remaining battle time in seconds.
	 * @param blueTeam The blue team.
	 * @param redTeam The red team.
	 * @param killer The record of the killer.
	 * @param killerTeamId The team of the killer.
	 * @param killed The record of the victim.
	 * @param killedTeamId The team of the victim.
	 */
	public ExCleftState(CleftStateType type, int remainingTime, AerialCleftTeam blueTeam, AerialCleftTeam redTeam, CleftPlayer killer, int killerTeamId, CleftPlayer killed, int killedTeamId)
	{
		_type = type;
		_remainingTime = remainingTime;
		_blueTeam = blueTeam;
		_redTeam = redTeam;
		_killer = killer;
		_killerTeamId = killerTeamId;
		_killed = killed;
		_killedTeamId = killedTeamId;
	}
	
	/**
	 * Creates a packet that announces the result of the battle.
	 * @param type The {@code CleftStateType.RESULT} update.
	 * @param winnerTeam The team that won.
	 * @param loserTeam The team that lost.
	 */
	public ExCleftState(CleftStateType type, AerialCleftTeam winnerTeam, AerialCleftTeam loserTeam)
	{
		_type = type;
		_blueTeam = winnerTeam;
		_redTeam = loserTeam;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_CLEFT_STATE.writeId(this, buffer);
		buffer.writeInt(_type.getType());
		switch (_type)
		{
			case TOTAL:
			{
				buffer.writeInt(_remainingTime);
				buffer.writeInt(_blueTeam.getPoints());
				buffer.writeInt(_redTeam.getPoints());
				writeCombatAerialTargetId(buffer, _blueTeam);
				writeCombatAerialTargetId(buffer, _redTeam);
				writeCombatAerialTargetName(buffer, _blueTeam);
				writeCombatAerialTargetName(buffer, _redTeam);
				writeTeamRecords(buffer, _blueTeam);
				writeTeamRecords(buffer, _redTeam);
				break;
			}
			case TOWER_DESTROY:
			{
				buffer.writeInt(_remainingTime);
				buffer.writeInt(_blueTeam.getPoints());
				buffer.writeInt(_redTeam.getPoints());
				buffer.writeInt(_targetTeam.getId());
				buffer.writeInt(_compressorId);
				writeRecord(buffer, _killer);
				break;
			}
			case CAT_UPDATE:
			{
				final CleftPlayer target = _targetTeam.getCombatAerialTarget();
				buffer.writeInt(_remainingTime);
				buffer.writeInt(_targetTeam.getId());
				buffer.writeInt(target == null ? 0 : target.getObjectId());
				buffer.writeString(target == null ? "" : target.getPlayer().getName());
				break;
			}
			case RESULT:
			{
				buffer.writeInt(_blueTeam.getId());
				buffer.writeInt(_redTeam.getId());
				break;
			}
			case PVP_KILL:
			{
				buffer.writeInt(_remainingTime);
				buffer.writeInt(_blueTeam.getPoints());
				buffer.writeInt(_redTeam.getPoints());
				buffer.writeInt(_killerTeamId);
				writeRecord(buffer, _killer);
				buffer.writeInt(_killedTeamId);
				writeRecord(buffer, _killed);
				break;
			}
			default:
			{
				break;
			}
		}
	}
	
	/**
	 * Writes the object id of the Combat Aerial Target of a team.<br>
	 * A team without a target is sent as an empty entry.
	 * @param buffer The buffer being written.
	 * @param team The team to write.
	 */
	private void writeCombatAerialTargetId(WriteBuffer buffer, AerialCleftTeam team)
	{
		final CleftPlayer target = team.getCombatAerialTarget();
		buffer.writeInt(target == null ? 0 : target.getObjectId());
	}
	
	/**
	 * Writes the name of the Combat Aerial Target of a team.<br>
	 * A team without a target is sent as an empty entry.
	 * @param buffer The buffer being written.
	 * @param team The team to write.
	 */
	private void writeCombatAerialTargetName(WriteBuffer buffer, AerialCleftTeam team)
	{
		final CleftPlayer target = team.getCombatAerialTarget();
		buffer.writeString(target == null ? "" : target.getPlayer().getName());
	}
	
	/**
	 * Writes the battle record of every participant of a team.
	 * @param buffer The buffer being written.
	 * @param team The team to write.
	 */
	private void writeTeamRecords(WriteBuffer buffer, AerialCleftTeam team)
	{
		buffer.writeInt(team.getPlayerCount());
		for (CleftPlayer cleftPlayer : team.getPlayers())
		{
			buffer.writeInt(cleftPlayer.getObjectId());
			buffer.writeInt(cleftPlayer.getKills());
			buffer.writeInt(cleftPlayer.getDeaths());
			buffer.writeInt(cleftPlayer.getCompressors());
		}
	}
	
	/**
	 * Writes the battle record of a single participant.
	 * @param buffer The buffer being written.
	 * @param cleftPlayer The record to write.
	 */
	private void writeRecord(WriteBuffer buffer, CleftPlayer cleftPlayer)
	{
		buffer.writeInt(cleftPlayer.getObjectId());
		buffer.writeInt(cleftPlayer.getCompressors());
		buffer.writeInt(cleftPlayer.getKills());
		buffer.writeInt(cleftPlayer.getDeaths());
	}
}
