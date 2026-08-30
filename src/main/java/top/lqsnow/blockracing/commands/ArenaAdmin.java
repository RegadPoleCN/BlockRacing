package top.lqsnow.blockracing.commands;

import java.util.ArrayList;
import java.util.List;

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

/** Admin operations on arenas: /bradmin <start|stop> */
public class ArenaAdmin implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("blockracing.admin")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }
        if (args.length != 1 || (!args[0].equalsIgnoreCase("start") && !args[0].equalsIgnoreCase("stop"))) {
            sender.sendMessage("Usage: /bradmin <start|stop>");
            return true;
        }

        GameInstance arena = sender instanceof Player player
                ? GameManager.get().sessions().arenaOf(player).orElse(GameManager.first())
                : GameManager.first();

        if (args[0].equalsIgnoreCase("start")) {
            if (arena.getState() != RoomState.WAITING) {
                sender.sendMessage("The game already started.");
                return true;
            }
            arena.start();
            sender.sendMessage("Game " + arena.getId() + " started.");
        } else {
            if (arena.getState() == RoomState.WAITING || arena.getState() == RoomState.RUNNING) {
                if (arena.getState() == RoomState.RUNNING) {
                    arena.forceEnd();
                } else {
                    arena.dispose();
                }
                sender.sendMessage("Game " + arena.getId() + " stopped.");
            } else {
                sender.sendMessage("The game is not running.");
            }
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("start");
            completions.add("stop");
        }
        if (!completions.isEmpty()) {
            String prefix = args[args.length - 1].toLowerCase();
            completions.removeIf(s -> !s.toLowerCase().startsWith(prefix));
        }
        return completions;
    }
}
