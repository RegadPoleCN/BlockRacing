package top.lqsnow.blockracing.lobby;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import top.lqsnow.blockracing.managers.Config;
import top.lqsnow.blockracing.managers.Message;
import top.lqsnow.blockracing.managers.Scoreboard;
import top.lqsnow.blockracing.session.PlayerSession;
import top.lqsnow.blockracing.session.SessionManager;

/** The hub world players wait in and return to. Protected by LobbyListener. */
public final class Lobby {
    private static Lobby instance;

    private final SessionManager sessions;
    private World world;

    private Lobby(SessionManager sessions) {
        this.sessions = sessions;
    }

    public static void init(SessionManager sessions) {
        if (instance == null) {
            instance = new Lobby(sessions);
            instance.initialize();
        }
    }

    public static Lobby get() {
        if (instance == null) {
            throw new IllegalStateException("Lobby not initialized");
        }
        return instance;
    }

    private void initialize() {
        String name = Config.LOBBY_WORLD.getString();
        world = Bukkit.getWorld(name);
        if (world == null) {
            world = Bukkit.createWorld(new WorldCreator(name));
        }
        if (world == null) {
            throw new IllegalStateException("Failed to load lobby world " + name);
        }
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setStorm(false);
        world.setThundering(false);
        world.setTime(1000);
        world.setGameRule(GameRules.KEEP_INVENTORY, true);
        world.getWorldBorder().setCenter(world.getSpawnLocation());
        world.getWorldBorder().setSize(200);
    }

    public static boolean isLobbyWorld(World candidate) {
        return instance != null && instance.world != null && instance.world.equals(candidate);
    }

    public World getWorld() {
        return world;
    }

    /** First join with no saved session: greet and place in the lobby. */
    public void joinAsFirst(Player player) {
        player.sendMessage(Message.NOTICE_WELCOME.getString(player));
        sendToLobby(player);
    }

    /** Returns a player to the lobby, restoring their saved session if any. */
    public void sendToLobby(Player player) {
        PlayerSession saved = takeSavedSession(player);
        if (saved != null) {
            saved.restore(player);
        } else {
            player.setGameMode(GameMode.ADVENTURE);
            player.getInventory().clear();
        }
        player.teleport(world.getSpawnLocation());
        Scoreboard.showScoreboard(player);
    }

    public void saveSession(Player player) {
        sessions.saveSession(player);
    }

    private PlayerSession takeSavedSession(Player player) {
        return sessions.takeSavedSession(player.getUniqueId());
    }
}
