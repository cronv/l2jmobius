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
package instances.KartiasLabyrinth;

import java.util.List;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.FriendlyNpc;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttacked;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.mechanics.events.holders.instance.OnInstanceStatusChange;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents the Guardian Spirit of Kartia Helper Eliyah.<br>
 * This script handles the behavior and logic for this specific entity within the {@code KartiasLabyrinth} instance.
 * @author flanagak
 */
public class KartiaHelperGuardian extends Script
{
	// NPCs
	private static final int[] KARTIA_FRIENDS =
	{
		33617, // Elise (Kartia 85)
		33628, // Elise (Kartia 90)
		33639, // Elise (Kartia 95)
		33609, // Adolph (Kartia 85)
		33620, // Adolph (Kartia 90)
		33631, // Adolph (Kartia 95)
		33611, // Barton (Kartia 85)
		33622, // Barton (Kartia 90)
		33633, // Barton (Kartia 95)
		33615, // Eliyah (Kartia 85)
		33626, // Eliyah (Kartia 90)
		33637, // Eliyah (Kartia 95)
		33613, // Hayuk (Kartia 85)
		33624, // Hayuk (Kartia 90)
		33635, // Hayuk (Kartia 95)
		33618, // Eliyah's Guardian Spirit (Kartia 85)
		33629, // Eliyah's Guardian Spirit (Kartia 90)
		33640, // Eliyah's Guardian Spirit (Kartia 95)
	};
	private static final int[] KARTIA_GUARDIANS =
	{
		33618, // Eliyah's Guardian Spirit (Kartia 85)
		33629, // Eliyah's Guardian Spirit (Kartia 90)
		33640, // Eliyah's Guardian Spirit (Kartia 95)
	};
	
	// Misc
	private static final int[] KARTIA_SOLO_INSTANCES =
	{
		205, // Solo 85
		206, // Solo 90
		207, // Solo 95
	};
	
	/**
	 * Private constructor for the {@code KartiaHelperGuardian} class.<br>
	 * It initializes the required event listeners and creature IDs.
	 */
	private KartiaHelperGuardian()
	{
		addCreatureSeeId(KARTIA_GUARDIANS);
		setCreatureKillId(this::onCreatureKill, KARTIA_GUARDIANS);
		setCreatureAttackedId(this::onCreatureAttacked, KARTIA_GUARDIANS);
		setInstanceStatusChangeId(this::onInstanceStatusChange, KARTIA_SOLO_INSTANCES);
	}
	
	/**
	 * Handles periodic timer events for the NPC.<br>
	 * This method manages movement logic and skill usage based on specific event types.
	 * @param event The name of the timer event to process.
	 * @param params A set of statistics used as parameters for the event.
	 * @param npc The {@code Npc} instance that triggered the event.
	 * @param player The {@code Player} associated with the NPC action.
	 */
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		final Instance instance = npc.getInstanceWorld();
		if (instance == null)
		{
			return;
		}
		
		if (event.equals("CHECK_ACTION"))
		{
			final FriendlyNpc eliyah = npc.getVariables().getObject("ELIYAH_OBJECT", FriendlyNpc.class);
			if (eliyah != null)
			{
				final double distance = npc.calculateDistance2D(eliyah);
				if (distance > 300)
				{
					final Location loc = new Location(eliyah.getX(), eliyah.getY(), eliyah.getZ() + 50);
					final Location randLoc = new Location(loc.getX() + getRandom(-50, 50), loc.getY() + getRandom(-50, 50), loc.getZ());
					if (distance > 600)
					{
						npc.teleToLocation(loc);
					}
					else
					{
						npc.setRunning();
					}
					
					addMoveToDesire(npc, randLoc, 23);
				}
				else if (!npc.isInCombat() || (npc.getTarget() == null))
				{
					final WorldObject target = eliyah.getTarget();
					if ((target != null) && target.isMonster())
					{
						final Monster monster = target.asMonster();
						if ((monster != null) && !ArrayUtil.contains(KARTIA_FRIENDS, monster.getId()))
						{
							addAttackDesire(npc, monster);
						}
					}
				}
			}
		}
		else if (event.equals("USE_SKILL"))
		{
			if ((npc.isInCombat() || npc.isAttackingNow() || (npc.getTarget() != null)) && (npc.getCurrentMpPercent() > 25) && !ArrayUtil.contains(KARTIA_FRIENDS, npc.getTargetId()))
			{
				useRandomSkill(npc);
			}
		}
	}
	
	/**
	 * Handles changes to the status of an instance.<br>
	 * This method checks if the instance is active and starts timers for guardians.
	 * @param event The {@code OnInstanceStatusChange} event containing the current world and status.
	 */
	public void onInstanceStatusChange(OnInstanceStatusChange event)
	{
		final Instance instance = event.getWorld();
		final int status = event.getStatus();
		if (status == 1)
		{
			final List<Npc> aliveGuardians = instance.getAliveNpcs(KARTIA_GUARDIANS);
			aliveGuardians.forEach(guardian -> getTimers().addRepeatingTimer("CHECK_ACTION", 3000, guardian, null));
			aliveGuardians.forEach(guardian -> getTimers().addRepeatingTimer("USE_SKILL", 6000, guardian, null));
		}
	}
	
	/**
	 * This method is called when an {@code Npc} sees a {@code Creature}.<br>
	 * It identifies the type of creature and stores it in the NPC variables.<br>
	 * Players are stored as {@code PLAYER_OBJECT}.<br>
	 * Specific NPCs like Adolph or Eliyah are stored in their respective object variables.
	 * @param npc The {@code Npc} that sees the creature.
	 * @param creature The {@code Creature} that was seen.
	 */
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayer())
		{
			npc.getVariables().set("PLAYER_OBJECT", creature.asPlayer());
		}
		else
		{
			switch (creature.getId())
			{
				case 33609: // Adolph (Kartia 85)
				case 33620: // Adolph (Kartia 90)
				case 33631: // Adolph (Kartia 95)
				{
					npc.getVariables().set("ADOLPH_OBJECT", creature);
					break;
				}
				case 33615: // Eliyah (Kartia 85)
				case 33626: // Eliyah (Kartia 90)
				case 33637: // Eliyah (Kartia 95)
				{
					npc.getVariables().set("ELIYAH_OBJECT", creature);
					break;
				}
			}
		}
	}
	
	/**
	 * Causes the {@code Npc} to cast a random skill on its current target.<br>
	 * This method checks if the target is valid and not a friend before casting.<br>
	 * It selects between two specific skills based on instance parameters.
	 * @param npc The {@code Npc} that will perform the action.
	 */
	public void useRandomSkill(Npc npc)
	{
		final WorldObject target = npc.getTarget();
		if (target == null)
		{
			return;
		}
		
		final Instance instance = npc.getInstanceWorld();
		if ((instance != null) && !npc.isCastingNow() && (!ArrayUtil.contains(KARTIA_FRIENDS, target.getId())))
		{
			final StatSet instParams = instance.getTemplateParameters();
			final SkillHolder skill01 = instParams.getSkillHolder("guardianSpiritsBlow");
			final SkillHolder skill02 = instParams.getSkillHolder("guardianSpiritsWrath");
			final int numberOfActiveSkills = 2;
			final int randomSkill = getRandom(numberOfActiveSkills + 1);
			
			switch (randomSkill)
			{
				case 0:
				case 1:
				{
					if ((skill01 != null) && SkillCaster.checkUseConditions(npc, skill01.getSkill()))
					{
						npc.doCast(skill01.getSkill(), null, true, false);
					}
					break;
				}
				case 2:
				{
					if ((skill02 != null) && SkillCaster.checkUseConditions(npc, skill02.getSkill()))
					{
						npc.doCast(skill02.getSkill(), null, true, false);
					}
					break;
				}
			}
		}
	}
	
	/**
	 * Handles the logic when a creature is attacked.<br>
	 * This method checks if an NPC should target an attacker based on specific conditions.<br>
	 * It ensures the NPC is not already in combat and the attacker is not a friend.
	 * @param event The {@code OnCreatureAttacked} event containing details about the attack.
	 */
	public void onCreatureAttacked(OnCreatureAttacked event)
	{
		final Npc npc = event.getTarget().asNpc();
		if (npc != null)
		{
			final Instance instance = npc.getInstanceWorld();
			if ((instance != null) && !npc.isInCombat() && !event.getAttacker().isPlayable() && !ArrayUtil.contains(KARTIA_FRIENDS, event.getAttacker().getId()))
			{
				npc.setTarget(event.getAttacker());
				addAttackDesire(npc, npc.getTarget().asCreature());
			}
		}
	}
	
	/**
	 * Handles the logic when a creature dies.<br>
	 * This method cancels active timers for the target {@code Npc}.<br>
	 * It also triggers the death animation for the attacker.
	 * @param event The {@code OnCreatureDeath} event containing the target and attacker.
	 */
	public void onCreatureKill(OnCreatureDeath event)
	{
		final Npc npc = event.getTarget().asNpc();
		final Instance world = npc.getInstanceWorld();
		if (world != null)
		{
			getTimers().cancelTimersOf(npc);
			npc.doDie(event.getAttacker());
		}
	}
	
	/**
	 * This is the main entry point for the {@code KartiaHelperGuardian} script.<br>
	 * It initializes a new instance of the class to start the logic.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new KartiaHelperGuardian();
	}
}
