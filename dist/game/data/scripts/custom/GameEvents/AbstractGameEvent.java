package custom.GameEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;

import custom.GameEvents.announce.EventMessages;
import custom.GameEvents.announce.IEventAnnouncer;
import custom.GameEvents.schedule.EveryHours;
import custom.GameEvents.schedule.IEventSchedule;

/**
 * Template Method. Расписание (IEventSchedule) задаёт старты: каждые N минут / H часов / D дней.
 * Память: только objectId (Set<Integer>, CHM newKeySet: O(1), lock-free чтения).
 * CPU: только разовые задачи ThreadPool, без тиков.
 */
public abstract class AbstractGameEvent implements IGameEvent
{
	private static final ScheduledFuture<?>[] EMPTY_TASKS = new ScheduledFuture<?>[0];

	private final GameEventType _type;
	private final Set<Integer> _participants = ConcurrentHashMap.newKeySet();

	private volatile EventState _state = EventState.INACTIVE;
	private volatile boolean _active;
	private volatile long _nextStartMillis;
	private volatile IParticipationPolicy _participationPolicy = IParticipationPolicy.DEFAULT;
	private volatile IRewardPolicy _rewardPolicy = IRewardPolicy.NONE;
	private volatile IEventAnnouncer _announcer = IEventAnnouncer.DEFAULT;

	private ScheduledFuture<?> _openTask;
	private ScheduledFuture<?> _startTask;
	private ScheduledFuture<?> _endTask;
	private ScheduledFuture<?>[] _countdownTasks = EMPTY_TASKS;

	// Тюнинг в конструкторе наследника.
	protected IEventSchedule _schedule = new EveryHours(2, 0);
	protected long _registrationTimeMs = 10 * 60 * 1000L; // за сколько до старта открыть регистрацию
	protected long _eventTimeMs = 10 * 60 * 1000L;
	protected int[] _countdownMinutes = { 10, 5, 1 };
	protected int _minLevel = 40;
	protected int _maxLevel = 130;
	protected int _minPlayers = 2;
	protected int _maxPlayers = 100;
	protected static final String TIMER_TEXT = "До конца ивента";

	protected AbstractGameEvent(GameEventType type)
	{
		_type = type;
	}

	@Override
	public final GameEventType getType()
	{
		return _type;
	}

	@Override
	public final String getDisplayName()
	{
		return _type.getDisplayName();
	}

	@Override
	public final EventState getState()
	{
		return _state;
	}

	@Override
	public final boolean isActive()
	{
		return _active;
	}

	@Override
	public final IEventSchedule getSchedule()
	{
		return _schedule;
	}

	@Override
	public final long getNextStartMillis()
	{
		return _nextStartMillis;
	}

	@Override
	public final int getParticipantCount()
	{
		return _participants.size(); // O(1)
	}

	@Override
	public final int getMinParticipants()
	{
		return _minPlayers;
	}

	@Override
	public final int getMaxParticipants()
	{
		return _maxPlayers;
	}

	@Override
	public final int getMinLevel()
	{
		return _minLevel;
	}

	@Override
	public final int getMaxLevel()
	{
		return _maxLevel;
	}

	@Override
	public final boolean isRegistered(int objectId)
	{
		return _participants.contains(objectId); // O(1)
	}

	public final void setParticipationPolicy(IParticipationPolicy policy)
	{
		_participationPolicy = policy;
	}

	public final void setRewardPolicy(IRewardPolicy policy)
	{
		_rewardPolicy = policy;
	}

	public final void setAnnouncer(IEventAnnouncer announcer)
	{
		_announcer = announcer;
	}

	protected final IRewardPolicy getRewardPolicy()
	{
		return _rewardPolicy;
	}

	// ---------------- Жизненный цикл ----------------

	private void planNextCycle()
	{
		final long now = System.currentTimeMillis();
		_nextStartMillis = _schedule.nextStartAfter(now);
		final long delay = (_nextStartMillis - _registrationTimeMs) - now;
		if (delay <= 0)
		{
			openRegistration();
		}
		else
		{
			_openTask = ThreadPool.schedule(this::openRegistration, delay);
		}
	}

	final void scheduleFirstCycle()
	{
		synchronized (this)
		{
			planNextCycle();
		}
	}

	public final void openRegistration()
	{
		synchronized (this)
		{
			if (_state != EventState.INACTIVE)
			{
				return;
			}
			final long now = System.currentTimeMillis();
			if (_nextStartMillis <= now)
			{
				_nextStartMillis = _schedule.nextStartAfter(now); // ручной вызов админом
			}
			_participants.clear();
			setState(EventState.REGISTERING);
			_startTask = ThreadPool.schedule(this::start, _nextStartMillis - now);
			scheduleCountdowns(now);
			onRegistrationOpened();
		}
	}

	private void scheduleCountdowns(long now)
	{
		cancelCountdowns();
		final int[] points = _countdownMinutes;
		final ScheduledFuture<?>[] tasks = new ScheduledFuture<?>[points.length];
		for (int i = 0; i < points.length; i++)
		{
			final int minutes = points[i];
			final long delay = (_nextStartMillis - (minutes * 60_000L)) - now;
			if (delay > 0)
			{
				tasks[i] = ThreadPool.schedule(() -> _announcer.announce(EventMessages.countdown(getDisplayName(), minutes)), delay);
			}
		}
		_countdownTasks = tasks;
	}

	private void cancelCountdowns()
	{
		for (ScheduledFuture<?> task : _countdownTasks)
		{
			if (task != null)
			{
				task.cancel(false);
			}
		}
		_countdownTasks = EMPTY_TASKS;
	}

	@Override
	public final boolean register(Player player)
	{
		synchronized (this)
		{
			if (_state != EventState.REGISTERING)
			{
				player.sendMessage("Registration is closed.");
				return false;
			}
			if (_participants.size() >= _maxPlayers)
			{
				player.sendMessage("Event is full.");
				return false;
			}
			final String deny = _participationPolicy.check(player, this);
			if (deny != null)
			{
				player.sendMessage(deny);
				return false;
			}
			if (!_participants.add(player.getObjectId()))
			{
				player.sendMessage("You are already registered.");
				return false;
			}
			onRegistered(player);
		}
		GameEventManager.getInstance().fireParticipantsChanged(this);
		return true;
	}

	@Override
	public final boolean unregister(Player player)
	{
		synchronized (this)
		{
			if ((_state != EventState.REGISTERING) || !_participants.remove(player.getObjectId()))
			{
				return false;
			}
			onUnregistered(player);
		}
		GameEventManager.getInstance().fireParticipantsChanged(this);
		return true;
	}

	final boolean removeParticipant(int objectId)
	{
		synchronized (this)
		{
			if ((_state != EventState.REGISTERING) || !_participants.remove(objectId))
			{
				return false;
			}
		}
		GameEventManager.getInstance().fireParticipantsChanged(this);
		return true;
	}

	public final void start()
	{
		synchronized (this)
		{
			if (_state != EventState.REGISTERING)
			{
				return;
			}
			cancelCountdowns();
			_announcer.announce(EventMessages.registrationClosed(getDisplayName()));

			// Итератор CHM weakly-consistent: remove во время обхода безопасен.
			final List<Player> players = new ArrayList<>(_participants.size());
			for (int objectId : _participants)
			{
				final Player player = World.getInstance().getPlayer(objectId);
				if (player != null)
				{
					players.add(player);
				}
				else
				{
					_participants.remove(objectId);
				}
			}
			if (players.size() < _minPlayers)
			{
				_announcer.announce(EventMessages.cancelled(getDisplayName()));
				_participants.clear();
				setState(EventState.INACTIVE);
				planNextCycle();
				onCancelled();
				return;
			}
			setState(EventState.RUNNING);
			_endTask = ThreadPool.schedule(this::end, _eventTimeMs);
			_announcer.announce(EventMessages.started(getDisplayName()));
			onBegin(players);
		}
	}

	public final void end()
	{
		synchronized (this)
		{
			if (_state != EventState.RUNNING)
			{
				return;
			}
			setState(EventState.FINISHED);
			onEnd();
			_participants.clear();
			setState(EventState.INACTIVE);
			_announcer.announce(EventMessages.finished(getDisplayName()));
			planNextCycle();
		}
	}

	public final void shutdown()
	{
		synchronized (this)
		{
			cancel(_openTask);
			cancel(_startTask);
			cancel(_endTask);
			cancelCountdowns();
			if (_state == EventState.RUNNING)
			{
				onEnd();
			}
			_participants.clear();
			setState(EventState.INACTIVE);
		}
	}

	private void setState(EventState newState)
	{
		final EventState oldState = _state;
		_state = newState;
		_active = (newState == EventState.REGISTERING) || (newState == EventState.RUNNING);
		GameEventManager.getInstance().fireStateChanged(this, oldState, newState);
	}

	private static void cancel(ScheduledFuture<?> task)
	{
		if (task != null)
		{
			task.cancel(false);
		}
	}

	// ---------------- Хуки ----------------

	protected void onRegistrationOpened()
	{
		_announcer.announce(EventMessages.registrationOpen(getDisplayName(), IEventSchedule.time(_nextStartMillis)));
	}

	protected void onRegistered(Player player)
	{
	}

	protected void onUnregistered(Player player)
	{
	}

	protected void onCancelled()
	{
	}

	protected abstract void onBegin(List<Player> participants);

	protected abstract void onEnd();
}
