package top.lqsnow.blockracing.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import top.lqsnow.blockracing.managers.Message;
import top.lqsnow.blockracing.session.SessionManager;
import top.lqsnow.blockracing.utils.ColorUtil;

/** Broadcasts scoped to one game instance: only players whose session points
 * at this arena receive messages, team messages and sounds. */
public final class GameBroadcast {
    private final GameInstance arena;
    private final SessionManager sessions;

    public GameBroadcast(GameInstance arena, SessionManager sessions) {
        this.arena = arena;
        this.sessions = sessions;
    }

    private List<Player> audience() {
        List<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (sessions.arenaOf(player).map(a -> a == arena).orElse(false)) {
                players.add(player);
            }
        }
        return players;
    }

    public void all(String message) {
        for (Player player : audience()) {
            player.sendMessage(ColorUtil.t(message));
        }
    }

    public void all(Message message) {
        all(message, (player, text) -> text);
    }

    public void all(Message message, BiFunction<Player, String, String> formatter) {
        for (Player player : audience()) {
            player.sendMessage(ColorUtil.t(formatter.apply(player, message.getString(player))));
        }
    }

    public void team(TeamColor color, String message) {
        for (Player player : audience()) {
            if (isMember(color, player)) {
                player.sendMessage(ColorUtil.t(message));
            }
        }
    }

    public void team(TeamColor color, Message message, BiFunction<Player, String, String> formatter) {
        for (Player player : audience()) {
            if (isMember(color, player)) {
                player.sendMessage(ColorUtil.t(formatter.apply(player, message.getString(player))));
            }
        }
    }

    public void sound(Sound sound) {
        for (Player player : audience()) {
            player.playSound(player, sound, 1F, 1F);
        }
    }

    private boolean isMember(TeamColor color, Player player) {
        return arena.getTeams().teamOf(player.getName()).map(color::equals).orElse(false);
    }
}
