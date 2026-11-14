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
package ai.bosses.Anais;

import java.util.ArrayList;

import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.QuestTimer;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * This class handles the artificial intelligence for the boss {@code Anais}.<br>
 * It defines the behaviors and logic used by this specific NPC during combat.
 * @author nonom
 */
public class Anais extends Script
{
	// NPCs
	private static final int ANAIS = 25701;
	private static final int DIVINE_BURNER = 18915;
	private static final int GRAIL_WARD = 18929;
	
	// Skill
	private static final SkillHolder DIVINE_NOVA = new SkillHolder(6326, 1);
	
	// Instances
	private final ArrayList<Npc> _divineBurners = new ArrayList<>(4);
	private Player _nextTarget = null;
	private Npc _current = null;
	private int _pot = 0;
	
	/**
	 * Private constructor for the {@code Anais} class.<br>
	 * It initializes the required attack, spawn, and kill IDs.<br>
	 * This method is not intended to be called from outside this class.
	 */
	private Anais()
	{
		addAttackId(ANAIS);
		addSpawnId(DIVINE_BURNER);
		addKillId(GRAIL_WARD);
	}
	
	/**
	 * Handles the behavior of a divine burner when it attacks.<br>
	 * It updates the visual effects and movement state of the NPC.<br>
	 * This method also manages the quest timer for specific burners.
	 * @param pot The index of the burner in the list.
	 * @param anais The {@code Npc} instance representing Anais.
	 */
	private void burnerOnAttack(int pot, Npc anais)
	{
		final Npc npc = _divineBurners.get(pot);
		npc.setDisplayEffect(1);
		npc.setWalking();
		if (pot < 4)
		{
			_current = npc;
			final QuestTimer checkAround = getQuestTimer("CHECK", anais, null);
			if (checkAround == null) // || !checkAround.isActive()
			{
				startQuestTimer("CHECK", 3000, anais, null);
			}
		}
		else
		{
			cancelQuestTimer("CHECK", anais, null);
		}
	}
	
	/**
	 * Handles specific AI events for the {@code Anais} boss.<br>
	 * It processes logic for checking targets, attacking guards, and self-destruction.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance currently executing the script.
	 * @param player The {@code Player} involved in the current action.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "CHECK":
			{
				if (!npc.isAttackingNow())
				{
					cancelQuestTimer("CHECK", npc, null);
				}
				
				if ((_current != null) || (_pot < 4))
				{
					final WorldObject target = npc.getTarget();
					_nextTarget = (target != null) && target.isPlayer() ? target.asPlayer() : null;
					
					final Npc b = _divineBurners.get(_pot);
					_pot += 1;
					b.setDisplayEffect(1);
					b.setWalking();
					
					final Npc ward = addSpawn(GRAIL_WARD, new Location(b.getX(), b.getY(), b.getZ()), true, 0);
					ward.asAttackable().addDamageHate(_nextTarget, 0, 999);
					ward.setRunning();
					
					final CreatureAI ai = ward.getAI();
					ai.setIntentionAttack(_nextTarget);
					startQuestTimer("GUARD_ATTACK", 1000, ward, _nextTarget, true);
					startQuestTimer("SUICIDE", 20000, ward, null);
					ai.setIntentionAttack(_nextTarget);
				}
				break;
			}
			case "GUARD_ATTACK":
			{
				if (_nextTarget != null)
				{
					final double distance = npc.calculateDistance2D(_nextTarget);
					if (distance < 100)
					{
						npc.doCast(DIVINE_NOVA.getSkill());
					}
					else if (distance > 2000)
					{
						npc.doDie(null);
						cancelQuestTimer("GUARD_ATTACK", npc, player);
					}
				}
				break;
			}
			case "SUICIDE":
			{
				npc.doCast(DIVINE_NOVA.getSkill());
				cancelQuestTimer("GUARD_ATTACK", npc, _nextTarget);
				if (_current != null)
				{
					_current.setDisplayEffect(2);
					_current.setWalking();
					_current = null;
				}
				
				npc.doDie(null);
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked.<br>
	 * This method determines which action to take based on the current health of the {@code Npc}.<br>
	 * It triggers specific behaviors depending on the internal potion state.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} who performed the attack.
	 * @param damage The amount of damage dealt to the {@code Npc}.
	 * @param isSummon Whether the attack was performed by a summon.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (_pot == 0)
		{
			burnerOnAttack(0, npc);
		}
		else if ((npc.getCurrentHp() <= (npc.getMaxRecoverableHp() * 0.75)) && (_pot == 1))
		{
			burnerOnAttack(1, npc);
		}
		else if ((npc.getCurrentHp() <= (npc.getMaxRecoverableHp() * 0.5)) && (_pot == 2))
		{
			burnerOnAttack(2, npc);
		}
		else if ((npc.getCurrentHp() <= (npc.getMaxRecoverableHp() * 0.25)) && (_pot == 3))
		{
			burnerOnAttack(3, npc);
		}
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It adds the new {@code Npc} to the internal list of divine burners.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		_divineBurners.add(npc);
	}
	
	/**
	 * This method is called when an {@code Npc} is killed.<br>
	 * It handles the logic for effects that occur after a death.<br>
	 * It also resets specific quest timers and updates the current target state.
	 * @param npc The {@code Npc} instance that was killed.
	 * @param killer The {@code Player} who dealt the killing blow.
	 * @param isSummon Indicates if the kill was performed by a summon as {@code true}.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		npc.doCast(DIVINE_NOVA.getSkill());
		cancelQuestTimer("GUARD_ATTACK", npc, _nextTarget);
		cancelQuestTimer("CHECK", npc, null);
		if (_current != null)
		{
			_current.setDisplayEffect(2);
			_current.setWalking();
			_current = null;
		}
	}
	
	/**
	 * This is the entry point for the {@code Anais} class.<br>
	 * It initializes a new instance of the {@code Anais} object.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Anais();
	}
}
