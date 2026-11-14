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
package org.l2jmobius.gameserver.mechanics.script;

import org.l2jmobius.gameserver.network.NpcStringId;

/**
 * Holds an id, count and NPC-string flag for an NPC quest log entry.
 * @author Sdw
 */
public class NpcLogListHolder
{
	private final int _id;
	private final boolean _isNpcString;
	private final int _count;
	
	/**
	 * Creates a holder from an NPC string id, flagging it as an NPC string.
	 * @param npcStringId The {@code NpcStringId} whose id is stored.
	 * @param count The count for this log entry.
	 */
	public NpcLogListHolder(NpcStringId npcStringId, int count)
	{
		_id = npcStringId.getId();
		_isNpcString = true;
		_count = count;
	}
	
	/**
	 * Creates a holder from a raw id with an explicit NPC-string flag.
	 * @param id The log entry id.
	 * @param isNpcString {@code true} if the id refers to an {@code NpcStringId}.
	 * @param count The count for this log entry.
	 */
	public NpcLogListHolder(int id, boolean isNpcString, int count)
	{
		_id = id;
		_isNpcString = isNpcString;
		_count = count;
	}
	
	/**
	 * Retrieves the log entry id.
	 * @return The id as an {@code int}.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Indicates whether this entry refers to an NPC string.
	 * @return {@code true} if an NPC string, {@code false} otherwise.
	 */
	public boolean isNpcString()
	{
		return _isNpcString;
	}
	
	/**
	 * Retrieves the count for this log entry.
	 * @return The count as an {@code int}.
	 */
	public int getCount()
	{
		return _count;
	}
}
