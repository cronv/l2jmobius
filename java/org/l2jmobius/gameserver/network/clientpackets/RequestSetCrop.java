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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.managers.CastleManorManager;
import org.l2jmobius.gameserver.mechanics.siege.manor.CropProcure;
import org.l2jmobius.gameserver.mechanics.siege.manor.Seed;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

/**
 * Handles the client request to set a specific crop.<br>
 * This packet is used during manor siege mechanics to define which {@code Seed} is being planted.
 * @author l3x, Galagard
 */
public class RequestSetCrop extends ClientPacket
{
	private static final int BATCH_LENGTH = 21; // length of the one item
	
	private int _manorId;
	private List<CropProcure> _items;
	
	@Override
	protected void readImpl()
	{
		_manorId = readInt();
		final int count = readInt();
		if ((count <= 0) || (count > PlayerConfig.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != remaining()))
		{
			return;
		}
		
		_items = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
		{
			final int itemId = readInt();
			final long sales = readLong();
			final long price = readLong();
			final int type = readByte();
			if ((itemId < 1) || (sales < 0) || (price < 0))
			{
				_items.clear();
				return;
			}
			
			if (sales > 0)
			{
				_items.add(new CropProcure(itemId, sales, type, sales, price));
			}
		}
	}
	
	@Override
	protected void runImpl()
	{
		if ((_items == null) || _items.isEmpty())
		{
			return;
		}
		
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final CastleManorManager manor = CastleManorManager.getInstance();
		if (!manor.isModifiablePeriod())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Check player privileges
		final Clan clan = player.getClan();
		if ((clan == null) || (clan.getCastleId() != _manorId) || !player.hasAccess(ClanAccess.CASTLE_MANOR) || !player.getLastFolkNPC().canInteract(player))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Filter crops with start amount lower than 0 and incorrect price.
		final List<CropProcure> list = new ArrayList<>(_items.size());
		for (CropProcure cp : _items)
		{
			final Seed s = manor.getSeedByCrop(cp.getId(), _manorId);
			if ((s != null) && (cp.getStartAmount() <= s.getCropLimit()) && (cp.getPrice() >= s.getCropMinPrice()) && (cp.getPrice() <= s.getCropMaxPrice()))
			{
				list.add(cp);
			}
		}
		
		// Save crop list
		manor.setNextCropProcure(list, _manorId);
	}
}
