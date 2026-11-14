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
package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.zone.ZoneId;

/**
 * Retail only_pve area: no PvP (as {@link NoPvPZone}), and players who die inside lose no XP and drop no items.
 * @author Mobius
 */
public class OnlyPveZone extends NoPvPZone
{
	/**
	 * Creates a new {@code OnlyPveZone}.
	 * @param id The unique identifier for this zone.
	 */
	public OnlyPveZone(int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		creature.setInsideZone(ZoneId.ONLY_PVE, true);
		super.onEnter(creature);
	}
	
	@Override
	protected void onExit(Creature creature)
	{
		creature.setInsideZone(ZoneId.ONLY_PVE, false);
		super.onExit(creature);
	}
}
