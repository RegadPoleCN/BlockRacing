package top.lqsnow.blockracing.game;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;

import top.lqsnow.blockracing.Main;

/** Read-only block pools loaded from the data folder, plus the pure weight
 * functions shared by every game instance. */
public final class BlockCatalog {
    private List<String> easyBlocks;
    private List<String> mediumBlocks;
    private List<String> hardBlocks;
    private List<String> dyedBlocks;
    private List<String> endBlocks;

    public BlockCatalog(List<String> easyBlocks, List<String> mediumBlocks, List<String> hardBlocks,
                        List<String> dyedBlocks, List<String> endBlocks) {
        this.easyBlocks = List.copyOf(easyBlocks);
        this.mediumBlocks = List.copyOf(mediumBlocks);
        this.hardBlocks = List.copyOf(hardBlocks);
        this.dyedBlocks = List.copyOf(dyedBlocks);
        this.endBlocks = List.copyOf(endBlocks);
    }

    public static BlockCatalog load(File dataFolder) {
        return new BlockCatalog(
                List.of(readFile(dataFolder, "EasyBlocks.txt")),
                List.of(readFile(dataFolder, "MediumBlocks.txt")),
                List.of(readFile(dataFolder, "HardBlocks.txt")),
                List.of(readFile(dataFolder, "DyedBlocks.txt")),
                List.of(readFile(dataFolder, "EndBlocks.txt")));
    }

    public void reload(File dataFolder) {
        easyBlocks = List.of(readFile(dataFolder, "EasyBlocks.txt"));
        mediumBlocks = List.of(readFile(dataFolder, "MediumBlocks.txt"));
        hardBlocks = List.of(readFile(dataFolder, "HardBlocks.txt"));
        dyedBlocks = List.of(readFile(dataFolder, "DyedBlocks.txt"));
        endBlocks = List.of(readFile(dataFolder, "EndBlocks.txt"));
    }

    public List<String> getEasyBlocks() {
        return easyBlocks;
    }

    public List<String> getMediumBlocks() {
        return mediumBlocks;
    }

    public List<String> getHardBlocks() {
        return hardBlocks;
    }

    public List<String> getDyedBlocks() {
        return dyedBlocks;
    }

    public List<String> getEndBlocks() {
        return endBlocks;
    }

    /** The pool of blocks selectable with the current room settings
     * (original Block.blocks / Block.allBlocks). */
    public List<String> pool(RoomSettings settings) {
        List<String> all = new ArrayList<>(easyBlocks);
        if (settings.isEnableMediumBlock()) all.addAll(mediumBlocks);
        if (settings.isEnableHardBlock()) all.addAll(hardBlocks);
        if (settings.isEnableDyedBlock()) all.addAll(dyedBlocks);
        if (settings.isEnableEndBlock()) all.addAll(endBlocks);
        return all;
    }

    public int maxBlockAmount(RoomSettings settings) {
        return pool(settings).size();
    }

    public static int clampBlockAmount(int requestedAmount, int availableBlocks) {
        if (availableBlocks < 0) {
            throw new IllegalArgumentException("Available block amount cannot be negative");
        }
        return Math.min(Math.max(10, requestedAmount), availableBlocks);
    }

    // Weight decreases from 100 to 20 as progress goes from 0 to 1
    public static int calculateEasyBlocksWeight(float progress) {
        return (int) (100 - 80 * progress);
    }

    // Weight increases from 20 to 60 as progress goes from 0 to 0.4,
    // then remains constant at 60 as progress goes from 0.4 to 1
    public static int calculateMediumBlocksWeight(float progress) {
        if (progress <= 0.4) {
            return (int) (20 + 40 * progress / 0.4);
        } else {
            return 60;
        }
    }

    // Weight increases from 1 to 20 as progress goes from 0 to 0.5,
    // then increases from 20 to 60 as progress goes from 0.5 to 1
    public static int calculateHardBlocksWeight(float progress) {
        if (progress <= 0.5) {
            return (int) (1 + 19 * progress / 0.5);
        } else {
            return (int) (20 + 40 * (progress - 0.5) / 0.5);
        }
    }

    // Weight remains constant at 10 regardless of progress
    public static int calculateDyedBlocksWeight(float progress) {
        return 10;
    }

    // Weight is 0 for progress from 0 to 0.8 (unless only end blocks remain),
    // then increases to 60 as progress goes from 0.8 to 1
    public static int calculateEndBlocksWeight(float progress, int endPoolSize, int totalPoolSize) {
        float prop = (float) endPoolSize / totalPoolSize;
        if (progress <= 1 - prop) {
            if (progress <= 0.8) return 0;
            if (progress > 0.8) return (int) (60 * (progress - 0.8) / 0.2);
        } else {
            return 60;
        }
        return 0;
    }

    public static String[] readFile(File dataFolder, String fileName) {
        try {
            File file = new File(dataFolder, fileName);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.equals("")) lines.add(line);
            }
            reader.close();
            return lines.toArray(new String[0]);
        } catch (IOException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "[BlockRacing] Error reading blocks file!", e);
        }
        return null;
    }
}
