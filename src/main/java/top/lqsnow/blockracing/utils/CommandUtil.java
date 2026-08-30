package top.lqsnow.blockracing.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.lqsnow.blockracing.managers.Message;

import java.util.function.BiFunction;

import static top.lqsnow.blockracing.utils.ColorUtil.t;

public class CommandUtil {
    /** Server-wide broadcast; per-game messages go through GameBroadcast. */
    public static void sendAll(String message) {
        Bukkit.getOnlinePlayers().forEach((Player player) -> {
            player.sendMessage(t(message));
        });
    }

    public static void sendAll(Message message) {
        sendAll(message, (player, text) -> text);
    }

    public static void sendAll(Message message, BiFunction<Player, String, String> formatter) {
        Bukkit.getOnlinePlayers().forEach(player ->
                player.sendMessage(t(formatter.apply(player, message.getString(player)))));
    }
}
