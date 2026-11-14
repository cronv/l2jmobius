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
package org.l2jmobius.gameserver.mechanics.skill.holders;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.SocialClass;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class manages the logic for learning new skills.<br>
 * It handles the requirements and conditions necessary for a character to acquire a {@code Skill}.
 * @author Zoey76
 */
public class SkillLearn
{
	private final String _skillName;
	private final int _skillId;
	private final int _skillLevel;
	private final int _getLevel;
	private final int _getDualClassLevel;
	private final boolean _autoGet;
	private final long _levelUpSp;
	private final List<List<ItemHolder>> _requiredItems = new ArrayList<>(1);
	private final Set<Race> _races = EnumSet.noneOf(Race.class);
	private final Set<SkillHolder> _preReqSkills = new HashSet<>(1);
	private SocialClass _socialClass;
	private final boolean _residenceSkill;
	private final Set<Integer> _residenceIds = new HashSet<>(1);
	private final boolean _learnedByNpc;
	private final boolean _learnedByFS;
	private final Set<Integer> _removeSkills = new HashSet<>(1);
	private final int _treeId;
	private final int _row;
	private final int _column;
	private final int _pointsRequired;
	
	/**
	 * Creates a new {@code SkillLearn} instance using the provided data.<br>
	 * This constructor populates all internal fields from a {@code StatSet}.
	 * @param set The {@code StatSet} containing the skill configuration data.
	 */
	public SkillLearn(StatSet set)
	{
		_skillName = set.getString("skillName");
		_skillId = set.getInt("skillId");
		_skillLevel = set.getInt("skillLevel");
		_getLevel = set.getInt("getLevel");
		_getDualClassLevel = set.getInt("getDualClassLevel", 0);
		_autoGet = set.getBoolean("autoGet", false);
		_levelUpSp = set.getLong("levelUpSp", 0);
		_residenceSkill = set.getBoolean("residenceSkill", false);
		_learnedByNpc = set.getBoolean("learnedByNpc", false);
		_learnedByFS = set.getBoolean("learnedByFS", false);
		_treeId = set.getInt("treeId", 0);
		_row = set.getInt("row", 0);
		_column = set.getInt("row", 0);
		_pointsRequired = set.getInt("pointsRequired", 0);
	}
	
	/**
	 * Retrieves the name of the skill.
	 * @return the {@code String} name of the skill.
	 */
	public String getName()
	{
		return _skillName;
	}
	
	/**
	 * Retrieves the unique identifier for the skill.<br>
	 * This value corresponds to the ID defined in the game data.
	 * @return the {@code int} ID of the skill.
	 */
	public int getSkillId()
	{
		return _skillId;
	}
	
	/**
	 * Retrieves the current level of the skill.
	 * @return the integer value representing the skill level.
	 */
	public int getSkillLevel()
	{
		return _skillLevel;
	}
	
	/**
	 * Retrieves the level required to obtain this skill.
	 * @return the integer value of the required level.
	 */
	public int getGetLevel()
	{
		return _getLevel;
	}
	
	/**
	 * Retrieves the level required for dual class skills.<br>
	 * This value is used to determine if a skill can be learned via dual classing.
	 * @return the dual class level as an {@code int}.
	 */
	public int getDualClassLevel()
	{
		return _getDualClassLevel;
	}
	
	/**
	 * Retrieves the Skill Points required to level up this skill.
	 * @return the amount of {@code long} points needed for a level increase.
	 */
	public long getLevelUpSp()
	{
		return _levelUpSp;
	}
	
	/**
	 * Checks if the skill is automatically acquired.
	 * @return {@code true} if the skill is auto-acquired, {@code false} otherwise.
	 */
	public boolean isAutoGet()
	{
		return _autoGet;
	}
	
	/**
	 * Retrieves the list of items required to learn this skill.<br>
	 * Each inner list represents a group of items that must be provided together.
	 * @return a {@code List<List<ItemHolder>>} containing all required item groups.
	 */
	public List<List<ItemHolder>> getRequiredItems()
	{
		return _requiredItems;
	}
	
	/**
	 * Adds a new list of required items to the skill.<br>
	 * This updates the internal {@code _requiredItems} collection.
	 * @param list the list of {@code ItemHolder} objects to add
	 */
	public void addRequiredItem(List<ItemHolder> list)
	{
		_requiredItems.add(list);
	}
	
	/**
	 * Retrieves the set of races associated with this skill.
	 * @return a {@code Set} containing all {@code Race} types.
	 */
	public Set<Race> getRaces()
	{
		return _races;
	}
	
	/**
	 * Adds a specific race to the set of allowed races.<br>
	 * This updates the internal collection used by {@code SkillLearn}.
	 * @param race the {@code Race} to add
	 */
	public void addRace(Race race)
	{
		_races.add(race);
	}
	
	/**
	 * Retrieves the set of skills required to learn this skill.
	 * @return a {@code Set} containing all necessary {@code SkillHolder} objects.
	 */
	public Set<SkillHolder> getPreReqSkills()
	{
		return _preReqSkills;
	}
	
	/**
	 * Adds a new prerequisite skill to the requirements list.<br>
	 * This method updates the internal set of skills needed to learn this one.
	 * @param skill The {@code SkillHolder} object representing the required skill.
	 */
	public void addPreReqSkill(SkillHolder skill)
	{
		_preReqSkills.add(skill);
	}
	
	/**
	 * Retrieves the {@code SocialClass} associated with this skill.
	 * @return the {@code SocialClass} of the skill.
	 */
	public SocialClass getSocialClass()
	{
		return _socialClass;
	}
	
	/**
	 * Sets the {@code SocialClass} for this skill.<br>
	 * This value is only assigned if the current value is {@code null}.
	 * @param socialClass The {@code SocialClass} to assign.
	 */
	public void setSocialClass(SocialClass socialClass)
	{
		if (_socialClass == null)
		{
			_socialClass = socialClass;
		}
	}
	
	/**
	 * Checks if the skill is a residential skill.<br>
	 * Returns {@code true} if it is a residence skill, otherwise returns {@code false}.
	 * @return the residential status of the skill.
	 */
	public boolean isResidencialSkill()
	{
		return _residenceSkill;
	}
	
	/**
	 * Retrieves the set of residence identifiers associated with this skill.
	 * @return a {@code Set<Integer>} containing the residence IDs.
	 */
	public Set<Integer> getResidenceIds()
	{
		return _residenceIds;
	}
	
	/**
	 * Adds a new residence identifier to the set of allowed residences.<br>
	 * This method updates the internal collection used by {@code isResidencialSkill}.
	 * @param id The unique identifier for the residence.
	 */
	public void addResidenceId(Integer id)
	{
		_residenceIds.add(id);
	}
	
	/**
	 * Checks if the skill is learned by an NPC.
	 * @return {@code true} if the skill is learned by an NPC, {@code false} otherwise.
	 */
	public boolean isLearnedByNpc()
	{
		return _learnedByNpc;
	}
	
	/**
	 * Checks if the skill is learned by a Free Soul.
	 * @return {@code true} if the skill is learned by a Free Soul, {@code false} otherwise.
	 */
	public boolean isLearnedByFS()
	{
		return _learnedByFS;
	}
	
	/**
	 * Adds a skill ID to the set of skills that must be removed.<br>
	 * This method updates the internal collection of restricted skills.
	 * @param skillId The unique identifier for the skill to remove.
	 */
	public void addRemoveSkills(int skillId)
	{
		_removeSkills.add(skillId);
	}
	
	/**
	 * Retrieves the set of skills that are removed when this skill is learned.
	 * @return a {@code Set<Integer>} containing the IDs of the removed skills.
	 */
	public Set<Integer> getRemoveSkills()
	{
		return _removeSkills;
	}
	
	/**
	 * Retrieves the unique identifier for the skill tree.<br>
	 * This value determines which category the skill belongs to.
	 * @return the {@code int} ID of the skill tree.
	 */
	public int getTreeId()
	{
		return _treeId;
	}
	
	/**
	 * Retrieves the row index of the skill.<br>
	 * This value is used to determine the position within a skill tree.
	 * @return the current row as an {@code int}.
	 */
	public int getRow()
	{
		return _row;
	}
	
	/**
	 * Retrieves the column index of the skill.<br>
	 * This value is used to determine the position within a skill tree.
	 * @return the current column as an {@code int}.
	 */
	public int getColumn()
	{
		return _column;
	}
	
	/**
	 * Retrieves the number of points needed to learn this skill.
	 * @return the amount of required points as an {@code int}.
	 */
	public int getPointsRequired()
	{
		return _pointsRequired;
	}
	
	/**
	 * Returns a string representation of the {@code SkillLearn} data.<br>
	 * This includes the skill name, tree coordinates, and required points.
	 * @return A formatted string containing the skill details.
	 */
	@Override
	public String toString()
	{
		final Skill skill = SkillData.getInstance().getSkill(_skillId, _skillLevel);
		return "[" + skill + " treeId: " + _treeId + " row: " + _row + " column: " + _column + " pointsRequired:" + _pointsRequired + "]";
	}
}
