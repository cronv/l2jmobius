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
package events.ChuseokHarvestFestival;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.mechanics.script.LongTimeEvent;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * Manages the logic for the Chuseok Harvest Festival event.<br>
 * This class handles the festival mechanics and activities for players.<br>
 * It extends {@code LongTimeEvent} to manage its duration.
 * @URL https://l2central.info/main/events_and_promos/2090.html?lang=en
 * @author CostyKiller, Tanatos
 */
public class ChuseokHarvestFestival extends LongTimeEvent
{
	// NPCs
	private static final int MOON_CHILD = 34604;
	private static final int FULL_MOON = 34605;
	
	// Skills
	private static final SkillHolder MOON_BLESSING = new SkillHolder(34400, 1);
	private static final SkillHolder WISHING_BLESSING = new SkillHolder(39817, 1);
	
	// Items
	private static final int VITALITY_RUNE = 81206;
	
	// Misc
	private static final String VITALITY_RUNE_VAR = "VITALITY_RUNE_RECEIVED";
	private static final Location GIRAN_LOC = new Location(81115, 148769, -3464);
	private static final Location RUNE_LOC = new Location(46000, -47746, -792);
	
	/**
	 * Initializes the {@code ChuseokHarvestFestival} event.<br>
	 * This constructor registers the required NPCs and talk IDs.
	 */
	private ChuseokHarvestFestival()
	{
		addStartNpc(MOON_CHILD, FULL_MOON);
		addFirstTalkId(MOON_CHILD, FULL_MOON);
		addVariableToClear(VITALITY_RUNE_VAR);
	}
	
	/**
	 * Handles specific interactions for the Chuseok Harvest Festival event.<br>
	 * This method processes requests like getting tickets, buffs, or teleporting.
	 * @param event The identifier of the action being performed.
	 * @param npc The {@code Npc} entity involved in the interaction.
	 * @param player The {@code Player} who triggered the event.
	 * @return The name of the HTML file to display, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "34604.htm":
			case "34604-01.htm":
			case "34605.htm":
			case "34605-01.htm":
			case "34605-02.htm":
			{
				htmltext = event;
				break;
			}
			case "toGiran":
			{
				if (npc.getId() != MOON_CHILD)
				{
					break;
				}
				
				player.teleToLocation(GIRAN_LOC);
				break;
			}
			case "toRune":
			{
				if (npc.getId() != MOON_CHILD)
				{
					break;
				}
				
				player.teleToLocation(RUNE_LOC);
				break;
			}
			case "makeWish":
			{
				if (npc.getId() != FULL_MOON)
				{
					break;
				}
				
				if (player.getAccountVariables().getBoolean(VITALITY_RUNE_VAR, false))
				{
					player.sendMessage("This account has already received a gift. The gift can only be given once per account");
					htmltext = "34605-02.htm";
					break;
				}
				
				player.getAccountVariables().set(VITALITY_RUNE_VAR, true);
				player.getAccountVariables().storeMe();
				giveItems(player, VITALITY_RUNE, 1);
				htmltext = "34605-02.htm";
				break;
			}
			
			// TODO: Craft
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the first interaction between a player and an {@code Npc}.<br>
	 * It determines which dialogue file to display based on the NPC ID.
	 * @param npc The {@code Npc} that is being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A string representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".htm";
	}
	
	/**
	 * Handles logic when a player logs into the server.<br>
	 * It checks if the event is currently active.<br>
	 * If inactive, it removes specific skill effects from the {@code Player}.<br>
	 * If active, it applies the {@code MOON_BLESSING} effect to the {@code Player}.
	 * @param event The {@code OnPlayerLogin} event containing the {@code Player} data.
	 */
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!isEventPeriod())
		{
			if (player.isAffectedBySkill(WISHING_BLESSING))
			{
				player.stopSkillEffects(WISHING_BLESSING.getSkill());
			}
			
			if (player.isAffectedBySkill(MOON_BLESSING))
			{
				player.stopSkillEffects(MOON_BLESSING.getSkill());
			}
			return;
		}
		
		MOON_BLESSING.getSkill().applyEffects(player, player);
	}
	
	/**
	 * This method handles logic when a player logs out.<br>
	 * It checks if the {@code Player} is affected by the moon blessing skill.<br>
	 * If they are, it removes that specific skill effect.
	 * @param event The {@code OnPlayerLogout} event containing the player data.
	 */
	@RegisterEvent(EventType.ON_PLAYER_LOGOUT)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogout(OnPlayerLogout event)
	{
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.isAffectedBySkill(MOON_BLESSING))
		{
			player.stopSkillEffects(MOON_BLESSING.getSkill());
		}
	}
	
	/**
	 * This is the entry point for the {@code ChuseokHarvestFestival} class.<br>
	 * It initializes a new instance of the event.
	 * @param args The command line arguments.
	 */
	public static void main(String[] args)
	{
		new ChuseokHarvestFestival();
	}
}
