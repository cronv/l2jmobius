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

import java.util.Collection;
import java.util.Collections;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.SkillCastingType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client when a magic skill is successfully launched.<br>
 * It contains information about the {@code Creature} who cast the skill and the target {@code WorldObject}.
 */
public class MagicSkillLaunched extends ServerPacket
{
	private final int _objectId;
	private final int _skillId;
	private final int _skillLevel;
	private final SkillCastingType _castingType;
	private final Collection<WorldObject> _targets;
	
	/**
	 * Creates a new {@code MagicSkillLaunched} packet.<br>
	 * This packet informs the client that a creature has cast a magic skill.<br>
	 * It handles multiple targets or defaults to the caster if the target list is {@code null}.
	 * @param creature The {@code Creature} who performed the action.
	 * @param skillId The unique identifier for the skill used.
	 * @param skillLevel The level of the skill being cast.
	 * @param castingType The type of casting method used.
	 * @param targets A collection of {@code WorldObject} instances affected by the skill.
	 */
	public MagicSkillLaunched(Creature creature, int skillId, int skillLevel, SkillCastingType castingType, Collection<WorldObject> targets)
	{
		_objectId = creature.getObjectId();
		_skillId = skillId;
		_skillLevel = skillLevel;
		_castingType = castingType;
		if (targets == null)
		{
			_targets = Collections.singletonList(creature);
			return;
		}
		
		_targets = targets;
	}
	
	/**
	 * This constructor creates a {@code MagicSkillLaunched} packet for a single target.<br>
	 * It handles the logic of assigning the correct {@code WorldObject} to the skill.<br>
	 * If the provided {@code target} is {@code null}, it defaults to the {@code creature}.
	 * @param creature The {@code Creature} who launched the skill.
	 * @param skillId The unique identifier for the skill used.
	 * @param skillLevel The level of the skill being cast.
	 * @param castingType The type of casting method used.
	 * @param target The specific {@code WorldObject} targeted by the skill.
	 */
	public MagicSkillLaunched(Creature creature, int skillId, int skillLevel, SkillCastingType castingType, WorldObject target)
	{
		this(creature, skillId, skillLevel, castingType, Collections.singletonList(target == null ? creature : target));
	}
	
	/**
	 * This constructor creates a {@code MagicSkillLaunched} packet for a basic skill.<br>
	 * It uses the default {@code SkillCastingType.NORMAL}.<br>
	 * The creature itself is set as the primary target.
	 * @param creature The {@code Creature} that launched the skill.
	 * @param skillId The unique identifier for the skill.
	 * @param skillLevel The level of the skill being used.
	 */
	public MagicSkillLaunched(Creature creature, int skillId, int skillLevel)
	{
		this(creature, skillId, skillLevel, SkillCastingType.NORMAL, Collections.singletonList(creature));
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.MAGIC_SKILL_LAUNCHED.writeId(this, buffer);
		buffer.writeInt(_castingType.getClientBarId()); // MagicSkillUse castingType
		buffer.writeInt(_objectId);
		buffer.writeInt(_skillId);
		buffer.writeInt(_skillLevel);
		buffer.writeInt(_targets.size());
		for (WorldObject target : _targets)
		{
			buffer.writeInt(target.getObjectId());
		}
	}
}
