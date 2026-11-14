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

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Handles the artificial intelligence for monsters that flee from players.<br>
 * This {@code Script} determines how these entities behave when threatened.
 * @author Mobius, NosBit
 */
public class FleeMonsters extends Script
{
	// NPCs
	private static final int[] MOBS =
	{
		20002, // Rabbit
		25604, // Mutated Elpy
	};
	
	// Misc
	private static final int FLEE_DISTANCE = 500;
	
	/**
	 * Initializes the {@code FleeMonsters} script.<br>
	 * Registers the list of monster IDs to the attack system.
	 */
	private FleeMonsters()
	{
		addAttackId(MOBS);
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked.<br>
	 * This method makes the {@code Npc} flee from the attacker.<br>
	 * It calculates a destination based on the {@code FLEE_DISTANCE}.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} who initiated the attack.
	 * @param damage The amount of damage dealt to the {@code Npc}.
	 * @param isSummon A boolean indicating if the attack came from a summon.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		npc.disableCoreAI(true);
		npc.setRunning();
		
		final Summon summon = isSummon ? attacker.getServitors().values().stream().findFirst().orElse(attacker.getPet()) : null;
		final ILocational attackerLoc = summon == null ? attacker : summon;
		final double radians = Math.toRadians(LocationUtil.calculateAngleFrom(attackerLoc, npc));
		final int npcX = npc.getX();
		final int posX = (int) (npcX + (FLEE_DISTANCE * Math.cos(radians)));
		final int npcY = npc.getY();
		final int posY = (int) (npcY + (FLEE_DISTANCE * Math.sin(radians)));
		final int posZ = npc.getZ();
		final Location destination = GeoEngine.getInstance().getValidLocation(npcX, npcY, posZ, posX, posY, posZ, npc.getInstanceWorld());
		npc.getAI().setIntentionMoveTo(destination);
	}
	
	/**
	 * This is the entry point for the {@code FleeMonsters} script.<br>
	 * It initializes a new instance of the class to start the logic.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new FleeMonsters();
	}
}
