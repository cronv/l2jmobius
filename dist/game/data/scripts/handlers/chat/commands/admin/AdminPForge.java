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

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Boat;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.network.serverpackets.AdminForgePacket;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * This class handles administrative commands that allow Game Masters to forge and send custom packets.<br>
 * It provides functionality for debugging and testing network communication within the game.
 * @author Maktakien, HorridoJoho
 */
public class AdminPForge implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_forge",
		"admin_forge_values",
		"admin_forge_send"
	};
	
	/**
	 * Extracts operation codes from a {@code StringTokenizer}.<br>
	 * It stops processing if it encounters a semicolon.
	 * @param st The {@code StringTokenizer} containing the input tokens.
	 * @return An array of strings representing the extracted opCodes, or {@code null} if none were found.
	 */
	private String[] getOpCodes(StringTokenizer st)
	{
		Collection<String> opCodes = null;
		while (st.hasMoreTokens())
		{
			final String token = st.nextToken();
			if (";".equals(token))
			{
				break;
			}
			
			if (opCodes == null)
			{
				opCodes = new ArrayList<>();
			}
			
			opCodes.add(token);
		}
		
		if (opCodes == null)
		{
			return null;
		}
		
		return opCodes.toArray(new String[0]);
	}
	
	/**
	 * Checks if the provided array of op codes is valid.<br>
	 * It ensures the {@code String[]} is not {@code null} and has between 1 and 3 elements.<br>
	 * Each code must be a non-negative number within specific size limits based on its position.
	 * @param opCodes The array of op codes to validate.
	 * @return {@code true} if the op codes are valid, otherwise {@code false}.
	 */
	private boolean validateOpCodes(String[] opCodes)
	{
		if ((opCodes == null) || (opCodes.length == 0) || (opCodes.length > 3))
		{
			return false;
		}
		
		for (int i = 0; i < opCodes.length; ++i)
		{
			final String opCode = opCodes[i];
			long opCodeLong;
			try
			{
				opCodeLong = Long.decode(opCode);
			}
			catch (Exception e)
			{
				return i > 0;
			}
			
			if (opCodeLong < 0)
			{
				return false;
			}
			
			if ((i == 0) && (opCodeLong > 255))
			{
				return false;
			}
			else if ((i == 1) && (opCodeLong > 65535))
			{
				return false;
			}
			else if ((i == 2) && (opCodeLong > 4294967295L))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Checks if the provided format string contains only valid characters.<br>
	 * It verifies that each character matches a supported data type.<br>
	 * Returns {@code false} if an invalid character is detected.
	 * @param format The format string to validate.
	 * @return {@code true} if the format is valid, otherwise {@code false}.
	 */
	private boolean validateFormat(String format)
	{
		for (int chIdx = 0; chIdx < format.length(); ++chIdx)
		{
			switch (format.charAt(chIdx))
			{
				case 'b':
				case 'B':
				case 'x':
				case 'X':
				{
					// Array.
					break;
				}
				case 'c':
				case 'C':
				{
					// Byte.
					break;
				}
				case 'h':
				case 'H':
				{
					// Word.
					break;
				}
				case 'd':
				case 'D':
				{
					// Dword.
					break;
				}
				case 'q':
				case 'Q':
				{
					// Qword.
					break;
				}
				case 'f':
				case 'F':
				{
					// Double.
					break;
				}
				case 's':
				case 'S':
				{
					// String.
					break;
				}
				default:
				{
					return false;
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Checks if the provided method name is valid.<br>
	 * It verifies if the input matches specific allowed codes.
	 * @param method The method string to check.
	 * @return {@code true} if the method is valid, otherwise {@code false}.
	 */
	private boolean validateMethod(String method)
	{
		switch (method)
		{
			case "sc":
			case "sb":
			case "cs":
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Displays the correct usage instructions for the forge values command.<br>
	 * It sends a system message to the player.<br>
	 * It then redirects the player back to the main page.
	 * @param activeChar The {@code Player} who executed the command.
	 */
	private void showValuesUsage(Player activeChar)
	{
		activeChar.sendSysMessage("Usage: //forge_values opcode1[ opcode2[ opcode3]] ;[ format]");
		showMainPage(activeChar);
	}
	
	/**
	 * Displays the usage instructions for the forge send command.<br>
	 * It checks if {@code opCodes} is {@code null}.<br>
	 * If it is null, it shows the main page to the player.<br>
	 * Otherwise, it displays the values page.
	 * @param activeChar The {@code Player} who executed the command.
	 * @param opCodes An array of strings representing the operation codes.
	 * @param format A string defining the message format.
	 */
	private void showSendUsage(Player activeChar, String[] opCodes, String format)
	{
		activeChar.sendSysMessage("Usage: //forge_send sc|sb|cs opcode1[;opcode2[;opcode3]][ format value1 ... valueN] ");
		if (opCodes == null)
		{
			showMainPage(activeChar);
		}
		else
		{
			showValuesPage(activeChar, opCodes, format);
		}
	}
	
	/**
	 * Displays the main page for the admin forge tool.<br>
	 * This method sends the {@code pforge/main.htm} file to the player.
	 * @param activeChar The {@code Player} who is currently using the command.
	 */
	private void showMainPage(Player activeChar)
	{
		AdminHtml.showAdminHtml(activeChar, "pforge/main.htm");
	}
	
	/**
	 * Displays the packet forging values page to a player.<br>
	 * This method generates an HTML message based on provided opcodes and format.<br>
	 * It replaces placeholders in the template with specific bypass strings and editor elements.
	 * @param activeChar The {@code Player} who will receive the HTML message.
	 * @param opCodes An array of {@code String} values used to determine the bypass structure.
	 * @param format A {@code String} representing the formatting pattern for the packet.
	 */
	private void showValuesPage(Player activeChar, String[] opCodes, String format)
	{
		String sendBypass = null;
		String valuesHtml = HtmCache.getInstance().getHtm(activeChar, "data/html/admin/pforge/values.htm");
		if (opCodes.length == 3)
		{
			valuesHtml = valuesHtml.replace("%opformat%", "chd");
			sendBypass = opCodes[0] + ";" + opCodes[1] + ";" + opCodes[2];
		}
		else if (opCodes.length == 2)
		{
			valuesHtml = valuesHtml.replace("%opformat%", "ch");
			sendBypass = opCodes[0] + ";" + opCodes[1];
		}
		else
		{
			valuesHtml = valuesHtml.replace("%opformat%", "c");
			sendBypass = opCodes[0];
		}
		
		valuesHtml = valuesHtml.replace("%opcodes%", sendBypass);
		String editorsHtml = "";
		if (format == null)
		{
			valuesHtml = valuesHtml.replace("%format%", "");
			editorsHtml = "";
		}
		else
		{
			valuesHtml = valuesHtml.replace("%format%", format);
			sendBypass += " " + format;
			
			final String editorTemplate = HtmCache.getInstance().getHtm(activeChar, "data/html/admin/pforge/inc/editor.htm");
			if (editorTemplate != null)
			{
				final StringBuilder singleCharSequence = new StringBuilder(1);
				singleCharSequence.append(' ');
				for (int chIdx = 0; chIdx < format.length(); ++chIdx)
				{
					final char ch = format.charAt(chIdx);
					singleCharSequence.setCharAt(0, ch);
					editorsHtml += editorTemplate.replace("%format%", singleCharSequence).replace("%editor_index%", String.valueOf(chIdx));
					sendBypass += " $v" + chIdx;
				}
			}
			else
			{
				editorsHtml = "";
			}
		}
		
		valuesHtml = valuesHtml.replace("%editors%", editorsHtml);
		valuesHtml = valuesHtml.replace("%send_bypass%", sendBypass);
		activeChar.sendPacket(new NpcHtmlMessage(0, 1, valuesHtml));
	}
	
	/**
	 * Processes admin commands for forging and sending custom packets.<br>
	 * It handles the {@code admin_forge}, {@code admin_forge_values}, and {@code admin_forge_send} commands.<br>
	 * The method validates inputs and sends the resulting packets to the player or broadcasts them.
	 * @param command The raw string input from the player.
	 * @param activeChar The {@code Player} object who executed the command.
	 * @return {@code true} if the command was processed successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.equals("admin_forge"))
		{
			showMainPage(activeChar);
		}
		else if (command.startsWith("admin_forge_values "))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command);
				st.nextToken(); // skip command token
				if (!st.hasMoreTokens())
				{
					showValuesUsage(activeChar);
					return false;
				}
				
				final String[] opCodes = getOpCodes(st);
				if (!validateOpCodes(opCodes))
				{
					activeChar.sendSysMessage("Invalid op codes!");
					showValuesUsage(activeChar);
					return false;
				}
				
				String format = null;
				if (st.hasMoreTokens())
				{
					format = st.nextToken();
					if (!validateFormat(format))
					{
						activeChar.sendSysMessage("Format invalid!");
						showValuesUsage(activeChar);
						return false;
					}
				}
				
				showValuesPage(activeChar, opCodes, format);
			}
			catch (Exception e)
			{
				showValuesUsage(activeChar);
				return false;
			}
		}
		else if (command.startsWith("admin_forge_send "))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command);
				st.nextToken(); // skip command token
				if (!st.hasMoreTokens())
				{
					showSendUsage(activeChar, null, null);
					return false;
				}
				
				final String method = st.nextToken();
				if (!validateMethod(method))
				{
					activeChar.sendSysMessage("Invalid method!");
					showSendUsage(activeChar, null, null);
					return false;
				}
				
				final String[] opCodes = st.nextToken().split(";");
				if (!validateOpCodes(opCodes))
				{
					activeChar.sendSysMessage("Invalid op codes!");
					showSendUsage(activeChar, null, null);
					return false;
				}
				
				String format = null;
				if (st.hasMoreTokens())
				{
					format = st.nextToken();
					if (!validateFormat(format))
					{
						activeChar.sendSysMessage("Format invalid!");
						showSendUsage(activeChar, null, null);
						return false;
					}
				}
				
				AdminForgePacket afp = null;
				ByteBuffer bb = null;
				for (int i = 0; i < opCodes.length; ++i)
				{
					char type;
					if (i == 0)
					{
						type = 'c';
					}
					else if (i == 1)
					{
						type = 'h';
					}
					else
					{
						type = 'd';
					}
					
					if (method.equals("sc") || method.equals("sb"))
					{
						if (afp == null)
						{
							afp = new AdminForgePacket();
						}
						
						afp.addPart((byte) type, opCodes[i]);
					}
					else
					{
						if (bb == null)
						{
							bb = ByteBuffer.allocate(32767);
						}
						
						write((byte) type, opCodes[i], bb);
					}
				}
				
				if (format != null)
				{
					for (int i = 0; i < format.length(); ++i)
					{
						if (!st.hasMoreTokens())
						{
							activeChar.sendSysMessage("Not enough values!");
							showSendUsage(activeChar, null, null);
							return false;
						}
						
						WorldObject target = null;
						Boat boat = null;
						String value = st.nextToken();
						switch (value)
						{
							case "$oid":
							{
								value = String.valueOf(activeChar.getObjectId());
								break;
							}
							case "$boid":
							{
								boat = activeChar.getBoat();
								if (boat != null)
								{
									value = String.valueOf(boat.getObjectId());
								}
								else
								{
									value = "0";
								}
								break;
							}
							case "$title":
							{
								value = activeChar.getTitle();
								break;
							}
							case "$name":
							{
								value = activeChar.getName();
								break;
							}
							case "$x":
							{
								value = String.valueOf(activeChar.getX());
								break;
							}
							case "$y":
							{
								value = String.valueOf(activeChar.getY());
								break;
							}
							case "$z":
							{
								value = String.valueOf(activeChar.getZ());
								break;
							}
							case "$heading":
							{
								value = String.valueOf(activeChar.getHeading());
								break;
							}
							case "$toid":
							{
								value = String.valueOf(activeChar.getTargetId());
								break;
							}
							case "$tboid":
							{
								target = activeChar.getTarget();
								if ((target != null) && target.isPlayable())
								{
									boat = target.asPlayer().getBoat();
									if (boat != null)
									{
										value = String.valueOf(boat.getObjectId());
									}
									else
									{
										value = "0";
									}
								}
								break;
							}
							case "$ttitle":
							{
								target = activeChar.getTarget();
								if ((target != null) && target.isCreature())
								{
									value = target.asCreature().getTitle();
								}
								else
								{
									value = "";
								}
								break;
							}
							case "$tname":
							{
								target = activeChar.getTarget();
								if (target != null)
								{
									value = target.getName();
								}
								else
								{
									value = "";
								}
								break;
							}
							case "$tx":
							{
								target = activeChar.getTarget();
								if (target != null)
								{
									value = String.valueOf(target.getX());
								}
								else
								{
									value = "0";
								}
								break;
							}
							case "$ty":
							{
								target = activeChar.getTarget();
								if (target != null)
								{
									value = String.valueOf(target.getY());
								}
								else
								{
									value = "0";
								}
								break;
							}
							case "$tz":
							{
								target = activeChar.getTarget();
								if (target != null)
								{
									value = String.valueOf(target.getZ());
								}
								else
								{
									value = "0";
								}
								break;
							}
							case "$theading":
							{
								target = activeChar.getTarget();
								if (target != null)
								{
									value = String.valueOf(target.getHeading());
								}
								else
								{
									value = "0";
								}
								break;
							}
						}
						
						if (method.equals("sc") || method.equals("sb"))
						{
							if (afp != null)
							{
								afp.addPart((byte) format.charAt(i), value);
							}
						}
						else
						{
							write((byte) format.charAt(i), value, bb);
						}
					}
				}
				
				if (method.equals("sc"))
				{
					activeChar.sendPacket(afp);
				}
				else if (method.equals("sb"))
				{
					activeChar.broadcastPacket(afp);
				}
				else if (bb != null)
				{
					// TODO: Implement me!
					// @formatter:off
					/*bb.flip();
					GameClientPacket p = (GameClientPacket) GameServer.gameServer.getGamePacketHandler().handle(bb, activeChar.getClient());
					if (p != null)
					{
						p.setBuffers(bb, activeChar.getClient(), new NioNetStringBuffer(2000));
						if (p.read())
						{
							ThreadPoolManager.getInstance().executePacket(p);
						}
					}*/
					// @formatter:on
					throw new UnsupportedOperationException("Not implemented yet!");
				}
				
				showValuesPage(activeChar, opCodes, format);
			}
			catch (Exception e)
			{
				showSendUsage(activeChar, null, null);
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Writes a value to the buffer based on a specific type code.<br>
	 * It parses the provided string and puts it into the {@code ByteBuffer}.
	 * @param b The character representing the data type.
	 * @param string The string representation of the value to write.
	 * @param buf The buffer where the data will be stored.
	 * @return {@code true} if the write was successful, otherwise {@code false}.
	 */
	private boolean write(byte b, String string, ByteBuffer buf)
	{
		switch (b)
		{
			case 'C':
			case 'c':
			{
				buf.put(Byte.decode(string));
				return true;
			}
			case 'D':
			case 'd':
			{
				buf.putInt(Integer.decode(string));
				return true;
			}
			case 'H':
			case 'h':
			{
				buf.putShort(Short.decode(string));
				return true;
			}
			case 'F':
			case 'f':
			{
				buf.putDouble(Double.parseDouble(string));
				return true;
			}
			case 'S':
			case 's':
			{
				final int len = string.length();
				for (int i = 0; i < len; i++)
				{
					buf.putChar(string.charAt(i));
				}
				
				buf.putChar('\000');
				return true;
			}
			case 'B':
			case 'b':
			case 'X':
			case 'x':
			{
				buf.put(new BigInteger(string).toByteArray());
				return true;
			}
			case 'Q':
			case 'q':
			{
				buf.putLong(Long.decode(string));
				return true;
			}
			default:
			{
				return false;
			}
		}
	}
	
	/**
	 * Retrieves the list of available admin forge commands.<br>
	 * This method returns all valid command strings for this handler.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
