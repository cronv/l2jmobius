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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.MacroType;
import org.l2jmobius.gameserver.entity.actor.enums.player.MacroUpdateType;
import org.l2jmobius.gameserver.entity.actor.enums.player.ShortcutType;
import org.l2jmobius.gameserver.network.serverpackets.SendMacroList;

/**
 * Manages the collection of macros associated with a {@code Player}.<br>
 * It handles loading, saving, and retrieving macro data from the database.<br>
 * This class provides the necessary structure to synchronize player macros with the game client.
 */
public class MacroList
{
	private static final Logger LOGGER = Logger.getLogger(MacroList.class.getName());
	
	private final Player _owner;
	private int _macroId;
	private final Map<Integer, Macro> _macroses = Collections.synchronizedMap(new LinkedHashMap<>());
	
	/**
	 * Creates a new {@code MacroList} for a specific player.<br>
	 * This constructor initializes the list and sets the starting macro ID to {@code 1000}.
	 * @param owner The {@code Player} who owns this macro list.
	 */
	public MacroList(Player owner)
	{
		_owner = owner;
		_macroId = 1000;
	}
	
	/**
	 * Retrieves all macros associated with the player.<br>
	 * This method returns a map of IDs to {@code Macro} objects.
	 * @return A {@code Map} containing all registered macros.
	 */
	public Map<Integer, Macro> getAllMacroses()
	{
		return _macroses;
	}
	
	/**
	 * Adds a new {@code Macro} to the list or updates an existing one.<br>
	 * If the {@code Macro} has an ID of 0, it is assigned a new unique identifier.<br>
	 * The method also synchronizes the change with the database and notifies the owner.
	 * @param macro The {@code Macro} object to register or update.
	 */
	public void registerMacro(Macro macro)
	{
		MacroUpdateType updateType = MacroUpdateType.ADD;
		if (macro.getId() == 0)
		{
			macro.setId(_macroId++);
			while (_macroses.containsKey(macro.getId()))
			{
				macro.setId(_macroId++);
			}
			
			_macroses.put(macro.getId(), macro);
			registerMacroInDb(macro);
		}
		else
		{
			updateType = MacroUpdateType.MODIFY;
			final Macro old = _macroses.put(macro.getId(), macro);
			if (old != null)
			{
				deleteMacroFromDb(old);
			}
			
			registerMacroInDb(macro);
		}
		
		_owner.sendPacket(new SendMacroList(1, macro, updateType));
	}
	
	/**
	 * Removes a macro from the player's list.<br>
	 * This method deletes the macro from memory and the database.<br>
	 * It also removes any associated shortcuts for that macro.<br>
	 * Finally, it sends a notification to the player.
	 * @param id The unique identifier of the macro to delete.
	 */
	public void deleteMacro(int id)
	{
		final Macro removed = _macroses.remove(id);
		if (removed != null)
		{
			deleteMacroFromDb(removed);
		}
		
		for (Shortcut sc : _owner.getAllShortcuts())
		{
			if ((sc.getId() == id) && (sc.getType() == ShortcutType.MACRO))
			{
				_owner.deleteShortcut(sc.getSlot(), sc.getPage());
			}
		}
		
		_owner.sendPacket(new SendMacroList(0, removed, MacroUpdateType.DELETE));
	}
	
	/**
	 * Sends the full list of macros to the player.<br>
	 * This method iterates through all registered {@code Macro} objects.<br>
	 * It sends a {@code SendMacroList} packet for each macro found.<br>
	 * If no macros exist, it sends an empty list update.
	 */
	public void sendAllMacros()
	{
		final Collection<Macro> allMacros = _macroses.values();
		final int count = allMacros.size();
		
		synchronized (_macroses)
		{
			if (allMacros.isEmpty())
			{
				_owner.sendPacket(new SendMacroList(0, null, MacroUpdateType.LIST));
			}
			else
			{
				for (Macro m : allMacros)
				{
					_owner.sendPacket(new SendMacroList(count, m, MacroUpdateType.LIST));
				}
			}
		}
	}
	
	/**
	 * Saves a {@code Macro} object into the database.<br>
	 * This method inserts the macro data into the character macros table.<br>
	 * It handles formatting the commands into a semicolon-separated string.
	 * @param macro The {@code Macro} object to be saved.
	 */
	private void registerMacroInDb(Macro macro)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO character_macroses (charId,id,icon,name,descr,acronym,commands) values(?,?,?,?,?,?,?)"))
		{
			ps.setInt(1, _owner.getObjectId());
			ps.setInt(2, macro.getId());
			ps.setInt(3, macro.getIcon());
			ps.setString(4, macro.getName());
			ps.setString(5, macro.getDescr());
			ps.setString(6, macro.getAcronym());
			final StringBuilder sb = new StringBuilder(1255);
			for (MacroCmd cmd : macro.getCommands())
			{
				sb.append(cmd.getType().ordinal() + "," + cmd.getD1() + "," + cmd.getD2());
				if ((cmd.getCmd() != null) && !cmd.getCmd().isEmpty())
				{
					sb.append("," + cmd.getCmd());
				}
				sb.append(';');
			}
			
			if (sb.length() > 1000)
			{
				sb.setLength(1000);
			}
			
			ps.setString(7, sb.toString());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "could not store macro:", e);
		}
	}
	
	/**
	 * Removes a specific macro from the database.<br>
	 * This method uses the {@code _owner} ID and the macro's unique ID to target the record.<br>
	 * It logs a warning if an error occurs during the deletion process.
	 * @param macro The {@code Macro} object that needs to be removed from the database.
	 */
	private void deleteMacroFromDb(Macro macro)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM character_macroses WHERE charId=? AND id=?"))
		{
			ps.setInt(1, _owner.getObjectId());
			ps.setInt(2, macro.getId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "could not delete macro:", e);
		}
	}
	
	/**
	 * Loads all macros from the database for the owner.<br>
	 * Clears the current macro list before loading new data.<br>
	 * Returns {@code true} if the operation succeeds.<br>
	 * Returns {@code false} if an error occurs during the database query.
	 * @return The success status of the restoration process.
	 */
	public boolean restoreMe()
	{
		_macroses.clear();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT charId, id, icon, name, descr, acronym, commands FROM character_macroses WHERE charId=?"))
		{
			ps.setInt(1, _owner.getObjectId());
			try (ResultSet rset = ps.executeQuery())
			{
				while (rset.next())
				{
					final int id = rset.getInt("id");
					final int icon = rset.getInt("icon");
					final String name = rset.getString("name");
					final String descr = rset.getString("descr");
					final String acronym = rset.getString("acronym");
					final List<MacroCmd> commands = new ArrayList<>();
					final StringTokenizer st1 = new StringTokenizer(rset.getString("commands"), ";");
					while (st1.hasMoreTokens())
					{
						final StringTokenizer st = new StringTokenizer(st1.nextToken(), ",");
						if (st.countTokens() < 3)
						{
							continue;
						}
						
						final MacroType type = MacroType.values()[Integer.parseInt(st.nextToken())];
						final int d1 = Integer.parseInt(st.nextToken());
						final int d2 = Integer.parseInt(st.nextToken());
						String cmd = "";
						if (st.hasMoreTokens())
						{
							cmd = st.nextToken();
						}
						
						commands.add(new MacroCmd(commands.size(), type, d1, d2, cmd));
					}
					
					_macroses.put(id, new Macro(id, icon, name, descr, acronym, commands));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "could not store shortcuts:", e);
			return false;
		}
		
		return true;
	}
}
