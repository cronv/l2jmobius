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
package handlers.actions.click;

import org.l2jmobius.gameserver.data.xml.ClanHallData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.holders.creature.DoorRequestHolder;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.residences.ClanHall;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.IActionClickHandler;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;

/**
 * Handles the interaction logic when a player clicks on a {@code Door} object.<br>
 * It manages opening, closing, and interacting with doors within the game world.
 * @author Mobius
 */
public class DoorClick implements IActionClickHandler
{
	/**
	 * Handles the logic when a {@code Player} interacts with a {@code WorldObject}.<br>
	 * This method manages targeting, attacking, and opening doors based on clan ownership.
	 * @param player The {@code Player} who is performing the action.
	 * @param target The {@code WorldObject} being interacted with.
	 * @param interact A boolean indicating if the player is currently interacting with the object.
	 * @return Always returns {@code true}.
	 */
	@Override
	public boolean onAction(Player player, WorldObject target, boolean interact)
	{
		// Check if the Player already target the Npc.
		if (player.getTarget() != target)
		{
			player.setTarget(target);
		}
		else if (interact)
		{
			final Door door = target.asDoor();
			
			// MyTargetSelected my = new MyTargetSelected(getObjectId(), player.getLevel());
			// player.sendPacket(my);
			
			if (door.isAutoAttackable(player))
			{
				if (Math.abs(player.getZ() - door.getZ()) <= door.getHeight())
				{
					if (GeoEngine.getInstance().canSeeTarget(player, door))
					{
						player.getAI().setIntentionAttack(door);
					}
					else
					{
						player.sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
					}
				}
			}
			else
			{
				final Clan clan = player.getClan();
				if (clan != null)
				{
					final ClanHall clanHall = ClanHallData.getInstance().getClanHallByDoorId(door.getId());
					if ((clanHall != null) && (player.getClanId() == clanHall.getOwnerId()))
					{
						if (!door.isInsideRadius2D(player, Npc.INTERACTION_DISTANCE))
						{
							player.getAI().setIntentionInteract(door);
						}
						else // if (!clanHall.isSiegableHall() || !((SiegableHall) clanHall).isInSiege())
						{
							player.addScript(new DoorRequestHolder(door));
							
							if (!door.isOpen())
							{
								player.sendPacket(new ConfirmDlg(SystemMessageId.WOULD_YOU_LIKE_TO_OPEN_THE_GATE.getId()));
							}
							else
							{
								player.sendPacket(new ConfirmDlg(SystemMessageId.WOULD_YOU_LIKE_TO_CLOSE_THE_GATE.getId()));
							}
						}
					}
					else
					{
						final Fort fort = door.getFort();
						if ((fort != null) && (clan == fort.getOwnerClan()) && door.isOpenableBySkill() && !fort.getSiege().isInProgress())
						{
							if (!door.isInsideRadius2D(player, Npc.INTERACTION_DISTANCE))
							{
								player.getAI().setIntentionInteract(door);
							}
							else
							{
								player.addScript(new DoorRequestHolder(door));
								
								if (!door.isOpen())
								{
									player.sendPacket(new ConfirmDlg(SystemMessageId.WOULD_YOU_LIKE_TO_OPEN_THE_GATE.getId()));
								}
								else
								{
									player.sendPacket(new ConfirmDlg(SystemMessageId.WOULD_YOU_LIKE_TO_CLOSE_THE_GATE.getId()));
								}
							}
						}
					}
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Retrieves the type of instance associated with this action.<br>
	 * This method returns a constant value from the {@code InstanceType} enum.
	 * @return the {@code InstanceType} for this door click.
	 */
	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.Door;
	}
}
