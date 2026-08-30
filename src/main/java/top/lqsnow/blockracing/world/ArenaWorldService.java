package top.lqsnow.blockracing.world;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import top.lqsnow.blockracing.Main;

/**
 * Creates and recycles per-game arena worlds.
 *
 * Deletion strategy ("move to backup"): unload without saving, rename the
 * world folder into world-backups/ (an instant metadata operation on the same
 * volume, freeing the world name immediately for the next game), then delete
 * the backup asynchronously. Locked files (e.g. session.lock) are skipped
 * during the move; deletion failures never block the game.
 */
public final class ArenaWorldService {
    private static final DateTimeFormatter BACKUP_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());

    private static ArenaWorldService instance;

    private final Path serverRoot;
    private final ExecutorService deleteExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "BlockRacing-WorldCleanup");
        thread.setDaemon(true);
        return thread;
    });

    private ArenaWorldService() {
        Path dataFolder = Main.getInstance().getDataFolder().toPath().toAbsolutePath().normalize();
        Path pluginsFolder = dataFolder.getParent();
        Path resolvedRoot = pluginsFolder == null ? null : pluginsFolder.getParent();
        if (resolvedRoot == null) {
            throw new IllegalStateException("Unable to resolve the server root from " + dataFolder);
        }
        this.serverRoot = resolvedRoot;
    }

    public static void init() {
        if (instance == null) {
            instance = new ArenaWorldService();
        }
    }

    public static ArenaWorldService get() {
        if (instance == null) {
            throw new IllegalStateException("ArenaWorldService not initialized");
        }
        return instance;
    }

    public World createArenaWorld(String worldName) {
        World world = Bukkit.createWorld(new WorldCreator(worldName)
                .environment(World.Environment.NORMAL)
                .seed(new Random().nextLong()));
        if (world == null) {
            throw new IllegalStateException("Failed to create arena world " + worldName);
        }
        world.setKeepSpawnInMemory(false);
        world.setAutoSave(false);
        return world;
    }

    /** Unloads the world and moves its folder into the backup area; actual
     * deletion happens on the background thread. */
    public void deleteArenaWorld(World world) {
        String name = world.getName();
        Bukkit.unloadWorld(world, false);
        Path source = serverRoot.resolve(name);
        if (!Files.isDirectory(source)) {
            return;
        }
        try {
            Path backup = uniqueBackupDirectory(name);
            Files.createDirectories(backup);
            List<Move> completed = new ArrayList<>();
            moveWorldContents(source, backup, completed);
            deleteExecutor.submit(() -> deleteQuietlyWithRetry(backup, 3));
        } catch (IOException ex) {
            Main.getInstance().getLogger().log(Level.SEVERE,
                    "Could not move arena world " + name + " to backups; leaving folder in place.", ex);
        }
    }

    /** Startup cleanup: arena world folders left over from a crash are moved
     * into the backup area, and the backup count is pruned to the limit. */
    public void cleanupOrphansAndPruneBackups(String arenaPrefix, int keep) {
        try (Stream<Path> entries = Files.list(serverRoot)) {
            for (Path entry : entries.toList()) {
                String name = entry.getFileName().toString();
                if (!name.startsWith(arenaPrefix) || !Files.isDirectory(entry)) {
                    continue;
                }
                if (Bukkit.getWorld(name) != null) {
                    continue;
                }
                Path backup = uniqueBackupDirectory(name);
                Files.createDirectories(backup);
                moveWorldContents(entry, backup, new ArrayList<>());
                Main.getInstance().getLogger().info("Moved leftover arena world " + name + " to backups.");
            }
        } catch (IOException ex) {
            Main.getInstance().getLogger().log(Level.WARNING, "Arena orphan cleanup failed.", ex);
        }
        pruneBackups(arenaPrefix, keep);
    }

    private void pruneBackups(String arenaPrefix, int keep) {
        Path backups = backupRoot();
        if (!Files.isDirectory(backups) || keep < 0) {
            return;
        }
        try (Stream<Path> entries = Files.list(backups)) {
            List<Path> candidates = entries
                    .filter(p -> p.getFileName().toString().startsWith(arenaPrefix + "_"))
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparingLong(this::lastModifiedOrZero))
                    .toList();
            int excess = candidates.size() - keep;
            for (int i = 0; i < excess; i++) {
                Path candidate = candidates.get(i);
                deleteExecutor.submit(() -> deleteQuietlyWithRetry(candidate, 1));
            }
        } catch (IOException ex) {
            Main.getInstance().getLogger().log(Level.WARNING, "Backup pruning failed.", ex);
        }
    }

    private long lastModifiedOrZero(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ex) {
            return 0L;
        }
    }

    private Path backupRoot() {
        return serverRoot.resolve("world-backups");
    }

    private Path uniqueBackupDirectory(String worldName) {
        Path backups = backupRoot();
        String base = worldName + "_" + BACKUP_TIME.format(Instant.now());
        Path candidate = backups.resolve(base);
        int suffix = 1;
        while (Files.exists(candidate)) {
            candidate = backups.resolve(base + "-" + suffix++);
        }
        return candidate;
    }

    private void deleteQuietlyWithRetry(Path directory, int attempts) {
        for (int attempt = 1; attempt <= attempts; attempt++) {
            if (!Files.exists(directory)) {
                return;
            }
            try (Stream<Path> walk = Files.walk(directory)) {
                List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
                boolean allDeleted = true;
                for (Path path : paths) {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException locked) {
                        allDeleted = false;
                    }
                }
                if (allDeleted) {
                    return;
                }
            } catch (IOException ex) {
                Main.getInstance().getLogger().log(Level.WARNING,
                        "Backup cleanup pass failed for " + directory, ex);
            }
            if (attempt < attempts) {
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        Main.getInstance().getLogger().warning("Backup directory " + directory
                + " could not be fully deleted; it will be pruned on next startup.");
    }

    // ------------------------------------------------------------------
    // Resilient folder move (ported from WorldResetMarker)
    // ------------------------------------------------------------------

    /** Paper holds the primary world's session.lock; other worlds can have
     * transient locks on Windows. Move file-by-file and skip locked files so
     * the rest of the folder still gets out of the way. */
    private void moveWorldContents(Path world, Path destination, List<Move> completed) throws IOException {
        Files.createDirectories(destination);
        try (var entries = Files.list(world)) {
            for (Path source : entries.toList()) {
                if (source.getFileName().toString().equals("session.lock")) {
                    continue;
                }
                Path target = destination.resolve(source.getFileName().toString());
                try {
                    move(source, target);
                    completed.add(new Move(source, target));
                } catch (IOException lockedFile) {
                    Main.getInstance().getLogger().warning(
                            "Could not move " + source + " to backups: " + lockedFile);
                }
            }
        }
    }

    private void move(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
            Files.move(source, destination);
        }
    }

    private record Move(Path source, Path destination) {
    }
}
