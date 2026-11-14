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
package custom.listeners;

import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.Id;
import org.l2jmobius.gameserver.mechanics.events.annotations.NpcLevelRange;
import org.l2jmobius.gameserver.mechanics.events.annotations.Priority;
import org.l2jmobius.gameserver.mechanics.events.annotations.Range;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnAttackableAttack;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerDlgAnswer;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemCreate;
import org.l2jmobius.gameserver.mechanics.events.holders.sieges.OnCastleSiegeStart;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.scripting.annotations.Disabled;

/**
 * This class serves as an example of how to use the {@code Listener} system.<br>
 * It demonstrates basic event handling within a {@code Script}.
 * @author UnAfraid
 */
@Disabled
public class ListenerTest extends Script
{
	private static final int[] ELPIES =
	{
		20432,
		22228
	};
	
	/**
	 * Initializes the listeners for {@code ListenerTest}.<br>
	 * Registers a preset listener for specific IDs.<br>
	 * Performs manual registration for player dialogue answers.
	 */
	private ListenerTest()
	{
		// This method registers a preset listener using a Consumer that takes one parameter and returns nothing.
		setAttackableAttackId(this::onAttackableAttack, ELPIES);
		
		// Manual listener registration
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_DLG_ANSWER, (OnPlayerDlgAnswer event) -> LOGGER.info(getClass().getSimpleName() + ": " + event.getPlayer() + " OnPlayerDlgAnswer: Answer: " + event.getAnswer() + " MessageId: " + event.getMessageId()), this));
	}
	
	/**
	 * Handles the logic when an attackable entity is attacked.<br>
	 * Logs details about the attacker, target, damage, and skill used.
	 * @param event The {@code OnAttackableAttack} event containing attack data.
	 */
	private void onAttackableAttack(OnAttackableAttack event)
	{
		LOGGER.info(getClass().getSimpleName() + ": " + event.getClass().getSimpleName() + " invoked attacker: " + event.getAttacker() + " target: " + event.getTarget() + " damage: " + event.getDamage() + " skill: " + event.getSkill());
	}
	
	/**
	 * This method will be invoked as soon as Attackable (Rabbits 20432 and 22228) are being killed by Player (a player)<br>
	 * This listener is registered into individual npcs container.
	 * @param event
	 */
	// Annotation listener registration
	/**
	 * Handles the logic when a creature dies.<br>
	 * Logs the attacker and the target to the console.
	 * @param event The {@code OnCreatureDeath} event containing kill details.
	 */
	@RegisterEvent(EventType.ON_CREATURE_DEATH)
	@RegisterType(ListenerRegisterType.NPC)
	@Id(20432)
	@Id(22228)
	private void onCreatureKill(OnCreatureDeath event)
	{
		LOGGER.info(getClass().getSimpleName() + ": " + event.getClass().getSimpleName() + " invoked attacker: " + event.getAttacker() + " target: " + event.getTarget());
	}
	
	/**
	 * Handles the logic when a castle siege begins.<br>
	 * Logs the name and residence ID of the starting castle to the console.
	 * @param event The {@code OnCastleSiegeStart} event containing siege details.
	 */
	@RegisterEvent(EventType.ON_CASTLE_SIEGE_START)
	@RegisterType(ListenerRegisterType.CASTLE)
	@Range(from = 1, to = 9)
	private void onSiegeStart(OnCastleSiegeStart event)
	{
		LOGGER.info(getClass().getSimpleName() + ": The siege of " + event.getSiege().getCastle().getName() + " (" + event.getSiege().getCastle().getResidenceId() + ") has started!");
	}
	
	/**
	 * Handles the logic when a new item is created.<br>
	 * Logs the details of the created item and the associated actor.
	 * @param event The {@code OnItemCreate} event containing item and character data.
	 */
	@RegisterEvent(EventType.ON_ITEM_CREATE)
	@RegisterType(ListenerRegisterType.ITEM)
	@Id(5575)
	private void onItemCreate(OnItemCreate event)
	{
		LOGGER.info(getClass().getSimpleName() + ": Item [" + event.getItem() + "] has been created actor: " + event.getActiveChar() + " reference: " + event.getReference());
	}
	
	/**
	 * Handles the logic for when a creature is killed.<br>
	 * This method gives a player a random item with a 30% chance.<br>
	 * It checks if the attacker is a playable character and the target is attackable.
	 * @param event The {@code OnCreatureDeath} event containing kill details.
	 */
	@RegisterEvent(EventType.ON_CREATURE_DEATH)
	@RegisterType(ListenerRegisterType.NPC)
	@NpcLevelRange(from = 1, to = 10)
	@Priority(100)
	private void OnCreatureKill(OnCreatureDeath event)
	{
		// 70% chance to drop
		if (getRandom(100) >= 70)
		{
			return;
		}
		
		// Make sure a player killed this monster.
		if ((event.getAttacker() != null) && event.getAttacker().isPlayable() && event.getTarget().isAttackable())
		{
			final Attackable monster = event.getTarget().asAttackable();
			monster.dropItem(event.getAttacker().asPlayer(), new ItemHolder(57, getRandom(100, 1000)));
		}
	}
	
	/**
	 * Handles the logic when a player logs into the game.<br>
	 * Logs a message to the console identifying the player who joined.
	 * @param event The {@code OnPlayerLogin} event containing player data.
	 */
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void OnPlayerLogin(OnPlayerLogin event)
	{
		LOGGER.info(getClass().getSimpleName() + ": Player: " + event.getPlayer() + " has logged in!");
	}
	
	/**
	 * Handles the logic when a player dies.<br>
	 * This method checks if the target is a GM and prevents their death if they are.
	 * @param event The {@code OnCreatureDeath} event containing the creature that died.
	 * @return A {@code TerminateReturn} object with all values set to {@code true} if the death was prevented, otherwise {@code null}.
	 */
	@RegisterEvent(EventType.ON_CREATURE_DEATH)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	@Priority(Integer.MAX_VALUE)
	private TerminateReturn onPlayerDeath(OnCreatureDeath event)
	{
		if (event.getTarget().isGM())
		{
			LOGGER.info(getClass().getSimpleName() + ": Player: " + event.getTarget() + " was prevented from dying!");
			return new TerminateReturn(true, true, true);
		}
		
		return null;
	}
	
	/**
	 * This is the entry point for the {@code ListenerTest} class.<br>
	 * It creates a new instance of {@code ListenerTest}.
	 * @param args The command line arguments.
	 */
	public static void main(String[] args)
	{
		new ListenerTest();
	}
}
