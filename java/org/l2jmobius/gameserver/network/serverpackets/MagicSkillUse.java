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
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.SkillCastingType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This class handles the {@code MagicSkillUse} server packet.<br>
 * It processes requests from clients to execute magic skills in the game world.
 * @author Mobius
 */
public class MagicSkillUse extends ServerPacket
{
	private final int _skillId;
	private final int _skillLevel;
	private final int _hitTime;
	private final int _reuseGroup;
	private final int _reuseDelay;
	private final int _actionId; // If skill is called from RequestActionUse, use that ID.
	private final SkillCastingType _castingType; // Defines which client bar is going to use.
	private final Creature _creature;
	private final WorldObject _target;
	private final boolean _isGroundTargetSkill;
	private final Location _groundLocation;
	
	/**
	 * Constructs a new {@code MagicSkillUse} packet with all required skill details.<br>
	 * This constructor initializes the data needed to synchronize a magic skill cast on the client.
	 * @param creature The {@code Creature} performing the action.
	 * @param target The {@code WorldObject} being targeted by the skill.
	 * @param skillId The unique identifier for the skill.
	 * @param skillLevel The level of the skill being used.
	 * @param hitTime The time in milliseconds until the skill hits.
	 * @param reuseDelay The cooldown delay for the skill.
	 * @param reuseGroup The group ID for managing concurrent skill reuses.
	 * @param actionId The specific action identifier for the skill.
	 * @param castingType The {@code SkillCastingType} used to determine the client bar.
	 * @param isGroundTargetSkill A boolean indicating if the skill targets a ground location.
	 */
	public MagicSkillUse(Creature creature, WorldObject target, int skillId, int skillLevel, int hitTime, int reuseDelay, int reuseGroup, int actionId, SkillCastingType castingType, boolean isGroundTargetSkill)
	{
		_creature = creature;
		_target = target;
		_skillId = skillId;
		_skillLevel = skillLevel;
		_hitTime = hitTime;
		_reuseGroup = reuseGroup;
		_reuseDelay = reuseDelay;
		_actionId = actionId;
		_castingType = castingType;
		_isGroundTargetSkill = isGroundTargetSkill;
		_groundLocation = creature.isPlayer() ? creature.asPlayer().getCurrentSkillWorldPosition() : null;
	}
	
	/**
	 * Creates a new {@code MagicSkillUse} packet.<br>
	 * This constructor initializes all necessary data for a magic skill execution.
	 * @param creature The {@code Creature} performing the action.
	 * @param target The {@code WorldObject} being targeted by the skill.
	 * @param skillId The unique identifier for the skill.
	 * @param skillLevel The level of the skill being used.
	 * @param hitTime The time required for the skill to hit.
	 * @param reuseDelay The delay before the skill can be reused.
	 * @param reuseGroup The group identifier for skill reuse logic.
	 * @param actionId The specific action ID associated with the skill.
	 * @param castingType The {@code SkillCastingType} used to determine the client bar.
	 */
	public MagicSkillUse(Creature creature, WorldObject target, int skillId, int skillLevel, int hitTime, int reuseDelay, int reuseGroup, int actionId, SkillCastingType castingType)
	{
		this(creature, target, skillId, skillLevel, hitTime, reuseDelay, reuseGroup, actionId, castingType, false);
	}
	
	/**
	 * Creates a new {@code MagicSkillUse} packet for a specific creature and target.<br>
	 * This constructor uses default values for the action ID and casting type.
	 * @param creature The {@code Creature} who is performing the skill.
	 * @param target The {@code WorldObject} being targeted by the skill.
	 * @param skillId The unique identifier for the skill.
	 * @param skillLevel The level of the skill being used.
	 * @param hitTime The time required for the skill to hit.
	 * @param reuseDelay The delay before the skill can be reused.
	 */
	public MagicSkillUse(Creature creature, WorldObject target, int skillId, int skillLevel, int hitTime, int reuseDelay)
	{
		this(creature, target, skillId, skillLevel, hitTime, reuseDelay, -1, -1, SkillCastingType.NORMAL);
	}
	
	/**
	 * Creates a {@code MagicSkillUse} packet for a creature using a specific skill.<br>
	 * This constructor sets the target to the same as the {@code creature}.<br>
	 * It uses default values for action ID and casting type.
	 * @param creature The {@code Creature} performing the skill.
	 * @param skillId The unique identifier for the skill.
	 * @param skillLevel The level of the skill being used.
	 * @param hitTime The time in milliseconds until the skill hits.
	 * @param reuseDelay The cooldown delay for the skill.
	 */
	public MagicSkillUse(Creature creature, int skillId, int skillLevel, int hitTime, int reuseDelay)
	{
		this(creature, creature, skillId, skillLevel, hitTime, reuseDelay, -1, -1, SkillCastingType.NORMAL);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.MAGIC_SKILL_USE.writeId(this, buffer);
		buffer.writeInt(_castingType.getClientBarId()); // Casting bar type: 0 - default, 1 - default up, 2 - blue, 3 - green, 4 - red.
		buffer.writeInt(_creature.getObjectId());
		buffer.writeInt(_target.getObjectId());
		buffer.writeInt(_skillId);
		buffer.writeInt(_skillLevel);
		buffer.writeInt(_hitTime);
		buffer.writeInt(_reuseGroup);
		buffer.writeInt(_reuseDelay);
		buffer.writeInt(_creature.getX());
		buffer.writeInt(_creature.getY());
		buffer.writeInt(_creature.getZ());
		buffer.writeShort(_isGroundTargetSkill ? 65535 : 0);
		if (_groundLocation == null)
		{
			buffer.writeShort(0);
		}
		else
		{
			buffer.writeShort(1);
			buffer.writeInt(_groundLocation.getX());
			buffer.writeInt(_groundLocation.getY());
			buffer.writeInt(_groundLocation.getZ());
		}
		
		buffer.writeInt(_target.getX());
		buffer.writeInt(_target.getY());
		buffer.writeInt(_target.getZ());
		buffer.writeInt(_actionId >= 0); // 1 when ID from RequestActionUse is used
		buffer.writeInt(_actionId >= 0 ? _actionId : 0); // ID from RequestActionUse. Used to set cooldown on summon skills.
		if (_groundLocation == null)
		{
			buffer.writeInt(-1); // 306
		}
	}
}
