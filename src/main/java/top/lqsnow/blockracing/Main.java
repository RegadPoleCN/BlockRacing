package top.lqsnow.blockracing;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import top.lqsnow.blockracing.commands.*;
import top.lqsnow.blockracing.core.GameManager;
import top.lqsnow.blockracing.core.RoomState;
import top.lqsnow.blockracing.game.InstanceProgressStore;
import top.lqsnow.blockracing.listeners.ArenaListener;
import top.lqsnow.blockracing.managers.*;
import top.lqsnow.blockracing.toolkit.menu.MenuListener;
import top.lqsnow.blockracing.world.ArenaWorldService;

import static org.bukkit.Bukkit.getPluginCommand;
import static org.bukkit.Bukkit.getPluginManager;


public class Main extends JavaPlugin {
    private static Main instance;
    private boolean initialized;

    public static Main getInstance() {
        return instance;
    }

    public static String getVersion() {
        return instance.getPluginMeta().getVersion();
    }

    @Override
    public void onEnable() {
        instance = this;

        // Register events
        getPluginManager().registerEvents(new ArenaListener(), this);
        getPluginManager().registerEvents(new MenuListener(), this);
        getPluginManager().registerEvents(new top.lqsnow.blockracing.lobby.LobbyListener(), this);
        getPluginManager().registerEvents(new TeamChestNavigation(), this);

        // Register commands
        getPluginCommand("debug").setExecutor(new Debug());
        getPluginCommand("debug").setTabCompleter(new Debug());
        getPluginCommand("menu").setExecutor(new Menu());
        getPluginCommand("menu").setTabCompleter(new Menu());
        getPluginCommand("arena").setExecutor(new ArenaCommand());
        getPluginCommand("arena").setTabCompleter(new ArenaCommand());
        getPluginCommand("waypoint").setExecutor(new WayPoint());
        getPluginCommand("locatestructure").setExecutor(new LocateStructure());
        getPluginCommand("locatestructure").setTabCompleter(new LocateStructure());
        getPluginCommand("locatebiome").setExecutor(new LocateBiome());
        getPluginCommand("locatebiome").setTabCompleter(new LocateBiome());
        getPluginCommand("bradmin").setExecutor(new ArenaAdmin());
        getPluginCommand("bradmin").setTabCompleter(new ArenaAdmin());
        getPluginCommand("tp").setExecutor(new Teleport());
        getPluginCommand("block").setExecutor(new GetBlock());
        getPluginCommand("block").setTabCompleter(new GetBlock());
        getPluginCommand("randomteam").setExecutor(new RandomTeam());
        getPluginCommand("shout").setExecutor(new Shout());
        Language language = new Language();
        getPluginCommand("language").setExecutor(language);
        getPluginCommand("language").setTabCompleter(language);

        // Save resources
        saveIfAbsent(
                "EasyBlocks.txt",
                "MediumBlocks.txt",
                "HardBlocks.txt",
                "DyedBlocks.txt",
                "EndBlocks.txt",
                "zh_cn.json",
                "en_us.json",
                "languages/en_us/lang.yml"
        );


        // Load managers
        Config.saveDefaultConfig();
        Config.load();
        Message.saveDefaultConfig();
        Message.load();
        LanguageManager.load();
        MenuMemory.load();
        if (!getVersion().equals(Config.CONFIG_VERSION.getString()) || !getVersion().equals(Message.MESSAGE_VERSION.getString())
                || LanguageManager.hasOutdatedLanguageFiles()) {
            getLogger().warning("Configuration version mismatch. Stop the server, back up plugins/BlockRacing (including unfinished matches and custom files), delete that folder, and restart to regenerate it. Review old custom settings before restoring them; do not copy old config/lang files back wholesale.");
        }
        ArenaWorldService.init();
        GameManager.init();
        GameManager.get().createGame();
        Scoreboard.createScoreboard();
        new Block();
        Block.checkBlock();
        boolean recoveredGame = InstanceProgressStore.load(GameManager.first());
        if (recoveredGame) {
            GameManager.first().resumeRecovered();
        }
        Scoreboard.updateScoreboard();
        InstanceProgressStore.startAutosave();
        // Runs after recovery so a world adopted by recovery is not treated as an orphan.
        ArenaWorldService.get().cleanupOrphansAndPruneBackups(
                Config.WORLD_PREFIX.getString(), Config.BACKUP_KEEP.getInt());
        GameManager.get().startWatchdog();
        GameManager.get().maintainIdleRooms();
        top.lqsnow.blockracing.lobby.Lobby.init(GameManager.get().sessions());

        // Complete
        initialized = true;
        new top.lqsnow.blockracing.updates.UpdateChecker(this).start();
        Bukkit.getLogger().info("[BlockRacing] Load Complete!");
    }

    @Override
    public void onDisable() {
        // A malformed new preset/catalogue must not delete a valid match that was never loaded.
        if (!initialized) return;
        boolean anyRunning = GameManager.get().all().stream()
                .anyMatch(arena -> arena.getState() == RoomState.RUNNING);
        if (anyRunning) {
            GameManager.get().all().stream()
                    .filter(arena -> arena.getState() == RoomState.RUNNING)
                    .forEach(InstanceProgressStore::saveNow);
        } else {
            InstanceProgressStore.clearAll();
        }
        Config.saveConfig();
    }

    private void saveIfAbsent(String... paths) {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        for (String path : paths) {
            java.io.File out = new java.io.File(getDataFolder(), path);
            if (!out.exists()) {
                saveResource(path, false); // 只在缺失时复制，避免 WARNING
            }
        }
    }
}
