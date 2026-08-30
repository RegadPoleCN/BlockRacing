package top.lqsnow.blockracing.managers;

import java.util.List;

import top.lqsnow.blockracing.core.GameInstance;
import top.lqsnow.blockracing.core.GameManager;
import top.lqsnow.blockracing.core.TeamColor;
import top.lqsnow.blockracing.game.BlockCatalog;

/**
 * Transitional facade: read-only pools live in {@link BlockCatalog}, per-game
 * target lists in the instance's {@link top.lqsnow.blockracing.game.BlockState}.
 */
@Deprecated
public class Block {
    private Block() {
    }

    private static GameInstance arena() {
        return GameManager.first();
    }

    public static List<String> easyBlocks() {
        return arena().getCatalog().getEasyBlocks();
    }

    public static List<String> mediumBlocks() {
        return arena().getCatalog().getMediumBlocks();
    }

    public static List<String> hardBlocks() {
        return arena().getCatalog().getHardBlocks();
    }

    public static List<String> dyedBlocks() {
        return arena().getCatalog().getDyedBlocks();
    }

    public static List<String> endBlocks() {
        return arena().getCatalog().getEndBlocks();
    }

    public static List<String> blocks() {
        return arena().getCatalog().pool(arena().getSettings());
    }

    public static int maxBlockAmount() {
        return arena().getCatalog().maxBlockAmount(arena().getSettings());
    }

    public static List<String> redTeamBlocks() {
        return arena().getBlockState().all(TeamColor.RED);
    }

    public static List<String> blueTeamBlocks() {
        return arena().getBlockState().all(TeamColor.BLUE);
    }

    public static List<String> redTeamRemainingBlocks() {
        return arena().getBlockState().remaining(TeamColor.RED);
    }

    public static List<String> blueTeamRemainingBlocks() {
        return arena().getBlockState().remaining(TeamColor.BLUE);
    }

    public static boolean refreshAvailableBlocksAndClampAmount() {
        int clampedAmount = BlockCatalog.clampBlockAmount(arena().getSettings().getBlockAmount(), maxBlockAmount());
        if (clampedAmount == arena().getSettings().getBlockAmount()) {
            return false;
        }
        arena().getSettings().setBlockAmount(clampedAmount);
        return true;
    }

    public static int clampBlockAmount(int requestedAmount, int availableBlocks) {
        return BlockCatalog.clampBlockAmount(requestedAmount, availableBlocks);
    }

    public static boolean checkBlock() {
        return arena().validateBlocks();
    }

    public static void reloadBlock() {
        arena().getCatalog().reload(top.lqsnow.blockracing.Main.getInstance().getDataFolder());
    }

    public static int calculateEasyBlocksWeight(float progress) {
        return BlockCatalog.calculateEasyBlocksWeight(progress);
    }

    public static int calculateMediumBlocksWeight(float progress) {
        return BlockCatalog.calculateMediumBlocksWeight(progress);
    }

    public static int calculateHardBlocksWeight(float progress) {
        return BlockCatalog.calculateHardBlocksWeight(progress);
    }

    public static int calculateDyedBlocksWeight(float progress) {
        return BlockCatalog.calculateDyedBlocksWeight(progress);
    }

    public static int calculateEndBlocksWeight(float progress) {
        List<String> pool = blocks();
        return BlockCatalog.calculateEndBlocksWeight(progress, endBlocks().size(), pool.size());
    }
}
