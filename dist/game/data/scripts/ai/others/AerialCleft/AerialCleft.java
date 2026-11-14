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
package ai.others.AerialCleft;

import org.l2jmobius.gameserver.config.AerialCleftConfig;
import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.games.AerialCleftManager;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * This class defines the artificial intelligence behavior of the Aerial Cleft npcs.<br>
 * The Vortex Gates register players for the battle, the compressors score for the team that destroys them and the Emery Converter trades the spoils.
 * @author Altur
 */
public class AerialCleft extends Script
{
	// NPCs
	private static final int[] VORTEX_GATES =
	{
		32518,
		32519
	};
	private static final int EMERY_CONVERTER = 32520;
	private static final int[] BATTLE_NPCS =
	{
		22553,
		22554,
		22555,
		22556,
		22557,
		22558
	};
	
	// Skills
	private static final int[] FLIGHT_TRANSFORMATIONS =
	{
		840,
		841,
		842
	};
	
	// Misc
	private static final int EMERY_MULTISELL = 32520001;
	
	/**
	 * Private constructor for the {@code AerialCleft} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private AerialCleft()
	{
		addFirstTalkId(VORTEX_GATES);
		addFirstTalkId(EMERY_CONVERTER);
		addKillId(BATTLE_NPCS);
	}
	
	/**
	 * Handles the links of the Aerial Cleft npcs.<br>
	 * It registers players for the battle, cancels their registration and opens the exchange of the Emery Converter.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the npc.
	 * @return A {@code String} representing the HTML response or the result of the super method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final AerialCleftManager manager = AerialCleftManager.getInstance();
		switch (event)
		{
			case "register":
			{
				if (manager.isParticipant(player.getObjectId()))
				{
					return "32518-02.html";
				}
				
				if ((player.getLevel() < AerialCleftConfig.CLEFT_MIN_LEVEL) || !canFly(player))
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_ENTER_THE_AERIAL_CLEFT_BECAUSE_YOU_ARE_BELOW_THE_REQUIRED_LEVEL_ENTRY_IS_POSSIBLE_ONLY_AT_LEVEL_75_OR_ABOVE);
					return "32518-03.html";
				}
				
				if (player.isRegisteredOnEvent() || player.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player) || (player.getKrateiArena() != null) || (player.getUCState() > 0))
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_BE_SIMULTANEOUSLY_REGISTERED_FOR_PVP_MATCHES_SUCH_AS_THE_OLYMPIAD_UNDERGROUND_COLISEUM_AERIAL_CLEFT_KRATEI_S_CUBE_AND_HANDY_S_BLOCK_CHECKERS);
					return null;
				}
				
				if (player.isCursedWeaponEquipped() || player.isInDuel() || player.isDead())
				{
					return null;
				}
				
				if (!manager.isRegistrationOpen())
				{
					return "32518-04.html";
				}
				
				if (manager.getParticipantCount() >= AerialCleftConfig.CLEFT_MAX_PLAYERS)
				{
					return "32518-05.html";
				}
				
				manager.registerPlayer(player);
				return null;
			}
			case "leave":
			{
				final int objectId = player.getObjectId();
				if (manager.isParticipant(objectId))
				{
					manager.removePlayer(objectId, true);
					player.sendPacket(SystemMessageId.THE_AERIAL_CLEFT_REGISTRATION_IS_CANCELLED);
				}
				
				return null;
			}
			case "32518-01.html":
			{
				return event;
			}
			case "exchange":
			{
				MultisellData.getInstance().separateAndSend(EMERY_MULTISELL, player, npc, false);
				return null;
			}
			default:
			{
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method handles the first interaction between a {@code Player} and an {@code Npc}.
	 * @param npc The {@code Npc} entity that is being spoken to.
	 * @param player The {@code Player} entity who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * Reports the death of a battlefield npc to the event.
	 * @param npc The killed npc.
	 * @param killer The {@code Player} that killed it.
	 * @param isSummon {@code true} if the killing blow came from a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		AerialCleftManager.getInstance().onBattleNpcKilled(npc, killer);
	}
	
	/**
	 * Verifies if a player knows one of the aerial transformations the battlefield requires.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the player can transform, {@code false} otherwise.
	 */
	private boolean canFly(Player player)
	{
		for (int skillId : FLIGHT_TRANSFORMATIONS)
		{
			if (player.getSkillLevel(skillId) > 0)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * This is the entry point for the {@code AerialCleft} class.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new AerialCleft();
	}
}
