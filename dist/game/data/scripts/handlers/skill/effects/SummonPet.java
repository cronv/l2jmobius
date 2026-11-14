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

import java.util.logging.Level;

import org.l2jmobius.gameserver.data.holders.PetDataHolder;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.PetData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.holders.PetItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.PetItemList;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for the {@code SummonPet} skill effect.<br>
 * This class manages the creation and spawning of a pet entity based on provided data.<br>
 * It ensures the correct {@code Pet} instance is initialized and linked to the player.
 * @author UnAfraid
 */
public class SummonPet extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code SummonPet} effect.<br>
	 * This constructor initializes the effect using provided statistics.
	 * @param params The {@code StatSet} containing the required parameters.
	 */
	public SummonPet(StatSet params)
	{
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} for summoning a pet.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.SUMMON_PET;
	}
	
	/**
	 * Checks if the effect occurs immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect of summoning a pet.<br>
	 * This method validates requirements and spawns the pet for the player.
	 * @param effector The {@code Creature} who performs the action.
	 * @param effected The {@code Creature} who receives the effect.
	 * @param skill The {@code Skill} being used to trigger the effect.
	 * @param item The {@code Item} used as a collar to summon the pet.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effector.isPlayer() || !effected.isPlayer() || effected.isAlikeDead())
		{
			return;
		}
		
		final Player player = effector.asPlayer();
		
		if (player.hasPet() || player.isMounted())
		{
			player.sendPacket(SystemMessageId.YOU_ALREADY_HAVE_A_PET);
			return;
		}
		
		final PetItemHolder holder = player.removeScript(PetItemHolder.class);
		if (holder == null)
		{
			LOGGER.log(Level.WARNING, "Summoning pet without attaching PetItemHandler!", new Throwable());
			return;
		}
		
		final Item collar = holder.getItem();
		if (player.getInventory().getItemByObjectId(collar.getObjectId()) != collar)
		{
			LOGGER.warning("Player: " + player + " is trying to summon pet from item that he doesn't owns.");
			return;
		}
		
		final PetDataHolder petData = PetData.getInstance().getPetDataHolderByItemId(collar.getId());
		if ((petData == null) || (petData.getNpcId() == -1))
		{
			return;
		}
		
		final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(petData.getNpcId());
		final Pet pet = Pet.spawnPet(npcTemplate, player, collar);
		
		pet.setShowSummonAnimation(true);
		if (!pet.isRespawned())
		{
			pet.fullRestore();
			pet.getStat().setExp(pet.getExpForThisLevel());
			pet.setCurrentFed(pet.getMaxFed());
		}
		
		pet.setRunning();
		
		if (!pet.isRespawned())
		{
			pet.storeMe();
		}
		
		collar.setEnchantLevel(pet.getLevel());
		player.setPet(pet);
		pet.spawnMe(player.getX() + 50, player.getY() + 100, player.getZ());
		pet.startFeed();
		pet.setFollowStatus(true);
		pet.getOwner().sendPacket(new PetItemList(pet.getInventory().getItems()));
		pet.broadcastStatusUpdate();
	}
}
