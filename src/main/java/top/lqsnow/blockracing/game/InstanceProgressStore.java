package top.lqsnow.blockracing.game;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.core.GameInstance;
import top.lqsnow.blockracing.core.GameManager;
import top.lqsnow.blockracing.core.RoomState;
import top.lqsnow.blockracing.core.TeamColor;
import top.lqsnow.blockracing.managers.Config;

/** Crash-recovery snapshot of one game instance (was managers.GameProgressStore).
 * The single save file lives in the plugin data folder; recovery only applies
 * when the arena world folder still exists. */
public final class InstanceProgressStore {
    private static final int FORMAT_VERSION = 2;
    private static final String FILE_NAME = "game-progress.yml";
    private static File file;
    private static boolean recoveredGame;

    private InstanceProgressStore() {
    }

    private static File file() {
        if (file == null) {
            file = new File(Main.getInstance().getDataFolder(), FILE_NAME);
        }
        return file;
    }

    public static void startAutosave() {
        Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            for (GameInstance arena : GameManager.get().all()) {
                if (arena.getState() == RoomState.RUNNING) {
                    saveNow(arena);
                }
            }
        }, 100L, 100L);
    }

    public static boolean load(GameInstance target) {
        if (!Config.RECOVERY_ENABLED.getBoolean()) {
            clear();
            return false;
        }
        if (!file().isFile()) {
            return false;
        }
        YamlConfiguration state = YamlConfiguration.loadConfiguration(file());
        String savedState = state.getString("state");
        if (!"INGAME".equals(savedState) && !RoomState.RUNNING.name().equals(savedState)) {
            clear();
            return false;
        }
        if (!restoreWorld(state, target)) {
            clear();
            return false;
        }
        try {
            target.setState(RoomState.RUNNING);
            TeamColor[] colors = TeamColor.values();
            target.setScore(colors[0], state.getInt("scores.red"));
            target.setScore(colors[1], state.getInt("scores.blue"));
            target.setCurrentAmount(colors[0], state.getInt("progress.red-current"));
            target.setCurrentAmount(colors[1], state.getInt("progress.blue-current"));
            target.setTotalAmount(colors[0], state.getInt("progress.red-total"));
            target.setTotalAmount(colors[1], state.getInt("progress.blue-total"));
            target.setRollCount(colors[0], state.getInt("rolls.red"));
            target.setRollCount(colors[1], state.getInt("rolls.blue"));
            target.setLocateCost(state.getInt("locate-cost"));
            RoomSettings settings = target.getSettings();
            settings.setSpeedMode(state.getBoolean("settings.speed-mode", settings.isSpeedMode()));
            try {
                settings.setGameMode(GameModeType.valueOf(
                        state.getString("settings.game-mode", settings.getGameMode().name())));
            } catch (IllegalArgumentException ignored) {
                // Keep the configured mode if an older/corrupt state contains an unknown value.
            }

            BlockState blockState = target.getBlockState();
            replace(blockState.all(TeamColor.RED), state.getStringList("blocks.red-all"));
            replace(blockState.all(TeamColor.BLUE), state.getStringList("blocks.blue-all"));
            replace(blockState.remaining(TeamColor.RED), state.getStringList("blocks.red-remaining"));
            replace(blockState.remaining(TeamColor.BLUE), state.getStringList("blocks.blue-remaining"));

            target.getTeams().restore(state.getStringList("teams.red"), state.getStringList("teams.blue"));
            replace(target.getInGamePlayers(), state.getStringList("players.in-game"));
            replace(target.getFreeRandomTp(), state.getStringList("players.free-random-tp"));
            replace(target.getLocatePermission(), state.getStringList("players.locate-permission"));
            target.getCollectAmount().clear();
            target.getCollectAmount().putAll(readIntegerMap(state.getConfigurationSection("collected")));

            replaceLocations(target.waypoints(TeamColor.RED), readLocations(state.getConfigurationSection("waypoints.red")));
            replaceLocations(target.waypoints(TeamColor.BLUE), readLocations(state.getConfigurationSection("waypoints.blue")));
            restoreChests(state, "chests.red", target.chests(TeamColor.RED));
            restoreChests(state, "chests.blue", target.chests(TeamColor.BLUE));
            recoveredGame = true;
            Main.getInstance().getLogger().info("Recovered unfinished BlockRacing game progress.");
            return true;
        } catch (RuntimeException ex) {
            Main.getInstance().getLogger().log(Level.SEVERE,
                    "Unable to restore game-progress.yml; starting in waiting state instead.", ex);
            target.setState(RoomState.WAITING);
            return false;
        }
    }

    private static boolean restoreWorld(YamlConfiguration state, GameInstance target) {
        int arenaId = state.getInt("arena-id", 1);
        if (arenaId != target.getId()) {
            Main.getInstance().getLogger().warning(
                    "Saved game belongs to arena " + arenaId + "; only arena "
                            + target.getId() + " can be recovered.");
            return false;
        }
        String prefix = Config.WORLD_PREFIX.getString();
        String worldName = (prefix == null ? "arena_" : prefix) + arenaId;
        Path dataFolder = Main.getInstance().getDataFolder().toPath().toAbsolutePath().normalize();
        Path pluginsFolder = dataFolder.getParent();
        Path serverRoot = pluginsFolder == null ? null : pluginsFolder.getParent();
        if (serverRoot == null || !Files.isDirectory(serverRoot.resolve(worldName))) {
            Main.getInstance().getLogger().info(
                    "Arena world " + worldName + " no longer exists; skipping game recovery.");
            return false;
        }
        World world = Bukkit.createWorld(new org.bukkit.WorldCreator(worldName));
        if (world == null) {
            return false;
        }
        target.adoptWorld(world);
        return true;
    }

    public static synchronized void saveNow(GameInstance arena) {
        YamlConfiguration state = snapshot(arena);
        File temporary = new File(file().getParentFile(), file().getName() + ".tmp");
        try {
            Files.writeString(temporary.toPath(), state.saveToString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary.toPath(), file().toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicMoveFailure) {
                Files.move(temporary.toPath(), file().toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            Main.getInstance().getLogger().log(Level.SEVERE, "Unable to save game progress.", ex);
        }
    }

    public static boolean hasRecoveredGame() {
        return recoveredGame;
    }

    public static void clear() {
        recoveredGame = false;
        try {
            Files.deleteIfExists(file().toPath());
            Files.deleteIfExists(new File(file().getParentFile(), file().getName() + ".tmp").toPath());
        } catch (IOException ex) {
            Main.getInstance().getLogger().log(Level.WARNING, "Unable to remove old game progress.", ex);
        }
    }

    private static YamlConfiguration snapshot(GameInstance arena) {
        YamlConfiguration state = new YamlConfiguration();
        state.set("format-version", FORMAT_VERSION);
        state.set("state", arena.getState().name());
        state.set("arena-id", arena.getId());
        state.set("scores.red", arena.getScore(TeamColor.RED));
        state.set("scores.blue", arena.getScore(TeamColor.BLUE));
        state.set("progress.red-current", arena.getCurrentAmount(TeamColor.RED));
        state.set("progress.blue-current", arena.getCurrentAmount(TeamColor.BLUE));
        state.set("progress.red-total", arena.getTotalAmount(TeamColor.RED));
        state.set("progress.blue-total", arena.getTotalAmount(TeamColor.BLUE));
        state.set("rolls.red", arena.getRollCount(TeamColor.RED));
        state.set("rolls.blue", arena.getRollCount(TeamColor.BLUE));
        state.set("locate-cost", arena.getLocateCost());
        state.set("settings.speed-mode", arena.getSettings().isSpeedMode());
        state.set("settings.game-mode", arena.getSettings().getGameMode().name());
        state.set("blocks.red-all", new ArrayList<>(arena.getBlockState().all(TeamColor.RED)));
        state.set("blocks.blue-all", new ArrayList<>(arena.getBlockState().all(TeamColor.BLUE)));
        state.set("blocks.red-remaining", new ArrayList<>(arena.getBlockState().remaining(TeamColor.RED)));
        state.set("blocks.blue-remaining", new ArrayList<>(arena.getBlockState().remaining(TeamColor.BLUE)));
        state.set("teams.red", new ArrayList<>(arena.getTeams().players(TeamColor.RED)));
        state.set("teams.blue", new ArrayList<>(arena.getTeams().players(TeamColor.BLUE)));
        state.set("players.in-game", new ArrayList<>(arena.getInGamePlayers()));
        state.set("players.free-random-tp", new ArrayList<>(arena.getFreeRandomTp()));
        state.set("players.locate-permission", new ArrayList<>(arena.getLocatePermission()));
        arena.getCollectAmount().forEach((name, amount) -> state.set("collected." + name, amount));
        arena.waypoints(TeamColor.RED).forEach((index, location) -> state.set("waypoints.red." + index, location));
        arena.waypoints(TeamColor.BLUE).forEach((index, location) -> state.set("waypoints.blue." + index, location));
        saveChests(state, "chests.red", arena.chests(TeamColor.RED));
        saveChests(state, "chests.blue", arena.chests(TeamColor.BLUE));
        return state;
    }

    private static void saveChests(YamlConfiguration state, String path, List<Inventory> chests) {
        for (int index = 0; index < chests.size(); index++) {
            state.set(path + "." + index, Arrays.asList(chests.get(index).getContents()));
        }
    }

    private static void restoreChests(YamlConfiguration state, String path, List<Inventory> chests) {
        for (int index = 0; index < chests.size(); index++) {
            List<?> values = state.getList(path + "." + index, List.of());
            ItemStack[] contents = new ItemStack[chests.get(index).getSize()];
            for (int slot = 0; slot < values.size() && slot < contents.length; slot++) {
                if (values.get(slot) instanceof ItemStack item) {
                    contents[slot] = item;
                }
            }
            chests.get(index).setContents(contents);
        }
    }

    private static HashMap<Integer, Location> readLocations(ConfigurationSection section) {
        HashMap<Integer, Location> locations = new HashMap<>();
        if (section == null) {
            return locations;
        }
        for (String key : section.getKeys(false)) {
            Location location = section.getLocation(key);
            if (location != null) {
                locations.put(Integer.parseInt(key), location);
            }
        }
        return locations;
    }

    private static Map<String, Integer> readIntegerMap(ConfigurationSection section) {
        Map<String, Integer> values = new HashMap<>();
        if (section != null) {
            section.getKeys(false).forEach(key -> values.put(key, section.getInt(key)));
        }
        return values;
    }

    private static void replace(List<String> target, List<String> values) {
        target.clear();
        target.addAll(values);
    }

    private static void replaceLocations(Map<Integer, Location> target, Map<Integer, Location> values) {
        target.clear();
        target.putAll(values);
    }
}
