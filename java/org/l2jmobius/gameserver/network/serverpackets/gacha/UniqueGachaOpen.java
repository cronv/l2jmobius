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

import java.util.Map.Entry;
import java.util.Set;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.holders.GachaItemHolder;
import org.l2jmobius.gameserver.managers.events.UniqueGachaManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client when a player opens a {@code UniqueGacha}.<br>
 * It contains the results of the gacha pull for the user.
 * @author Atronic
 */
public class UniqueGachaOpen extends ServerPacket
{
	private final static int SHORT_PACKET_INFO = 2 + 1 + 4 + 8;
	
	private final Player _player;
	private final int _fullInfo;
	private final int _openMode;
	
	/**
	 * Initializes a new {@code UniqueGachaOpen} packet for a specific player.<br>
	 * This constructor sets up the gacha data based on the provided mode and info flags.<br>
	 * It retrieves necessary statistics from the {@code UniqueGachaManager}.
	 * @param player The {@code Player} who is opening the unique gacha.
	 * @param fullInfo A flag indicating whether to include full reward details in the packet.
	 * @param openMode The specific mode used for opening the gacha.
	 */
	public UniqueGachaOpen(Player player, int fullInfo, int openMode)
	{
		_player = player;
		_fullInfo = fullInfo;
		_openMode = openMode;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		final UniqueGachaManager manager = UniqueGachaManager.getInstance();
		ServerPackets.EX_UNIQUE_GACHA_OPEN.writeId(this, buffer);
		/**
		 * open mode 1 = const GACHA_TITLE_FORM = 1; = will open main window?
		 */
		buffer.writeByte(_openMode); // open mode // char // 1 = main menu
		buffer.writeByte(0); // result // char // not used?
		buffer.writeLong(manager.getCurrencyCount(_player)); // my cost item amount // int // current item count for roll?
		buffer.writeInt(_player.getVariables().getInt(UniqueGachaManager.GACHA_PLAYER_VARIABLE, 0)); // my confirm count // int // left to guarant
		buffer.writeInt(manager.getGuaranteeRoll()); // nNextConfirmCount - 557
		buffer.writeInt((int) ((manager.getActiveUntilPeriod() - System.currentTimeMillis()) / 1000L)); // remain time in seconds // int
		buffer.writeByte(_fullInfo); // full info // char
		buffer.writeByte(manager.isShowProbability() ? 1 : 0); // show probability // char
		if (_fullInfo == 1)
		{
			writeFullInfo(manager, buffer);
		}
		else
		{
			writeDummyInfo(buffer);
		}
	}
	
	/**
	 * Writes the complete gacha information to the provided buffer.<br>
	 * This includes visible items, reward items, and cost details.
	 * @param manager The {@code UniqueGachaManager} containing all necessary data.
	 * @param buffer The {@code WriteBuffer} used to store the outgoing packet data.
	 */
	private void writeFullInfo(UniqueGachaManager manager, WriteBuffer buffer)
	{
		buffer.writeInt(manager.getVisibleItems().size()); // show items size // int
		for (GachaItemHolder item : manager.getVisibleItems())
		{
			buffer.writeShort(SHORT_PACKET_INFO); // current size // short
			buffer.writeByte(item.getRank().getClientId()); // rank type // char
			buffer.writeInt(item.getId()); // item type // int
			buffer.writeLong(item.getCount()); // amount // long
			buffer.writeInt(getChance(manager.isShowProbability(), item.getItemChance())); // probability // double
		}
		
		buffer.writeInt(manager.getTotalRewardCount()); // reward item size // int
		for (Set<GachaItemHolder> items : manager.getRewardItems().values())
		{
			for (GachaItemHolder item : items)
			{
				buffer.writeShort(SHORT_PACKET_INFO); // current size // short
				buffer.writeByte(item.getRank().getClientId()); // rank type // char
				buffer.writeInt(item.getId()); // item type // int
				buffer.writeLong(item.getCount()); // amount // long
				buffer.writeInt(getChance(manager.isShowProbability(), item.getItemChance())); // probability // double
			}
		}
		
		buffer.writeInt(0); // 557
		
		buffer.writeByte(1); // cost type // char // bool?
		buffer.writeInt(manager.getCurrencyItemId()); // cost item type // int // item id
		buffer.writeInt(manager.getGameCosts().size()); // cost amount item info // int
		for (Entry<Integer, Long> entry : manager.getGameCosts().entrySet())
		{
			buffer.writeInt(entry.getKey()); // game count // int
			buffer.writeLong(entry.getValue()); // amount // long
		}
	}
	
	/**
	 * Writes placeholder data to the {@code WriteBuffer}.<br>
	 * This method is used to maintain the correct packet structure.<br>
	 * It populates fields with default values like 0.
	 * @param buffer The {@code WriteBuffer} where the dummy information will be written.
	 */
	private void writeDummyInfo(WriteBuffer buffer)
	{
		buffer.writeInt(0); // show items size // int
		buffer.writeInt(0); // reward item size // int
		buffer.writeInt(0); // confirmed item size // int - 557
		buffer.writeByte(0); // cost type // char // bool?
		buffer.writeInt(0); // cost item type // int // item id
		buffer.writeInt(0); // cost amount item info // int
	}
	
	/**
	 * Calculates the chance value for a gacha item.<br>
	 * It checks if the chance should be displayed to the user.<br>
	 * If {@code showChance} is {@code true}, it returns a percentage calculation.<br>
	 * Otherwise, it returns {@code 0}.
	 * @param showChance Determines if the result should be visible.
	 * @param itemChance The base chance value of the item.
	 * @return The calculated chance as an {@code int}.
	 */
	private int getChance(boolean showChance, int itemChance)
	{
		return showChance ? (itemChance / 100) : 0;
	}
}
