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
package custom.events.Rabbits;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.time.SchedulingPattern;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Event;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the {@code Rabbits} event logic.<br>
 * Players must use specific transformation skills to locate and open hidden chests on Fantasy Isle.
 * @author Gnacik, Zoey76
 */
public class Rabbits extends Event
{
	// NPCs
	private static final int NPC_MANAGER = 900101;
	private static final int CHEST = 900102;
	
	// Skills
	private static final SkillHolder RABBIT_MAGIC_EYE = new SkillHolder(629, 1);
	private static final SkillHolder RABBIT_TORNADO = new SkillHolder(630, 1);
	private static final SkillHolder RABBIT_TRANSFORMATION = new SkillHolder(2428, 1);
	private static final SkillHolder RAID_CURSE = new SkillHolder(4515, 1);
	
	// Misc
	private static final int EVENT_TIME = 10;
	private static final int TOTAL_CHEST_COUNT = 75;
	private static final int TRANSFORMATION_ID = 105;
	private final Collection<Npc> _npcs = ConcurrentHashMap.newKeySet();
	private final List<Player> _players = new ArrayList<>();
	private boolean _isActive = false;
	
	/**
	 * Drop data:<br>
	 * Higher the chance harder the item.<br>
	 * ItemId, chance in percent, min amount, max amount
	 */
	private static final int[][] DROPLIST =
	{
		// @formatter:off
		{  1540,  80, 10, 15 },	// Quick Healing Potion
		{  1538,  60,  5, 10 },	// Blessed Scroll of Escape
		{  3936,  40,  5, 10 },	// Blessed Scroll of Ressurection
		{  6387,  25,  5, 10 },	// Blessed Scroll of Ressurection Pets
		{ 22025,  15,  5, 10 },	// Powerful Healing Potion
		{  6622,  10,  1, 1 },	// Giant's Codex
		{ 20034,   5,  1, 1 },	// Revita Pop
		{ 20004,   1,  1, 1 },	// Energy Ginseng
		{ 20004,   0,  1, 1 }	// Energy Ginseng
		// @formatter:on
	};
	
	/**
	 * Initializes the {@code Rabbits} event.<br>
	 * Registers necessary NPCs and skills for the event.<br>
	 * Loads the configuration data from the files.
	 */
	private Rabbits()
	{
		addFirstTalkId(NPC_MANAGER, CHEST);
		addTalkId(NPC_MANAGER);
		addStartNpc(NPC_MANAGER);
		addSkillSeeId(CHEST);
		addAttackId(CHEST);
		
		loadConfig();
	}
	
	/**
	 * Loads the event configuration from an XML file.<br>
	 * This method parses the data pack located at {@code data/scripts/custom/events/Rabbits/config.xml}.<br>
	 * It initializes the event schedules and parameters into the system timers.
	 */
	private void loadConfig()
	{
		new IXmlReader()
		{
			@Override
			public void load()
			{
				parseDatapackFile("data/scripts/custom/events/Rabbits/config.xml");
			}
			
			@Override
			public void parseDocument(Document document, File file)
			{
				final AtomicInteger count = new AtomicInteger(0);
				forEach(document, "event", eventNode ->
				{
					final StatSet att = new StatSet(parseAttributes(eventNode));
					final String name = att.getString("name");
					for (Node node = document.getDocumentElement().getFirstChild(); node != null; node = node.getNextSibling())
					{
						switch (node.getNodeName())
						{
							case "schedule":
							{
								final StatSet attributes = new StatSet(parseAttributes(node));
								final String pattern = attributes.getString("pattern");
								final SchedulingPattern schedulingPattern = new SchedulingPattern(pattern);
								final StatSet params = new StatSet();
								params.set("Name", name);
								params.set("SchedulingPattern", pattern);
								final long delay = schedulingPattern.getDelayToNextFromNow();
								getTimers().addTimer("Schedule" + count.incrementAndGet(), params, delay + 5000, null, null); // Added 5 seconds to prevent overlapping.
								LOGGER.info("Event " + name + " scheduled at " + TimeUtil.getDateTimeString(System.currentTimeMillis() + delay));
								break;
							}
						}
					}
				});
			}
		}.load();
	}
	
	/**
	 * Handles periodic timer events for the rabbit event.<br>
	 * This method checks if the {@code event} string starts with {@code Schedule}.<br>
	 * If it does, it starts the event and adds a new timer to the schedule.
	 * @param event The name or type of the event to process.
	 * @param params A {@code StatSet} containing configuration data like patterns and names.
	 * @param npc The {@code Npc} associated with this event trigger.
	 * @param player The {@code Player} involved in this event trigger.
	 */
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		if (event.startsWith("Schedule"))
		{
			eventStart(null);
			final SchedulingPattern schedulingPattern = new SchedulingPattern(params.getString("SchedulingPattern"));
			final long delay = schedulingPattern.getDelayToNextFromNow();
			getTimers().addTimer(event, params, delay + 5000, null, null); // Added 5 seconds to prevent overlapping.
			LOGGER.info("Event " + params.getString("Name") + " scheduled at " + TimeUtil.getDateTimeString(System.currentTimeMillis() + delay));
		}
	}
	
	/**
	 * Starts the Rabbits event for the specified player.<br>
	 * This method checks if the event is already active and verifies that custom NPCs are enabled.<br>
	 * If successful, it spawns the manager and chests, then broadcasts a start message to all players.
	 * @param eventMaker The {@code Player} who initiated the event.
	 * @return {@code true} if the event started successfully, or {@code false} otherwise.
	 */
	@Override
	public boolean eventStart(Player eventMaker)
	{
		// Don't start event if it is active.
		if (_isActive)
		{
			eventMaker.sendMessage("Event " + getName() + " is already started!");
			return false;
		}
		
		// Check starting conditions
		if (!GeneralConfig.CUSTOM_NPC_DATA)
		{
			final String name = getName();
			LOGGER.info(name + ": Event can't be started, because custom NPCs are disabled!");
			eventMaker.sendMessage("Event " + name + " can't be started because custom NPCs are disabled!");
			return false;
		}
		
		// Set Event active
		_isActive = true;
		
		// Spawn Manager
		recordSpawn(_npcs, NPC_MANAGER, -59227, -56939, -2039, 64106, false, 0);
		
		// Spawn Chests
		for (int i = 0; i <= TOTAL_CHEST_COUNT; i++)
		{
			recordSpawn(_npcs, CHEST, getRandom(-60653, -58772), getRandom(-55830, -58146), -2030, 0, false, EVENT_TIME * 60000);
		}
		
		// Announce event start
		World.broadcastToAllOnlinePlayers("Rabbits Event: Chests spawned!");
		World.broadcastToAllOnlinePlayers("Rabbits Event: Go to Fantasy Isle and grab some rewards!");
		World.broadcastToAllOnlinePlayers("Rabbits Event: You have " + EVENT_TIME + " minuntes!");
		World.broadcastToAllOnlinePlayers("Rabbits Event: After that time all chests will disappear...");
		
		// Schedule event end
		startQuestTimer("END_RABBITS_EVENT", EVENT_TIME * 60000, null, eventMaker);
		return true;
	}
	
	/**
	 * Stops the current rabbit event.<br>
	 * This method sets the event to inactive and cleans up all active NPCs.<br>
	 * It also removes transformations from players and broadcasts a finish message.
	 * @return {@code true} if the event was successfully stopped, or {@code false} if it was already inactive.
	 */
	@Override
	public boolean eventStop()
	{
		// Don't stop inactive event.
		if (!_isActive)
		{
			return false;
		}
		
		// Set inactive
		_isActive = false;
		
		// Cancel timer
		cancelQuestTimers("END_RABBITS_EVENT");
		
		// Despawn NPCs
		for (Npc npc : _npcs)
		{
			if (npc != null)
			{
				npc.deleteMe();
			}
		}
		
		_npcs.clear();
		
		for (Player player : _players)
		{
			if ((player != null) && (player.getTransformationId() == TRANSFORMATION_ID))
			{
				player.untransform();
			}
		}
		
		_players.clear();
		
		// Announce event end
		World.broadcastToAllOnlinePlayers("Rabbits Event: Event has finished.");
		return true;
	}
	
	/**
	 * Handles specific logic for different event types.<br>
	 * It manages transformations, player states, and event completion.
	 * @param event The unique identifier or name of the event to trigger.
	 * @param npc The {@code Npc} involved in the current action.
	 * @param player The {@code Player} interacting with the event.
	 * @return A {@code String} containing the HTML text if applicable, otherwise {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "900101-1.htm":
			{
				htmltext = "900101-1.htm";
				break;
			}
			case "transform":
			{
				if (player.isTransformed())
				{
					player.untransform();
				}
				
				if (player.isSitting())
				{
					player.standUp();
				}
				
				RABBIT_TRANSFORMATION.getSkill().applyEffects(player, player);
				_players.add(player);
				break;
			}
			case "END_RABBITS_EVENT":
			{
				World.broadcastToAllOnlinePlayers("Rabbits Event: Time up!");
				eventStop();
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the interaction when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * It returns the filename of the dialogue to be displayed.
	 * @param npc The {@code Npc} that is being spoken to.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".htm";
	}
	
	/**
	 * Handles the logic when a player uses a specific skill on an {@code Npc}.<br>
	 * This method checks for special interactions based on the {@code Skill} used.<br>
	 * It can reveal invisible NPCs or trigger item drops and NPC removal.
	 * @param npc The {@code Npc} being interacted with.
	 * @param caster The {@code Player} who cast the skill.
	 * @param skill The {@code Skill} that was executed.
	 * @param targets A collection of {@code WorldObject} items affected by the skill.
	 * @param isSummon Indicates if the skill used was a summon.
	 */
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, Collection<WorldObject> targets, boolean isSummon)
	{
		final int skillId = skill.getId();
		if (skillId == RABBIT_TORNADO.getSkillId())
		{
			if (!npc.isInvisible() && targets.contains(npc))
			{
				dropItem(npc, caster, DROPLIST);
				npc.deleteMe();
				_npcs.remove(npc);
				
				if (_npcs.size() <= 1)
				{
					World.broadcastToAllOnlinePlayers("Rabbits Event: No more chests...");
					eventStop();
				}
			}
		}
		else if ((skillId == RABBIT_MAGIC_EYE.getSkillId()) && npc.isInvisible() && npc.isInsideRadius2D(caster, skill.getAffectRange()))
		{
			npc.setInvisible(false);
		}
	}
	
	/**
	 * Handles the logic when a player attacks an {@code Npc}.<br>
	 * It checks if the attack was made using a specific skill.<br>
	 * If the skill is not {@code RABBIT_TORNADO}, it applies a curse effect.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt.
	 * @param isSummon Whether the attack was performed by a summoned unit.
	 * @param skill The {@code Skill} used to perform the attack.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if (_isActive && ((skill == null) || (skill.getId() != RABBIT_TORNADO.getSkillId())))
		{
			RAID_CURSE.getSkill().applyEffects(npc, attacker);
		}
	}
	
	/**
	 * Handles the logic for dropping items from an {@code Npc}.<br>
	 * It selects a random item from the provided {@code droplist} based on weight.<br>
	 * The method then calls {@code npc.dropItem} to give the item to the {@code Player}.
	 * @param npc The {@code Npc} that will drop the item.
	 * @param player The {@code Player} who will receive the item.
	 * @param droplist A 2D array where each row contains the item ID, weight, and quantity range.
	 */
	private void dropItem(Npc npc, Player player, int[][] droplist)
	{
		final int chance = getRandom(100);
		for (int[] drop : droplist)
		{
			if (chance > drop[1])
			{
				npc.dropItem(player, drop[0], getRandom(drop[2], drop[3]));
				return;
			}
		}
	}
	
	/**
	 * Registers a new {@code Npc} into the internal collection.<br>
	 * This method handles spawning the entity and configuring its initial state.<br>
	 * It specifically applies restrictions to chests such as making them invisible and immobile.
	 * @param npcs The collection where the spawned {@code Npc} will be stored.
	 * @param npcId The unique identifier for the {@code Npc} type.
	 * @param x The X coordinate for the spawn location.
	 * @param y The Y coordinate for the spawn location.
	 * @param z The Z coordinate for the spawn location.
	 * @param heading The initial rotation of the spawned entity.
	 * @param randomOffSet Whether to apply a random position offset.
	 * @param despawnDelay The time in milliseconds before the entity despawns.
	 */
	private void recordSpawn(Collection<Npc> npcs, int npcId, int x, int y, int z, int heading, boolean randomOffSet, long despawnDelay)
	{
		final Npc npc = addSpawn(npcId, x, y, z, heading, randomOffSet, despawnDelay);
		if (npc.getId() == CHEST)
		{
			npc.setImmobilized(true);
			npc.disableCoreAI(true);
			npc.setInvisible(true);
		}
		
		npcs.add(npc);
	}
	
	/**
	 * Checks if a specific action should be bypassed for a player.<br>
	 * This method currently always returns {@code false}.
	 * @param player The {@code Player} attempting the action.
	 * @param bypass The unique identifier for the bypass type.
	 * @return Always returns {@code false}.
	 */
	@Override
	public boolean eventBypass(Player player, String bypass)
	{
		return false;
	}
	
	/**
	 * This is the main entry point for the {@code Rabbits} class.<br>
	 * It initializes a new instance of the event.
	 * @param args The command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Rabbits();
	}
}
