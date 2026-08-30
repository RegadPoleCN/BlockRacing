package top.lqsnow.blockracing.game;

import org.junit.jupiter.api.Test;

import top.lqsnow.blockracing.managers.Setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomSettingsTest {

    @Test
    void rawFactoryKeepsExactValues() {
        RoomSettings settings = RoomSettings.fromRaw(2, Setting.GameMode.RACING);
        assertEquals(2, settings.getBlockAmount());
        assertEquals(Setting.GameMode.RACING, settings.getGameMode());
        assertFalse(settings.isSpeedMode());
        assertFalse(settings.isEnableMediumBlock());
    }

    @Test
    void gameModeDefaultsToNormal() {
        RoomSettings settings = RoomSettings.fromRaw(10, null);
        assertEquals(Setting.GameMode.NORMAL, settings.getGameMode());
    }
}
