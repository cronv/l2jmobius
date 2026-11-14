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
package instances.PailakaRuneCastle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.FriendlyNpc;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.instancezone.InstanceTemplate;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * This class handles the logic for the {@code PailakaRuneCastle} instance.<br>
 * It manages the specific mechanics and scripts required for this zone.
 * @author Mobius
 */
public class PailakaRuneCastle extends InstanceScript
{
	// NPCs
	private static final int[] VICTIMS =
	{
		36562,
		36563,
		36564,
		36565,
		36566,
		36567,
		36568,
		36569,
	};
	private static final Map<Integer, Integer> MANAGERS = new HashMap<>();
	static
	{
		MANAGERS.put(36403, 80); // Gludio
		MANAGERS.put(36404, 81); // Dion
		MANAGERS.put(36405, 82); // Giran
		MANAGERS.put(36406, 83); // Oren
		MANAGERS.put(36407, 84); // Aden
		MANAGERS.put(36408, 85); // Innadril
		MANAGERS.put(36409, 86); // Goddard
		MANAGERS.put(36410, 87); // Rune
		MANAGERS.put(36411, 88); // Schuttgart
		MANAGERS.put(35666, 89); // Shanty
		MANAGERS.put(35698, 90); // Southern
		MANAGERS.put(35735, 91); // Hive
		MANAGERS.put(35767, 92); // Valley
		MANAGERS.put(35804, 93); // Ivory
		MANAGERS.put(35835, 94); // Narsell
		MANAGERS.put(35867, 95); // Bayou
		MANAGERS.put(35904, 96); // White Sands
		MANAGERS.put(35936, 97); // Borderland
		MANAGERS.put(35974, 98); // Swamp
		MANAGERS.put(36011, 99); // Archaic
		MANAGERS.put(36043, 100); // Floran
		MANAGERS.put(36081, 101); // Cloud Mountain
		MANAGERS.put(36118, 102); // Tanor
		MANAGERS.put(36149, 103); // Dragonspine
		MANAGERS.put(36181, 104); // Antharas
		MANAGERS.put(36219, 105); // Western
		MANAGERS.put(36257, 106); // Hunter
		MANAGERS.put(36294, 107); // Aaru
		MANAGERS.put(36326, 108); // Demon
		MANAGERS.put(36364, 109); // Monastic
	}
	
	// Misc
	private static final long REENTER = 24 * 3600000; // 24 hours
	private static final Map<Integer, Long> REENETER_HOLDER = new ConcurrentHashMap<>();
	
	/**
	 * Initializes the {@code PailakaRuneCastle} instance script.<br>
	 * This constructor sets up the required NPCs for interaction.<br>
	 * It registers IDs for first talk, general talk, and starting NPCs.
	 */
	public PailakaRuneCastle()
	{
		super(MANAGERS.values().stream().mapToInt(Integer::valueOf).toArray());
		addFirstTalkId(VICTIMS);
		addTalkId(VICTIMS);
		final Set<Integer> managerKeys = MANAGERS.keySet();
		addTalkId(managerKeys);
		addStartNpc(managerKeys);
		addInstanceCreatedId(MANAGERS.values());
	}
	
	/**
	 * Handles specific script events for the Pailaka Rune Castle instance.<br>
	 * It manages wave progression, monster spawning, and player ejection.
	 * @param event The name of the event to process.
	 * @param npc The {@code Npc} object associated with the event.
	 * @param player The {@code Player} object involved in the event.
	 * @return A {@code String} value or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "WAVE_DEFEATED_CHECK":
			{
				final Instance world = player.getInstanceWorld();
				if (world == null)
				{
					return null;
				}
				
				if (world.getAliveNpcs(Monster.class).isEmpty())
				{
					switch (world.getStatus())
					{
						case 0:
						case 1:
						case 2:
						{
							startQuestTimer("SPAWN_NEXT_WAVE", 20000, null, player, false);
							break;
						}
						case 3:
						{
							// for (Player member : world.getPlayers())
							// {
							// final QuestState qs = member.getQuestState(world.getTemplateId() < 89 ? Q00727_HopeWithinTheDarkness.class.getSimpleName() : Q00726_LightWithinTheDarkness.class.getSimpleName());
							// if ((qs != null) && qs.isCond(1))
							// {
							// qs.setCond(2);
							// }
							// }
							
							world.finishInstance();
							break;
						}
					}
					
					world.incStatus();
					return null;
				}
				
				startQuestTimer("WAVE_DEFEATED_CHECK", 5000, null, player, false);
				break;
			}
			case "SPAWN_NEXT_WAVE":
			{
				final Instance world = player.getInstanceWorld();
				if (world == null)
				{
					return null;
				}
				
				List<Npc> monsters = new ArrayList<>();
				switch (world.getStatus())
				{
					case 0:
					{
						showOnScreenMsg(world, NpcStringId.LV_1_2, ExShowScreenMessage.TOP_CENTER, 5000, true);
						monsters = world.spawnGroup("monsters_first_wave");
						break;
					}
					case 1:
					{
						showOnScreenMsg(world, NpcStringId.LV_2_2, ExShowScreenMessage.TOP_CENTER, 5000, true);
						monsters = world.spawnGroup("monsters_second_wave");
						break;
					}
					case 2:
					{
						showOnScreenMsg(world, NpcStringId.LV_3_2, ExShowScreenMessage.TOP_CENTER, 5000, true);
						monsters = world.spawnGroup("monsters_third_wave");
						break;
					}
				}
				
				final List<FriendlyNpc> helpers = world.getAliveNpcs(FriendlyNpc.class);
				if (!helpers.isEmpty())
				{
					for (Npc monster : monsters)
					{
						final Npc helper = helpers.get(getRandom(helpers.size()));
						helper.reduceCurrentHp(1, monster, null); // TODO: Find better way for attack
						addAttackDesire(helper, monster);
						addAttackDesire(monster, helper);
						helper.setRunning();
						monster.reduceCurrentHp(1, helper, null); // TODO: Find better way for attack
						monster.setRandomWalking(false);
						monster.setRunning();
						addMoveToDesire(monster, helper.getLocation(), 10);
					}
				}
				
				cancelQuestTimer("FORCE_NEXT_WAVE", null, player);
				startQuestTimer("FORCE_NEXT_WAVE", 480000, null, player, false); // 8 minutes
				startQuestTimer("WAVE_DEFEATED_CHECK", 1000, null, player, false);
				break;
			}
			case "FORCE_NEXT_WAVE":
			{
				final Instance world = player.getInstanceWorld();
				if (world == null)
				{
					return null;
				}
				
				if (world.getStatus() < 3)
				{
					cancelQuestTimer("WAVE_DEFEATED_CHECK", null, player);
					world.incStatus();
					startQuestTimer("SPAWN_NEXT_WAVE", 1000, null, player, false);
				}
				break;
			}
			case "exit":
			{
				final Instance world = npc.getInstanceWorld();
				if (world == null)
				{
					return null;
				}
				
				world.ejectPlayer(player);
				break;
			}
		}
		
		return null;
	}
	
	/**
	 * Handles the dialogue for a player's first interaction with an {@code Npc}.<br>
	 * It determines which HTML file to display based on the world status and NPC state.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} initiating the conversation.
	 * @return A string representing the name of the HTML file to show, or {@code null}.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance world = npc.getInstanceWorld();
		if (world == null)
		{
			return null;
		}
		
		if (npc.isDead())
		{
			return "victim-02.html";
		}
		
		if (world.getStatus() < 3)
		{
			return "victim-01.html";
		}
		
		return "victim-03.html";
	}
	
	/**
	 * Handles the logic when a player talks to an {@code Npc}.<br>
	 * It checks if the {@code Npc} is a manager and enters the instance if it is.
	 * @param npc The {@code Npc} that was interacted with.
	 * @param player The {@code Player} who initiated the talk.
	 * @return Returns {@code null} after processing the logic.
	 */
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final int npcId = npc.getId();
		final Integer template = MANAGERS.get(npcId);
		if (template != null)
		{
			enterInstance(player, npc, template);
		}
		
		return null;
	}
	
	/**
	 * This method is called when a new instance is created.<br>
	 * It sets the re-entry time for the instance.<br>
	 * It also starts a timer to spawn the first wave of enemies.
	 * @param instance The {@code Instance} that was just created.
	 * @param player The {@code Player} who entered the instance.
	 */
	@Override
	public void onInstanceCreated(Instance instance, Player player)
	{
		// Put re-enter for instance
		REENETER_HOLDER.put(instance.getTemplateId(), System.currentTimeMillis() + REENTER);
		
		// Schedule spawn of first wave
		startQuestTimer("SPAWN_NEXT_WAVE", 120000, null, player, false); // 2 minutes
	}
	
	/**
	 * Checks if the group meets the requirements to enter a castle or fort.<br>
	 * It verifies pledge status and entry restrictions based on the instance type.
	 * @param group The list of players attempting to enter.
	 * @param npc The NPC being interacted with.
	 * @param template The template of the instance being accessed.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	protected boolean validateConditions(List<Player> group, Npc npc, InstanceTemplate template)
	{
		final Player groupLeader = group.get(0);
		final int templateId = template.getId();
		if (templateId < 89) // Castle
		{
			final Castle castle = npc.getCastle();
			if (castle == null)
			{
				showHtmlFile(groupLeader, "noProperPledge.html");
				return false;
			}
			else if (REENETER_HOLDER.containsKey(templateId))
			{
				final long time = REENETER_HOLDER.get(templateId);
				if (time > System.currentTimeMillis())
				{
					showHtmlFile(groupLeader, "enterRestricted.html");
					return false;
				}
				
				REENETER_HOLDER.remove(templateId);
			}
		}
		else // Fort
		{
			final Fort fort = npc.getFort();
			if (fort == null)
			{
				showHtmlFile(groupLeader, "noProperPledge.html");
				return false;
			}
			else if (fort.getFortState() == 0)
			{
				showHtmlFile(groupLeader, "noContractYet.html");
				return false;
			}
			else if (fort.getFortState() == 2)
			{
				showHtmlFile(groupLeader, "noCastleContract.html");
				return false;
			}
			else if (REENETER_HOLDER.containsKey(templateId))
			{
				final long time = REENETER_HOLDER.get(templateId);
				if (time > System.currentTimeMillis())
				{
					showHtmlFile(groupLeader, "enterRestricted.html");
					return false;
				}
				
				REENETER_HOLDER.remove(templateId);
			}
		}
		
		return true;
	}
	
	/**
	 * This is the entry point for the {@code PailakaRuneCastle} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new PailakaRuneCastle();
	}
}
