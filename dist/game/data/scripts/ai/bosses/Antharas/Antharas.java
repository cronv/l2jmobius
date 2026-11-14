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
package ai.bosses.Antharas;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcInfo;

/**
 * This class manages the AI behavior for the boss {@code Antharas}.<br>
 * It handles state transitions, cinematic sequences, and minion management.<br>
 * The script also controls advanced skill rotations and scripted movement phases.
 * @author Notorion
 */
public class Antharas extends InstanceScript
{
	// NPCs
	private static final int FIGHT_ANTHARAS = 29387;
	private static final int GUIDE = 34543;
	private static final int ANTHARAS_SYMBOL = 29390;
	private static final int CLONE_FIGHT_ANTHARAS = 29388;
	
	// Skill Summon Antharas' Avatar
	private static final int CLONE_EFFECT_SKILL = 34312;
	
	// Skills
	private static final int ATTACK_SKILL_ID = 34309;
	private static final SkillHolder ATTACK_SKILL = new SkillHolder(ATTACK_SKILL_ID, 1);
	
	// Skill Earth Scratch
	private static final SkillHolder EARTH_SCRATCH_SKILL = new SkillHolder(34313, 1);
	private static final int TRAP_NPC_ID = 18919;
	
	// Buff Antharas' Earth Guard
	private static final int BUFF_ID = 34315;
	
	// Reward
	private static final int BELLRA_GREEN_CHEST = 82939;
	private static final int REWARD_COUNT = 1;
	private static final String INSTANCE_COMPLETED = "instance_completed";
	
	// Locations Antharas Symbol
	private static final Location[] SYMBOL_LOCATIONS_1 =
	{
		new Location(114348, 114884, -10576),
		new Location(113329, 114248, -10576),
		new Location(114438, 113140, -10576),
		new Location(115417, 114256, -10576)
	};
	private static final Location[] SYMBOL_LOCATIONS_2 =
	{
		new Location(114336, 115406, -10576),
		new Location(113179, 114292, -10576),
		new Location(114528, 112904, -10576),
		new Location(115736, 114377, -10576)
	};
	
	private static final Location BOSS_SPAWN_LOC = new Location(114388, 114277, -10592);
	private static final Location CLONE_SPAWN_LOC = new Location(113984, 114394, -10576);
	
	private static final int TEMPLATE_ID = 316;
	private static final long BOSS_SPAWN_DELAY = 5000; // 5 seconds
	private static final long CHECK_SYMBOL_INTERVAL = 1000; // 1 second
	
	/**
	 * Initializes a new instance of the {@code Antharas} class.<br>
	 * This constructor sets up the required IDs for talk, spawn, and combat events.
	 */
	public Antharas()
	{
		super(TEMPLATE_ID);
		addTalkId(GUIDE);
		addSpawnId(FIGHT_ANTHARAS);
		addAttackId(FIGHT_ANTHARAS);
		addKillId(FIGHT_ANTHARAS);
		addKillId(ANTHARAS_SYMBOL);
		addKillId(CLONE_FIGHT_ANTHARAS);
		addInstanceEnterId(TEMPLATE_ID);
		addSpawnId(ANTHARAS_SYMBOL);
	}
	
	/**
	 * Handles logic when a player enters the instance.<br>
	 * It checks if the instance is already completed before proceeding.<br>
	 * If it is the first time entering, it starts a quest timer.<br>
	 * It also schedules an {@code NpcInfo} packet to be sent after 2000ms if the boss is spawned.
	 * @param player The {@code Player} who entered the instance.
	 * @param instance The {@code Instance} being entered.
	 * @param firstEnter A boolean indicating if this is the player's first time entering.
	 */
	@Override
	protected void onEnter(Player player, Instance instance, boolean firstEnter)
	{
		super.onEnter(player, instance, firstEnter);
		if (instance.getParameters().getBoolean(INSTANCE_COMPLETED, false))
		{
			return;
		}
		
		if (firstEnter)
		{
			startQuestTimer("check_symbols", CHECK_SYMBOL_INTERVAL, null, null);
		}
		
		final Npc fightAntharas = instance.getParameters().getObject("fightAntharas", Npc.class);
		if ((fightAntharas != null) && fightAntharas.isSpawned())
		{
			ThreadPool.schedule(() ->
			{
				if (player.isOnline() && (player.getInstanceWorld() == instance))
				{
					player.sendPacket(new NpcInfo(fightAntharas));
				}
			}, 2000);
		}
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It handles the initialization logic for specific NPCs in the instance.<br>
	 * If the NPC is a symbol, it starts a quest timer and checks all symbols.
	 * @param npc The {@code Npc} instance that has just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		final Instance world = npc.getInstanceWorld();
		if ((world != null) && (npc.getId() == ANTHARAS_SYMBOL))
		{
			// Timer buff Antharas.
			startQuestTimer("add_symbol_buff", 10000, npc, null);
			checkSymbols(world);
		}
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This method checks if the target is the main boss to trigger specific events.<br>
	 * It ensures that certain quest timers are started only once per instance.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt in the attack.
	 * @param isSummon Indicates if the attack was performed by a summoned entity.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.getId() == FIGHT_ANTHARAS)
		{
			final Instance world = npc.getInstanceWorld();
			if (world != null)
			{
				if (!world.getParameters().getBoolean("earthScratchStarted", false))
				{
					world.getParameters().set("earthScratchStarted", true);
					
					startQuestTimer("manage_earth_scratch", 90000, npc, null);
				}
			}
		}
		super.onAttack(npc, attacker, damage, isSummon);
	}
	
	/**
	 * Handles various game events for the Antharas boss encounter.<br>
	 * This method processes logic such as entering/exiting the nest, camera sequences, and combat mechanics.<br>
	 * It manages player teleportation, quest timers, and NPC behaviors based on the provided event type.
	 * @param event The name of the event to be processed.
	 * @param npc The {@code Npc} instance associated with the boss or encounter.
	 * @param player The {@code Player} who triggered the event.
	 * @return A {@code String} representing the HTML text for UI display, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final Instance world = npc != null ? npc.getInstanceWorld() : player != null ? player.getInstanceWorld() : null;
		if (world != null)
		{
			switch (event)
			{
				case "antharas_attack":
				{
					if ((npc != null) && !npc.isDead())
					{
						final Player target = World.getRandomVisibleObjectInRange(npc, Player.class, 1000);
						if (target != null)
						{
							npc.getAI().setIntentionCast(ATTACK_SKILL.getSkill(), target);
						}
					}
					break;
				}
				case "check_symbols":
				{
					checkSymbols(world);
					break;
				}
				case "spawn_extra_symbol":
				{
					if (!world.getParameters().getBoolean("extraSymbolSpawned", false))
					{
						world.setParameter("extraSymbolSpawned", true);
						Location extraLoc = new Location(114336, 115406, -10576);
						final Npc extraSymbol = addSpawn(ANTHARAS_SYMBOL, extraLoc, false, 0, false, world.getId());
						final List<Npc> symbols = world.getParameters().getList("SYMBOLS", Npc.class, new ArrayList<>());
						symbols.add(extraSymbol);
						
						checkSymbols(world);
					}
					break;
				}
				case "manage_earth_scratch":
				{
					final Npc fightAntharas = world.getNpc(FIGHT_ANTHARAS);
					if ((fightAntharas != null) && !fightAntharas.isDead())
					{
						if (!fightAntharas.isInCombat())
						{
							world.setParameter("earthScratchStarted", false);
							break;
						}
						
						final List<Player> players = new ArrayList<>(world.getPlayers());
						players.removeIf(p -> p.isDead());
						
						int count = 0;
						while (!players.isEmpty() && (count < 6))
						{
							final Player target = getRandomEntry(players);
							players.remove(target);
							
							if (target != null)
							{
								final Npc trap = addSpawn(TRAP_NPC_ID, target.getLocation(), false, 7000, false, world.getId());
								trap.setTarget(trap);
								trap.doCast(EARTH_SCRATCH_SKILL.getSkill());
							}
							count++;
						}
						startQuestTimer("manage_earth_scratch", 90000, fightAntharas, null);
					}
					break;
				}
				case "spawn_fight_antharas":
				{
					final Npc fightAntharas = addSpawn(FIGHT_ANTHARAS, BOSS_SPAWN_LOC, false, 0, false, world.getId());
					fightAntharas.setRandomWalking(false);
					world.setParameter("fightAntharas", fightAntharas);
					startQuestTimer("check_antharas_hp", 5000, fightAntharas, null, true);
					world.broadcastPacket(new NpcInfo(fightAntharas));
					break;
				}
				case "check_antharas_hp":
				{
					checkBossHP(npc, world);
					break;
				}
				case "spawn_clones":
				{
					for (int i = 0; i < 2; i++)
					{
						final Npc clone = addSpawn(CLONE_FIGHT_ANTHARAS, CLONE_SPAWN_LOC, false, 0, false, world.getId());
						if (clone != null)
						{
							final SkillHolder skillHolder = new SkillHolder(CLONE_EFFECT_SKILL, 1);
							final Skill skill = skillHolder.getSkill();
							if (skill != null)
							{
								SkillCaster.triggerCast(clone, clone, skill);
							}
						}
					}
					
					if (npc != null)
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, "Not bad, I can show you my abilities. My clones will show you my true power.");
					}
					break;
				}
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Monitors the health of the boss to trigger specific events.<br>
	 * It spawns symbols and clones based on HP percentage thresholds.<br>
	 * This method updates world parameters and triggers quest timers.
	 * @param mainBoss The {@code Npc} instance representing the primary boss.
	 * @param world The {@code Instance} where the fight is taking place.
	 */
	private void checkBossHP(Npc mainBoss, Instance world)
	{
		if ((mainBoss != null) && !mainBoss.isDead())
		{
			final double currentHP = mainBoss.getCurrentHp();
			final long maxHP = mainBoss.getMaxHp();
			final int currentHPPercentage = (int) ((currentHP / maxHP) * 100);
			
			// 75% HP
			if ((currentHPPercentage <= 75) && !world.getParameters().getBoolean("firstSymbolsSpawned", false))
			{
				world.setParameter("firstSymbolsSpawned", true);
				
				for (Location loc : SYMBOL_LOCATIONS_1)
				{
					final Npc symbol = addSpawn(ANTHARAS_SYMBOL, loc, false, 0, false, world.getId());
					final List<Npc> symbols = world.getParameters().getList("SYMBOLS", Npc.class, new ArrayList<>());
					symbols.add(symbol);
				}
				
				world.setParameter("symbols_spawned", true);
				mainBoss.broadcastSay(ChatType.NPC_GENERAL, "It's been a while since I face worthy adversaries. I'll show you my power. I will receive the power of the earth once again.");
				
				startQuestTimer("spawn_extra_symbol", 60000, mainBoss, null);
			}
			
			// 15% HP
			if ((currentHPPercentage <= 15) && !world.getParameters().getBoolean("symbolsSpawned15", false))
			{
				world.setParameter("symbolsSpawned15", true);
				world.setParameter("secondSymbolsSpawned", true);
				world.setParameter("wave2KillCount", 0);
				
				for (Location loc : SYMBOL_LOCATIONS_2)
				{
					final Npc symbol = addSpawn(ANTHARAS_SYMBOL, loc, false, 0, false, world.getId());
					final List<Npc> symbols = world.getParameters().getList("SYMBOLS", Npc.class, new ArrayList<>());
					symbols.add(symbol);
					symbol.getVariables().set("isWave2", true);
				}
				
				world.setParameter("symbols_spawned", true);
				mainBoss.broadcastSay(ChatType.NPC_GENERAL, "It's been a while since I face worthy adversaries. I'll show you my power. I will receive the power of the earth once again.");
				checkSymbols(world);
			}
			
			checkSymbols(world);
			
			// 50% HP
			if ((currentHPPercentage <= 50) && !world.getParameters().getBoolean("clonesSpawned", false))
			{
				world.setParameter("clonesSpawned", true);
				startQuestTimer("spawn_clones", 100, mainBoss, null);
				world.setParameter("clones_spawned", true);
			}
		}
	}
	
	/**
	 * Checks for the presence of symbols in the world.<br>
	 * It manages the buff applied to the boss based on symbol count.<br>
	 * This method updates the {@code buffApplied} parameter accordingly.
	 * @param world The {@code Instance} where the check is performed.
	 */
	private void checkSymbols(Instance world)
	{
		final Npc fightAntharas = world.getNpc(FIGHT_ANTHARAS);
		if (fightAntharas != null)
		{
			if (world.getAliveNpcCount(ANTHARAS_SYMBOL) > 0)
			{
				if (!world.getParameters().getBoolean("buffApplied", false))
				{
					final Skill skill = new SkillHolder(BUFF_ID, 1).getSkill();
					if (skill != null)
					{
						SkillCaster.triggerCast(fightAntharas, fightAntharas, skill);
						world.getParameters().set("buffApplied", true);
					}
				}
			}
			else if (world.getParameters().getBoolean("buffApplied", false))
			{
				fightAntharas.getEffectList().stopSkillEffects(null, BUFF_ID);
				world.getParameters().set("buffApplied", false);
			}
		}
	}
	
	/**
	 * This method is called when an {@code Npc} is killed.<br>
	 * It handles the death logic for Antharas and his minions.<br>
	 * If Antharas dies, it triggers respawn timers and visual effects.<br>
	 * If a minion dies inside the nest zone, it updates the minion count.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who dealt the killing blow.
	 * @param isSummon Indicates if the kill was performed by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (world == null)
		{
			return;
		}
		
		final int npcId = npc.getId();
		if (npcId == ANTHARAS_SYMBOL)
		{
			final List<Npc> symbols = world.getParameters().getList("SYMBOLS", Npc.class, new ArrayList<>());
			symbols.remove(npc);
			
			final boolean firstWaveDone = world.getParameters().getBoolean("firstSymbolsSpawned", false);
			final boolean extraSymbolSpawned = world.getParameters().getBoolean("extraSymbolSpawned", false);
			
			if (symbols.isEmpty())
			{
				final Npc fightAntharas = world.getNpc(FIGHT_ANTHARAS);
				if ((fightAntharas != null) && world.getParameters().getBoolean("buffApplied", false))
				{
					fightAntharas.getEffectList().stopSkillEffects(null, BUFF_ID);
					world.getParameters().set("buffApplied", false);
				}
			}
			
			if (symbols.isEmpty() && firstWaveDone && extraSymbolSpawned && !world.getParameters().getBoolean("msgWave1Sent", false))
			{
				final Npc fightAntharas = world.getNpc(FIGHT_ANTHARAS);
				if (fightAntharas != null)
				{
					fightAntharas.broadcastSay(ChatType.NPC_GENERAL, "All my symbols were destroyed!");
					world.setParameter("msgWave1Sent", true);
				}
			}
			
			if (npc.getVariables().getBoolean("isWave2", false))
			{
				final int killCount = world.getParameters().getInt("wave2KillCount", 0) + 1;
				world.setParameter("wave2KillCount", killCount);
				
				if ((killCount >= 4) && !world.getParameters().getBoolean("msgWave2Sent", false))
				{
					final Npc fightAntharas = world.getNpc(FIGHT_ANTHARAS);
					if (fightAntharas != null)
					{
						fightAntharas.broadcastSay(ChatType.NPC_GENERAL, "All my symbols were destroyed!");
						world.setParameter("msgWave2Sent", true);
					}
				}
			}
		}
		else if (npcId == FIGHT_ANTHARAS)
		{
			world.getParameters().set(INSTANCE_COMPLETED, true);
			for (Player player : world.getPlayers())
			{
				if ((player != null) && player.isOnline())
				{
					player.addItem(ItemProcessType.REWARD, BELLRA_GREEN_CHEST, REWARD_COUNT, player, true);
				}
			}
			
			for (Npc symbol : world.getNpcs(ANTHARAS_SYMBOL))
			{
				symbol.deleteMe();
			}
			
			for (Npc clone : world.getNpcs(CLONE_FIGHT_ANTHARAS))
			{
				clone.deleteMe();
			}
			
			cancelQuestTimers("check_antharas_hp");
			cancelQuestTimers("manage_earth_scratch");
			cancelQuestTimers("spawn_extra_symbol");
			world.getParameters().remove("fightAntharas");
			world.setParameter("symbolsSpawned15", false);
			world.setParameter("clonesSpawned", false);
			world.setParameter("firstSymbolsSpawned", false);
			world.setParameter("secondSymbolsSpawned", false);
			world.setParameter("extraSymbolSpawned", false);
			world.setParameter("earthScratchStarted", false);
			world.setParameter("msgWave1Sent", false);
			world.setParameter("msgWave2Sent", false);
			cancelQuestTimers("antharas_attack");
			finishInstance(killer);
		}
	}
	
	/**
	 * Handles the logic when a {@code Player} interacts with an {@code Npc}.<br>
	 * It enters the instance and manages the boss spawn timer if necessary.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} who initiated the interaction.
	 * @return A {@code String} result from the super class method.
	 */
	@Override
	public String onTalk(Npc npc, Player player)
	{
		enterInstance(player, npc, TEMPLATE_ID);
		
		final Instance world = player.getInstanceWorld();
		if (world != null)
		{
			if (world.getParameters().getBoolean(INSTANCE_COMPLETED, false))
			{
				return super.onTalk(npc, player);
			}
			
			final Npc fightAntharas = world.getParameters().getObject("fightAntharas", Npc.class);
			if ((fightAntharas == null) || !fightAntharas.isSpawned())
			{
				startQuestTimer("spawn_fight_antharas", BOSS_SPAWN_DELAY, null, player);
				world.setParameter("bossSpawned", true);
			}
		}
		
		return super.onTalk(npc, player);
	}
	
	/**
	 * This is the entry point for the {@code Antharas} class.<br>
	 * It initializes a new instance of the boss entity.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Antharas();
	}
}
