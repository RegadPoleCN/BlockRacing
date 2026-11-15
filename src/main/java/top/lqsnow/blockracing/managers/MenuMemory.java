package top.lqsnow.blockracing.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.core.GameInstance;
import top.lqsnow.blockracing.core.GameManager;
import top.lqsnow.blockracing.core.RoomState;
import top.lqsnow.blockracing.core.TeamColor;
import top.lqsnow.blockracing.menus.*;
import top.lqsnow.blockracing.toolkit.menu.*;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

/** Remembers which in-game menu a player wants to return to (was global Game state; now per arena). */
public final class MenuMemory {
    private static MenuHistory history = new MenuHistory();
    private static File file;
    private MenuMemory() {}

    public static void load() {
        history = new MenuHistory();
        file = new File(Main.getInstance().getDataFolder(), "menu-preferences.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String id : yaml.getKeys(false)) {
            try { history.enable(UUID.fromString(id), yaml.getBoolean(id)); }
            catch (IllegalArgumentException ignored) { /* Ignore invalid preference IDs. */ }
        }
    }

    public static boolean enabled(Player player) { return history.enabled(player.getUniqueId()); }
    public static void toggle(Player player) {
        history.enable(player.getUniqueId(), !enabled(player));
        YamlConfiguration yaml = new YamlConfiguration();
        history.enabledPlayers().forEach(id -> yaml.set(id.toString(), true));
        try { yaml.save(file); }
        catch (IOException ex) { Main.getInstance().getLogger().log(Level.WARNING, "Unable to save menu preferences", ex); }
    }

    public static void resetDestinations() { history.resetDestinations(); }

    private static String team(GameInstance arena, Player player) {
        return arena.getTeams().teamOf(player.getName())
                .map(team -> team.name().toLowerCase(Locale.ROOT)).orElse("");
    }

    public static void record(Player player, MenuView menu) {
        GameInstance arena = GameManager.get().sessions().arenaOf(player).orElse(null);
        if (arena == null || arena.getState() != RoomState.RUNNING) return;
        MenuHistory.Kind kind;
        if (menu instanceof GameMenu) kind = MenuHistory.Kind.MAIN;
        else if (menu instanceof GameMenu.TeamChestSelectMenu) kind = MenuHistory.Kind.CHEST_SELECT;
        else if (menu instanceof GameMenu.WayPointMenu) kind = MenuHistory.Kind.WAYPOINTS;
        else if (menu instanceof GameMenu.TeammateTeleportMenu) kind = MenuHistory.Kind.TEAMMATES;
        else if (menu instanceof LanguageMenu) kind = MenuHistory.Kind.LANGUAGE;
        else return;
        history.record(player.getUniqueId(), new MenuHistory.Destination(kind,
                kind == MenuHistory.Kind.MAIN || kind == MenuHistory.Kind.LANGUAGE ? "" : team(arena, player), 0));
    }

    public static void recordChest(Player player, GameInstance arena, int index) {
        if (arena.getState() == RoomState.RUNNING) {
            history.record(player.getUniqueId(), new MenuHistory.Destination(MenuHistory.Kind.CHEST, team(arena, player), index));
        }
    }

    public static boolean reopen(Player player, GameInstance arena) {
        if (!enabled(player)) return false;
        var remembered = history.last(player.getUniqueId());
        if (remembered.isEmpty()) return false;
        var destination = remembered.get();
        if (!destination.team().isEmpty() && !destination.team().equals(team(arena, player))) return false;
        switch (destination.kind()) {
            case MAIN -> { return false; }
            case LANGUAGE -> new LanguageMenu().open(player);
            case CHEST_SELECT -> {
                if (team(arena, player).isEmpty()) return false;
                new GameMenu.TeamChestSelectMenu(arena).open(player);
            }
            case TEAMMATES -> {
                if (team(arena, player).isEmpty()) return false;
                new GameMenu.TeammateTeleportMenu(player, arena).open(player);
            }
            case WAYPOINTS -> {
                if (team(arena, player).isEmpty()) return false;
                boolean red = team(arena, player).equals("red");
                new GameMenu.WayPointMenu(arena.waypoints(red ? TeamColor.RED : TeamColor.BLUE),
                        arena.waypointIcons(red ? TeamColor.RED : TeamColor.BLUE), arena).open(player);
            }
            case CHEST -> {
                if (team(arena, player).isEmpty()
                        || destination.index() >= arena.chests(team(arena, player).equals("red") ? TeamColor.RED : TeamColor.BLUE).size()) return false;
                Gui.openTeamChest(player, destination.index());
            }
        }
        return true;
    }
}
