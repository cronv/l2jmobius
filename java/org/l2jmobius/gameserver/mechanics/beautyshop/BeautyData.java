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
package org.l2jmobius.gameserver.mechanics.beautyshop;

import java.util.HashMap;
import java.util.Map;

/**
 * This class serves as a data container for beauty shop items.<br>
 * It stores and manages the configuration details required for the beauty shop system.
 * @author Sdw
 */
public class BeautyData
{
	private final Map<Integer, BeautyItem> _hairList = new HashMap<>();
	private final Map<Integer, BeautyItem> _faceList = new HashMap<>();
	
	/**
	 * Adds a new hair item to the internal list.<br>
	 * This method stores the {@code BeautyItem} using its unique ID as the key.
	 * @param hair The {@code BeautyItem} object to be added.
	 */
	public void addHair(BeautyItem hair)
	{
		_hairList.put(hair.getId(), hair);
	}
	
	/**
	 * Adds a new {@code BeautyItem} to the internal face list.<br>
	 * This method stores the item using its unique ID as the key.
	 * @param face The {@code BeautyItem} object to be added.
	 */
	public void addFace(BeautyItem face)
	{
		_faceList.put(face.getId(), face);
	}
	
	/**
	 * Retrieves the list of all hair items.<br>
	 * The data is stored in a {@code Map} where the key is an {@code Integer}.
	 * @return A {@code Map} containing the hair items.
	 */
	public Map<Integer, BeautyItem> getHairList()
	{
		return _hairList;
	}
	
	/**
	 * Retrieves the list of all face items.<br>
	 * The map uses an {@code Integer} ID as the key.
	 * @return A {@code Map} containing the face data.
	 */
	public Map<Integer, BeautyItem> getFaceList()
	{
		return _faceList;
	}
}
