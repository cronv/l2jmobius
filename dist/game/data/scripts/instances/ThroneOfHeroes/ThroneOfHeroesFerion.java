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
package instances.ThroneOfHeroes;

import java.util.Collection;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * @author CostyKiller
 */
public class ThroneOfHeroesFerion extends InstanceScript
{
	// NPCs
	private static final int ROIENTAL = 34571;
	
	// Monsters
	private static final int FERION = 29448;
	private static final int FERION_MINION_PRAETORIAN = 29449;
	private static final int FERION_MINION_JUDGE_OF_LIGHT = 29450;
	
	// Throne's Treasure Chest Ferion
	private static final int TREASURE_CHEST = 29455;
	
	// Template
	private static final int TEMPLATE_ID = 364;
	
	// HP Threshold Minions Spawn Settings
	private static final int RITUAL_OF_LIGHT_HP_THRESHOLD_1 = 80; // Ritual of Light #1
	private static final int JUDGE_OF_LIGHT_HP_THRESHOLD_1 = 50; // Judge of Light 1st row of waves
	private static final int RITUAL_OF_LIGHT_HP_THRESHOLD_2 = 35; // Ritual of Light #2
	private static final int JUDGE_OF_LIGHT_HP_THRESHOLD_2 = 30; // Judge of Light 2nd row of waves
	
	// Minions Settings
	private static final int JUDGE_OF_LIGHT_SPAWN_COUNT = 20; // Minions spawned per wave
	private static final int JUDGE_OF_LIGHT_WAVE_REPEATS = 5; // Number of repeating waves
	private static final int JUDGE_OF_LIGHT_WAVE_MIN_DELAY = 10; // Seconds between waves (min)
	private static final int JUDGE_OF_LIGHT_WAVE_MAX_DELAY = 15; // Seconds between waves (max)
	private static final int JUDGE_OF_LIGHT_MIN_DIST = 400; // Spawn distance from Ferion (min)
	private static final int JUDGE_OF_LIGHT_MAX_DIST = 600; // Spawn distance from Ferion (max)
	private static final int JUDGE_OF_LIGHT_DECAY_DELAY = 3000; // ms after casting before the minion disappears
	private static final String MINIONS_SEALED = "MINIONS_SEALED";
	private static final String RITUAL_NEXT_STATUS = "RITUAL_NEXT_STATUS"; // Which status to advance to when a Ritual of Light is successfully interrupted
	
	// Skills
	private static final SkillHolder BLINDING_LIGHT = new SkillHolder(62543, 1); // Judge of Light minion casting skill
	private static final SkillHolder RING_OF_LIGHT = new SkillHolder(62544, 1); // Praetorian minion Casting Skill
	private static final int RING_OF_LIGHT_INTERVAL = 2; // Seconds between beam re-casts while the ritual is active - tune to match the skill's actual channel/animation length
	private static final int PROTECTION_LOST_HP_PERCENT = 20; // Ferion's Protection is lost below this HP%
	private static final SkillHolder REVERENCE_VEIL = new SkillHolder(39929, 1); // Sealing skill
	private static final SkillHolder CELESTIAL_MIGHT = new SkillHolder(62546, 1); // Ferion's long ritual cast
	private static final SkillHolder CELESTIAL_MIGHT_AWAKENING = new SkillHolder(62546, 2); // Same skill, lvl 2 - ritual failure: heals Ferion + buff
	
	// AVE - maybe they need update
	final AbnormalVisualEffect AVE_WORSHIP = AbnormalVisualEffect.V_BOSS_PERION_WORSHIP_AVE;
	final AbnormalVisualEffect AVE_SHIELD = AbnormalVisualEffect.V_BOSS_PERION_SHIELD_3LV_AVE;
	final AbnormalVisualEffect AVE_CHAINS = AbnormalVisualEffect.V_BOSS_PERION_SEAL_AVE;
	
	// NPC dialogs
	private static final int FERION_MESSAGES_DELAY = 60; // Seconds
	private static final NpcStringId[] FERION_MESSAGES =
	{
		NpcStringId.THE_HOLY_SWORD_OF_HEAVEN_WILL_STRIKE_YOU_DOWN,
	};
	
	public ThroneOfHeroesFerion()
	{
		super(TEMPLATE_ID);
		addInstanceCreatedId(TEMPLATE_ID);
		addStartNpc(ROIENTAL);
		addTalkId(ROIENTAL);
		addAttackId(FERION, FERION_MINION_PRAETORIAN);
		addKillId(FERION, FERION_MINION_PRAETORIAN, FERION_MINION_JUDGE_OF_LIGHT);
		addSpawnId(FERION, FERION_MINION_PRAETORIAN, FERION_MINION_JUDGE_OF_LIGHT);
		addSkillSeeId(FERION_MINION_PRAETORIAN);
		addSpellFinishedId(FERION);
	}
	
	@Override
	public void onInstanceCreated(Instance activeInstance, Player player)
	{
		activeInstance.setStatus(0);
		activeInstance.setParameter(MINIONS_SEALED, 0);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "enterInstance":
			{
				enterInstance(player, npc, TEMPLATE_ID);
				startQuestTimer("ANNOUNCE_RAID_START", 10000, null, player);
				break;
			}
			case "reenterInstance":
			{
				final Instance activeInstance = getPlayerInstance(player);
				if (isInInstance(activeInstance))
				{
					enterInstance(player, npc, activeInstance.getTemplateId());
				}
				break;
			}
			case "ANNOUNCE_RAID_START":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.THE_CHALLENGE_FOR_THE_THRONE_OF_HEROES_FERION_WILL_BEGIN_SHORTLY_PLEASE_GET_READY_HEROES, ExShowScreenMessage.TOP_CENTER, 5000, true);
					startQuestTimer("ANNOUNCE_RAID_PREP", 15000, null, player);
				}
				break;
			}
			case "ANNOUNCE_RAID_PREP":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.BATTLE_WITH_FERION_STARTS_IN_10_SEC, ExShowScreenMessage.TOP_CENTER, 5000, true);
					startQuestTimer("ANNOUNCE_5", 10000, null, player);
				}
				break;
			}
			case "ANNOUNCE_5":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.FIVE_SECONDS, ExShowScreenMessage.TOP_CENTER, 1000, true);
					startQuestTimer("ANNOUNCE_4", 1000, null, player);
				}
				break;
			}
			case "ANNOUNCE_4":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.FOUR_SECONDS, ExShowScreenMessage.TOP_CENTER, 1000, true);
					startQuestTimer("ANNOUNCE_3", 1000, null, player);
				}
				break;
			}
			case "ANNOUNCE_3":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.THREE_SECONDS_2, ExShowScreenMessage.TOP_CENTER, 1000, true);
					startQuestTimer("ANNOUNCE_2", 1000, null, player);
				}
				break;
			}
			case "ANNOUNCE_2":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.TWO_SECONDS_2, ExShowScreenMessage.TOP_CENTER, 1000, true);
					startQuestTimer("ANNOUNCE_1", 1000, null, player);
				}
				break;
			}
			case "ANNOUNCE_1":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.ONE_SECOND_2, ExShowScreenMessage.TOP_CENTER, 1000, true);
					startQuestTimer("SPAWN_FERION", 1000, null, player);
				}
				break;
			}
			case "SPAWN_FERION":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.I_AM_FERION_THE_ARCHANGEL_OF_RADIANCE_I_AM_HERE_TO_TEST_YOUR_VALOR_SHOW_ME_WHAT_YOU_ARE_CAPABLE_OF, ExShowScreenMessage.TOP_CENTER, 5000, true);
					world.spawnGroup("FERION");
				}
				break;
			}
			case "ANNOUNCE_FERION_SPAWNS_PRAETORIANS":
			{
				final Instance world = npc.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.PRAETORIANS_COMPLETE_THE_RITUAL_OF_LIGHT_BEFORE_THE_CELESTIAL_MIGHT_AWAKENS, ExShowScreenMessage.TOP_CENTER, 5000, true);
					startQuestTimer("FERION_SPAWNS_PRAETORIANS", 1000, npc, null);
				}
				break;
			}
			case "FERION_SPAWNS_PRAETORIANS":
			{
				final Instance world = npc.getInstanceWorld();
				if (isInInstance(world))
				{
					final Npc ferion = world.getNpc(FERION);
					showOnScreenMsg(world, NpcStringId.TO_REMOVE_THE_DEFENSIVE_BARRIER_DEFEAT_FERION_S_PRAETORIANS_AFTERWARDS_SEAL_THEM_WITH_THE_REVERENCE_VEIL_AND_STOP_THE_RITUAL_OF_LIGHT, ExShowScreenMessage.TOP_CENTER, 5000, true);
					world.spawnGroup("FERION_MINION_PRAETORIANS");
					ferion.setInvul(true);
					
					// Ferion begins the long Celestial Might cast; onSpellFinished decides pass/fail when it completes.
					showOnScreenMsg(world, NpcStringId.SOON_THE_CELESTIAL_MIGHT_WILL_AWAKEN, ExShowScreenMessage.TOP_CENTER, 5000, true);
					final Skill celestialMight = CELESTIAL_MIGHT.getSkill();
					ferion.setTarget(ferion);
					ferion.doCast(celestialMight);
					
					// Safety net only, in case onSpellFinished doesn't fire for some reason (cast interrupted/desynced).
					// Tied to the skill's own hit time rather than a guessed duration, plus a small buffer.
					startQuestTimer("RITUAL_OF_LIGHT_TIMEOUT", celestialMight.getHitTime() + 2000, npc, null);
				}
				break;
			}
			case "RITUAL_OF_LIGHT_TIMEOUT":
			{
				final Instance world = npc.getInstanceWorld();
				if (isInInstance(world) && npc.isInvul())
				{
					failRitualOfLight(world);
				}
				break;
			}
			case "RING_OF_LIGHT_CAST":
			{
				final Instance world = npc.getInstanceWorld();
				if (!isInInstance(world) || npc.isDead() || npc.isDecayed() || npc.getEffectList().hasAbnormalVisualEffect(AVE_CHAINS))
				{
					// Praetorian is gone, dead, or already sealed - stop the beam.
					cancelQuestTimer("RING_OF_LIGHT_CAST", npc, null);
					break;
				}
				
				final Npc ferion = world.getNpc(FERION);
				if ((ferion == null) || ferion.isDead())
				{
					cancelQuestTimer("RING_OF_LIGHT_CAST", npc, null);
					break;
				}
				
				npc.setTarget(ferion);
				npc.doCast(RING_OF_LIGHT.getSkill());
				break;
			}
			case "FERION_MESSAGES":
			{
				final Instance world = npc.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, FERION_MESSAGES[getRandom(FERION_MESSAGES.length)], ExShowScreenMessage.TOP_CENTER, 5000, true);
				}
				break;
			}
			case "JUDGE_OF_LIGHT_DECAY":
			{
				if (!npc.isDead() && !npc.isDecayed())
				{
					npc.deleteMe();
				}
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			final int hpPer = npc.getCurrentHpPercent();
			if (npc.getId() == FERION)
			{
				// Status legend:
				// 0 = pre-80% / reset state | 1 = Ritual #1 active | 2 = waiting for 50%
				// 3 = Judge of Light wave phase 1 (50%) active | 4 = waiting for 35%
				// 5 = Ritual #2 active | 6 = waiting for 30%
				// 7 = Judge of Light wave phase 2 (30%) active | 8 = done
				if ((hpPer <= RITUAL_OF_LIGHT_HP_THRESHOLD_1) && world.isStatus(0))
				{
					world.setParameter(RITUAL_NEXT_STATUS, 2);
					startQuestTimer("ANNOUNCE_FERION_SPAWNS_PRAETORIANS", 5000, npc, null);
					world.setStatus(1);
				}
				else if ((hpPer <= JUDGE_OF_LIGHT_HP_THRESHOLD_1) && world.isStatus(2))
				{
					world.setStatus(3);
					triggerJudgeOfLightWave(world, npc, 1, JUDGE_OF_LIGHT_WAVE_REPEATS, true);
				}
				else if ((hpPer <= RITUAL_OF_LIGHT_HP_THRESHOLD_2) && world.isStatus(4))
				{
					world.setParameter(RITUAL_NEXT_STATUS, 6);
					startQuestTimer("ANNOUNCE_FERION_SPAWNS_PRAETORIANS", 5000, npc, null);
					world.setStatus(5);
				}
				else if ((hpPer <= JUDGE_OF_LIGHT_HP_THRESHOLD_2) && world.isStatus(6))
				{
					world.setStatus(7);
					triggerJudgeOfLightWave(world, npc, 1, JUDGE_OF_LIGHT_WAVE_REPEATS, false);
				}
			}
			else if ((npc.getId() == FERION_MINION_PRAETORIAN) && (hpPer <= PROTECTION_LOST_HP_PERCENT) && npc.getEffectList().hasAbnormalVisualEffect(AVE_SHIELD))
			{
				// Ferion's Protection is lost below 20% HP; praetorian can now be sealed with the Reverence Veil.
				npc.getEffectList().stopAbnormalVisualEffect(AVE_SHIELD);
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			if (npc.getId() == FERION)
			{
				// Final message
				showOnScreenMsg(world, NpcStringId.EXCELLENT_HEAVEN_WILL_REMEMBER_YOU, ExShowScreenMessage.TOP_CENTER, 5000, true);
				// Despawn minions and stop timers
				cancelQuestTimer("ANNOUNCE_FERION_SPAWNS_PRAETORIANS", npc, null);
				cancelQuestTimer("FERION_SPAWNS_PRAETORIANS", npc, null);
				cancelQuestTimer("FERION_MESSAGES", npc, null);
				cancelQuestTimer("RITUAL_OF_LIGHT_TIMEOUT", npc, null);
				world.getAliveNpcs(FERION_MINION_PRAETORIAN).forEach(beast ->
				{
					cancelQuestTimer("RING_OF_LIGHT_CAST", beast, null);
					beast.doDie(null);
				});
				world.getAliveNpcs(FERION_MINION_JUDGE_OF_LIGHT).forEach(minion ->
				{
					cancelQuestTimer("JUDGE_OF_LIGHT_DECAY", minion, null);
					minion.doDie(null);
				});
				
				// Spawn treasure chests
				boolean eightCCMembersOrMore = ((killer.getCommandChannel() != null) && (killer.getCommandChannel().getMemberCount() >= 8));
				if (killer.isGM() || eightCCMembersOrMore)
				{
					addSpawn(TREASURE_CHEST, killer.getX() + getRandom(-150, 150), killer.getY() + getRandom(-150, 150), killer.getZ() + 20, 0, false, 0, true, world.getId());
					addSpawn(TREASURE_CHEST, killer.getX() + getRandom(-150, 150), killer.getY() + getRandom(-150, 150), killer.getZ() + 20, 0, false, 0, true, world.getId());
				}
				else
				{
					addSpawn(TREASURE_CHEST, killer.getX() + getRandom(-150, 150), killer.getY() + getRandom(-150, 150), killer.getZ() + 20, 0, false, 0, true, world.getId());
				}
				
				// Finish instance
				world.finishInstance(2);
				if (!killer.isGM())
				{
					// Set clan variable
					killer.getClan().getVariables().set("TOH_FERION_DONE", System.currentTimeMillis());
					killer.getClan().getVariables().storeMe();
				}
			}
			else if (npc.getId() == FERION_MINION_PRAETORIAN)
			{
				cancelQuestTimer("RING_OF_LIGHT_CAST", npc, null);
				updateSealedCount(world);
				checkPraetoriansCleared(world);
			}
			else if (npc.getId() == FERION_MINION_JUDGE_OF_LIGHT)
			{
				cancelQuestTimer("JUDGE_OF_LIGHT_DECAY", npc, null);
			}
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			if (npc.getId() == FERION)
			{
				startQuestTimer("FERION_MESSAGES", FERION_MESSAGES_DELAY * 1000, npc, null, true);
			}
			else if (npc.getId() == FERION_MINION_PRAETORIAN)
			{
				npc.getEffectList().startAbnormalVisualEffect(AVE_SHIELD);
				npc.disableCoreAI(true);
				npc.setRandomWalking(false);
				
				final Npc ferion = world.getNpc(FERION);
				if (ferion != null)
				{
					// Ferion gets worship ave
					ferion.getEffectList().startAbnormalVisualEffect(AVE_WORSHIP);
					ferion.disableCoreAI(true);
				}
				
				// Ring of Light is a continuous channel, simulated here by re-casting on a fixed interval
				// for as long as this praetorian is alive, unsealed, and Ferion is still around.
				startQuestTimer("RING_OF_LIGHT_CAST", RING_OF_LIGHT_INTERVAL * 1000, npc, null, true);
			}
			else if (npc.getId() == FERION_MINION_JUDGE_OF_LIGHT)
			{
				npc.disableCoreAI(true);
				npc.setRandomWalking(false);
				
				final Player nearestPlayer = getNearestPlayer(world, npc);
				if (nearestPlayer != null)
				{
					npc.setTarget(nearestPlayer);
					npc.doCast(BLINDING_LIGHT.getSkill());
				}
				
				// Casts once, then disappears shortly after - no sealing, no death reward, just a one-shot caster.
				startQuestTimer("JUDGE_OF_LIGHT_DECAY", JUDGE_OF_LIGHT_DECAY_DELAY, npc, null);
			}
		}
	}
	
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, Collection<WorldObject> targets, boolean isSummon)
	{
		if ((npc.getId() == FERION_MINION_PRAETORIAN) && !npc.isDead() && (skill.getId() == REVERENCE_VEIL.getSkillId()) && !npc.getEffectList().hasAbnormalVisualEffect(AVE_SHIELD))
		{
			// Only sealable once Ferion's Protection is gone (HP <= 20%); a veil cast while still protected does nothing.
			npc.setTarget(null);
			npc.abortCast();
			// npc.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, RING_OF_LIGHT.getSkillId());
			npc.setInvul(true);
			npc.getAI().setIntentionIdle();
			npc.getEffectList().startAbnormalVisualEffect(AVE_CHAINS);
			cancelQuestTimer("RING_OF_LIGHT_CAST", npc, null);
			
			final Instance world = npc.getInstanceWorld();
			if (isInInstance(world))
			{
				updateSealedCount(world);
				checkPraetoriansCleared(world);
			}
		}
	}
	
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if ((npc.getId() == FERION) && (skill.getId() == CELESTIAL_MIGHT.getSkillId()))
		{
			final Instance world = npc.getInstanceWorld();
			if (isInInstance(world))
			{
				if (npc.isInvul()) // still invul == ritual was never successfully interrupted
				{
					cancelQuestTimer("RITUAL_OF_LIGHT_TIMEOUT", npc, null);
					failRitualOfLight(world);
				}
			}
		}
	}
	
	/**
	 * Spawns {@code count} Judge of Light minions around Ferion at a random distance between {@link #JUDGE_OF_LIGHT_MIN_DIST} and {@link #JUDGE_OF_LIGHT_MAX_DIST}.<br>
	 * ASSUMPTION: distance is computed via polar coordinates (angle + radius) so the actual distance from Ferion falls within that range, rather than a plain X/Y offset jitter.
	 * @param world the instance world
	 * @param ferion Ferion's npc instance, used as the spawn center
	 * @param count how many minions to spawn
	 */
	private void spawnJudgeOfLightBurst(Instance world, Npc ferion, int count)
	{
		for (int i = 0; i < count; i++)
		{
			final double angle = Math.toRadians(getRandom(360));
			final int distance = getRandom(JUDGE_OF_LIGHT_MIN_DIST, JUDGE_OF_LIGHT_MAX_DIST);
			final int x = ferion.getX() + (int) (distance * Math.cos(angle));
			final int y = ferion.getY() + (int) (distance * Math.sin(angle));
			addSpawn(FERION_MINION_JUDGE_OF_LIGHT, x, y, ferion.getZ() + 20, 0, false, 0, true, world.getId());
		}
	}
	
	/**
	 * Fires one Judge of Light wave and, if more remain, schedules the next one after a random delay. On the final wave, advances world status to the next phase gate (current status + 1, relying on the sequential numbering documented in onAttack).
	 * @param world the instance world
	 * @param ferion Ferion's npc instance
	 * @param waveNumber the current wave (1-based)
	 * @param totalWaves total waves for this phase
	 * @param showIntroMessage whether to show the one-time phase-intro line before the first wave (phase 1 only, per design - phase 2 doesn't repeat it)
	 */
	private void triggerJudgeOfLightWave(Instance world, Npc ferion, int waveNumber, int totalWaves, boolean showIntroMessage)
	{
		if (ferion.isDead() || !isInInstance(world))
		{
			return;
		}
		
		if (showIntroMessage && (waveNumber == 1))
		{
			showOnScreenMsg(world, NpcStringId.NOT_BAD_LET_S_SEE_WHETHER_YOU_LL_BE_ABLE_TO_ENDURE_A_GREATER_POWER_OF_LIGHT, ExShowScreenMessage.TOP_CENTER, 5000, true);
		}
		
		showOnScreenMsg(world, NpcStringId.LOOK_UP_AND_TRY_TO_ENDURE_THE_RADIANCE, ExShowScreenMessage.TOP_CENTER, 5000, true);
		spawnJudgeOfLightBurst(world, ferion, JUDGE_OF_LIGHT_SPAWN_COUNT);
		
		if (waveNumber < totalWaves)
		{
			final int delaySeconds = getRandom(JUDGE_OF_LIGHT_WAVE_MIN_DELAY, JUDGE_OF_LIGHT_WAVE_MAX_DELAY);
			ThreadPool.schedule(() -> triggerJudgeOfLightWave(world, ferion, waveNumber + 1, totalWaves, showIntroMessage), delaySeconds * 1000L);
		}
		else
		{
			// All waves for this phase are done - advance to the next threshold gate.
			world.setStatus(world.getStatus() + 1);
		}
	}
	
	/**
	 * @param world the instance world
	 * @param npc the caster looking for a target
	 * @return the nearest living player, or null if none found
	 */
	private Player getNearestPlayer(Instance world, Npc npc)
	{
		Player nearest = null;
		double nearestDist = Double.MAX_VALUE;
		for (Player player : world.getPlayers())
		{
			if (player.isDead())
			{
				continue;
			}
			final double dist = npc.calculateDistance3D(player);
			if (dist < nearestDist)
			{
				nearestDist = dist;
				nearest = player;
			}
		}
		return nearest;
	}
	
	/**
	 * Recomputes the MINIONS_SEALED parameter from scratch based on currently alive praetorians.<br>
	 * This avoids incrementing a stale cached value once per alive minion, which previously caused undercounting whenever more than one praetorian was already sealed.
	 * @param world the instance world
	 */
	private void updateSealedCount(Instance world)
	{
		final long sealedCount = world.getAliveNpcs(FERION_MINION_PRAETORIAN).stream().filter(minion -> minion.getEffectList().hasAbnormalVisualEffect(AVE_CHAINS)).count();
		world.setParameter(MINIONS_SEALED, (int) sealedCount);
	}
	
	/**
	 * Lifts Ferion's invulnerability once every remaining praetorian is either dead or sealed.<br>
	 * Called both when a praetorian dies and when one is sealed, since sealing (not just killing) can be how players clear the last remaining minion. Advances world status to whichever phase should follow (stored in RITUAL_NEXT_STATUS, since this same logic now serves both Ritual of Light #1 and #2).
	 * @param world the instance world
	 */
	private void checkPraetoriansCleared(Instance world)
	{
		final boolean allSealedOrDead = world.getAliveNpcs(FERION_MINION_PRAETORIAN).stream().allMatch(minion -> minion.getEffectList().hasAbnormalVisualEffect(AVE_CHAINS));
		if (allSealedOrDead)
		{
			final Npc ferion = world.getNpc(FERION);
			if ((ferion != null) && !ferion.isDead())
			{
				showOnScreenMsg(world, NpcStringId.YOU_VE_MANAGED_TO_INTERRUPT_THE_RITUAL_NOT_BAD_NOT_BAD_STILL_YOUR_TRIAL_IS_NOT_OVER_YET, ExShowScreenMessage.TOP_CENTER, 5000, true);
				ferion.disableCoreAI(false);
				ferion.setInvul(false);
				ferion.getEffectList().stopAbnormalVisualEffect(AVE_WORSHIP);
				ferion.setTarget(null);
				ferion.abortCast();
				cancelQuestTimer("RITUAL_OF_LIGHT_TIMEOUT", ferion, null);
				world.setStatus(world.getParameters().getInt(RITUAL_NEXT_STATUS, 2));
			}
		}
	}
	
	/**
	 * Called when the Ritual of Light isn't stopped by the time Ferion's Celestial Might cast finishes.<br>
	 * Ferion becomes attackable again and casts Celestial Might: Awakening on himself, which restores his HP and applies the buff. The ritual is reset so players get another attempt at it.
	 * @param world the instance world
	 */
	private void failRitualOfLight(Instance world)
	{
		final Npc ferion = world.getNpc(FERION);
		if ((ferion == null) || ferion.isDead())
		{
			return;
		}
		
		// Ferion recovers his lost health - handled manually here (not solely relying on the skill effect),
		// plus the Celestial Might: Awakening cast for the visual/buff (skill 62546 lvl 2).
		ferion.setCurrentHp(ferion.getMaxHp(), true);
		ferion.getEffectList().stopAbnormalVisualEffect(AVE_WORSHIP);
		ferion.disableCoreAI(false);
		ferion.setInvul(false); // otherwise Ferion stays permanently invulnerable after a failed attempt
		ferion.setTarget(ferion);
		ferion.doCast(CELESTIAL_MIGHT_AWAKENING.getSkill());
		showOnScreenMsg(world, NpcStringId.BEHOLD_THE_CELESTIAL_POWER, ExShowScreenMessage.TOP_CENTER, 5000, true);
		
		// Clear whatever praetorians are left (sealed or not) so a fresh batch spawns next attempt.
		world.getAliveNpcs(FERION_MINION_PRAETORIAN).forEach(minion ->
		{
			cancelQuestTimer("RING_OF_LIGHT_CAST", minion, null);
			minion.deleteMe();
		});
		world.setParameter(MINIONS_SEALED, 0);
		
		// Ferion is attackable again; onAttack will re-trigger the ritual once HP drops past BOSS_HP_THRESHOLD again.
		world.setStatus(0);
	}
	
	public static void main(String[] args)
	{
		new ThroneOfHeroesFerion();
	}
}