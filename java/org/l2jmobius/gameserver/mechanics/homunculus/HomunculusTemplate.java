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
package org.l2jmobius.gameserver.mechanics.homunculus;

/**
 * Template holding the base stats and configuration used to create homunculus entities.
 * @author NviX
 */
public class HomunculusTemplate
{
	private final int _id;
	private final String _name;
	private final int _type;
	private final int _basicSkillId;
	private final int _basicSkillLevel;
	private final int _skillId1;
	private final int _skillId2;
	private final int _skillId3;
	private final int _skillId4;
	private final int _skillId5;
	private final int _hpLevel1;
	private final int _atkLevel1;
	private final int _defLevel1;
	private final int _expToLevel2;
	private final int _hpLevel2;
	private final int _atkLevel2;
	private final int _defLevel2;
	private final int _expToLevel3;
	private final int _hpLevel3;
	private final int _atkLevel3;
	private final int _defLevel3;
	private final int _expToLevel4;
	private final int _hpLevel4;
	private final int _atkLevel4;
	private final int _defLevel4;
	private final int _expToLevel5;
	private final int _hpLevel5;
	private final int _atkLevel5;
	private final int _defLevel5;
	private final int _expToLevel6;
	private final int _critRate;
	
	/**
	 * Creates a homunculus template with identity, skills and per-level progression stats.
	 * @param id The template id.
	 * @param name The homunculus name.
	 * @param type The homunculus type.
	 * @param basicSkillId The basic skill id.
	 * @param basicSkillLevel The basic skill level.
	 * @param skillId1 The first additional skill id.
	 * @param skillId2 The second additional skill id.
	 * @param skillId3 The third additional skill id.
	 * @param skillId4 The fourth additional skill id.
	 * @param skillId5 The fifth additional skill id.
	 * @param hpLevel1 The health points at level 1.
	 * @param atkLevel1 The attack power at level 1.
	 * @param defLevel1 The defense power at level 1.
	 * @param expToLevel2 The experience to reach level 2.
	 * @param hpLevel2 The health points at level 2.
	 * @param atkLevel2 The attack power at level 2.
	 * @param defLevel2 The defense power at level 2.
	 * @param expToLevel3 The experience to reach level 3.
	 * @param hpLevel3 The health points at level 3.
	 * @param atkLevel3 The attack power at level 3.
	 * @param defLevel3 The defense power at level 3.
	 * @param expToLevel4 The experience to reach level 4.
	 * @param hpLevel4 The health points at level 4.
	 * @param atkLevel4 The attack power at level 4.
	 * @param defLevel4 The defense power at level 4.
	 * @param expToLevel5 The experience to reach level 5.
	 * @param hpLevel5 The health points at level 5.
	 * @param atkLevel5 The attack power at level 5.
	 * @param defLevel5 The defense power at level 5.
	 * @param expToLevel6 The experience to reach level 6.
	 * @param critRate The critical hit rate.
	 */
	public HomunculusTemplate(int id, String name, int type, int basicSkillId, int basicSkillLevel, int skillId1, int skillId2, int skillId3, int skillId4, int skillId5, int hpLevel1, int atkLevel1, int defLevel1, int expToLevel2, int hpLevel2, int atkLevel2, int defLevel2, int expToLevel3, int hpLevel3, int atkLevel3, int defLevel3, int expToLevel4, int hpLevel4, int atkLevel4, int defLevel4, int expToLevel5, int hpLevel5, int atkLevel5, int defLevel5, int expToLevel6, int critRate)
	{
		_id = id;
		_name = name;
		_type = type;
		_basicSkillId = basicSkillId;
		_basicSkillLevel = basicSkillLevel;
		_skillId1 = skillId1;
		_skillId2 = skillId2;
		_skillId3 = skillId3;
		_skillId4 = skillId4;
		_skillId5 = skillId5;
		_hpLevel1 = hpLevel1;
		_atkLevel1 = atkLevel1;
		_defLevel1 = defLevel1;
		_expToLevel2 = expToLevel2;
		_hpLevel2 = hpLevel2;
		_atkLevel2 = atkLevel2;
		_defLevel2 = defLevel2;
		_expToLevel3 = expToLevel3;
		_hpLevel3 = hpLevel3;
		_atkLevel3 = atkLevel3;
		_defLevel3 = defLevel3;
		_expToLevel4 = expToLevel4;
		_hpLevel4 = hpLevel4;
		_atkLevel4 = atkLevel4;
		_defLevel4 = defLevel4;
		_expToLevel5 = expToLevel5;
		_hpLevel5 = hpLevel5;
		_atkLevel5 = atkLevel5;
		_defLevel5 = defLevel5;
		_expToLevel6 = expToLevel6;
		_critRate = critRate;
	}
	
	/**
	 * Retrieves the template id.
	 * @return The id as an {@code int}.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the homunculus name.
	 * @return The name as a {@code String}.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the homunculus type.
	 * @return The type as an {@code int}.
	 */
	public int getType()
	{
		return _type;
	}
	
	/**
	 * Retrieves the basic skill id.
	 * @return The basic skill id as an {@code int}.
	 */
	public int getBasicSkillId()
	{
		return _basicSkillId;
	}
	
	/**
	 * Retrieves the basic skill level.
	 * @return The basic skill level as an {@code int}.
	 */
	public int getBasicSkillLevel()
	{
		return _basicSkillLevel;
	}
	
	/**
	 * Retrieves the first additional skill id.
	 * @return The first skill id as an {@code int}.
	 */
	public int getSkillId1()
	{
		return _skillId1;
	}
	
	/**
	 * Retrieves the second additional skill id.
	 * @return The second skill id as an {@code int}.
	 */
	public int getSkillId2()
	{
		return _skillId2;
	}
	
	/**
	 * Retrieves the third additional skill id.
	 * @return The third skill id as an {@code int}.
	 */
	public int getSkillId3()
	{
		return _skillId3;
	}
	
	/**
	 * Retrieves the fourth additional skill id.
	 * @return The fourth skill id as an {@code int}.
	 */
	public int getSkillId4()
	{
		return _skillId4;
	}
	
	/**
	 * Retrieves the fifth additional skill id.
	 * @return The fifth skill id as an {@code int}.
	 */
	public int getSkillId5()
	{
		return _skillId5;
	}
	
	/**
	 * Retrieves the health points at level 1.
	 * @return The health points as an {@code int}.
	 */
	public int getHpLevel1()
	{
		return _hpLevel1;
	}
	
	/**
	 * Retrieves the health points at level 2.
	 * @return The health points as an {@code int}.
	 */
	public int getHpLevel2()
	{
		return _hpLevel2;
	}
	
	/**
	 * Retrieves the health points at level 3.
	 * @return The health points as an {@code int}.
	 */
	public int getHpLevel3()
	{
		return _hpLevel3;
	}
	
	/**
	 * Retrieves the health points at level 4.
	 * @return The health points as an {@code int}.
	 */
	public int getHpLevel4()
	{
		return _hpLevel4;
	}
	
	/**
	 * Retrieves the health points at level 5.
	 * @return The health points as an {@code int}.
	 */
	public int getHpLevel5()
	{
		return _hpLevel5;
	}
	
	/**
	 * Retrieves the attack power at level 1.
	 * @return The attack power as an {@code int}.
	 */
	public int getAtkLevel1()
	{
		return _atkLevel1;
	}
	
	/**
	 * Retrieves the attack power at level 2.
	 * @return The attack power as an {@code int}.
	 */
	public int getAtkLevel2()
	{
		return _atkLevel2;
	}
	
	/**
	 * Retrieves the attack power at level 3.
	 * @return The attack power as an {@code int}.
	 */
	public int getAtkLevel3()
	{
		return _atkLevel3;
	}
	
	/**
	 * Retrieves the attack power at level 4.
	 * @return The attack power as an {@code int}.
	 */
	public int getAtkLevel4()
	{
		return _atkLevel4;
	}
	
	/**
	 * Retrieves the attack power at level 5.
	 * @return The attack power as an {@code int}.
	 */
	public int getAtkLevel5()
	{
		return _atkLevel5;
	}
	
	/**
	 * Retrieves the defense power at level 1.
	 * @return The defense power as an {@code int}.
	 */
	public int getDefLevel1()
	{
		return _defLevel1;
	}
	
	/**
	 * Retrieves the defense power at level 2.
	 * @return The defense power as an {@code int}.
	 */
	public int getDefLevel2()
	{
		return _defLevel2;
	}
	
	/**
	 * Retrieves the defense power at level 3.
	 * @return The defense power as an {@code int}.
	 */
	public int getDefLevel3()
	{
		return _defLevel3;
	}
	
	/**
	 * Retrieves the defense power at level 4.
	 * @return The defense power as an {@code int}.
	 */
	public int getDefLevel4()
	{
		return _defLevel4;
	}
	
	/**
	 * Retrieves the defense power at level 5.
	 * @return The defense power as an {@code int}.
	 */
	public int getDefLevel5()
	{
		return _defLevel5;
	}
	
	/**
	 * Retrieves the experience required to reach level 2.
	 * @return The required experience as an {@code int}.
	 */
	public int getExpToLevel2()
	{
		return _expToLevel2;
	}
	
	/**
	 * Retrieves the experience required to reach level 3.
	 * @return The required experience as an {@code int}.
	 */
	public int getExpToLevel3()
	{
		return _expToLevel3;
	}
	
	/**
	 * Retrieves the experience required to reach level 4.
	 * @return The required experience as an {@code int}.
	 */
	public int getExpToLevel4()
	{
		return _expToLevel4;
	}
	
	/**
	 * Retrieves the experience required to reach level 5.
	 * @return The required experience as an {@code int}.
	 */
	public int getExpToLevel5()
	{
		return _expToLevel5;
	}
	
	/**
	 * Retrieves the experience required to reach level 6.
	 * @return The required experience as an {@code int}.
	 */
	public int getExpToLevel6()
	{
		return _expToLevel6;
	}
	
	/**
	 * Retrieves the critical hit rate.
	 * @return The critical hit rate as an {@code int}.
	 */
	public int getCritRate()
	{
		return _critRate;
	}
}
