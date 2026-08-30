package top.lqsnow.blockracing.managers;

import top.lqsnow.blockracing.core.GameInstance;
import top.lqsnow.blockracing.core.GameManager;
import top.lqsnow.blockracing.game.RoomSettings;

/**
 * Transitional facade: the mutable settings live in the instance's
 * {@link RoomSettings}; changes are still written through to config.yml.
 */
@Deprecated
public class Setting {
    public enum GameMode {NORMAL, RACING}

    private Setting() {
    }

    private static RoomSettings settings() {
        return arena().getSettings();
    }

    private static GameInstance arena() {
        return GameManager.first();
    }

    public static boolean isEnableMediumBlock() {
        return settings().isEnableMediumBlock();
    }

    public static boolean isEnableHardBlock() {
        return settings().isEnableHardBlock();
    }

    public static boolean isEnableDyedBlock() {
        return settings().isEnableDyedBlock();
    }

    public static boolean isEnableEndBlock() {
        return settings().isEnableEndBlock();
    }

    public static int getBlockAmount() {
        return settings().getBlockAmount();
    }

    public static int getMaxTeamChestNum() {
        return settings().getMaxTeamChestNum();
    }

    public static int getMaxTeamWaypointNum() {
        return settings().getMaxTeamWaypointNum();
    }

    public static int getMaxRollCount() {
        return settings().getMaxRollCount();
    }

    public static boolean isSpeedMode() {
        return settings().isSpeedMode();
    }

    public static GameMode getCurrentGameMode() {
        return settings().getGameMode();
    }

    public static void setEnableMediumBlock(boolean value) {
        settings().setEnableMediumBlock(value);
    }

    public static void setEnableHardBlock(boolean value) {
        settings().setEnableHardBlock(value);
    }

    public static void setEnableDyedBlock(boolean value) {
        settings().setEnableDyedBlock(value);
    }

    public static void setEnableEndBlock(boolean value) {
        settings().setEnableEndBlock(value);
    }

    public static void setBlockAmount(int value) {
        settings().setBlockAmount(value);
    }

    public static void setSpeedMode(boolean value) {
        settings().setSpeedMode(value);
    }

    public static void setCurrentGameMode(GameMode value) {
        settings().setGameMode(value);
    }

    public static void toggleMediumBlock() {
        settings().toggleMediumBlock();
    }

    public static void toggleHardBlock() {
        settings().toggleHardBlock();
    }

    public static void toggleDyedBlock() {
        settings().toggleDyedBlock();
    }

    public static void toggleEndBlock() {
        settings().toggleEndBlock();
    }

    public static void toggleSpeedMode() {
        settings().toggleSpeedMode();
    }
}
