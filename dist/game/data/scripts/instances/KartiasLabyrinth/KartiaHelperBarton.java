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
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class defines the artificial intelligence for the helper NPC named Barton.<br>
 * It handles the behavior of a Tyrr Warrior within the {@code KartiasLabyrinth} instance.
 * @author flanagak
 */
public class KartiaHelperBarton extends Script
{
	// NPCs
	private static final int[] KARTIA_BARTON =
	{
		33611, // Barton (Kartia 85)
		33622, // Barton (Kartia 90)
		33633, // Barton (Kartia 95)
	};
	private static final int[] KARTIA_ADOLPH =
	{
		33609, // Adolph (Kartia 85)
		33620, // Adolph (Kartia 90)
		33631, // Adolph (Kartia 95)
	};
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
	
	// Misc
	private static final int[] KARTIA_SOLO_INSTANCES =
	{
		205, // Solo 85
		206, // Solo 90
		207, // Solo 95
	};
	
	/**
	 * Private constructor for the {@code KartiaHelperBarton} class.<br>
	 * It initializes the required event listeners and IDs.
	 */
	private KartiaHelperBarton()
	{
		addCreatureSeeId(KARTIA_BARTON);
		setCreatureKillId(this::onCreatureKill, KARTIA_BARTON);
		setCreatureAttackedId(this::onCreatureAttacked, KARTIA_BARTON);
		setInstanceStatusChangeId(this::onInstanceStatusChange, KARTIA_SOLO_INSTANCES);
	}
	
	/**
	 * Handles periodic timer events for the NPC.<br>
	 * This method manages movement logic and skill usage based on specific event types.<br>
	 * It checks for actions like following a target or using skills during combat.
	 * @param event The name of the timer event to process.
	 * @param params The set of statistics associated with the event.
	 * @param npc The {@code Npc} instance that triggered the event.
	 * @param player The {@code Player} instance involved in the event.
	 */
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		final Instance instance = npc.getInstanceWorld();
		if ((instance != null) && event.equals("CHECK_ACTION"))
		{
			final FriendlyNpc adolph = npc.getVariables().getObject("ADOLPH_OBJECT", FriendlyNpc.class);
			if (adolph != null)
			{
				final double distance = npc.calculateDistance2D(adolph);
				if (distance > 300)
				{
					final Location loc = new Location(adolph.getX(), adolph.getY(), adolph.getZ() + 50);
					final Location randLoc = new Location(loc.getX() + getRandom(-100, 100), loc.getY() + getRandom(-100, 100), loc.getZ());
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
					final WorldObject target = adolph.getTarget();
					if ((target != null) && target.isMonster())
					{
						final Monster monster = target.asMonster();
						if ((monster != null) && adolph.isInCombat() && !ArrayUtil.contains(KARTIA_FRIENDS, monster.getId()))
						{
							addAttackDesire(npc, monster);
						}
					}
				}
			}
		}
		else if ((instance != null) && event.equals("USE_SKILL"))
		{
			if (npc.isInCombat() || npc.isAttackingNow() || (npc.getTarget() != null))
			{
				if ((npc.getCurrentMpPercent() > 25) && !ArrayUtil.contains(KARTIA_FRIENDS, npc.getTargetId()))
				{
					useRandomSkill(npc);
				}
			}
		}
	}
	
	/**
	 * Handles changes to the status of an instance.<br>
	 * This method checks if the instance is active and starts timers for specific NPCs.
	 * @param event The {@code OnInstanceStatusChange} event containing the current world and status.
	 */
	public void onInstanceStatusChange(OnInstanceStatusChange event)
	{
		final Instance instance = event.getWorld();
		final int status = event.getStatus();
		if (status == 1)
		{
			final List<Npc> aliveBartons = instance.getAliveNpcs(KARTIA_BARTON);
			aliveBartons.forEach(barton -> getTimers().addRepeatingTimer("CHECK_ACTION", 3000, barton, null));
			aliveBartons.forEach(barton -> getTimers().addRepeatingTimer("USE_SKILL", 6000, barton, null));
		}
	}
	
	/**
	 * This method is called when an {@code Npc} sees a {@code Creature}.<br>
	 * It updates the internal variables of the {@code Npc} based on the type of creature seen.<br>
	 * If the creature is a {@code Player}, it stores the player object in a variable.<br>
	 * If the creature is part of the {@code KARTIA_ADOLPH} group, it stores that creature instead.
	 * @param npc The {@code Npc} that sees the creature.
	 * @param creature The {@code Creature} that was seen by the {@code Npc}.
	 */
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayer())
		{
			npc.getVariables().set("PLAYER_OBJECT", creature.asPlayer());
		}
		else if (ArrayUtil.contains(KARTIA_ADOLPH, creature.getId()))
		{
			npc.getVariables().set("ADOLPH_OBJECT", creature);
		}
	}
	
	/**
	 * Selects and executes a random skill for the specified {@code Npc}.<br>
	 * This method checks if the {@code Npc} has a valid target and is not currently casting.<br>
	 * It picks one of five specific skills from the instance parameters to cast.
	 * @param npc The {@code Npc} that will perform the skill action.
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
			final SkillHolder skill1 = instParams.getSkillHolder("bartonInfinity");
			final SkillHolder skill2 = instParams.getSkillHolder("bartonBerserker");
			final SkillHolder skill3 = instParams.getSkillHolder("bartonHurricane");
			final SkillHolder skill4 = instParams.getSkillHolder("bartonPowerBomber");
			final SkillHolder skill5 = instParams.getSkillHolder("bartonSonicStar");
			final int numberOfActiveSkills = 5;
			final int randomSkill = getRandom(numberOfActiveSkills + 1);
			
			switch (randomSkill)
			{
				case 0:
				case 1:
				{
					if ((skill1 != null) && SkillCaster.checkUseConditions(npc, skill1.getSkill()))
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.I_WILL_SHOW_YOU_THE_JUSTICE_OF_ADEN);
						npc.doCast(skill1.getSkill(), null, true, false);
					}
					break;
				}
				case 2:
				{
					if ((skill2 != null) && SkillCaster.checkUseConditions(npc, skill2.getSkill()))
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THOSE_WHO_ARE_IN_FRONT_OF_MY_EYES_WILL_BE_DESTROYED_2);
						npc.doCast(skill2.getSkill(), null, true, false);
					}
					break;
				}
				case 3:
				{
					if ((skill3 != null) && SkillCaster.checkUseConditions(npc, skill3.getSkill()))
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.FOR_THE_GODDESS);
						npc.doCast(skill3.getSkill(), null, true, false);
					}
					break;
				}
				case 4:
				{
					if ((skill4 != null) && SkillCaster.checkUseConditions(npc, skill4.getSkill()))
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.YOU_WILL_BE_DESTROYED);
						npc.doCast(skill4.getSkill(), null, true, false);
					}
					break;
				}
				case 5:
				{
					if ((skill5 != null) && SkillCaster.checkUseConditions(npc, skill5.getSkill()))
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.YOU_WILL_DIE);
						npc.doCast(skill5.getSkill(), null, true, false);
					}
					break;
				}
			}
		}
	}
	
	/**
	 * Handles logic when a creature is attacked.<br>
	 * This method checks if the target is an {@code Npc}.<br>
	 * It sets the target and adds attack desire if specific conditions are met.
	 * @param event The {@code OnCreatureAttacked} event containing attacker and target data.
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
	 * This method cancels active timers for the {@code Npc}.<br>
	 * It then triggers the death animation for the attacker.
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
	 * This is the entry point for the {@code KartiaHelperBarton} class.<br>
	 * It initializes a new instance of the script.
	 * @param args The command line arguments.
	 */
	public static void main(String[] args)
	{
		new KartiaHelperBarton();
	}
}
