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
package org.l2jmobius.gameserver.ai;

/**
 * Represents a queued action that can be triggered by a specific {@code Action} and removed by a specific {@code Intention}.<br>
 * When triggered, it executes a callback provided by the {@code Callback} interface.
 * @author Mobius
 */
public class NextAction
{
	/**
	 * A callback interface that defines the behavior to execute when the next action is triggered.
	 */
	public interface Callback
	{
		void doAction();
	}
	
	private final Action _action;
	private final Intention _intention;
	private final Callback _callback;
	
	/**
	 * Creates a new instance of {@code NextAction}.<br>
	 * This constructor initializes the required action, intention, and callback.
	 * @param action The {@code Action} that triggers this next action.
	 * @param intention The {@code Intention} that can remove this next action.
	 * @param callback The {@code Callback} to execute when triggered.
	 */
	public NextAction(Action action, Intention intention, Callback callback)
	{
		_action = action;
		_intention = intention;
		_callback = callback;
	}
	
	/**
	 * Checks if this action was triggered by a specific {@code Action}.<br>
	 * It compares the provided {@code action} with the internal action.
	 * @param action The {@code Action} to check against.
	 * @return {@code true} if the actions match, otherwise {@code false}.
	 */
	public boolean isTriggeredBy(Action action)
	{
		return _action == action;
	}
	
	/**
	 * Checks if this action was removed by a specific {@code Intention}.<br>
	 * It compares the stored {@code Intention} with the one provided.
	 * @param intention The {@code Intention} to check against.
	 * @return {@code true} if the intentions match, otherwise {@code false}.
	 */
	public boolean isRemovedBy(Intention intention)
	{
		return _intention == intention;
	}
	
	/**
	 * Executes the callback associated with this {@code NextAction}.<br>
	 * This method triggers the logic defined in the {@code Callback} interface.
	 */
	public void doAction()
	{
		_callback.doAction();
	}
}
