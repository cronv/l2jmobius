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
package org.l2jmobius.gameserver.entity.actor.request;

import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * Handles requests related to the {@code PrimeShop} system.<br>
 * This class processes interactions between a {@code Player} and the shop interface.
 * @author UnAfraid
 */
public class PrimeShopRequest extends AbstractRequest
{
	/**
	 * Creates a new {@code PrimeShopRequest} for a specific player.<br>
	 * This constructor initializes the request with the provided {@code Player} object.
	 * @param player The {@code Player} who is making the request.
	 */
	public PrimeShopRequest(Player player)
	{
		super(player);
	}
	
	/**
	 * Checks if the request is currently being used by a specific object.<br>
	 * This method always returns {@code false}.
	 * @param objectId The unique identifier of the object to check.
	 * @return {@code true} if the object is using the request, otherwise {@code false}.
	 */
	@Override
	public boolean isUsing(int objectId)
	{
		return false;
	}
}
