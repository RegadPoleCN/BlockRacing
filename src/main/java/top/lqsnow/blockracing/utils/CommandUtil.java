package top.lqsnow.blockracing.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.lqsnow.blockracing.core.TeamColor;
import top.lqsnow.blockracing.managers.Message;
import top.lqsnow.blockracing.managers.Team;

import java.util.List;
import java.util.function.BiFunction;

import static top.lqsnow.blockracing.utils.ColorUtil.t;

public class CommandUtil {
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

    public static void sendRed(String message) {
        sendTeam(TeamColor.RED, message);
    }

    public static void sendRed(Message message, BiFunction<Player, String, String> formatter) {
        sendTeam(TeamColor.RED, message, formatter);
    }

    public static void sendBlue(String message) {
        sendTeam(TeamColor.BLUE, message);
    }

    public static void sendBlue(Message message, BiFunction<Player, String, String> formatter) {
        sendTeam(TeamColor.BLUE, message, formatter);
    }

    public static void sendTeam(TeamColor color, String message) {
        teamMembers(color).forEach(playerName -> {
            Player player = Bukkit.getPlayerExact(playerName);
            if (player != null) player.sendMessage(t(message));
        });
    }

    public static void sendTeam(TeamColor color, Message message, BiFunction<Player, String, String> formatter) {
        teamMembers(color).forEach(playerName -> {
            Player player = Bukkit.getPlayerExact(playerName);
            if (player != null) player.sendMessage(t(formatter.apply(player, message.getString(player))));
        });
    }

    private static List<String> teamMembers(TeamColor color) {
        return color == TeamColor.RED ? Team.redTeamPlayers() : Team.blueTeamPlayers();
    }
}
