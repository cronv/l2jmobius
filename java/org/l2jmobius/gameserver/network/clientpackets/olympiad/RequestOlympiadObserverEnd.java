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
package org.l2jmobius.gameserver.network.clientpackets.olympiad;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.ObserverMode;
import org.l2jmobius.gameserver.handler.AdminCommandHandler;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadMode;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadMode;

/**
 * This packet is sent by the client to request the end of an observer mode.<br>
 * It allows players to stop viewing a specific {@code OlympiadMode} event.
 * @author -Wooden-, Mobius
 */
public class RequestOlympiadObserverEnd extends ClientPacket
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final ObserverMode observerMode = player.getObserverMode();
		if (observerMode.isActive())
		{
			observerMode.leaveOlympiad();
		}
		else if (player.isFreeLook())
		{
			AdminCommandHandler.getInstance().onCommand(player, "admin_freecam off", false);
		}
		else
		{
			player.sendPacket(new ExOlympiadMode(OlympiadMode.NONE));
		}
	}
}
