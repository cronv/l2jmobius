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
package ai.others.CursedWeaponAttack;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttack;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSkillUse;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

/**
 * Handles the mechanics for {@code CursedWeaponAttack} and the Prison of Souls system.<br>
 * This class manages specific AI behaviors related to cursed weapon interactions.
 * @author Notorion
 */
public class CursedWeaponAttack extends Script
{
	// Logger instance for this class
	private static final Logger LOGGER = Logger.getLogger(CursedWeaponAttack.class.getName());
	
	// Skill IDs
	private static final int SKILL_ID_ZARICHE = 35404; // Prison of Souls - Zariche.
	private static final int SKILL_ID_AKAMANAH = 35405; // Prison of Souls - Akamanah.
	
	// Mechanics constants
	private static final int HITS_TO_BREAK = 540; // 540 Hits required to break.
	private static final int DURATION_SECONDS = 120; // 2 Minutes duration.
	private static final int AUTO_TRIGGER_COOLDOWN = 3000; // 3 seconds reuse
	
	// Scar skills
	private static final int SKILL_SCAR_ZARICHE = 35521; // Soul Scar - Zariche - 130sec.
	private static final int SKILL_SCAR_AKAMANAH = 35522; // Soul Scar - Akamanah - 130sec.
	
	// Fallback location if teleport fails.
	private static final Location FALLBACK_LOC = new Location(17860, 170170, -3507);
	
	// Active prisoners map (ObjectId -> PrisonData).
	private static final Map<Integer, PrisonData> _prisoners = new ConcurrentHashMap<>();
	
	// Track the last time a player auto-triggered the prison (ObjectId -> LastTimeMillis)
	private static final Map<Integer, Long> _lastTriggerTimes = new ConcurrentHashMap<>();
	
	private static class PrisonData
	{
		final int skillId;
		final AtomicInteger hitCount = new AtomicInteger(0);
		final int victimObjectId;
		volatile ScheduledFuture<?> teleportTask;
		
		PrisonData(int skillId, int objId)
		{
			this.skillId = skillId;
			this.victimObjectId = objId;
		}
		
		synchronized void cancelTask()
		{
			if ((teleportTask != null) && !teleportTask.isDone())
			{
				teleportTask.cancel(false);
				teleportTask = null;
			}
		}
	}
	
	/**
	 * Initializes the {@code CursedWeaponAttack} script.<br>
	 * Registers global listeners for skill use, attacks, logins, and logouts.
	 */
	public CursedWeaponAttack()
	{
		LOGGER.info("CursedWeaponAttack initialized: registering global event Cursed Weapons Defense.");
		
		// 1. Skill Use.
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_CREATURE_SKILL_USE, event -> onCreatureSkillUse((OnCreatureSkillUse) event), this));
		
		// 2. Attack.
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_CREATURE_ATTACK, event -> onCreatureAttack((OnCreatureAttack) event), this));
		
		// 3. Player Logout.
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_LOGOUT, event -> onPlayerLogout((OnPlayerLogout) event), this));
		
		// Player Login.
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_LOGIN, event -> onPlayerLogin((OnPlayerLogin) event), this));
	}
	
	/**
	 * Handles the logic when a creature uses a skill.<br>
	 * This method checks if the caster is a {@code Player}.<br>
	 * It manages imprisonment effects for specific skills.<br>
	 * It also handles magic attacks against existing prisoners.
	 * @param event The {@code OnCreatureSkillUse} event containing the skill and caster data.
	 */
	public void onCreatureSkillUse(OnCreatureSkillUse event)
	{
		if (!event.getCaster().isPlayer())
		{
			return;
		}
		
		final Player caster = event.getCaster().asPlayer();
		final Skill skill = event.getSkill();
		final int skillId = skill.getId();
		
		final Creature target = (Creature) caster.getTarget();
		
		// Automatic imprisonment system (Trigger every 3s)
		if (caster.isCursedWeaponEquipped() && skill.hasNegativeEffect() && (skillId != SKILL_ID_ZARICHE) && (skillId != SKILL_ID_AKAMANAH))
		{
			long now = System.currentTimeMillis();
			final int objectId = caster.getObjectId();
			long lastTrigger = _lastTriggerTimes.getOrDefault(objectId, 0L);
			
			if ((now - lastTrigger) >= AUTO_TRIGGER_COOLDOWN)
			{
				boolean appliedToSomeone = false;
				int autoSkillId = (caster.getCursedWeaponEquippedId() == 8190) ? SKILL_ID_ZARICHE : SKILL_ID_AKAMANAH;
				Skill autoSkill = SkillData.getInstance().getSkill(autoSkillId, 1);
				
				if (autoSkill != null)
				{
					// If the skill is Aura/No target (e.g., Provoke), the check center is the caster itself
					WorldObject mainTarget = (target != null) ? target : caster;
					
					// The engine lists exactly who will be affected by this skill while automatically respecting Peace Zones, PvP, Party, Clan, and Flag.
					final List<WorldObject> affectedTargets = skill.getTargetsAffected(caster, mainTarget);
					
					if (affectedTargets != null)
					{
						for (WorldObject obj : affectedTargets)
						{
							if ((obj != null) && obj.isPlayer() && (obj != caster))
							{
								final Player victim = obj.asPlayer();
								
								// Imprisons only those who passed the official engine filter
								if (!victim.isDead() && !victim.isCursedWeaponEquipped() && !_prisoners.containsKey(victim.getObjectId()))
								{
									autoSkill.applyEffects(caster, victim);
									registerPrisoner(caster, victim, autoSkillId);
									appliedToSomeone = true;
								}
							}
						}
					}
					
					if (appliedToSomeone)
					{
						_lastTriggerTimes.put(objectId, now);
					}
				}
			}
		}
		
		// Manual skill system
		if ((skillId == SKILL_ID_ZARICHE) || (skillId == SKILL_ID_AKAMANAH))
		{
			if ((target == null) || !target.isPlayer())
			{
				return;
			}
			
			final Player victim = target.asPlayer();
			registerPrisoner(caster, victim, skillId);
			return;
		}
		
		// Magic attack on prisoner.
		if (!_prisoners.isEmpty() && (target != null) && target.isPlayer())
		{
			// Checks if the target took magic damage to subtract one hit from the barrier
			if (skill.hasNegativeEffect() && (skillId != SKILL_ID_ZARICHE) && (skillId != SKILL_ID_AKAMANAH))
			{
				checkHit(target.asPlayer(), caster);
			}
		}
	}
	
	// Record of imprisoned player
	/**
	 * Registers a victim as a prisoner in the system.<br>
	 * This method handles the logic for imprisoning a player using a specific skill.<br>
	 * It prevents imprisonment if the victim is already holding a cursed weapon.<br>
	 * It also ensures that a player cannot be imprisoned more than once at a time.
	 * @param caster The player who performs the action.
	 * @param victim The target player being imprisoned.
	 * @param skillId The unique identifier for the skill used to imprison the victim.
	 */
	private void registerPrisoner(Player caster, Player victim, int skillId)
	{
		// Holder protection block.
		if (victim.isCursedWeaponEquipped())
		{
			ThreadPool.schedule(() ->
			{
				Skill s = SkillData.getInstance().getSkill(skillId, 1);
				if (s != null)
				{
					victim.stopSkillEffects(s);
				}
				
				// Extra visual cleanup for safety.
				victim.getEffectList().stopAbnormalVisualEffect(AbnormalVisualEffect.ZARICHE_PRISION_AVE);
				
				// victim.sendMessage("The prison effect does not work on another cursed weapon holder.");
			}, 500);
			
			caster.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final int objectId = victim.getObjectId();
		final PrisonData newData = new PrisonData(skillId, objectId);
		final PrisonData existing = _prisoners.putIfAbsent(objectId, newData);
		
		if (existing != null)
		{
			caster.sendPacket(SystemMessageId.INVALID_TARGET);
			caster.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// LOGGER.info("CursedWeaponAttack: " + victim.getName() + " imprisoned. Timer set to 120s.");
		
		newData.teleportTask = ThreadPool.schedule(() -> executePrisonEnd(newData), DURATION_SECONDS * 1000);
	}
	
	/**
	 * This method handles the logic when a creature performs an attack.<br>
	 * It checks if the target is a {@code Player}.<br>
	 * If valid, it triggers the hit check for prisoners.
	 * @param event The {@code OnCreatureAttack} event containing attacker and target data.
	 */
	public void onCreatureAttack(OnCreatureAttack event)
	{
		if (_prisoners.isEmpty())
		{
			return;
		}
		
		final Creature target = event.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			return;
		}
		
		checkHit(target.asPlayer(), event.getAttacker());
	}
	
	/**
	 * Checks if a {@code Player} has been hit enough times to break the curse.<br>
	 * It increments the hit counter for the victim.<br>
	 * If the limit is reached, it calls the {@code liberate} method.
	 * @param victim The {@code Player} who was struck by an attack.
	 * @param attacker The {@code Creature} that performed the attack.
	 */
	private void checkHit(Player victim, Creature attacker)
	{
		final PrisonData data = _prisoners.get(victim.getObjectId());
		if (data == null)
		{
			return;
		}
		
		final int hits = data.hitCount.incrementAndGet();
		
		// logger to confirm hits
		// LOGGER.info("CursedWeaponAttack: Hit " + hits + "/" + HITS_TO_BREAK + " in " + victim.getName());
		
		if (hits >= HITS_TO_BREAK)
		{
			liberate(victim, data, attacker);
		}
	}
	
	/**
	 * Releases a {@code Player} from their current prison state.<br>
	 * This method cancels active tasks and removes the victim from the prisoner list.<br>
	 * It also stops any specific skill effects associated with the prison.
	 * @param victim The {@code Player} being released.
	 * @param data The {@code PrisonData} containing information about the current imprisonment.
	 * @param attacker The {@code Creature} that triggered the liberation.
	 */
	private void liberate(Player victim, PrisonData data, Creature attacker)
	{
		// LOGGER.info("CursedWeaponAttack: " + victim.getName() + " RELEASED!");
		
		data.cancelTask();
		_prisoners.remove(victim.getObjectId());
		
		final Skill s = SkillData.getInstance().getSkill(data.skillId, 1);
		if (s != null)
		{
			victim.stopSkillEffects(s);
		}
		
		int scarId = 0;
		
		if (data.skillId == SKILL_ID_ZARICHE)
		{
			scarId = SKILL_SCAR_ZARICHE; // 35521
		}
		else if (data.skillId == SKILL_ID_AKAMANAH)
		{
			scarId = SKILL_SCAR_AKAMANAH; // 35522
		}
		
		if (scarId > 0)
		{
			Skill scarSkill = SkillData.getInstance().getSkill(scarId, 1);
			if (scarSkill != null)
			{
				victim.stopSkillEffects(scarSkill);
			}
		}
		
		// victim.sendMessage("The Limit Barrier has been destroyed!");
		// if ((attacker != null) && attacker.isPlayer())
		// {
		// attacker.sendMessage("You helped break the Prison of Souls!");
		// }
	}
	
	/**
	 * Handles the completion of a prison effect for a victim.<br>
	 * It removes visual effects and teleports the player to a town.<br>
	 * The method also schedules the removal of the prisoner record after a delay.
	 * @param expectedData The data containing the details of the imprisonment to end.
	 */
	private void executePrisonEnd(PrisonData expectedData)
	{
		try
		{
			final PrisonData currentData = _prisoners.get(expectedData.victimObjectId);
			if ((currentData == null) || (currentData != expectedData))
			{
				return;
			}
			
			// The ID remains imprisoned to avoid bugs during loading.
			final Player victim = World.getPlayer(expectedData.victimObjectId);
			
			if ((victim == null) || !victim.isOnline())
			{
				return;
			}
			
			// Remove visual effects.
			final Skill s = SkillData.getInstance().getSkill(expectedData.skillId, 1);
			if (s != null)
			{
				victim.stopSkillEffects(s);
			}
			
			int scarId = 0;
			
			if (expectedData.skillId == SKILL_ID_ZARICHE)
			{
				scarId = SKILL_SCAR_ZARICHE;
			}
			
			else if (expectedData.skillId == SKILL_ID_AKAMANAH)
			{
				scarId = SKILL_SCAR_AKAMANAH;
			}
			
			if (scarId > 0)
			{
				Skill scarSkill = SkillData.getInstance().getSkill(scarId, 1);
				if (scarSkill != null)
				{
					victim.stopSkillEffects(scarSkill);
				}
			}
			
			if (!victim.isDead())
			{
				Location loc = MapRegionData.getInstance().getTeleToLocation(victim, TeleportWhereType.TOWN);
				if (loc == null)
				{
					loc = FALLBACK_LOC;
				}
				
				victim.teleToLocation(loc, true);
				// LOGGER.info("CursedWeaponAttack: teleported " + victim.getName());
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("CursedWeaponAttack Error: " + e.getMessage());
		}
		finally
		{
			ThreadPool.schedule(() ->
			{
				_prisoners.remove(expectedData.victimObjectId);
			}, 2000); // Seconds of immunity against new imprisonment.
		}
	}
	
	/**
	 * This method is called when a player logs out of the game.<br>
	 * It checks if the logging out player is currently in prison.<br>
	 * If they are, it removes them from the prisoner list and cancels their task.
	 * @param event The {@code OnPlayerLogout} event containing the player data.
	 */
	public void onPlayerLogout(OnPlayerLogout event)
	{
		if (_prisoners.isEmpty())
		{
			return;
		}
		
		final PrisonData data = _prisoners.remove(event.getPlayer().getObjectId());
		if (data != null)
		{
			data.cancelTask();
		}
	}
	
	/**
	 * This method is called when a player logs into the game.<br>
	 * It checks if the player has specific active buffs.<br>
	 * If they do, it reactivates their prison task based on the remaining buff time.
	 * @param event The {@code OnPlayerLogin} event containing the player data.
	 */
	public void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		// 1. Check if the player has the prison skill.
		final boolean hasZariche = player.getEffectList().isAffectedBySkill(SKILL_ID_ZARICHE);
		final boolean hasAkamanah = player.getEffectList().isAffectedBySkill(SKILL_ID_AKAMANAH);
		
		if (!hasZariche && !hasAkamanah)
		{
			return;
		}
		
		final int skillId = hasZariche ? SKILL_ID_ZARICHE : SKILL_ID_AKAMANAH;
		
		// 2. Read the actual remaining time on the buff.
		long remainingTime = 3000L; // Minimum safety.
		
		final BuffInfo info = player.getEffectList().getBuffInfoBySkillId(skillId);
		if (info != null)
		{
			long val = info.getTime() * 1000L;
			if (val > 0)
			{
				remainingTime = val;
			}
		}
		
		// LOGGER.info("CursedWeaponAttack: Fugitive " + player.getName() + " recaptured. Remaining time: " + (remainingTime / 1000) + "s");
		
		// 3. Reactivate the task in the script.
		final int objectId = player.getObjectId();
		final PrisonData newData = new PrisonData(skillId, objectId);
		_prisoners.put(objectId, newData);
		
		newData.teleportTask = ThreadPool.schedule(() -> executePrisonEnd(newData), remainingTime);
	}
	
	// Receives the commands sent by AdminCursedWeapons.java
	/**
	 * Handles specific custom events for the cursed weapon system.<br>
	 * This method allows administrators to manually free players from the Soul Prison.<br>
	 * It checks if the {@code player} is a Game Master and processes the request based on the {@code event} string.
	 * @param event The name of the event to trigger, such as "admin_cw_unprison".
	 * @param npc The {@code Npc} involved in the event, can be {@code null}.
	 * @param player The {@code Player} who triggered the event.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("admin_cw_unprison") && (player != null) && player.isGM())
		{
			Creature target = (Creature) player.getTarget();
			
			if ((target != null) && target.isPlayer())
			{
				Player victim = target.asPlayer();
				PrisonData data = _prisoners.get(victim.getObjectId());
				
				if (data != null)
				{
					liberate(victim, data, player);
					player.sendMessage("GM: You have freed " + victim.getName() + " from the Soul Prison.");
					victim.sendMessage("A Game Master has freed you from the Soul Prison.");
				}
				else
				{
					player.sendMessage("GM: This player is not imprisoned.");
				}
			}
			else
			{
				player.sendMessage("GM: Select a player and type //cw_unprison.");
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This is the entry point for the {@code CursedWeaponAttack} class.<br>
	 * It initializes a new instance of the class to start the logic.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new CursedWeaponAttack();
	}
}
