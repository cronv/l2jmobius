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
package handlers.chat.commands.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestFinishType;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.script.State;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowQuestMark;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.QuestList;

/**
 * Handles the admin command to display quest information.<br>
 * This class allows administrators to view and manage {@code Quest} data within the game world.
 * @author Korvin, Zoey76
 */
public class AdminShowQuests implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminShowQuests.class.getName());
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_charquestmenu",
		"admin_setcharquest"
	};
	
	private static final String[] _states =
	{
		"CREATED",
		"STARTED",
		"COMPLETED"
	};
	
	/**
	 * Processes admin commands for viewing or setting character quests.<br>
	 * It identifies the target player and executes the corresponding action.
	 * @param command The full command string provided by the administrator.
	 * @param activeChar The {@code Player} object who is executing the command.
	 * @return {@code true} if the command was processed successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final String[] cmdParams = command.split(" ");
		Player target = null;
		WorldObject targetObject = null;
		final String[] val = new String[4];
		val[0] = null;
		if (cmdParams.length > 1)
		{
			target = World.getPlayer(cmdParams[1]);
			if (cmdParams.length > 2)
			{
				if (cmdParams[2].equals("0"))
				{
					val[0] = "var";
					val[1] = "Start";
				}
				
				if (cmdParams[2].equals("1"))
				{
					val[0] = "var";
					val[1] = "Started";
				}
				
				if (cmdParams[2].equals("2"))
				{
					val[0] = "var";
					val[1] = "Completed";
				}
				
				if (cmdParams[2].equals("3"))
				{
					val[0] = "full";
				}
				
				if (cmdParams[2].contains("_"))
				{
					val[0] = "name";
					val[1] = cmdParams[2];
				}
				
				if ((cmdParams.length > 3) && cmdParams[3].equals("custom"))
				{
					val[0] = "custom";
					val[1] = cmdParams[2];
				}
			}
		}
		else
		{
			targetObject = activeChar.getTarget();
			if ((targetObject != null) && targetObject.isPlayer())
			{
				target = targetObject.asPlayer();
			}
		}
		
		if (target == null)
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return false;
		}
		
		if (command.startsWith("admin_charquestmenu"))
		{
			if (val[0] != null)
			{
				showQuestMenu(target, activeChar, val);
			}
			else
			{
				showFirstQuestMenu(target, activeChar);
			}
		}
		else if (command.startsWith("admin_setcharquest"))
		{
			if (cmdParams.length >= 5)
			{
				val[0] = cmdParams[2];
				val[1] = cmdParams[3];
				val[2] = cmdParams[4];
				if (cmdParams.length == 6)
				{
					val[3] = cmdParams[5];
				}
				
				setQuestVar(target, activeChar, val);
			}
			else
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Displays the initial quest management menu to an administrator.<br>
	 * This method sends an {@code NpcHtmlMessage} containing buttons for different quest states.<br>
	 * It allows the admin to view and edit quests for a specific player.
	 * @param target The {@code Player} whose quests will be managed.
	 * @param actor The {@code Player} who is executing the command.
	 */
	private void showFirstQuestMenu(Player target, Player actor)
	{
		final StringBuilder replyMSG = new StringBuilder("<html><body><table width=270><tr><td width=45><button value=\"Main\" action=\"bypass admin_admin\" width=45 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td width=180><center>Player: " + target.getName() + "</center></td><td width=45><button value=\"Back\" action=\"bypass admin_admin6\" width=45 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(0, 1);
		final int ID = target.getObjectId();
		final String name = target.getName();
		replyMSG.append("Quest Menu for <font color=\"LEVEL\">" + name + "</font> (ID:" + ID + ")<br><center>");
		replyMSG.append("<table width=250><tr><td><button value=\"CREATED\" action=\"bypass -h admin_charquestmenu " + name + " 0\" width=85 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
		replyMSG.append("<tr><td><button value=\"STARTED\" action=\"bypass -h admin_charquestmenu " + name + " 1\" width=85 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
		replyMSG.append("<tr><td><button value=\"COMPLETED\" action=\"bypass -h admin_charquestmenu " + name + " 2\" width=85 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
		replyMSG.append("<tr><td><br><button value=\"All\" action=\"bypass -h admin_charquestmenu " + name + " 3\" width=85 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
		replyMSG.append("<tr><td><br><br>Manual Edit by Quest number:<br></td></tr>");
		replyMSG.append("<tr><td><edit var=\"qn\" width=50 height=15><br><button value=\"Edit\" action=\"bypass -h admin_charquestmenu " + name + " $qn custom\" width=50 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
		replyMSG.append("</table></center></body></html>");
		adminReply.setHtml(replyMSG.toString());
		actor.sendPacket(adminReply);
	}
	
	/**
	 * Displays a quest management menu for a specific player.<br>
	 * This method generates an HTML interface to view, edit, or delete character quests.<br>
	 * It handles different display modes based on the provided values array.
	 * @param target The {@code Player} whose quests will be displayed.
	 * @param actor The {@code Player} who is executing the command.
	 * @param val An array of strings containing the menu mode and specific quest identifiers.
	 */
	private void showQuestMenu(Player target, Player actor, String[] val)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			ResultSet rs;
			PreparedStatement req;
			final int ID = target.getObjectId();
			final StringBuilder replyMSG = new StringBuilder("<html><body>");
			final NpcHtmlMessage adminReply = new NpcHtmlMessage(0, 1);
			
			switch (val[0])
			{
				case "full":
				{
					replyMSG.append("<table width=250><tr><td>Full Quest List for <font color=\"LEVEL\">" + target.getName() + "</font> (ID:" + ID + ")</td></tr>");
					req = con.prepareStatement("SELECT DISTINCT name FROM character_quests WHERE charId=? AND var='<state>' ORDER by name");
					req.setInt(1, ID);
					req.execute();
					rs = req.getResultSet();
					while (rs.next())
					{
						replyMSG.append("<tr><td><a action=\"bypass -h admin_charquestmenu " + target.getName() + " " + rs.getString(1) + "\">" + rs.getString(1) + "</a></td></tr>");
					}
					replyMSG.append("</table></body></html>");
					rs.close();
					req.close();
					break;
				}
				case "name":
				{
					final QuestState qs = target.getQuestState(val[1]);
					final String state = (qs != null) ? _states[qs.getState()] : "CREATED";
					replyMSG.append("Character: <font color=\"LEVEL\">" + target.getName() + "</font><br>Quest: <font color=\"LEVEL\">" + val[1] + "</font><br>State: <font color=\"LEVEL\">" + state + "</font><br><br>");
					replyMSG.append("<center><table width=250><tr><td>Var</td><td>Value</td><td>New Value</td><td>&nbsp;</td></tr>");
					req = con.prepareStatement("SELECT var,value FROM character_quests WHERE charId=? and name=?");
					req.setInt(1, ID);
					req.setString(2, val[1]);
					req.execute();
					rs = req.getResultSet();
					while (rs.next())
					{
						final String var_name = rs.getString(1);
						if (var_name.equals("<state>"))
						{
							continue;
						}
						
						replyMSG.append("<tr><td>" + var_name + "</td><td>" + rs.getString(2) + "</td><td><edit var=\"var" + var_name + "\" width=80 height=15></td><td><button value=\"Set\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + val[1] + " " + var_name + " $var" + var_name + "\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td><button value=\"Del\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + val[1] + " " + var_name + " delete\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
					}
					
					rs.close();
					req.close();
					replyMSG.append("</table><br><br><table width=250><tr><td>Repeatable quest:</td><td>Unrepeatable quest:</td></tr>");
					replyMSG.append("<tr><td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + val[1] + " state COMPLETED 1\" width=120 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
					replyMSG.append("<td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + val[1] + " state COMPLETED 0\" width=120 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
					replyMSG.append("</table><br><br><font color=\"ff0000\">Delete Quest from DB:</font><br><button value=\"Quest Delete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + val[1] + " state DELETE\" width=120 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
					replyMSG.append("</center></body></html>");
					break;
				}
				case "var":
				{
					replyMSG.append("Character: <font color=\"LEVEL\">" + target.getName() + "</font><br>Quests with state: <font color=\"LEVEL\">" + val[1] + "</font><br>");
					replyMSG.append("<table width=250>");
					req = con.prepareStatement("SELECT DISTINCT name FROM character_quests WHERE charId=? and var='<state>' and value=?");
					req.setInt(1, ID);
					req.setString(2, val[1]);
					req.execute();
					rs = req.getResultSet();
					while (rs.next())
					{
						replyMSG.append("<tr><td><a action=\"bypass -h admin_charquestmenu " + target.getName() + " " + rs.getString(1) + "\">" + rs.getString(1) + "</a></td></tr>");
					}
					
					rs.close();
					req.close();
					replyMSG.append("</table></body></html>");
					break;
				}
				case "custom":
				{
					boolean exqdb = true;
					boolean exqch = true;
					final int qnumber = Integer.parseInt(val[1]);
					String state = null;
					String qname = null;
					QuestState qs = null;
					
					final Quest quest = ScriptManager.getInstance().getQuest(qnumber);
					if (quest != null)
					{
						qname = quest.getName();
						qs = target.getQuestState(qname);
					}
					else
					{
						exqdb = false;
					}
					
					if (qs != null)
					{
						state = _states[qs.getState()];
					}
					else
					{
						exqch = false;
						state = "N/A";
					}
					
					if (exqdb)
					{
						if (exqch)
						{
							replyMSG.append("Character: <font color=\"LEVEL\">" + target.getName() + "</font><br>Quest: <font color=\"LEVEL\">" + qname + "</font><br>State: <font color=\"LEVEL\">" + state + "</font><br><br>");
							replyMSG.append("<center><table width=250><tr><td>Var</td><td>Value</td><td>New Value</td><td>&nbsp;</td></tr>");
							req = con.prepareStatement("SELECT var,value FROM character_quests WHERE charId=? and name=?");
							req.setInt(1, ID);
							req.setString(2, qname);
							req.execute();
							rs = req.getResultSet();
							while (rs.next())
							{
								final String var_name = rs.getString(1);
								if (var_name.equals("<state>"))
								{
									continue;
								}
								
								replyMSG.append("<tr><td>" + var_name + "</td><td>" + rs.getString(2) + "</td><td><edit var=\"var" + var_name + "\" width=80 height=15></td><td><button value=\"Set\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + qname + " " + var_name + " $var" + var_name + "\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td><button value=\"Del\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + qname + " " + var_name + " delete\" width=30 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
							}
							replyMSG.append("</table><br><br><table width=250><tr><td>Repeatable quest:</td><td>Unrepeatable quest:</td></tr>");
							replyMSG.append("<tr><td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + qname + " state COMPLETED 1\" width=100 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
							replyMSG.append("<td><button value=\"Quest Complete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + qname + " state COMPLETED 0\" width=100 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
							replyMSG.append("</table><br><br><font color=\"ff0000\">Delete Quest from DB:</font><br><button value=\"Quest Delete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + qname + " state DELETE\" width=100 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
							replyMSG.append("</center></body></html>");
						}
						else
						{
							replyMSG.append("Character: <font color=\"LEVEL\">" + target.getName() + "</font><br>Quest: <font color=\"LEVEL\">" + qname + "</font><br>State: <font color=\"LEVEL\">" + state + "</font><br><br>");
							replyMSG.append("<center>Start this Quest for player:<br>");
							replyMSG.append("<button value=\"Create Quest\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + qnumber + " state CREATE\" width=100 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br><br>");
							replyMSG.append("<font color=\"ee0000\">Only for Unrepeateble quests:</font><br>");
							replyMSG.append("<button value=\"Create & Complete\" action=\"bypass -h admin_setcharquest " + target.getName() + " " + qnumber + " state CC\" width=130 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"><br><br>");
							replyMSG.append("</center></body></html>");
						}
					}
					else
					{
						replyMSG.append("<center><font color=\"ee0000\">Quest with number </font><font color=\"LEVEL\">" + qnumber + "</font><font color=\"ee0000\"> doesn't exist!</font></center></body></html>");
					}
					break;
				}
			}
			
			adminReply.setHtml(replyMSG.toString());
			actor.sendPacket(adminReply);
		}
		catch (Exception e)
		{
			actor.sendMessage("There was an error.");
			LOGGER.warning(AdminShowQuests.class.getSimpleName() + ": " + e.getMessage());
		}
	}
	
	/**
	 * Updates or modifies the quest variables for a specific player.<br>
	 * This method handles actions like creating, deleting, or changing states of quests.<br>
	 * It also updates the visual quest information for the target player.
	 * @param target The {@code Player} whose quest data is being modified.
	 * @param actor The {@code Player} who is performing the action.
	 * @param val An array of strings containing the quest ID, operation type, and specific values.
	 */
	private void setQuestVar(Player target, Player actor, String[] val)
	{
		QuestState qs = target.getQuestState(val[0]);
		if ((qs == null) && val[2].equals("CREATE"))
		{
			final Quest quest = ScriptManager.getInstance().getQuest(Integer.parseInt(val[0]));
			if (quest != null)
			{
				qs = quest.newQuestState(target);
				target.setQuestState(qs);
			}
		}
		
		if (qs == null)
		{
			actor.sendMessage("Player has not created " + val[0]);
			return;
		}
		
		final String[] outval = new String[3];
		qs.setSimulated(false);
		
		if (val[1].equals("state"))
		{
			switch (val[2])
			{
				case "COMPLETED":
				{
					qs.exitQuest((val[3].equals("1")) ? QuestFinishType.REPEATABLE : QuestFinishType.ONE_TIME);
					break;
				}
				case "DELETE":
				{
					Quest.deleteQuestInDb(qs, true);
					qs.exitQuest(QuestFinishType.REPEATABLE);
					target.sendPacket(new QuestList(target));
					target.sendPacket(new ExShowQuestMark(qs.getQuest().getId(), qs.getCond()));
					break;
				}
				case "CREATE":
				{
					qs = ScriptManager.getInstance().getQuest(Integer.parseInt(val[0])).newQuestState(target);
					qs.setState(State.STARTED);
					qs.setCond(1);
					target.sendPacket(new QuestList(target));
					target.sendPacket(new ExShowQuestMark(qs.getQuest().getId(), qs.getCond()));
					val[0] = qs.getQuest().getName();
					break;
				}
				case "CC":
				{
					qs = ScriptManager.getInstance().getQuest(Integer.parseInt(val[0])).newQuestState(target);
					qs.exitQuest(QuestFinishType.ONE_TIME);
					target.sendPacket(new QuestList(target));
					target.sendPacket(new ExShowQuestMark(qs.getQuest().getId(), qs.getCond()));
					val[0] = qs.getQuest().getName();
					break;
				}
			}
		}
		else
		{
			if (val[2].equals("delete"))
			{
				qs.unset(val[1]);
			}
			else
			{
				qs.set(val[1], val[2]);
			}
			
			target.sendPacket(new QuestList(target));
			target.sendPacket(new ExShowQuestMark(qs.getQuest().getId(), qs.getCond()));
		}
		
		actor.sendMessage("");
		outval[0] = "name";
		outval[1] = val[0];
		showQuestMenu(target, actor, outval);
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns all valid command strings for this handler.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
