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

import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;

/**
 * Represents a chest entity within the game world.<br>
 * This class handles the logic for interacting with and opening chests.<br>
 * It extends {@code Monster} to manage its presence as an interactable object.
 * @author Julian
 */
public class Chest extends Monster
{
	private volatile boolean _specialDrop;
	
	/**
	 * Creates a new {@code Chest} instance.<br>
	 * This constructor initializes the chest using a provided template.<br>
	 * It sets the default behavior for chests in the game world.
	 * @param template The {@code NpcTemplate} used to define the chest properties.
	 */
	public Chest(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Chest);
		setRandomWalking(false);
		_specialDrop = false;
	}
	
	/**
	 * This method is called when the {@code Chest} entity is first created.<br>
	 * It initializes the internal state of the chest.<br>
	 * It sets the {@code _specialDrop} flag to {@code false}.<br>
	 * It ensures that the chest rewards experience points.
	 */
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_specialDrop = false;
		setMustRewardExpSp(true);
	}
	
	/**
	 * Enables the special drop flag for this chest.<br>
	 * Sets the {@code _specialDrop} field to {@code true}.
	 */
	public synchronized void setSpecialDrop()
	{
		_specialDrop = true;
	}
	
	/**
	 * Handles the logic for dropping items from a chest.<br>
	 * This method determines which {@code NpcData} template to use based on specific ID ranges.<br>
	 * It then calls the parent class method to perform the actual drop.
	 * @param npcTemplate The template of the NPC being interacted with.
	 * @param lastAttacker The creature that last attacked the chest.
	 */
	@Override
	public void doItemDrop(NpcTemplate npcTemplate, Creature lastAttacker)
	{
		int id = getTemplate().getId();
		if (!_specialDrop)
		{
			if ((id >= 18265) && (id <= 18286))
			{
				id += 3536;
			}
			else
			{
				switch (id)
				{
					case 18287:
					case 18288:
					{
						id = 21671;
						break;
					}
					case 18289:
					case 18290:
					{
						id = 21694;
						break;
					}
					case 18291:
					case 18292:
					{
						id = 21717;
						break;
					}
					case 18293:
					case 18294:
					{
						id = 21740;
						break;
					}
					case 18295:
					case 18296:
					{
						id = 21763;
						break;
					}
					case 18297:
					case 18298:
					{
						id = 21786;
						break;
					}
				}
			}
		}
		
		super.doItemDrop(NpcData.getInstance().getTemplate(id), lastAttacker);
	}
	
	/**
	 * Checks if the movement of this chest is currently disabled.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if movement is disabled, {@code false} otherwise.
	 */
	@Override
	public boolean isMovementDisabled()
	{
		return true;
	}
	
	/**
	 * Checks if the chest plays random animations.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the chest has a random animation, otherwise {@code false}.
	 */
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
}
