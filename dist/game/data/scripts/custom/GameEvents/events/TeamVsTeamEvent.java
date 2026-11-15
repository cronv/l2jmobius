package custom.GameEvents.events;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Team; // +
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;

import custom.GameEvents.AbstractGameEvent;
import custom.GameEvents.EventState;
import custom.GameEvents.GameEventType;
import custom.GameEvents.schedule.EveryHours;

public class TeamVsTeamEvent extends AbstractGameEvent
{
	// Подставь координаты своей арены.
	private static final int[] BLUE_SPAWN = { 148688, 46728, -3416 };
	private static final int[] RED_SPAWN = { 149384, 46728, -3416 };
	private static final int[] EXIT_SPAWN = { 83437, 148634, -3403 }; // Giran

	private static final int PLACE_DRAW = 0;
	private static final int PLACE_WIN = 1;
	private static final int PLACE_LOSE = 2;

	private final Set<Integer> _blue = ConcurrentHashMap.newKeySet();    // живые синие
	private final Set<Integer> _red = ConcurrentHashMap.newKeySet();     // живые красные
	private final Set<Integer> _blueAll = ConcurrentHashMap.newKeySet(); // полный состав (для наград)
	private final Set<Integer> _redAll = ConcurrentHashMap.newKeySet();
	private final AtomicInteger _blueKills = new AtomicInteger();
	private final AtomicInteger _redKills = new AtomicInteger();

	private ConsumerEventListener _deathListener;
	private ConsumerEventListener _logoutListener;

	public TeamVsTeamEvent()
	{
		super(GameEventType.TEAM_VS_TEAM);
		_schedule = new EveryHours(2, 13);       // каждые 2 часа в :13 → 00:13, 02:13, ... 22:13
		_registrationTimeMs = 10 * 60 * 1000L;   // регистрация за 10 минут до старта
		_eventTimeMs = 10 * 60 * 1000L;
		_countdownMinutes = new int[] { 10, 5, 1 };
		_minPlayers = 2;
		_maxPlayers = 40;

		// Пример наград (раскомментируй и настрой): победа 1 млн адены, поражение/ничья 100к.
		// setRewardPolicy((player, event, place) -> player.addItem("TvT", 57, (place == PLACE_WIN) ? 1_000_000 : 100_000, player, true));
	}

	@Override
	protected void onBegin(List<Player> participants)
	{
		_blue.clear();
		_red.clear();
		_blueAll.clear();
		_redAll.clear();
		_blueKills.set(0);
		_redKills.set(0);

		int index = 0;

		for (Player player : participants)
		{
			if ((index++ % 2) == 0)
			{
				_blue.add(player.getObjectId());
				_blueAll.add(player.getObjectId());
				player.setTeam(Team.BLUE);
				player.teleToLocation(BLUE_SPAWN[0], BLUE_SPAWN[1], BLUE_SPAWN[2]);
			}
			else
			{
				_red.add(player.getObjectId());
				_redAll.add(player.getObjectId());
				player.setTeam(Team.RED);
				player.teleToLocation(RED_SPAWN[0], RED_SPAWN[1], RED_SPAWN[2]);
			}
			player.broadcastUserInfo();
			player.sendPacket(new ExSendUIEvent(player, false, false, 0, (int) (_eventTimeMs / 1000), TIMER_TEXT)); // ← таймер сверху
		}

		// Листенеры живут только во время ивента — вне TvT оверхеда нет.
		_deathListener = new ConsumerEventListener(Containers.Global(), EventType.ON_CREATURE_DEATH, (OnCreatureDeath event) -> onDeath(event), this);
		Containers.Global().addListener(_deathListener);
		_logoutListener = new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_LOGOUT, (OnPlayerLogout event) -> onLogout(event), this);
		Containers.Global().addListener(_logoutListener);

		World.broadcastToAllOnlinePlayers("[TvT] Started! " + participants.size() + " players: " + _blue.size() + " blue vs " + _red.size() + " red.");
	}

	private void onDeath(OnCreatureDeath event)
	{
		if (getState() != EventState.RUNNING)
		{
			return;
		}
		final Creature target = event.getTarget();
		if (!target.isPlayable())
		{
			return; // умер моб — нам не интересно (самый частый случай, выходим рано)
		}
		final int victimId = target.asPlayer().getObjectId();
		final boolean isBlue = _blue.remove(victimId);
		final boolean isRed = !isBlue && _red.remove(victimId);
		if (!isBlue && !isRed)
		{
			return; // умер не участник (или уже выбыл)
		}

		// Очко — только если убийца игрок ВРАЖЕСКОЙ команды.
		final Creature attacker = event.getAttacker();
		if ((attacker != null) && attacker.isPlayable())
		{
			final int killerId = attacker.asPlayer().getObjectId();
			if (isBlue && _red.contains(killerId))
			{
				_redKills.incrementAndGet();
			}
			else if (isRed && _blue.contains(killerId))
			{
				_blueKills.incrementAndGet();
			}
		}

		World.broadcastToAllOnlinePlayersOnScreen("[TvT] " + _blueKills.get() + " : " + _redKills.get());

		// Вылет с арены: через 3 сек (анимация смерти), оживёт уже в городе.
		ThreadPool.schedule(() ->
		{
			final Player player = World.getInstance().getPlayer(victimId);
			if (player != null)
			{
				player.sendPacket(new ExSendUIEvent(player)); // ← скрыть таймер выбывшему
				player.setTeam(Team.NONE);
				player.broadcastUserInfo();
				player.teleToLocation(EXIT_SPAWN[0], EXIT_SPAWN[1], EXIT_SPAWN[2]);
			}
		}, 3000);

		checkTeamWiped();
	}

	private void onLogout(OnPlayerLogout event)
	{
		if (getState() != EventState.RUNNING)
		{
			return;
		}
		final int objectId = event.getPlayer().getObjectId();
		if (_blue.remove(objectId) | _red.remove(objectId))
		{
			checkTeamWiped();
		}
		// Круг сбрасывать не нужно: team не персистится, при релоге объект Player создаётся с Team.NONE.
	}

	/** Досрочная победа: одна из команд полностью выбыла. */
	private void checkTeamWiped()
	{
		if ((getState() == EventState.RUNNING) && (_blue.isEmpty() || _red.isEmpty()))
		{
			// Уходим из потока обработки смерти; guard в end() защитит от повторного вызова.
			ThreadPool.schedule(this::end, 1000);
		}
	}

	@Override
	protected void onEnd()
	{
		unregisterListeners();

		final int blueKills = _blueKills.get();
		final int redKills = _redKills.get();

		// 1) выжившие, 2) очки, 3) ничья
		final int result;
		if (!_blue.isEmpty() && _red.isEmpty())
		{
			result = PLACE_WIN;
		}
		else if (!_red.isEmpty() && _blue.isEmpty())
		{
			result = PLACE_LOSE;
		}
		else if (blueKills > redKills)
		{
			result = PLACE_WIN;
		}
		else if (redKills > blueKills)
		{
			result = PLACE_LOSE;
		}
		else
		{
			result = PLACE_DRAW;
		}

		final String message = switch (result)
		{
			case PLACE_WIN -> "[TvT] Победа синих! Счёт " + blueKills + ":" + redKills + ".";
			case PLACE_LOSE -> "[TvT] Победа красных! Счёт " + blueKills + ":" + redKills + ".";
			default -> "[TvT] Ничья! Счёт " + blueKills + ":" + redKills + ".";
		};
		World.broadcastToAllOnlinePlayers(message);
		World.broadcastToAllOnlinePlayersOnScreen(message);

		// Награды всему составу (включая погибших), телепорт — только живым.
		for (int objectId : _blueAll)
		{
			finishPlayer(objectId, (result == PLACE_DRAW) ? PLACE_DRAW : ((result == PLACE_WIN) ? PLACE_WIN : PLACE_LOSE), _blue.contains(objectId));
		}
		for (int objectId : _redAll)
		{
			finishPlayer(objectId, (result == PLACE_DRAW) ? PLACE_DRAW : ((result == PLACE_LOSE) ? PLACE_WIN : PLACE_LOSE), _red.contains(objectId));
		}

		_blue.clear();
		_red.clear();
		_blueAll.clear();
		_redAll.clear();
	}

	private void finishPlayer(int objectId, int place, boolean alive)
	{
		final Player player = World.getInstance().getPlayer(objectId);
		if (player == null)
		{
			return;
		}
		player.sendPacket(new ExSendUIEvent(player)); // ← скрыть таймер (в т.ч. при досрочной победе)
		player.setTeam(Team.NONE);
		player.broadcastUserInfo();
		if (alive)
		{
			player.teleToLocation(EXIT_SPAWN[0], EXIT_SPAWN[1], EXIT_SPAWN[2]);
		}
		getRewardPolicy().reward(player, this, place);
	}

	private void unregisterListeners()
	{
		if (_deathListener != null)
		{
			_deathListener.unregisterMe();
			_deathListener = null;
		}
		if (_logoutListener != null)
		{
			_logoutListener.unregisterMe();
			_logoutListener = null;
		}
	}
}
