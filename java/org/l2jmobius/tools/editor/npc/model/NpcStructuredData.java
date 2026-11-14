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
package org.l2jmobius.tools.editor.npc.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Editable repeated NPC structures that cannot be represented safely as scalar fields.
 * @author BazookaRpm
 */
public final class NpcStructuredData
{
	private final boolean _skillListPresent;
	private final boolean _parametersPresent;
	private final List<NpcSkillEntry> _skills = new ArrayList<>();
	private final List<NpcParameterEntry> _parameters = new ArrayList<>();
	private final List<NpcParameterSkillEntry> _parameterSkills = new ArrayList<>();
	private final List<NpcMinionSet> _minionSets = new ArrayList<>();
	
	public NpcStructuredData(boolean skillListPresent, boolean parametersPresent)
	{
		_skillListPresent = skillListPresent;
		_parametersPresent = parametersPresent;
	}
	
	public boolean hasSkillList()
	{
		return _skillListPresent;
	}
	
	public boolean hasParameters()
	{
		return _parametersPresent;
	}
	
	public void addLoadedSkill(NpcSkillEntry skill)
	{
		_skills.add(skill);
	}
	
	public void addSkill(NpcSkillEntry skill)
	{
		_skills.add(skill);
	}
	
	public void removeSkill(NpcSkillEntry skill)
	{
		if (skill.isNew())
		{
			_skills.remove(skill);
		}
		else
		{
			skill.setRemoved(true);
		}
	}
	
	public void addLoadedParameter(NpcParameterEntry parameter)
	{
		_parameters.add(parameter);
	}
	
	public void addParameter(NpcParameterEntry parameter)
	{
		_parameters.add(parameter);
	}
	
	public void removeParameter(NpcParameterEntry parameter)
	{
		if (parameter.isNew())
		{
			_parameters.remove(parameter);
		}
		else
		{
			parameter.setRemoved(true);
		}
	}
	
	public void addLoadedParameterSkill(NpcParameterSkillEntry skill)
	{
		_parameterSkills.add(skill);
	}
	
	public void addParameterSkill(NpcParameterSkillEntry skill)
	{
		_parameterSkills.add(skill);
	}
	
	public void removeParameterSkill(NpcParameterSkillEntry skill)
	{
		if (skill.isNew())
		{
			_parameterSkills.remove(skill);
		}
		else
		{
			skill.setRemoved(true);
		}
	}
	
	public void addLoadedMinionSet(NpcMinionSet minionSet)
	{
		_minionSets.add(minionSet);
	}
	
	public void addMinionSet(NpcMinionSet minionSet)
	{
		_minionSets.add(minionSet);
	}
	
	public void removeMinionSet(NpcMinionSet minionSet)
	{
		if (minionSet.isNew())
		{
			_minionSets.remove(minionSet);
		}
		else
		{
			minionSet.setRemoved(true);
		}
	}
	
	public List<NpcSkillEntry> getSkills()
	{
		final List<NpcSkillEntry> result = new ArrayList<>();
		for (NpcSkillEntry skill : _skills)
		{
			if (!skill.isRemoved())
			{
				result.add(skill);
			}
		}
		
		return Collections.unmodifiableList(result);
	}
	
	public List<NpcSkillEntry> getAllSkills()
	{
		return Collections.unmodifiableList(_skills);
	}
	
	public List<NpcParameterEntry> getParameters()
	{
		final List<NpcParameterEntry> result = new ArrayList<>();
		for (NpcParameterEntry parameter : _parameters)
		{
			if (!parameter.isRemoved())
			{
				result.add(parameter);
			}
		}
		
		return Collections.unmodifiableList(result);
	}
	
	public List<NpcParameterEntry> getAllParameters()
	{
		return Collections.unmodifiableList(_parameters);
	}
	
	public List<NpcParameterSkillEntry> getParameterSkills()
	{
		final List<NpcParameterSkillEntry> result = new ArrayList<>();
		for (NpcParameterSkillEntry skill : _parameterSkills)
		{
			if (!skill.isRemoved())
			{
				result.add(skill);
			}
		}
		
		return Collections.unmodifiableList(result);
	}
	
	public List<NpcParameterSkillEntry> getAllParameterSkills()
	{
		return Collections.unmodifiableList(_parameterSkills);
	}
	
	public List<NpcMinionSet> getMinionSets()
	{
		final List<NpcMinionSet> result = new ArrayList<>();
		for (NpcMinionSet minionSet : _minionSets)
		{
			if (!minionSet.isRemoved())
			{
				result.add(minionSet);
			}
		}
		
		return Collections.unmodifiableList(result);
	}
	
	public List<NpcMinionSet> getAllMinionSets()
	{
		return Collections.unmodifiableList(_minionSets);
	}
	
	public int getActiveParameterChildCount()
	{
		return getParameters().size() + getParameterSkills().size() + getMinionSets().size();
	}
	
	public int getMinionCount()
	{
		int count = 0;
		for (NpcMinionSet minionSet : getMinionSets())
		{
			count += minionSet.getMinions().size();
		}
		
		return count;
	}
	
	public boolean isDirty()
	{
		return getChangedCount() > 0;
	}
	
	public int getChangedCount()
	{
		int count = 0;
		for (NpcSkillEntry skill : _skills)
		{
			if (skill.isChanged())
			{
				count++;
			}
		}
		
		for (NpcParameterEntry parameter : _parameters)
		{
			if (parameter.isChanged())
			{
				count++;
			}
		}
		
		for (NpcParameterSkillEntry skill : _parameterSkills)
		{
			if (skill.isChanged())
			{
				count++;
			}
		}
		
		for (NpcMinionSet minionSet : _minionSets)
		{
			count += minionSet.getChangedCount();
		}
		
		return count;
	}
	
	public void revert()
	{
		for (int index = _skills.size() - 1; index >= 0; index--)
		{
			if (_skills.get(index).isNew())
			{
				_skills.remove(index);
			}
		}
		
		for (NpcSkillEntry skill : _skills)
		{
			skill.revert();
		}
		
		for (int index = _parameters.size() - 1; index >= 0; index--)
		{
			if (_parameters.get(index).isNew())
			{
				_parameters.remove(index);
			}
		}
		
		for (NpcParameterEntry parameter : _parameters)
		{
			parameter.revert();
		}
		
		for (int index = _parameterSkills.size() - 1; index >= 0; index--)
		{
			if (_parameterSkills.get(index).isNew())
			{
				_parameterSkills.remove(index);
			}
		}
		
		for (NpcParameterSkillEntry skill : _parameterSkills)
		{
			skill.revert();
		}
		
		for (int index = _minionSets.size() - 1; index >= 0; index--)
		{
			if (_minionSets.get(index).isNew())
			{
				_minionSets.remove(index);
			}
		}
		
		for (NpcMinionSet minionSet : _minionSets)
		{
			minionSet.revert();
		}
	}
}
