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
package handlers.chat.commands.admin;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;

/**
 * This class provides a test command for administrators.<br>
 * It allows developers to verify the functionality of admin commands within the game environment.
 */
public class AdminTest implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_skill_test"
	};
	
	/**
	 * Processes administrative commands sent by a player.<br>
	 * This method checks if the {@code command} starts with {@code admin_skill_test}.<br>
	 * It parses the input and executes the corresponding logic for the {@code activeChar}.
	 * @param command The raw string input from the player.
	 * @param activeChar The {@code Player} object who sent the command.
	 * @return {@code true} if the command was handled successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_skill_test"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command);
				st.nextToken();
				final int id = Integer.parseInt(st.nextToken());
				adminTestSkill(activeChar, id, command.startsWith("admin_skill_test"));
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Command format is //skill_test <ID>");
			}
		}
		
		return true;
	}
	
	/**
	 * Tests a specific skill for an administrator.<br>
	 * This method determines the caster based on whether the player has a target.<br>
	 * It then executes the skill using either a packet broadcast or a direct cast.
	 * @param activeChar The {@code Player} who initiates the command.
	 * @param id The unique identifier of the skill to test.
	 * @param msu A boolean flag indicating if the skill should be sent as a {@code MagicSkillUse} packet.
	 */
	private void adminTestSkill(Player activeChar, int id, boolean msu)
	{
		Creature caster;
		final WorldObject target = activeChar.getTarget();
		if (!target.isCreature())
		{
			caster = activeChar;
		}
		else
		{
			caster = target.asCreature();
		}
		
		final Skill skill = SkillData.getInstance().getSkill(id, 1);
		if (skill != null)
		{
			caster.setTarget(activeChar);
			if (msu)
			{
				caster.broadcastPacket(new MagicSkillUse(caster, activeChar, id, 1, skill.getHitTime(), skill.getReuseDelay()));
			}
			else
			{
				caster.doCast(skill);
			}
		}
	}
	
	/**
	 * Retrieves the list of available administrative commands.<br>
	 * This method returns all commands registered for this handler.
	 * @return An array of {@code String} objects containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
