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
package handlers.skill.effects;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for granting experience points and skill points to a creature.<br>
 * It is an implementation of {@code AbstractEffect} used by skills to reward players.
 * @author QuangNguyen
 */
public class GiveExpAndSp extends AbstractEffect
{
	private final int _xp;
	private final int _sp;
	
	/**
	 * Creates a new effect that grants experience and skill points.<br>
	 * It extracts the required values from the provided {@code StatSet}.
	 * @param params The {@code StatSet} containing the xp and sp values.
	 */
	public GiveExpAndSp(StatSet params)
	{
		_xp = params.getInt("xp", 0);
		_sp = params.getInt("sp", 0);
	}
	
	/**
	 * Checks if the effect happens immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies the instant effect to a target creature.<br>
	 * This method grants experience and skill points to the player who performed the action.<br>
	 * It checks if both creatures are players and that the target is alive.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} involved in the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effector.isPlayer() || !effected.isPlayer() || effected.isAlikeDead())
		{
			return;
		}
		
		if ((_sp != 0) && (_xp != 0))
		{
			final PlayerStat stat = effector.asPlayer().getStat();
			stat.addExp(_xp);
			stat.addSp(_sp);
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_XP_BONUS_S2_AND_S3_SP_BONUS_S4);
			sm.addLong(_xp);
			sm.addLong(0);
			sm.addLong(_sp);
			sm.addLong(0);
			effector.sendPacket(sm);
		}
	}
}
