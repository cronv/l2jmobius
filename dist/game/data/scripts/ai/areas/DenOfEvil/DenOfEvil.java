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
package ai.areas.DenOfEvil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.entity.zone.type.EffectZone;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.Id;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Kasha's Eyes of the {@code Den of Evil}.<br>
 * Every camp has an invisible Nest Controller with four spots; on the seconds 0, 20 and 40 of the clock it fills the empty spots with an eye of a random kind.<br>
 * Each kind of eye alive raises one level of its own effect over the camp: Kasha's Curse, Kasha's Yearning or Kasha's Despair, on the players and on the monsters inside.<br>
 * Killing an eye lowers that level at once and the spot is filled again; ten minutes without an eye dying, the players inside are warned three times, the camp casts Kasha's Betrayal on everyone around and the eyes die to be born again.
 * @author Altur
 */
public class DenOfEvil extends Script
{
	// NPCs
	private static final int NEST_CONTROLLER = 32656;
	private static final int KASHAS_EYE_CURSE = 18812; // my_buff_type 1
	private static final int KASHAS_EYE_YEARNING = 18813; // my_buff_type 2
	private static final int KASHAS_EYE_DESPAIR = 18814; // my_buff_type 3
	private static final int[] KASHAS_EYES =
	{
		KASHAS_EYE_CURSE,
		KASHAS_EYE_YEARNING,
		KASHAS_EYE_DESPAIR,
	};
	
	// Skills, indexed by the kind of eye (1 = Kasha's Curse, 2 = Kasha's Yearning, 3 = Kasha's Despair)
	private static final int[] PLAYER_SKILLS =
	{
		0,
		6150,
		6152,
		6154,
	};
	private static final int[] MONSTER_SKILLS =
	{
		0,
		6151,
		6153,
		6155,
	};
	private static final SkillHolder KASHAS_BETRAYAL = new SkillHolder(6149, 1);
	
	// Misc
	private static final int SPOTS = 4;
	private static final int KINDS = 3;
	private static final int MAX_LEVEL = 4;
	private static final int LIMIT_COUNT = 10; // minutes without an eye dying before the camp is destroyed
	private static final int FRAME_PERIOD = 20000; // the eyes are born on the seconds 0, 20 and 40 of the clock
	private static final int MINUTE_PERIOD = 60000;
	private static final int MONSTER_TICK_PERIOD = 15000; // tick of the camp areas
	private static final int DESTROY_DELAY = 3000;
	private static final int CAMP_RANGE = 2000; // range of Kasha's Betrayal, and it covers every camp polygon
	private static final SystemMessageId[] WARNINGS =
	{
		SystemMessageId.DEFEAT_KASHA_S_MONSTER_EYES_TO_LIFT_THE_GREAT_CURSE, // one minute left
		SystemMessageId.A_GREAT_CURSE_CAN_BE_FELT_FROM_KASHA_S_MONSTER_EYES, // two minutes left
		SystemMessageId.ALL_4_OF_KASHA_S_MONSTER_EYES_HAVE_APPEARED, // three minutes left
	};
	
	private final Map<Integer, Camp> _camps = new ConcurrentHashMap<>();
	private final Map<Integer, int[]> _eyes = new ConcurrentHashMap<>(); // object id of the eye -> {camp, spot}
	
	private DenOfEvil()
	{
		addSpawnId(NEST_CONTROLLER);
		
		// The controllers are already in the world when the script is reloaded.
		for (Spawn spawn : SpawnTable.getInstance().getSpawns(NEST_CONTROLLER))
		{
			final Npc controller = spawn.getLastSpawn();
			if ((controller != null) && controller.isSpawned())
			{
				initCamp(controller);
			}
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "FRAME_ALIGN":
			{
				fillAll();
				startQuestTimer("FRAME", FRAME_PERIOD, null, null, true);
				break;
			}
			case "FRAME":
			{
				fillAll();
				break;
			}
			case "MINUTE":
			{
				final Camp camp = campOf(npc);
				if (camp != null)
				{
					countMinute(camp, npc);
				}
				break;
			}
			case "DESTROY":
			{
				final Camp camp = campOf(npc);
				if (camp != null)
				{
					destroy(camp, npc);
				}
				break;
			}
			case "MONSTER_TICK":
			{
				final Camp camp = campOf(npc);
				if (camp != null)
				{
					buffMonsters(camp, npc);
				}
				break;
			}
			default:
			{
				break;
			}
		}
		
		return null;
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		initCamp(npc);
	}
	
	/**
	 * Every death of an eye, whoever the killer, empties its spot, lowers the level of its kind and resets the count of the camp.
	 * @param event the death
	 */
	@RegisterEvent(EventType.ON_CREATURE_DEATH)
	@RegisterType(ListenerRegisterType.NPC)
	@Id(
	{
		KASHAS_EYE_CURSE,
		KASHAS_EYE_YEARNING,
		KASHAS_EYE_DESPAIR
	})
	public void onEyeDeath(OnCreatureDeath event)
	{
		final Npc eye = event.getTarget().asNpc();
		final int[] place = _eyes.get(eye.getObjectId());
		if (place == null)
		{
			return; // not one of ours
		}
		
		final Camp camp = _camps.get(place[0]);
		if (camp == null)
		{
			return;
		}
		
		synchronized (camp)
		{
			eyeGone(camp, place[1], eye);
		}
	}
	
	@Override
	public void unload(boolean removeFromList)
	{
		for (Camp camp : _camps.values())
		{
			teardown(camp, "script unloaded");
		}
		
		super.unload(removeFromList);
	}
	
	/**
	 * Builds the camp of a controller from its spawn parameters and fills its spots.
	 * @param controller the Nest Controller
	 */
	private void initCamp(Npc controller)
	{
		final StatSet params = controller.getParameters();
		final Location origin = controller.getLocation();
		final int group = params.getInt("GroupNum", 0);
		final EffectZone zone = ZoneManager.getInstance().getZoneByName(params.getString("my_area", ""), EffectZone.class);
		final ZoneType announce = ZoneManager.getInstance().getZoneByName(params.getString("my_trr", ""));
		final Location[] spots = new Location[SPOTS];
		boolean validSpots = true;
		for (int spot = 0; spot < SPOTS; spot++)
		{
			final String name = "Spot" + (spot + 1) + "_";
			spots[spot] = new Location(params.getInt(name + "x", 0), params.getInt(name + "y", 0), params.getInt(name + "z", 0), params.getInt(name + "dir", 0));
			if ((spots[spot].getX() == 0) && (spots[spot].getY() == 0))
			{
				validSpots = false;
			}
		}
		
		if (!validSpots || (group <= 0) || (zone == null) || (announce == null))
		{
			LOGGER.warning(getClass().getSimpleName() + ": Nest Controller at " + origin + " without valid parameters (GroupNum " + group + ").");
			return;
		}
		
		final Camp camp = new Camp(group, controller, zone, announce, spots);
		if (_camps.putIfAbsent(group, camp) != null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Nest Controller at " + origin + " repeats GroupNum " + group + ", ignored.");
			return;
		}
		
		controller.setUndying(true);
		controller.disableCoreAI(true);
		
		// Eyes left behind by a previous instance of the script.
		for (Monster monster : World.getVisibleObjectsInRange(controller, Monster.class, CAMP_RANGE))
		{
			if (isEye(monster.getId()) && !_eyes.containsKey(monster.getObjectId()))
			{
				SpawnTable.getInstance().removeSpawn(monster.getSpawn());
				monster.deleteMe();
			}
		}
		
		zone.clearSkills();
		
		synchronized (camp)
		{
			fill(camp);
		}
		
		startQuestTimer("MINUTE", MINUTE_PERIOD, controller, null, true);
		startQuestTimer("MONSTER_TICK", MONSTER_TICK_PERIOD, controller, null, true);
		if ((getQuestTimer("FRAME", null, null) == null) && (getQuestTimer("FRAME_ALIGN", null, null) == null))
		{
			startQuestTimer("FRAME_ALIGN", FRAME_PERIOD - (System.currentTimeMillis() % FRAME_PERIOD), null, null, false);
		}
	}
	
	/**
	 * Fills the empty spots of every camp, and dismantles the camps whose controller is gone.
	 */
	private void fillAll()
	{
		for (Camp camp : _camps.values())
		{
			try
			{
				if (!camp._controller.isSpawned())
				{
					teardown(camp, "controller gone");
					continue;
				}
				
				synchronized (camp)
				{
					fill(camp);
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Camp " + camp._group + " could not be filled.", e);
			}
		}
	}
	
	/**
	 * Fills every empty spot of a camp with an eye of a random kind. Called with the lock of the camp.
	 * @param camp the camp
	 */
	private void fill(Camp camp)
	{
		for (int spot = 0; spot < SPOTS; spot++)
		{
			final Npc old = camp._eyes[spot];
			if (old != null)
			{
				if (old.isSpawned())
				{
					continue;
				}
				
				eyeGone(camp, spot, old); // deleted without dying
			}
			
			final Location location = camp._spots[spot];
			final Npc eye = addSpawn(getRandomEntry(KASHAS_EYES), location.getX(), location.getY(), location.getZ(), location.getHeading(), false, 0);
			if (eye == null)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Camp " + camp._group + " could not spawn the eye of spot " + (spot + 1) + ".");
				continue;
			}
			
			eye.disableCoreAI(true);
			eye.setImmobilized(true);
			SpawnTable.getInstance().addSpawn(eye.getSpawn());
			_eyes.put(eye.getObjectId(), new int[]
			{
				camp._group,
				spot
			});
			camp._eyes[spot] = eye;
			levelUp(camp, kindOf(eye));
		}
	}
	
	/**
	 * Empties the spot of an eye that died or was deleted. Called with the lock of the camp.
	 * @param camp the camp
	 * @param spot the spot
	 * @param eye the eye
	 */
	private void eyeGone(Camp camp, int spot, Npc eye)
	{
		if (camp._eyes[spot] != eye)
		{
			return;
		}
		
		camp._eyes[spot] = null;
		_eyes.remove(eye.getObjectId());
		SpawnTable.getInstance().removeSpawn(eye.getSpawn());
		levelDown(camp, kindOf(eye));
		if (!camp._destroying)
		{
			camp._idleMinutes = 0;
		}
	}
	
	private void levelUp(Camp camp, int kind)
	{
		if ((kind < 1) || (kind > KINDS) || (camp._levels[kind] >= MAX_LEVEL))
		{
			return;
		}
		
		camp._levels[kind]++;
		applyLevel(camp, kind);
	}
	
	private void levelDown(Camp camp, int kind)
	{
		if ((kind < 1) || (kind > KINDS) || (camp._levels[kind] <= 0))
		{
			return;
		}
		
		camp._levels[kind]--;
		if (camp._levels[kind] == 0)
		{
			camp._zone.removeSkill(PLAYER_SKILLS[kind]); // the running effect ends by itself
			return;
		}
		
		applyLevel(camp, kind);
	}
	
	/**
	 * Sets the level of a kind on the zone and applies it at once to the players inside: the zone would wait for the running effect to end.
	 * @param camp the camp
	 * @param kind the kind of eye
	 */
	private void applyLevel(Camp camp, int kind)
	{
		final int level = camp._levels[kind];
		camp._zone.addSkill(PLAYER_SKILLS[kind], level);
		final Skill skill = SkillData.getInstance().getSkill(PLAYER_SKILLS[kind], level);
		for (Player player : camp._zone.getPlayersInside())
		{
			if (!player.isDead() && !player.isMounted())
			{
				skill.applyEffects(player, player);
			}
		}
	}
	
	/**
	 * One more minute without an eye dying: warns the players on the last three and starts the destruction on the tenth.
	 * @param camp the camp
	 * @param controller the Nest Controller
	 */
	private void countMinute(Camp camp, Npc controller)
	{
		synchronized (camp)
		{
			if (camp._destroying || (liveEyes(camp).isEmpty()))
			{
				return;
			}
			
			camp._idleMinutes++;
			final int left = LIMIT_COUNT - camp._idleMinutes;
			if ((left >= 1) && (left <= WARNINGS.length))
			{
				camp._announce.broadcastPacket(new SystemMessage(WARNINGS[left - 1]));
			}
			else if (left <= 0)
			{
				camp._destroying = true;
				camp._victims = liveEyes(camp); // only the eyes alive now will die
				startQuestTimer("DESTROY", DESTROY_DELAY, controller, null, false);
			}
		}
	}
	
	/**
	 * Kasha's Betrayal on every playable around the controller, and the eyes doomed three seconds ago die without drops.
	 * @param camp the camp
	 * @param controller the Nest Controller
	 */
	private void destroy(Camp camp, Npc controller)
	{
		controller.broadcastPacket(new MagicSkillUse(controller, controller, KASHAS_BETRAYAL.getSkillId(), KASHAS_BETRAYAL.getSkillLevel(), 0, 0));
		final Skill betrayal = KASHAS_BETRAYAL.getSkill();
		for (Playable playable : World.getVisibleObjectsInRange(controller, Playable.class, CAMP_RANGE))
		{
			if (!playable.isDead())
			{
				betrayal.applyEffects(controller, playable);
			}
		}
		
		final List<Npc> victims;
		synchronized (camp)
		{
			victims = camp._victims;
			camp._victims = null;
		}
		
		if (victims != null)
		{
			for (Npc eye : victims) // outside the lock: the death listener runs in this thread
			{
				if (eye.isSpawned() && !eye.isDead())
				{
					eye.asAttackable().clearAggroList();
					eye.doDie(null);
				}
			}
		}
		
		synchronized (camp)
		{
			camp._destroying = false;
			camp._idleMinutes = 0;
		}
	}
	
	/**
	 * Applies the effect of every kind alive to the monsters inside the camp polygon, the eyes included.
	 * @param camp the camp
	 * @param controller the Nest Controller
	 */
	private void buffMonsters(Camp camp, Npc controller)
	{
		final int[] levels;
		synchronized (camp)
		{
			levels = camp._levels.clone();
		}
		
		final List<Monster> monsters = World.getVisibleObjectsInRange(controller, Monster.class, CAMP_RANGE);
		for (int kind = 1; kind <= KINDS; kind++)
		{
			if (levels[kind] == 0)
			{
				continue;
			}
			
			final Skill skill = SkillData.getInstance().getSkill(MONSTER_SKILLS[kind], levels[kind]);
			for (Monster monster : monsters)
			{
				if (!monster.isDead() && camp._zone.isInsideZone(monster))
				{
					skill.applyEffects(monster, monster);
				}
			}
		}
	}
	
	/**
	 * Removes a camp: its eyes, its levels and its timers.
	 * @param camp the camp
	 * @param reason the reason for the log
	 */
	private void teardown(Camp camp, String reason)
	{
		_camps.remove(camp._group);
		synchronized (camp)
		{
			for (int spot = 0; spot < SPOTS; spot++)
			{
				final Npc eye = camp._eyes[spot];
				if (eye != null)
				{
					_eyes.remove(eye.getObjectId());
					SpawnTable.getInstance().removeSpawn(eye.getSpawn());
					if (eye.isSpawned())
					{
						eye.deleteMe();
					}
					
					camp._eyes[spot] = null;
				}
			}
			
			camp._zone.clearSkills();
			for (int kind = 1; kind <= KINDS; kind++)
			{
				camp._levels[kind] = 0;
			}
			
			camp._victims = null;
		}
		
		cancelQuestTimer("MINUTE", camp._controller, null);
		cancelQuestTimer("MONSTER_TICK", camp._controller, null);
		cancelQuestTimer("DESTROY", camp._controller, null);
		LOGGER.info(getClass().getSimpleName() + ": Camp " + camp._group + " dismantled (" + reason + ").");
	}
	
	private Camp campOf(Npc controller)
	{
		final Camp camp = _camps.get(controller.getParameters().getInt("GroupNum", 0));
		return ((camp != null) && (camp._controller == controller)) ? camp : null;
	}
	
	private static List<Npc> liveEyes(Camp camp)
	{
		final List<Npc> eyes = new ArrayList<>(SPOTS);
		for (int spot = 0; spot < SPOTS; spot++)
		{
			final Npc eye = camp._eyes[spot];
			if ((eye != null) && eye.isSpawned() && !eye.isDead())
			{
				eyes.add(eye);
			}
		}
		
		return eyes;
	}
	
	private static int kindOf(Npc eye)
	{
		return eye.getParameters().getInt("my_buff_type", 0);
	}
	
	private static boolean isEye(int npcId)
	{
		for (int eye : KASHAS_EYES)
		{
			if (eye == npcId)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * One camp of the Den of Evil: its controller, its zones, its four spots and what lives on them.
	 */
	private static class Camp
	{
		private final int _group;
		private final Npc _controller;
		private final EffectZone _zone; // buffs of the camp
		private final ZoneType _announce; // warnings of the camp
		private final Location[] _spots;
		private final Npc[] _eyes = new Npc[SPOTS];
		private final int[] _levels = new int[KINDS + 1];
		private int _idleMinutes;
		private boolean _destroying;
		private List<Npc> _victims;
		
		protected Camp(int group, Npc controller, EffectZone zone, ZoneType announce, Location[] spots)
		{
			_group = group;
			_controller = controller;
			_zone = zone;
			_announce = announce;
			_spots = spots;
		}
	}
	
	public static void main(String[] args)
	{
		new DenOfEvil();
	}
}
