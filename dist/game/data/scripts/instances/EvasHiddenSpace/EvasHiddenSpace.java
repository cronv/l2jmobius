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
package instances.EvasHiddenSpace;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;

/**
 * Defines the logic and behavior for the {@code EvasHiddenSpace} instance zone.<br>
 * This class handles the script execution for Eva's Hidden Space area.
 * @author CostyKiller
 */
public class EvasHiddenSpace extends InstanceScript
{
	// NPCs
	private static final int EVAS_AVATAR = 34515;
	
	// Misc
	private static final int TEMPLATE_ID = 217;
	
	/**
	 * Initializes the {@code EvasHiddenSpace} instance script.<br>
	 * Sets up the required NPCs and talk IDs for the zone.
	 */
	public EvasHiddenSpace()
	{
		super(TEMPLATE_ID);
		addStartNpc(EVAS_AVATAR);
		addFirstTalkId(EVAS_AVATAR);
	}
	
	/**
	 * Handles specific events triggered within the instance.<br>
	 * It manages entering and exiting the zone based on quest progress.
	 * @param event The name of the event to process.
	 * @param npc The {@code Npc} involved in the event.
	 * @param player The {@code Player} who triggered the event.
	 * @return A {@code String} containing the HTML text to display, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("enterInstance"))
		{
			enterInstance(player, npc, TEMPLATE_ID);
		}
		return null;
	}
	
	/**
	 * Handles the logic when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * This method completes the instance and returns {@code null}.
	 * @param npc The {@code Npc} that is being interacted with.
	 * @param player The {@code Player} who is initiating the conversation.
	 * @return A {@code String} value of {@code null}.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		finishInstance(player, 1);
		return null;
	}
	
	/**
	 * This is the entry point for the {@code EvasHiddenSpace} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new EvasHiddenSpace();
	}
}
