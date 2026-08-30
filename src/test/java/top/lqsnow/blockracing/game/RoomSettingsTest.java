package top.lqsnow.blockracing.game;

import org.junit.jupiter.api.Test;

import top.lqsnow.blockracing.game.GameModeType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomSettingsTest {

    @Test
    void rawFactoryKeepsExactValues() {
        RoomSettings settings = RoomSettings.fromRaw(2, GameModeType.RACING);
        assertEquals(2, settings.getBlockAmount());
        assertEquals(GameModeType.RACING, settings.getGameMode());
        assertFalse(settings.isSpeedMode());
        assertFalse(settings.isEnableMediumBlock());
    }

    @Test
    void gameModeDefaultsToNormal() {
        RoomSettings settings = RoomSettings.fromRaw(10, null);
        assertEquals(GameModeType.NORMAL, settings.getGameMode());
    }
}
