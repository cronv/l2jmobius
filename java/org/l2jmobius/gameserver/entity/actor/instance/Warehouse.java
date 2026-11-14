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
package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;

/**
 * Represents a storage area where players can deposit and retrieve items.<br>
 * This {@code Warehouse} instance acts as a container for player inventory management.
 */
public class Warehouse extends Folk
{
	/**
	 * Creates a new {@code Warehouse} instance.<br>
	 * This constructor initializes the warehouse using a provided template.<br>
	 * It sets the {@code InstanceType} to {@code Warehouse}.
	 * @param template The {@code NpcTemplate} used to create this warehouse.
	 */
	public Warehouse(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Warehouse);
	}
	
	/**
	 * Checks if the warehouse can be attacked by a specific creature.<br>
	 * This method returns {@code true} if the attacker is a monster.<br>
	 * Otherwise, it uses the default behavior from the parent class.
	 * @param attacker The {@code Creature} attempting to attack.
	 * @return {@code true} if the warehouse can be attacked, {@code false} otherwise.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if (attacker.isMonster())
		{
			return true;
		}
		
		return super.isAutoAttackable(attacker);
	}
	
	/**
	 * Checks if the current entity is a warehouse.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the entity is a warehouse.
	 */
	@Override
	public boolean isWarehouse()
	{
		return true;
	}
	
	/**
	 * Retrieves the file path for a warehouse HTML template.<br>
	 * It constructs a path based on the provided NPC ID and value.
	 * @param npcId The unique identifier of the NPC.
	 * @param value An additional value used to differentiate templates.
	 * @param player The {@code Player} object associated with the request.
	 * @return A {@code String} containing the full path to the .htm file.
	 */
	@Override
	public String getHtmlPath(int npcId, int value, Player player)
	{
		String pom = "";
		if (value == 0)
		{
			pom = Integer.toString(npcId);
		}
		else
		{
			pom = npcId + "-" + value;
		}
		
		return "data/html/warehouse/" + pom + ".htm";
	}
}
