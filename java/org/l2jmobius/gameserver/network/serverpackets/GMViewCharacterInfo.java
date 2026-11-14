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
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.entity.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.entity.actor.holders.player.Recommendations;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.options.AugmentationInstance;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to Game Masters to display detailed information about a character.<br>
 * It contains data such as attributes, appearance, and experience for the target {@code Player}.
 * @author Mobius
 */
public class GMViewCharacterInfo extends ServerPacket
{
	private static final int[] PAPERDOLL_ITEM_SLOTS =
	{
		Inventory.PAPERDOLL_UNDER,
		Inventory.PAPERDOLL_REAR,
		Inventory.PAPERDOLL_LEAR,
		Inventory.PAPERDOLL_NECK,
		Inventory.PAPERDOLL_RFINGER,
		Inventory.PAPERDOLL_LFINGER,
		Inventory.PAPERDOLL_HEAD,
		Inventory.PAPERDOLL_RHAND,
		Inventory.PAPERDOLL_LHAND,
		Inventory.PAPERDOLL_GLOVES,
		Inventory.PAPERDOLL_CHEST,
		Inventory.PAPERDOLL_LEGS,
		Inventory.PAPERDOLL_FEET,
		Inventory.PAPERDOLL_CLOAK,
		Inventory.PAPERDOLL_RHAND,
		Inventory.PAPERDOLL_HAIR,
		Inventory.PAPERDOLL_HAIR2,
		Inventory.PAPERDOLL_RBRACELET,
		Inventory.PAPERDOLL_LBRACELET,
		Inventory.PAPERDOLL_AGATHION1,
		Inventory.PAPERDOLL_AGATHION2,
		Inventory.PAPERDOLL_AGATHION3,
		Inventory.PAPERDOLL_AGATHION4,
		Inventory.PAPERDOLL_AGATHION5,
		Inventory.PAPERDOLL_DECO1,
		Inventory.PAPERDOLL_DECO2,
		Inventory.PAPERDOLL_DECO3,
		Inventory.PAPERDOLL_DECO4,
		Inventory.PAPERDOLL_DECO5,
		Inventory.PAPERDOLL_DECO6,
		Inventory.PAPERDOLL_BELT,
		Inventory.PAPERDOLL_BROOCH,
		Inventory.PAPERDOLL_BROOCH_JEWEL1,
		Inventory.PAPERDOLL_BROOCH_JEWEL2,
		Inventory.PAPERDOLL_BROOCH_JEWEL3,
		Inventory.PAPERDOLL_BROOCH_JEWEL4,
		Inventory.PAPERDOLL_BROOCH_JEWEL5,
		Inventory.PAPERDOLL_BROOCH_JEWEL6,
		Inventory.PAPERDOLL_ARTIFACT_BOOK,
		Inventory.PAPERDOLL_ARTIFACT1,
		Inventory.PAPERDOLL_ARTIFACT2,
		Inventory.PAPERDOLL_ARTIFACT3,
		Inventory.PAPERDOLL_ARTIFACT4,
		Inventory.PAPERDOLL_ARTIFACT5,
		Inventory.PAPERDOLL_ARTIFACT6,
		Inventory.PAPERDOLL_ARTIFACT7,
		Inventory.PAPERDOLL_ARTIFACT8,
		Inventory.PAPERDOLL_ARTIFACT9,
		Inventory.PAPERDOLL_ARTIFACT10,
		Inventory.PAPERDOLL_ARTIFACT11,
		Inventory.PAPERDOLL_ARTIFACT12,
		Inventory.PAPERDOLL_ARTIFACT13,
		Inventory.PAPERDOLL_ARTIFACT14,
		Inventory.PAPERDOLL_ARTIFACT15,
		Inventory.PAPERDOLL_ARTIFACT16,
		Inventory.PAPERDOLL_ARTIFACT17,
		Inventory.PAPERDOLL_ARTIFACT18,
		Inventory.PAPERDOLL_ARTIFACT19,
		Inventory.PAPERDOLL_ARTIFACT20,
		Inventory.PAPERDOLL_ARTIFACT21,
	};
	
	private final Player _player;
	private final int _runSpd;
	private final int _walkSpd;
	private final int _swimRunSpd;
	private final int _swimWalkSpd;
	private final int _flyRunSpd;
	private final int _flyWalkSpd;
	private final double _moveMultiplier;
	
	/**
	 * Creates a new {@code GMViewCharacterInfo} packet for a specific player.<br>
	 * This constructor calculates the movement speeds based on the player's current stats.<br>
	 * It adjusts the values using the player's movement speed multiplier.
	 * @param player The {@code Player} object used to gather character data.
	 */
	public GMViewCharacterInfo(Player player)
	{
		_player = player;
		_moveMultiplier = player.getMovementSpeedMultiplier();
		_runSpd = (int) Math.round(player.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) Math.round(player.getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = (int) Math.round(player.getSwimRunSpeed() / _moveMultiplier);
		_swimWalkSpd = (int) Math.round(player.getSwimWalkSpeed() / _moveMultiplier);
		final boolean isFlying = player.isFlying();
		_flyRunSpd = isFlying ? _runSpd : 0;
		_flyWalkSpd = isFlying ? _walkSpd : 0;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.GM_VIEW_CHARACTER_INFO.writeId(this, buffer);
		buffer.writeInt(_player.getX());
		buffer.writeInt(_player.getY());
		buffer.writeInt(_player.getZ());
		buffer.writeInt(_player.getHeading());
		buffer.writeInt(_player.getObjectId());
		buffer.writeString(_player.getName());
		buffer.writeInt(_player.getRace().ordinal());
		final PlayerAppearance appearance = _player.getAppearance();
		buffer.writeInt(appearance.isFemale());
		final int playerClassId = _player.getPlayerClass().getId();
		buffer.writeInt(playerClassId);
		buffer.writeInt(_player.getLevel());
		buffer.writeLong(_player.getExp());
		final int level = _player.getLevel();
		buffer.writeDouble((float) (_player.getExp() - ExperienceData.getInstance().getExpForLevel(level)) / (ExperienceData.getInstance().getExpForLevel(level + 1) - ExperienceData.getInstance().getExpForLevel(level))); // High Five exp %
		buffer.writeInt(_player.getSTR());
		buffer.writeInt(_player.getDEX());
		buffer.writeInt(_player.getCON());
		buffer.writeInt(_player.getINT());
		buffer.writeInt(_player.getWIT());
		buffer.writeInt(_player.getMEN());
		buffer.writeInt(_player.getLUC());
		buffer.writeInt(_player.getCHA());
		buffer.writeInt((int) _player.getMaxHp());
		buffer.writeInt((int) _player.getCurrentHp());
		buffer.writeInt(_player.getMaxMp());
		buffer.writeInt((int) _player.getCurrentMp());
		buffer.writeLong(_player.getSp());
		buffer.writeInt(_player.getCurrentLoad());
		buffer.writeInt(_player.getMaxLoad());
		final int pkKills = _player.getPkKills();
		buffer.writeInt(pkKills);
		
		for (int slot : PAPERDOLL_ITEM_SLOTS)
		{
			buffer.writeInt(_player.getInventory().getPaperdollObjectId(slot));
		}
		
		for (int slot : PAPERDOLL_ITEM_SLOTS)
		{
			buffer.writeInt(_player.getInventory().getPaperdollItemDisplayId(slot));
		}
		
		for (int slot = 0; slot < 11; slot++)
		{
			final AugmentationInstance augment = _player.getInventory().getPaperdollAugmentation(slot);
			if (augment != null)
			{
				buffer.writeInt(augment.getOption1Id());
				buffer.writeInt(augment.getOption2Id());
				// buffer.writeInt(augment.getOption3Id());
			}
			else
			{
				buffer.writeInt(0);
				buffer.writeInt(0);
				// buffer.writeInt(0);
			}
		}
		
		for (int index = 0; index < 98; index++)
		{
			buffer.writeInt(0); // unk
		}
		
		buffer.writeByte(0); // unk
		buffer.writeByte(0); // unk
		buffer.writeByte(_player.getInventory().getTalismanSlots()); // CT2.3
		buffer.writeByte(_player.getInventory().canEquipCloak()); // CT2.3
		buffer.writeByte(0);
		buffer.writeShort(0);
		buffer.writeLong(_player.getPAtk());
		buffer.writeInt(_player.getPAtkSpd());
		buffer.writeInt(_player.getPDef());
		buffer.writeInt(_player.getEvasionRate());
		buffer.writeInt(_player.getAccuracy());
		buffer.writeInt(_player.getCriticalHit());
		buffer.writeLong(_player.getMAtk());
		buffer.writeInt(_player.getMAtkSpd());
		buffer.writeInt(_player.getPAtkSpd());
		buffer.writeInt(_player.getMDef());
		buffer.writeInt(_player.getMagicEvasionRate());
		buffer.writeInt(_player.getMagicAccuracy());
		buffer.writeInt(_player.getMCriticalHit());
		buffer.writeInt(_player.getPvpFlag()); // 0-non-pvp 1-pvp = violett name
		buffer.writeInt(_player.getReputation());
		buffer.writeInt(_runSpd);
		buffer.writeInt(_walkSpd);
		buffer.writeInt(_swimRunSpd);
		buffer.writeInt(_swimWalkSpd);
		buffer.writeInt(_flyRunSpd);
		buffer.writeInt(_flyWalkSpd);
		buffer.writeInt(_flyRunSpd);
		buffer.writeInt(_flyWalkSpd);
		buffer.writeDouble(_moveMultiplier);
		buffer.writeDouble(_player.getAttackSpeedMultiplier()); // 2.9); //
		buffer.writeDouble(_player.getCollisionRadius()); // scale
		buffer.writeDouble(_player.getCollisionHeight()); // y offset ??!? fem dwarf 4033
		buffer.writeInt(appearance.getHairStyle());
		buffer.writeInt(appearance.getHairColor());
		buffer.writeInt(appearance.getFace());
		buffer.writeInt(_player.isGM()); // builder level
		buffer.writeString(_player.getTitle());
		buffer.writeInt(_player.getClanId()); // pledge id
		buffer.writeInt(_player.getClanCrestId()); // pledge crest id
		buffer.writeInt(_player.getAllyId()); // ally id
		buffer.writeByte(_player.getMountType().ordinal()); // mount type
		buffer.writeByte(_player.getPrivateStoreType().getId());
		buffer.writeByte(_player.getCreateItemLevel() > 0);
		buffer.writeInt(pkKills);
		buffer.writeInt(_player.getPvpKills());
		final Recommendations recommendations = _player.getRecommendations();
		buffer.writeShort(recommendations.getLeft());
		buffer.writeShort(recommendations.getHave()); // Blue value for name (0 = white, 255 = pure blue)
		buffer.writeInt(playerClassId);
		buffer.writeInt(0); // special effects? circles around player...
		buffer.writeInt(_player.getMaxCp());
		buffer.writeInt((int) _player.getCurrentCp());
		buffer.writeByte(_player.isRunning()); // Changes the Speed display on Status Window.
		buffer.writeByte(321);
		buffer.writeInt(_player.getPledgeClass()); // Changes the text above CP on Status Window.
		buffer.writeByte(_player.getNobleLevel());
		buffer.writeByte(_player.isHero());
		buffer.writeInt(appearance.getNameColor());
		buffer.writeInt(appearance.getTitleColor());
		
		final AttributeType attackAttribute = _player.getAttackElement();
		buffer.writeShort(attackAttribute.getClientId());
		buffer.writeShort(_player.getAttackElementValue(attackAttribute));
		for (AttributeType type : AttributeType.ATTRIBUTE_TYPES)
		{
			buffer.writeShort(_player.getDefenseElementValue(type));
		}
		
		buffer.writeInt(_player.getFame());
		buffer.writeInt(_player.getVitalityPoints());
		buffer.writeInt(0);
		buffer.writeInt(0);
		buffer.writeInt(0); // unk - parsed but discarded by the client
		buffer.writeInt(0); // unk - parsed but discarded by the client
		buffer.writeInt(0); // unk - parsed but discarded by the client
		buffer.writeInt(0); // unk - parsed but discarded by the client
		buffer.writeInt(0); // unk - parsed but discarded by the client
		buffer.writeInt(0); // unk - parsed but discarded by the client
		buffer.writeByte(0); // unk - parsed but discarded by the client
		buffer.writeByte(0); // unk - parsed but discarded by the client
		buffer.writeByte(0); // unk - parsed but discarded by the client
	}
}
