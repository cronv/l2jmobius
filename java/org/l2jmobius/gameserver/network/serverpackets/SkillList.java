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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet sends a list of available skills to the client.<br>
 * It is used to populate the skill window or other UI elements that display skills.
 * @author Mobius
 */
public class SkillList extends ServerPacket
{
	private final List<Skill> _skills = new ArrayList<>();
	private int _lastLearnedSkillId = 0;
	
	/**
	 * Creates a new instance of the {@code SkillList} class.<br>
	 * This constructor initializes an empty list of skills.
	 */
	public SkillList()
	{
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.SKILL_LIST.writeId(this, buffer);
		_skills.sort(Comparator.comparingInt(s -> SkillData.getInstance().getSkill(s.id, s.level, s.subLevel).isToggle() ? 1 : 0));
		buffer.writeInt(_skills.size());
		for (Skill temp : _skills)
		{
			buffer.writeInt(temp.passive);
			buffer.writeShort(temp.level);
			buffer.writeShort(temp.subLevel);
			buffer.writeInt(temp.id);
			buffer.writeInt(temp.reuseDelayGroup); // GOD ReuseDelayShareGroupID
			buffer.writeByte(temp.disabled); // iSkillDisabled
			buffer.writeByte(temp.enchanted); // CanEnchant
		}
		
		buffer.writeInt(_lastLearnedSkillId);
	}
	
	/**
	 * Adds a new skill to the internal list.<br>
	 * This method creates a {@code Skill} object using the provided parameters.
	 * @param id The unique identifier for the skill.
	 * @param reuseDelayGroup The group ID for the skill's reuse delay.
	 * @param level The current level of the skill.
	 * @param subLevel The current sub-level of the skill.
	 * @param passive Set to {@code true} if the skill is a passive ability.
	 * @param disabled Set to {@code true} if the skill is currently disabled.
	 * @param enchanted Set to {@code true} if the skill has been enchanted.
	 */
	public void addSkill(int id, int reuseDelayGroup, int level, int subLevel, boolean passive, boolean disabled, boolean enchanted)
	{
		_skills.add(new Skill(id, reuseDelayGroup, level, subLevel, passive, disabled, enchanted));
	}
	
	/**
	 * Updates the ID of the most recently learned skill.<br>
	 * This value is used to track progress in the skill list.
	 * @param lastLearnedSkillId The unique identifier for the last skill learned.
	 */
	public void setLastLearnedSkillId(int lastLearnedSkillId)
	{
		_lastLearnedSkillId = lastLearnedSkillId;
	}
	
	private static class Skill
	{
		public int id;
		public int reuseDelayGroup;
		public int level;
		public int subLevel;
		public boolean passive;
		public boolean disabled;
		public boolean enchanted;
		
		Skill(int pId, int pReuseDelayGroup, int pLevel, int pSubLevel, boolean pPassive, boolean pDisabled, boolean pEnchanted)
		{
			id = pId;
			reuseDelayGroup = pReuseDelayGroup;
			level = pLevel;
			subLevel = pSubLevel;
			passive = pPassive;
			disabled = pDisabled;
			enchanted = pEnchanted;
		}
	}
}
