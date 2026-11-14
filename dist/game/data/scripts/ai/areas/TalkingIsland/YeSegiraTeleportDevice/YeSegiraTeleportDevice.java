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
package ai.areas.TalkingIsland.YeSegiraTeleportDevice;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerCreate;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.enums.Movie;

/**
 * This class handles the artificial intelligence for the Ye Segira Teleport Device.<br>
 * It manages the logic required to teleport players when they interact with the device.
 * @author St3eT
 */
public class YeSegiraTeleportDevice extends Script
{
	// NPCs
	private static final int[] TELEPORT_DEVICES =
	{
		33180,
		33181,
		33182,
		33183,
		33185,
		33186,
		33187,
		33188,
		33189,
		33190,
		33191,
		33205,
		33192,
		33197,
	};
	
	// Locations
	private static final Map<String, Location> LOCATIONS = new HashMap<>();
	static
	{
		LOCATIONS.put("village", new Location(-114413, 252159, -1592));
		LOCATIONS.put("village2", new Location(-112529, 256741, -1456));
		LOCATIONS.put("observatory", new Location(-114675, 230171, -1648));
		LOCATIONS.put("easten_exit", new Location(-109294, 237397, -2928));
		LOCATIONS.put("western_exit", new Location(-122189, 241009, -2328));
		LOCATIONS.put("1_exploration_zone", new Location(-115005, 237383, -3088));
		LOCATIONS.put("2_exploration_zone", new Location(-118350, 233992, -2904));
		LOCATIONS.put("3_exploration_zone", new Location(-116303, 239062, -2736));
		LOCATIONS.put("4_exploration_zone", new Location(-112382, 238710, -2904));
		LOCATIONS.put("5_exploration_zone", new Location(-110980, 233774, -3200));
	}
	
	// Misc
	private static final String MOVIE_VAR = "TI_YESEGIRA_MOVIE";
	
	/**
	 * Private constructor for the {@code YeSegiraTeleportDevice} class.<br>
	 * This constructor initializes the script by registering NPC IDs.<br>
	 * It sets up the required talk and start IDs for teleportation devices.
	 */
	private YeSegiraTeleportDevice()
	{
		addFirstTalkId(TELEPORT_DEVICES);
		addStartNpc(TELEPORT_DEVICES);
		addTalkId(TELEPORT_DEVICES);
	}
	
	/**
	 * Handles specific events for the teleport device.<br>
	 * It teleports the {@code Player} to a location based on the provided {@code event}.<br>
	 * This method also handles special movie playback and quest notifications.
	 * @param event The name of the location or action to trigger.
	 * @param npc The {@code Npc} instance involved in the interaction.
	 * @param player The {@code Player} who triggered the event.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final Location loc = LOCATIONS.get(event);
		if (loc != null)
		{
			player.teleToLocation(loc, true);
			if (event.equals("observatory") && player.getVariables().getBoolean(MOVIE_VAR, false))
			{
				playMovie(player, Movie.SI_ILLUSION_03_QUE);
				player.getVariables().remove(MOVIE_VAR);
			}
			else if (event.equals("5_exploration_zone"))
			{
				// final QuestState qs = player.getQuestState(Q10365_ForTheSearchdogKing.class.getSimpleName());
				// if ((qs != null) && qs.isStarted() && qs.isCond(1))
				// {
				// final Quest quest_10365 = ScriptManager.getInstance().getScript(Q10365_ForTheSearchdogKing.class.getSimpleName());
				// if (quest_10365 != null)
				// {
				// quest_10365.notifyEvent("TELEPORT_TO_NEXT_STAGE", null, player);
				// }
				// }
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when a new player enters the game.<br>
	 * It checks if the {@code Player} belongs to the {@code Race.ERTHEIA} race.<br>
	 * If they are not an Ertheia, it sets a specific movie variable to {@code true}.
	 * @param event The {@code OnPlayerCreate} event containing the player data.
	 */
	@RegisterEvent(EventType.ON_PLAYER_CREATE)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerCreate(OnPlayerCreate event)
	{
		final Player player = event.getPlayer();
		if (player.getRace() != Race.ERTHEIA)
		{
			player.getVariables().set(MOVIE_VAR, true);
		}
	}
	
	/**
	 * Handles the first interaction between a player and an {@code Npc}.<br>
	 * It returns the filename for the dialogue to be displayed.
	 * @param npc The {@code Npc} that is being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the main entry point for the {@code YeSegiraTeleportDevice} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new YeSegiraTeleportDevice();
	}
}
