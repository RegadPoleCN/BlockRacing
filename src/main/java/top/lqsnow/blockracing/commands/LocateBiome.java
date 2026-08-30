package top.lqsnow.blockracing.commands;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.managers.Message;

import java.util.List;

public class LocateBiome implements CommandExecutor, TabCompleter {
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

        top.lqsnow.blockracing.core.GameInstance arena =
                top.lqsnow.blockracing.core.GameManager.get().sessions().arenaOf(player).orElse(null);
        if (arena == null) {
            player.sendMessage(Message.NOTICE_NOT_IN_GAME.getString(player));
            return true;
        }
        if (arena.getLocatePermission().contains(player.getName())) {
            if (player.performCommand("locate biome " + args[0])) {
                arena.getLocatePermission().remove(player.getName());
                player.addAttachment(Main.getInstance(), "minecraft.command.locate", false);
            }
        } else player.sendMessage(Message.NOTICE_LOCATE_NO_PERMISSION.getString(player));

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length != 1) {
            return List.of();
        }
        String prefix = strings[0].toLowerCase();
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).keyStream()
                .filter(key -> key.getNamespace().equals("minecraft"))
                .map(key -> key.getKey())
                .filter(key -> key.startsWith(prefix))
                .sorted()
                .toList();
    }
}
