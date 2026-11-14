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
package org.l2jmobius.gameserver.entity.residences;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.ResidenceFunctionsData;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.network.serverpackets.AgitDecoInfo;

/**
 * This class handles the logic and functionality for player residences.<br>
 * It manages various actions such as decorations, item interactions, and automated tasks.
 * @author UnAfraid
 */
public class ResidenceFunction
{
	private final int _id;
	private final int _level;
	private long _expiration;
	private final AbstractResidence _residense;
	private ScheduledFuture<?> _task;
	
	/**
	 * Creates a new instance of {@code ResidenceFunction}.<br>
	 * This constructor initializes the function with its required properties.<br>
	 * It also triggers the internal initialization process.
	 * @param id The unique identifier for the residence function.
	 * @param level The current level of the residence function.
	 * @param expiration The timestamp when this function will expire.
	 * @param residense The {@code AbstractResidence} object associated with this function.
	 */
	public ResidenceFunction(int id, int level, long expiration, AbstractResidence residense)
	{
		_id = id;
		_level = level;
		_expiration = expiration;
		_residense = residense;
		init();
	}
	
	/**
	 * Creates a new instance of a {@code ResidenceFunction}.<br>
	 * This constructor initializes the function with its ID and level.<br>
	 * It also sets the expiration time based on the template duration.
	 * @param id The unique identifier for the function.
	 * @param level The current level of the function.
	 * @param residense The {@code AbstractResidence} associated with this function.
	 */
	public ResidenceFunction(int id, int level, AbstractResidence residense)
	{
		_id = id;
		_level = level;
		final ResidenceFunctionTemplate template = getTemplate();
		_expiration = Instant.now().toEpochMilli() + template.getDuration().toMillis();
		_residense = residense;
		init();
	}
	
	/**
	 * Initializes the expiration task for this residence function.<br>
	 * It checks if a template exists and if the current time is before the expiration.<br>
	 * If both conditions are met, it schedules the {@code onFunctionExpiration} method.
	 */
	private void init()
	{
		final ResidenceFunctionTemplate template = getTemplate();
		if ((template != null) && (_expiration > System.currentTimeMillis()))
		{
			_task = ThreadPool.schedule(this::onFunctionExpiration, _expiration - System.currentTimeMillis());
		}
	}
	
	/**
	 * Retrieves the unique identifier for this residence function.<br>
	 * This value is assigned when the {@code ResidenceFunction} is created.
	 * @return The {@code int} ID of the residence function.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the current level of this residence function.<br>
	 * This value is used to determine the strength or tier of the function.
	 * @return The integer level of the {@code ResidenceFunction}.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Retrieves the expiration time of this residence function.<br>
	 * The value is returned as a {@code long}.
	 * @return The expiration timestamp.
	 */
	public long getExpiration()
	{
		return _expiration;
	}
	
	/**
	 * Retrieves the unique identifier of the owner.<br>
	 * This value is obtained from the associated {@code AbstractResidence}.
	 * @return The {@code int} ID of the owner.
	 */
	public int getOwnerId()
	{
		return _residense.getOwnerId();
	}
	
	/**
	 * Retrieves the value associated with this residence function.<br>
	 * It fetches the value from the {@code ResidenceFunctionTemplate}.<br>
	 * If no template exists, it returns {@code 0}.
	 * @return The numeric value of the function or {@code 0} if null.
	 */
	public double getValue()
	{
		final ResidenceFunctionTemplate template = getTemplate();
		return template == null ? 0 : template.getValue();
	}
	
	/**
	 * Retrieves the type of this residence function.<br>
	 * It checks the associated {@code ResidenceFunctionTemplate}.<br>
	 * If no template exists, it returns {@code ResidenceFunctionType.NONE}.
	 * @return The {@code ResidenceFunctionType} of the function.
	 */
	public ResidenceFunctionType getType()
	{
		final ResidenceFunctionTemplate template = getTemplate();
		return template == null ? ResidenceFunctionType.NONE : template.getType();
	}
	
	/**
	 * Retrieves the data template for this residence function.<br>
	 * It looks up the configuration based on the current {@code id} and {@code level}.
	 * @return the {@code ResidenceFunctionTemplate} associated with this function.
	 */
	public ResidenceFunctionTemplate getTemplate()
	{
		return ResidenceFunctionsData.getInstance().getFunction(_id, _level);
	}
	
	/**
	 * Handles the logic when a residence function expires.<br>
	 * It attempts to reactivate the function first.<br>
	 * If reactivation fails, it removes the function from the {@code AbstractResidence}.<br>
	 * It then broadcasts an update to all online members of the owner's clan.
	 */
	private void onFunctionExpiration()
	{
		if (!reactivate())
		{
			_residense.removeFunction(this);
			
			final Clan clan = ClanTable.getInstance().getClan(_residense.getOwnerId());
			if (clan != null)
			{
				clan.broadcastToOnlineMembers(new AgitDecoInfo(_residense));
			}
		}
	}
	
	/**
	 * This method attempts to renew the duration of a residence function.<br>
	 * It checks if the owner has enough items in their warehouse to pay the cost.<br>
	 * If successful, it updates the expiration time and restarts the timer.
	 * @return {@code true} if the function was successfully reactivated, otherwise {@code false}.
	 */
	public boolean reactivate()
	{
		final ResidenceFunctionTemplate template = getTemplate();
		if (template == null)
		{
			return false;
		}
		
		final Clan clan = ClanTable.getInstance().getClan(_residense.getOwnerId());
		if (clan == null)
		{
			return false;
		}
		
		final ItemContainer wh = clan.getWarehouse();
		final Item item = wh.getItemByItemId(template.getCost().getId());
		if ((item == null) || (item.getCount() < template.getCost().getCount()))
		{
			return false;
		}
		
		if (wh.destroyItem(ItemProcessType.FEE, item, template.getCost().getCount(), null, this) != null)
		{
			_expiration = System.currentTimeMillis() + (template.getDuration().getSeconds() * 1000);
			init();
		}
		
		return true;
	}
	
	/**
	 * Stops the scheduled expiration of this function.<br>
	 * It cancels the active {@code ScheduledFuture} task if it is still running.<br>
	 * The internal task reference is then set to {@code null}.
	 */
	public void cancelExpiration()
	{
		if ((_task != null) && !_task.isDone())
		{
			_task.cancel(true);
		}
		
		_task = null;
	}
}
