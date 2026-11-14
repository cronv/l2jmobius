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
package custom.SellBuff;

import java.util.StringTokenizer;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.custom.SellBuffsConfig;
import org.l2jmobius.gameserver.data.holders.SellBuffHolder;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.handler.BypassHandler;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.managers.SellBuffsManager;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * This class handles bypass commands related to the {@code SellBuff} system.<br>
 * It allows players to interact with specific script actions via bypass messages.
 * @author St3eT
 */
public class SellBuffBypassHandler implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"sellbuffadd",
		"sellbuffaddskill",
		"sellbuffedit",
		"sellbuffchangeprice",
		"sellbuffremove",
		"sellbuffbuymenu",
		"sellbuffbuyskill",
		"sellbuffstart",
		"sellbuffstop",
	};
	
	/**
	 * Private constructor for the {@code SellBuffBypassHandler} class.<br>
	 * This constructor registers the handler if it is enabled in the configuration.<br>
	 * It prevents other classes from creating new instances of this handler.
	 */
	private SellBuffBypassHandler()
	{
		if (SellBuffsConfig.SELLBUFF_ENABLED)
		{
			BypassHandler.getInstance().registerHandler(this);
		}
	}
	
	/**
	 * Processes a command sent by a {@code Player}.<br>
	 * This method handles the logic for specific bypass commands.<br>
	 * It checks if the command is valid and executes the corresponding action.
	 * @param command The raw command string provided by the player.
	 * @param player The {@code Player} who executed the command.
	 * @param target The {@code Creature} that was targeted, if any.
	 * @return {@code true} if the command was handled successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		String cmd = "";
		final StringBuilder params = new StringBuilder();
		final StringTokenizer st = new StringTokenizer(command, " ");
		
		if (st.hasMoreTokens())
		{
			cmd = st.nextToken();
		}
		
		while (st.hasMoreTokens())
		{
			params.append(st.nextToken() + (st.hasMoreTokens() ? " " : ""));
		}
		
		if (cmd.isEmpty())
		{
			return false;
		}
		
		return onCommand(cmd, player, params.toString());
	}
	
	/**
	 * Processes bypass commands for the sell buffs system.<br>
	 * It handles actions like adding, editing, and starting buff sales.
	 * @param command The name of the command to execute.
	 * @param player The {@code Player} who sent the command.
	 * @param params The additional parameters provided with the command.
	 * @return {@code true} if the command was handled successfully, otherwise {@code false}.
	 */
	private boolean onCommand(String command, Player player, String params)
	{
		if (!SellBuffsConfig.SELLBUFF_ENABLED)
		{
			return false;
		}
		
		switch (command)
		{
			case "sellbuffstart":
			{
				if (player.isSellingBuffs() || (params == null) || params.isEmpty())
				{
					return false;
				}
				else if (player.getSellingBuffs().isEmpty())
				{
					player.sendMessage("Your list of buffs is empty, please add some buffs first!");
					return false;
				}
				else
				{
					final StringBuilder title = new StringBuilder();
					title.append("BUFF SELL: ");
					final StringTokenizer st = new StringTokenizer(params, " ");
					while (st.hasMoreTokens())
					{
						title.append(st.nextToken() + " ");
					}
					
					if (title.length() > 40)
					{
						player.sendMessage("Your title cannot exceed 29 characters in length. Please try again.");
						return false;
					}
					
					SellBuffsManager.getInstance().startSellBuffs(player, title.toString());
				}
				break;
			}
			case "sellbuffstop":
			{
				if (player.isSellingBuffs())
				{
					SellBuffsManager.getInstance().stopSellBuffs(player);
				}
				break;
			}
			case "sellbuffadd":
			{
				if (!player.isSellingBuffs())
				{
					int index = 0;
					if ((params != null) && !params.isEmpty() && StringUtil.isNumeric(params))
					{
						index = Integer.parseInt(params);
					}
					
					SellBuffsManager.getInstance().sendBuffChoiceMenu(player, index);
				}
				break;
			}
			case "sellbuffedit":
			{
				if (!player.isSellingBuffs())
				{
					SellBuffsManager.getInstance().sendBuffEditMenu(player);
				}
				break;
			}
			case "sellbuffchangeprice":
			{
				if (!player.isSellingBuffs() && (params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");
					
					int skillId = -1;
					int price = -1;
					
					if (st.hasMoreTokens())
					{
						skillId = Integer.parseInt(st.nextToken());
					}
					
					if (st.hasMoreTokens())
					{
						try
						{
							price = Integer.parseInt(st.nextToken());
						}
						catch (NumberFormatException e)
						{
							player.sendMessage("Too big price! Maximum price is " + SellBuffsConfig.SELLBUFF_MAX_PRICE);
							SellBuffsManager.getInstance().sendBuffEditMenu(player);
						}
					}
					
					if ((skillId == -1) || (price == -1))
					{
						return false;
					}
					
					final Skill skillToChange = player.getKnownSkill(skillId);
					if (skillToChange == null)
					{
						return false;
					}
					
					final SellBuffHolder holder = player.getSellingBuffs().stream().filter(h -> (h.getSkillId() == skillToChange.getId())).findFirst().orElse(null);
					if (holder != null)
					{
						player.sendMessage("Price of " + player.getKnownSkill(holder.getSkillId()).getName() + " has been changed to " + price + "!");
						holder.setPrice(price);
						SellBuffsManager.getInstance().sendBuffEditMenu(player);
					}
				}
				break;
			}
			case "sellbuffremove":
			{
				if (!player.isSellingBuffs() && (params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");
					
					int skillId = -1;
					
					if (st.hasMoreTokens())
					{
						skillId = Integer.parseInt(st.nextToken());
					}
					
					if (skillId == -1)
					{
						return false;
					}
					
					final Skill skillToRemove = player.getKnownSkill(skillId);
					if (skillToRemove == null)
					{
						return false;
					}
					
					final SellBuffHolder holder = player.getSellingBuffs().stream().filter(h -> (h.getSkillId() == skillToRemove.getId())).findFirst().orElse(null);
					if ((holder != null) && player.getSellingBuffs().remove(holder))
					{
						player.sendMessage("Skill " + player.getKnownSkill(holder.getSkillId()).getName() + " has been removed!");
						SellBuffsManager.getInstance().sendBuffEditMenu(player);
					}
				}
				break;
			}
			case "sellbuffaddskill":
			{
				if (!player.isSellingBuffs() && (params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");
					
					int skillId = -1;
					long price = -1;
					
					if (st.hasMoreTokens())
					{
						skillId = Integer.parseInt(st.nextToken());
					}
					
					if (st.hasMoreTokens())
					{
						try
						{
							price = Integer.parseInt(st.nextToken());
						}
						catch (NumberFormatException e)
						{
							player.sendMessage("Too big price! Maximum price is " + SellBuffsConfig.SELLBUFF_MIN_PRICE);
							SellBuffsManager.getInstance().sendBuffEditMenu(player);
						}
					}
					
					if ((skillId == -1) || (price == -1))
					{
						return false;
					}
					
					final Skill skillToAdd = player.getKnownSkill(skillId);
					if (skillToAdd == null)
					{
						return false;
					}
					else if (price < SellBuffsConfig.SELLBUFF_MIN_PRICE)
					{
						player.sendMessage("Too small price! Minimum price is " + SellBuffsConfig.SELLBUFF_MIN_PRICE);
						return false;
					}
					else if (price > SellBuffsConfig.SELLBUFF_MAX_PRICE)
					{
						player.sendMessage("Too big price! Maximum price is " + SellBuffsConfig.SELLBUFF_MAX_PRICE);
						return false;
					}
					else if (player.getSellingBuffs().size() >= SellBuffsConfig.SELLBUFF_MAX_BUFFS)
					{
						player.sendMessage("You already reached max count of buffs! Max buffs is: " + SellBuffsConfig.SELLBUFF_MAX_BUFFS);
						return false;
					}
					else if (!SellBuffsManager.getInstance().isInSellList(player, skillToAdd))
					{
						player.getSellingBuffs().add(new SellBuffHolder(skillToAdd.getId(), price));
						player.sendMessage(skillToAdd.getName() + " has been added!");
						SellBuffsManager.getInstance().sendBuffChoiceMenu(player, 0);
					}
				}
				break;
			}
			case "sellbuffbuymenu":
			{
				if ((params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");
					
					int objId = -1;
					int index = 0;
					if (st.hasMoreTokens())
					{
						objId = Integer.parseInt(st.nextToken());
					}
					
					if (st.hasMoreTokens())
					{
						index = Integer.parseInt(st.nextToken());
					}
					
					final Player seller = World.getPlayer(objId);
					if (seller != null)
					{
						if (!seller.isSellingBuffs() || !player.isInsideRadius3D(seller, Npc.INTERACTION_DISTANCE))
						{
							return false;
						}
						
						SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
					}
				}
				break;
			}
			case "sellbuffbuyskill":
			{
				if ((params != null) && !params.isEmpty())
				{
					final StringTokenizer st = new StringTokenizer(params, " ");
					int objId = -1;
					int skillId = -1;
					int index = 0;
					
					if (st.hasMoreTokens())
					{
						objId = Integer.parseInt(st.nextToken());
					}
					
					if (st.hasMoreTokens())
					{
						skillId = Integer.parseInt(st.nextToken());
					}
					
					if (st.hasMoreTokens())
					{
						index = Integer.parseInt(st.nextToken());
					}
					
					if ((skillId == -1) || (objId == -1))
					{
						return false;
					}
					
					final Player seller = World.getPlayer(objId);
					if (seller == null)
					{
						return false;
					}
					
					final Skill skillToBuy = seller.getKnownSkill(skillId);
					if (!seller.isSellingBuffs() || !LocationUtil.checkIfInRange(Npc.INTERACTION_DISTANCE, player, seller, true) || (skillToBuy == null))
					{
						return false;
					}
					
					if (seller.getCurrentMp() < (skillToBuy.getMpConsume() * SellBuffsConfig.SELLBUFF_MP_MULTIPLER))
					{
						player.sendMessage(seller.getName() + " has no enough mana for " + skillToBuy.getName() + "!");
						SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
						return false;
					}
					
					final SellBuffHolder holder = seller.getSellingBuffs().stream().filter(h -> (h.getSkillId() == skillToBuy.getId())).findFirst().orElse(null);
					if (holder != null)
					{
						if (Quest.getQuestItemsCount(player, SellBuffsConfig.SELLBUFF_PAYMENT_ID) >= holder.getPrice())
						{
							Quest.takeItems(player, SellBuffsConfig.SELLBUFF_PAYMENT_ID, holder.getPrice());
							Quest.giveItems(seller, SellBuffsConfig.SELLBUFF_PAYMENT_ID, holder.getPrice());
							seller.reduceCurrentMp(skillToBuy.getMpConsume() * SellBuffsConfig.SELLBUFF_MP_MULTIPLER);
							skillToBuy.activateSkill(seller, player);
						}
						else
						{
							final ItemTemplate item = ItemData.getInstance().getTemplate(SellBuffsConfig.SELLBUFF_PAYMENT_ID);
							if (item != null)
							{
								player.sendMessage("Not enough " + item.getName() + "!");
							}
							else
							{
								player.sendMessage("Not enough items!");
							}
						}
					}
					
					SellBuffsManager.getInstance().sendBuffMenu(player, seller, index);
				}
				break;
			}
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of available bypass commands.<br>
	 * This method returns all valid command strings for this handler.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
	
	/**
	 * This is the entry point for the {@code SellBuffBypassHandler} class.<br>
	 * It initializes a new instance of the handler to start the application.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new SellBuffBypassHandler();
	}
}
