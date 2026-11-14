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
package org.l2jmobius.gameserver.network.serverpackets.gacha;

import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.holders.GachaItemHolder;
import org.l2jmobius.gameserver.managers.events.UniqueGachaManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * Handles the network communication for the {@code UniqueGacha} game system.<br>
 * This packet is used to synchronize gacha results between the server and the client.
 * @author Atronic
 */
public class UniqueGachaGame extends ServerPacket
{
	public static final int FAILURE = 0;
	public static final int SUCCESS = 1;
	
	private static final int SHORT_PACKET_INFO = 2 + 1 + 4 + 8;
	
	private final int _success;
	private final Player _player;
	private final List<GachaItemHolder> _rewards;
	private final boolean _rare;
	
	/**
	 * Creates a new {@code UniqueGachaGame} packet.<br>
	 * This constructor initializes the game results and rewards for a player.<br>
	 * It also updates the currency count and guaranteed reward steps from the manager.
	 * @param success The result of the gacha attempt, such as {@code 1} for success or {@code 0} for failure.
	 * @param player The {@code Player} who performed the action.
	 * @param rewards A {@code List} of {@code GachaItemHolder} objects to be given as prizes.
	 * @param rare A boolean indicating if the reward is considered a rare item.
	 */
	public UniqueGachaGame(int success, Player player, List<GachaItemHolder> rewards, boolean rare)
	{
		_success = success;
		_player = player;
		_rewards = rewards;
		_rare = rare;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		final UniqueGachaManager manager = UniqueGachaManager.getInstance();
		ServerPackets.EX_UNIQUE_GACHA_GAME.writeId(this, buffer);
		buffer.writeByte(_success); // result // char
		buffer.writeLong(manager.getCurrencyCount(_player)); // amount // int
		buffer.writeInt(_player.getVariables().getInt(UniqueGachaManager.GACHA_PLAYER_VARIABLE, 0)); // guaruant // int
		buffer.writeInt(manager.getGuaranteeRoll()); // nNextConfirmCount - 557
		// Zero is yellow and one is purple.
		buffer.writeByte(_rare ? 1 : 0); // rank // char
		buffer.writeInt(_rewards.size()); // size // int
		for (GachaItemHolder item : _rewards)
		{
			buffer.writeShort(SHORT_PACKET_INFO); // _SUniqueGachaBaseItemInfo size prefix - 557
			buffer.writeByte(item.getRank().getClientId()); // rank // char
			buffer.writeInt(item.getId()); // itemId // int
			buffer.writeLong(item.getCount()); // count // long
			buffer.writeByte(1); // bIsConfirmed - 557
		}
	}
}
