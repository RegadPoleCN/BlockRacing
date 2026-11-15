package top.lqsnow.blockracing.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.game.GameModeType;
import top.lqsnow.blockracing.game.RoomSettings;
import top.lqsnow.blockracing.pools.BlockCatalog;
import top.lqsnow.blockracing.pools.CustomSettings;
import top.lqsnow.blockracing.pools.DifficultyPreset;
import top.lqsnow.blockracing.pools.DisplayDifficulty;
import top.lqsnow.blockracing.pools.LocalBlockPool;
import top.lqsnow.blockracing.pools.PoolEntry;
import top.lqsnow.blockracing.pools.TaskSession;
import top.lqsnow.blockracing.pools.TierSummary;
import top.lqsnow.blockracing.toolkit.material.Materials;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Global block-library resources: reviewed pools, difficulty presets and the
 * custom selection. Per-match generation lives in each arena's TaskSession
 * (see GameInstance), replacing the former static match state. */
public class Block {
    // Retained for legacy saves and debug commands. Fresh matches never read them.
    public static List<String> easyBlocks = List.of(), mediumBlocks = List.of(), hardBlocks = List.of(),
            dyedBlocks = List.of(), endBlocks = List.of();
    private static BlockCatalog catalog;
    private static final Map<DifficultyPreset.Kind, DifficultyPreset> presets = new EnumMap<>(DifficultyPreset.Kind.class);
    private static CustomSettings customSettings;

    public Block() { reloadBlock(); }

    private static void saveIfAbsent(String resource) {
        if (!Files.exists(Main.getInstance().getDataFolder().toPath().resolve(resource))) {
            Main.getInstance().saveResource(resource, false);
        }
    }

    public static void reloadBlock() {
        try {
            saveIfAbsent("block-pools/material-metadata.tsv");
            saveIfAbsent("block-pools/catalogue.yml");
            saveIfAbsent("block-pools/copper-basic.txt");
            saveIfAbsent("block-pools/copper-extras.txt");
            saveIfAbsent("block-pools/acquisition-tags.tsv");
            for (PoolEntry.Dimension dimension : PoolEntry.Dimension.values()) {
                for (int tier = 1; tier <= 6; tier++) {
                    saveIfAbsent("block-pools/" + dimension.name().toLowerCase(Locale.ROOT) + "/tier-" + tier + ".txt");
                }
            }
            for (DifficultyPreset.Kind kind : DifficultyPreset.Kind.standards()) {
                String resource = "presets/" + kind.id() + ".yml";
                saveIfAbsent(resource);
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.options().parseComments(true);
                yaml.load(Main.getInstance().getDataFolder().toPath().resolve(resource).toFile());
                DifficultyPreset preset = DifficultyPreset.load(yaml);
                if (preset.kind() != kind) throw new IllegalArgumentException("Preset ID does not match " + resource);
                boolean migrated = false;
                if (!yaml.isSet("pacing")) {
                    preset.pacing().save(yaml.createSection("pacing"));
                    migrated = true;
                }
                if (!yaml.isSet("tier-progression")) {
                    preset.tierProgression().save(yaml.createSection("tier-progression"));
                    migrated = true;
                }
                if (migrated) yaml.save(Main.getInstance().getDataFolder().toPath().resolve(resource).toFile());
                presets.put(kind, preset);
            }
            catalog = BlockCatalog.load(Main.getInstance().getDataFolder().toPath().resolve("block-pools"));
            BlockCatalog.validateLive(catalog.entries());
            saveIfAbsent("custom/settings.yml");
            saveIfAbsent("custom/blocks.yml");
            YamlConfiguration customYaml = new YamlConfiguration();
            customYaml.load(Main.getInstance().getDataFolder().toPath().resolve("custom/settings.yml").toFile());
            customSettings = CustomSettings.load(customYaml);
            easyBlocks = readLegacy("EasyBlocks.txt");
            mediumBlocks = readLegacy("MediumBlocks.txt");
            hardBlocks = readLegacy("HardBlocks.txt");
            dyedBlocks = readLegacy("DyedBlocks.txt");
            endBlocks = readLegacy("EndBlocks.txt");
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to load reviewed block pools/presets; inspect the named file and Material ID", ex);
        }
    }

    private static List<String> readLegacy(String name) throws IOException {
        Path path = Main.getInstance().getDataFolder().toPath().resolve(name);
        return Files.readAllLines(path, StandardCharsets.UTF_8).stream().map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#")).toList();
    }

    public static DifficultyPreset preset(DifficultyPreset.Kind kind, RoomSettings settings) {
        return kind == DifficultyPreset.Kind.CUSTOM ? customSelection(customSettings, settings).preset() : presets.get(kind);
    }
    public static CustomSettings customSettings() { return customSettings; }

    public static void saveCustomSettings(CustomSettings next) {
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.options().parseComments(true);
            Path path = Main.getInstance().getDataFolder().toPath().resolve("custom/settings.yml");
            yaml.load(path.toFile());
            next.save(yaml);
            yaml.save(path.toFile());
            customSettings = next;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to save custom/settings.yml: " + ex.getMessage(), ex);
        }
    }

    /** One resolved block selection: the preset that governs pacing plus the pool it draws from. */
    public record Selection(DifficultyPreset preset, List<PoolEntry> pool, boolean copperExtras) {}

    public static Selection selectionFor(RoomSettings settings) {
        return selection(settings.getDifficulty(), settings.isCopperExtras());
    }

    private static Selection selection(DifficultyPreset.Kind kind, boolean copperExtras) {
        return kind == DifficultyPreset.Kind.CUSTOM ? customSelection(customSettings, copperExtras)
                : new Selection(presets.get(kind), catalog.entries(), copperExtras);
    }

    private static Selection customSelection(CustomSettings custom, boolean copperExtras) {
        if (custom.source() == CustomSettings.Source.TIERS) return new Selection(custom.preset(), catalog.entries(), copperExtras);
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(Main.getInstance().getDataFolder().toPath().resolve("custom/blocks.yml").toFile());
            LocalBlockPool.Configuration local = LocalBlockPool.read(yaml, catalog);
            // Local files explicitly choose their blocks and thresholds, independently of GUI settings.
            return new Selection(local.preset(), local.entries(), true);
        } catch (Exception ex) {
            throw new IllegalArgumentException("custom/blocks.yml: " + ex.getMessage(), ex);
        }
    }

    private static Selection customSelection(CustomSettings custom, RoomSettings settings) {
        return customSelection(custom, settings.isCopperExtras());
    }

    /** Applies a custom source to the shared custom settings and selects it for this arena. */
    public static void activateCustom(top.lqsnow.blockracing.core.GameInstance arena, CustomSettings.Source source) {
        RoomSettings settings = arena.getSettings();
        CustomSettings next = customSettings.withSource(source);
        createSession(customSelection(next, settings), settings, 0L); // Validate before changing the active selection.
        saveCustomSettings(next);
        settings.setDifficulty(DifficultyPreset.Kind.CUSTOM);
    }

    /** Creates a session for an explicit selection; used to validate and to start matches. */
    private static TaskSession createSession(Selection selection, RoomSettings settings, long seed) {
        return new TaskSession(selection.preset(), selection.copperExtras(), settings.getBlockAmount(),
                settings.getMaxRollCount(), settings.getGameMode() == GameModeType.RACING, seed, selection.pool());
    }

    /** Creates a throwaway session with the room's totals; used to validate settings. */
    public static TaskSession createSession(RoomSettings settings, long seed) {
        return createSession(selectionFor(settings), settings, seed);
    }

    public static void validateSettings(RoomSettings settings) {
        createSession(settings, 0L);
    }

    public static boolean checkBlock() {
        try {
            validateSettings(RoomSettings.fromConfig());
            return true;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Bukkit.getLogger().severe("[BlockRacing] Invalid block-library settings: " + ex.getMessage());
            return false;
        }
    }

    public static List<PoolEntry> candidates(DifficultyPreset.Kind kind, boolean copperExtras) {
        Selection selection = selection(kind, copperExtras);
        return selection.pool().stream().filter(selection.preset()::allows)
                .filter(e -> selection.copperExtras() || !e.copperExtra()).toList();
    }

    /** How many target blocks the current selection can actually provide. */
    public static int capacityFor(RoomSettings settings) {
        return candidates(settings.getDifficulty(), settings.isCopperExtras()).size();
    }

    public static TierSummary tierSummary(PoolEntry.Dimension dimension, int tier, boolean copperExtras) {
        return TierSummary.from(catalog.entries(), dimension, tier, copperExtras);
    }

    public static PoolEntry find(String id) {
        String key = Materials.require(id).getKey().toString();
        return catalog.find(key);
    }

    public static DisplayDifficulty displayDifficulty(String id) {
        PoolEntry entry = find(id);
        return entry == null ? DisplayDifficulty.EASY : DisplayDifficulty.forTier(entry.tier());
    }

    public static int clampBlockAmount(int requestedAmount, int availableBlocks) {
        if (availableBlocks < 0) throw new IllegalArgumentException("Available block amount cannot be negative");
        return Math.min(Math.max(10, requestedAmount), availableBlocks);
    }

    // Compatibility helpers for existing extensions. Fresh matches use preset tier weights exclusively.
    @Deprecated public static int calculateEasyBlocksWeight(float p) { return (int) (100 - 80 * p); }
    @Deprecated public static int calculateMediumBlocksWeight(float p) { return p <= .4 ? (int) (20 + 40 * p / .4) : 60; }
    @Deprecated public static int calculateHardBlocksWeight(float p) { return p <= .5 ? (int) (1 + 19 * p / .5) : (int) (20 + 40 * (p - .5) / .5); }
    @Deprecated public static int calculateDyedBlocksWeight(float p) { return 10; }
}
