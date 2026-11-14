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

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ExAlterSkillRequest;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the {@code AirBind} skill effect.<br>
 * This class manages the mechanics for binding a target in mid-air.<br>
 * It ensures that affected creatures are restricted from moving while suspended.
 * @author Mobius
 */
public class AirBind extends AbstractEffect
{
	private static final Set<Creature> ACTIVE_AIRBINDS = ConcurrentHashMap.newKeySet();
	private static final Map<PlayerClass, Integer> AIRBIND_SKILLS = new EnumMap<>(PlayerClass.class);
	static
	{
		AIRBIND_SKILLS.put(PlayerClass.SIGEL_PHOENIX_KNIGHT, 10249); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.SIGEL_HELL_KNIGHT, 10249); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.SIGEL_EVA_TEMPLAR, 10249); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.SIGEL_SHILLIEN_TEMPLAR, 10249); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.SIGEL_DEATH_KNIGHT, 10249); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.TYRR_DUELIST, 10499); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.TYRR_DREADNOUGHT, 10499); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.TYRR_TITAN, 10499); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.TYRR_GRAND_KHAVATARI, 10499); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.TYRR_MAESTRO, 10499); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.TYRR_DOOMBRINGER, 10499); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.OTHELL_ADVENTURER, 10749); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.OTHELL_WIND_RIDER, 10749); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.OTHELL_GHOST_HUNTER, 10749); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.OTHELL_FORTUNE_SEEKER, 10749); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.YUL_SAGITTARIUS, 10999); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.YUL_MOONLIGHT_SENTINEL, 10999); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.YUL_GHOST_SENTINEL, 10999); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.YUL_TRICKSTER, 10999); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.FEOH_ARCHMAGE, 11249); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.FEOH_SOULTAKER, 11249); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.FEOH_MYSTIC_MUSE, 11249); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.FEOH_STORM_SCREAMER, 11249); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.FEOH_SOUL_HOUND, 11249); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.ISS_HIEROPHANT, 11749); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.ISS_SWORD_MUSE, 11749); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.ISS_SPECTRAL_DANCER, 11749); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.ISS_DOMINATOR, 11749); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.ISS_DOOMCRYER, 11749); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.WYNN_ARCANA_LORD, 11499); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.WYNN_ELEMENTAL_MASTER, 11499); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.WYNN_SPECTRAL_MASTER, 11499); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.AEORE_CARDINAL, 11999); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.AEORE_EVA_SAINT, 11999); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.AEORE_SHILLIEN_SAINT, 11999); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.SHINE_MAKER, 11999); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.WARG_4, 11999); // Heavy Hit
		AIRBIND_SKILLS.put(PlayerClass.SAMURAI, 11999); // Heavy Hit
	}
	
	/**
	 * Creates a new instance of the {@code AirBind} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public AirBind(StatSet params)
	{
	}
	
	/**
	 * Checks if the effect is triggered instantly.<br>
	 * This method returns {@code false} because this effect is not an instant action.
	 * @return {@code true} if the effect is instant, otherwise {@code false}.
	 */
	@Override
	public boolean isInstant()
	{
		return false;
	}
	
	/**
	 * Handles the logic for a continuous instant effect.<br>
	 * This method triggers specific actions when an effect is applied to a target.<br>
	 * It checks for nearby players and sends packets if certain conditions are met.
	 * @param effector The creature who cast the skill.
	 * @param effected The creature who received the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} associated with the action.
	 */
	@Override
	public void continuousInstant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!ACTIVE_AIRBINDS.contains(effected))
		{
			ACTIVE_AIRBINDS.add(effected);
			
			World.forEachVisibleObjectInRange(effected, Player.class, 1200, nearby ->
			{
				if ((nearby.getRace() != Race.ERTHEIA) && (nearby.getTarget() == effected) && nearby.isInCategory(CategoryType.SIXTH_CLASS_GROUP) && !nearby.isAlterSkillActive())
				{
					final int chainSkill = AIRBIND_SKILLS.get(nearby.getPlayerClass()).intValue();
					if (nearby.getSkillRemainingReuseTime(chainSkill) == -1)
					{
						nearby.sendPacket(new ExAlterSkillRequest(nearby, chainSkill, chainSkill, 5));
					}
				}
			});
		}
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes the target from the active air binds list.<br>
	 * If the target is not a {@code Player}, it notifies the AI to think of a new action.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature affected by the skill.
	 * @param skill The {@code Skill} used to apply this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		ACTIVE_AIRBINDS.remove(effected);
		
		if (!effected.isPlayer())
		{
			effected.getAI().notifyActionThink();
		}
	}
}
