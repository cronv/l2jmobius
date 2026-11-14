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
package ai.others;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class provides a simple AI to manage special conditions for the {@code DivineBeast} summon.<br>
 * It handles specific behaviors and logic required by this unique entity type.
 * @author UnAfraid
 */
public class DivineBeast extends Script
{
	private static final int DIVINE_BEAST = 14870;
	private static final int TRANSFORMATION_ID = 258;
	private static final int CHECK_TIME = 2 * 1000;
	
	/**
	 * Private constructor for the {@code DivineBeast} class.<br>
	 * This constructor initializes the script and registers the summon ID.<br>
	 * It prevents other classes from creating new instances of this class.
	 */
	private DivineBeast()
	{
		addSummonSpawnId(DIVINE_BEAST);
	}
	
	/**
	 * This method is called when a new {@code Summon} is created.<br>
	 * It starts a timer to validate the transformation for the summon.
	 * @param summon The {@code Summon} entity that was just spawned.
	 */
	@Override
	public void onSummonSpawn(Summon summon)
	{
		startQuestTimer("VALIDATE_TRANSFORMATION", CHECK_TIME, null, summon.asPlayer(), true);
	}
	
	/**
	 * Handles specific events for the Divine Beast script.<br>
	 * This method checks if a {@code Player} has active servitors and the correct transformation.<br>
	 * It cancels timers or removes summons if requirements are not met.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} involved in the event.
	 * @param player The {@code Player} interacting with the script.
	 * @return The result of the base {@code onEvent} method call.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ((player == null) || !player.hasServitors())
		{
			cancelQuestTimer(event, npc, player);
		}
		else if (player.getTransformationId() != TRANSFORMATION_ID)
		{
			cancelQuestTimer(event, npc, player);
			player.getServitors().values().forEach(summon -> summon.unSummon(player));
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This is the entry point for the {@code DivineBeast} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new DivineBeast();
	}
}
