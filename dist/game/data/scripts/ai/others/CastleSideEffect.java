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
package ai.others;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.network.serverpackets.ExCastleState;

/**
 * This class handles the visual representation of castle side effects within cities.<br>
 * It ensures that players can see relevant siege information in urban areas.
 * @author Gigi
 * @date 2019-05-14 - [12:47:33]
 */
public class CastleSideEffect extends Script
{
	private static final int[] ZONE_ID =
	{
		11020, // Giran
		11027, // Gludio
		11028, // Dion
		11029, // Oren
		11031, // aden
		11032, // Goddard
		11033, // Rune
		11034, // Heine
		11035, // Shuttgard
	};
	
	/**
	 * Initializes the {@code CastleSideEffect} script.<br>
	 * This constructor registers specific zone IDs for side effect processing.
	 */
	public CastleSideEffect()
	{
		addEnterZoneId(ZONE_ID);
	}
	
	/**
	 * This method is called when a {@code Creature} enters a new zone.<br>
	 * It checks if the character is a player.<br>
	 * If they are, it sends the current castle states to them.
	 * @param character The {@code Creature} that entered the zone.
	 * @param zone The type of {@code ZoneType} being entered.
	 */
	@Override
	public void onEnterZone(Creature character, ZoneType zone)
	{
		if (character.isPlayer())
		{
			for (Castle castle : CastleManager.getInstance().getCastles())
			{
				character.sendPacket(new ExCastleState(castle));
			}
		}
	}
	
	/**
	 * This is the entry point for the {@code CastleSideEffect} class.<br>
	 * It initializes a new instance of the script to start execution.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new CastleSideEffect();
	}
}
