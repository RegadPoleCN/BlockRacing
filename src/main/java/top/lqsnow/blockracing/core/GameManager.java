package top.lqsnow.blockracing.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;

import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.game.RoomSettings;
import top.lqsnow.blockracing.managers.Config;
import top.lqsnow.blockracing.session.SessionManager;

/** Registry of running game instances. During the transition there is exactly
 * one instance; {@link #first()} keeps the old global-singleton call sites
 * compiling until the facades are removed. */
public final class GameManager {
    private static GameManager instance;

    private final Map<Integer, GameInstance> games = new LinkedHashMap<>();
    private final SessionManager sessions = new SessionManager();
    private int nextId = 1;

    private GameManager() {
    }

    public static void init() {
        if (instance == null) {
            instance = new GameManager();
        }
    }

    public static GameManager get() {
        if (instance == null) {
            throw new IllegalStateException("GameManager not initialized");
        }
        return instance;
    }

    /** The (currently single) game instance, for the legacy facades. */
    public static GameInstance first() {
        return get().games.values().iterator().next();
    }

    public GameInstance createGame() {
        return createGame(nextId);
    }

    /** Creates a game with a specific id (used by crash recovery). */
    public GameInstance createGame(int id) {
        GameInstance game = new GameInstance(id, sessions, RoomSettings.fromConfig());
        games.put(id, game);
        nextId = Math.max(nextId, id + 1);
        return game;
    }

    /** A waiting game with room left, or a freshly created one. */
    public GameInstance findJoinable() {
        for (GameInstance game : games.values()) {
            if (game.getState() == RoomState.WAITING
                    && game.playerCount() < Config.MAX_PLAYERS.getInt()) {
                return game;
            }
        }
        return createGame();
    }

    /** Keeps at least idle-rooms empty waiting games available. */
    public void maintainIdleRooms() {
        int idleRooms = Math.max(1, Config.IDLE_ROOMS.getInt());
        long emptyWaiting = games.values().stream()
                .filter(game -> game.getState() == RoomState.WAITING && game.playerCount() == 0)
                .count();
        for (long i = emptyWaiting; i < idleRooms; i++) {
            createGame();
        }
    }

    /** Force-ends games that have been running longer than the configured limit. */
    public void startWatchdog() {
        long maxMillis = Math.max(1, Config.MAX_GAME_MINUTES.getInt()) * 60_000L;
        Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            for (GameInstance game : games.values()) {
                if (game.getState() == RoomState.RUNNING
                        && System.currentTimeMillis() - game.getStartedAt() > maxMillis) {
                    game.forceEnd();
                }
            }
        }, 400L, 400L);
    }

    /** Ends the "game over" display phase after 10 seconds and recycles the arena. */
    public void scheduleDispose(GameInstance game) {
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), game::dispose, 200L);
    }

    public Optional<GameInstance> byId(int id) {
        return Optional.ofNullable(games.get(id));
    }

    public void remove(GameInstance game) {
        games.remove(game.getId());
    }

    public Collection<GameInstance> all() {
        return java.util.Collections.unmodifiableCollection(games.values());
    }

    public SessionManager sessions() {
        return sessions;
    }
}
