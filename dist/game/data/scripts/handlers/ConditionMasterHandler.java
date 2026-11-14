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
package handlers;

import org.l2jmobius.gameserver.handler.ConditionHandler;

import handlers.conditions.CategoryTypeCondition;
import handlers.conditions.NpcLevelCondition;
import handlers.conditions.PlayerLevelCondition;

/**
 * This class serves as the primary manager for processing various game conditions.<br>
 * It coordinates how {@code CategoryTypeCondition}, {@code NpcLevelCondition}, and {@code PlayerLevelCondition} are evaluated.
 * @author Sdw
 */
public class ConditionMasterHandler
{
	/**
	 * This is the main entry point for the {@code ConditionMasterHandler} class.<br>
	 * It registers various condition handlers into the system.<br>
	 * These include {@code CategoryType}, {@code NpcLevel}, and {@code PlayerLevel}.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		ConditionHandler.getInstance().registerHandler("CategoryType", CategoryTypeCondition::new);
		ConditionHandler.getInstance().registerHandler("NpcLevel", NpcLevelCondition::new);
		ConditionHandler.getInstance().registerHandler("PlayerLevel", PlayerLevelCondition::new);
	}
}
