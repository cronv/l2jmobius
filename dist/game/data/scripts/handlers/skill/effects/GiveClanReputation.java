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
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanMember;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for granting clan reputation to a player.<br>
 * It is an implementation of {@code AbstractEffect} used when a skill triggers a reputation gain.
 * @author Mobius
 */
public class GiveClanReputation extends AbstractEffect
{
	private final int _reputation;
	
	/**
	 * Creates a new effect to grant clan reputation.<br>
	 * This constructor initializes the reputation amount from the provided parameters.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public GiveClanReputation(StatSet params)
	{
		_reputation = params.getInt("reputation", 0);
	}
	
	/**
	 * Checks if the effect happens immediately.<br>
	 * This method returns {@code true} for this specific effect.
	 * @return {@code true} if the effect is instant, otherwise {@code false}.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies the reputation gain effect immediately.<br>
	 * This method checks if both creatures are players and have a valid clan.<br>
	 * It updates the clan reputation score and sends a message to all online members.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} involved in the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effector.isPlayer() || !effected.isPlayer() || effected.isAlikeDead())
		{
			return;
		}
		
		final Clan clan = effector.asPlayer().getClan();
		if (clan == null)
		{
			return;
		}
		
		clan.addReputationScore(_reputation);
		
		final SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_REPUTATION_POINTS_S1);
		sm.addInt(_reputation);
		for (ClanMember member : clan.getMembers())
		{
			if (member.isOnline())
			{
				member.getPlayer().sendPacket(sm);
			}
		}
	}
}
