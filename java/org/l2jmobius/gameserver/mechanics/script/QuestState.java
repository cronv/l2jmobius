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
package org.l2jmobius.gameserver.mechanics.script;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerQuestAccept;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerQuestComplete;
import org.l2jmobius.gameserver.mechanics.script.newquestdata.QuestCondType;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestAcceptableList;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestNotification;
import org.l2jmobius.gameserver.network.serverpackets.quest.ExQuestUI;

/**
 * Represents the current progress and status of a quest for a player.
 * @author Luis Arias, Mobius
 */
public class QuestState
{
	protected static final Logger LOGGER = Logger.getLogger(QuestState.class.getName());
	
	// Constants
	private static final String COND_VAR = "cond";
	private static final String COUNT_VAR = "count";
	private static final String RESTART_VAR = "restartTime";
	private static final String MEMO_VAR = "memoState";
	private static final String MEMO_EX_VAR = "memoStateEx";
	private static final int RESET_HOUR = 6;
	private static final int RESET_MINUTES = 30;
	
	/** The name of the quest of this QuestState */
	private final String _questName;
	
	/** The "owner" of this QuestState object */
	private final Player _player;
	
	/** The current state of the quest */
	private byte _state;
	
	/** The current condition of the quest */
	private int _cond = 0;
	
	/** Used for simulating Quest onTalk */
	private boolean _simulated = false;
	
	/** A map of key->value pairs containing the quest state variables and their values */
	private Map<String, String> _vars;
	
	/**
	 * boolean flag letting QuestStateManager know to exit quest when cleaning up
	 */
	private boolean _isExitQuestOnCleanUp = false;
	
	/**
	 * Creates a quest state for a player, tracking the given quest and initial progress.
	 * @param quest The quest being tracked.
	 * @param player The owning {@code Player}.
	 * @param state The initial progress value.
	 */
	public QuestState(Quest quest, Player player, byte state)
	{
		_questName = quest.getName();
		_player = player;
		_state = state;
		player.setQuestState(this);
	}
	
	/**
	 * Retrieves the name of the associated quest.
	 * @return The quest name as a {@code String}.
	 */
	public String getQuestName()
	{
		return _questName;
	}
	
	/**
	 * Retrieves the associated quest.
	 * @return The {@code Quest}.
	 */
	public Quest getQuest()
	{
		return ScriptManager.getInstance().getScript(_questName);
	}
	
	/**
	 * Retrieves the player who owns this quest state.
	 * @return The owning {@code Player}.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the current quest state.
	 * @return The current state value as a {@code byte}.
	 */
	public byte getState()
	{
		return _state;
	}
	
	/**
	 * Indicates whether the quest is in the created state.
	 * @return {@code true} if just created, {@code false} otherwise.
	 */
	public boolean isCreated()
	{
		return _state == State.CREATED;
	}
	
	/**
	 * Indicates whether the quest has been started.
	 * @return {@code true} if started, {@code false} otherwise.
	 */
	public boolean isStarted()
	{
		return _state == State.STARTED;
	}
	
	/**
	 * Indicates whether the quest has been completed.
	 * @return {@code true} if completed, {@code false} otherwise.
	 */
	public boolean isCompleted()
	{
		return _state == State.COMPLETED;
	}
	
	/**
	 * Updates the quest state and persists it to the database.
	 * @param state The new state value.
	 */
	public void setState(byte state)
	{
		setState(state, true);
	}
	
	/**
	 * Updates the quest state, optionally persisting the change.
	 * @param state The new state value.
	 * @param saveInDb {@code true} to persist the change.
	 */
	public void setState(byte state, boolean saveInDb)
	{
		if (_simulated)
		{
			return;
		}
		
		if (_state == state)
		{
			return;
		}
		
		final boolean newQuest = isCreated();
		_state = state;
		if (saveInDb)
		{
			if (newQuest)
			{
				Quest.createQuestInDb(this);
			}
			else
			{
				Quest.updateQuestInDb(this);
			}
		}
		
		// _player.sendPacket(new QuestList(_player));
	}
	
	/**
	 * Sets a quest variable in memory without database synchronization, updating the condition for the cond variable.
	 * @param variable The variable name.
	 * @param value The value to assign, treated as empty when {@code null}.
	 */
	public void setInternal(String variable, String value)
	{
		if (_simulated)
		{
			return;
		}
		
		if (_vars == null)
		{
			_vars = new HashMap<>();
		}
		
		if (value == null)
		{
			_vars.put(variable, "");
			return;
		}
		
		if (COND_VAR.equals(variable))
		{
			try
			{
				_cond = Integer.parseInt(value);
			}
			catch (Exception ignored)
			{
			}
		}
		
		_vars.put(variable, value);
	}
	
	/**
	 * Sets a numeric quest variable.
	 * @param variable The variable name.
	 * @param value The integer value to assign.
	 */
	public void set(String variable, int value)
	{
		if (_simulated)
		{
			return;
		}
		
		set(variable, Integer.toString(value));
	}
	
	/**
	 * Sets a quest variable, persists it and updates the condition when the cond variable changes.
	 * @param variable The variable name.
	 * @param value The value to assign, treated as empty when {@code null}.
	 */
	public void set(String variable, String value)
	{
		if (_simulated)
		{
			return;
		}
		
		if (_vars == null)
		{
			_vars = new HashMap<>();
		}
		
		String newValue = value;
		if (newValue == null)
		{
			newValue = "";
		}
		
		final String old = _vars.put(variable, newValue);
		if (old != null)
		{
			Quest.updateQuestVarInDb(this, variable, newValue);
		}
		else
		{
			Quest.createQuestVarInDb(this, variable, newValue);
		}
		
		if (COND_VAR.equals(variable))
		{
			try
			{
				int previousVal = 0;
				try
				{
					previousVal = Integer.parseInt(old);
				}
				catch (Exception ignored)
				{
				}
				
				int newCond = 0;
				try
				{
					newCond = Integer.parseInt(newValue);
				}
				catch (Exception ignored)
				{
				}
				
				_cond = newCond;
				setCond(newCond, previousVal);
				getQuest().sendNpcLogList(getPlayer());
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, _player.getName() + ", " + _questName + " cond [" + newValue + "] is not an integer.  Value stored, but no packet was sent: " + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Updates the quest condition, managing skip flags and synchronizing progress with the client.
	 * @param cond The new condition value.
	 * @param old The previous condition value.
	 */
	private void setCond(int cond, int old)
	{
		if (_simulated)
		{
			return;
		}
		
		if (cond == old)
		{
			return;
		}
		
		int completedStateFlags = 0;
		
		// Conditions 0 and 1 do not need completedStateFlags; furthermore, since the first step cannot be skipped when condition is greater than 1, we can safely assume no steps have been skipped if the condition is 2.
		// Finally, more than 31 steps CANNOT be supported in any way with skipping.
		if ((cond < 3) || (cond > 31))
		{
			unset("__compltdStateFlags");
		}
		else
		{
			completedStateFlags = getInt("__compltdStateFlags");
		}
		
		// case 1: No steps have been skipped so far...
		if (completedStateFlags == 0)
		{
			// Check if this step also doesn't skip anything. If so, no further work is needed also, in this case, no work is needed if the state is being reset to a smaller value in those cases, skip forward to informing the client about the change...
			// ELSE, if we just now skipped for the first time...prepare the flags!!!
			if (cond > (old + 1))
			{
				// Set the most significant bit to 1 (indicates that there exist skipped states).
				// Also, ensure that the least significant bit is an 1 (the first step is never skipped, no matter what the cond says).
				completedStateFlags = 0x80000001;
				
				// Since no flag had been skipped until now, the least significant bits must all be set to 1, up until "old" number of bits.
				completedStateFlags |= (1 << old) - 1;
				
				// Now, just set the bit corresponding to the passed cond to 1 (current step).
				completedStateFlags |= 1 << (cond - 1);
				set("__compltdStateFlags", String.valueOf(completedStateFlags));
			}
		}
		// case 2: There were exist previously skipped steps
		else if (cond < old) // if this is a push back to a previous step, clear all completion flags ahead
		{
			completedStateFlags &= (1 << cond) - 1; // note, this also unsets the flag indicating that there exist skips
			
			// now, check if this resulted in no steps being skipped any more
			if (completedStateFlags == ((1 << cond) - 1))
			{
				unset("__compltdStateFlags");
			}
			else
			{
				// set the most significant bit back to 1 again, to correctly indicate that this skips states.
				// Also, ensure that the least significant bit is an 1 (the first step is never skipped, no matter what the cond says).
				completedStateFlags |= 0x80000001;
				set("__compltdStateFlags", String.valueOf(completedStateFlags));
			}
		}
		
		// If this moves forward, it changes nothing on previously skipped steps.
		// Just mark this state and we are done.
		else
		{
			completedStateFlags |= 1 << (cond - 1);
			set("__compltdStateFlags", String.valueOf(completedStateFlags));
		}
		
		// Send a packet to the client to inform it of the quest progress (step change).
		_player.sendPacket(new ExQuestUI(_player));
		_player.sendPacket(new ExQuestNotification(this));
	}
	
	/**
	 * Removes a quest variable from memory and the database, resetting the condition for the cond variable.
	 * @param variable The variable name.
	 */
	public void unset(String variable)
	{
		if (_simulated)
		{
			return;
		}
		
		if (_vars == null)
		{
			return;
		}
		
		final String old = _vars.remove(variable);
		if (old != null)
		{
			if (COND_VAR.equals(variable))
			{
				_cond = 0;
			}
			
			Quest.deleteQuestVarInDb(this, variable);
		}
	}
	
	/**
	 * Retrieves the value of a quest variable.
	 * @param variable The variable name.
	 * @return The variable value, or {@code null} if absent.
	 */
	public String get(String variable)
	{
		if (_vars == null)
		{
			return null;
		}
		
		return _vars.get(variable);
	}
	
	/**
	 * Retrieves the integer value of a quest variable.
	 * @param variable The variable name.
	 * @return The parsed integer, or {@code 0} if missing or invalid.
	 */
	public int getInt(String variable)
	{
		if (_vars == null)
		{
			return 0;
		}
		
		final String varStr = _vars.get(variable);
		if ((varStr == null) || varStr.isEmpty())
		{
			return 0;
		}
		
		int varInt = 0;
		try
		{
			varInt = Integer.parseInt(varStr);
		}
		catch (NumberFormatException nfe)
		{
			LOGGER.log(Level.INFO, "Quest " + _questName + ", method getInt(" + variable + "), tried to parse a non-integer value (" + varStr + "). Char Id: " + _player.getObjectId(), nfe);
		}
		
		return varInt;
	}
	
	/**
	 * Indicates whether the current condition matches the given value.
	 * @param condition The value to compare.
	 * @return {@code true} if they match, {@code false} otherwise.
	 */
	public boolean isCond(int condition)
	{
		return _cond == condition;
	}
	
	/**
	 * Indicates whether the current condition matches the given type.
	 * @param condition The condition type to compare.
	 * @return {@code true} if they match, {@code false} otherwise.
	 */
	public boolean isCond(QuestCondType condition)
	{
		return _cond == condition.getId();
	}
	
	/**
	 * Sets the quest condition, playing a sound when set to done.
	 * @param condition The new condition value.
	 */
	public void setCond(int condition)
	{
		if (_simulated)
		{
			return;
		}
		
		if (isStarted())
		{
			set(COND_VAR, Integer.toString(condition));
			if (condition == QuestCondType.DONE.getId())
			{
				_player.sendPacket(QuestSound.ITEMSOUND_QUEST_MIDDLE.getPacket());
			}
		}
	}
	
	/**
	 * Sets the quest condition from a condition type.
	 * @param condition The condition type to set.
	 */
	public void setCond(QuestCondType condition)
	{
		setCond(condition.getId());
	}
	
	/**
	 * Retrieves the current condition value.
	 * @return The condition value, or {@code 0} if not started.
	 */
	public int getCond()
	{
		if (isStarted())
		{
			return _cond;
		}
		
		return 0;
	}
	
	/**
	 * Retrieves the current condition bit set, decoding skipped-step flags.
	 * @return The condition value, or {@code 0} if not started.
	 */
	public int getCondBitSet()
	{
		if (isStarted())
		{
			int val = getInt(COND_VAR);
			if ((val & 0x80000000) != 0)
			{
				val &= 0x7fffffff;
				for (int i = 1; i < 32; i++)
				{
					val = (val >> 1);
					if (val == 0)
					{
						val = i;
						break;
					}
				}
			}
			
			return val;
		}
		
		return 0;
	}
	
	/**
	 * Indicates whether a quest variable has been set.
	 * @param variable The variable name.
	 * @return {@code true} if it exists, {@code false} otherwise.
	 */
	public boolean isSet(String variable)
	{
		return get(variable) != null;
	}
	
	/**
	 * Updates the quest condition, optionally playing the middle quest sound.
	 * @param value The new condition value.
	 * @param playQuestMiddle {@code true} to play the middle quest sound.
	 */
	public void setCond(int value, boolean playQuestMiddle)
	{
		if (_simulated)
		{
			return;
		}
		
		if (!isStarted())
		{
			return;
		}
		
		set(COND_VAR, String.valueOf(value));
		
		if (playQuestMiddle)
		{
			_player.sendPacket(QuestSound.ITEMSOUND_QUEST_MIDDLE.getPacket());
		}
	}
	
	/**
	 * Sets the memo state for this quest.
	 * @param value The new memo state value.
	 */
	public void setMemoState(int value)
	{
		if (_simulated)
		{
			return;
		}
		
		set(MEMO_VAR, String.valueOf(value));
	}
	
	/**
	 * Retrieves the memo state of this quest.
	 * @return The memo state value, or {@code 0} if not started.
	 */
	public int getMemoState()
	{
		if (isStarted())
		{
			return getInt(MEMO_VAR);
		}
		
		return 0;
	}
	
	/**
	 * Updates the quest count and notifies the player.
	 * @param value The new count value.
	 */
	public void setCount(int value)
	{
		if (_simulated)
		{
			return;
		}
		
		set(COUNT_VAR, String.valueOf(value));
		
		_player.sendPacket(QuestSound.ITEMSOUND_QUEST_ITEMGET.getPacket());
		_player.sendPacket(new ExQuestUI(_player));
		_player.sendPacket(new ExQuestNotification(this));
	}
	
	/**
	 * Retrieves the current quest count.
	 * @return The count value, or {@code 0} if not started.
	 */
	public int getCount()
	{
		if (isStarted())
		{
			return getInt(COUNT_VAR);
		}
		
		return 0;
	}
	
	/**
	 * Indicates whether the memo state matches the given value.
	 * @param memoState The value to compare.
	 * @return {@code true} if they match, {@code false} otherwise.
	 */
	public boolean isMemoState(int memoState)
	{
		return getInt(MEMO_VAR) == memoState;
	}
	
	/**
	 * Retrieves the extended memo state for the given slot.
	 * @param slot The memo state slot index.
	 * @return The slot value, or {@code 0} if not started.
	 */
	public int getMemoStateEx(int slot)
	{
		if (isStarted())
		{
			return getInt(MEMO_EX_VAR + slot);
		}
		
		return 0;
	}
	
	/**
	 * Sets the extended memo state for the given slot.
	 * @param slot The memo state slot index.
	 * @param value The value to store.
	 */
	public void setMemoStateEx(int slot, int value)
	{
		if (_simulated)
		{
			return;
		}
		
		set(MEMO_EX_VAR + slot, String.valueOf(value));
	}
	
	/**
	 * Indicates whether the extended memo state at a slot matches the given value.
	 * @param slot The memo state slot index.
	 * @param memoStateEx The value to compare.
	 * @return {@code true} if they match, {@code false} otherwise.
	 */
	public boolean isMemoStateEx(int slot, int memoStateEx)
	{
		return (getMemoStateEx(slot) == memoStateEx);
	}
	
	/**
	 * Indicates whether the quest should be removed during cleanup.
	 * @return {@code true} if it should be cleaned up, {@code false} otherwise.
	 */
	public boolean isExitQuestOnCleanUp()
	{
		return _isExitQuestOnCleanUp;
	}
	
	/**
	 * Sets whether the quest should be removed during cleanup.
	 * @param isExitQuestOnCleanUp {@code true} to exit the quest on cleanup.
	 */
	public void setIsExitQuestOnCleanUp(boolean isExitQuestOnCleanUp)
	{
		if (_simulated)
		{
			return;
		}
		
		_isExitQuestOnCleanUp = isExitQuestOnCleanUp;
	}
	
	/**
	 * Starts the quest for the player, updating state and sending acceptance packets for non-custom quests.
	 */
	public void startQuest()
	{
		if (_simulated)
		{
			return;
		}
		
		if (isCreated() && !getQuest().isCustomQuest())
		{
			set(COND_VAR, "1");
			set(COUNT_VAR, "0");
			setState(State.STARTED);
			_player.sendPacket(QuestSound.ITEMSOUND_QUEST_ACCEPT.getPacket());
			// _player.sendPacket(new ExQuestUI(_player));
			// _player.sendPacket(new ExQuestNotification(this));
			_player.sendPacket(new ExQuestAcceptableList(_player));
			
			if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_QUEST_ACCEPT, _player, Containers.Players()))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerQuestAccept(_player, getQuest().getId()), _player, Containers.Players());
			}
		}
	}
	
	/**
	 * Finishes the quest by type, scheduling a restart for daily quests and notifying listeners.
	 * @param type The quest type being finished.
	 */
	public void exitQuest(QuestFinishType type)
	{
		if (_simulated)
		{
			return;
		}
		
		switch (type)
		{
			case DAILY:
			{
				exitQuest(false);
				setRestartTime();
				break;
			}
			
			// case ONE_TIME:
			// case REPEATABLE:
			default:
			{
				exitQuest(type == QuestFinishType.REPEATABLE);
				break;
			}
		}
		
		// Notify to scripts
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_QUEST_COMPLETE, _player))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerQuestComplete(_player, getQuest().getId(), type), _player);
		}
	}
	
	/**
	 * Finishes the quest by type, optionally playing the completion sound.
	 * @param type The quest type being finished.
	 * @param playExitQuest {@code true} to play the completion sound.
	 */
	public void exitQuest(QuestFinishType type, boolean playExitQuest)
	{
		if (_simulated)
		{
			return;
		}
		
		exitQuest(type);
		
		if (playExitQuest)
		{
			_player.sendPacket(QuestSound.ITEMSOUND_QUEST_FINISH.getPacket());
		}
	}
	
	/**
	 * Finishes the quest, removing death notifications and registered items and deleting or completing the state.
	 * @param repeatable {@code true} to delete the state, {@code false} to mark it completed.
	 */
	private void exitQuest(boolean repeatable)
	{
		if (_simulated)
		{
			return;
		}
		
		_player.removeNotifyQuestOfDeath(this);
		
		if (!isStarted())
		{
			return;
		}
		
		// Clean registered quest items.
		getQuest().removeRegisteredQuestItems(_player);
		
		Quest.deleteQuestInDb(this, repeatable);
		if (repeatable)
		{
			_player.delQuestState(_questName);
		}
		else
		{
			setState(State.COMPLETED);
		}
		
		_player.sendPacket(new ExQuestNotification(this));
		_player.sendPacket(new ExQuestUI(_player));
		
		_vars = null;
	}
	
	/**
	 * Finishes the quest, optionally playing the completion sound, and notifies listeners.
	 * @param repeatable {@code true} for a repeatable quest, {@code false} for a one-time quest.
	 * @param playExitQuest {@code true} to play the completion sound.
	 */
	public void exitQuest(boolean repeatable, boolean playExitQuest)
	{
		if (_simulated)
		{
			return;
		}
		
		exitQuest(repeatable);
		
		if (playExitQuest)
		{
			_player.sendPacket(QuestSound.ITEMSOUND_QUEST_FINISH.getPacket());
		}
		
		// Notify to scripts
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_QUEST_COMPLETE, _player))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerQuestComplete(_player, getQuest().getId(), repeatable ? QuestFinishType.REPEATABLE : QuestFinishType.ONE_TIME), _player);
		}
	}
	
	/**
	 * Calculates and stores the next restart time based on the daily reset threshold.
	 */
	public void setRestartTime()
	{
		if (_simulated)
		{
			return;
		}
		
		final Calendar reDo = Calendar.getInstance();
		if (reDo.get(Calendar.HOUR_OF_DAY) >= RESET_HOUR)
		{
			reDo.add(Calendar.DATE, 1);
		}
		
		reDo.set(Calendar.HOUR_OF_DAY, RESET_HOUR);
		reDo.set(Calendar.MINUTE, RESET_MINUTES);
		set(RESTART_VAR, String.valueOf(reDo.getTimeInMillis()));
	}
	
	/**
	 * Indicates whether the quest is available again.
	 * @return {@code true} if the restart time has passed or is unset, {@code false} otherwise.
	 */
	public boolean isNowAvailable()
	{
		final String val = get(RESTART_VAR);
		return (val != null) && (Long.parseLong(val) <= System.currentTimeMillis());
	}
	
	/**
	 * Sets whether the quest state is being simulated.
	 * @param simulated {@code true} to enable simulation mode.
	 */
	public void setSimulated(boolean simulated)
	{
		_simulated = simulated;
	}
}
