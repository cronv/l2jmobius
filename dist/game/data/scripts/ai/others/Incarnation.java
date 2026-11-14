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
package ai.others;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.Id;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttack;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSkillFinishCast;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcSpawn;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class handles the logic for the {@code Incarnation} script.<br>
 * It manages specific behaviors and events for entities using this script.
 * @author Nik
 */
public class Incarnation extends Script
{
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It checks if the NPC has a summoner.<br>
	 * If a summoner exists, it adds attack and skill listeners to them.
	 * @param event The {@code OnNpcSpawn} event containing the spawned NPC.
	 */
	@RegisterEvent(EventType.ON_NPC_SPAWN)
	@RegisterType(ListenerRegisterType.NPC)
	@Id(13302)
	@Id(13303)
	@Id(13304)
	@Id(13305)
	@Id(13455)
	@Id(13456)
	@Id(13457)
	@Id(13578)
	@Id(13579)
	public void onNpcSpawn(OnNpcSpawn event)
	{
		final Npc npc = event.getNpc();
		if (npc.getSummoner() != null)
		{
			final Creature summoner = npc.getSummoner();
			summoner.addListener(new ConsumerEventListener(npc, EventType.ON_CREATURE_ATTACK, (OnCreatureAttack e) -> onOffense(npc, e.getAttacker(), e.getTarget()), this));
			summoner.addListener(new ConsumerEventListener(npc, EventType.ON_CREATURE_SKILL_FINISH_CAST, (OnCreatureSkillFinishCast e) -> onOffense(npc, e.getCaster(), e.getTarget()), this));
		}
	}
	
	/**
	 * This method handles the behavior when an {@code Npc} is attacked.<br>
	 * It checks if the attacker is the target and ensures the {@code Npc} has a summoner.<br>
	 * If valid, it forces the {@code Npc} to attack the specified {@code WorldObject}.
	 * @param npc The {@code Npc} entity that is reacting to the offense.
	 * @param attacker The {@code Creature} that initiated the attack.
	 * @param target The {@code WorldObject} being targeted by the attack.
	 */
	public void onOffense(Npc npc, Creature attacker, WorldObject target)
	{
		if ((attacker == target) || (npc.getSummoner() == null))
		{
			return;
		}
		
		// Attack target of summoner.
		npc.setRunning();
		npc.getAI().setIntentionAttack(target);
	}
	
	/**
	 * This is the main entry point for the {@code Incarnation} script.<br>
	 * It initializes a new instance of the {@code Incarnation} class.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Incarnation();
	}
}
