package top.lqsnow.blockracing.managers;

import org.junit.jupiter.api.Test;

import top.lqsnow.blockracing.game.BlockCatalog;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockWeightTest {

    @Test
    void blockAmountShrinksWithAvailablePool() {
        assertEquals(120, BlockCatalog.clampBlockAmount(300, 120));
    }

    @Test
    void blockAmountKeepsConfiguredMinimumWhenPossible() {
        assertEquals(10, BlockCatalog.clampBlockAmount(3, 120));
    }

    @Test
    void blockAmountNeverExceedsSmallAvailablePool() {
        assertEquals(7, BlockCatalog.clampBlockAmount(10, 7));
    }

    @Test
    void easyWeightDecreasesAcrossGame() {
        assertEquals(100, BlockCatalog.calculateEasyBlocksWeight(0));
        assertEquals(60, BlockCatalog.calculateEasyBlocksWeight(0.5f));
        assertEquals(20, BlockCatalog.calculateEasyBlocksWeight(1));
    }

    @Test
    void mediumWeightRisesThenPlateaus() {
        assertEquals(20, BlockCatalog.calculateMediumBlocksWeight(0));
        assertEquals(60, BlockCatalog.calculateMediumBlocksWeight(0.4f));
        assertEquals(60, BlockCatalog.calculateMediumBlocksWeight(1));
    }

    @Test
    void hardWeightRisesAcrossBothSegments() {
        assertEquals(1, BlockCatalog.calculateHardBlocksWeight(0));
        assertEquals(20, BlockCatalog.calculateHardBlocksWeight(0.5f));
        assertEquals(60, BlockCatalog.calculateHardBlocksWeight(1));
    }

    @Test
    void dyedWeightIsConstant() {
        assertEquals(10, BlockCatalog.calculateDyedBlocksWeight(0));
        assertEquals(10, BlockCatalog.calculateDyedBlocksWeight(1));
    }
}
