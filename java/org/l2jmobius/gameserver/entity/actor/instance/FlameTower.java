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

import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Tower;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.ZoneManager;

/**
 * Represents the Flame Control Tower instance.<br>
 * This class handles the specific logic and behavior for this tower type within the game world.
 * @author JIV, Mobius
 */
public class FlameTower extends Tower
{
	private static final int FLAME_CONTROL_TOWER = 13004;
	private static final int FLAME_CONTROL_TOWER_WRECK = 13005;
	
	private int _upgradeLevel = 0;
	private List<Integer> _zoneList;
	
	/**
	 * Creates a new {@code FlameTower} instance.<br>
	 * This constructor initializes the tower using the provided template.<br>
	 * It sets the internal instance type to {@code InstanceType.FlameTower}.
	 * @param template The {@code NpcTemplate} used to define the tower's properties.
	 */
	public FlameTower(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.FlameTower);
	}
	
	/**
	 * Handles the logic when this tower is defeated.<br>
	 * It disables all active zones before calling the parent death logic.
	 * @param killer The {@code Creature} that dealt the killing blow.
	 * @return {@code true} if the death was successful, {@code false} otherwise.
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		enableZones(false);
		
		// A destroyed Flame Control Tower leaves its wreck behind.
		if ((getId() == FLAME_CONTROL_TOWER) && getCastle().getSiege().isInProgress())
		{
			getCastle().getSiege().spawnTowerWreck(this, FLAME_CONTROL_TOWER_WRECK);
		}
		
		return super.doDie(killer);
	}
	
	/**
	 * Removes this instance from the game world.<br>
	 * This method disables all active zones before deletion.
	 * @return {@code true} if the deletion was successful, {@code false} otherwise.
	 */
	@Override
	public boolean deleteMe()
	{
		enableZones(false);
		return super.deleteMe();
	}
	
	/**
	 * Enables or disables specific zones associated with this tower.<br>
	 * This method checks if the {@code _zoneList} is not {@code null} and the {@code _upgradeLevel} is not {@code 0}.<br>
	 * It iterates through the zones based on the current upgrade level and updates their status.
	 * @param value The state to set for the zones. Use {@code true} to enable them or {@code false} to disable them.
	 */
	public void enableZones(boolean value)
	{
		if ((_zoneList != null) && (_upgradeLevel != 0))
		{
			final int maxIndex = _upgradeLevel * 2;
			for (int i = 0; i < maxIndex; i++)
			{
				final ZoneType zone = ZoneManager.getInstance().getZoneById(_zoneList.get(i));
				if (zone != null)
				{
					zone.setEnabled(value);
				}
			}
		}
	}
	
	/**
	 * Sets the current upgrade level of the {@code FlameTower}.<br>
	 * This value determines the tower's power and progression.
	 * @param level The new integer level to assign.
	 */
	public void setUpgradeLevel(int level)
	{
		_upgradeLevel = level;
	}
	
	/**
	 * Sets the list of zones for this tower.<br>
	 * This method also enables the zones automatically.
	 * @param list The {@code List<Integer>} containing zone IDs.
	 */
	public void setZoneList(List<Integer> list)
	{
		_zoneList = list;
		enableZones(true);
	}
}
