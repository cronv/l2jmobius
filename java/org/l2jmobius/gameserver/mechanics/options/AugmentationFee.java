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

/**
 * Fee required to augment an item with a specific mineral.
 * @author Mobius
 */
public class AugmentationFee
{
	private final int _itemId;
	private final long _itemCount;
	private final long _adenaFee;
	private final long _cancelFee;
	
	/**
	 * Creates an augmentation fee entry.
	 * @param itemId The id of the item consumed as payment.
	 * @param itemCount The amount of the payment item consumed.
	 * @param adenaFee The adena cost of applying the augmentation.
	 * @param cancelFee The adena cost of removing the augmentation.
	 */
	public AugmentationFee(int itemId, long itemCount, long adenaFee, long cancelFee)
	{
		_itemId = itemId;
		_itemCount = itemCount;
		_adenaFee = adenaFee;
		_cancelFee = cancelFee;
	}
	
	/**
	 * Returns the id of the item consumed as payment.
	 * @return The payment item id.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Returns the amount of the payment item consumed.
	 * @return The payment item amount.
	 */
	public long getItemCount()
	{
		return _itemCount;
	}
	
	/**
	 * Returns the adena cost of applying the augmentation.
	 * @return The application cost.
	 */
	public long getAdenaFee()
	{
		return _adenaFee;
	}
	
	/**
	 * Returns the adena cost of removing the augmentation.
	 * @return The cancellation cost.
	 */
	public long getCancelFee()
	{
		return _cancelFee;
	}
}
