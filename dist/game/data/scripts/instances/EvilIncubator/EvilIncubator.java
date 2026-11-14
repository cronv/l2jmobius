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
package instances.EvilIncubator;

import java.util.EnumMap;
import java.util.List;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.instance.FriendlyNpc;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.script.State;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.Earthquake;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class defines the logic for the {@code EvilIncubator} instance zone.<br>
 * It handles the scripts and mechanics required for this specific instance.
 * @author St3eT
 */
public class EvilIncubator extends InstanceScript
{
	// NPCs
	private static final int VANGUARD_MEMBER = 33165;
	private static final int ADOLPH = 33170;
	private static final int ALICE = 33171;
	private static final int BARTON = 33172;
	private static final int HAYUK = 33173;
	private static final int ELIYAH = 33174;
	private static final int[] MONSTERS =
	{
		27431, // Slayer
		27432, // Tracker
		27433, // Priest of Darkness
		27434, // Guardian of Darkness
		27430, // Screaming Shaman
	};
	private static final int[] BOSSES =
	{
		27425, // Death Wound
		27426, // Death Wound
		27427, // Death Wound
		27428, // Death Wound
		27429, // Death Wound
	};
	private static final int[] HELPERS =
	{
		33170, // Adolph
		// 33171, // Priest Alice
		33172, // Officer Barton
		33173, // Sniper Hayuk
		33174, // Magus Eliyah
		33414, // Van Archer
		33415, // Van Infantry
	};
	
	// Items
	private static final int SOE = 736; // Scroll of Escape
	
	// Locations
	private static final Location PLAYER_BATTLE_LOC = new Location(56167, -175615, -7944);
	private static final Location BATTLE_LOC_2 = new Location(56005, -175536, -7952);
	private static final Location BATTLE_LOC_3 = new Location(56325, -175536, -7952);
	private static final Location BOSS_LOC = new Location(56165, -177550, -7944);
	
	// Misc
	private static final int TEMPLATE_ID = 185;
	// @formatter:off
	// private static final EnumMap<Race, String> RACE_QUESTS = new EnumMap<>(Race.class);
	// static
	// {
	// 	RACE_QUESTS.put(Race.HUMAN, Q10341_DayOfDestinyHumansFate.class.getSimpleName());
	// 	RACE_QUESTS.put(Race.ELF, Q10342_DayOfDestinyElvenFate.class.getSimpleName());
	// 	RACE_QUESTS.put(Race.DARK_ELF, Q10343_DayOfDestinyDarkElfsFate.class.getSimpleName());
	// 	RACE_QUESTS.put(Race.ORC, Q10344_DayOfDestinyOrcsFate.class.getSimpleName());
	// 	RACE_QUESTS.put(Race.DWARF, Q10345_DayOfDestinyDwarfsFate.class.getSimpleName());
	// 	RACE_QUESTS.put(Race.KAMAEL, Q10346_DayOfDestinyKamaelsFate.class.getSimpleName());
	// }
	
	private static final EnumMap<PlayerClass, Integer> CLASS_BOSS = new EnumMap<>(PlayerClass.class);
	static
	{
		CLASS_BOSS.put(PlayerClass.GLADIATOR, 27426);
		CLASS_BOSS.put(PlayerClass.WARLORD, 27426);
		CLASS_BOSS.put(PlayerClass.PALADIN, 27425);
		CLASS_BOSS.put(PlayerClass.DARK_AVENGER, 27425);
		CLASS_BOSS.put(PlayerClass.TREASURE_HUNTER, 27426);
		CLASS_BOSS.put(PlayerClass.HAWKEYE, 27428);
		CLASS_BOSS.put(PlayerClass.SORCERER, 27428);
		CLASS_BOSS.put(PlayerClass.NECROMANCER, 27428);
		CLASS_BOSS.put(PlayerClass.WARLOCK, 27428);
		CLASS_BOSS.put(PlayerClass.BISHOP, 27427);
		CLASS_BOSS.put(PlayerClass.PROPHET, 27429);
		CLASS_BOSS.put(PlayerClass.TEMPLE_KNIGHT, 27425);
		CLASS_BOSS.put(PlayerClass.SWORDSINGER, 27429);
		CLASS_BOSS.put(PlayerClass.PLAINS_WALKER, 27426);
		CLASS_BOSS.put(PlayerClass.SILVER_RANGER, 27428);
		CLASS_BOSS.put(PlayerClass.SPELLSINGER, 27428);
		CLASS_BOSS.put(PlayerClass.ELEMENTAL_SUMMONER, 27428);
		CLASS_BOSS.put(PlayerClass.ELDER, 27427);
		CLASS_BOSS.put(PlayerClass.SHILLIEN_KNIGHT, 27425);
		CLASS_BOSS.put(PlayerClass.BLADEDANCER, 27429);
		CLASS_BOSS.put(PlayerClass.ABYSS_WALKER, 27426);
		CLASS_BOSS.put(PlayerClass.PHANTOM_RANGER, 27428);
		CLASS_BOSS.put(PlayerClass.SPELLHOWLER, 27428);
		CLASS_BOSS.put(PlayerClass.PHANTOM_SUMMONER, 27428);
		CLASS_BOSS.put(PlayerClass.SHILLIEN_ELDER, 27427);
		CLASS_BOSS.put(PlayerClass.DESTROYER, 27426);
		CLASS_BOSS.put(PlayerClass.TYRANT, 27426);
		CLASS_BOSS.put(PlayerClass.OVERLORD, 27429);
		CLASS_BOSS.put(PlayerClass.WARCRYER, 27429);
		CLASS_BOSS.put(PlayerClass.BOUNTY_HUNTER, 27426);
		CLASS_BOSS.put(PlayerClass.WARSMITH, 27426);
		CLASS_BOSS.put(PlayerClass.BERSERKER, 27426);
		CLASS_BOSS.put(PlayerClass.MALE_SOULBREAKER, 27426);
		CLASS_BOSS.put(PlayerClass.FEMALE_SOULBREAKER, 27426);
		CLASS_BOSS.put(PlayerClass.ARBALESTER, 27428);
	}
	private static final EnumMap<PlayerClass, Integer> CLASS_ITEMS = new EnumMap<>(PlayerClass.class);
	static
	{
		CLASS_ITEMS.put(PlayerClass.GLADIATOR, 17484);
		CLASS_ITEMS.put(PlayerClass.WARLORD, 17485);
		CLASS_ITEMS.put(PlayerClass.PALADIN, 17486);
		CLASS_ITEMS.put(PlayerClass.DARK_AVENGER, 17487);
		CLASS_ITEMS.put(PlayerClass.TREASURE_HUNTER, 17488);
		CLASS_ITEMS.put(PlayerClass.HAWKEYE, 17489);
		CLASS_ITEMS.put(PlayerClass.SORCERER, 17490);
		CLASS_ITEMS.put(PlayerClass.NECROMANCER, 17491);
		CLASS_ITEMS.put(PlayerClass.WARLOCK, 17492);
		CLASS_ITEMS.put(PlayerClass.BISHOP, 17493);
		CLASS_ITEMS.put(PlayerClass.PROPHET, 17494);
		CLASS_ITEMS.put(PlayerClass.TEMPLE_KNIGHT, 17495);
		CLASS_ITEMS.put(PlayerClass.SWORDSINGER, 17496);
		CLASS_ITEMS.put(PlayerClass.PLAINS_WALKER, 17497);
		CLASS_ITEMS.put(PlayerClass.SILVER_RANGER, 17498);
		CLASS_ITEMS.put(PlayerClass.SPELLSINGER, 17499);
		CLASS_ITEMS.put(PlayerClass.ELEMENTAL_SUMMONER, 17500);
		CLASS_ITEMS.put(PlayerClass.ELDER, 17501);
		CLASS_ITEMS.put(PlayerClass.SHILLIEN_KNIGHT, 17502);
		CLASS_ITEMS.put(PlayerClass.BLADEDANCER, 17503);
		CLASS_ITEMS.put(PlayerClass.ABYSS_WALKER, 17504);
		CLASS_ITEMS.put(PlayerClass.PHANTOM_RANGER, 17505);
		CLASS_ITEMS.put(PlayerClass.SPELLHOWLER, 17506);
		CLASS_ITEMS.put(PlayerClass.PHANTOM_SUMMONER, 17507);
		CLASS_ITEMS.put(PlayerClass.SHILLIEN_ELDER, 17508);
		CLASS_ITEMS.put(PlayerClass.DESTROYER, 17509);
		CLASS_ITEMS.put(PlayerClass.TYRANT, 17510);
		CLASS_ITEMS.put(PlayerClass.OVERLORD, 17511);
		CLASS_ITEMS.put(PlayerClass.WARCRYER, 17512);
		CLASS_ITEMS.put(PlayerClass.BOUNTY_HUNTER, 17513);
		CLASS_ITEMS.put(PlayerClass.WARSMITH, 17514);
		CLASS_ITEMS.put(PlayerClass.BERSERKER, 17515);
		CLASS_ITEMS.put(PlayerClass.MALE_SOULBREAKER, 17516);
		CLASS_ITEMS.put(PlayerClass.FEMALE_SOULBREAKER, 17516);
		CLASS_ITEMS.put(PlayerClass.ARBALESTER, 17517);
	}
	// @formatter:on
	
	/**
	 * Initializes the {@code EvilIncubator} instance script.<br>
	 * This constructor sets up the starting NPCs and talk IDs.<br>
	 * It also registers the creature kill events for monsters and bosses.
	 */
	public EvilIncubator()
	{
		super(TEMPLATE_ID);
		addStartNpc(VANGUARD_MEMBER);
		addTalkId(VANGUARD_MEMBER, ADOLPH, ALICE, BARTON, HAYUK, ELIYAH);
		addFirstTalkId(ADOLPH, ALICE, BARTON, HAYUK, ELIYAH);
		setCreatureKillId(this::onCreatureKill, MONSTERS);
		setCreatureKillId(this::onCreatureKill, BOSSES);
	}
	
	/**
	 * Handles various script events for the Evil Incubator instance.<br>
	 * It checks the current quest state before processing any logic.<br>
	 * The method manages transitions, NPC teleportation, and item rewards.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} involved in the event.
	 * @param player The {@code Player} interacting with the script.
	 * @return The HTML text to display or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState st = getQuestState(player);
		if ((st == null) || !st.isStarted())
		{
			return super.onEvent(event, npc, player);
		}
		
		String htmltext = null;
		if (event.equals("enterInstance"))
		{
			enterInstance(player, npc, TEMPLATE_ID);
			st.setCond(5);
			htmltext = "33165-01.html";
		}
		else
		{
			final Instance world = npc.getInstanceWorld();
			if (world != null)
			{
				switch (event)
				{
					case "33170-02.html":
					{
						if (st.isCond(5))
						{
							st.setCond(6, true);
							htmltext = event;
						}
						break;
					}
					case "selectHelper":
					{
						int helperCount = world.getParameters().getInt("HELPER_COUNT", 0);
						if ((helperCount < 2) && st.isCond(6))
						{
							helperCount++;
							world.setParameter("HELPER_COUNT", helperCount);
							npc.teleToLocation(helperCount == 1 ? BATTLE_LOC_2 : BATTLE_LOC_3);
							if (helperCount == 2)
							{
								st.setCond(7, true);
								World.forEachVisibleObjectInRange(world.getNpc(ADOLPH), FriendlyNpc.class, 1000, FriendlyNpc::deleteMe);
							}
						}
						break;
					}
					case "teamSelected":
					{
						int helperCount = world.getParameters().getInt("HELPER_COUNT", 0);
						if ((helperCount == 2) && st.isCond(7))
						{
							helperCount++;
							world.setParameter("HELPER_COUNT", helperCount);
							st.setCond(8, true);
							npc.teleToLocation(PLAYER_BATTLE_LOC);
							player.teleToLocation(PLAYER_BATTLE_LOC);
							getTimers().addTimer("SPAWN_WAVE", 5000, e -> manageWaveSpawn(world));
						}
						break;
					}
					case "giveItem":
					{
						if (st.isCond(9))
						{
							st.setCond(10, true);
							final PlayerClass playerClass = player.getPlayerClass();
							if (CLASS_ITEMS.containsKey(playerClass))
							{
								final int classItemId = CLASS_ITEMS.get(playerClass).intValue();
								if (!hasQuestItems(player, classItemId))
								{
									giveItems(player, classItemId, 1);
								}
							}
							
							npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THIS_CRY_OF_DESTINY_ARTIFACT_WILL_BE_HELPFUL_TO_YOU_PLEASE_USE_IT_IN_THE_NEXT_BATTLE);
							htmltext = "33170-06.html";
						}
						break;
					}
					case "resumeFight":
					{
						if (st.isCond(10))
						{
							st.setCond(11, true);
							manageWaveSpawn(world);
						}
						break;
					}
				}
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the dialogue logic when a player talks to an {@code Npc}.<br>
	 * It checks the current quest state of the {@code Player}.<br>
	 * It returns specific HTML text based on the NPC ID and quest conditions.
	 * @param npc The {@code Npc} that was interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return The HTML string to be displayed to the player.
	 */
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState st = getQuestState(player);
		if ((st == null) || !st.isStarted())
		{
			return super.onTalk(npc, player);
		}
		
		String htmltext = getNoQuestMsg(player);
		if (st.getState() == State.STARTED)
		{
			final int npcId = npc.getId();
			switch (npcId)
			{
				case ADOLPH:
				{
					switch (st.getCond())
					{
						case 5:
						{
							htmltext = npcId + "-01.html";
							break;
						}
						case 6:
						{
							htmltext = "33170-02.html";
							break;
						}
						case 7:
						{
							htmltext = "33170-03.html";
							break;
						}
						case 9:
						{
							htmltext = "33170-05.html";
							break;
						}
						case 10:
						{
							htmltext = "33170-07.html";
							break;
						}
						case 12:
						{
							st.setCond(13, true);
							giveItems(player, SOE, 1);
							break;
						}
						case 13:
						{
							htmltext = "33170-08.html";
							break;
						}
					}
					break;
				}
				case ALICE:
				case BARTON:
				case ELIYAH:
				case HAYUK:
				{
					switch (st.getCond())
					{
						case 5:
						{
							htmltext = npcId + "-01.html";
							break;
						}
						case 6:
						{
							htmltext = npcId + "-02.html";
							break;
						}
					}
					break;
				}
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the dialogue for a player's first interaction with an {@code Npc}.<br>
	 * This method checks the current quest state to determine which HTML file to display.<br>
	 * It returns the filename based on the specific NPC and progress conditions.
	 * @param npc The {@code Npc} that is being spoken to.
	 * @param player The {@code Player} who is initiating the conversation.
	 * @return The name of the HTML file to display, or {@code null} if the quest has not started.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = null;
		final QuestState st = getQuestState(player);
		if ((st == null) || !st.isStarted())
		{
			return htmltext;
		}
		
		switch (npc.getId())
		{
			case ADOLPH:
			{
				switch (st.getCond())
				{
					case 5:
					case 6:
					case 7:
					case 9:
					case 10:
					case 12:
					case 13:
					{
						htmltext = "33170.html";
						break;
					}
					default:
					{
						htmltext = "33170-04.html";
						break;
					}
				}
				break;
			}
			case ALICE:
			case BARTON:
			case ELIYAH:
			case HAYUK:
			{
				final int npcId = npc.getId();
				htmltext = st.getCond() > 7 ? (npcId + "-03.html") : (npcId + ".html");
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles logic when a creature dies in the instance.<br>
	 * It checks if the dead creature is a boss to update quest conditions.<br>
	 * It also manages wave spawning and world attacks based on current progress.
	 * @param event The {@code OnCreatureDeath} event containing the target creature.
	 */
	public void onCreatureKill(OnCreatureDeath event)
	{
		final Npc npc = event.getTarget().asNpc();
		final Instance world = npc.getInstanceWorld();
		if (world != null)
		{
			final int waveId = world.getParameters().getInt("WORLD_WAVE", 1);
			if (ArrayUtil.contains(BOSSES, npc.getId()))
			{
				final QuestState st = getQuestState(world.getFirstPlayer());
				if ((st == null) || !st.isStarted())
				{
					return;
				}
				
				st.setCond(12, true);
			}
			else if (waveId < 8)
			{
				if (world.getAliveNpcCount(MONSTERS) == 0)
				{
					getTimers().addTimer("SPAWN_WAVE", 5000, e -> manageWaveSpawn(world));
				}
			}
			
			getTimers().addTimer("WORLD_ATTACK", 1000, e -> managerWorldAttack(world, null));
		}
	}
	
	/**
	 * Handles logic for specific timed events within the instance.<br>
	 * This method is triggered by the script timer system.
	 * @param event The name of the timer event to process.
	 * @param params A {@code StatSet} containing data required for the event.
	 * @param npc The {@code Npc} associated with the event.
	 * @param player The {@code Player} involved in the event.
	 */
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
	}
	
	/**
	 * Handles the logic for spawning waves of enemies within the instance.<br>
	 * It checks the current quest state and the wave ID from the world parameters.<br>
	 * The method spawns specific NPC groups based on the current wave number.<br>
	 * It also updates the wave counter and displays messages to the players.
	 * @param world The {@code Instance} object representing the current game world.
	 */
	private void manageWaveSpawn(Instance world)
	{
		final QuestState st = getQuestState(world.getFirstPlayer());
		if ((st == null) || !st.isStarted())
		{
			return;
		}
		
		List<Npc> spawnedNpcs = null;
		int waveId = world.getParameters().getInt("WORLD_WAVE", 1);
		switch (waveId)
		{
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			{
				spawnedNpcs = world.spawnGroup("wave_" + waveId);
				waveId++;
				world.setParameter("WORLD_WAVE", waveId);
				showOnScreenMsg(world, NpcStringId.EVIL_INCUBATOR_GET_READY_TO_FIGHT_EVIL_FORCES, ExShowScreenMessage.TOP_CENTER, 5000, true);
				break;
			}
			case 7:
			{
				waveId++;
				world.setParameter("WORLD_WAVE", waveId);
				showOnScreenMsg(world, NpcStringId.CREATURES_HAVE_STOPPED_THEIR_ATTACK_REST_AND_THEN_SPEAK_WITH_ADOLPH, ExShowScreenMessage.TOP_CENTER, 5000, true);
				st.setCond(9, true);
				break;
			}
			case 8:
			case 9:
			case 10:
			case 11:
			case 12:
			case 13:
			case 14:
			{
				spawnedNpcs = world.spawnGroup("wave_" + waveId);
				waveId++;
				world.setParameter("WORLD_WAVE", waveId);
				showOnScreenMsg(world, NpcStringId.EVIL_INCUBATOR_GET_READY_TO_FIGHT_EVIL_FORCES, ExShowScreenMessage.TOP_CENTER, 5000, true);
				getTimers().addTimer("SPAWN_WAVE", 20000, e -> manageWaveSpawn(world));
				break;
			}
			case 15:
			{
				spawnedNpcs = world.spawnGroup("wave_" + waveId);
				waveId++;
				world.setParameter("WORLD_WAVE", waveId);
				showOnScreenMsg(world, NpcStringId.I_SHALL_GRANT_YOU_DEATH_IT_S_THE_ONLY_GIFT_FROM_THE_GODDESS_OF_DESTRUCTION, ExShowScreenMessage.TOP_CENTER, 5000, true);
				getTimers().addTimer("SPAWN_WAVE", 20000, e -> manageWaveSpawn(world));
				final Npc boss = addSpawn(CLASS_BOSS.get(world.getFirstPlayer().getPlayerClass()).intValue(), BOSS_LOC, false, 0, false, world.getId());
				world.broadcastPacket(new Earthquake(boss, 50, 10));
				break;
			}
		}
		
		managerWorldAttack(world, spawnedNpcs);
	}
	
	/**
	 * Manages the combat interactions between spawned NPCs and friendly helpers.<br>
	 * It applies damage to specific targets based on proximity and helper types.<br>
	 * This method also updates attack desires for all visible enemies.
	 * @param world The {@code Instance} where the action takes place.
	 * @param spawnedNpcs A list of {@code Npc} objects that were recently spawned.
	 */
	private void managerWorldAttack(Instance world, List<Npc> spawnedNpcs)
	{
		final List<FriendlyNpc> helperList = World.getVisibleObjects(world.getFirstPlayer(), FriendlyNpc.class);
		if ((spawnedNpcs != null) && !spawnedNpcs.isEmpty())
		{
			for (Npc npc : spawnedNpcs)
			{
				if (!helperList.isEmpty())
				{
					final FriendlyNpc helper = helperList.get(getRandom(helperList.size()));
					if (ArrayUtil.contains(HELPERS, helper.getId()))
					{
						npc.reduceCurrentHp(1, helper, null);
						helper.reduceCurrentHp(1, npc, null);
						addAttackDesire(helper, npc);
						helperList.remove(helper);
					}
				}
			}
		}
		
		for (FriendlyNpc helper : helperList)
		{
			World.forEachVisibleObject(helper, Attackable.class, monster ->
			{
				if (!(monster instanceof FriendlyNpc))
				{
					addAttackDesire(helper, monster);
				}
			});
		}
	}
	
	/**
	 * Retrieves the current quest state for a specific player.<br>
	 * It checks if the player has an active race-based quest.<br>
	 * Returns {@code null} if no quest is found or if the player is {@code null}.
	 * @param player The {@code Player} to check for quest progress.
	 * @return The current {@code QuestState} of the player, or {@code null}.
	 */
	private QuestState getQuestState(Player player)
	{
		if (player == null)
		{
			return null;
		}
		
		// return RACE_QUESTS.containsKey(player.getRace()) ? player.getQuestState(RACE_QUESTS.get(player.getRace())) : null;
		return null;
	}
	
	/**
	 * This is the entry point for the {@code EvilIncubator} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new EvilIncubator();
	}
}
