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
package ai.areas.Parnassus.EntrancePortalToCrystalCaverns;

import java.util.Calendar;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.OnEventTrigger;
import org.l2jmobius.gameserver.util.StatSet;

import instances.CrystalCaverns.CrystalCavernsCoralGarden;
import instances.CrystalCaverns.CrystalCavernsEmeraldSquare;
import instances.CrystalCaverns.CrystalCavernsSteamCorridor;

/**
 * Handles the artificial intelligence and logic for the entrance portal to the Crystal Caverns.<br>
 * This {@code EntrancePortalToCrystalCaverns} class manages interactions related to this specific area.
 * @author St3eT
 */
public class EntrancePortalToCrystalCaverns extends Script
{
	// NPCs
	private static final int CAVERNS_ENTRACE = 33522;
	
	// Misc
	private static final int EMERALD_SQUARE_TEMPLATE_ID = 163;
	private static final int STEAM_CORRIDOR_TEMPLATE_ID = 164;
	private static final int CORAL_GARDEN_TEMPLATE_ID = 165;
	private static final int PRISON_ENTRACE_TRIGGER_1 = 24230010;
	private static final int PRISON_ENTRACE_TRIGGER_2 = 24230012;
	private static final int CAVERNS_ENTRACE_TRIGGER_1 = 24230014;
	private static final int CAVERNS_ENTRACE_TRIGGER_2 = 24230016;
	private static final int CAVERNS_ENTRACE_TRIGGER_3 = 24230018;
	
	/**
	 * Initializes the default NPC settings for this script.<br>
	 * It registers the required IDs for talking, spawning, and visibility.
	 */
	private EntrancePortalToCrystalCaverns()
	{
		addStartNpc(CAVERNS_ENTRACE);
		addTalkId(CAVERNS_ENTRACE);
		addFirstTalkId(CAVERNS_ENTRACE);
		addSpawnId(CAVERNS_ENTRACE);
		addCreatureSeeId(CAVERNS_ENTRACE);
	}
	
	/**
	 * Handles specific events triggered by the portal.<br>
	 * This method checks if the event is {@code enterInstance}.<br>
	 * It then notifies the correct instance script based on the current template ID.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} that triggered the event.
	 * @param player The {@code Player} involved in the event.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("enterInstance"))
		{
			Quest instanceScript = null;
			
			switch (getCurrentInstanceTemplateId())
			{
				case EMERALD_SQUARE_TEMPLATE_ID:
				{
					instanceScript = ScriptManager.getInstance().getScript(CrystalCavernsEmeraldSquare.class.getSimpleName());
					break;
				}
				case STEAM_CORRIDOR_TEMPLATE_ID:
				{
					instanceScript = ScriptManager.getInstance().getScript(CrystalCavernsSteamCorridor.class.getSimpleName());
					break;
				}
				case CORAL_GARDEN_TEMPLATE_ID:
				{
					instanceScript = ScriptManager.getInstance().getScript(CrystalCavernsCoralGarden.class.getSimpleName());
					break;
				}
			}
			
			if (instanceScript != null)
			{
				instanceScript.notifyEvent(event, npc, player);
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles the first interaction between a player and an {@code Npc}.<br>
	 * It returns the path to the correct HTML file based on the current instance.
	 * @param npc The {@code Npc} that is being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the filename of the dialogue.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "EntrancePortal_" + getCurrentInstanceTemplateId() + ".html";
	}
	
	/**
	 * This method is called when the {@code Npc} is first spawned.<br>
	 * It sets a display effect on the {@code Npc}.<br>
	 * It also starts a repeating timer for the NPC.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		npc.setDisplayEffect(1);
		getTimers().addRepeatingTimer("LOOP_TIMER", 10000, npc, null);
	}
	
	/**
	 * Handles periodic timer events for the script.<br>
	 * This method updates player triggers when a {@code LOOP_TIMER} event occurs.
	 * @param event The name of the event being triggered.
	 * @param params A set of statistics used as parameters for the event.
	 * @param npc The {@code Npc} instance that owns this script.
	 * @param player The {@code Player} instance involved in the interaction.
	 */
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		if (event.equals("LOOP_TIMER"))
		{
			final int currentTemplateId = getCurrentInstanceTemplateId();
			World.forEachVisibleObjectInRange(npc, Player.class, 500, p -> updateTriggersForPlayer(player, currentTemplateId));
		}
	}
	
	/**
	 * This method is called when an {@code Npc} sees a {@code Creature}.<br>
	 * It checks if the creature is a {@code Player}.<br>
	 * If it is a player, it sends specific trigger packets and updates their triggers.
	 * @param npc The {@code Npc} that sees the creature.
	 * @param creature The {@code Creature} that was seen.
	 */
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			player.sendPacket(new OnEventTrigger(PRISON_ENTRACE_TRIGGER_1, true));
			player.sendPacket(new OnEventTrigger(PRISON_ENTRACE_TRIGGER_2, true));
			updateTriggersForPlayer(player, getCurrentInstanceTemplateId());
		}
	}
	
	/**
	 * Updates the active triggers for a specific player based on the current area.<br>
	 * This method sends {@code OnEventTrigger} packets to the {@code Player}.<br>
	 * It enables the correct trigger depending on the provided {@code currentTemplateId}.
	 * @param player The {@code Player} who will receive the trigger updates.
	 * @param currentTemplateId The ID of the current area template used to determine which trigger to enable.
	 */
	public void updateTriggersForPlayer(Player player, int currentTemplateId)
	{
		if (player != null)
		{
			player.sendPacket(new OnEventTrigger(CAVERNS_ENTRACE_TRIGGER_1, false));
			player.sendPacket(new OnEventTrigger(CAVERNS_ENTRACE_TRIGGER_2, false));
			player.sendPacket(new OnEventTrigger(CAVERNS_ENTRACE_TRIGGER_3, false));
			
			switch (currentTemplateId)
			{
				case EMERALD_SQUARE_TEMPLATE_ID:
				{
					player.sendPacket(new OnEventTrigger(CAVERNS_ENTRACE_TRIGGER_1, true));
					break;
				}
				case STEAM_CORRIDOR_TEMPLATE_ID:
				{
					player.sendPacket(new OnEventTrigger(CAVERNS_ENTRACE_TRIGGER_2, true));
					break;
				}
				case CORAL_GARDEN_TEMPLATE_ID:
				{
					player.sendPacket(new OnEventTrigger(CAVERNS_ENTRACE_TRIGGER_3, true));
					break;
				}
			}
		}
	}
	
	/**
	 * Retrieves the current instance template ID based on the time and day.<br>
	 * This method checks the current system clock to determine which area is active.<br>
	 * It returns a specific {@code int} value corresponding to the correct zone.
	 * @return The {@code int} template ID of the currently active instance.
	 */
	public int getCurrentInstanceTemplateId()
	{
		final int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
		final int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		int templateId = -1;
		
		switch (day)
		{
			case Calendar.MONDAY:
			{
				templateId = (hour < 18) ? EMERALD_SQUARE_TEMPLATE_ID : STEAM_CORRIDOR_TEMPLATE_ID;
				break;
			}
			case Calendar.TUESDAY:
			{
				templateId = (hour < 18) ? CORAL_GARDEN_TEMPLATE_ID : EMERALD_SQUARE_TEMPLATE_ID;
				break;
			}
			case Calendar.WEDNESDAY:
			{
				templateId = (hour < 18) ? STEAM_CORRIDOR_TEMPLATE_ID : CORAL_GARDEN_TEMPLATE_ID;
				break;
			}
			case Calendar.THURSDAY:
			{
				templateId = (hour < 18) ? EMERALD_SQUARE_TEMPLATE_ID : STEAM_CORRIDOR_TEMPLATE_ID;
				break;
			}
			case Calendar.FRIDAY:
			{
				templateId = (hour < 18) ? CORAL_GARDEN_TEMPLATE_ID : EMERALD_SQUARE_TEMPLATE_ID;
				break;
			}
			case Calendar.SATURDAY:
			{
				templateId = (hour < 18) ? STEAM_CORRIDOR_TEMPLATE_ID : CORAL_GARDEN_TEMPLATE_ID;
				break;
			}
			case Calendar.SUNDAY:
			{
				templateId = (hour < 18) ? EMERALD_SQUARE_TEMPLATE_ID : STEAM_CORRIDOR_TEMPLATE_ID;
				break;
			}
		}
		
		return templateId;
	}
	
	/**
	 * This is the main entry point for the {@code EntrancePortalToCrystalCaverns} script.<br>
	 * It initializes a new instance of the class to start the logic.
	 * @param args The command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new EntrancePortalToCrystalCaverns();
	}
}
