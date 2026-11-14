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
package events.LoveYourGatekeeper;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.script.LongTimeEvent;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Handles the logic for the {@code LoveYourGatekeeper} event.<br>
 * This class manages the progression and requirements of the specific gatekeeper activity.
 * @author Gladicek
 */
public class LoveYourGatekeeper extends LongTimeEvent
{
	// NPC
	private static final int GATEKEEPER = 32477;
	
	// Item
	private static final int GATEKEEPER_TRANSFORMATION_STICK = 12814;
	
	// Skills
	private static final SkillHolder TELEPORTER_TRANSFORM = new SkillHolder(5655, 1);
	
	// Misc
	private static final int HOURS = 24;
	private static final int PRICE = 10000;
	private static final String REUSE = LoveYourGatekeeper.class.getSimpleName() + "_reuse";
	
	/**
	 * Private constructor for the {@code LoveYourGatekeeper} class.<br>
	 * This constructor initializes the event by registering required NPC IDs.<br>
	 * It sets up the starting NPC and talk IDs for the gatekeeper.
	 */
	private LoveYourGatekeeper()
	{
		addStartNpc(GATEKEEPER);
		addFirstTalkId(GATEKEEPER);
		addTalkId(GATEKEEPER);
		addVariableToClear(REUSE);
	}
	
	/**
	 * Handles specific logic for the Love Your Gatekeeper event.<br>
	 * It processes actions like purchasing a transformation stick or casting a skill.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} involved in the interaction.
	 * @param player The {@code Player} performing the action.
	 * @return A string representing the response or {@code null} if successful.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "transform_stick":
			{
				if (player.getAdena() >= PRICE)
				{
					final long reuse = player.getVariables().getLong(REUSE, 0);
					if (reuse > System.currentTimeMillis())
					{
						final long remainingTime = (reuse - System.currentTimeMillis()) / 1000;
						final int hours = (int) (remainingTime / 3600);
						final int minutes = (int) ((remainingTime % 3600) / 60);
						final SystemMessage sm = new SystemMessage(SystemMessageId.S1_WILL_BE_AVAILABLE_AGAIN_IN_S2_H_S3_MIN);
						sm.addItemName(GATEKEEPER_TRANSFORMATION_STICK);
						sm.addInt(hours);
						sm.addInt(minutes);
						player.sendPacket(sm);
					}
					else
					{
						takeItems(player, Inventory.ADENA_ID, PRICE);
						giveItems(player, GATEKEEPER_TRANSFORMATION_STICK, 1);
						player.getVariables().set(REUSE, System.currentTimeMillis() + (HOURS * 3600000));
					}
				}
				else
				{
					return "32477-3.htm";
				}
				
				return null;
			}
			case "transform":
			{
				if (!player.isTransformed())
				{
					player.doCast(TELEPORTER_TRANSFORM.getSkill());
				}
				
				return null;
			}
		}
		
		return event;
	}
	
	/**
	 * This method handles the initial interaction between a {@code Player} and an {@code Npc}.<br>
	 * It returns the name of the HTML file to display.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the filename for the dialogue window.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "32477.htm";
	}
	
	/**
	 * This is the entry point for the {@code LoveYourGatekeeper} class.<br>
	 * It initializes a new instance of the event.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new LoveYourGatekeeper();
	}
}
