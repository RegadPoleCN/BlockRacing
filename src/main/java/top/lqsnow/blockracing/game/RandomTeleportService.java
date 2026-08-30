package top.lqsnow.blockracing.game;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.bukkit.Location;

/** Server-wide pool of pre-rolled random teleport candidates, shared by all
 * game instances (was Game.randomTpPool). */
public final class RandomTeleportService {
    private static final Deque<Location> POOL = new ArrayDeque<>();

    private RandomTeleportService() {
    }

    public static synchronized void addCandidate(Location location) {
        if (location != null) {
            POOL.addLast(location);
        }
    }

    public static synchronized Location pollCandidate() {
        return POOL.pollFirst();
    }

    public static synchronized int poolSize() {
        return POOL.size();
    }

    public static synchronized List<Location> snapshot() {
        return List.copyOf(POOL);
    }
}
