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
package ai.bosses.Baylor;

import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * Handles the logic for the Crystal Prison instance script.<br>
 * This class manages the progression and events within the boss encounter.<br>
 * It coordinates interactions between {@code Player} entities and the environment.
 * @author St3eT, Tanatos
 * @URL https://www.youtube.com/watch?v=eZWDAvnykss
 */
public class CrystalPrison extends InstanceScript
{
	// NPCs
	private static final int BENUSTA = 34542;
	private static final int INNER_BENUSTA = 34652;
	private static final int BAYLOR_105 = 29213;
	private static final int BAYLOR_110 = 29380;
	private static final int BAYLOR_115 = 29401;
	private static final int BAYLOR_120 = 29407;
	
	// Items
	private static final ItemHolder BENUSTAS_REWARD_BOX_105 = new ItemHolder(82453, 1);
	private static final ItemHolder BENUSTAS_REWARD_BOX_110 = new ItemHolder(82454, 1);
	private static final ItemHolder BENUSTAS_REWARD_BOX_115 = new ItemHolder(82455, 1);
	private static final ItemHolder BENUSTAS_REWARD_BOX_120 = new ItemHolder(82925, 1);
	private static final ItemHolder BENUSTAS_SHINING_REWARD_BOX = new ItemHolder(82456, 1);
	
	// Misc
	private static final int TEMPLATE_ID = 320;
	
	/**
	 * Initializes the {@code CrystalPrison} instance script.<br>
	 * Sets up the starting NPCs and required IDs for events.<br>
	 * Registers the specific enemies to be attacked or killed.
	 */
	public CrystalPrison()
	{
		super(TEMPLATE_ID);
		addStartNpc(BENUSTA);
		addFirstTalkId(INNER_BENUSTA);
		addAttackId(BAYLOR_105, BAYLOR_110, BAYLOR_115, BAYLOR_120);
		addKillId(BAYLOR_105, BAYLOR_110, BAYLOR_115, BAYLOR_120);
	}
	
	/**
	 * Handles various script events for the Crystal Prison instance.<br>
	 * It manages party entry requirements, difficulty selection, and NPC spawning.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} involved in the event.
	 * @param player The {@code Player} triggering the event.
	 * @return A {@code String} representing the result or original event name, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "34652-1.html":
			case "34652-2.html":
			{
				return event;
			}
			case "enterInstance":
			{
				if (player.isInParty())
				{
					final Party party = player.getParty();
					if (!party.isLeader(player))
					{
						player.sendPacket(SystemMessageId.ONLY_A_PARTY_LEADER_CAN_MAKE_THE_REQUEST_TO_ENTER);
						return null;
					}
					
					if (player.isInCommandChannel())
					{
						player.sendPacket(SystemMessageId.YOU_CANNOT_ENTER_AS_YOU_DON_T_MEET_THE_REQUIREMENTS);
						return null;
					}
					
					final List<Player> members = party.getMembers();
					for (Player member : members)
					{
						if (!member.isInsideRadius3D(npc, 1000))
						{
							player.sendMessage("Player " + member.getName() + " must come closer.");
							return null;
						}
					}
					
					for (Player member : members)
					{
						enterInstance(member, npc, TEMPLATE_ID);
					}
				}
				else if (player.isGM())
				{
					enterInstance(player, npc, TEMPLATE_ID);
				}
				else
				{
					player.sendPacket(SystemMessageId.YOU_ARE_NOT_IN_A_PARTY_SO_YOU_CANNOT_ENTER);
				}
				
				if (player.getInstanceWorld() != null)
				{
					startQuestTimer("check_status", 10000, null, player);
				}
				break;
			}
			case "benusta_texts":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				final int benustaText = world.getParameters().getInt("BENUSTA_TEXT", 0);
				if (world.getAliveNpcCount(INNER_BENUSTA) > 0)
				{
					switch (benustaText)
					{
						case 0:
						{
							showOnScreenMsg(world, NpcStringId.THE_PARTY_LEADER_SHOULD_TALK_TO_ME_AND_SELECT_THE_DIFFICULTY_LEVEL, ExShowScreenMessage.TOP_CENTER, 5000, true);
							world.getNpc(INNER_BENUSTA).broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THE_PARTY_LEADER_SHOULD_TALK_TO_ME_AND_SELECT_THE_DIFFICULTY_LEVEL);
							startQuestTimer("benusta_texts", 4000, null, player);
							world.getParameters().set("BENUSTA_TEXT", 1);
							break;
						}
						case 1:
						{
							showOnScreenMsg(world, NpcStringId.TAKE_INTO_ACCOUNT_THE_ABILITIES_OF_YOUR_PARTY_MEMBERS_WHEN_SELECTING_THE_DIFFICULTY, ExShowScreenMessage.TOP_CENTER, 5000, true);
							world.getNpc(INNER_BENUSTA).broadcastSay(ChatType.NPC_GENERAL, NpcStringId.TAKE_INTO_ACCOUNT_THE_ABILITIES_OF_YOUR_PARTY_MEMBERS_WHEN_SELECTING_THE_DIFFICULTY);
							startQuestTimer("benusta_texts", 4000, null, player);
							world.getParameters().set("BENUSTA_TEXT", 2);
							break;
						}
						case 2:
						{
							showOnScreenMsg(world, NpcStringId.THE_SELECTED_DIFFICULTY_CANNOT_BE_CHANGED_SO_CHOOSE_WISELY, ExShowScreenMessage.TOP_CENTER, 5000, true);
							world.getNpc(INNER_BENUSTA).broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THE_SELECTED_DIFFICULTY_CANNOT_BE_CHANGED_SO_CHOOSE_WISELY);
							world.getParameters().set("BENUSTA_TEXT", 0);
							break;
						}
					}
				}
				break;
			}
			case "easyInstance":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.getParameters().set("INSTANCE_LEVEL", "105");
				world.broadcastPacket(new ExShowScreenMessage("Lv. 105 is selected. Prepare for battle!", ExShowScreenMessage.TOP_CENTER, 8000, 0, true, true));
				world.getNpc(INNER_BENUSTA).deleteMe();
				world.setStatus(1);
				break;
			}
			case "mediumInstance":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.getParameters().set("INSTANCE_LEVEL", "110");
				world.broadcastPacket(new ExShowScreenMessage("Lv. 110 is selected. Prepare for battle!", ExShowScreenMessage.TOP_CENTER, 8000, 0, true, true));
				world.getNpc(INNER_BENUSTA).deleteMe();
				world.setStatus(1);
				break;
			}
			case "hardInstance":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.getParameters().set("INSTANCE_LEVEL", "115");
				world.broadcastPacket(new ExShowScreenMessage("Lv. 115 is selected. Prepare for battle!", ExShowScreenMessage.TOP_CENTER, 8000, 0, true, true));
				world.getNpc(INNER_BENUSTA).deleteMe();
				world.setStatus(1);
				break;
			}
			case "extremeInstance":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.getParameters().set("INSTANCE_LEVEL", "120");
				world.broadcastPacket(new ExShowScreenMessage("Lv. 120 is selected. Prepare for battle!", ExShowScreenMessage.TOP_CENTER, 8000, 0, true, true));
				world.getNpc(INNER_BENUSTA).deleteMe();
				world.setStatus(1);
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
					case 0:
					{
						if (world.getAliveNpcCount(INNER_BENUSTA) == 0)
						{
							world.spawnGroup("BENUSTA");
						}
						
						startQuestTimer("benusta_texts", 5000, null, player);
						startQuestTimer("check_status", 5000, null, player);
						break;
					}
					case 1:
					{
						final String instanceLv = world.getParameters().getString("INSTANCE_LEVEL", null);
						world.setStatus(2);
						switch (instanceLv)
						{
							case "105":
							{
								world.spawnGroup("BAYLOR_105");
								world.getNpcsOfGroup("BAYLOR_105").forEach(mob ->
								{
									if (mob != null)
									{
										mob.setRandomWalking(false);
									}
								});
								break;
							}
							case "110":
							{
								world.spawnGroup("BAYLOR_110");
								world.getNpcsOfGroup("BAYLOR_110").forEach(mob ->
								{
									if (mob != null)
									{
										mob.setRandomWalking(false);
									}
								});
								break;
							}
							case "115":
							{
								world.spawnGroup("BAYLOR_115");
								world.getNpcsOfGroup("BAYLOR_115").forEach(mob ->
								{
									if (mob != null)
									{
										mob.setRandomWalking(false);
									}
								});
								break;
							}
							case "120":
							{
								world.spawnGroup("BAYLOR_120");
								world.getNpcsOfGroup("BAYLOR_120").forEach(mob ->
								{
									if (mob != null)
									{
										mob.setRandomWalking(false);
									}
								});
								break;
							}
						}
						
						world.setReenterTime();
						break;
					}
				}
				break;
			}
		}
		
		return null;
	}
	
	/**
	 * Determines the correct dialogue file to show when a player talks to an {@code Npc} for the first time.<br>
	 * This method checks if the player is in an instance and identifies their role or status.<br>
	 * It returns different HTML files based on whether the player is a party leader or a GM.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} initiating the conversation.
	 * @return A string representing the path to the dialogue file, or {@code null} if no dialogue should play.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance world = player.getInstanceWorld();
		if (isInInstance(world))
		{
			if (player.isInParty())
			{
				final Party party = player.getParty();
				if (!party.isLeader(player))
				{
					return "34652-1.html";
				}
				
				return "34652.html";
			}
			else if (player.isGM())
			{
				return "34652.html";
			}
		}
		
		return null;
	}
	
	/**
	 * This method is called when an {@code Npc} is killed.<br>
	 * It handles reward distribution and instance completion logic.<br>
	 * Rewards are given to party members or administrators based on the specific boss defeated.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who dealt the killing blow.
	 * @param isSummon Indicates if the killed entity was a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			final Player firstPlayer = world.getFirstPlayer();
			final Party party = firstPlayer != null ? firstPlayer.getParty() : null;
			final Player randomPlayer = party != null ? party.getRandomPlayer() : null;
			final int killedBaylor = world.getParameters().getInt("KILLED_BAYLOR", 0);
			final int npcId = npc.getId();
			if (killedBaylor == 1)
			{
				switch (npcId)
				{
					case BAYLOR_105:
					{
						for (Player member : world.getPlayers())
						{
							if ((member.getParty() != null) || (member.isGM()))
							{
								giveItems(member, BENUSTAS_REWARD_BOX_105);
							}
						}
						
						if ((randomPlayer != null) && (getRandom(100) < 80) && (world.getPlayersCount() == world.getParameters().getInt("INITIAL_PARTY_MEMBERS", 0)))
						{
							if (randomPlayer.getParty() != null)
							{
								giveItems(randomPlayer, BENUSTAS_SHINING_REWARD_BOX);
							}
						}
						
						world.finishInstance();
						break;
					}
					case BAYLOR_110:
					{
						for (Player member : world.getPlayers())
						{
							if ((member.getParty() != null) || (member.isGM()))
							{
								giveItems(member, BENUSTAS_REWARD_BOX_110);
							}
						}
						
						if ((randomPlayer != null) && (getRandom(100) < 80) && (world.getPlayersCount() == world.getParameters().getInt("INITIAL_PARTY_MEMBERS", 0)))
						{
							if (randomPlayer.getParty() != null)
							{
								giveItems(randomPlayer, BENUSTAS_SHINING_REWARD_BOX);
							}
						}
						
						world.finishInstance();
						break;
					}
					case BAYLOR_115:
					{
						for (Player member : world.getPlayers())
						{
							if ((member.getParty() != null) || (member.isGM()))
							{
								giveItems(member, BENUSTAS_REWARD_BOX_115);
							}
						}
						
						if ((randomPlayer != null) && (getRandom(100) < 80) && (world.getPlayersCount() == world.getParameters().getInt("INITIAL_PARTY_MEMBERS", 0)))
						{
							if (randomPlayer.getParty() != null)
							{
								giveItems(randomPlayer, BENUSTAS_SHINING_REWARD_BOX);
							}
						}
						
						world.finishInstance();
						break;
					}
					case BAYLOR_120:
					{
						for (Player member : world.getPlayers())
						{
							if ((member.getParty() != null) || (member.isGM()))
							{
								giveItems(member, BENUSTAS_REWARD_BOX_120);
							}
						}
						
						if ((randomPlayer != null) && (getRandom(100) < 80) && (world.getPlayersCount() == world.getParameters().getInt("INITIAL_PARTY_MEMBERS", 0)))
						{
							if (randomPlayer.getParty() != null)
							{
								giveItems(randomPlayer, BENUSTAS_SHINING_REWARD_BOX);
							}
						}
						
						world.finishInstance();
						break;
					}
				}
			}
			else
			{
				world.getParameters().set("KILLED_BAYLOR", 1);
			}
		}
	}
	
	/**
	 * This method is called when the instance is first created.<br>
	 * It sets the initial party member count in the world parameters.<br>
	 * If the {@code player} is not in a party, it defaults to 1.
	 * @param world The {@code Instance} where the event occurs.
	 * @param player The {@code Player} who triggered the instance creation.
	 */
	@Override
	public void onInstanceCreated(Instance world, Player player)
	{
		world.getParameters().set("INITIAL_PARTY_MEMBERS", player.getParty() != null ? player.getParty().getMemberCount() : 1);
	}
	
	/**
	 * This is the entry point for the {@code CrystalPrison} class.<br>
	 * It initializes a new instance of the script.
	 * @param args The command line arguments.
	 */
	public static void main(String[] args)
	{
		new CrystalPrison();
	}
}
