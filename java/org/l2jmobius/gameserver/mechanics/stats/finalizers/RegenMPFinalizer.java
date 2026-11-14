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
package org.l2jmobius.gameserver.mechanics.stats.finalizers;

import java.util.OptionalDouble;

import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.ClanHallData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.residences.AbstractResidence;
import org.l2jmobius.gameserver.entity.residences.ResidenceFunction;
import org.l2jmobius.gameserver.entity.residences.ResidenceFunctionType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.type.CastleZone;
import org.l2jmobius.gameserver.entity.zone.type.ClanHallZone;
import org.l2jmobius.gameserver.entity.zone.type.FortZone;
import org.l2jmobius.gameserver.entity.zone.type.MotherTreeZone;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Castle.CastleFunction;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.Fort.FortFunction;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.IStatFunction;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * This class handles the final calculation of Mana Regeneration stats.<br>
 * It applies specific modifiers based on the player's location and active buffs. It ensures that {@code RegenMP} values are correctly adjusted for various game mechanics.
 * @author UnAfraid, Mobius
 */
public class RegenMPFinalizer implements IStatFunction
{
	/**
	 * Calculates the final value for a specific statistic based on the creature's state.<br>
	 * This method applies various multipliers and bonuses from zones, clan properties, and movement states.<br>
	 * It handles different logic for players, pets, and NPCs.
	 * @param creature The {@code Creature} whose stat is being calculated.
	 * @param base An {@code OptionalDouble} containing the base value.
	 * @param stat The {@code Stat} type to calculate.
	 * @return The final calculated double value for the statistic.
	 */
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		
		final boolean playerCreature = creature.isPlayer();
		double baseValue = playerCreature ? creature.asPlayer().getTemplate().getBaseMpRegen(creature.getLevel()) : creature.getTemplate().getBaseMpReg();
		baseValue *= creature.isRaid() ? NpcConfig.RAID_MP_REGEN_MULTIPLIER : PlayerConfig.MP_REGEN_MULTIPLIER;
		if (playerCreature)
		{
			final Player player = creature.asPlayer();
			final double siegeModifier = RegenHPFinalizer.calcSiegeRegenModifier(player);
			if (siegeModifier > 0)
			{
				baseValue *= siegeModifier;
			}
			
			final Clan clan = player.getClan();
			if (player.isInsideZone(ZoneId.CLAN_HALL) && (clan != null))
			{
				final int clanHallIndex = clan.getHideoutId();
				if (clanHallIndex > 0)
				{
					final ClanHallZone zone = ZoneManager.getInstance().getZone(player, ClanHallZone.class);
					final int posChIndex = zone == null ? -1 : zone.getResidenceId();
					if (clanHallIndex == posChIndex)
					{
						final AbstractResidence residense = ClanHallData.getInstance().getClanHallById(clanHallIndex);
						if (residense != null)
						{
							final ResidenceFunction func = residense.getFunction(ResidenceFunctionType.MP_REGEN);
							if (func != null)
							{
								baseValue *= func.getValue();
							}
						}
					}
				}
			}
			
			if (player.isInsideZone(ZoneId.CASTLE) && (clan != null))
			{
				final int castleIndex = clan.getCastleId();
				if (castleIndex > 0)
				{
					final CastleZone zone = ZoneManager.getInstance().getZone(player, CastleZone.class);
					final int posCastleIndex = zone == null ? -1 : zone.getResidenceId();
					if (castleIndex == posCastleIndex)
					{
						final Castle castle = CastleManager.getInstance().getCastleById(castleIndex);
						if (castle != null)
						{
							final CastleFunction func = castle.getCastleFunction(Castle.FUNC_RESTORE_MP);
							if (func != null)
							{
								baseValue *= (func.getLvl() / 100);
							}
						}
					}
				}
			}
			
			if (player.isInsideZone(ZoneId.FORT) && (clan != null))
			{
				final int fortIndex = clan.getFortId();
				if (fortIndex > 0)
				{
					final FortZone zone = ZoneManager.getInstance().getZone(player, FortZone.class);
					final int posFortIndex = zone == null ? -1 : zone.getResidenceId();
					if (fortIndex == posFortIndex)
					{
						final Fort fort = FortManager.getInstance().getFortById(clan.getCastleId());
						if (fort != null)
						{
							final FortFunction func = fort.getFortFunction(Fort.FUNC_RESTORE_MP);
							if (func != null)
							{
								baseValue *= (func.getLevel() / 100);
							}
						}
					}
				}
			}
			
			// Mother Tree effect is calculated at last.
			if (player.isInsideZone(ZoneId.MOTHER_TREE))
			{
				final MotherTreeZone zone = ZoneManager.getInstance().getZone(player, MotherTreeZone.class);
				final int mpBonus = zone == null ? 0 : zone.getMpRegenBonus();
				baseValue += mpBonus;
			}
			
			// Calculate Movement bonus
			if (player.isSitting())
			{
				baseValue *= 1.5; // Sitting
			}
			else if (!player.isMoving())
			{
				baseValue *= 1.1; // Staying
			}
			else if (player.isRunning())
			{
				baseValue *= 0.7; // Running
			}
			
			// Add MEN bonus
			baseValue *= creature.getLevelMod() * BaseStat.MEN.calcBonus(creature);
		}
		else if (creature.isPet())
		{
			baseValue = creature.asPet().getPetLevelData().getPetRegenMP() * NpcConfig.PET_MP_REGEN_MULTIPLIER;
		}
		
		return Stat.defaultValue(creature, stat, baseValue);
	}
}
