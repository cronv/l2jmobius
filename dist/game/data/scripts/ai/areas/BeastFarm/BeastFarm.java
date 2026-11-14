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
package ai.areas.BeastFarm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.TamedBeast;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerTameBeast;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.serverpackets.NpcInfo;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * Manages the logic for growth-capable mobs in the beast farm area.<br>
 * These mobs can polymorph into new forms upon successful feeding.
 * @author Fulminus, Gigiikun
 */
public class BeastFarm extends Script
{
	private static final int GOLDEN_SPICE = 15474;
	private static final int CRYSTAL_SPICE = 15475;
	private static final int SKILL_GOLDEN_SPICE = 9049;
	private static final int SKILL_CRYSTAL_SPICE = 9050;
	private static final int SKILL_BLESSED_GOLDEN_SPICE = 9051;
	private static final int SKILL_BLESSED_CRYSTAL_SPICE = 9052;
	private static final int SKILL_SGRADE_GOLDEN_SPICE = 9053;
	private static final int SKILL_SGRADE_CRYSTAL_SPICE = 9054;
	private static final int[] TAMED_BEASTS =
	{
		18869,
		18870,
		18871,
		18872
	};
	private static final int TAME_CHANCE = 20;
	protected static final int[] SPECIAL_SPICE_CHANCES =
	{
		33,
		75
	};
	
	// all mobs that can eat...
	private static final int[] FEEDABLE_BEASTS =
	{
		// Kookaburras
		18873,
		18874,
		18875,
		18876,
		18877,
		18878,
		18879,
		// Cougars
		18880,
		18881,
		18882,
		18883,
		18884,
		18885,
		18886,
		// Buffalos
		18887,
		18888,
		18889,
		18890,
		18891,
		18892,
		18893,
		// Grendels
		18894,
		18895,
		18896,
		18897,
		18898,
		18899,
		18900
	};
	
	private static Map<Integer, GrowthCapableMob> GROWTH_CAPABLE_MOBS = new HashMap<>();
	private static List<TamedBeastSkillHolder> TAMED_BEAST_DATA = new ArrayList<>();
	private final Map<Integer, Integer> _feedInfo = new ConcurrentHashMap<>();
	
	/**
	 * Private constructor for the {@code BeastFarm} class.<br>
	 * This constructor initializes the growth data and skill holders.<br>
	 * It prevents direct instantiation of this script.
	 */
	private BeastFarm()
	{
		addSkillSeeId(FEEDABLE_BEASTS);
		addKillId(FEEDABLE_BEASTS);
		
		GrowthCapableMob temp;
		
		// Kookabura
		temp = new GrowthCapableMob(100, 0, 18869);
		temp.addNpcIdForSkillId(SKILL_GOLDEN_SPICE, 18874);
		temp.addNpcIdForSkillId(SKILL_CRYSTAL_SPICE, 18875);
		temp.addNpcIdForSkillId(SKILL_BLESSED_GOLDEN_SPICE, 18869);
		temp.addNpcIdForSkillId(SKILL_BLESSED_CRYSTAL_SPICE, 18869);
		temp.addNpcIdForSkillId(SKILL_SGRADE_GOLDEN_SPICE, 18878);
		temp.addNpcIdForSkillId(SKILL_SGRADE_CRYSTAL_SPICE, 18879);
		GROWTH_CAPABLE_MOBS.put(18873, temp);
		
		temp = new GrowthCapableMob(40, 1, 18869);
		temp.addNpcIdForSkillId(SKILL_GOLDEN_SPICE, 18876);
		GROWTH_CAPABLE_MOBS.put(18874, temp);
		
		temp = new GrowthCapableMob(40, 1, 18869);
		temp.addNpcIdForSkillId(SKILL_CRYSTAL_SPICE, 18877);
		GROWTH_CAPABLE_MOBS.put(18875, temp);
		
		temp = new GrowthCapableMob(25, 2, 18869);
		temp.addNpcIdForSkillId(SKILL_GOLDEN_SPICE, 18878);
		GROWTH_CAPABLE_MOBS.put(18876, temp);
		
		temp = new GrowthCapableMob(25, 2, 18869);
		temp.addNpcIdForSkillId(SKILL_CRYSTAL_SPICE, 18879);
		GROWTH_CAPABLE_MOBS.put(18877, temp);
		
		// Cougar
		temp = new GrowthCapableMob(100, 0, 18870);
		temp.addNpcIdForSkillId(SKILL_GOLDEN_SPICE, 18881);
		temp.addNpcIdForSkillId(SKILL_CRYSTAL_SPICE, 18882);
		temp.addNpcIdForSkillId(SKILL_BLESSED_GOLDEN_SPICE, 18870);
		temp.addNpcIdForSkillId(SKILL_BLESSED_CRYSTAL_SPICE, 18870);
		temp.addNpcIdForSkillId(SKILL_SGRADE_GOLDEN_SPICE, 18885);
		temp.addNpcIdForSkillId(SKILL_SGRADE_CRYSTAL_SPICE, 18886);
		GROWTH_CAPABLE_MOBS.put(18880, temp);
		
		temp = new GrowthCapableMob(40, 1, 18870);
		temp.addNpcIdForSkillId(SKILL_GOLDEN_SPICE, 18883);
		GROWTH_CAPABLE_MOBS.put(18881, temp);
		
		temp = new GrowthCapableMob(40, 1, 18870);
		temp.addNpcIdForSkillId(SKILL_CRYSTAL_SPICE, 18884);
		GROWTH_CAPABLE_MOBS.put(18882, temp);
		
		temp = new GrowthCapableMob(25, 2, 18870);
		temp.addNpcIdForSkillId(SKILL_GOLDEN_SPICE, 18885);
		GROWTH_CAPABLE_MOBS.put(18883, temp);
		
		temp = new GrowthCapableMob(25, 2, 18870);
		temp.addNpcIdForSkillId(SKILL_CRYSTAL_SPICE, 18886);
		GROWTH_CAPABLE_MOBS.put(18884, temp);
		
		// Buffalo
		temp = new GrowthCapableMob(100, 0, 18871);
		temp.addNpcIdForSkillId(SKILL_GOLDEN_SPICE, 18888);
		temp.addNpcIdForSkillId(SKILL_CRYSTAL_SPICE, 18889);
		temp.addNpcIdForSkillId(SKILL_BLESSED_GOLDEN_SPICE, 18871);
		temp.addNpcIdForSkillId(SKILL_BLESSED_CRYSTAL_SPICE, 18871);
		temp.addNpcIdForSkillId(SKILL_SGRADE_GOLDEN_SPICE, 18892);
		temp.addNpcIdForSkillId(SKILL_SGRADE_CRYSTAL_SPICE, 18893);
		GROWTH_CAPABLE_MOBS.put(18887, temp);
		
		temp = new GrowthCapableMob(40, 1, 18871);
		temp.addNpcIdForSkillId(SKILL_GOLDEN_SPICE, 18890);
		GROWTH_CAPABLE_MOBS.put(18888, temp);
		
		temp = new GrowthCapableMob(40, 1, 18871);
		temp.addNpcIdForSkillId(SKILL_CRYSTAL_SPICE, 18891);
		GROWTH_CAPABLE_MOBS.put(18889, temp);
		
		temp = new GrowthCapableMob(25, 2, 18871);
		temp.addNpcIdForSkillId(SKILL_GOLDEN_SPICE, 18892);
		GROWTH_CAPABLE_MOBS.put(18890, temp);
		
		temp = new GrowthCapableMob(25, 2, 18871);
		temp.addNpcIdForSkillId(SKILL_CRYSTAL_SPICE, 18893);
		GROWTH_CAPABLE_MOBS.put(18891, temp);
		
		// Grendel
		temp = new GrowthCapableMob(100, 0, 18872);
		temp.addNpcIdForSkillId(SKILL_GOLDEN_SPICE, 18895);
		temp.addNpcIdForSkillId(SKILL_CRYSTAL_SPICE, 18896);
		temp.addNpcIdForSkillId(SKILL_BLESSED_GOLDEN_SPICE, 18872);
		temp.addNpcIdForSkillId(SKILL_BLESSED_CRYSTAL_SPICE, 18872);
		temp.addNpcIdForSkillId(SKILL_SGRADE_GOLDEN_SPICE, 18899);
		temp.addNpcIdForSkillId(SKILL_SGRADE_CRYSTAL_SPICE, 18900);
		GROWTH_CAPABLE_MOBS.put(18894, temp);
		
		temp = new GrowthCapableMob(40, 1, 18872);
		temp.addNpcIdForSkillId(SKILL_GOLDEN_SPICE, 18897);
		GROWTH_CAPABLE_MOBS.put(18895, temp);
		
		temp = new GrowthCapableMob(40, 1, 18872);
		temp.addNpcIdForSkillId(SKILL_CRYSTAL_SPICE, 18898);
		GROWTH_CAPABLE_MOBS.put(18896, temp);
		
		temp = new GrowthCapableMob(25, 2, 18872);
		temp.addNpcIdForSkillId(SKILL_GOLDEN_SPICE, 18899);
		GROWTH_CAPABLE_MOBS.put(18897, temp);
		
		temp = new GrowthCapableMob(25, 2, 18872);
		temp.addNpcIdForSkillId(SKILL_CRYSTAL_SPICE, 18900);
		GROWTH_CAPABLE_MOBS.put(18898, temp);
		
		// Tamed beasts data
		TAMED_BEAST_DATA.add(new TamedBeastSkillHolder("%name% of Focus", new SkillHolder(6432, 1), new SkillHolder(6668, 1)));
		TAMED_BEAST_DATA.add(new TamedBeastSkillHolder("%name% of Guiding", new SkillHolder(6433, 1), new SkillHolder(6670, 1)));
		TAMED_BEAST_DATA.add(new TamedBeastSkillHolder("%name% of Swifth", new SkillHolder(6434, 1), new SkillHolder(6667, 1)));
		TAMED_BEAST_DATA.add(new TamedBeastSkillHolder("Berserker %name%", new SkillHolder(6671, 1)));
		TAMED_BEAST_DATA.add(new TamedBeastSkillHolder("%name% of Protect", new SkillHolder(6669, 1), new SkillHolder(6672, 1)));
		TAMED_BEAST_DATA.add(new TamedBeastSkillHolder("%name% of Vigor", new SkillHolder(6431, 1), new SkillHolder(6666, 1)));
	}
	
	/**
	 * Handles the evolution of an {@code Npc} after it is fed.<br>
	 * It removes the old entity and spawns a new one based on the provided ID.<br>
	 * If the new ID is a tamed beast, it initializes special skills and names.<br>
	 * Otherwise, it creates a hostile {@code Attackable} entity that targets the player.
	 * @param npc The current {@code Npc} being replaced.
	 * @param player The {@code Player} who fed the previous entity.
	 * @param nextNpcId The ID of the new {@code Npc} to spawn.
	 * @param food The amount of food used for the evolution.
	 */
	public void spawnNext(Npc npc, Player player, int nextNpcId, int food)
	{
		// Remove the feedinfo of the mob that got despawned, if any.
		final int objectId = npc.getObjectId();
		final int playerObjectId = player.getObjectId();
		if (_feedInfo.containsKey(objectId) && (_feedInfo.get(objectId) == playerObjectId))
		{
			_feedInfo.remove(objectId);
		}
		
		// Despawn the old mob.
		npc.deleteMe();
		
		// If this is finally a trained mob, then despawn any other trained mobs that the player might have and initialize the Tamed Beast.
		if (ArrayUtil.contains(TAMED_BEASTS, nextNpcId))
		{
			final TamedBeast nextNpc = new TamedBeast(nextNpcId, player, food, npc.getX(), npc.getY(), npc.getZ(), true);
			final TamedBeastSkillHolder beast = getRandomEntry(TAMED_BEAST_DATA);
			String name = beast.getName();
			switch (nextNpcId)
			{
				case 18869:
				{
					name = name.replace("%name%", "Alpine Kookaburra");
					break;
				}
				case 18870:
				{
					name = name.replace("%name%", "Alpine Cougar");
					break;
				}
				case 18871:
				{
					name = name.replace("%name%", "Alpine Buffalo");
					break;
				}
				case 18872:
				{
					name = name.replace("%name%", "Alpine Grendel");
					break;
				}
			}
			
			nextNpc.setName(name);
			nextNpc.broadcastPacket(new NpcInfo(nextNpc));
			nextNpc.setRunning();
			
			final SkillData st = SkillData.getInstance();
			for (SkillHolder sh : beast.getSkills())
			{
				nextNpc.addBeastSkill(st.getSkill(sh.getSkillId(), sh.getSkillLevel()));
			}
			
			// TODO: Quest removed with Etina's Fate.
			// Q00020_BringUpWithLove.checkJewelOfInnocence(player);
			
			// Notify scripts of the Taming Wild Beasts quest listener.
			if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_TAME_BEAST))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerTameBeast(player, nextNpc.getId()));
			}
		}
		else
		{
			// If not trained, the newly spawned mob will automatically be agro against its feeders (what happened to "never bite the hand that feeds you" anyway?!).
			final Attackable nextNpc = addSpawn(nextNpcId, npc).asAttackable();
			
			// Register the player in the feedinfo for the mob that just spawned.
			_feedInfo.put(nextNpc.getObjectId(), playerObjectId);
			nextNpc.setRunning();
			nextNpc.addDamageHate(player, 0, 99999);
			nextNpc.getAI().setIntentionAttack(player);
			player.setTarget(nextNpc);
		}
	}
	
	/**
	 * Handles logic when a player uses a skill on an {@code Npc}.<br>
	 * This method checks if the {@code Npc} is a feedable beast and processes growth.<br>
	 * It validates the {@code Skill} used and updates the beast's level or state.
	 * @param npc The {@code Npc} being targeted by the skill.
	 * @param caster The {@code Player} who cast the skill.
	 * @param skill The {@code Skill} that was executed.
	 * @param targets A {@code Collection} of {@code WorldObject} entities affected by the skill.
	 * @param isSummon A boolean indicating if the action is a summon.
	 */
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, Collection<WorldObject> targets, boolean isSummon)
	{
		// This behavior is only run when the target of skill is the passed npc (chest) i.e. when the player is attempting to open the chest using a skill.
		if (!targets.contains(npc))
		{
			return;
		}
		
		// Gather some values on local variables.
		final int npcId = npc.getId();
		final int skillId = skill.getId();
		
		// Check if the npc and skills used are valid for this script. Exit if invalid.
		if (!ArrayUtil.contains(FEEDABLE_BEASTS, npcId) || ((skillId != SKILL_GOLDEN_SPICE) && (skillId != SKILL_CRYSTAL_SPICE) && (skillId != SKILL_BLESSED_GOLDEN_SPICE) && (skillId != SKILL_BLESSED_CRYSTAL_SPICE) && (skillId != SKILL_SGRADE_GOLDEN_SPICE) && (skillId != SKILL_SGRADE_CRYSTAL_SPICE)))
		{
			return;
		}
		
		// First gather some values on local variables.
		final int objectId = npc.getObjectId();
		int growthLevel = 3; // if a mob is in FEEDABLE_BEASTS but not in _GrowthCapableMobs, then it's at max growth (3)
		final GrowthCapableMob growthData = GROWTH_CAPABLE_MOBS.get(npcId);
		if (growthData != null)
		{
			growthLevel = growthData.getGrowthLevel();
		}
		
		// Prevent an exploit where two players simultaneously raise the same zero-growth beast by locking it to the first feeder when the mob is at level zero.
		if ((growthLevel == 0) && _feedInfo.containsKey(objectId))
		{
			return;
		}
		
		final int casterObjectId = caster.getObjectId();
		_feedInfo.put(objectId, casterObjectId);
		
		// Display the social action of the beast eating the food.
		npc.broadcastSocialAction(2);
		
		int food = 0;
		switch (skillId)
		{
			case SKILL_GOLDEN_SPICE:
			case SKILL_BLESSED_GOLDEN_SPICE:
			{
				food = GOLDEN_SPICE;
				break;
			}
			case SKILL_CRYSTAL_SPICE:
			case SKILL_BLESSED_CRYSTAL_SPICE:
			{
				food = CRYSTAL_SPICE;
				break;
			}
		}
		
		// If this pet can't grow, it's all done.
		final GrowthCapableMob growthData2 = GROWTH_CAPABLE_MOBS.get(npcId);
		if (growthData2 != null)
		{
			// Do nothing if this monster doesn't eat the specified food (food gets consumed but has no effect).
			final int newNpcId = growthData2.getLeveledNpcId(skillId);
			if (newNpcId == -1)
			{
				if (growthLevel == 0)
				{
					_feedInfo.remove(objectId);
					npc.setRunning();
					npc.asAttackable().addDamageHate(caster, 0, 1);
					npc.getAI().setIntentionAttack(caster);
				}
				return;
			}
			else if ((growthLevel > 0) && (_feedInfo.get(objectId) != casterObjectId))
			{
				// Check if this is the same player as the one who raised it from growth 0.
				// If not, then do not allow a chance to raise the pet (food gets consumed but has no effect).
				return;
			}
			
			spawnNext(npc, caster, newNpcId, food);
		}
		else
		{
			caster.sendMessage("The beast spit out the feed instead of eating it.");
			npc.dropItem(caster, food, 1);
		}
	}
	
	/**
	 * This method is called when an {@code Npc} is killed.<br>
	 * It removes the feeding information for that specific {@code Npc}.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who killed the {@code Npc}.
	 * @param isSummon Indicates if the kill was a summon action.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		// Remove the feedinfo of the mob that got killed, if any.
		final int objectId = npc.getObjectId();
		if (_feedInfo.containsKey(objectId))
		{
			_feedInfo.remove(objectId);
		}
	}
	
	// All mobs that grow by eating.
	private static class GrowthCapableMob
	{
		private final int _chance;
		private final int _growthLevel;
		private final int _tameNpcId;
		private final Map<Integer, Integer> _skillSuccessNpcIdList = new ConcurrentHashMap<>();
		
		public GrowthCapableMob(int chance, int growthLevel, int tameNpcId)
		{
			_chance = chance;
			_growthLevel = growthLevel;
			_tameNpcId = tameNpcId;
		}
		
		public void addNpcIdForSkillId(int skillId, int npcId)
		{
			_skillSuccessNpcIdList.put(skillId, npcId);
		}
		
		public int getGrowthLevel()
		{
			return _growthLevel;
		}
		
		public int getLeveledNpcId(int skillId)
		{
			if (!_skillSuccessNpcIdList.containsKey(skillId))
			{
				return -1;
			}
			else if ((skillId == SKILL_BLESSED_GOLDEN_SPICE) || (skillId == SKILL_BLESSED_CRYSTAL_SPICE) || (skillId == SKILL_SGRADE_GOLDEN_SPICE) || (skillId == SKILL_SGRADE_CRYSTAL_SPICE))
			{
				if (getRandom(100) < SPECIAL_SPICE_CHANCES[0])
				{
					if (getRandom(100) < SPECIAL_SPICE_CHANCES[1])
					{
						return _skillSuccessNpcIdList.get(skillId);
					}
					else if ((skillId == SKILL_BLESSED_GOLDEN_SPICE) || (skillId == SKILL_SGRADE_GOLDEN_SPICE))
					{
						return _skillSuccessNpcIdList.get(SKILL_GOLDEN_SPICE);
					}
					else
					{
						return _skillSuccessNpcIdList.get(SKILL_CRYSTAL_SPICE);
					}
				}
				
				return -1;
			}
			else if ((_growthLevel == 2) && (getRandom(100) < TAME_CHANCE))
			{
				return _tameNpcId;
			}
			else if (getRandom(100) < _chance)
			{
				return _skillSuccessNpcIdList.get(skillId);
			}
			else
			{
				return -1;
			}
		}
	}
	
	private static class TamedBeastSkillHolder
	{
		private final String name;
		private final SkillHolder[] sh;
		
		public TamedBeastSkillHolder(String beastName, SkillHolder... holders)
		{
			name = beastName;
			sh = holders;
		}
		
		public String getName()
		{
			return name;
		}
		
		public SkillHolder[] getSkills()
		{
			return sh;
		}
	}
	
	/**
	 * This is the main entry point for the {@code BeastFarm} class.<br>
	 * It initializes a new instance of the {@code BeastFarm} object.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new BeastFarm();
	}
}
