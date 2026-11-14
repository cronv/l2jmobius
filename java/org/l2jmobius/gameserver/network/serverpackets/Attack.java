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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.creature.Hit;
import org.l2jmobius.gameserver.entity.item.enums.BroochJewel;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to notify it of an attack occurring in the game world.<br>
 * It contains details about the attacker, the target, and the resulting damage.
 * @author Mobius
 */
public class Attack extends ServerPacket
{
	private final int _attackerObjId;
	private final Location _attackerLoc;
	private final Location _targetLoc;
	private final List<Hit> _hits = new ArrayList<>();
	private final int _soulshotVisualSubstitute;
	
	/**
	 * Creates a new {@code Attack} packet.<br>
	 * This constructor initializes the attacker and target information.<br>
	 * It also determines the visual substitute for soulshots based on active jewels.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param target The {@code Creature} receiving the attack.
	 */
	public Attack(Creature attacker, Creature target)
	{
		_attackerObjId = attacker.getObjectId();
		_attackerLoc = new Location(attacker);
		_targetLoc = new Location(target);
		final Player player = attacker.asPlayer();
		if (player == null)
		{
			_soulshotVisualSubstitute = 0;
		}
		else
		{
			final BroochJewel activeRuby = player.getActiveRubyJewel();
			final BroochJewel activeSapphire = player.getActiveSapphireJewel();
			if (activeRuby != null)
			{
				_soulshotVisualSubstitute = activeRuby.getItemId();
			}
			else if (activeSapphire != null)
			{
				_soulshotVisualSubstitute = activeSapphire.getItemId();
			}
			else
			{
				_soulshotVisualSubstitute = 0;
			}
		}
	}
	
	/**
	 * Adds a new {@code Hit} to the list of hits.<br>
	 * This method updates the internal collection used for attack data.
	 * @param hit The {@code Hit} object to be added.
	 */
	public void addHit(Hit hit)
	{
		_hits.add(hit);
	}
	
	/**
	 * Retrieves the list of all hits associated with this attack.<br>
	 * This method returns the internal {@code List} of {@code Hit} objects.
	 * @return a {@code List} containing all {@code Hit} instances.
	 */
	public List<Hit> getHits()
	{
		return _hits;
	}
	
	/**
	 * Checks if the attack contains any hits.<br>
	 * Returns {@code true} if there is at least one hit in the list.<br>
	 * Returns {@code false} if the list is empty.
	 * @return a boolean indicating whether the attack has hits.
	 */
	public boolean hasHits()
	{
		return !_hits.isEmpty();
	}
	
	/**
	 * Writes the details of a single {@code Hit} into the provided buffer.<br>
	 * This method handles serializing target ID, damage, flags, and grade.
	 * @param hit The {@code Hit} object containing the data to write.
	 * @param buffer The {@code WriteBuffer} used to store the serialized data.
	 */
	private void writeHit(Hit hit, WriteBuffer buffer)
	{
		buffer.writeInt(hit.getTargetId());
		buffer.writeInt(hit.getDamage());
		buffer.writeInt(hit.getFlags());
		buffer.writeInt(hit.getGrade()); // GOD
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		final Iterator<Hit> it = _hits.iterator();
		final Hit firstHit = it.next();
		ServerPackets.ATTACK.writeId(this, buffer);
		buffer.writeInt(_attackerObjId);
		buffer.writeInt(firstHit.getTargetId());
		buffer.writeInt(_soulshotVisualSubstitute); // Ertheia
		buffer.writeInt(firstHit.getDamage());
		buffer.writeInt(firstHit.getFlags());
		buffer.writeInt(firstHit.getGrade()); // GOD
		buffer.writeInt(_attackerLoc.getX());
		buffer.writeInt(_attackerLoc.getY());
		buffer.writeInt(_attackerLoc.getZ());
		buffer.writeShort(_hits.size() - 1);
		while (it.hasNext())
		{
			writeHit(it.next(), buffer);
		}
		
		buffer.writeInt(_targetLoc.getX());
		buffer.writeInt(_targetLoc.getY());
		buffer.writeInt(_targetLoc.getZ());
	}
}
