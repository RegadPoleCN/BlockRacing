package top.lqsnow.blockracing.game;

import java.util.ArrayList;
import java.util.List;

import top.lqsnow.blockracing.core.TeamColor;

/** Per-arena display lists of target blocks, mirrored from the arena's
 * TaskSession. Generation itself lives in the pools engine. */
public final class BlockState {
    private final List<String> redAll = new ArrayList<>();
    private final List<String> blueAll = new ArrayList<>();
    private final List<String> redRemaining = new ArrayList<>();
    private final List<String> blueRemaining = new ArrayList<>();

    /** Replaces the lists for one team; used when syncing from the session. */
    public void install(TeamColor team, List<String> all, List<String> remaining) {
        List<String> allList = all(team);
        allList.clear();
        allList.addAll(all);
        List<String> remainingList = remaining(team);
        remainingList.clear();
        remainingList.addAll(remaining);
    }

    public List<String> all(TeamColor team) {
        return team == TeamColor.RED ? redAll : blueAll;
    }

    public List<String> remaining(TeamColor team) {
        return team == TeamColor.RED ? redRemaining : blueRemaining;
    }

    /** The blocks currently hunted by the team (up to 4). */
    public List<String> current(TeamColor team) {
        List<String> remaining = remaining(team);
        return remaining.subList(0, Math.min(remaining.size(), 4));
    }

    public void complete(TeamColor team, String block) {
        remaining(team).remove(block);
    }
}
