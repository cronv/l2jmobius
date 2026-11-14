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
package instances.LurinSecretWorkshop;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Handles the logic for the {@code LurinSecretWorkshop} instance.<br>
 * This script manages player interactions and events within this specific zone.
 * @author CostyKiller, Tanatos
 */
public class LurinSecretWorkshop extends InstanceScript
{
	// NPC
	private static final int HESED = 33780;
	private static final int GRIMNIR = 8310;
	
	// Bosses
	private static final int[] BOSSES =
	{
		8306, // Lurin Archlord
		8307, // Lurin Archlord
		8308, // Lurin Archlord
		8309, // Lurin Archlord
	};
	
	// Additional Monsters Spawn Chance
	private static final int ADDITIONAL_MONSTER_SPAWN_CHANCE = 7;
	
	// Additional Monsters
	private static final int[] ADDITIONAL_MONSTERS =
	{
		8302, // Golem's Copy
		8303, // Golem's Copy
		8304, // Golem's Copy
		8304, // Golem's Copy
	};
	
	// Monsters 105
	private static final int[] MONSTERS_105 =
	{
		8278, // Artisan
		8282, // Scavenger
		8286, // Dwarven Defender
		8290, // Dwarven Hunter
		8294, // Dwarven Mage
		8298, // Dwarven Berserker
	};
	
	// Monsters 110
	private static final int[] MONSTERS_110 =
	{
		8279, // Artisan
		8283, // Scavenger
		8287, // Dwarven Defender
		8291, // Dwarven Hunter
		8295, // Dwarven Mage
		8299, // Dwarven Berserker
	};
	
	// Monsters 115
	private static final int[] MONSTERS_115 =
	{
		8280, // Artisan
		8284, // Scavenger
		8288, // Dwarven Defender
		8292, // Dwarven Hunter
		8296, // Dwarven Mage
		8300, // Dwarven Berserker
	};
	
	// Monsters 120
	private static final int[] MONSTERS_120 =
	{
		8281, // Artisan
		8285, // Scavenger
		8289, // Dwarven Defender
		8293, // Dwarven Hunter
		8297, // Dwarven Mage
		8301, // Dwarven Berserker
	};
	
	// Misc
	private static final int TEMPLATE_ID = 5000;
	private static final SkillHolder TRANSFORM_SKILL = new SkillHolder(29608, 1); // Grimnir's Siege Golem
	// Instance Status
	private static final int SPAWNING_MONSTERS = 1;
	private static final int FIGHTING_MONSTERS = 2;
	private static final int FIGHTING_BOSS = 3;
	private int _instanceLevel = 0;
	
	/**
	 * Initializes the {@code LurinSecretWorkshop} instance script.<br>
	 * This constructor sets up all required NPCs, monsters, and events.<br>
	 * It registers starting conditions and spawn IDs for the workshop.
	 */
	public LurinSecretWorkshop()
	{
		super(TEMPLATE_ID);
		addStartNpc(HESED);
		addFirstTalkId(GRIMNIR);
		addTalkId(HESED, GRIMNIR);
		addSpawnId(MONSTERS_105);
		addSpawnId(MONSTERS_110);
		addSpawnId(MONSTERS_115);
		addSpawnId(MONSTERS_120);
		addSpawnId(ADDITIONAL_MONSTERS);
		addSpawnId(BOSSES);
		addCreatureSeeId(MONSTERS_105);
		addCreatureSeeId(MONSTERS_110);
		addCreatureSeeId(MONSTERS_115);
		addCreatureSeeId(MONSTERS_120);
		addCreatureSeeId(ADDITIONAL_MONSTERS);
		addCreatureSeeId(BOSSES);
		addInstanceLeaveId(TEMPLATE_ID);
	}
	
	/**
	 * Handles various events within the Lurin Secret Workshop instance.<br>
	 * It manages NPC interactions, instance entry/exit logic, and monster spawning based on player level.<br>
	 * The method also handles specific quest timers for boss spawns and status checks.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} object involved in the event.
	 * @param player The {@code Player} object interacting with the instance.
	 * @return The HTML content to display or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		final long currentTime = System.currentTimeMillis();
		switch (event)
		{
			case "33780.html":
			case "33780-1.html":
			{
				htmltext = event;
				return htmltext;
			}
			case "showPoints":
			{
				final NpcHtmlMessage html = getNpcHtmlMessage(player, npc, "points.html");
				html.replace("%points%", player.getVariables().getInt(PlayerVariables.ILLUSORY_POINTS_ACQUIRED, 0));
				player.sendPacket(html);
				break;
			}
			case "enterInstance":
			{
				if (currentTime < InstanceManager.getInstance().getInstanceTime(player, TEMPLATE_ID))
				{
					player.sendPacket(new SystemMessage(SystemMessageId.C1_CANNOT_ENTER_YET).addString(player.getName()));
					htmltext = "condNoEnter.html";
					return htmltext;
				}
				
				enterInstance(player, npc, TEMPLATE_ID);
				break;
			}
			case "exitInstance":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.finishInstance();
				break;
			}
			case "startInstance":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				final int playerLevel = player.getLevel();
				if (playerLevel <= 109)
				{
					_instanceLevel = 105;
				}
				else if ((playerLevel >= 110) && (playerLevel <= 114))
				{
					_instanceLevel = 110;
				}
				else if ((playerLevel >= 115) && (playerLevel <= 119))
				{
					_instanceLevel = 115;
				}
				else if (playerLevel >= 120)
				{
					_instanceLevel = 120;
				}
				
				// Dispell transform or mount
				if (player.isTransformed())
				{
					player.untransform();
				}
				
				if (player.isMounted())
				{
					player.dismount();
				}
				
				player.doCast(TRANSFORM_SKILL.getSkill());
				world.setStatus(SPAWNING_MONSTERS);
				startQuestTimer("check_status", 1000, null, player);
				break;
			}
			case "check_status":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				switch (world.getStatus())
				{
					case SPAWNING_MONSTERS:
					{
						if (!world.getParameters().getBoolean("MONSTERS_SPAWNED", false))
						{
							world.setStatus(FIGHTING_MONSTERS);
							world.getParameters().set("MONSTERS_SPAWNED", true);
							player.sendPacket(new ExSendUIEvent(player, false, false, 600, 0, NpcStringId.TIME_LEFT));
							startQuestTimer("spawn_copy", 180 * 1000, null, player); // Copy Golem appears in 3 minutes.
							startQuestTimer("spawn_boss", 420 * 1000, null, player); // Boss Lurin appears in 7 minutes.
							startQuestTimer("end_fight", 600 * 1000, null, player); // End fight timer.
							startQuestTimer("check_status", 1000, null, player);
							switch (_instanceLevel)
							{
								case 105:
								{
									world.spawnGroup("MONSTERS_105_GROUP_1");
									world.spawnGroup("MONSTERS_105_GROUP_2");
									world.spawnGroup("MONSTERS_105_GROUP_3");
									world.spawnGroup("MONSTERS_105_GROUP_4");
									break;
								}
								case 110:
								{
									world.spawnGroup("MONSTERS_110_GROUP_1");
									world.spawnGroup("MONSTERS_110_GROUP_2");
									world.spawnGroup("MONSTERS_110_GROUP_3");
									world.spawnGroup("MONSTERS_110_GROUP_4");
									break;
								}
								case 115:
								{
									world.spawnGroup("MONSTERS_115_GROUP_1");
									world.spawnGroup("MONSTERS_115_GROUP_2");
									world.spawnGroup("MONSTERS_115_GROUP_3");
									world.spawnGroup("MONSTERS_115_GROUP_4");
									break;
								}
								case 120:
								{
									world.spawnGroup("MONSTERS_120_GROUP_1");
									world.spawnGroup("MONSTERS_120_GROUP_2");
									world.spawnGroup("MONSTERS_120_GROUP_3");
									world.spawnGroup("MONSTERS_120_GROUP_4");
									break;
								}
							}
						}
						break;
					}
					case FIGHTING_BOSS:
					{
						if (world.getAliveNpcCount(BOSSES) == 0)
						{
							world.despawnGroup("GRIMNIR_DOOR");
							world.finishInstance(5);
						}
						else
						{
							startQuestTimer("check_status", 1000, null, player);
						}
						break;
					}
				}
				break;
			}
			case "spawn_copy":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				if (getRandom(100) < ADDITIONAL_MONSTER_SPAWN_CHANCE)
				{
					switch (_instanceLevel)
					{
						case 105:
						{
							world.spawnGroup("ADDITIONAL_MONSTER_105");
							break;
						}
						case 110:
						{
							world.spawnGroup("ADDITIONAL_MONSTER_110");
							break;
						}
						case 115:
						{
							world.spawnGroup("ADDITIONAL_MONSTER_115");
							break;
						}
						case 120:
						{
							world.spawnGroup("ADDITIONAL_MONSTER_120");
							break;
						}
					}
					
					showOnScreenMsg(world, NpcStringId.DESTROY_THE_INVADER, ExShowScreenMessage.TOP_CENTER, 10000, true);
				}
				break;
			}
			case "spawn_boss":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				switch (_instanceLevel)
				{
					case 105:
					{
						world.spawnGroup("BOSS_105");
						break;
					}
					case 110:
					{
						world.spawnGroup("BOSS_110");
						break;
					}
					case 115:
					{
						world.spawnGroup("BOSS_115");
						break;
					}
					case 120:
					{
						world.spawnGroup("BOSS_120");
						break;
					}
				}
				
				showOnScreenMsg(world, NpcStringId.YOU_MADE_ME_ANGRY, ExShowScreenMessage.TOP_CENTER, 10000, true);
				world.setStatus(FIGHTING_BOSS);
				break;
			}
			case "end_fight":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				switch (_instanceLevel)
				{
					case 105:
					{
						world.despawnGroup("MONSTERS_105_GROUP_1");
						world.despawnGroup("MONSTERS_105_GROUP_2");
						world.despawnGroup("MONSTERS_105_GROUP_3");
						world.despawnGroup("MONSTERS_105_GROUP_4");
						break;
					}
					case 110:
					{
						world.despawnGroup("MONSTERS_110_GROUP_1");
						world.despawnGroup("MONSTERS_110_GROUP_2");
						world.despawnGroup("MONSTERS_110_GROUP_3");
						world.despawnGroup("MONSTERS_110_GROUP_4");
						break;
					}
					case 115:
					{
						world.despawnGroup("MONSTERS_115_GROUP_1");
						world.despawnGroup("MONSTERS_115_GROUP_2");
						world.despawnGroup("MONSTERS_115_GROUP_3");
						world.despawnGroup("MONSTERS_115_GROUP_4");
						break;
					}
					case 120:
					{
						world.despawnGroup("MONSTERS_120_GROUP_1");
						world.despawnGroup("MONSTERS_120_GROUP_2");
						world.despawnGroup("MONSTERS_120_GROUP_3");
						world.despawnGroup("MONSTERS_120_GROUP_4");
						break;
					}
				}
				
				world.spawnGroup("GRIMNIR_CENTER");
				world.despawnGroup("GRIMNIR_DOOR");
				if (player.isTransformed())
				{
					player.untransform();
				}
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when an {@code Npc} sees a {@code Creature}.<br>
	 * It checks if the {@code Creature} is a {@code Player} and within range.<br>
	 * If conditions are met, it makes the {@code Npc} aggressive toward the player.
	 * @param npc The {@code Npc} that sees the creature.
	 * @param player The {@code Creature} being seen by the {@code Npc}.
	 */
	@Override
	public void onCreatureSee(Npc npc, Creature player)
	{
		final Instance world = player.getInstanceWorld();
		if ((world != null) && (player.isPlayer()))
		{
			final double distance = npc.calculateDistance2D(player);
			if (distance < 900)
			{
				npc.asMonster().addDamageHate(player, 0, 1000);
				npc.getAI().setIntentionActive();
				addAttackDesire(npc, player);
			}
		}
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It checks if the NPC is inside an instance.<br>
	 * If it is, it disables random animations and enables walking for the NPC.
	 * @param npc The {@code Npc} that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		final Instance world = npc.getInstanceWorld();
		if (!isInInstance(world))
		{
			return;
		}
		
		npc.setRandomAnimation(false);
		npc.setWalking();
	}
	
	/**
	 * Handles the logic when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * It checks the current status of the instance world.<br>
	 * The method returns different HTML files based on whether the player is fighting a boss, monsters, or neither.
	 * @param npc The {@code Npc} that was interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A string representing the HTML file to display, or {@code null} if the world is invalid.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance world = npc.getInstanceWorld();
		if (world != null)
		{
			final int status = world.getStatus();
			if (status == FIGHTING_BOSS)
			{
				return "8310-exit.html";
			}
			
			if (status == FIGHTING_MONSTERS)
			{
				// Dispell transform or mount
				if (player.isTransformed())
				{
					player.untransform();
				}
				
				if (player.isMounted())
				{
					player.dismount();
				}
				
				player.doCast(TRANSFORM_SKILL.getSkill());
				return "8310-reenter.html";
			}
			
			return "8310-enter.html";
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return null;
	}
	
	/**
	 * This method is called when a {@code Player} leaves an {@code Instance}.<br>
	 * It checks if the player is currently transformed.<br>
	 * If they are, it reverts them to their original form.
	 * @param player The {@code Player} who is leaving the instance.
	 * @param instance The {@code Instance} being exited.
	 */
	@Override
	public void onInstanceLeave(Player player, Instance instance)
	{
		if (player.isTransformed())
		{
			player.untransform();
		}
	}
	
	/**
	 * Retrieves and creates an {@code NpcHtmlMessage} from a specific file.<br>
	 * It loads the HTML content based on the provided {@code fileName}.<br>
	 * If the file is not found, it logs an error and returns {@code null}.
	 * @param player The {@code Player} who will receive the message.
	 * @param npc The {@code Npc} sending the message.
	 * @param fileName The name of the HTML file to load.
	 * @return The created {@code NpcHtmlMessage} or {@code null} if the file is missing.
	 */
	private NpcHtmlMessage getNpcHtmlMessage(Player player, Npc npc, String fileName)
	{
		final String text = getHtm(player, fileName);
		if (text == null)
		{
			LOGGER.info("Cannot find HTML file for " + getClass().getSimpleName() + " Instance: " + fileName);
			return null;
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(text);
		return html;
	}
	
	/**
	 * This is the entry point for the {@code LurinSecretWorkshop} class.<br>
	 * It initializes a new instance of the script.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new LurinSecretWorkshop();
	}
}
