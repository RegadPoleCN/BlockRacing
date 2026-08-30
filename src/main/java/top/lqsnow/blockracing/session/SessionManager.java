package top.lqsnow.blockracing.session;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import top.lqsnow.blockracing.core.GameInstance;

/** Tracks which game instance each player belongs to, plus transient
 * per-player session state (chat-based block amount editing). Keys are
 * player names, matching the rest of the codebase. */
public final class SessionManager {
    private final Map<String, GameInstance> inArena = new ConcurrentHashMap<>();
    private final Set<String> editAmountPlayers = ConcurrentHashMap.newKeySet();

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

    /** Players currently typing a block amount in chat (was BasicListener.editAmountPlayer). */
    public Set<String> getEditAmountPlayers() {
        return editAmountPlayers;
    }
}
