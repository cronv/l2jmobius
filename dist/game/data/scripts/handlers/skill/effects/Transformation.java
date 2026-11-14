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
package handlers.skill.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for transformation effects.<br>
 * It manages how a creature changes its appearance or properties when affected by a {@code Skill}.
 * @author nBd
 */
public class Transformation extends AbstractEffect
{
	private final List<Integer> _id;
	
	/**
	 * Creates a new {@code Transformation} instance.<br>
	 * This constructor initializes the transformation using provided parameters.<br>
	 * It parses the transformation IDs from the {@code StatSet}.
	 * @param params The {@code StatSet} containing configuration data.
	 */
	public Transformation(StatSet params)
	{
		final String ids = params.getString("transformationId", null);
		if ((ids != null) && !ids.isEmpty())
		{
			_id = new ArrayList<>();
			for (String id : ids.split(";"))
			{
				_id.add(Integer.parseInt(id));
			}
		}
		else
		{
			_id = Collections.emptyList();
		}
	}
	
	/**
	 * Checks if the transformation effect can be applied to a target.<br>
	 * This method returns {@code false} if the target is a door.<br>
	 * Otherwise, it returns {@code true}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the effect.
	 * @return {@code true} if the effect can start, {@code false} otherwise.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return !effected.isDoor();
	}
	
	/**
	 * This method is called when the transformation effect begins.<br>
	 * It applies a random transformation to the {@code effected} creature.<br>
	 * The transformation ID is selected from the internal list of IDs.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the transformation.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} involved in the action, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!_id.isEmpty())
		{
			effected.transform(_id.get(Rnd.get(_id.size())).intValue(), true);
		}
	}
	
	/**
	 * This method is called when a transformation effect ends.<br>
	 * It handles the logic for removing the transformation from the target.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature who was affected by the skill.
	 * @param skill The {@code Skill} used to trigger the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.stopTransformation(false);
	}
}
