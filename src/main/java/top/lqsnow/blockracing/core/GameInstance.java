package top.lqsnow.blockracing.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.game.BlockCatalog;
import top.lqsnow.blockracing.game.BlockState;
import top.lqsnow.blockracing.game.RandomTeleportService;
import top.lqsnow.blockracing.game.RoomSettings;
import top.lqsnow.blockracing.game.TeamState;
import top.lqsnow.blockracing.managers.Config;
import top.lqsnow.blockracing.game.GameModeType;
import top.lqsnow.blockracing.game.InstanceProgressStore;
import top.lqsnow.blockracing.managers.Message;
import top.lqsnow.blockracing.managers.Scoreboard;
import top.lqsnow.blockracing.session.SessionManager;
import top.lqsnow.blockracing.toolkit.item.ItemBuilder;
import top.lqsnow.blockracing.toolkit.material.Materials;
import top.lqsnow.blockracing.toolkit.text.Texts;
import top.lqsnow.blockracing.utils.TranslationUtil;

/** One running game: teams, target blocks, scores, chests, waypoints and the
 * per-tick loops. Ported from the former static Game/Team/Block state. */
public class GameInstance {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private final int id;
    private final SessionManager sessions;
    private final GameBroadcast broadcast;
    private final RoomSettings settings;
    private final BlockCatalog catalog;
    private final TeamState teams = new TeamState();
    private final BlockState blockState = new BlockState();

    private RoomState state = RoomState.WAITING;
    private final Set<String> readyPlayers = new HashSet<>();
    private int redScore;
    private int blueScore;
    private int redCurrentBlockAmount;
    private int blueCurrentBlockAmount;
    private int redTotalBlockAmount;
    private int blueTotalBlockAmount;
    private int redRollCount;
    private int blueRollCount;
    private final List<String> redRollPlayers = new ArrayList<>();
    private final List<String> blueRollPlayers = new ArrayList<>();
    private final List<String> inGamePlayers = new ArrayList<>();
    private final List<String> freeRandomTp = new ArrayList<>();
    private final List<String> locatePermission = new ArrayList<>();
    private final Map<String, org.bukkit.permissions.PermissionAttachment> locateAttachments = new HashMap<>();
    private int locateCost;
    private final Map<String, Integer> collectAmount = new HashMap<>();

    private final List<Inventory> redChests = new ArrayList<>();
    private final List<Inventory> blueChests = new ArrayList<>();
    private final Map<Integer, Location> redWaypoints = new HashMap<>();
    private final Map<Integer, Location> blueWaypoints = new HashMap<>();
    private final Map<Integer, Material> redWaypointIcons = new HashMap<>();
    private final Map<Integer, Material> blueWaypointIcons = new HashMap<>();

    private BukkitTask waitingTask;
    private BukkitTask tickTask;
    private BukkitTask countdownTask;
    private World world;
    private long startedAt;

    public GameInstance(int id, SessionManager sessions, RoomSettings settings) {
        this.id = id;
        this.sessions = sessions;
        this.broadcast = new GameBroadcast(this, sessions);
        this.settings = settings;
        this.catalog = BlockCatalog.load(Main.getInstance().getDataFolder());
        initChests();
        startWaitingEffects();
    }

    private void initChests() {
        int teamChestNum = settings.getMaxTeamChestNum();
        for (int i = 0; i < teamChestNum; i++) {
            redChests.add(Bukkit.createInventory(null, 6 * 9,
                    LEGACY_SERIALIZER.deserialize(Message.MENU_RED_CHEST.getString() + (i + 1))));
            blueChests.add(Bukkit.createInventory(null, 6 * 9,
                    LEGACY_SERIALIZER.deserialize(Message.MENU_BLUE_CHEST.getString() + (i + 1))));
        }
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /** Before the game: keep players regenerated and saturated. */
    private void startWaitingEffects() {
        waitingTask = new RunPer2Tick().runTaskTimer(Main.getInstance(), 0L, 2L);
    }

    /** Resumes only the tick loop; used when recovering a saved game. */
    public void resumeRecovered() {
        tickTask = new RunPer5Tick().runTaskTimer(Main.getInstance(), 0L, 5L);
    }

    public void playerLogin(Player player) {
        sessions.enterArena(player, this);
        Scoreboard.showScoreboard(player);

        if (state == RoomState.WAITING) {
            player.setGameMode(GameMode.ADVENTURE);
            resetPregameInventory(player);
            Message.NOTICE_WELCOME_LINES.getStringList(player).stream()
                    .map(line -> line.replace("%player%", player.getName()))
                    .forEach(player::sendMessage);
        } else if (state == RoomState.RUNNING) {
            if (InstanceProgressStore.hasRecoveredGame()) {
                Message.NOTICE_RECOVERED_GAME.getStringList(player).forEach(player::sendMessage);
            }
            // Spectator
            if (!teams.contains(player.getName())) {
                player.setGameMode(GameMode.SPECTATOR);
                player.teleport(gameWorld().getSpawnLocation());
                player.sendMessage(Message.NOTICE_SPECTATOR_JOIN.getString(player));
                return;
            }

            // Players who choose a team before the start of the game and exit, but enter
            // after the start of the game
            if (!inGamePlayers.contains(player.getName())) {
                initPlayer(player);
                freeRandomTp.add(player.getName());
            }
        }

        // The permissions will disappear when the player exits and re-enters,
        // permissions need to be given again.
        if (locatePermission.contains(player.getName())) {
            locateAttachments.put(player.getName(),
                    player.addAttachment(Main.getInstance(), "minecraft.command.locate", true));
        }

        checkUpdate(player);
    }

    /** Entry point from the lobby: snapshot, bind and run the login flow. */
    public void join(Player player) {
        if (sessions.arenaOf(player).isPresent()) {
            player.sendMessage(Message.NOTICE_ALREADY_IN_GAME.getString(player));
            return;
        }
        if (state == RoomState.WAITING
                && playerCount() >= Math.max(1, Config.MAX_PLAYERS.getInt())) {
            player.sendMessage(Message.NOTICE_ARENA_FULL.getString(player));
            return;
        }
        sessions.saveSession(player);
        playerLogin(player);
    }

    /** Back to the lobby: session unbound, snapshot restored. */
    public void leave(Player player) {
        playerQuit(player);
        sessions.leaveArena(player);
        broadcast.all(Message.NOTICE_LEFT_ARENA, (viewer, text) -> text.replace("%player%", player.getName()));
        top.lqsnow.blockracing.lobby.Lobby.get().sendToLobby(player);
    }

    public void playerQuit(Player player) {
        blueRollPlayers.remove(player.getName());
        redRollPlayers.remove(player.getName());
        readyPlayers.remove(player.getName());
        sessions.getEditAmountPlayers().remove(player.getName());
        cancelCountdown();
    }

    private void checkUpdate(Player player) {
        player.resetTitle();
        if (!top.lqsnow.blockracing.managers.Config.CONFIG_VERSION.getString().equals(Main.getVersion())
                || !Message.MESSAGE_VERSION.getString().equals(Main.getVersion())) {
            if (Message.NOTICE_VERSION_MISMATCH.getString() != null) {
                Message.NOTICE_VERSION_MISMATCH_LINES.getStringList(player)
                        .forEach(player::sendMessage);
                player.showTitle(Title.title(
                        LEGACY_SERIALIZER.deserialize(Message.NOTICE_VERSION_MISMATCH_TITLE.getString(player)),
                        LEGACY_SERIALIZER.deserialize(Message.NOTICE_VERSION_MISMATCH_SUBTITLE.getString(player)),
                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(100), Duration.ZERO)
                ));
            } else {
                player.sendMessage(top.lqsnow.blockracing.utils.ColorUtil.t(
                        "&cWarning! The current file versions of your config.yml and lang.yml do not correspond to the plugin version! You may have updated the plugin, but did not update the configuration file! This may lead to some unexpected errors! You can delete the two configuration files in the \\plugins\\BlockRacing folder, and then restart the server, or download the latest version of the configuration file on GitHub to replace it!"));
                player.showTitle(Title.title(
                        LEGACY_SERIALIZER.deserialize(top.lqsnow.blockracing.utils.ColorUtil.t("&cWarning! Version Mismatch!")),
                        LEGACY_SERIALIZER.deserialize(top.lqsnow.blockracing.utils.ColorUtil.t("&cPlease check the specific information in the chat!")),
                        Title.Times.times(Duration.ofSeconds(1), Duration.ZERO, Duration.ZERO)
                ));
            }
            Bukkit.getLogger().severe(Message.NOTICE_VERSION_MISMATCH.getString());
        }
    }

    public void playerReady(Player player) {
        if (!readyPlayers.contains(player.getName())) {
            readyPlayers.add(player.getName());
            broadcast.all(Message.NOTICE_READY, (viewer, text) -> text.replace("%player%", player.getName()));
            // Check if the game can start, just notice players
            if (readyPlayers.size() > 1 && readyPlayers.size() == arenaPlayers().size()) {
                broadcast.all(Message.NOTICE_ALL_READY);
            }
        } else {
            readyPlayers.remove(player.getName());
            broadcast.all(Message.NOTICE_CANCEL_READY, (viewer, text) -> text.replace("%player%", player.getName()));
            cancelCountdown();
            return;
        }
        maybeAutoStart();
    }

    /** Starts automatically when auto-start is on and everyone ready satisfies the limits. */
    private void maybeAutoStart() {
        if (!Config.AUTO_START.getBoolean() || state != RoomState.WAITING) {
            return;
        }
        if (!startConditionsMet()) {
            return;
        }
        int seconds = Math.max(0, Config.COUNTDOWN_SECONDS.getInt());
        if (seconds <= 0) {
            beginStart();
            return;
        }
        if (countdownTask != null) {
            return; // already counting down
        }
        broadcast.all(Message.NOTICE_ARENA_COUNTDOWN,
                (viewer, text) -> text.replace("%seconds%", String.valueOf(seconds)));
        countdownTask = Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            countdownTask = null;
            if (state == RoomState.WAITING && startConditionsMet()) {
                beginStart();
            } else {
                broadcast.all(Message.NOTICE_ARENA_COUNTDOWN_CANCEL);
            }
        }, seconds * 20L);
    }

    /** Cancels a pending start countdown, if one is running. */
    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
            broadcast.all(Message.NOTICE_ARENA_COUNTDOWN_CANCEL);
        }
    }

    /** min-players, everyone ready, both teams filled and valid blocks. */
    private boolean startConditionsMet() {
        int online = arenaPlayers().size();
        if (online < Math.max(1, Config.MIN_PLAYERS.getInt())) {
            return false;
        }
        if (readyPlayers.isEmpty() || readyPlayers.size() != online) {
            return false;
        }
        if (teams.hasEmptyTeam() || !validateBlocks()) {
            return false;
        }
        return true;
    }

    private void beginStart() {
        broadcast.all(Message.NOTICE_START);
        start();
    }

    public int playerCount() {
        return arenaPlayers().size();
    }

    /** Online players whose session points at this arena. */
    public List<Player> arenaPlayers() {
        List<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (sessions.arenaOf(player).map(a -> a == this).orElse(false)) {
                players.add(player);
            }
        }
        return players;
    }

    // Check if the game can start. If not, send the reason to the player; if
    // possible, start the game directly
    public void checkStartDemands(Player player) {
        // Game already start
        if (state == RoomState.RUNNING)
            return;

        // Not enough players
        if (!(arenaPlayers().size() > 1)) {
            player.sendMessage(Message.NOTICE_NOT_ENOUGH_PLAYERS.getString(player));
            return;
        }

        // Exist unready players
        if (!(readyPlayers.size() == arenaPlayers().size())) {
            List<String> unreadyPlayers = getOnlinePlayerNames();
            unreadyPlayers.removeAll(readyPlayers);
            player.sendMessage(Message.NOTICE_EXIST_UNREADY.getString(player));
            player.sendMessage(Message.NOTICE_UNREADY_PLAYERS.getString(player) + unreadyPlayers);
            return;
        }

        // Exist empty team
        if (teams.hasEmptyTeam()) {
            player.sendMessage(Message.NOTICE_EMPTY_TEAM.getString(player));
            return;
        }

        // Blocks have problems
        if (!validateBlocks()) {
            return;
        }

        // Start the game
        broadcast.all(Message.NOTICE_START);
        start();
    }

    public void start() {
        // Init
        state = RoomState.RUNNING;
        startedAt = System.currentTimeMillis();
        arenaPlayers().forEach(Player::closeInventory);
        sessions.getEditAmountPlayers().clear();
        blockState.setup(settings, catalog, new Random());
        Bukkit.getLogger().info("[BlockRacing] Blocks generate complete.");
        Bukkit.getLogger().info("Red team blocks: " + blockState.all(TeamColor.RED));
        Bukkit.getLogger().info("Blue team blocks: " + blockState.all(TeamColor.BLUE));
        redTotalBlockAmount = blockState.all(TeamColor.RED).size();
        blueTotalBlockAmount = blockState.all(TeamColor.BLUE).size();
        setLocateCost();
        Scoreboard.updateScoreboard();
        List<Player> starting = arenaPlayers();
        starting.forEach(player -> freeRandomTp.add(player.getName()));
        tickTask = new RunPer5Tick().runTaskTimer(Main.getInstance(), 0L, 5L);
        if (world == null) {
            world = top.lqsnow.blockracing.world.ArenaWorldService.get()
                    .createArenaWorld(worldName());
        }
        world.setDifficulty(Difficulty.EASY);
        world.setTime(1000);
        world.setStorm(false);
        world.setThundering(false);
        world.getEntities().stream().filter(e -> e instanceof Item).forEach(Entity::remove);
        world.setGameRule(GameRules.LOCATOR_BAR, false);

        // World border
        world.getWorldBorder().setCenter(world.getSpawnLocation());
        world.getWorldBorder().setSize(59999968);

        // Processing of unselected team players (spectators)
        inGamePlayers.clear();
        starting.forEach(player -> inGamePlayers.add(player.getName()));
        for (Player player : starting) {
            if (!teams.contains(player.getName())) {
                player.setGameMode(GameMode.SPECTATOR);
                player.teleport(world.getSpawnLocation());
                player.sendMessage(Message.NOTICE_SPECTATOR.getString(player));
                inGamePlayers.remove(player.getName());
            }
        }

        // Settings for each player
        for (String p : inGamePlayers) {
            Player player = Bukkit.getPlayer(p);
            initPlayer(player);
        }

        Bukkit.getLogger().info("Red team players: " + teams.players(TeamColor.RED));
        Bukkit.getLogger().info("Blue team players: " + teams.players(TeamColor.BLUE));
        if (settings.getGameMode() == GameModeType.NORMAL)
            Bukkit.getLogger().info("Game mode: Normal");
        else if (settings.getGameMode() == GameModeType.RACING)
            Bukkit.getLogger().info("Game mode: Racing");
        Bukkit.getLogger().info(settings.isSpeedMode() ? "Speed mode: On" : "Speed mode: Off");
        InstanceProgressStore.saveNow(this);
    }

    // Player init
    public void initPlayer(Player player) {
        // General
        player.getInventory().clear();
        randomTeleport(player, true);
        player.setHealth(20);
        player.setExp(0);
        player.setLevel(0);
        player.setFoodLevel(20);
        player.setSaturation(10);
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().addItem(ItemBuilder.of(Material.STONE_PICKAXE).build());
        player.getInventory().addItem(ItemBuilder.of(Material.STONE_AXE).build());
        player.getInventory().addItem(ItemBuilder.of(Material.STONE_SHOVEL).build());
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 1200, 4, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 1200, 4, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1200, 4, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0, false, false));

        // Speed mode
        if (settings.isSpeedMode()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, -1, 4, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 1, false, false));
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 1, false, false));
            }, 1300L); // 延迟发放 避免冲突
            player.getInventory().addItem(ItemBuilder.of(Material.IRON_PICKAXE)
                    .enchant(Enchantment.SILK_TOUCH, 1)
                    .build());
            player.getInventory().addItem(ItemBuilder.of(Material.GOLDEN_CARROT).amount(64).build());

            ItemStack damagedElytra = new ItemStack(Material.ELYTRA);
            ItemMeta elytraMeta = damagedElytra.getItemMeta();
            Damageable damageable = (Damageable) elytraMeta;
            damageable.setDamage(damagedElytra.getType().getMaxDurability() - 1);
            Repairable repairable = (Repairable) elytraMeta;
            repairable.setRepairCost(15);
            damagedElytra.setItemMeta(elytraMeta);
            player.getInventory().addItem(damagedElytra);

            ItemStack xpBook = new ItemStack(Material.ENCHANTED_BOOK);
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) xpBook.getItemMeta();
            meta.addStoredEnchant(Enchantment.MENDING, 1, true);
            xpBook.setItemMeta(meta);
            player.getInventory().addItem(xpBook);
        }
    }

    // Roll
    public void roll(Player player) {
        java.util.Optional<TeamColor> team = teams.teamOf(player.getName());
        if (team.isEmpty()) {
            return;
        }
        if (getRollCount(team.get()) >= settings.getMaxRollCount()) {
            player.sendMessage(rollLimitMessage(player));
            return;
        }
        List<String> rollRequesters = rollPlayers(team.get());
        if (!rollRequesters.contains(player.getName())) {
            rollRequesters.add(player.getName());
            broadcast.team(team.get(), Message.NOTICE_ROLL_REQUEST, (viewer, text) -> text.replace("%player%", player.getName()));
        } else {
            rollRequesters.remove(player.getName());
            broadcast.team(team.get(), Message.NOTICE_ROLL_REQUEST_CANCEL, (viewer, text) -> text.replace("%player%", player.getName()));
        }
    }

    public void locate(Player player) {
        if (locatePermission.contains(player.getName())) {
            player.sendMessage(Message.NOTICE_LOCATE_ALREADY_BOUGHT.getString(player));
            return;
        }
        java.util.Optional<TeamColor> team = teams.teamOf(player.getName());
        if (team.isEmpty()) {
            return;
        }
        if (getScore(team.get()) >= locateCost) {
            addScore(team.get(), -locateCost);
            Scoreboard.updateScoreboard();
            locatePermission.add(player.getName());
            broadcast.all(Message.NOTICE_BUY_LOCATE, (viewer, text) -> text.replace("%player%", player.getName()));
            locateAttachments.put(player.getName(),
                    player.addAttachment(Main.getInstance(), "minecraft.command.locate", true));
            InstanceProgressStore.saveNow(this);
        } else {
            player.sendMessage(Message.NOTICE_NOT_ENOUGH_SCORE.getString(player));
        }
    }

    public void buySupply(Player buyer) {
        if (!settings.isSpeedMode()) {
            buyer.sendMessage(Message.NOTICE_SUPPLY_SPEED_ONLY.getString(buyer));
            return;
        }
        java.util.Optional<TeamColor> team = teams.teamOf(buyer.getName());
        if (team.isEmpty()) {
            return;
        }
        if (getScore(team.get()) < 5) {
            buyer.sendMessage(Message.NOTICE_NOT_ENOUGH_SCORE.getString(buyer));
            return;
        }
        addScore(team.get(), -5);

        for (String playerName : teams.players(team.get())) {
            Player teammate = Bukkit.getPlayerExact(playerName);
            if (teammate == null) {
                continue;
            }
            giveOrDrop(teammate, new ItemStack(Material.GOLDEN_CARROT, 16));
            giveOrDrop(teammate, new ItemStack(Material.FIREWORK_ROCKET, 32));
        }
        TeamColor selectedTeam = team.get();
        broadcast.all(Message.NOTICE_SUPPLY_PURCHASED, (viewer, text) -> text
                .replace("%player%", buyer.getName())
                .replace("%team%", selectedTeam == TeamColor.RED
                        ? Message.TEAM_RED_NAME.getString(viewer)
                        : Message.TEAM_BLUE_NAME.getString(viewer)));
        Scoreboard.updateScoreboard();
        InstanceProgressStore.saveNow(this);
    }

    private String rollLimitMessage(Player player) {
        String message = Message.NOTICE_CANNOT_ROLL.getString(player);
        if (!message.contains("%count%")) {
            message = top.lqsnow.blockracing.managers.LanguageManager.usesChinese(player)
                    ? "§c您的队伍已经用完本局 %count% 次轮换机会！"
                    : "§cYour team has used all %count% rerolls for this game!";
        }
        return message.replace("%count%", String.valueOf(settings.getMaxRollCount()));
    }

    // Random Teleport
    public void randomTeleport(Player player, boolean avoidOcean) {
        World playerWorld = gameWorld();
        int maxAttempts = avoidOcean ? 12 : 1;
        startAsyncRandomTeleport(player, playerWorld, avoidOcean, 1, maxAttempts);
    }

    private void randomTeleportSynchronously(Player player, World playerWorld, boolean avoidOcean,
                                             int maxAttempts) {
        Random random = new Random();
        Location offset = null;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            Location candidate = RandomTeleportService.pollCandidate();
            double randX = candidate != null ? candidate.getX() : (random.nextInt(20000) - 10000);
            double randZ = candidate != null ? candidate.getZ() : (random.nextInt(20000) - 10000);
            Location current = playerWorld.getHighestBlockAt(new Location(playerWorld, randX, 0, randZ)).getLocation().add(0, 1, 0);
            offset = current;
            if (!avoidOcean || !isOcean(current.getBlock().getBiome())) {
                break;
            }
        }
        if (offset == null) return;
        completeRandomTeleport(player, offset, avoidOcean);
    }

    private void startAsyncRandomTeleport(Player player, World world, boolean avoidOcean,
                                          int attempt, int maxAttempts) {
        Random random = new Random();
        Location candidate = RandomTeleportService.pollCandidate();
        int blockX = candidate != null ? candidate.getBlockX() : random.nextInt(20000) - 10000;
        int blockZ = candidate != null ? candidate.getBlockZ() : random.nextInt(20000) - 10000;
        world.getChunkAtAsync(blockX >> 4, blockZ >> 4, true)
                .whenComplete((chunk, error) -> Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    if (!player.isOnline()) return;
                    if (error != null) {
                        if (attempt < maxAttempts) {
                            startAsyncRandomTeleport(player, world, avoidOcean, attempt + 1, maxAttempts);
                        } else {
                            randomTeleportSynchronously(player, world, avoidOcean, 1);
                        }
                        return;
                    }
                    Location offset = world.getHighestBlockAt(blockX, blockZ).getLocation().add(0, 1, 0);
                    if (avoidOcean && isOcean(offset.getBlock().getBiome()) && attempt < maxAttempts) {
                        startAsyncRandomTeleport(player, world, true, attempt + 1, maxAttempts);
                        return;
                    }
                    completeRandomTeleport(player, offset, avoidOcean);
                }));
    }

    private void completeRandomTeleport(Player player, Location offset, boolean avoidOcean) {
        player.teleport(offset);

        String x = String.format("%.1f", offset.getX());
        String y = String.format("%.1f", offset.getY());
        String z = String.format("%.1f", offset.getZ());

        player.sendMessage(Message.NOTICE_TP_SUCCESS.getString(player).replace("%x%", x).replace("%y%", y).replace("%z%", z));
        if (avoidOcean && isOcean(offset.getBlock().getBiome())) {
            player.sendMessage(Message.NOTICE_TP_OCEAN.getString(player));
        }
    }

    public void resetPregameInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().addItem(createRuleBook(player));
    }

    private ItemStack createRuleBook(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.title(Texts.component(Message.RULE_BOOK_TITLE.getString(player)));
        meta.author(Texts.component(Message.RULE_BOOK_AUTHOR.getString(player)));
        meta.pages(Texts.components(Message.RULE_BOOK_PAGES.getStringList(player)));
        book.setItemMeta(meta);
        return book;
    }

    private void giveOrDrop(Player player, ItemStack item) {
        player.getInventory().addItem(item).values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private boolean isOcean(Biome biome) {
        return biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.DEEP_COLD_OCEAN
                || biome == Biome.LUKEWARM_OCEAN || biome == Biome.DEEP_FROZEN_OCEAN || biome == Biome.COLD_OCEAN
                || biome == Biome.WARM_OCEAN || biome == Biome.DEEP_LUKEWARM_OCEAN || biome == Biome.FROZEN_OCEAN;
    }

    // Waypoints
    // Return value: true -> waypoint changed, false -> waypoint doesn't change
    public boolean waypoint(Player player, int index, ClickType clickType) {
        java.util.Optional<TeamColor> teamOpt = teams.teamOf(player.getName());
        if (teamOpt.isEmpty()) {
            return false;
        }
        TeamColor team = teamOpt.get();
        Location waypoint = getWaypoint(team, index);
        String action = "";
        if (clickType.equals(ClickType.LEFT)) {
            action = "left";
        } else if (clickType.equals(ClickType.RIGHT)) {
            action = "right";
        }

        switch (action) {
            case "left" -> {
                if (waypoint == null) {
                    setWaypoint(player, team, index);
                    return true;
                } else {
                    player.teleport(waypoint);
                    String x = String.format("%.1f", waypoint.getX());
                    String y = String.format("%.1f", waypoint.getY());
                    String z = String.format("%.1f", waypoint.getZ());

                    player.sendMessage(Message.NOTICE_TP_SUCCESS.getString(player).replace("%x%", x).replace("%y%", y)
                            .replace("%z%", z));
                    return false;
                }
            }
            case "right" -> removeWaypoint(player, team, index);
        }
        return false;
    }

    public Location getWaypoint(TeamColor team, int index) {
        return waypoints(team).get(index);
    }

    public void setWaypoint(Player player, TeamColor team, int index) {
        Location waypoint = player.getLocation();
        waypoints(team).put(index, waypoint);
        InstanceProgressStore.saveNow(this);
    }

    private void removeWaypoint(Player player, TeamColor team, int index) {
        Component message = LEGACY_SERIALIZER.deserialize(
                        Message.NOTICE_REMOVE_WAYPOINT.getString(player).replace("%index%", String.valueOf(index)))
                .clickEvent(ClickEvent.runCommand("/waypoint remove " + index));
        player.sendMessage(message);
        player.closeInventory();
    }

    // Run per 2t
    // Before the game, provide regeneration and saturation effects
    private class RunPer2Tick extends BukkitRunnable {
        @Override
        public void run() {
            for (Player player : arenaPlayers()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10, 255));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 10, 255));
            }
            if (state == RoomState.RUNNING)
                this.cancel();
        }
    }

    // Run per 5t
    // During the game
    private class RunPer5Tick extends BukkitRunnable {

        @Override
        public void run() {

            // Inventory check
            checkInventories();

            // Win check
            if (blockState.hasFinished(TeamColor.RED)) {
                end(TeamColor.RED);
                showRanking();
                this.cancel();
                return;
            }
            if (blockState.hasFinished(TeamColor.BLUE)) {
                end(TeamColor.BLUE);
                showRanking();
                this.cancel();
                return;
            }

            // Roll check
            if (!redRollPlayers.isEmpty())
                checkRoll(TeamColor.RED);
            if (!blueRollPlayers.isEmpty())
                checkRoll(TeamColor.BLUE);

        }
    }

    private void showRanking() {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(collectAmount.entrySet());
        entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        broadcast.all(Message.NOTICE_RANKING);
        for (Map.Entry<String, Integer> entry : entries) {
            java.util.Optional<TeamColor> team = teams.teamOf(entry.getKey());
            var message = team.map(color -> color == TeamColor.RED ? Message.NOTICE_RANKING_RED : Message.NOTICE_RANKING_BLUE)
                    .orElse(Message.NOTICE_RANKING_OFFLINE);
            broadcast.all(message, (viewer, text) -> text
                    .replace("%player%", entry.getKey()).replace("%amount%", entry.getValue().toString()));
        }
        broadcast.all(Message.NOTICE_RANKING_DIVIDER);
    }

    private void checkRoll(TeamColor team) {
        Set<String> rollSet = new HashSet<>(rollPlayers(team));
        Set<String> onlineSet = new HashSet<>(teams.onlinePlayers(team));
        if (!onlineSet.isEmpty() && rollSet.containsAll(onlineSet)) {
            List<String> b = new ArrayList<>(catalog.pool(settings));
            b.removeAll(blockState.all(team));
            Random random = new Random();
            int rollAmount = blockState.current(team).size();
            for (int i = 0; i < rollAmount; i++) {
                blockState.remaining(team).set(i, b.get(random.nextInt(b.size())));
            }
            broadcast.all(team == TeamColor.RED ? Message.NOTICE_RED_ROLL_SUCCESS : Message.NOTICE_BLUE_ROLL_SUCCESS);
            addRollCount(team);
            rollPlayers(team).clear();
            Scoreboard.updateScoreboard();
            InstanceProgressStore.saveNow(this);
        }
    }

    private void setLocateCost() {
        if (settings.getBlockAmount() <= 20)
            locateCost = 2;
        else if (settings.getBlockAmount() <= 50)
            locateCost = 3;
        else if (settings.getBlockAmount() <= 100)
            locateCost = 5;
        else if (settings.getBlockAmount() <= 200)
            locateCost = 8;
        else
            locateCost = 10;
    }

    private void checkInventories() {
        for (TeamColor team : TeamColor.values()) {
            // Complete from player
            for (String player : teams.players(team)) {
                for (String block : blockState.current(team)) {
                    Player p = Bukkit.getPlayer(player);
                    if (p == null)
                        continue;
                    if (p.getInventory().contains(Material.valueOf(block))) {
                        collectBlock(team, block, player);
                        return;
                    }
                }
            }
            // Complete from team chest
            for (String block : blockState.current(team)) {
                for (Inventory chest : chests(team)) {
                    if (chest.contains(Material.valueOf(block))) {
                        collectBlock(team, block, teamChestLabel(team).getString());
                        return;
                    }
                }
            }
        }
    }

    public void collectBlock(TeamColor team, String block, String player) {
        boolean collectedFromTeamChest = player.equals(teamChestLabel(team).getString());
        Message collectNotice = team == TeamColor.RED ? Message.NOTICE_RED_COLLECT : Message.NOTICE_BLUE_COLLECT;
        broadcast.all(collectNotice, (viewer, text) -> text
                .replace("%block%", TranslationUtil.getValue(block, viewer))
                .replace("%player%", collectedFromTeamChest
                        ? teamChestLabel(team).getString(viewer)
                        : player));
        Bukkit.getLogger().info(collectNotice.getString()
                .replace("%block%", TranslationUtil.getValue(block)).replace("%player%", player).replaceAll("§.", ""));
        broadcast.sound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        blockState.complete(team, block);
        if (settings.isSpeedMode())
            addScore(team, 3);
        else
            addScore(team, 1);
        if (team == TeamColor.RED)
            redCurrentBlockAmount += 1;
        else
            blueCurrentBlockAmount += 1;
        collect(player);
        Scoreboard.updateScoreboard();
        // Put items into the opponent's team chest
        if (settings.getGameMode() == GameModeType.NORMAL) {
            List<Inventory> opponentChests = chests(team.opponent());
            for (int i = opponentChests.size() - 1; i >= 0; i--) {
                Inventory chest = opponentChests.get(i);
                int emptyPos = chest.firstEmpty();
                if (emptyPos == -1) {
                    continue;
                }
                chest.setItem(emptyPos, Materials.stack(block, 64));
                InstanceProgressStore.saveNow(this);
                return;
            }
            Message opponentName = team == TeamColor.RED ? Message.TEAM_BLUE_NAME : Message.TEAM_RED_NAME;
            broadcast.all(Message.NOTICE_TEAM_CHEST_FULL, (viewer, text) -> text
                    .replace("%team%", opponentName.getString(viewer))
                    .replace("%block%", TranslationUtil.getValue(block, viewer)));
        }
        InstanceProgressStore.saveNow(this);
    }

    private Message teamChestLabel(TeamColor team) {
        return team == TeamColor.RED ? Message.NOTICE_RED_TEAM_CHEST : Message.NOTICE_BLUE_TEAM_CHEST;
    }

    public void end(TeamColor winner) {
        Message winNotice = winner == TeamColor.RED ? Message.NOTICE_RED_WIN : Message.NOTICE_BLUE_WIN;
        for (Player player : arenaPlayers()) {
            player.closeInventory();
            player.showTitle(Title.title(
                    LEGACY_SERIALIZER.deserialize(winNotice.getString(player)),
                    Component.empty()
            ));
            player.setGameMode(GameMode.SPECTATOR);
        }
        broadcast.all(winNotice);
        broadcast.sound(Sound.UI_TOAST_CHALLENGE_COMPLETE);
        state = RoomState.ENDING;
        InstanceProgressStore.clear();
        GameManager.get().scheduleDispose(this);
    }

    /** Watchdog exit: no winner, no titles, straight to recycling. */
    public void forceEnd() {
        if (state != RoomState.RUNNING) {
            return;
        }
        Bukkit.getLogger().warning("[BlockRacing] Game " + id + " exceeded the time limit and was force-ended.");
        for (Player player : arenaPlayers()) {
            player.closeInventory();
            player.setGameMode(GameMode.SPECTATOR);
        }
        state = RoomState.ENDING;
        InstanceProgressStore.clear();
        GameManager.get().scheduleDispose(this);
    }

    /** Recycles the arena: players go back to a fresh waiting game, the world
     * folder is moved to backups and deleted in the background. */
    public void dispose() {
        if (state == RoomState.DISPOSED) {
            return;
        }
        state = RoomState.DISPOSED;
        if (waitingTask != null) {
            waitingTask.cancel();
        }
        if (tickTask != null) {
            tickTask.cancel();
        }
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        List<Player> online = new ArrayList<>();
        for (String name : sessions.boundPlayers(this)) {
            Player player = Bukkit.getPlayerExact(name);
            sessions.leaveArena(name);
            if (player != null) {
                // Recycle the locate permission granted for this game.
                org.bukkit.permissions.PermissionAttachment attachment = locateAttachments.remove(name);
                if (attachment != null) {
                    player.removeAttachment(attachment);
                }
                online.add(player);
            } else {
                // Offline: drop the pending lobby restore so it does not linger.
                sessions.dropSavedSession(name);
            }
        }
        locateAttachments.clear();
        GameManager.get().remove(this);
        GameInstance fresh = GameManager.get().createGame();
        for (Player player : online) {
            top.lqsnow.blockracing.lobby.Lobby.get().sendToLobby(player);
            fresh.playerLogin(player);
        }
        if (world != null) {
            top.lqsnow.blockracing.world.ArenaWorldService.get().deleteArenaWorld(world);
            world = null;
        }
    }

    /** Clamps the room's target amount to the currently available pool. */
    public boolean refreshAvailableBlocksAndClampAmount() {
        int clamped = BlockCatalog.clampBlockAmount(settings.getBlockAmount(), catalog.maxBlockAmount(settings));
        if (clamped == settings.getBlockAmount()) {
            return false;
        }
        settings.setBlockAmount(clamped);
        return true;
    }

    /** Validates the selectable pool for the current settings (original Block.checkBlock). */
    public boolean validateBlocks() {        boolean flag = true;
        for (String str : catalog.pool(settings)) {
            try {
                Material material = Materials.require(str);
                if (!material.isItem()) {
                    throw new IllegalArgumentException("Material cannot be represented as an item: " + str);
                }
            } catch (Exception e) {
                Bukkit.getLogger().severe(String.format("[BlockRacing] " + Message.NOTICE_ERROR_BLOCK.getString(), str));
                broadcast.all(Message.NOTICE_ERROR_BLOCK,
                        (viewer, text) -> String.format(text, str));
                flag = false;
            }
        }
        return flag;
    }

    public List<String> currentBlocks(TeamColor team) {
        return blockState.current(team);
    }

    public List<String> getOnlinePlayerNames() {
        List<String> onlinePlayers = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach((Player player) -> onlinePlayers.add(player.getName()));
        return onlinePlayers;
    }

    public void playSound(Sound sound) {
        broadcast.sound(sound);
    }

    public String getCoords(Location location) {
        return String.format("%.1f, %.1f, %.1f", location.getX(), location.getY(), location.getZ());
    }

    public void collect(String p) {
        collectAmount.merge(p, 1, Integer::sum);
    }

    /** The arena world once the game has started; the pregame world before that. */
    private World gameWorld() {
        return world != null ? world : getPrimaryWorld();
    }

    private String worldName() {
        String prefix = Config.WORLD_PREFIX.getString();
        return (prefix == null ? "arena_" : prefix) + id;
    }

    private World getPrimaryWorld() {
        return Bukkit.getWorlds().stream()
                .filter(candidate -> candidate.getEnvironment() == World.Environment.NORMAL)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No overworld is loaded"));
    }

    // ------------------------------------------------------------------
    // State accessors
    // ------------------------------------------------------------------

    public int getId() {
        return id;
    }

    public long getStartedAt() {
        return startedAt;
    }

    /** Binds a world that already exists on disk (crash recovery). */
    public void adoptWorld(World world) {
        this.world = world;
    }

    public RoomState getState() {
        return state;
    }

    public void setState(RoomState state) {
        this.state = state;
    }

    public TeamState getTeams() {
        return teams;
    }

    /** Joins the team with the historical messages; syncs scoreboard entries. */
    public boolean joinTeam(Player player, TeamColor color) {
        List<String> members = teams.players(color);
        if (members.contains(player.getName())) {
            player.sendMessage(color == TeamColor.RED
                    ? Message.NOTICE_ALREADY_IN_RED.getString(player)
                    : Message.NOTICE_ALREADY_IN_BLUE.getString(player));
            return false;
        }
        if (!teams.add(player.getName(), color)) {
            return false;
        }
        Scoreboard.syncPlayerTeams();
        broadcast.all(color == TeamColor.RED ? Message.NOTICE_JOIN_RED : Message.NOTICE_JOIN_BLUE,
                (viewer, text) -> text.replace("%player%", player.getName()));
        return true;
    }

    public GameBroadcast broadcast() {
        return broadcast;
    }

    public BlockState getBlockState() {
        return blockState;
    }

    public RoomSettings getSettings() {
        return settings;
    }

    public BlockCatalog getCatalog() {
        return catalog;
    }

    public Set<String> getReadyPlayers() {
        return readyPlayers;
    }

    public List<Inventory> chests(TeamColor team) {
        return team == TeamColor.RED ? redChests : blueChests;
    }

    public Map<Integer, Location> waypoints(TeamColor team) {
        return team == TeamColor.RED ? redWaypoints : blueWaypoints;
    }

    public Map<Integer, Material> waypointIcons(TeamColor team) {
        return team == TeamColor.RED ? redWaypointIcons : blueWaypointIcons;
    }

    public List<String> rollPlayers(TeamColor team) {
        return team == TeamColor.RED ? redRollPlayers : blueRollPlayers;
    }

    public int getScore(TeamColor team) {
        return team == TeamColor.RED ? redScore : blueScore;
    }

    public void setScore(TeamColor team, int score) {
        if (team == TeamColor.RED) redScore = score;
        else blueScore = score;
    }

    public void addScore(TeamColor team, int amount) {
        setScore(team, getScore(team) + amount);
    }

    public int getCurrentAmount(TeamColor team) {
        return team == TeamColor.RED ? redCurrentBlockAmount : blueCurrentBlockAmount;
    }

    public void setCurrentAmount(TeamColor team, int amount) {
        if (team == TeamColor.RED) redCurrentBlockAmount = amount;
        else blueCurrentBlockAmount = amount;
    }

    public int getTotalAmount(TeamColor team) {
        return team == TeamColor.RED ? redTotalBlockAmount : blueTotalBlockAmount;
    }

    public void setTotalAmount(TeamColor team, int amount) {
        if (team == TeamColor.RED) redTotalBlockAmount = amount;
        else blueTotalBlockAmount = amount;
    }

    public int getRollCount(TeamColor team) {
        return team == TeamColor.RED ? redRollCount : blueRollCount;
    }

    public void setRollCount(TeamColor team, int count) {
        if (team == TeamColor.RED) redRollCount = count;
        else blueRollCount = count;
    }

    public void addRollCount(TeamColor team) {
        setRollCount(team, getRollCount(team) + 1);
    }

    public List<String> getFreeRandomTp() {
        return freeRandomTp;
    }

    public List<String> getLocatePermission() {
        return locatePermission;
    }

    public int getLocateCost() {
        return locateCost;
    }

    public void setLocateCost(int cost) {
        this.locateCost = cost;
    }

    public Map<String, Integer> getCollectAmount() {
        return collectAmount;
    }

    public List<String> getInGamePlayers() {
        return inGamePlayers;
    }
}
