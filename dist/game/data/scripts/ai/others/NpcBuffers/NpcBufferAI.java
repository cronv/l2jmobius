/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ai.others.NpcBuffers;

import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class handles the artificial intelligence for NPCs that provide buffs to other entities.<br>
 * It manages the logic for casting {@code Skill} effects and maintaining active {@code BuffInfo} states.
 * @author UnAfraid
 */
class NpcBufferAI implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(NpcBufferAI.class.getName());
	private final Npc _npc;
	private final NpcBufferSkillData _skillData;
	
	/**
	 * Initializes a new instance of the {@code NpcBufferAI} class.<br>
	 * This constructor sets up the required NPC and skill data for the AI task.
	 * @param npc The {@code Npc} entity that will perform the buffering action.
	 * @param skill The {@code NpcBufferSkillData} containing the specific skill details.
	 */
	protected NpcBufferAI(Npc npc, NpcBufferSkillData skill)
	{
		_npc = npc;
		_skillData = skill;
	}
	
	/**
	 * Retrieves the appropriate {@code Skill} for a specific {@code Player}.<br>
	 * It checks if the player already has a buff and adjusts the skill level accordingly.<br>
	 * If no buff is found or scaling is not required, it returns the default skill.
	 * @param player The {@code Player} to check for existing buffs.
	 * @return The calculated {@code Skill} object.
	 */
	private Skill getSkill(Player player)
	{
		final int scaleToLevel = _skillData.getScaleToLevel();
		if (scaleToLevel < 1)
		{
			return _skillData.getSkill();
		}
		
		final BuffInfo currentBuff = player.getEffectList().getBuffInfoBySkillId(_skillData.getSkill().getId());
		if (currentBuff != null)
		{
			int level = currentBuff.getSkill().getLevel();
			if (scaleToLevel > level)
			{
				level++;
			}
			
			final Skill skill = SkillData.getInstance().getSkill(_skillData.getSkill().getId(), level);
			if (skill == null)
			{
				LOGGER.warning("Requested non existing skill level: " + level + " for id: " + _skillData.getSkill().getId());
			}
			
			return skill;
		}
		
		return _skillData.getSkill();
	}
	
	@Override
	public void run()
	{
		if ((_npc == null) || !_npc.isSpawned() || _npc.isDecayed() || _npc.isDead() || (_skillData == null) || (_skillData.getSkill() == null))
		{
			return;
		}
		
		if ((_npc.getSummoner() == null) || !_npc.getSummoner().isPlayer())
		{
			return;
		}
		
		final Player player = _npc.getSummoner().asPlayer();
		final Skill skill = getSkill(player);
		if (skill == null)
		{
			return;
		}
		
		_npc.doCast(skill);
		
		ThreadPool.schedule(this, skill.getReuseDelay());
	}
}
