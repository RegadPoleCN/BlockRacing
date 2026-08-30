package top.lqsnow.blockracing;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import top.lqsnow.blockracing.commands.*;
import top.lqsnow.blockracing.core.GameManager;
import top.lqsnow.blockracing.listeners.BasicListener;
import top.lqsnow.blockracing.managers.*;
import top.lqsnow.blockracing.toolkit.menu.MenuListener;
import top.lqsnow.blockracing.world.ArenaWorldService;

import static org.bukkit.Bukkit.getPluginCommand;
import static org.bukkit.Bukkit.getPluginManager;


public class Main extends JavaPlugin {
    private static Main instance;

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
        getPluginManager().registerEvents(new BasicListener(), this);
        getPluginManager().registerEvents(new MenuListener(), this);
        getPluginManager().registerEvents(new top.lqsnow.blockracing.lobby.LobbyListener(), this);

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
        ArenaWorldService.init();
        GameManager.init();
        GameManager.get().createGame();
        Team.createTeam();
        Scoreboard.createScoreboard();
        Block.checkBlock();
        Block.refreshAvailableBlocksAndClampAmount();
        boolean recoveredGame = GameProgressStore.load();
        if (recoveredGame) {
            Game.resumeRecoveredGame();
            Scoreboard.setInGameScoreboard();
        } else {
            Scoreboard.setPreGameScoreboard();
        }
        GameProgressStore.startAutosave();
        // Runs after recovery so a world adopted by recovery is not treated as an orphan.
        ArenaWorldService.get().cleanupOrphansAndPruneBackups(
                Config.WORLD_PREFIX.getString(), Config.BACKUP_KEEP.getInt());
        GameManager.get().startWatchdog();
        GameManager.get().maintainIdleRooms();
        top.lqsnow.blockracing.lobby.Lobby.init(GameManager.get().sessions());

        // Complete
        Bukkit.getLogger().info("[BlockRacing] Load Complete!");
    }

    @Override
    public void onDisable() {
        if (Game.getCurrentGameState() == Game.GameState.INGAME) {
            GameProgressStore.saveNow();
        } else {
            GameProgressStore.clear();
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
