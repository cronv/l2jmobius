package custom.GameEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * Template Method: register/unregister/start/end — final, наследники пишут только on* хуки.
 * Память: храним ТОЛЬКО objectId (int) в примитивном IntSet — никаких ссылок на Player (нет утечек).
 * CPU: никаких тикающих задач — только разовые ThreadPool.schedule на переходах состояний.
 */
public abstract class AbstractGameEvent implements IGameEvent
{
	private final GameEventType _type;
	private final Set<Integer> _participants = ConcurrentHashMap.newKeySet();

	private volatile EventState _state = EventState.INACTIVE;
	private volatile boolean _active; // тот самый bool active: читается без блокировок
	private volatile IParticipationPolicy _participationPolicy = IParticipationPolicy.DEFAULT;
	private volatile IRewardPolicy _rewardPolicy = IRewardPolicy.NONE;

	private ScheduledFuture<?> _startTask;
	private ScheduledFuture<?> _endTask;
	private ScheduledFuture<?> _nextCycleTask;

	// Тюнинг переопределяется в конструкторе наследника (или вынеси в properties).
	protected int _minLevel = 40;
	protected int _maxLevel = 130;
	protected int _minPlayers = 2;
	protected int _maxPlayers = 100;
	protected long _registrationTimeMs = 10 * 60 * 1000L;
	protected long _eventTimeMs = 10 * 60 * 1000L;
	protected long _cyclePeriodMs = 2 * 60 * 60 * 1000L; // 0 = одноразовый ивент
	protected long _initialDelayMs = 60 * 1000L;

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

	protected final IRewardPolicy getRewardPolicy()
	{
		return _rewardPolicy;
	}

	// ---------------- Жизненный цикл (все переходы под одним монитором) ----------------

	public final void openRegistration()
	{
		synchronized (this)
		{
			if (_state != EventState.INACTIVE)
			{
				return;
			}
			_participants.clear();
			setState(EventState.REGISTERING);
			_startTask = ThreadPool.schedule(this::start, _registrationTimeMs);
			onRegistrationOpened();
		}
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
			if (!_participants.add(player.getObjectId())) // проверка+вставка атомарны под锁
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

	/** Вызывается менеджером на logout: снимаем регистрацию, не держа ссылок на Player. */
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
			// Резолвим Player только сейчас, из World (онлайн). Оффлайн — выкидываем.
			final int[] ids = _participants.stream().mapToInt(Integer::intValue).toArray();
			final List<Player> players = new ArrayList<>(ids.length);
			for (int objectId : ids)
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
				World.broadcastToAllOnlinePlayers("[Events] " + getDisplayName() + " cancelled: not enough participants.");
				_participants.clear();
				setState(EventState.INACTIVE);
				scheduleNextCycle();
				onCancelled();
				return;
			}
			setState(EventState.RUNNING);
			_endTask = ThreadPool.schedule(this::end, _eventTimeMs);
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
			onEnd(); // наследник сам телепортит/награждает, пока знает участников
			_participants.clear(); // освобождаем память сразу
			setState(EventState.INACTIVE);
			scheduleNextCycle();
		}
	}

	/** Ручная остановка (админ-команда, shutdown). */
	public final void shutdown()
	{
		synchronized (this)
		{
			cancel(_startTask);
			cancel(_endTask);
			cancel(_nextCycleTask);
			if (_state == EventState.RUNNING)
			{
				onEnd();
			}
			_participants.clear();
			setState(EventState.INACTIVE);
		}
	}

	final void scheduleFirstCycle()
	{
		_nextCycleTask = ThreadPool.schedule(this::openRegistration, _initialDelayMs);
	}

	private void scheduleNextCycle()
	{
		if (_cyclePeriodMs > 0)
		{
			_nextCycleTask = ThreadPool.schedule(this::openRegistration, _cyclePeriodMs);
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

	// ---------------- Хуки для наследников ----------------

	protected void onRegistrationOpened()
	{
		World.broadcastToAllOnlinePlayers("[Events] " + getDisplayName() + ": registration is open!");
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
