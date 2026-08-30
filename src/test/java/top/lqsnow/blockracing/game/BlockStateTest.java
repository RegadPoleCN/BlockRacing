package top.lqsnow.blockracing.game;

import org.junit.jupiter.api.Test;

import top.lqsnow.blockracing.core.TeamColor;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockStateTest {

    private static BlockCatalog smallCatalog() {
        return new BlockCatalog(
                List.of("STONE", "DIRT", "GRASS_BLOCK", "SAND", "GRAVEL", "OAK_LOG"),
                List.of("ANVIL", "BEEHIVE"),
                List.of("AMETHYST_CLUSTER"),
                List.of(),
                List.of());
    }

    private static RoomSettings settings(int amount, GameModeType mode) {
        RoomSettings settings = RoomSettings.fromRaw(amount, mode);
        return settings;
    }

    @Test
    void setupProducesRequestedAmountsForBothTeams() {
        BlockState blocks = new BlockState();
        BlockCatalog catalog = smallCatalog();
        blocks.setup(settings(5, GameModeType.NORMAL), catalog, new Random(42));

        assertEquals(5, blocks.all(TeamColor.RED).size());
        assertEquals(5, blocks.all(TeamColor.BLUE).size());
        assertEquals(5, blocks.remaining(TeamColor.RED).size());
    }

    @Test
    void setupOnlyUsesEnabledPool() {
        BlockState blocks = new BlockState();
        BlockCatalog catalog = smallCatalog();
        Set<String> allowed = new HashSet<>(catalog.pool(settings(6, GameModeType.NORMAL)));

        blocks.setup(settings(6, GameModeType.NORMAL), catalog, new Random(7));

        for (String block : blocks.all(TeamColor.RED)) {
            assertTrue(allowed.contains(block), "unexpected block " + block);
        }
    }

    @Test
    void racingModeGivesIdenticalOrder() {
        BlockState blocks = new BlockState();
        BlockCatalog catalog = smallCatalog();
        blocks.setup(settings(5, GameModeType.RACING), catalog, new Random(42));

        assertEquals(blocks.all(TeamColor.RED), blocks.all(TeamColor.BLUE));
    }

    @Test
    void completeRemovesFromRemainingAndFinishesGame() {
        BlockState blocks = new BlockState();
        BlockCatalog catalog = smallCatalog();
        blocks.setup(settings(2, GameModeType.NORMAL), catalog, new Random(42));

        List<String> remaining = List.copyOf(blocks.remaining(TeamColor.RED));
        for (String block : remaining) {
            assertFalse(blocks.hasFinished(TeamColor.RED));
            blocks.complete(TeamColor.RED, block);
        }
        assertTrue(blocks.hasFinished(TeamColor.RED));
        assertFalse(blocks.hasFinished(TeamColor.BLUE));
    }

    @Test
    void currentShowsUpToFourBlocks() {
        BlockState blocks = new BlockState();
        BlockCatalog catalog = smallCatalog();
        blocks.setup(settings(6, GameModeType.NORMAL), catalog, new Random(42));

        assertEquals(4, blocks.current(TeamColor.RED).size());
        assertEquals(blocks.remaining(TeamColor.RED).subList(0, 4), blocks.current(TeamColor.RED));
    }
}
