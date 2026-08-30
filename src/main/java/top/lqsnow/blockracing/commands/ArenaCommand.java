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
import top.lqsnow.blockracing.managers.Message;

/** /arena join [id] | /arena leave | /arena list */
public class ArenaCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Bukkit.getLogger().info("This command can only be run by a player.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(Message.NOTICE_ERROR_COMMAND.getString(player));
            return true;
        }

        if (args[0].equalsIgnoreCase("join")) {
            GameInstance target;
            if (args.length >= 2) {
                try {
                    int id = Integer.parseInt(args[1]);
                    target = GameManager.get().byId(id).orElse(null);
                } catch (NumberFormatException ex) {
                    target = null;
                }
                if (target == null) {
                    player.sendMessage(Message.NOTICE_ERROR_COMMAND.getString(player));
                    return true;
                }
            } else {
                target = GameManager.get().findJoinable();
            }
            target.join(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("leave")) {
            Optional<GameInstance> arena = GameManager.get().sessions().arenaOf(player);
            if (arena.isEmpty()) {
                player.sendMessage(Message.NOTICE_NOT_IN_GAME.getString(player));
                return true;
            }
            arena.get().leave(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            for (GameInstance arena : GameManager.get().all()) {
                player.sendMessage("Game #" + arena.getId() + " - " + arena.getState()
                        + " - " + arena.playerCount() + " players");
            }
            return true;
        }

        player.sendMessage(Message.NOTICE_ERROR_COMMAND.getString(player));
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("join");
            completions.add("leave");
            completions.add("list");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            for (GameInstance arena : GameManager.get().all()) {
                completions.add(String.valueOf(arena.getId()));
            }
        }
        if (!completions.isEmpty()) {
            String prefix = args[args.length - 1].toLowerCase();
            completions.removeIf(s -> !s.toLowerCase().startsWith(prefix));
        }
        return completions;
    }
}
