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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.enums.Movie;

/**
 * This packet is sent to the client to initiate the playback of a specific scene movie.<br>
 * It contains the necessary information for the {@code GameClient} to load and play the correct animation.
 * @author JIV
 */
public class ExStartScenePlayer extends ServerPacket
{
	private final Movie _movie;
	
	/**
	 * Creates a new {@code ExStartScenePlayer} packet.<br>
	 * This packet is used to start playing a specific movie scene.
	 * @param movie The {@code Movie} type to be played.
	 */
	public ExStartScenePlayer(Movie movie)
	{
		_movie = movie;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_START_SCENE_PLAYER.writeId(this, buffer);
		buffer.writeInt(_movie.getClientId());
	}
}
