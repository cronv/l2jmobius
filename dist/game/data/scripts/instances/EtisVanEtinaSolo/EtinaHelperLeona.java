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
package instances.EtisVanEtinaSolo;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.FriendlyNpc;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttacked;
import org.l2jmobius.gameserver.mechanics.events.holders.instance.OnInstanceStatusChange;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class defines the artificial intelligence for the {@code EtinaHelperLeona} NPC.<br>
 * It handles the behavior of Leona as a helper during the solo instance.
 * @author CostyKiller
 */
public class EtinaHelperLeona extends Script
{
	// NPCs
	private static final int ETINA_HELPER_LEONA = 34471;
	private static final int[] NOT_ATK_NPCS =
	{
		33798, // Common Decoration - Barricade
		19677, // Seal of Gnosis
		19678, // Seal of Strife
		19679, // Seal of Avarice
		19680, // Seal of Punishment
		19681, // Seal of Awakening
		19682, // Seal of Calamity
		19683, // Seal of Destruction
	};
	private static final int[] ETINA_HELPERS =
	{
		34471, // Leona Blackbird
		34472, // Devianne
		34473, // Elikia
		34474, // Sporcha
		34475, // Aliber
	};
	
	// RB
	private static final int ETIS_VAN_ETINA1 = 26321;
	private static final int ETIS_VAN_ETINA2 = 26322;
	private static final int KAIN_VAN_HALTER = 26235;
	private static final int CAMILLE = 26236;
	
	// Misc
	private static final int[] ETINA_SOLO_INSTANCES =
	{
		292, // Fall of Etina (Solo)
		293, // Fall of Etina (Solo)
	};
	
	/**
	 * Initializes the {@code EtinaHelperLeona} script.<br>
	 * Registers necessary event listeners for the NPC.<br>
	 * Sets up IDs for spells, vision, and attacks.
	 */
	private EtinaHelperLeona()
	{
		addSpellFinishedId(ETINA_HELPER_LEONA);
		addCreatureSeeId(ETINA_HELPER_LEONA);
		setCreatureAttackedId(this::onCreatureAttacked, ETINA_HELPER_LEONA);
		setInstanceStatusChangeId(this::onInstanceStatusChange, ETINA_SOLO_INSTANCES);
	}
	
	/**
	 * Handles periodic timer events for the NPC.<br>
	 * This method manages movement and combat logic based on the {@code CHECK_ACTION} event.<br>
	 * It updates the NPC's target and position relative to the player.
	 * @param event The name of the timer event being triggered.
	 * @param params A set of statistics or variables associated with the event.
	 * @param npc The {@code Npc} instance that is executing the script.
	 * @param player The {@code Player} object involved in the interaction.
	 */
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		final Instance instance = npc.getInstanceWorld();
		if ((instance != null) && event.equals("CHECK_ACTION"))
		{
			final StatSet npcVars = npc.getVariables();
			final Player plr = npcVars.getObject("PLAYER_OBJECT", Player.class);
			if (plr != null)
			{
				final double distance = npc.calculateDistance2D(plr);
				if (!npc.isAttackingNow() && !npc.isMoving() && (distance > 250))
				{
					final Location loc = new Location(plr.getX(), plr.getY(), plr.getZ() + 50);
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
					((FriendlyNpc) npc).setCanReturnToSpawnPoint(false);
				}
				else if (!npc.isInCombat() || !npc.isAttackingNow() || (npc.getTarget() == null))
				{
					WorldObject target = npc.getTarget();
					if (target == null)
					{
						npc.setTarget(World.getRandomVisibleObjectInRange(npc, Monster.class, 2500));
					}
					
					if ((target != null) && !target.isInvul() && target.isTargetable() && GeoEngine.getInstance().canSeeTarget(npc, target) && !ArrayUtil.contains(NOT_ATK_NPCS, target.getId()) && !ArrayUtil.contains(ETINA_HELPERS, target.getId()))
					{
						npc.setInvul(true);
						npc.setRunning();
						((FriendlyNpc) npc).setCanReturnToSpawnPoint(false);
						addAttackDesire(npc, target.asCreature());
						// npc.reduceCurrentHp(1, monster, null);
					}
				}
			}
		}
	}
	
	/**
	 * This method handles logic when a creature is attacked.<br>
	 * It checks if the target is a {@code FriendlyNpc} and not currently in combat.<br>
	 * It triggers specific dialogue based on the ID of the attacking NPC.
	 * @param event The {@code OnCreatureAttacked} event containing attacker and target data.
	 */
	public void onCreatureAttacked(OnCreatureAttacked event)
	{
		final FriendlyNpc npc = (FriendlyNpc) event.getTarget();
		final Npc attacker = event.getAttacker().asNpc();
		if ((npc != null) && !npc.isInCombat())
		{
			final Instance instance = npc.getInstanceWorld();
			if ((instance != null) && !event.getAttacker().isPlayable() && !ArrayUtil.contains(ETINA_HELPERS, event.getAttacker().getId()))
			{
				switch (attacker.getId())
				{
					case CAMILLE:
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.GISELLE_NO_IS_SHE_CAMILLE_IN_THAT_STATE);
						break;
					}
					case KAIN_VAN_HALTER:
					{
						break;
					}
					case ETIS_VAN_ETINA1:
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.EVEN_THE_MIGHTY_ETINA_SEEMS_TO_FEAR_US);
						break;
					}
					case ETIS_VAN_ETINA2:
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.ETIS_VAN_ETINA_WE_FINALLY_MEET);
						break;
					}
					default:
					{
						if (getRandom(50) < 5)
						{
							npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.DON_T_GET_IN_MY_WAY);
						}
						break;
					}
				}
			}
		}
	}
	
	/**
	 * Handles changes to the instance status.<br>
	 * This method checks if the instance is in a specific state.<br>
	 * It adds a repeating timer for {@code ETINA_HELPER_LEONA} NPCs if the condition is met.
	 * @param event The {@code OnInstanceStatusChange} event containing the current status and world data.
	 */
	public void onInstanceStatusChange(OnInstanceStatusChange event)
	{
		final int status = event.getStatus();
		if ((status == 1) || (status == 2) || (status == 3))
		{
			final Instance instance = event.getWorld();
			instance.getAliveNpcs(ETINA_HELPER_LEONA).forEach(etinaHelperLeona -> getTimers().addRepeatingTimer("CHECK_ACTION", 3000, etinaHelperLeona, null));
			// instance.getAliveNpcs(ETINA_HELPER_LEONA).forEach(etinaHelperLeona -> getTimers().addRepeatingTimer("USE_SKILL", 6000, etinaHelperLeona, null));
		}
	}
	
	/**
	 * This method is called when an {@code Npc} sees a {@code Creature}.<br>
	 * It checks if the seen entity is a {@code Player}.<br>
	 * If it is a player, it saves the player object into the NPC variables.
	 * @param npc The {@code Npc} that sees the creature.
	 * @param creature The {@code Creature} that was spotted.
	 */
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayer())
		{
			npc.getVariables().set("PLAYER_OBJECT", creature.asPlayer());
		}
	}
	
	/**
	 * This is the entry point for the {@code EtinaHelperLeona} class.<br>
	 * It initializes a new instance of the script.
	 * @param args The command line arguments.
	 */
	public static void main(String[] args)
	{
		new EtinaHelperLeona();
	}
}
