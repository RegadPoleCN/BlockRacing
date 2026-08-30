package top.lqsnow.blockracing.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import top.lqsnow.blockracing.core.GameInstance;
import top.lqsnow.blockracing.core.GameManager;
import top.lqsnow.blockracing.core.RoomState;
import top.lqsnow.blockracing.core.TeamColor;
import top.lqsnow.blockracing.managers.Message;

public class Teleport implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Bukkit.getLogger().info("This command can only be run by a player.");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(Message.NOTICE_ERROR_COMMAND.getString(player));
            return true;
        }
        GameInstance arena = GameManager.get().sessions().arenaOf(player).orElse(null);
        if (arena == null || arena.getState() != RoomState.RUNNING) {
            player.sendMessage(Message.NOTICE_GAME_NOT_START.getString(player));
            return true;
        }
        TeamColor team = arena.getTeams().teamOf(player.getName()).orElse(null);
        if (team == null) {
            // Spectator
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target != null) {
                player.teleport(target);
                player.sendMessage(Message.NOTICE_SPECTATOR_TP_PLAYER_SUCCESS.getString(player).replace("%player%", target.getName()));
                return true;
            } else {
                player.sendMessage(Message.NOTICE_ERROR_COMMAND.getString(player));
                return true;
            }
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(Message.NOTICE_PLAYER_NOT_EXIST.getString(player));
            return true;
        }
        TeamColor targetTeam = arena.getTeams().teamOf(target.getName()).orElse(null);
        if (team == targetTeam) {
            player.teleport(target);
            player.sendMessage(Message.NOTICE_TP_PLAYER_SUCCESS.getString(player).replace("%player%", target.getName()));
            return true;
        }
        player.sendMessage(Message.NOTICE_PLAYER_NOT_IN_SAME_TEAM.getString(player));
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Optional<GameInstance> arena = GameManager.get().sessions().arenaOf(sender.getName());
        if (arena.isPresent()) {
            TeamColor team = arena.get().getTeams().teamOf(sender.getName()).orElse(null);
            if (team != null) {
                return List.copyOf(arena.get().getTeams().onlinePlayers(team));
            }
            return List.copyOf(arena.get().getOnlinePlayerNames());
        }
        List<String> names = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(player -> names.add(player.getName()));
        return names;
    }
}
