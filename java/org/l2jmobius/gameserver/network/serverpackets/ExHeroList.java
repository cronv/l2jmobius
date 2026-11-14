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

import java.util.Map;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.mechanics.olympiad.Hero;
import org.l2jmobius.gameserver.mechanics.olympiad.Olympiad;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This packet is sent to the client to provide a list of heroes participating in an {@code Olympiad}.<br>
 * It contains information about each {@code Hero} and their current statistics.<br>
 * Use this class to synchronize the hero list with the game client.
 * @author Mobius
 */
public class ExHeroList extends ServerPacket
{
	private final Map<Integer, StatSet> _heroList;
	
	/**
	 * Creates a new instance of {@code ExHeroList}.<br>
	 * This constructor initializes the internal hero list.<br>
	 * It retrieves data from the {@code Hero} singleton.
	 */
	public ExHeroList()
	{
		_heroList = Hero.getInstance().getHeroes();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_HERO_LIST.writeId(this, buffer);
		buffer.writeInt(_heroList.size());
		for (StatSet hero : _heroList.values())
		{
			buffer.writeString(hero.getString(Olympiad.CHAR_NAME));
			buffer.writeInt(hero.getInt(Olympiad.CLASS_ID));
			buffer.writeString(hero.getString(Hero.CLAN_NAME, ""));
			buffer.writeInt(0); // hero.getInt(Hero.CLAN_CREST, 0)
			buffer.writeString(hero.getString(Hero.ALLY_NAME, ""));
			buffer.writeInt(0); // hero.getInt(Hero.ALLY_CREST, 0)
			buffer.writeInt(hero.getInt(Hero.COUNT));
			buffer.writeInt(ServerConfig.SERVER_ID);
			buffer.writeByte(0); // 272
		}
	}
}
