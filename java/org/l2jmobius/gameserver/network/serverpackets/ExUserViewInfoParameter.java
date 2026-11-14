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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Position;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.item.enums.ItemGrade;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.TraitType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This class represents a packet containing extended user view information.<br>
 * It is used to synchronize specific player data with the client.
 * @author Mobius, Atronic
 */
public class ExUserViewInfoParameter extends ServerPacket
{
	private final Player _player;
	
	/**
	 * Creates a new {@code ExUserViewInfoParameter} object.<br>
	 * This constructor initializes the packet with specific player data.
	 * @param player The {@code Player} instance to associate with this packet.
	 */
	public ExUserViewInfoParameter(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_USER_VIEW_INFO_PARAMETER.writeId(this, buffer);
		
		final PlayerStat stat = _player.getStat();
		int index = 0;
		
		// Number of parameters.
		buffer.writeInt(237);
		
		// Attack percentage.
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.PHYSICAL_ATTACK) - 1) * 10000));
		
		// P. Atk. (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.PHYSICAL_ATTACK));
		
		// M. Atk. (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.MAGIC_ATTACK) - 1) * 10000));
		
		// M. Atk. (num)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.MAGIC_ATTACK));
		
		// Soulshot Damage - Activation
		buffer.writeShort(index++);
		buffer.writeInt((_player.isChargedShot(ShotType.BLESSED_SOULSHOTS) || _player.isChargedShot(ShotType.SOULSHOTS)) ? (10000 + (_player.getActiveRubyJewel() != null ? (int) _player.getActiveRubyJewel().getBonus() * 1000 : 0)) : 0);
		
		// Spiritshot Damage - Activation
		buffer.writeShort(index++);
		buffer.writeInt((_player.isChargedShot(ShotType.BLESSED_SPIRITSHOTS) || _player.isChargedShot(ShotType.SPIRITSHOTS)) ? (10000 + (_player.getActiveSapphireJewel() != null ? (int) _player.getActiveSapphireJewel().getBonus() * 1000 : 0)) : 0);
		
		// Soulshot Damage - Enchanted Weapons
		buffer.writeShort(index++);
		buffer.writeInt((((_player.getActiveWeaponInstance() != null) && _player.getActiveWeaponInstance().isEnchanted()) ? (int) (_player.getActiveWeaponInstance().getEnchantLevel() * (_player.getActiveWeaponItem().getItemGrade() == ItemGrade.S ? 1.6 : _player.getActiveWeaponItem().getItemGrade() == ItemGrade.A ? 1.4 : _player.getActiveWeaponItem().getItemGrade() == ItemGrade.B ? 0.7 : _player.getActiveWeaponItem().getItemGrade().equals(ItemGrade.C) ? 0.4 : _player.getActiveWeaponItem().getItemGrade().equals(ItemGrade.D) ? 0.4 : 0) * 100) : 0));
		
		// Spiritshot Damage - Enchanted Weapons
		buffer.writeShort(index++);
		buffer.writeInt((((_player.getActiveWeaponInstance() != null) && _player.getActiveWeaponInstance().isEnchanted()) ? (int) (_player.getActiveWeaponInstance().getEnchantLevel() * (_player.getActiveWeaponItem().getItemGrade() == ItemGrade.S ? 1.6 : _player.getActiveWeaponItem().getItemGrade() == ItemGrade.A ? 1.4 : _player.getActiveWeaponItem().getItemGrade() == ItemGrade.B ? 0.7 : _player.getActiveWeaponItem().getItemGrade().equals(ItemGrade.C) ? 0.4 : _player.getActiveWeaponItem().getItemGrade().equals(ItemGrade.D) ? 0.4 : 0) * 100) : 0));
		
		// Soulshot Damage - Misc.
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.SHOTS_BONUS) - 1) * 10000));
		
		// Spiritshot Damage - Misc.
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.SHOTS_BONUS) - 1) * 10000));
		
		// Basic PvP Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.PVP_PHYSICAL_ATTACK_DAMAGE, 100) - 100) * 100));
		
		// P. Skill Damage in PvP
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.PVP_PHYSICAL_SKILL_DAMAGE, 100) - 100) * 100));
		
		// M. Skill Damage in PvP
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.PVP_MAGICAL_SKILL_DAMAGE, 100) - 100) * 100));
		
		// Inflicted PvP Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.PVP_PHYSICAL_ATTACK_DAMAGE, 0));
		
		// PvP Damage Decrease Ignore
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Basic PvE Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.PVE_PHYSICAL_ATTACK_DAMAGE, 100) - 100) * 100));
		
		// P. Skill Damage in PvE
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.PVE_PHYSICAL_SKILL_DAMAGE, 100) - 100) * 100));
		
		// M. Skill Damage in PvE
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.PVE_MAGICAL_SKILL_DAMAGE, 100) - 100) * 100));
		
		// PvE Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.PVE_DAMAGE_TAKEN) * 100);
		
		// PvE Damage Decrease Ignore
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.PVE_PHYSICAL_SKILL_DAMAGE) * 100);
		
		// Basic Attack Power
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.AUTO_ATTACK_DAMAGE_BONUS) - 1) * 10000));
		
		// P. Skill Power
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.PHYSICAL_SKILL_POWER, 100) - 100) * 100));
		
		// M. Skill Power
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.MAGICAL_SKILL_POWER, 100) - 100) * 100));
		
		// AoE Skill Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.AREA_OF_EFFECT_DAMAGE_MODIFY) * 100);
		
		// Damage Bonus - Sword
		buffer.writeShort(index++);
		buffer.writeInt(((_player.getActiveWeaponInstance() != null) && (_player.getActiveWeaponInstance().getItemType() == WeaponType.SWORD)) ? stat.getWeaponBonusPAtk() : 0);
		
		// Damage Bonus - Sword Two hand
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Damage Bonus - Magic Sword
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Damage Bonus - Ancient Sword
		buffer.writeShort(index++);
		buffer.writeInt((_player.getActiveWeaponInstance() != null) && (_player.getActiveWeaponInstance().getItemType() == WeaponType.ANCIENTSWORD) ? stat.getWeaponBonusPAtk() : 0);
		
		// Damage Bonus - Dagger
		buffer.writeShort(index++);
		buffer.writeInt((_player.getActiveWeaponInstance() != null) && (_player.getActiveWeaponInstance().getItemType() == WeaponType.DAGGER) ? stat.getWeaponBonusPAtk() : 0);
		
		// Damage Bonus - Rapier
		buffer.writeShort(index++);
		buffer.writeInt((_player.getActiveWeaponInstance() != null) && (_player.getActiveWeaponInstance().getItemType() == WeaponType.RAPIER) ? stat.getWeaponBonusPAtk() : 0);
		
		// Damage Bonus - Blunt Weapon (one hand)
		buffer.writeShort(index++);
		buffer.writeInt((_player.getActiveWeaponInstance() != null) && ((_player.getActiveWeaponInstance().getItemType() == WeaponType.ETC) || (_player.getActiveWeaponInstance().getItemType() == WeaponType.BLUNT) || (_player.getActiveWeaponInstance().getItemType() == WeaponType.DUALBLUNT)) ? stat.getWeaponBonusPAtk() : 0);
		
		// Damage Bonus - Blunt Weapon (two hand)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Damage Bonus - Magic Blunt Weapon (one hand)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Damage Bonus - Magic Blunt Weapon (two hand)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Damage Bonus - Spear
		buffer.writeShort(index++);
		buffer.writeInt((_player.getActiveWeaponInstance() != null) && (_player.getActiveWeaponInstance().getItemType() == WeaponType.POLE) ? stat.getWeaponBonusPAtk() : 0);
		
		// Damage Bonus - Fists
		buffer.writeShort(index++);
		buffer.writeInt((_player.getActiveWeaponInstance() != null) && ((_player.getActiveWeaponInstance().getItemType() == WeaponType.FIST) || (_player.getActiveWeaponInstance().getItemType() == WeaponType.DUALFIST)) ? stat.getWeaponBonusPAtk() : 0);
		
		// Damage Bonus - Dual Swords
		buffer.writeShort(index++);
		buffer.writeInt((_player.getActiveWeaponInstance() != null) && (_player.getActiveWeaponInstance().getItemType() == WeaponType.DUAL) ? stat.getWeaponBonusPAtk() : 0);
		
		// Damage Bonus - Bow
		buffer.writeShort(index++);
		buffer.writeInt((_player.getActiveWeaponInstance() != null) && ((_player.getActiveWeaponInstance().getItemType() == WeaponType.BOW) || (_player.getActiveWeaponInstance().getItemType() == WeaponType.CROSSBOW) || (_player.getActiveWeaponInstance().getItemType() == WeaponType.TWOHANDCROSSBOW)) ? stat.getWeaponBonusPAtk() : 0);
		
		// Damage Bonus - Firearms
		buffer.writeShort(index++);
		
		// ESSENCE buffer.writeInt((_player.getActiveWeaponInstance() != null) && (_player.getActiveWeaponInstance().getItemType() == WeaponType.PISTOLS) ? stat.getWeaponBonusPAtk() : 0);
		buffer.writeInt(0);
		
		// Defense percentage.
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.PHYSICAL_DEFENCE) - 1) * 10000));
		
		// P. Def. (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.PHYSICAL_DEFENCE));
		
		// M. Def. (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.MAGICAL_DEFENCE) - 1) * 10000));
		
		// M. Def. (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.MAGICAL_DEFENCE));
		
		// Soulshot Damage Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) (100 - (stat.getValue(Stat.SOULSHOT_RESISTANCE, 1) * 100)));
		
		// Spiritshot Damage Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) (100 - (stat.getValue(Stat.SPIRITSHOT_RESISTANCE, 1) * 100)));
		
		// Received PvP Damage from Basic Attacks
		buffer.writeShort(index++);
		buffer.writeInt((int) ((100 - stat.getValue(Stat.PVP_PHYSICAL_ATTACK_DEFENCE, 100)) * 100));
		
		// Received PvP P. Skill Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) ((100 - stat.getValue(Stat.PVP_PHYSICAL_SKILL_DEFENCE, 100)) * 100));
		
		// Received PvP M. Skill Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) ((100 - stat.getValue(Stat.PVP_MAGICAL_SKILL_DEFENCE, 100)) * 100));
		
		// Received PvP Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.PVP_DAMAGE_TAKEN) * 100);
		
		// PvP Damage Decrease
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Received PvE Damage from Basic Attacks
		buffer.writeShort(index++);
		buffer.writeInt((int) ((100 - stat.getValue(Stat.PVE_PHYSICAL_ATTACK_DEFENCE, 100)) * 100));
		
		// Received PvE P. Skill Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) ((100 - stat.getValue(Stat.PVE_PHYSICAL_SKILL_DEFENCE, 100)) * 100));
		
		// Received PvE M. Skill Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) ((100 - stat.getValue(Stat.PVE_MAGICAL_SKILL_DEFENCE, 100)) * 100));
		
		// Received PvE Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.PVE_DAMAGE_TAKEN) * 100);
		
		// PvE Damage Decrease
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Received basic damage power
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// P. Skill Power when hit
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.PHYSICAL_SKILL_POWER) * 100);
		
		// M. Skill Power when hit
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.MAGICAL_SKILL_POWER) * 100);
		
		// AOE Damage Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.AREA_OF_EFFECT_DAMAGE_DEFENCE, 100) - 100) * 100));
		
		// Sword Resistance Nr. 60
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.SWORD) * 10000));
		
		// Shield Defense (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Shield Defence (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Shield Defence Rate
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Dagger Weapon Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.DAGGER) * 10000));
		
		// Received Damage when Immobilized
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Blunt Weapon Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.BLUNT) * 10000));
		
		// TODO: Accessory P. Def.
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Accuracy percentage for evasion.
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// P. Accuracy (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Spear Weapon Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.POLE) * 10000));
		
		// Fists Weapon Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.DUALFIST) * 10000));
		
		// Dual Sword Resistance Nr. 72
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.DUAL) * 10000));
		
		// Bow Weapon Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.BOW) * 10000));
		
		// M. Evasion (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Shield Defence (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.SHIELD_DEFENCE) - 1) * 10000));
		
		// Shield Defence (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.SHIELD_DEFENCE));
		
		// Shield Defence Rate
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.SHIELD_DEFENCE_RATE) - 1) * 10000));
		
		// Casting Spd. (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Casting Spd. (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Speed (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Speed (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Received fixed damage
		buffer.writeShort(index++);
		buffer.writeInt((int) ((1 - stat.getMul(Stat.REAL_DAMAGE_RESIST)) * 10000));
		
		// Basic Critical Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// P. Skill Critical Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// P. Accuracy (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.ACCURACY_COMBAT) * 100);
		
		// P. Accuracy (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.ACCURACY_COMBAT));
		
		// M. Accuracy (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.ACCURACY_MAGIC) * 100);
		
		// M. Accuracy (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.ACCURACY_MAGIC));
		
		// TODO: Received basic Critical Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Received P. Skill Critical Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// P. Evasion (%)
		buffer.writeShort(index++);
		buffer.writeInt(((_player.getEvasionRate() * 100) / PlayerConfig.MAX_EVASION));
		
		// P. Evasion (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.EVASION_RATE));
		
		// M. Evasion (%)
		buffer.writeShort(index++);
		buffer.writeInt(((_player.getMagicEvasionRate() * 100) / PlayerConfig.MAX_EVASION));
		
		// M. Evasion (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.MAGIC_EVASION_RATE));
		
		// P. Skill Critical Damage (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// M. Skill Critical Damage (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Received Basic Critical Damage (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Received P. Skill Critical Damage (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Atk. Spd. (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.PHYSICAL_ATTACK_SPEED) - 1) * 10000));
		
		// Atk. Spd. (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.PHYSICAL_ATTACK_SPEED));
		
		// Casting Spd. (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.MAGIC_ATTACK_SPEED) - 1) * 10000));
		
		// Casting Spd. (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.MAGIC_ATTACK_SPEED));
		
		// Speed (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.RUN_SPEED) - 1) * 10000));
		
		// Speed (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.RUN_SPEED));
		
		// Basic Critical Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.CRITICAL_RATE) - 1) * 10000));
		
		// Basic Critical Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.CRITICAL_RATE));
		
		// P. Skill Critical Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.CRITICAL_RATE_SKILL) - 1) * 10000));
		
		// P. Skill Critical Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.CRITICAL_RATE_SKILL));
		
		// M. Skill Critical Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.MAGIC_CRITICAL_RATE) - 1) * 10000));
		
		// M. Skill Critical Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.MAGIC_CRITICAL_RATE));
		
		// Received basic Critical Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.DEFENCE_CRITICAL_RATE, 100) - 100) * 100));
		
		// Received basic Critical Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.DEFENCE_CRITICAL_RATE_ADD));
		
		// Received P. Skill Critical Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.DEFENCE_PHYSICAL_SKILL_CRITICAL_RATE, 100) - 100) * 100));
		
		// Received P. Skill Critical Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.DEFENCE_PHYSICAL_SKILL_CRITICAL_RATE_ADD));
		
		// Received M. Skill Critical Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.DEFENCE_MAGIC_CRITICAL_RATE, 100) - 100) * 100));
		
		// Received M. Skill Critical Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.DEFENCE_MAGIC_CRITICAL_RATE_ADD));
		
		// Basic Critical Damage (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.CRITICAL_DAMAGE, 100) - 100) * 100));
		
		// MP Recovery Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// P. Skill Critical Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.PHYSICAL_SKILL_CRITICAL_DAMAGE, 100) - 100) * 100));
		
		// CP Recovery Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Assist M. Skill Critical Damage.
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.MAGIC_CRITICAL_DAMAGE, 100) - 100) * 100));
		
		// M. Skill Cooldown (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Received C. Damage from Basic Attacks
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.DEFENCE_CRITICAL_DAMAGE, 100) - 100) * 100));
		
		// P. Skill MP Consumption Decrease (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Received P. Skill Critical Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.DEFENCE_PHYSICAL_SKILL_CRITICAL_DAMAGE, 100) - 100) * 100));
		
		// M. Skill MP Consumption Decrease (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Received M. Skill Critical Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.DEFENCE_MAGIC_CRITICAL_DAMAGE, 100) - 100) * 100));
		
		// TODO: XP
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// HP ReCovery Potions' Effect (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.ADDITIONAL_POTION_HP, 100) - 100) * 100));
		
		// HP Recovery Potions' Effect (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.ADDITIONAL_POTION_HP));
		
		// MP Recovery Potions' Effect (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.ADDITIONAL_POTION_MP, 100) - 100) * 100));
		
		// MP Recovery Potions' Effect (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.ADDITIONAL_POTION_MP));
		
		// HP Recovery Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.REGENERATE_HP_RATE) - 1) * 10000));
		
		// HP Recovery Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.REGENERATE_HP_RATE));
		
		// TODO: Skill Mastery Rate (+)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Debuff Resistance (%).
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Debilitation Atk. Rate
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getAttackTrait(TraitType.DEBILITATION) * 100));
		
		// Confusion Atk. Rate
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getAttackTrait(TraitType.CONFUSION) * 100));
		
		// Stupor Atk. Rate
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getAttackTrait(TraitType.STUPOR) * 100));
		
		// Danger Zone Atk. Rate
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getAttackTrait(TraitType.DANGER_ZONE) * 100));
		
		// Pull Atk. Rate
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getAttackTrait(TraitType.PULL) * 100));
		
		// Petrification Atk. Rate
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getAttackTrait(TraitType.PETRIFICATION) * 100));
		
		// MP Recovery Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.REGENERATE_MP_RATE) - 1) * 10000));
		
		// MP Recovery Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.REGENERATE_MP_RATE));
		
		// Debilitation Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.DEBILITATION) * 100));
		
		// Confusion Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.CONFUSION) * 100));
		
		// Stupor Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.STUPOR) * 100));
		
		// Danger Zone Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.DANGER_ZONE) * 100));
		
		// Pull Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.PULL) * 100));
		
		// Petrification Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.PETRIFICATION) * 100));
		
		// Infection Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.INFECTION) * 100));
		
		// Obstruction Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.OBSTRUCTION) * 100));
		
		// CP Recovery Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.REGENERATE_CP_RATE) - 1) * 10000));
		
		// CP Recovery Rate (num.) nr. 153
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.REGENERATE_CP_RATE));
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// P. Skill Cooldown (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getReuseTypeValue(0) - 1) * 10000));
		
		// M. Skill Cooldown (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getReuseTypeValue(1) - 1) * 10000));
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// P. Skill MP Consumption Decrease (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMpConsumeTypeValue(0) - 1) * 10000));
		
		// M. Skill MP Consumption Decrease (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMpConsumeTypeValue(1) - 1) * 10000));
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// P. Skill MP Consumption Decrease (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// M. Skill MP Consumption Decrease (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Debuff Resistance Nr. 173
		buffer.writeShort(index++);
		buffer.writeInt((int) ((1 - stat.getValue(Stat.RESIST_ABNORMAL_DEBUFF, 1)) * 10000));
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Confusion Attack Rate
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getAttackTrait(TraitType.CONFUSION) - 1) * 100));
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Stupor Attack Rate
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getAttackTrait(TraitType.STUPOR) - 1) * 100));
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Pull Attack Rate
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getAttackTrait(TraitType.PULL) - 1) * 100));
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Obstruction Attack Rate
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getAttackTrait(TraitType.OBSTRUCTION) - 1) * 100));
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Confusion Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.CONFUSION) * 100));
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Stupor Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.STUPOR) * 100));
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Pull Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.PULL) * 100));
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Obstruction Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.OBSTRUCTION) * 100));
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// None
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Infection Attack Rate
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getAttackTrait(TraitType.INFECTION) - 1) * 100));
		
		// Infection Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.INFECTION) * 100));
		
		// Weapon P. Atk.
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.WEAPON_BONUS_PHYSICAL_ATTACK, 0));
		
		// Weapon M. Atk.
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.WEAPON_BONUS_MAGIC_ATTACK, 0));
		
		// Backstab Damage Up
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getPositionTypeValue(Stat.REAR_DAMAGE_RATE, Position.BACK) - 1) * 10000));
		
		// Damage to Immobilized Targets
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.IMMOBILE_DAMAGE_BONUS, 100) - 100) * 100));
		
		// Received Damage when Immobilized
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.IMMOBILE_DAMAGE_RESIST) - 1) * 10000));
		
		// Armor P. Def.
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.ARMOR_BONUS_PHYSICAL_DEFENCE, 0));
		
		// Accessory M. Def.
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.ARMOR_BONUS_MAGICAL_DEFENCE, 0));
		
		// Dual Blunt Weapon Resistance Nr. 206
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.DUALBLUNT) * 10000));
		
		// Crossbow Weapon Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.CROSSBOW) * 10000));
		
		// Dual Daggers Weapon Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.DUALDAGGER) * 10000));
		
		// TODO: Perfection Defense
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Perfection Trigger Rate
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.PERFECTION_RATE, 0));
		
		// Perfection Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.PERFECTION_RESIST, 0));
		
		// Perfection Power Nr. 212
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.PERFECTION_POWER, 0));
		
		// Max Hp (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.MAX_HP) - 1) * 10000));
		
		// Max Hp (num)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.MAX_HP));
		
		// Max Mp (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.MAX_MP) - 1) * 10000));
		
		// Max Mp (num)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.MAX_MP));
		
		// Max Cp (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.MAX_CP) - 1) * 10000));
		
		// Max Cp (num)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.MAX_CP));
		
		// Max HP Limit
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAdd(Stat.HP_LIMIT));
		
		// Vitality Bonus
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round(Math.max(stat.getVitalityExpBonus() - (_player.hasPremiumStatus() ? RatesConfig.RATE_VITALITY_EXP_PREMIUM_MULTIPLIER : RatesConfig.RATE_VITALITY_EXP_MULTIPLIER), 0) * 10000)); // Retail shows only what vitality skills add on top of the rate.
		
		// XP
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.BONUS_EXP) * 100));
		
		// SP
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.BONUS_SP) * 100));
		
		// Adena Drop Rate
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.BONUS_DROP_ADENA) - 1) * 10000));
		
		// Adena Drop Rate with Vitality
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.BONUS_DROP_ADENA) - 1) * 10000));
		
		// Received Healing
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.HEAL_EFFECT, 100) - 100) * 100));
		
		// Healing Power
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.HEAL_EFFECT_ADD, 0));
		
		// Skill Mastery Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getMul(Stat.SKILL_MASTERY_RATE) - 1) * 10000));
		
		// Skill Mastery Rate (num)
		buffer.writeShort(index++);
		final int skillMastery = (int) stat.getAdd(Stat.SKILL_MASTERY, -1);
		buffer.writeInt(skillMastery == -1 ? 0 : (int) (BaseStat.values()[skillMastery].calcBonus(_player) * 100));
		
		// Debilitation Attack Rate
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getAttackTrait(TraitType.DEBILITATION) - 1) * 100));
		
		// Danger Zone Attack Rate
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getAttackTrait(TraitType.DANGER_ZONE) - 1) * 100));
		
		// Petrification Attack Rate Nr. 231
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getAttackTrait(TraitType.PETRIFICATION) - 1) * 100));
		
		// Debilitation Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.DEBILITATION) * 100));
		
		// Danger Zone Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.DANGER_ZONE) * 100));
		
		// Petrification Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getDefenceTrait(TraitType.PETRIFICATION) * 100));
		
		// P. Debuff Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.ABNORMAL_RESIST_PHYSICAL, 100) - 100) * 100));
		
		// M. Debuff Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) ((stat.getValue(Stat.ABNORMAL_RESIST_MAGICAL, 100) - 100) * 100));
		
		// Accuracy Bonus
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round(stat.getValue(Stat.ACCURACY_BONUS)));
	}
}
