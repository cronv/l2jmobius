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
package ai.others.CastleSiegeManager;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class manages the artificial intelligence for castle siege events.<br>
 * It handles the logic and behavior of entities during these specific game activities.
 * @author St3eT
 */
public class CastleSiegeManager extends Script
{
	// NPCs
	private static final int[] SIEGE_MANAGER =
	{
		35104, // Gludio Castle
		35146, // Dion Castle
		35188, // Giran Castle
		35232, // Oren Castle
		35278, // Aden Castle
		35320, // Innadril Castle
		35367, // Goddard Castle
		35513, // Rune Castle
		35559, // Schuttgart Castle
	};
	
	/**
	 * This is a private constructor for the {@code CastleSiegeManager} class.<br>
	 * It initializes the script by registering the required NPC IDs.<br>
	 * This method should not be called from outside this class.
	 */
	private CastleSiegeManager()
	{
		addFirstTalkId(SIEGE_MANAGER);
	}
	
	/**
	 * Handles the initial interaction between a {@code Player} and an {@code Npc}.<br>
	 * It determines which HTML message to display based on clan ownership.<br>
	 * If no specific message is set, it registers the player for the siege.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} who initiated the talk.
	 * @return A {@code String} containing the HTML text to display or null.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = null;
		if (player.isClanLeader() && (player.getClanId() == npc.getCastle().getOwnerId()))
		{
			if (isInSiege(npc))
			{
				htmltext = "CastleSiegeManager.html";
			}
			else
			{
				htmltext = "CastleSiegeManager-01.html";
			}
		}
		else if (isInSiege(npc))
		{
			htmltext = "CastleSiegeManager-02.html";
		}
		else
		{
			npc.getCastle().getSiege().listRegisterClan(player);
		}
		
		return htmltext;
	}
	
	/**
	 * Checks if the given {@code Npc} is currently involved in a siege.<br>
	 * This method verifies if the castle associated with the {@code Npc} has an active siege.
	 * @param npc The {@code Npc} to check.
	 * @return {@code true} if a siege is in progress, {@code false} otherwise.
	 */
	private boolean isInSiege(Npc npc)
	{
		return npc.getCastle().getSiege().isInProgress();
	}
	
	/**
	 * This is the entry point for the {@code CastleSiegeManager} application.<br>
	 * It initializes a new instance of the manager class.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new CastleSiegeManager();
	}
}
