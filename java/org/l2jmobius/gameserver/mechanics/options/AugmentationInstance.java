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
package org.l2jmobius.gameserver.mechanics.options;

import org.l2jmobius.gameserver.data.xml.OptionData;
import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * Augmentation rolled onto a specific item, holding the two granted options.
 * @author durgus, UnAfraid, Pere, Mobius
 */
public class AugmentationInstance
{
	private final int _mineralId;
	private final Options _option1;
	private final Options _option2;
	private final Options _option3;
	
	/**
	 * Creates a new {@code AugmentationInstance} using specific IDs.<br>
	 * This constructor initializes the mineral and its three associated options.
	 * @param mineralId The unique identifier for the mineral.
	 * @param option1Id The unique identifier for the first option.
	 * @param option2Id The unique identifier for the second option.
	 * @param option3Id The unique identifier for the third option.
	 */
	public AugmentationInstance(int mineralId, int option1Id, int option2Id, int option3Id)
	{
		_mineralId = mineralId;
		_option1 = OptionData.getInstance().getOptions(option1Id);
		_option2 = OptionData.getInstance().getOptions(option2Id);
		_option3 = OptionData.getInstance().getOptions(option3Id);
	}
	
	/**
	 * Creates a new {@code AugmentationInstance} with specific options.<br>
	 * This constructor initializes the mineral and its three associated bonuses.
	 * @param mineralId The unique identifier for the mineral.
	 * @param op1 The first {@code Options} object.
	 * @param op2 The second {@code Options} object.
	 * @param op3 The third {@code Options} object.
	 */
	public AugmentationInstance(int mineralId, Options op1, Options op2, Options op3)
	{
		_mineralId = mineralId;
		_option1 = op1;
		_option2 = op2;
		_option3 = op3;
	}
	
	/**
	 * Retrieves the unique identifier for the mineral.<br>
	 * This value is used to identify which mineral this instance belongs to.
	 * @return The {@code int} ID of the mineral.
	 */
	public int getMineralId()
	{
		return _mineralId;
	}
	
	/**
	 * Retrieves the unique identifier for the first option.<br>
	 * This value is stored within the {@code Options} object.
	 * @return The {@code int} ID of the first option, or 0 if it is {@code null}.
	 */
	public int getOption1Id()
	{
		return _option1 == null ? 0 : _option1.getId();
	}
	
	/**
	 * Retrieves the unique identifier for the second option.<br>
	 * This value is stored within the {@code Options} object.
	 * @return The {@code int} ID of the second option.
	 */
	public int getOption2Id()
	{
		return _option2 == null ? 0 : _option2.getId();
	}
	
	/**
	 * Retrieves the unique identifier for the third option.<br>
	 * Returns {@code 0} if no third option exists.
	 * @return The {@code int} ID of the third option.
	 */
	public int getOption3Id()
	{
		return _option3 == null ? 0 : _option3.getId();
	}
	
	/**
	 * Applies the bonuses from this augmentation to a {@code Player}.<br>
	 * It processes both of the associated options.
	 * @param player The {@code Player} who will receive the bonus.
	 */
	public void applyBonus(Player player)
	{
		if (_option1 != null)
		{
			_option1.apply(player);
		}
		
		if (_option2 != null)
		{
			_option2.apply(player);
		}
		
		if (_option3 != null)
		{
			_option3.apply(player);
		}
	}
	
	/**
	 * Removes all active bonuses from a {@code Player}.<br>
	 * This method clears the effects of both options associated with this instance.
	 * @param player The {@code Player} whose bonuses should be removed.
	 */
	public void removeBonus(Player player)
	{
		if (_option1 != null)
		{
			_option1.remove(player);
		}
		
		if (_option2 != null)
		{
			_option2.remove(player);
		}
		
		if (_option3 != null)
		{
			_option3.remove(player);
		}
	}
}
