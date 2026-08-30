package top.lqsnow.blockracing.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import top.lqsnow.blockracing.core.GameInstance;
/** Tracks which game instance each player belongs to, plus transient
 * per-player session state (chat-based block amount editing). Keys are
 * player names, matching the rest of the codebase. */
public final class SessionManager {
    private final Map<String, GameInstance> inArena = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerSession> savedSessions = new ConcurrentHashMap<>();
    private final Set<String> editAmountPlayers = ConcurrentHashMap.newKeySet();

    /** Snapshot taken before a player enters a game. */
    public void saveSession(Player player) {
        savedSessions.put(player.getUniqueId(), PlayerSession.snapshot(player));
    }

    /** Returns and removes the saved session, if any. */
    public PlayerSession takeSavedSession(UUID playerId) {
        return savedSessions.remove(playerId);
    }

    public boolean hasSavedSession(UUID playerId) {
        return savedSessions.containsKey(playerId);
    }

    public Optional<GameInstance> arenaOf(Player player) {
        return Optional.ofNullable(inArena.get(player.getName()));
    }

    public Optional<GameInstance> arenaOf(String playerName) {
        return Optional.ofNullable(inArena.get(playerName));
    }

    public void enterArena(Player player, GameInstance arena) {
        inArena.put(player.getName(), arena);
    }

    public void leaveArena(Player player) {
        inArena.remove(player.getName());
    }

    public void leaveArena(String playerName) {
        inArena.remove(playerName);
    }

    public Collection<GameInstance> occupiedArenas() {
        return new java.util.LinkedHashSet<>(inArena.values());
    }

    /** All player names currently bound to the given arena (online or not). */
    public List<String> boundPlayers(GameInstance arena) {
        List<String> names = new ArrayList<>();
        inArena.forEach((name, bound) -> {
            if (bound == arena) {
                names.add(name);
            }
        });
        return names;
    }

    /** Players currently typing a block amount in chat (was BasicListener.editAmountPlayer). */
    public Set<String> getEditAmountPlayers() {
        return editAmountPlayers;
    }
}
