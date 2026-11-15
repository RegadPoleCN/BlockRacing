import org.bukkit.configuration.file.YamlConfiguration;
import top.lqsnow.blockracing.pools.*;

import java.nio.file.*;
import java.util.*;

/** Developer smoke check against the real Paper registries. Does not start a server or generate worlds. */
public class VerifyTaskRuntime {
    private static void require(boolean value, String reason) {
        if (!value) throw new IllegalStateException(reason);
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]).toAbsolutePath();
        Class.forName("net.minecraft.SharedConstants").getMethod("tryDetectVersion").invoke(null);
        Class.forName("net.minecraft.server.Bootstrap").getMethod("bootStrap").invoke(null);
        BlockCatalog catalog = BlockCatalog.load(root.resolve("src/main/resources/block-pools"));
        BlockCatalog.validateLive(catalog.entries());
        int matches = 0;
        for (var kind : DifficultyPreset.Kind.standards()) {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(root.resolve("src/main/resources/presets/" + kind.id() + ".yml").toFile());
            for (boolean copper : List.of(false, true)) for (boolean racing : List.of(false, true)) {
                TaskSession session = new TaskSession(DifficultyPreset.load(yaml), copper, 50, 3, racing, 78, catalog.entries());
                for (String team : List.of("red", "blue")) {
                    for (int completed = 0; completed < 50; completed++) {
                        if (completed == 7 || completed == 26) session.roll(team);
                        for (String id : session.active(team)) {
                            require(session.preset().unlocked(session.find(id), completed, 50), "Premature target " + id);
                        }
                        require(session.complete(team, session.active(team).getLast()), "Could not complete target");
                        if (completed == 24) {
                            YamlConfiguration saved = new YamlConfiguration();
                            session.save(saved.createSection("tasks"));
                            YamlConfiguration restored = new YamlConfiguration();
                            restored.loadFromString(saved.saveToString());
                            session = TaskSession.load(restored.getConfigurationSection("tasks"));
                        }
                    }
                    require(session.completed(team) == 50 && session.active(team).isEmpty(), "Wrong target budget");
                }
                matches++;
            }
        }

        // A real pre-pacing v2 fixture must continue its original sequence, not adopt new defaults.
        // The legacy v1 queue restore itself lives in GameInstance.restoreTasks and needs a running
        // arena; the pools engine contract is verified here.
        YamlConfiguration oldRules = new YamlConfiguration();
        oldRules.load(root.resolve("src/test/resources/legacy-task-session-v2.yml").toFile());
        TaskSession restored = TaskSession.load(oldRules.getConfigurationSection("tasks"), false);
        require(!restored.preset().pacing().enabled(), "Old match adopted pacing");
        for (String team : List.of("red", "blue")) {
            for (String frame : oldRules.getStringList("expected." + team)) {
                require(frame.equals(String.join(",", restored.active(team))), "Old v2 future sequence changed");
                if (!restored.active(team).isEmpty()) require(restored.complete(team, restored.active(team).getFirst()), "Old v2 completion failed");
            }
        }

        // Restore a new match through the same adapter path the arena uses (format 3 keeps pacing).
        TaskSession match = new TaskSession(DifficultyPreset.defaults(DifficultyPreset.Kind.HARD), false, 50, 3, true, 77, catalog.entries());
        for (int i = 0; i < 17; i++) match.complete("red", match.active("red").getFirst());
        require(match.roll("red"), "Expected legal roll");
        YamlConfiguration snapshot = new YamlConfiguration();
        snapshot.set("format-version", 3);
        match.save(snapshot.createSection("tasks"));
        YamlConfiguration reloaded = new YamlConfiguration();
        reloaded.loadFromString(snapshot.saveToString());
        TaskSession adapter = TaskSession.load(reloaded.getConfigurationSection("tasks"), true);
        require(adapter.completed("red") == 17 && adapter.total() == 50 && adapter.rolls("red") == 1, "Adapter restored wrong counters");
        require(adapter.active("red").equals(match.active("red")), "Adapter changed active slots");
        require(adapter.preset().pacing().enabled(), "New match lost pacing rules");
        require(adapter.complete("red", adapter.active("red").getFirst()), "Adapter completion failed");
        require(adapter.completed("red") == 18 && adapter.active("red").size() == 4, "Adapter refill failed");
        String result = "{\"paper\":\"26.3.build.244-stable\",\"simulated_matches\":" + matches
                + ",\"snapshot_version\":3,\"legacy_v2_fixture_sequence_preserved\":true,\"snapshot_roundtrip\":true,\"status\":\"passed\"}\n";
        Files.writeString(root.resolve(".runtime/material-audit/task-runtime-verification.json"), result);
    }
}
