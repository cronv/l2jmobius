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
package ai.bosses.Frintezza;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.Earthquake;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillCanceled;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcInfo;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.SpecialCamera;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * Handles the artificial intelligence for the boss in the Last Imperial Tomb.<br>
 * This class is a reworked version of the original L2J implementation.<br>
 * It manages combat behaviors and special mechanics for this specific instance.
 * @author Mobius, Trevor The Third
 */
public class LastImperialTomb extends InstanceScript
{
	// NPCs
	private static final int GUIDE = 34543;
	private static final int CUBE = 29061;
	private static final int HALL_ALARM = 18328;
	private static final int DUMMY = 29052;
	private static final int DUMMY2 = 29053;
	private static final int FRINTEZZA = 29045;
	private static final int SCARLET1 = 29046;
	private static final int SCARLET2 = 29047;
	private static final int PORTRAIT_1 = 29048;
	private static final int PORTRAIT_2 = 29049;
	private static final int[] PORTRAITS =
	{
		PORTRAIT_1,
		PORTRAIT_2
	};
	private static final int DEMON_1 = 29050;
	private static final int DEMON_2 = 29051;
	private static final int[] DEMONS =
	{
		DEMON_1,
		DEMON_2
	};
	
	// Items
	private static final int FIRST_SCARLET_WEAPON = 8204;
	private static final int SECOND_SCARLET_WEAPON = 7903; // 8222?
	
	// Doors
	private static final int[] FIRST_ROOM_DOORS =
	{
		17130051,
		17130052,
		17130053,
		17130054,
		17130055,
		17130056,
		17130057,
		17130058
	};
	private static final int[] SECOND_ROOM_DOORS =
	{
		17130061,
		17130062,
		17130063,
		17130064,
		17130065,
		17130066,
		17130067,
		17130068,
		17130069,
		17130070
	};
	private static final int[] FIRST_ROUTE_DOORS =
	{
		17130042,
		17130043
	};
	private static final int[] SECOND_ROUTE_DOORS =
	{
		17130045,
		17130046
	};
	
	// Skills
	private static final int DEWDROP_OF_DESTRUCTION_SKILL_ID = 2276;
	private static final int SOUL_BREAKING_ARROW_SKILL_ID = 2234;
	private static final SkillHolder INTRO_SKILL = new SkillHolder(5004, 1);
	private static final SkillHolder FIRST_MORPH_SKILL = new SkillHolder(5017, 1);
	private static final Map<Integer, NpcStringId> SKILL_MSG = new HashMap<>();
	static
	{
		SKILL_MSG.put(1, NpcStringId.REQUIEM_OF_HATRED);
		SKILL_MSG.put(2, NpcStringId.RONDO_OF_SOLITUDE);
		SKILL_MSG.put(3, NpcStringId.FRENETIC_TOCCATA);
		SKILL_MSG.put(4, NpcStringId.FUGUE_OF_RAPTURE);
		SKILL_MSG.put(5, NpcStringId.HYPNOTIC_MAZURKA);
	}
	
	// Spawns
	static final int[][] PORTRAIT_SPAWNS =
	{
		// @formatter:off
		{29048, -89381, -153981, -9168, 3368, -89378, -153968, -9168, 3368},
		{29048, -86234, -152467, -9168, 37656, -86261, -152492, -9168, 37656},
		{29049, -89342, -152479, -9168, -5152, -89311, -152491, -9168, -5152},
		{29049, -86189, -153968, -9168, 29456, -86217, -153956, -9168, 29456},
		// @formatter:on
	};
	
	// Misc
	private static final int TEMPLATE_ID = 302;
	private static final int FRINTEZZA_WAIT_TIME = 1; // minutes
	private static final int RANDOM_SONG_INTERVAL = 90; // seconds
	private static final int SONG_BREAK_TIME = 35; // seconds without a song after Soul Breaking Arrow
	private static final int SONG_PAUSE_TIME = 35; // seconds without a song after one ends
	private static final int TIME_BETWEEN_DEMON_SPAWNS = 20; // seconds
	private static final int MAX_DEMONS = 24;
	
	/**
	 * Initializes the {@code LastImperialTomb} instance.<br>
	 * Sets up all required NPCs, talk IDs, and attack IDs.<br>
	 * Configures kill effects and spawn behaviors for the dungeon.
	 */
	public LastImperialTomb()
	{
		super(TEMPLATE_ID);
		addTalkId(GUIDE, CUBE);
		addAttackId(SCARLET1);
		addSkillSeeId(PORTRAITS);
		addSkillSeeId(FRINTEZZA);
		addKillId(HALL_ALARM, SCARLET2);
		addKillId(PORTRAITS);
		addKillId(DEMONS);
		addSpawnId(HALL_ALARM, DUMMY, DUMMY2);
	}
	
	/**
	 * Handles specific scripted events for the Frintezza instance.<br>
	 * It manages quest timers, door states, NPC spawning, and camera movements.
	 * @param event The unique identifier for the script event to execute.
	 * @param npc The {@code Npc} entity associated with the current action.
	 * @param player The {@code Player} who triggered or is viewing the event.
	 * @return A string representing the result of the event processing.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "FRINTEZZA_INTRO_START":
			{
				final Instance world = player.getInstanceWorld();
				startQuestTimer("FRINTEZZA_INTRO_1", 17000, null, player, false);
				startQuestTimer("FRINTEZZA_INTRO_2", 20000, null, player, false);
				broadcastPacket(world, new Earthquake(-87784, -155083, -9087, 45, 27));
				break;
			}
			case "FRINTEZZA_INTRO_1":
			{
				final Instance world = player.getInstanceWorld();
				for (int doorId : FIRST_ROOM_DOORS)
				{
					world.openCloseDoor(doorId, false);
				}
				
				for (int doorId : FIRST_ROUTE_DOORS)
				{
					world.openCloseDoor(doorId, false);
				}
				
				for (int doorId : SECOND_ROOM_DOORS)
				{
					world.openCloseDoor(doorId, false);
				}
				
				for (int doorId : SECOND_ROUTE_DOORS)
				{
					world.openCloseDoor(doorId, false);
				}
				
				addSpawn(CUBE, -87904, -141296, -9168, 0, false, 0, false, world.getId());
				break;
			}
			case "FRINTEZZA_INTRO_2":
			{
				final Instance world = player.getInstanceWorld();
				final Npc frintezzaDummy = addSpawn(DUMMY, -87784, -155083, -9087, 16048, false, 0, false, world.getId());
				world.setParameter("frintezzaDummy", frintezzaDummy);
				
				final Npc overheadDummy = addSpawn(DUMMY, -87784, -153298, -9175, 16384, false, 0, false, world.getId());
				overheadDummy.setCollisionHeight(600);
				broadcastPacket(world, new NpcInfo(overheadDummy));
				world.setParameter("overheadDummy", overheadDummy);
				
				final Npc portraitDummy1 = addSpawn(DUMMY, -89566, -153168, -9165, 16048, false, 0, false, world.getId());
				world.setParameter("portraitDummy1", portraitDummy1);
				
				final Npc portraitDummy3 = addSpawn(DUMMY, -86004, -153168, -9165, 16048, false, 0, false, world.getId());
				world.setParameter("portraitDummy3", portraitDummy3);
				
				final Npc scarletDummy = addSpawn(DUMMY2, -87784, -153298, -9175, 16384, false, 0, false, world.getId());
				world.setParameter("scarletDummy", scarletDummy);
				disablePlayers(world);
				
				// broadcastPacket(world, new SpecialCamera(overheadDummy, 0, 75, -89, 0, 100, 0, 0, 1, 0, 0));
				broadcastPacket(world, new SpecialCamera(overheadDummy, 0, 75, 89, 0, 100, 0, 0, 1, 0, 0));
				broadcastPacket(world, new SpecialCamera(overheadDummy, 300, 90, 10, 6500, 7000, 0, 0, 1, 0, 0));
				
				final Npc frintezza = addSpawn(FRINTEZZA, -87780, -155086, -9080, 16384, false, 0, false, world.getId());
				frintezza.setImmobilized(true);
				frintezza.setInvul(true);
				frintezza.disableAllSkills();
				world.setParameter("frintezza", frintezza);
				
				final List<Monster> demons = new ArrayList<>();
				for (int[] element : PORTRAIT_SPAWNS)
				{
					final Monster demon = addSpawn(element[0] + 2, element[5], element[6], element[7], element[8], false, 0, false, world.getId()).asMonster();
					demon.setImmobilized(true);
					demon.disableAllSkills();
					demons.add(demon);
				}
				
				world.setParameter("demons", demons);
				startQuestTimer("FRINTEZZA_INTRO_3", 6500, null, player, false);
				break;
			}
			case "FRINTEZZA_INTRO_3":
			{
				final Instance world = player.getInstanceWorld();
				final Npc frintezzaDummy = world.getParameters().getObject("frintezzaDummy", Npc.class);
				broadcastPacket(world, new SpecialCamera(frintezzaDummy, 1800, 90, 8, 6500, 7000, 0, 0, 1, 0, 0));
				startQuestTimer("FRINTEZZA_INTRO_4", 900, null, player, false);
				break;
			}
			case "FRINTEZZA_INTRO_4":
			{
				final Instance world = player.getInstanceWorld();
				final Npc frintezzaDummy = world.getParameters().getObject("frintezzaDummy", Npc.class);
				broadcastPacket(world, new SpecialCamera(frintezzaDummy, 140, 90, 10, 2500, 4500, 0, 0, 1, 0, 0));
				startQuestTimer("FRINTEZZA_INTRO_5", 4000, null, player, false);
				break;
			}
			case "FRINTEZZA_INTRO_5":
			{
				final Instance world = player.getInstanceWorld();
				final Npc frintezza = world.getParameters().getObject("frintezza", Npc.class);
				broadcastPacket(world, new SpecialCamera(frintezza, 40, 75, 10, 0, 1000, 0, 0, 1, 0, 0));
				broadcastPacket(world, new SpecialCamera(frintezza, 40, 75, 10, 0, 12000, 0, 0, 1, 0, 0));
				startQuestTimer("FRINTEZZA_INTRO_6", 1350, null, player, false);
				break;
			}
			case "FRINTEZZA_INTRO_6":
			{
				final Instance world = player.getInstanceWorld();
				final Npc frintezza = world.getParameters().getObject("frintezza", Npc.class);
				broadcastPacket(world, new SocialAction(frintezza.getObjectId(), 2));
				final Npc frintezzaDummy = world.getParameters().getObject("frintezzaDummy", Npc.class);
				frintezzaDummy.deleteMe();
				startQuestTimer("FRINTEZZA_INTRO_7", 8000, null, player, false);
				break;
			}
			case "FRINTEZZA_INTRO_7":
			{
				final Instance world = player.getInstanceWorld();
				final List<Monster> demons = world.getParameters().getList("demons", Monster.class);
				broadcastPacket(world, new SocialAction(demons.get(1).getObjectId(), 1));
				broadcastPacket(world, new SocialAction(demons.get(2).getObjectId(), 1));
				startQuestTimer("FRINTEZZA_INTRO_8", 400, null, player, false);
				break;
			}
			case "FRINTEZZA_INTRO_8":
			{
				final Instance world = player.getInstanceWorld();
				final List<Monster> demons = world.getParameters().getList("demons", Monster.class);
				final Npc portraitDummy1 = world.getParameters().getObject("portraitDummy1", Npc.class);
				final Npc portraitDummy3 = world.getParameters().getObject("portraitDummy3", Npc.class);
				broadcastPacket(world, new SocialAction(demons.get(0).getObjectId(), 1));
				broadcastPacket(world, new SocialAction(demons.get(3).getObjectId(), 1));
				sendPacketX(world, new SpecialCamera(portraitDummy1, 1000, 118, 0, 0, 1000, 0, 0, 1, 0, 0), new SpecialCamera(portraitDummy3, 1000, 62, 0, 0, 1000, 0, 0, 1, 0, 0), -87784);
				sendPacketX(world, new SpecialCamera(portraitDummy1, 1000, 118, 0, 0, 10000, 0, 0, 1, 0, 0), new SpecialCamera(portraitDummy3, 1000, 62, 0, 0, 10000, 0, 0, 1, 0, 0), -87784);
				startQuestTimer("FRINTEZZA_INTRO_9", 2000, null, player, false);
				break;
			}
			case "FRINTEZZA_INTRO_9":
			{
				final Instance world = player.getInstanceWorld();
				final Npc frintezza = world.getParameters().getObject("frintezza", Npc.class);
				final Npc portraitDummy1 = world.getParameters().getObject("portraitDummy1", Npc.class);
				final Npc portraitDummy3 = world.getParameters().getObject("portraitDummy3", Npc.class);
				broadcastPacket(world, new SpecialCamera(frintezza, 240, 90, 0, 0, 1000, 0, 0, 1, 0, 0));
				broadcastPacket(world, new SpecialCamera(frintezza, 240, 90, 25, 5500, 10000, 0, 0, 1, 0, 0));
				broadcastPacket(world, new SocialAction(frintezza.getObjectId(), 3));
				portraitDummy1.deleteMe();
				portraitDummy3.deleteMe();
				startQuestTimer("FRINTEZZA_INTRO_10", 4500, null, player, false);
				break;
			}
			case "FRINTEZZA_INTRO_10":
			{
				final Instance world = player.getInstanceWorld();
				final Npc frintezza = world.getParameters().getObject("frintezza", Npc.class);
				broadcastPacket(world, new SpecialCamera(frintezza, 100, 195, 35, 0, 10000, 0, 0, 1, 0, 0));
				startQuestTimer("FRINTEZZA_INTRO_11", 700, null, player, false);
				break;
			}
			case "FRINTEZZA_INTRO_11":
			{
				final Instance world = player.getInstanceWorld();
				final Npc frintezza = world.getParameters().getObject("frintezza", Npc.class);
				broadcastPacket(world, new SpecialCamera(frintezza, 100, 195, 35, 0, 10000, 0, 0, 1, 0, 0));
				startQuestTimer("FRINTEZZA_INTRO_12", 1300, null, player, false);
				break;
			}
			case "FRINTEZZA_INTRO_12":
			{
				final Instance world = player.getInstanceWorld();
				final Npc frintezza = world.getParameters().getObject("frintezza", Npc.class);
				broadcastPacket(world, new ExShowScreenMessage(NpcStringId.MOURNFUL_CHORALE_PRELUDE, 2, 5000));
				broadcastPacket(world, new SpecialCamera(frintezza, 120, 180, 45, 1500, 10000, 0, 0, 1, 0, 0));
				broadcastPacket(world, new MagicSkillUse(frintezza, frintezza, 5006, 1, 34000, 0));
				startQuestTimer("FRINTEZZA_INTRO_13", 1500, null, player, false);
				break;
			}
			case "FRINTEZZA_INTRO_13":
			{
				final Instance world = player.getInstanceWorld();
				final Npc frintezza = world.getParameters().getObject("frintezza", Npc.class);
				broadcastPacket(world, new SpecialCamera(frintezza, 520, 135, 45, 8000, 10000, 0, 0, 1, 0, 0));
				startQuestTimer("FRINTEZZA_INTRO_14", 7500, null, player, false);
				break;
			}
			case "FRINTEZZA_INTRO_14":
			{
				final Instance world = player.getInstanceWorld();
				final Npc frintezza = world.getParameters().getObject("frintezza", Npc.class);
				broadcastPacket(world, new SpecialCamera(frintezza, 1500, 110, 25, 10000, 13000, 0, 0, 1, 0, 0));
				startQuestTimer("FRINTEZZA_INTRO_15", 9500, null, player, false);
				break;
			}
			case "FRINTEZZA_INTRO_15":
			{
				final Instance world = player.getInstanceWorld();
				final Npc overheadDummy = world.getParameters().getObject("overheadDummy", Npc.class);
				final Npc scarletDummy = world.getParameters().getObject("scarletDummy", Npc.class);
				broadcastPacket(world, new SpecialCamera(overheadDummy, 930, 160, 10, 0, 1000, 0, 0, 1, 0, 0));
				broadcastPacket(world, new SpecialCamera(overheadDummy, 1000, 180, 0, 0, 10000, 0, 0, 1, 0, 0));
				broadcastPacket(world, new MagicSkillUse(scarletDummy, overheadDummy, 5004, 1, 5800, 0));
				startQuestTimer("FRINTEZZA_INTRO_16", 5000, null, player, false);
				break;
			}
			case "FRINTEZZA_INTRO_16":
			{
				final Instance world = player.getInstanceWorld();
				final Npc scarletDummy = world.getParameters().getObject("scarletDummy", Npc.class);
				final Npc activeScarlet = addSpawn(SCARLET1, -87789, -153295, -9176, 16384, false, 0, false, world.getId());
				world.setParameter("activeScarlet", activeScarlet);
				activeScarlet.setRHandId(FIRST_SCARLET_WEAPON);
				activeScarlet.setInvul(true);
				activeScarlet.setImmobilized(true);
				activeScarlet.disableAllSkills();
				broadcastPacket(world, new SocialAction(activeScarlet.getObjectId(), 3));
				broadcastPacket(world, new SpecialCamera(scarletDummy, 800, 180, 10, 1000, 10000, 0, 0, 1, 0, 0));
				startQuestTimer("FRINTEZZA_INTRO_17", 2100, null, player, false);
				break;
			}
			case "FRINTEZZA_INTRO_17":
			{
				final Instance world = player.getInstanceWorld();
				final Npc activeScarlet = world.getParameters().getObject("activeScarlet", Npc.class);
				broadcastPacket(world, new SpecialCamera(activeScarlet, 300, 60, 8, 0, 10000, 0, 0, 1, 0, 0));
				startQuestTimer("FRINTEZZA_INTRO_18", 2000, null, player, false);
				break;
			}
			case "FRINTEZZA_INTRO_18":
			{
				final Instance world = player.getInstanceWorld();
				final Npc activeScarlet = world.getParameters().getObject("activeScarlet", Npc.class);
				broadcastPacket(world, new SpecialCamera(activeScarlet, 500, 90, 10, 3000, 5000, 0, 0, 1, 0, 0));
				world.setParameter("isPlayingSong", false);
				playRandomSong(world);
				startQuestTimer("FRINTEZZA_INTRO_19", 3000, null, player, false);
				break;
			}
			case "FRINTEZZA_INTRO_19":
			{
				final Instance world = player.getInstanceWorld();
				final Map<Npc, Integer> portraits = new HashMap<>();
				for (int i = 0; i < PORTRAIT_SPAWNS.length; i++)
				{
					final Npc portrait = addSpawn(PORTRAIT_SPAWNS[i][0], PORTRAIT_SPAWNS[i][1], PORTRAIT_SPAWNS[i][2], PORTRAIT_SPAWNS[i][3], PORTRAIT_SPAWNS[i][4], false, 0, false, world.getId());
					portraits.put(portrait, i);
				}
				
				world.setParameter("portraits", portraits);
				final Npc overheadDummy = world.getParameters().getObject("overheadDummy", Npc.class);
				final Npc scarletDummy = world.getParameters().getObject("scarletDummy", Npc.class);
				overheadDummy.deleteMe();
				scarletDummy.deleteMe();
				startQuestTimer("FRINTEZZA_INTRO_20", 2000, null, player, false);
				break;
			}
			case "FRINTEZZA_INTRO_20":
			{
				final Instance world = player.getInstanceWorld();
				final Npc frintezza = world.getParameters().getObject("frintezza", Npc.class);
				final Npc activeScarlet = world.getParameters().getObject("activeScarlet", Npc.class);
				final List<Monster> demons = world.getParameters().getList("demons", Monster.class);
				for (Npc demon : demons)
				{
					demon.setImmobilized(false);
					demon.enableAllSkills();
				}
				
				activeScarlet.setInvul(false);
				activeScarlet.setImmobilized(false);
				activeScarlet.enableAllSkills();
				activeScarlet.setRunning();
				activeScarlet.doCast(INTRO_SKILL.getSkill());
				frintezza.enableAllSkills();
				frintezza.disableCoreAI(true);
				frintezza.setInvul(true);
				enablePlayers(world);
				startQuestTimer("PLAY_RANDOM_SONG", RANDOM_SONG_INTERVAL * 1000, frintezza, null, false);
				startQuestTimer("SPAWN_DEMONS", TIME_BETWEEN_DEMON_SPAWNS * 1000, null, player, false);
				break;
			}
			case "SPAWN_DEMONS":
			{
				final Instance world = player.getInstanceWorld();
				if (world != null)
				{
					final Map<Npc, Integer> portraits = world.getParameters().getMap("portraits", Npc.class, Integer.class);
					if ((portraits != null) && !portraits.isEmpty())
					{
						final List<Monster> demons = world.getParameters().getList("demons", Monster.class);
						for (int i : portraits.values())
						{
							if (demons.size() > MAX_DEMONS)
							{
								break;
							}
							
							final Monster demon = addSpawn(PORTRAIT_SPAWNS[i][0] + 2, PORTRAIT_SPAWNS[i][5], PORTRAIT_SPAWNS[i][6], PORTRAIT_SPAWNS[i][7], PORTRAIT_SPAWNS[i][8], false, 0, false, world.getId()).asMonster();
							demons.add(demon);
						}
						
						world.setParameter("demons", demons);
						startQuestTimer("SPAWN_DEMONS", TIME_BETWEEN_DEMON_SPAWNS * 1000, null, player, false);
					}
				}
				break;
			}
			case "PLAY_RANDOM_SONG":
			{
				if (npc != null)
				{
					final Instance world = npc.getInstanceWorld();
					final Npc frintezza = world.getParameters().getObject("frintezza", Npc.class);
					
					// The next song follows 35 seconds after this one ends.
					final int songTime = playRandomSong(world);
					startQuestTimer("PLAY_RANDOM_SONG", songTime + (SONG_PAUSE_TIME * 1000), frintezza, null, false);
				}
				break;
			}
			case "SCARLET_FIRST_MORPH":
			{
				final Instance world = npc.getInstanceWorld();
				npc.doCast(FIRST_MORPH_SKILL.getSkill());
				playRandomSong(world);
				break;
			}
			case "SCARLET_SECOND_MORPH":
			{
				final Instance world = player.getInstanceWorld();
				disablePlayers(world);
				deactivateDemons(world);
				final Npc activeScarlet = world.getParameters().getObject("activeScarlet", Npc.class);
				final Npc frintezza = world.getParameters().getObject("frintezza", Npc.class);
				activeScarlet.abortAttack();
				activeScarlet.abortCast();
				activeScarlet.setInvul(true);
				activeScarlet.setImmobilized(true);
				activeScarlet.disableAllSkills();
				cancelQuestTimer("PLAY_RANDOM_SONG", frintezza, null);
				broadcastPacket(world, new MagicSkillCanceled(frintezza.getObjectId()));
				startQuestTimer("PLAY_RANDOM_SONG", RANDOM_SONG_INTERVAL * 1000, frintezza, null, false);
				startQuestTimer("SCARLET_SECOND_MORPH_CAMERA_1", 2000, null, player, false);
				break;
			}
			case "SCARLET_SECOND_MORPH_CAMERA_1":
			{
				final Instance world = player.getInstanceWorld();
				final Npc frintezza = world.getParameters().getObject("frintezza", Npc.class);
				broadcastPacket(world, new SocialAction(frintezza.getObjectId(), 4));
				broadcastPacket(world, new SpecialCamera(frintezza, 250, 120, 15, 0, 1000, 0, 0, 1, 0, 0));
				broadcastPacket(world, new SpecialCamera(frintezza, 250, 120, 15, 0, 10000, 0, 0, 1, 0, 0));
				startQuestTimer("SCARLET_SECOND_MORPH_CAMERA_2", 7000, null, player, false);
				break;
			}
			case "SCARLET_SECOND_MORPH_CAMERA_2":
			{
				final Instance world = player.getInstanceWorld();
				final Npc frintezza = world.getParameters().getObject("frintezza", Npc.class);
				broadcastPacket(world, new ExShowScreenMessage(NpcStringId.MOURNFUL_CHORALE_PRELUDE, 2, 5000));
				broadcastPacket(world, new SpecialCamera(frintezza, 500, 70, 15, 3000, 10000, 0, 0, 1, 0, 0));
				broadcastPacket(world, new MagicSkillUse(frintezza, frintezza, 5006, 1, 34000, 0));
				startQuestTimer("SCARLET_SECOND_MORPH_CAMERA_3", 3000, null, player, false);
				break;
			}
			case "SCARLET_SECOND_MORPH_CAMERA_3":
			{
				final Instance world = player.getInstanceWorld();
				final Npc frintezza = world.getParameters().getObject("frintezza", Npc.class);
				broadcastPacket(world, new SpecialCamera(frintezza, 2500, 90, 12, 6000, 10000, 0, 0, 1, 0, 0));
				startQuestTimer("SCARLET_SECOND_MORPH_CAMERA_4", 3000, null, player, false);
				break;
			}
			case "SCARLET_SECOND_MORPH_CAMERA_4":
			{
				final Instance world = player.getInstanceWorld();
				final Npc activeScarlet = world.getParameters().getObject("activeScarlet", Npc.class);
				final Location scarletLocation = activeScarlet.getLocation();
				int newHeading = 0;
				if (scarletLocation.getHeading() < 32768)
				{
					newHeading = Math.abs(180 - (int) (scarletLocation.getHeading() / 182.044444444));
				}
				else
				{
					newHeading = Math.abs(540 - (int) (scarletLocation.getHeading() / 182.044444444));
				}
				
				world.setParameter("scarletLocation", scarletLocation);
				world.setParameter("newHeading", newHeading);
				broadcastPacket(world, new SpecialCamera(activeScarlet, 250, newHeading, 12, 0, 1000, 0, 0, 1, 0, 0));
				broadcastPacket(world, new SpecialCamera(activeScarlet, 250, newHeading, 12, 0, 10000, 0, 0, 1, 0, 0));
				startQuestTimer("SCARLET_SECOND_MORPH_CAMERA_5", 500, null, player, false);
				break;
			}
			case "SCARLET_SECOND_MORPH_CAMERA_5":
			{
				final Instance world = player.getInstanceWorld();
				final Npc activeScarlet = world.getParameters().getObject("activeScarlet", Npc.class);
				final int newHeading = world.getParameters().getInt("newHeading");
				activeScarlet.doDie(activeScarlet);
				broadcastPacket(world, new SpecialCamera(activeScarlet, 450, newHeading, 14, 8000, 8000, 0, 0, 1, 0, 0));
				startQuestTimer("SCARLET_SECOND_MORPH_CAMERA_6", 6250, null, player, false);
				startQuestTimer("SCARLET_SECOND_MORPH_CAMERA_7", 7200, null, player, false);
				break;
			}
			case "SCARLET_SECOND_MORPH_CAMERA_6":
			{
				final Instance world = player.getInstanceWorld();
				final Npc activeScarlet = world.getParameters().getObject("activeScarlet", Npc.class);
				activeScarlet.deleteMe();
				break;
			}
			case "SCARLET_SECOND_MORPH_CAMERA_7":
			{
				final Instance world = player.getInstanceWorld();
				final int newHeading = world.getParameters().getInt("newHeading");
				final Location scarletLocation = world.getParameters().getLocation("scarletLocation");
				final Npc activeScarlet = addSpawn(SCARLET2, scarletLocation, false, 0, false, world.getId());
				world.setParameter("activeScarlet", activeScarlet);
				activeScarlet.setLHandId(SECOND_SCARLET_WEAPON);
				activeScarlet.setInvul(true);
				activeScarlet.setImmobilized(true);
				activeScarlet.disableAllSkills();
				broadcastPacket(world, new SpecialCamera(activeScarlet, 450, newHeading, 12, 500, 14000, 0, 0, 1, 0, 0));
				startQuestTimer("SCARLET_SECOND_MORPH_CAMERA_8", 8100, activeScarlet, null, false);
				break;
			}
			case "SCARLET_SECOND_MORPH_CAMERA_8":
			{
				final Instance world = npc.getInstanceWorld();
				broadcastPacket(world, new SocialAction(npc.getObjectId(), 2));
				startQuestTimer("SCARLET_SECOND_MORPH_CAMERA_9", 9000, npc, null, false);
				break;
			}
			case "SCARLET_SECOND_MORPH_CAMERA_9":
			{
				final Instance world = npc.getInstanceWorld();
				npc.setInvul(false);
				npc.setImmobilized(false);
				npc.enableAllSkills();
				enablePlayers(world);
				activateDemons(world);
				playRandomSong(world);
				break;
			}
			case "FINISH_CAMERA_1":
			{
				final Instance world = npc.getInstanceWorld();
				final Npc activeScarlet = world.getParameters().getObject("activeScarlet", Npc.class);
				final Npc frintezza = world.getParameters().getObject("frintezza", Npc.class);
				final int newHeading = world.getParameters().getInt("newHeading");
				broadcastPacket(world, new SpecialCamera(activeScarlet, 300, newHeading - 180, 5, 0, 7000, 0, 0, 1, 0, 0));
				broadcastPacket(world, new SpecialCamera(activeScarlet, 200, newHeading, 85, 4000, 10000, 0, 0, 1, 0, 0));
				startQuestTimer("FINISH_CAMERA_2", 7400, frintezza, player, false);
				startQuestTimer("FINISH_CAMERA_3", 7500, null, player, false);
				break;
			}
			case "FINISH_CAMERA_2":
			{
				final Instance world = npc.getInstanceWorld();
				final Npc frintezza = world.getParameters().getObject("frintezza", Npc.class);
				frintezza.doDie(player);
				break;
			}
			case "FINISH_CAMERA_3":
			{
				final Instance world = player.getInstanceWorld();
				final Npc frintezza = world.getParameters().getObject("frintezza", Npc.class);
				broadcastPacket(world, new SpecialCamera(frintezza, 100, 120, 5, 0, 7000, 0, 0, 1, 0, 0));
				broadcastPacket(world, new SpecialCamera(frintezza, 100, 90, 5, 5000, 15000, 0, 0, 1, 0, 0));
				startQuestTimer("FINISH_CAMERA_4", 7000, null, player, false);
				break;
			}
			case "FINISH_CAMERA_4":
			{
				final Instance world = player.getInstanceWorld();
				final Npc frintezza = world.getParameters().getObject("frintezza", Npc.class);
				broadcastPacket(world, new SpecialCamera(frintezza, 900, 90, 25, 7000, 10000, 0, 0, 1, 0, 0));
				startQuestTimer("FINISH_CAMERA_5", 9000, null, player, false);
				break;
			}
			case "FINISH_CAMERA_5":
			{
				final Instance world = player.getInstanceWorld();
				for (int doorId : FIRST_ROOM_DOORS)
				{
					world.openCloseDoor(doorId, true);
				}
				
				for (int doorId : FIRST_ROUTE_DOORS)
				{
					world.openCloseDoor(doorId, true);
				}
				
				for (int doorId : SECOND_ROOM_DOORS)
				{
					world.openCloseDoor(doorId, true);
				}
				
				for (int doorId : SECOND_ROUTE_DOORS)
				{
					world.openCloseDoor(doorId, true);
				}
				
				enablePlayers(world);
				break;
			}
		}
		
		return null;
	}
	
	/**
	 * Handles the logic when a {@code Player} interacts with an {@code Npc}.<br>
	 * It checks if the NPC is a guide to enter the instance.<br>
	 * Otherwise, it teleports the player out of the current world.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} who initiated the interaction.
	 * @return Always returns {@code null}.
	 */
	@Override
	public String onTalk(Npc npc, Player player)
	{
		if (npc.getId() == GUIDE)
		{
			enterInstance(player, npc, TEMPLATE_ID);
			Instance world = player.getInstanceWorld();
			if ((world != null) && (world.isStatus(0)))
			{
				world.setStatus(4);
				startQuestTimer("FRINTEZZA_INTRO_START", FRINTEZZA_WAIT_TIME * 60000, null, player);
			}
		}
		else // Teleport Cube
		{
			Instance world = player.getInstanceWorld();
			if (world != null)
			{
				teleportPlayerOut(player, world);
			}
		}
		
		return null;
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It initializes the movement and status of the spawned entity.<br>
	 * Specific logic is applied based on whether the NPC is a hall alarm or a dummy.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		npc.setRandomWalking(false);
		npc.setImmobilized(true);
		if (npc.getId() == HALL_ALARM)
		{
			npc.disableCoreAI(true);
		}
		else // dummy
		{
			npc.setInvul(true);
		}
	}
	
	/**
	 * Handles logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This method checks for specific health thresholds to trigger morphs.<br>
	 * It updates script values and starts quest timers based on the damage received.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt.
	 * @param isSummon Indicates if the attack was performed by a summon.
	 * @param skill The {@code Skill} used for the attack.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if (npc.getId() == SCARLET1)
		{
			final double currentHp = npc.getCurrentHp();
			final long maxHp = npc.getMaxHp();
			if (npc.isScriptValue(0) && (currentHp < (maxHp * 0.80)))
			{
				npc.setScriptValue(1);
				startQuestTimer("SCARLET_FIRST_MORPH", 1000, npc, null, false);
			}
			
			if (npc.isScriptValue(1) && (currentHp < (maxHp * 0.20)))
			{
				npc.setScriptValue(2);
				startQuestTimer("SCARLET_SECOND_MORPH", 1000, null, attacker, false);
			}
		}
	}
	
	/**
	 * This method handles specific logic when an {@code Npc} sees a skill used by a {@code Player}.<br>
	 * It checks if the {@code Skill} is a specific type and triggers a suicide for certain portraits.
	 * @param npc The {@code Npc} that is reacting to the skill.
	 * @param attacker The {@code Player} who used the skill.
	 * @param skill The {@code Skill} being cast by the player.
	 * @param targets A collection of {@code WorldObject} objects targeted by the skill.
	 * @param isSummon A boolean indicating if the skill was used on a summon.
	 */
	@Override
	public void onSkillSee(Npc npc, Player attacker, Skill skill, Collection<WorldObject> targets, boolean isSummon)
	{
		if (skill != null)
		{
			// When Dewdrop of Destruction is used on Portraits they suicide.
			if (ArrayUtil.contains(PORTRAITS, npc.getId()) && (skill.getId() == DEWDROP_OF_DESTRUCTION_SKILL_ID))
			{
				npc.doDie(attacker);
			}
			// Soul Breaking Arrow on Frintezza stops the current song; the next one follows after 35 seconds.
			else if ((npc.getId() == FRINTEZZA) && (skill.getId() == SOUL_BREAKING_ARROW_SKILL_ID) && targets.contains(npc))
			{
				cancelQuestTimer("PLAY_RANDOM_SONG", npc, null);
				npc.abortCast();
				broadcastPacket(npc.getInstanceWorld(), new MagicSkillCanceled(npc.getObjectId()));
				startQuestTimer("PLAY_RANDOM_SONG", SONG_BREAK_TIME * 1000, npc, null, false);
			}
		}
	}
	
	/**
	 * This method is called when a spell cast by an {@code Npc} has finished.<br>
	 * It checks if the skill was a suicide attack.<br>
	 * If it was a suicide attack, it triggers the {@code onKill} logic.
	 * @param npc The {@code Npc} that performed the action.
	 * @param player The {@code Player} who triggered the action.
	 * @param skill The {@code Skill} that was just completed.
	 */
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if (skill.isSuicideAttack())
		{
			onKill(npc, null, false);
		}
	}
	
	/**
	 * This method handles logic triggered when an {@code Npc} is killed.<br>
	 * It manages room transitions, monster spawning, and quest timers based on the specific NPC type.<br>
	 * It also updates the kill count and handles loot drops for certain entities.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who performed the kill.
	 * @param isSummon Indicates if the kill was performed by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = killer.getInstanceWorld();
		final int npcId = npc.getId();
		if (npcId == SCARLET2)
		{
			final Npc frintezza = world.getParameters().getObject("frintezza", Npc.class);
			cancelQuestTimer("PLAY_RANDOM_SONG", frintezza, null);
			broadcastPacket(world, new MagicSkillCanceled(frintezza.getObjectId()));
			startQuestTimer("FINISH_CAMERA_1", 500, npc, killer, false);
			killPortraitsAndDemons(world);
			world.finishInstance();
		}
		else
		{
			switch (npcId)
			{
				case DEMON_1:
				case DEMON_2:
				{
					final List<Npc> demons = world.getParameters().getList("demons", Npc.class);
					if (demons != null)
					{
						demons.remove(npc);
						world.setParameter("demons", demons);
					}
					break;
				}
				case PORTRAIT_1:
				case PORTRAIT_2:
				{
					final Map<Npc, Integer> portraits = world.getParameters().getMap("portraits", Npc.class, Integer.class);
					if (portraits != null)
					{
						portraits.remove(npc);
						world.setParameter("portraits", portraits);
					}
					break;
				}
			}
		}
	}
	
	/**
	 * Plays a random song effect in the instance.<br>
	 * This method checks if a song is already playing before starting a new one.<br>
	 * It selects a random song: Heal, Rampage and Power affect the monsters, Plague and Psycho affect the players.
	 * @param world The {@code Instance} where the song will be played.
	 * @return the cast time of the song in milliseconds, 0 when no song was started
	 */
	private int playRandomSong(Instance world)
	{
		final boolean isPlayingSong = world.getParameters().getBoolean("isPlayingSong");
		if (isPlayingSong)
		{
			return 0;
		}
		
		final Npc frintezza = world.getParameters().getObject("frintezza", Npc.class);
		world.setParameter("isPlayingSong", true);
		final int random = getRandom(1, 5);
		final SkillHolder skill = new SkillHolder(5007, random);
		final Skill songEffect = new SkillHolder(5008, random).getSkill();
		broadcastPacket(world, new ExShowScreenMessage(2, -1, 2, 0, 0, 0, 0, true, 4000, false, null, SKILL_MSG.get(random), null));
		broadcastPacket(world, new MagicSkillUse(frintezza, frintezza, skill.getSkillId(), skill.getSkillLevel(), skill.getSkill().getHitTime(), 0));
		if (random <= 3)
		{
			// Heal, Rampage and Power affect the monsters of the hall.
			for (Npc npc : world.getAliveNpcs())
			{
				if (npc.isMonster())
				{
					songEffect.applyEffects(frintezza, npc);
				}
			}
		}
		else
		{
			// Plague (40%) and Psycho (60%) affect the players.
			final int chance = random == 4 ? 40 : 60;
			for (Player player : world.getPlayers())
			{
				if ((player != null) && player.isOnline() && !player.isDead() && (getRandom(100) < chance))
				{
					songEffect.applyEffects(frintezza, player);
				}
			}
		}
		
		world.setParameter("isPlayingSong", false);
		return skill.getSkill().getHitTime();
	}
	
	/**
	 * Disables all actions for players in a specific instance.<br>
	 * This method stops movement, casting, and attacking for every online {@code Player}.<br>
	 * It also sets the player to an immobilized state with no target.
	 * @param world The {@code Instance} where players should be disabled.
	 */
	private void disablePlayers(Instance world)
	{
		for (Player player : world.getPlayers())
		{
			if ((player != null) && player.isOnline())
			{
				player.abortAttack();
				player.abortCast();
				player.disableAllSkills();
				player.setTarget(null);
				player.stopMove(null);
				player.setImmobilized(true);
				player.getAI().setIntentionIdle();
			}
		}
	}
	
	/**
	 * Enables all skills and removes immobilization for players in the instance.<br>
	 * This method iterates through every {@code Player} in the provided {@code Instance}.<br>
	 * It checks if the player is online before applying changes.
	 * @param world The {@code Instance} where players should be enabled.
	 */
	private void enablePlayers(Instance world)
	{
		for (Player player : world.getPlayers())
		{
			if ((player != null) && player.isOnline())
			{
				player.enableAllSkills();
				player.setImmobilized(false);
			}
		}
	}
	
	/**
	 * Sends a specific packet to all online players in an instance.<br>
	 * This method iterates through every {@code Player} in the provided {@code Instance}.<br>
	 * It checks if the player is not {@code null} and is currently online before sending.
	 * @param world The {@code Instance} where the packet will be broadcast.
	 * @param packet The {@code ServerPacket} to be sent to each player.
	 */
	void broadcastPacket(Instance world, ServerPacket packet)
	{
		for (Player player : world.getPlayers())
		{
			if ((player != null) && player.isOnline())
			{
				player.sendPacket(packet);
			}
		}
	}
	
	/**
	 * Sends specific packets to players based on their horizontal position.<br>
	 * It checks if a player's {@code x} coordinate is less than the provided value.<br>
	 * Players to the left receive {@code packet1}.<br>
	 * Players to the right or at the same position receive {@code packet2}.
	 * @param world The {@code Instance} where the players are located.
	 * @param packet1 The {@code ServerPacket} sent to players with an {@code x} coordinate less than the limit.
	 * @param packet2 The {@code ServerPacket} sent to all other online players.
	 * @param x The threshold value used to determine which packet to send.
	 */
	private void sendPacketX(Instance world, ServerPacket packet1, ServerPacket packet2, int x)
	{
		for (Player player : world.getPlayers())
		{
			if ((player != null) && player.isOnline())
			{
				if (player.getX() < x)
				{
					player.sendPacket(packet1);
				}
				else
				{
					player.sendPacket(packet2);
				}
			}
		}
	}
	
	/**
	 * Disables all active demons within the specified instance.<br>
	 * This method sets the immobilized state to {@code true} for each demon.<br>
	 * It also removes all skills from these entities.
	 * @param world The {@code Instance} where the demons are located.
	 */
	private void deactivateDemons(Instance world)
	{
		for (Npc demon : world.getAliveNpcs(DEMONS))
		{
			demon.setImmobilized(true);
			demon.disableAllSkills();
		}
	}
	
	/**
	 * Activates all demons within the specified instance.<br>
	 * This method removes immobilization from each demon.<br>
	 * It also enables all skills for every demon in the {@code DEMONS} group.
	 * @param world The {@code Instance} where the demons are located.
	 */
	private void activateDemons(Instance world)
	{
		for (Npc demon : world.getAliveNpcs(DEMONS))
		{
			demon.setImmobilized(false);
			demon.enableAllSkills();
		}
	}
	
	/**
	 * This method removes all active demons and portraits from the world.<br>
	 * It calls {@code doDie(null)} on every NPC in both categories.<br>
	 * It also resets the {@code demons} and {@code portraits} parameters to {@code 0}.
	 * @param world The {@code Instance} where the NPCs should be removed.
	 */
	private void killPortraitsAndDemons(Instance world)
	{
		for (Npc demon : world.getAliveNpcs(DEMONS))
		{
			demon.doDie(null);
		}
		
		world.setParameter("demons", 0);
		
		for (Npc portrait : world.getAliveNpcs(PORTRAITS))
		{
			portrait.doDie(null);
		}
		
		world.setParameter("portraits", 0);
	}
	
	/**
	 * This is the main entry point for the application.<br>
	 * It initializes a new instance of {@code LastImperialTomb}.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new LastImperialTomb();
	}
}
