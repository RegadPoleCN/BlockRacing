package top.lqsnow.blockracing.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.lqsnow.blockracing.core.GameInstance;
import top.lqsnow.blockracing.core.GameManager;
import top.lqsnow.blockracing.core.RoomState;
import top.lqsnow.blockracing.core.TeamColor;
import top.lqsnow.blockracing.managers.Gui;
import top.lqsnow.blockracing.managers.Message;
import top.lqsnow.blockracing.managers.Scoreboard;
import top.lqsnow.blockracing.menus.GameMenu;

import java.util.ArrayList;
import java.util.List;

public class Menu implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Bukkit.getLogger().info("This command can only be run by a player.");
            return true;
        }

        GameInstance arena = GameManager.get().sessions().arenaOf(player).orElse(null);
        if (arena == null) {
            if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("main"))) {
                Gui.openMenu(player);
            } else {
                player.sendMessage(Message.NOTICE_NOT_IN_GAME.getString(player));
            }
            return true;
        }
        if (arena.getState() == RoomState.ENDING || arena.getState() == RoomState.DISPOSED) {
            return true;
        }

        if (args.length == 0) {
            Gui.openMenu(player);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("main")) {
            Gui.openMainMenu(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("chest")) {
            if (args.length > 2) {
                player.sendMessage(Message.NOTICE_ERROR_COMMAND.getString(player));
                return true;
            }
            if (arena.getState() == top.lqsnow.blockracing.core.RoomState.WAITING) {
                player.sendMessage(Message.NOTICE_GAME_NOT_START.getString(player));
                return true;
            }
            TeamColor team = arena.getTeams().teamOf(player.getName()).orElse(null);
            if (team == null) {
                return true;
            }
            if (args.length == 1) {
                new GameMenu.TeamChestSelectMenu(arena).open(player);
                return true;
            }
            Integer ith = parseIndex(args[1], arena.chests(team).size());
            if (ith == null) {
                player.sendMessage(Message.NOTICE_ERROR_COMMAND.getString(player));
                return true;
            }
            Gui.openTeamChest(player, ith - 1);
            return true;
        }

        if (args[0].equalsIgnoreCase("waypoints")) {
            if (arena.getState() == top.lqsnow.blockracing.core.RoomState.WAITING) {
                player.sendMessage(Message.NOTICE_GAME_NOT_START.getString(player));
                return true;
            }

            if (args.length == 1) {
                TeamColor team = arena.getTeams().teamOf(player.getName()).orElse(null);
                if (team == TeamColor.RED) {
                    new GameMenu.WayPointMenu(arena.waypoints(TeamColor.RED),
                            arena.waypointIcons(TeamColor.RED), arena).open(player);
                } else if (team == TeamColor.BLUE) {
                    new GameMenu.WayPointMenu(arena.waypoints(TeamColor.BLUE),
                            arena.waypointIcons(TeamColor.BLUE), arena).open(player);
                }
                return true;
            }

            if (args.length == 3 && args[1].equalsIgnoreCase("use")) {
                Integer index = parseIndex(args[2], arena.getSettings().getMaxTeamWaypointNum());
                if (index == null) {
                    player.sendMessage(Message.NOTICE_ERROR_COMMAND.getString(player));
                    return true;
                }
                arena.waypoint(player, index, ClickType.LEFT);
            } else {
                player.sendMessage(Message.NOTICE_ERROR_COMMAND.getString(player));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("roll") && args.length == 1) {
            if (arena.getState() == top.lqsnow.blockracing.core.RoomState.WAITING) {
                player.sendMessage(Message.NOTICE_GAME_NOT_START.getString(player));
                return true;
            }
            arena.roll(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("locate") && args.length == 1) {
            if (arena.getState() == top.lqsnow.blockracing.core.RoomState.WAITING) {
                player.sendMessage(Message.NOTICE_GAME_NOT_START.getString(player));
                return true;
            }
            arena.locate(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("randomTP") && args.length == 1) {
            if (arena.getState() == top.lqsnow.blockracing.core.RoomState.WAITING) {
                player.sendMessage(Message.NOTICE_GAME_NOT_START.getString(player));
                return true;
            }
            TeamColor team = arena.getTeams().teamOf(player.getName()).orElse(null);
            if (arena.getFreeRandomTp().contains(player.getName())) {
                arena.randomTeleport(player, false);
                arena.getFreeRandomTp().remove(player.getName());
            } else {
                if (team != null && arena.getScore(team) < 2) {
                    player.sendMessage(Message.NOTICE_NOT_ENOUGH_SCORE.getString(player));
                    return true;
                }
                player.closeInventory();
                arena.randomTeleport(player, false);
                if (team != null) {
                    arena.addScore(team, -2);
                    arena.broadcast().all(Message.NOTICE_RANDOM_TP, (viewer, text) -> text.replace("%player%",
                            (team == TeamColor.RED
                                    ? Message.TEAM_RED_COLOR.getString(viewer)
                                    : Message.TEAM_BLUE_COLOR.getString(viewer)) + player.getName()));
                }
                Scoreboard.updateScoreboard();
            }
            return true;
        }

        player.sendMessage(Message.NOTICE_ERROR_COMMAND.getString(player));
        return true;
    }

    private static Integer parseIndex(String value, int maximum) {
        try {
            int index = Integer.parseInt(value);
            return index >= 1 && index <= maximum ? index : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("main");
            completions.add("chest");
            completions.add("waypoints");
            completions.add("roll");
            completions.add("locate");
            completions.add("randomTP");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("chest")) {
            for (int i = 1; i <= GameManager.first().getSettings().getMaxTeamChestNum(); i++) {
                completions.add(Integer.toString(i));
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("waypoints")) {
            completions.add("use");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("waypoints")) {
            for (int i = 1; i <= GameManager.first().getSettings().getMaxTeamWaypointNum(); i++) {
                completions.add(Integer.toString(i));
            }
        }

        String prefix = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(prefix));

        return completions;
    }
}
