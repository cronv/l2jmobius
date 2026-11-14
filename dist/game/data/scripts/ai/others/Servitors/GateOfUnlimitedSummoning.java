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
package ai.others.Servitors;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.script.timers.TimerExecutor;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the artificial intelligence for the {@code GateOfUnlimitedSummoning} entity.<br>
 * It defines the behaviors and actions for this specific Death Gate NPC.
 * @author Sdw, Mobius
 */
public class GateOfUnlimitedSummoning extends Script
{
	// NPCs
	private static final Map<Integer, Integer> DEATH_GATE = new HashMap<>();
	static
	{
		DEATH_GATE.put(14927, 1); // Death Gate
		DEATH_GATE.put(15200, 2); // Death Gate
		DEATH_GATE.put(15201, 3); // Death Gate
		DEATH_GATE.put(15202, 4); // Death Gate
		DEATH_GATE.put(15218, 5); // Death Gate
		DEATH_GATE.put(15219, 6); // Death Gate
		DEATH_GATE.put(15264, 7); // Death Gate
		DEATH_GATE.put(15265, 8); // Death Gate
	}
	
	// Skills
	private static final int GATE_ROOT = 11289;
	private static final int GATE_VORTEX = 11291;
	
	/**
	 * Private constructor for the {@code GateOfUnlimitedSummoning} class.<br>
	 * It initializes the script by registering spawn IDs.<br>
	 * This method is not intended to be called from outside this class.
	 */
	private GateOfUnlimitedSummoning()
	{
		addSpawnId(DEATH_GATE.keySet());
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It checks if the {@code Npc} has a player as its summoner.<br>
	 * If a player is found, it initializes several timers for the {@code Npc}.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		final Creature summoner = npc.getSummoner();
		if ((summoner != null) && summoner.isPlayer())
		{
			final TimerExecutor<String> timers = getTimers();
			timers.addTimer("SKILL_CAST_SLOW", 1000, npc, null);
			timers.addTimer("SKILL_CAST_DAMAGE", 2000, npc, null);
			timers.addTimer("END_OF_LIFE", 30000, npc, null);
		}
	}
	
	/**
	 * Handles specific timed events for the Death Gate NPC.<br>
	 * This method executes actions based on different event types.<br>
	 * It manages skill casting and NPC lifecycle logic.
	 * @param event The name of the timer event to process.
	 * @param params Additional parameters associated with the event.
	 * @param npc The {@code Npc} instance triggering the event.
	 * @param player The {@code Player} instance involved in the interaction.
	 */
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		switch (event)
		{
			case "SKILL_CAST_SLOW":
			{
				final int skillLevel = DEATH_GATE.get(npc.getId());
				if (skillLevel > 0)
				{
					final Skill skill = SkillData.getInstance().getSkill(GATE_ROOT, skillLevel);
					if (skill != null)
					{
						npc.doCast(skill);
					}
				}
				
				getTimers().addTimer("SKILL_CAST_SLOW", 3000, npc, null);
				break;
			}
			case "SKILL_CAST_DAMAGE":
			{
				final Skill skill = SkillData.getInstance().getSkill(GATE_VORTEX, 1);
				if (skill != null)
				{
					npc.doCast(skill);
				}
				
				getTimers().addTimer("SKILL_CAST_DAMAGE", 2000, npc, null);
				break;
			}
			case "END_OF_LIFE":
			{
				final TimerExecutor<String> timers = getTimers();
				timers.cancelTimer("SKILL_CAST_SLOW", npc, null);
				timers.cancelTimer("SKILL_CAST_DAMAGE", npc, null);
				npc.deleteMe();
				break;
			}
		}
	}
	
	/**
	 * This is the entry point for the application.<br>
	 * It initializes a new instance of {@code GateOfUnlimitedSummoning}.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new GateOfUnlimitedSummoning();
	}
}
