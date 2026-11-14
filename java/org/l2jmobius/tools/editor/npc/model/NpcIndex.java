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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-memory NPC browser index with observed values and lazy skill metadata lookup for the active datapack.
 * @author BazookaRpm
 */
public final class NpcIndex
{
	private final NpcProjectLayout _layout;
	private final List<NpcRecord> _npcs = new ArrayList<>();
	private final Map<Integer, NpcRecord> _npcsById = new LinkedHashMap<>();
	private final Map<Integer, NpcSkillInfo> _eagerSkillsById = new LinkedHashMap<>();
	private final Set<String> _types = new LinkedHashSet<>();
	private final Set<String> _races = new LinkedHashSet<>();
	private final Set<String> _sexes = new LinkedHashSet<>();
	private NpcSkillLookup _skillLookup;
	
	public NpcIndex(NpcProjectLayout layout)
	{
		_layout = layout;
	}
	
	public void add(NpcRecord npc)
	{
		_npcs.add(npc);
		_npcsById.put(npc.getId(), npc);
		addValue(_types, npc.getType());
		addValue(_races, npc.getRace());
		addValue(_sexes, npc.getSex());
	}
	
	public void addSkill(NpcSkillInfo skill)
	{
		_eagerSkillsById.put(skill.getId(), skill);
	}
	
	public void setSkillLookup(NpcSkillLookup skillLookup)
	{
		_skillLookup = skillLookup;
	}
	
	private void addValue(Set<String> values, String value)
	{
		if ((value != null) && !value.isBlank())
		{
			values.add(value);
		}
	}
	
	public NpcProjectLayout getLayout()
	{
		return _layout;
	}
	
	public List<NpcRecord> getNpcs()
	{
		return Collections.unmodifiableList(_npcs);
	}
	
	public Set<String> getTypes()
	{
		return Collections.unmodifiableSet(_types);
	}
	
	public Set<String> getRaces()
	{
		return Collections.unmodifiableSet(_races);
	}
	
	public Set<String> getSexes()
	{
		return Collections.unmodifiableSet(_sexes);
	}
	
	public NpcRecord getNpc(int npcId)
	{
		return _npcsById.get(npcId);
	}
	
	public NpcSkillInfo getSkill(int skillId)
	{
		final NpcSkillInfo eager = _eagerSkillsById.get(skillId);
		return eager != null ? eager : (_skillLookup == null ? null : _skillLookup.getSkill(skillId));
	}
	
	public Collection<NpcSkillInfo> getSkills()
	{
		if (_skillLookup == null)
		{
			return Collections.unmodifiableCollection(_eagerSkillsById.values());
		}
		
		final Map<Integer, NpcSkillInfo> combined = new LinkedHashMap<>();
		for (NpcSkillInfo skill : _skillLookup.getAllSkills())
		{
			combined.put(skill.getId(), skill);
		}
		
		combined.putAll(_eagerSkillsById);
		return Collections.unmodifiableCollection(combined.values());
	}
	
	public int getSkillCount()
	{
		return getSkills().size();
	}
	
	public int getLoadedSkillCount()
	{
		return _eagerSkillsById.size() + (_skillLookup == null ? 0 : _skillLookup.getLoadedSkillCount());
	}
	
	public int getSkillFileCount()
	{
		return _skillLookup == null ? 0 : _skillLookup.getFileCount();
	}
}
