package top.lqsnow.blockracing.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.lqsnow.blockracing.core.GameInstance;
import top.lqsnow.blockracing.core.GameManager;
import top.lqsnow.blockracing.core.TeamColor;
import top.lqsnow.blockracing.managers.Message;

public class WayPoint implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Bukkit.getLogger().info("This command can only be run by a player.");
            return true;
        }
        if (args.length != 2 || !args[0].equalsIgnoreCase("remove")) {
            player.sendMessage(Message.NOTICE_ERROR_COMMAND.getString(player));
            return true;
        }
        int index;
        try {
            index = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(Message.NOTICE_ERROR_COMMAND.getString(player));
            return true;
        }
        GameInstance arena = GameManager.get().sessions().arenaOf(player).orElse(null);
        if (arena == null) {
            player.sendMessage(Message.NOTICE_NOT_IN_GAME.getString(player));
            return true;
        }
        if (index < 1 || index > arena.getSettings().getMaxTeamWaypointNum()) {
            player.sendMessage(Message.NOTICE_ERROR_COMMAND.getString(player));
            return true;
        }
        TeamColor team = arena.getTeams().teamOf(player.getName()).orElse(null);
        if (team == null) {
            player.sendMessage(Message.NOTICE_NOT_IN_GAME.getString(player));
            return true;
        }
        arena.waypoints(team).remove(index);
        top.lqsnow.blockracing.game.InstanceProgressStore.saveNow(arena);
        arena.broadcast().team(team,
                team == TeamColor.RED ? Message.NOTICE_RED_REMOVE_WAYPOINT : Message.NOTICE_BLUE_REMOVE_WAYPOINT,
                (viewer, text) -> text
                        .replace("%player%", player.getName()).replace("%index%", String.valueOf(index)));
        return true;
    }
}
