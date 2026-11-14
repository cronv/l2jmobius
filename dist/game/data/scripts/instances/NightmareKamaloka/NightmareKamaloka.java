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
package instances.NightmareKamaloka;

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
import org.l2jmobius.gameserver.network.serverpackets.OnEventTrigger;

/**
 * This class defines the logic and behavior for the {@code NightmareKamaloka} instance zone.<br>
 * It handles the specific scripts and mechanics required for this dungeon.
 * @author St3eT, Tanatos
 */
public class NightmareKamaloka extends InstanceScript
{
	// NPCs
	private static final int BENUSTA = 34542;
	private static final int INNER_BENUSTA = 34653;
	
	// BOSSES
	private static final int MINO = 26093;
	private static final int SOLA = 26094;
	private static final int ARIARC = 26096;
	private static final int SIRRA = 26099;
	private static final int DARK_RIDER = 26102;
	
	private static final int NOEGG = 26461;
	private static final int KYSHIS = 26462;
	private static final int SSIZZ_CHRONIZEL = 26463;
	private static final int KANAN_CHRONIZEL = 26464;
	private static final int SIONE_ULAF = 26465;
	
	private static final int GLAKIAS = 26482;
	private static final int GARPAS = 26483;
	private static final int MARMO = 26484;
	private static final int GRAP = 26485;
	private static final int JIEL = 26486;
	
	private static final int KROW = 26536;
	private static final int SARBOTH = 26537;
	private static final int HARP = 26538;
	private static final int SKIA = 26539;
	private static final int WHIPLASH = 26540;
	
	// Items
	private static final ItemHolder BENUSTAS_REWARD_BOX_105 = new ItemHolder(82453, 1);
	private static final ItemHolder BENUSTAS_REWARD_BOX_110 = new ItemHolder(82454, 1);
	private static final ItemHolder BENUSTAS_REWARD_BOX_115 = new ItemHolder(82455, 1);
	private static final ItemHolder BENUSTAS_REWARD_BOX_120 = new ItemHolder(82925, 1);
	private static final ItemHolder BENUSTAS_SHINING_REWARD_BOX = new ItemHolder(82456, 1);
	
	// Misc
	private static final int TEMPLATE_ID = 321;
	private static final int DOOR1 = 18170002;
	private static final int DOOR2 = 18170004;
	private static final int DOOR3 = 18170006;
	private static final int DOOR4 = 18170008;
	
	// Effect Triggers
	private static final int DOOR1_CLOSED = 18170102;
	private static final int DOOR1_OPENING = 18170104;
	private static final int DOOR2_CLOSED = 18170202;
	private static final int DOOR2_OPENING = 18170204;
	private static final int DOOR3_CLOSED = 18170302;
	private static final int DOOR3_OPENING = 18170304;
	private static final int DOOR4_CLOSED = 18170402;
	private static final int DOOR4_OPENING = 18170404;
	
	/**
	 * Initializes the {@code NightmareKamaloka} instance script.<br>
	 * This constructor sets up the required NPCs and IDs for the zone.<br>
	 * It registers the starting NPC and defines spawn behaviors.
	 */
	public NightmareKamaloka()
	{
		super(TEMPLATE_ID);
		addStartNpc(BENUSTA);
		addFirstTalkId(INNER_BENUSTA);
		addKillId(MINO, SOLA, ARIARC, SIRRA, DARK_RIDER);
		addKillId(NOEGG, KYSHIS, SSIZZ_CHRONIZEL, KANAN_CHRONIZEL, SIONE_ULAF);
		addKillId(GLAKIAS, GARPAS, MARMO, GRAP, JIEL);
		addKillId(KROW, SARBOTH, HARP, SKIA, WHIPLASH);
	}
	
	/**
	 * Handles the dialogue for a player's first interaction with an {@code Npc}.<br>
	 * This method determines which HTML file to display based on the NPC ID and player status.<br>
	 * It checks if the player is in a party or has GM privileges.
	 * @param npc The {@code Npc} that was interacted with.
	 * @param player The {@code Player} who initiated the interaction.
	 * @return A {@code String} representing the path to the HTML file, or {@code null}.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance world = player.getInstanceWorld();
		String htmltext = null;
		if (isInInstance(world))
		{
			if (npc.getId() == INNER_BENUSTA)
			{
				if (player.isInParty())
				{
					final Party party = player.getParty();
					if (!party.isLeader(player))
					{
						return "34653-1.html";
					}
					
					return "34653.html";
				}
				else if (player.isGM())
				{
					return "34653.html";
				}
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles specific events for the Nightmare Kamaloka instance.<br>
	 * It manages entry requirements and boss spawning logic.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} associated with the event.
	 * @param player The {@code Player} who triggered the event.
	 * @return A {@code String} result or {@code null} if the action was blocked.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "34654-1.html":
			case "34654-2.html":
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
					startQuestTimer("door_close", 5000, null, player);
				}
				break;
			}
			case "door_close":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.broadcastPacket(new OnEventTrigger(DOOR1_CLOSED, true));
				world.broadcastPacket(new OnEventTrigger(DOOR2_CLOSED, true));
				world.broadcastPacket(new OnEventTrigger(DOOR3_CLOSED, true));
				world.broadcastPacket(new OnEventTrigger(DOOR4_CLOSED, true));
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
				
				final String instanceLv = world.getParameters().getString("INSTANCE_LEVEL", null);
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
						world.setStatus(2);
						switch (instanceLv)
						{
							case "105":
							{
								world.spawnGroup("BOSSES_105");
								break;
							}
							case "110":
							{
								world.spawnGroup("BOSSES_110");
								break;
							}
							case "115":
							{
								world.spawnGroup("BOSSES_115");
								break;
							}
							case "120":
							{
								world.spawnGroup("BOSSES_120");
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
	 * Handles logic when an {@code Npc} is killed.<br>
	 * This method checks if the kill occurred inside an instance.<br>
	 * It determines whether to open a new door or finish the instance based on the NPC ID.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who dealt the killing blow.
	 * @param isSummon Whether the kill was caused by a summon.
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
			final int npcId = npc.getId();
			switch (npcId)
			{
				case MINO:
				case NOEGG:
				case GLAKIAS:
				case KROW:
				{
					world.openCloseDoor(DOOR1, true);
					world.broadcastPacket(new OnEventTrigger(DOOR1_CLOSED, false));
					world.broadcastPacket(new OnEventTrigger(DOOR1_OPENING, true));
					break;
				}
				case SOLA:
				case KYSHIS:
				case GARPAS:
				case SARBOTH:
				{
					world.openCloseDoor(DOOR2, true);
					world.broadcastPacket(new OnEventTrigger(DOOR2_CLOSED, false));
					world.broadcastPacket(new OnEventTrigger(DOOR2_OPENING, true));
					break;
				}
				case ARIARC:
				case SSIZZ_CHRONIZEL:
				case MARMO:
				case HARP:
				{
					world.openCloseDoor(DOOR3, true);
					world.broadcastPacket(new OnEventTrigger(DOOR3_CLOSED, false));
					world.broadcastPacket(new OnEventTrigger(DOOR3_OPENING, true));
					break;
				}
				case SIRRA:
				case KANAN_CHRONIZEL:
				case GRAP:
				case SKIA:
				{
					world.openCloseDoor(DOOR4, true);
					world.broadcastPacket(new OnEventTrigger(DOOR4_CLOSED, false));
					world.broadcastPacket(new OnEventTrigger(DOOR4_OPENING, true));
					break;
				}
				case DARK_RIDER:
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
				case SIONE_ULAF:
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
				case JIEL:
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
				case WHIPLASH:
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
	}
	
	/**
	 * This method is called when a new instance is created.<br>
	 * It sets the initial party member count in the {@code Instance} parameters.<br>
	 * If the {@code Player} is not in a party, it defaults to 1.
	 * @param instance The {@code Instance} being created.
	 * @param player The {@code Player} who triggered the creation.
	 */
	@Override
	public void onInstanceCreated(Instance instance, Player player)
	{
		instance.getParameters().set("INITIAL_PARTY_MEMBERS", player.getParty() != null ? player.getParty().getMemberCount() : 1);
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It disables random walking for the spawned entity.
	 * @param npc The {@code Npc} that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		npc.setRandomWalking(false);
	}
	
	/**
	 * This is the entry point for the {@code NightmareKamaloka} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new NightmareKamaloka();
	}
}
