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
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to display a damage pop-up.<br>
 * It provides visual feedback for specific combat actions or events.
 * @author Meliodas
 */
public class ExDamagePopUp extends ServerPacket
{
	public static final byte NOMAKE = 0;
	public static final byte NORMAL_ATTACK = 1;
	public static final byte CONSECUTIVE_ATTACK = 2;
	public static final byte CRITICAL = 3;
	public static final byte OVERHIT = 4;
	public static final byte RECOVER_HP = 5;
	public static final byte RECOVER_MP = 6;
	public static final byte GET_SP = 7;
	public static final byte GET_EXP = 8;
	public static final byte MAGIC_DEFIANCE = 9;
	public static final byte SHIELD_GUARD = 10;
	public static final byte DODGE = 11;
	public static final byte IMMUNE = 12;
	public static final byte SKILL_HIT = 13;
	public static final byte RECOVER_CP = 14;
	public static final byte PHYSICAL_CRITICAL = 15;
	public static final byte MAGIC_CRITICAL = 16;
	public static final byte SKILL_EVADES = 17;
	public static final byte GET_EXP_BY_MAGIC_LAMP = 18;
	public static final byte GET_SP_BY_MAGIC_LAMP = 19;
	public static final byte ETC = 100;
	
	private final int _caster;
	private final int _target;
	private final int _damage;
	private final byte _type;
	private final int _skillId;
	private final int _skillSubLevel;
	
	/**
	 * Creates a new {@code ExDamagePopUp} packet.<br>
	 * This packet displays damage or status information on the client side.
	 * @param caster The ID of the character who performed the action.
	 * @param target The ID of the character who received the action.
	 * @param damage The amount of damage dealt or recovered.
	 * @param type The category of the effect, such as critical or dodge.
	 * @param skill The {@code Skill} object associated with the action, or {@code null}.
	 */
	public ExDamagePopUp(int caster, int target, int damage, byte type, Skill skill)
	{
		_caster = caster;
		_target = target;
		_damage = damage;
		_type = type;
		
		if (skill != null)
		{
			_skillId = skill.getDisplayId();
			_skillSubLevel = skill.getSubLevel();
		}
		else
		{
			_skillId = 0;
			_skillSubLevel = 0;
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_DAMAGE_POPUP.writeId(this, buffer);
		buffer.writeInt(_caster);
		buffer.writeInt(_target);
		buffer.writeInt(-_damage);
		buffer.writeByte(_type);
		buffer.writeInt(_skillId); // 541 - nSkillID
		buffer.writeInt(_skillSubLevel); // 542 - nSkillSubLevel
	}
}
