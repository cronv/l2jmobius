package custom.GameEvents.events;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;

import custom.GameEvents.AbstractGameEvent;
import custom.GameEvents.GameEventType;

public class TeamVsTeamEvent extends AbstractGameEvent
{
	// Подставь координаты своей арены.
	private static final int[] BLUE_SPAWN = { 148688, 46728, -3416 };
	private static final int[] RED_SPAWN = { 149384, 46728, -3416 };
	private static final int[] EXIT_SPAWN = { 83437, 148634, -3403 }; // Giran

	private final Set<Integer> _blue = ConcurrentHashMap.newKeySet();
	private final Set<Integer> _red = ConcurrentHashMap.newKeySet();

	public TeamVsTeamEvent()
	{
		super(GameEventType.TEAM_VS_TEAM);
		_minPlayers = 4;
		_maxPlayers = 40;
		_registrationTimeMs = 5 * 60 * 1000L;
		_eventTimeMs = 10 * 60 * 1000L;
	}

	@Override
	protected void onBegin(List<Player> participants)
	{
		_blue.clear();
		_red.clear();
		int index = 0;
		for (Player player : participants)
		{
			if ((index++ % 2) == 0)
			{
				_blue.add(player.getObjectId());
				player.teleToLocation(BLUE_SPAWN[0], BLUE_SPAWN[1], BLUE_SPAWN[2]);
			}
			else
			{
				_red.add(player.getObjectId());
				player.teleToLocation(RED_SPAWN[0], RED_SPAWN[1], RED_SPAWN[2]);
			}
		}
		World.broadcastToAllOnlinePlayers("[TvT] Started! " + participants.size() + " players.");
		// TODO: скоринг через листенер ON_PLAYER_PVP_KILL
	}

	@Override
	protected void onEnd()
	{
		// TODO: определить победителей по очкам; place = 1 победа / 2 поражение
		for (int objectId : _blue)
		{
			finishPlayer(objectId, 1);
		}
		for (int objectId : _red)
		{
			finishPlayer(objectId, 2);
		}
		_blue.clear();
		_red.clear();
	}

	private void finishPlayer(int objectId, int place)
	{
		final Player player = World.getInstance().getPlayer(objectId);
		if (player != null)
		{
			player.teleToLocation(EXIT_SPAWN[0], EXIT_SPAWN[1], EXIT_SPAWN[2]);
			getRewardPolicy().reward(player, this, place);
		}
	}
}
