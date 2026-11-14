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
package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Represents a specialized merchant NPC used for logistics within a fortress.<br>
 * This class handles specific trading logic and interactions for fort-related items.
 * @author Vice
 */
public class FortLogistics extends Merchant
{
	private static final int[] SUPPLY_BOX_IDS =
	{
		35665,
		35697,
		35734,
		35766,
		35803,
		35834,
		35866,
		35903,
		35935,
		35973,
		36010,
		36042,
		36080,
		36117,
		36148,
		36180,
		36218,
		36256,
		36293,
		36325,
		36363
	};
	
	/**
	 * Creates a new instance of {@code FortLogistics}.<br>
	 * This constructor initializes the object using the provided template.<br>
	 * It also sets the specific instance type to {@code InstanceType.FortLogistics}.
	 * @param template The {@code NpcTemplate} used to create this NPC.
	 */
	public FortLogistics(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.FortLogistics);
	}
	
	/**
	 * Handles bypass commands for the Fort Logistics NPC.<br>
	 * Processes specific requests like rewards, blood oath counts, and supply levels.<br>
	 * Sends the appropriate HTML message to the {@code Player} based on their permissions.
	 * @param player The {@code Player} who sent the command.
	 * @param command The string command received from the client.
	 */
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		final int objectId = getObjectId();
		if (player.getLastFolkNPC().getObjectId() != objectId)
		{
			return;
		}
		
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken(); // Get actual command
		final Clan clan = player.getClan();
		final boolean isMyLord = player.isClanLeader() && (clan.getFortId() == (getFort() != null ? getFort().getResidenceId() : -1));
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		if (actualCommand.equalsIgnoreCase("rewards"))
		{
			if (isMyLord)
			{
				html.setFile(player, "data/html/fortress/logistics-rewards.htm");
				html.replace("%bloodoath%", String.valueOf(clan.getBloodOathCount()));
			}
			else
			{
				html.setFile(player, "data/html/fortress/logistics-noprivs.htm");
			}
			
			html.replace("%objectId%", String.valueOf(objectId));
			player.sendPacket(html);
		}
		else if (actualCommand.equalsIgnoreCase("blood"))
		{
			if (isMyLord)
			{
				final int blood = clan.getBloodOathCount();
				if (blood > 0)
				{
					player.addItem(ItemProcessType.QUEST, 9910, blood, this, true);
					clan.resetBloodOathCount();
					html.setFile(player, "data/html/fortress/logistics-blood.htm");
				}
				else
				{
					html.setFile(player, "data/html/fortress/logistics-noblood.htm");
				}
			}
			else
			{
				html.setFile(player, "data/html/fortress/logistics-noprivs.htm");
			}
			
			html.replace("%objectId%", String.valueOf(objectId));
			player.sendPacket(html);
		}
		else if (actualCommand.equalsIgnoreCase("supplylvl"))
		{
			if (getFort().getFortState() == 2)
			{
				if (player.isClanLeader())
				{
					html.setFile(player, "data/html/fortress/logistics-supplylvl.htm");
					html.replace("%supplylvl%", String.valueOf(getFort().getSupplyLeveL()));
				}
				else
				{
					html.setFile(player, "data/html/fortress/logistics-noprivs.htm");
				}
			}
			else
			{
				html.setFile(player, "data/html/fortress/logistics-1.htm"); // TODO: Missing HTML?
			}
			
			html.replace("%objectId%", String.valueOf(objectId));
			player.sendPacket(html);
		}
		else if (actualCommand.equalsIgnoreCase("supply"))
		{
			if (isMyLord)
			{
				if (getFort().getSiege().isInProgress())
				{
					html.setFile(player, "data/html/fortress/logistics-siege.htm");
				}
				else
				{
					final int level = getFort().getSupplyLeveL();
					if (level > 0)
					{
						// spawn box
						final NpcTemplate boxTemplate = NpcData.getInstance().getTemplate(SUPPLY_BOX_IDS[level - 1]);
						final Monster box = new Monster(boxTemplate);
						box.setCurrentHp(box.getMaxHp());
						box.setCurrentMp(box.getMaxMp());
						box.setHeading(0);
						box.spawnMe(getX() - 23, getY() + 41, getZ());
						getFort().setSupplyLeveL(0);
						getFort().saveFortVariables();
						
						html.setFile(player, "data/html/fortress/logistics-supply.htm");
					}
					else
					{
						html.setFile(player, "data/html/fortress/logistics-nosupply.htm");
					}
				}
			}
			else
			{
				html.setFile(player, "data/html/fortress/logistics-noprivs.htm");
			}
			
			html.replace("%objectId%", String.valueOf(objectId));
			player.sendPacket(html);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	/**
	 * Opens the chat window for a specific player.<br>
	 * This method displays the default message window to the user.
	 * @param player The {@code Player} who will see the window.
	 */
	@Override
	public void showChatWindow(Player player)
	{
		showMessageWindow(player, 0);
	}
	
	/**
	 * Displays a specific HTML message window to the player.<br>
	 * This method sends an {@code ActionFailed} packet before showing the content.<br>
	 * It selects the correct file path based on the provided {@code value}.
	 * @param player The {@code Player} who will receive the message.
	 * @param value The identifier used to determine which HTML file to display.
	 */
	private void showMessageWindow(Player player, int value)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		String filename;
		if (value == 0)
		{
			filename = "data/html/fortress/logistics.htm";
		}
		else
		{
			filename = "data/html/fortress/logistics-" + value + ".htm";
		}
		
		final int objectId = getObjectId();
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		html.setFile(player, filename);
		html.replace("%objectId%", String.valueOf(objectId));
		html.replace("%npcId%", String.valueOf(getId()));
		if (getFort().getOwnerClan() != null)
		{
			html.replace("%clanname%", getFort().getOwnerClan().getName());
		}
		else
		{
			html.replace("%clanname%", "NPC");
		}
		
		player.sendPacket(html);
	}
	
	/**
	 * Retrieves the file path for a specific HTML template.<br>
	 * This is used to display information related to fortress logistics.
	 * @param npcId The unique identifier of the NPC.
	 * @param value The numeric value used to determine the specific HTML page.
	 * @param player The {@code Player} object requesting the path.
	 * @return A {@code String} containing the full file path to the .htm file.
	 */
	@Override
	public String getHtmlPath(int npcId, int value, Player player)
	{
		String pom = "";
		if (value == 0)
		{
			pom = "logistics";
		}
		else
		{
			pom = "logistics-" + value;
		}
		
		return "data/html/fortress/" + pom + ".htm";
	}
	
	/**
	 * Checks if this entity uses a random animation.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the entity has a random animation, otherwise {@code false}.
	 */
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
}
