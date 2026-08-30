package top.lqsnow.blockracing.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import top.lqsnow.blockracing.game.RoomSettings;
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
        GameInstance game = new GameInstance(nextId, sessions, RoomSettings.fromConfig());
        games.put(nextId, game);
        nextId++;
        return game;
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
