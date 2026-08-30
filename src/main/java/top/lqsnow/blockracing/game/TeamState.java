package top.lqsnow.blockracing.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import top.lqsnow.blockracing.core.TeamColor;

/** Team membership of one game instance. Holds names only; scoreboard
 * registration stays with the scoreboard layer. */
public final class TeamState {
    private final List<String> red = new CopyOnWriteArrayList<>();
    private final List<String> blue = new CopyOnWriteArrayList<>();

    /** @return true if the player changed membership. */
    public boolean add(String playerName, TeamColor team) {
        List<String> target = players(team);
        if (target.contains(playerName)) {
            return false;
        }
        players(team.opponent()).remove(playerName);
        target.add(playerName);
        return true;
    }

    public boolean remove(String playerName) {
        boolean removed = red.remove(playerName);
        removed |= blue.remove(playerName);
        return removed;
    }

    public boolean contains(String playerName) {
        return red.contains(playerName) || blue.contains(playerName);
    }

    public Optional<TeamColor> teamOf(String playerName) {
        if (red.contains(playerName)) {
            return Optional.of(TeamColor.RED);
        }
        if (blue.contains(playerName)) {
            return Optional.of(TeamColor.BLUE);
        }
        return Optional.empty();
    }

    public List<String> players(TeamColor team) {
        return team == TeamColor.RED ? red : blue;
    }

    public List<String> onlinePlayers(TeamColor team) {
        List<String> online = new ArrayList<>();
        for (String name : players(team)) {
            if (Bukkit.getPlayerExact(name) != null) {
                online.add(name);
            }
        }
        return online;
    }

    public boolean hasEmptyTeam() {
        return red.isEmpty() || blue.isEmpty();
    }

    public void restore(List<String> redPlayers, List<String> bluePlayers) {
        red.clear();
        blue.clear();
        red.addAll(redPlayers);
        blue.addAll(bluePlayers);
    }

    public void clear() {
        red.clear();
        blue.clear();
    }
}
