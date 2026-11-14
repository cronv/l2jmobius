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
package ai.others.Servitors;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for {@code ClanFlag} objects in the game world.<br>
 * It manages interactions between players, NPCs, and clans regarding flag ownership.<br>
 * It extends the {@code Script} class to provide specific behaviors for these entities.
 * @author Mobius
 */
public class ClanFlag extends Script
{
	// NPC
	private static final int CLAN_FLAG = 19269;
	
	// Skills
	private static final SkillHolder BUFF = new SkillHolder(15095, 1);
	private static final SkillHolder DEBUFF = new SkillHolder(15096, 1);
	
	/**
	 * Private constructor for the {@code ClanFlag} class.<br>
	 * This constructor registers the {@code CLAN_FLAG} spawn ID.<br>
	 * It prevents other classes from creating new instances of this script.
	 */
	private ClanFlag()
	{
		addSpawnId(CLAN_FLAG);
	}
	
	/**
	 * This method is called when the {@code Npc} is first created.<br>
	 * It initializes the timers for the script.
	 * @param npc The {@code Npc} instance that was spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		getTimers().addTimer("END_OF_LIFE", 1800000, npc, null);
		getTimers().addTimer("SKILL_CAST", 1000, npc, null);
	}
	
	/**
	 * Handles periodic events for the {@code ClanFlag} script.<br>
	 * This method processes logic based on specific timer event names.<br>
	 * It manages buff and debuff applications between clans.
	 * @param event The name of the timer event to process.
	 * @param params Additional parameters associated with the event.
	 * @param npc The {@code Npc} instance triggering the event.
	 * @param player The {@code Player} instance involved in the event.
	 */
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		switch (event)
		{
			case "SKILL_CAST":
			{
				if (npc.getSummoner() != null)
				{
					final Clan summonerClan = npc.getSummoner().getClan();
					if (summonerClan != null)
					{
						World.forEachVisibleObjectInRange(npc, Player.class, 2000, target ->
						{
							if ((target != null) && !target.isDead() && GeoEngine.getInstance().canSeeTarget(npc, target))
							{
								final Clan targetClan = target.getClan();
								if (targetClan != null)
								{
									if (targetClan == summonerClan)
									{
										BUFF.getSkill().applyEffects(npc, target);
									}
									else if (targetClan.isAtWarWith(summonerClan))
									{
										DEBUFF.getSkill().applyEffects(npc, target);
									}
								}
							}
						});
						getTimers().addTimer("SKILL_CAST", 3000, npc, null);
						return;
					}
				}
				
				getTimers().addTimer("END_OF_LIFE", 100, npc, null);
				break;
			}
			case "END_OF_LIFE":
			{
				getTimers().cancelTimer("SKILL_CAST", npc, null);
				npc.deleteMe();
				break;
			}
		}
	}
	
	/**
	 * This is the main entry point for the {@code ClanFlag} script.<br>
	 * It initializes a new instance of the {@code ClanFlag} class.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new ClanFlag();
	}
}
